/*
 *  Copyright 2026 Matthias Elinkmann, spTool3
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package tasks;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javafx.application.Platform;
import javafx.scene.control.ProgressBar;
import javafx.scene.text.Text;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.batch.AbstractLinearQueue;
import util.NF;
import util.SnF;

public class TaskManager {

  private static final Logger LOGGER = LogManager.getLogger(TaskManager.class);

  public static final long HOUSEKEEPING_SLEEP = 100; // milliseconds
  // HOUSEKEEPING_MODULO: one tracker tick each 100 ms; 20 ticks = 2000ms = 2s. 30 ticks = 3000ms = 3s.
  public static final int HOUSEKEEPING_MODULO = 50;

  // PRINCIPLE: executors can be shut down nicely. Better than creating Threads whenever housekeeping.
  private final ExecutorService graphPool;
  private final ExecutorService workingPool;
  private final ExecutorService housekeepingPool;
  private final ExecutorService parallelBatchHousekeepingPool; // limit parallel execution to keep weak refs
  private final ScheduledExecutorService managerPool;

  // Are tasks running?

  /*
  Keep track of running worker tasks to answer the question if processing is completely done
  via the isWorking boolean.
   */
  private final Map<WorkingTask, Future<TaskResult>> runningWorkerTasks;
  private final AtomicBoolean isWorking;

  /*
   Keep track of housekeeping (usually Batch organizing) tasks.
   If they are listed in the map, they contribute to the progress and can be terminated.
   Note: If e.g. a LinearQueue already lists the sub-tasks, they can be added silently by the
   linear queue.
   */
  private final Map<Task, Future<TaskResult>> runningWatchedBatchTasks;
  private final Map<Task, Future<TaskResult>> runningWatchedGraphTasks;

  /*
  Only one batch at a time is expected to run. Hence, have a waiting list.
  If a Batch organizing Task is at play, this is taken over by that Task. However, if Buttons are
  clicked twice or two different Buttons are clicked etc., then this waiting list is needed.
   */
  private final List<BatchTask> waitingBatchTasks;

  // Show progress.
  private ProgressBar mainProgressIndicator = new ProgressBar(1);
  private Text progressIndicatorPercent = new Text("0 %");
  private Text ramStatusLbl = new Text("0000/0000 MB");
  // Storage status
  private final AtomicInteger storageFileCounter = new AtomicInteger(0);
  private Text storageStatusLbl = new Text("0 GB");

  // Needed to cover the waiting list in the progress.
  private final AtomicInteger batchDoneCounter;
  private final AtomicInteger batchTotalCounter;


  public TaskManager(int parallelThreads) {

    /*
     Heavily active tasks --> not too many
     */
    workingPool = Executors.newFixedThreadPool(parallelThreads,
        new ThreadFactoryBuilder().setDaemon(true).build());


    /*
    Graphs: only ONE at a time to avoid parallel race conditions between javaFX and prism pipeline
     */
    graphPool = Executors.newFixedThreadPool(1, new ThreadFactoryBuilder().setDaemon(true).build());

    /*
    Mostly tasks waiting for futures to complete --> needs more at the same time,
    queueing of these is not really possible. E.g. a QueueBatch is running in an HousekeepingThread
    and gets&submits the heavy working Tasks on its own. It then only exists to check and report
    that its Batch is done completely. This is what the TaskManager checks and then removes from its
    FutureList.
    */
    housekeepingPool = Executors.newCachedThreadPool
        (new ThreadFactoryBuilder().setDaemon(true).build());


    /*
    see Housekeeping pool: Special case, as we want only n batches to be running in parallel. Why?
    As samples are becoming large AND have many weak reference pointers, when too many
    are scheduled at the same time, weak pointers are lost and need restoring too frequently.
     */
    parallelBatchHousekeepingPool = Executors.newFixedThreadPool(parallelThreads,
        new ThreadFactoryBuilder().setDaemon(true).build());

    /*
    The Manager is running in the background all the time,
    spTool uses a ScheduledPool with scheduleAtFixedRate();
     */
    managerPool = Executors.newScheduledThreadPool(1,
        new ThreadFactoryBuilder().setDaemon(true).build());

    /*
    In order to implement behaviour such as "do not export while still calculating" or the fact that
    the workingThreadPool is limited to the CPU's cores, spTool uses Lists of what is queued and running.
     */
    this.runningWorkerTasks = Collections.synchronizedMap(new LinkedHashMap<>());
    this.runningWatchedBatchTasks = Collections.synchronizedMap(new LinkedHashMap<>());
    this.runningWatchedGraphTasks = Collections.synchronizedMap(new LinkedHashMap<>());
    this.waitingBatchTasks = Collections.synchronizedList(new ArrayList<>());
    this.isWorking = new AtomicBoolean(false);

    this.batchDoneCounter = new AtomicInteger(0);
    this.batchTotalCounter = new AtomicInteger(0);

    /*
    For each Task, in this case mostly BatchOperations that report that all of their subTasks are done,
    the Manager must keep Track if they are done or not:
     */
    LOGGER.info("Created TaskManager. " + "Number of parallel threads: " + parallelThreads);

    final Runnable taskWatch = () -> {

      updateRamUsage();
//
//      if (!runningWorkerTasks.isEmpty()) {
//        LOGGER.trace("Running: " + runningWorkerTasks.keySet().stream()
//            .map(WorkingTask::getTaskName)
//            .collect(Collectors.joining()));
//      }
//
//      if (!waitingBatchTasks.isEmpty()) {
//        LOGGER.trace("Waiting: " + waitingBatchTasks.stream()
//            .map(BatchTask::getTaskName)
//            .collect(Collectors.joining()));
//      }
//
//      if (!runningWatchedBatchTasks.isEmpty()) {
//        LOGGER.trace("Running and watched: " + runningWatchedBatchTasks.keySet().stream()
//            .map(Task::getTaskName)
//            .collect(Collectors.joining()));
//      }

      try {
        // Check if finished tasks can be removed.
        runningWorkerTasks.entrySet().removeIf(entries -> entries.getValue().isDone());
        // Increment the "done" counter.
        runningWatchedBatchTasks
            .values()
            .stream()
            .filter(Future::isDone)
            .forEach(i -> batchDoneCounter.incrementAndGet());
        // Trigger method in the task result of the batch
        runningWatchedBatchTasks
            .values()
            .stream()
            .filter(Future::isDone)
            .forEach(f -> {
              try {
                TaskResult result = f.get();
                result.process();
              } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
              }
            });
        // Remove batches that have been done
        runningWatchedBatchTasks.entrySet().removeIf(entries -> entries.getValue().isDone());

        // Increment the "done" counter... exactly parallel as above
        runningWatchedGraphTasks
            .values()
            .stream()
            .filter(Future::isDone)
            .forEach(i -> batchDoneCounter.incrementAndGet());
        // Trigger method in the task result of the batch
        runningWatchedGraphTasks
            .values()
            .stream()
            .filter(Future::isDone)
            .forEach(f -> {
              try {
                TaskResult result = f.get();
                result.process();
              } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(ExceptionUtils.getStackTrace(e));
              }
            });
        // Remove batches that have been done
        runningWatchedGraphTasks.entrySet().removeIf(entries -> entries.getValue().isDone());

        // Calculate progress. Housekeepers should report adequate progress.
        if (runningWatchedBatchTasks.isEmpty() && waitingBatchTasks.isEmpty() && runningWatchedGraphTasks.isEmpty()) {
          showProgressStopped();
          batchDoneCounter.set(0);
          batchTotalCounter.set(0);
        }

        if (!runningWatchedBatchTasks.isEmpty() || !runningWatchedGraphTasks.isEmpty()) {
          int batchRunningCount = runningWatchedBatchTasks.size() + runningWatchedGraphTasks.size();
          int totalDone = batchDoneCounter.get();
          int totalTotal = Math.max(1, batchTotalCounter.get());
          double sumBatch = runningWatchedBatchTasks.keySet()
              .stream()
              .map(Task::getProgress)
              .map(AtomicDouble::doubleValue)
              .mapToDouble(Double::doubleValue)
              .sum();
          double sumGraph = runningWatchedGraphTasks.keySet()
              .stream()
              .map(Task::getProgress)
              .map(AtomicDouble::doubleValue)
              .mapToDouble(Double::doubleValue)
              .sum();
          double sumProgress = sumBatch + sumGraph;
          double progress = (sumProgress / batchRunningCount + totalDone) / totalTotal;
          showProgressRunning(progress);
        }

        // Check if waiting task in the HousekeepingQueue and add them.
        if (runningWatchedBatchTasks.isEmpty()) {
          if (!waitingBatchTasks.isEmpty()) {
            BatchTask batchTask = waitingBatchTasks.get(0);
            waitingBatchTasks.remove(batchTask);

            Future<TaskResult> future;
            if (batchTask instanceof AbstractLinearQueue) {
              future = parallelBatchHousekeepingPool.submit(batchTask);
            } else {
              future = housekeepingPool.submit(batchTask);
            }
            runningWatchedBatchTasks.put(batchTask, future);
          }
        }

        // Check if there are working tasks running and set "semaphore".
        isWorking.set(!runningWorkerTasks.isEmpty());

      } catch (Exception e) {
        LOGGER.error(ExceptionUtils.getStackTrace(e));
      }
    };

    // start the runnable
    managerPool.scheduleAtFixedRate(taskWatch, 1, HOUSEKEEPING_SLEEP, TimeUnit.MILLISECONDS);
  }


  // _________________ Called after main window closed  ___________________
  public synchronized void shutdownThreadPools() {
    housekeepingPool.shutdown();
    parallelBatchHousekeepingPool.shutdown();
    workingPool.shutdown();
    managerPool.shutdown();
    graphPool.shutdown();

    LOGGER.info("Shutdown of thread pools." +
        " Housekeeping down: " + housekeepingPool.isShutdown()
        + ". Parallel housekeeping down: " + parallelBatchHousekeepingPool.isShutdown()
        + ". Graphing down: " + graphPool.isShutdown()
        + ". Worker down: " + workingPool.isShutdown()
        + ". Manager down: "
        + managerPool.isShutdown());
  }

  // _________________ Updating GUI progress bar _________________________
  private void showProgressRunning(double val) {
    Platform.runLater(() -> {
      mainProgressIndicator.setProgress(val);
      mainProgressIndicator.setStyle("-fx-accent: rgb(50,50,255)");
      progressIndicatorPercent.setText(SnF.doubleToString(100 * val, NF.D1C0) + "  %");
    });
  }

  private void showProgressStopped() {
    Platform.runLater(() -> {
      mainProgressIndicator.setProgress(1.0);
      mainProgressIndicator.setStyle("-fx-accent: rgb(0,220,0)");
      progressIndicatorPercent.setText(SnF.doubleToString(100, NF.D1C0) + "  %");
    });
  }

  //
  private void updateRamUsage() {
    // RAM Status
    // Get current size of heap in bytes.
    // When heap grows, it will reach max and then just remain at max.
    long heapSize = Runtime.getRuntime().totalMemory();

    // Get maximum size of heap in bytes. The heap cannot grow beyond this size.
    // Any attempt will result in an OutOfMemoryException.
    long heapMaxSize = Runtime.getRuntime().maxMemory();

    // Get amount of free memory within the heap in bytes. This size will
    // increase after garbage collection and decrease as new objects are created.
    // This is the interesting value for us!
    long heapFreeSize = Runtime.getRuntime().freeMemory();

    final String ram =
        SnF.doubleToString(SnF.round((heapSize - heapFreeSize) / 1E6, 10), NF.D1C0)
            + " / "
            + SnF.doubleToString(SnF.round(heapMaxSize / 1E6, 10), NF.D1C0)
            + " MB";

    Platform.runLater(() -> {
      ramStatusLbl.setText(ram);
      storageStatusLbl.setText(storageFileCounter.get() + " GB");
    });
  }

  //
  public void setMainProgressIndicator(ProgressBar mainProgressIndicator,
                                       Text progressIndicatorPercent) {
    this.mainProgressIndicator = mainProgressIndicator;
    this.progressIndicatorPercent = progressIndicatorPercent;
    showProgressStopped(); //initialize as full and green
  }

  public void setRamStatusLbl(Text ramStatusLbl) {
    this.ramStatusLbl = ramStatusLbl;
  }

  public void setStorageStatusLbl(Text storageStatusLbl) {
    this.storageStatusLbl = storageStatusLbl;
  }


  // _________________ Interfacing with the Queues _______________________
  public synchronized Future<TaskResult> submit(WorkingTask task) {
    Future<TaskResult> future = workingPool.submit(task);
    runningWorkerTasks.put(task, future);
    return future;
  }

  public AtomicBoolean getIsWorking() {
    return isWorking;
  }

  // _________________ Housekeeping, i.e. managing or watching threads

  /**
   * Manually add a single Batch to the Housekeeping pool WITH progress report.
   *
   * @param batchTask: Preferably a BatchTask instance.
   */
  public synchronized Future<TaskResult> forceToHousekeepingPool(Task batchTask) {
    Future<TaskResult> future;
    if (batchTask instanceof AbstractLinearQueue) {
      future = parallelBatchHousekeepingPool.submit(batchTask);
    } else {
      future = housekeepingPool.submit(batchTask);
    }
    batchTotalCounter.incrementAndGet();
    runningWatchedBatchTasks.put(batchTask, future);
    return future;
  }


  public synchronized Future<TaskResult> forceToGraphPool(Task graphTask) {
    Future<TaskResult> future = graphPool.submit(graphTask);
    batchTotalCounter.incrementAndGet();
    runningWatchedGraphTasks.put(graphTask, future);
    return future;
  }


  /**
   * Linear Batch can add its Sub-Parallel Batches without putting them on the progress watch or on
   * the stop list. Why? --< Scenario: A queue of e.g. parallel operations wants to queue its
   * parallel operations. The parent queue already correctly reports the progress of itself and the
   * children. In that case "queueToHousekeepingPool()" would put its children also on progress
   * watch which also calculates the remaining un-submitted tasks. Thus, these un-submitted would
   * yield wrong progress estimates.
   *
   * @param batchTask: BatchTask instance.
   */

  public synchronized Future<TaskResult> forceSilentlyToHousekeepingPool(BatchTask batchTask) {
    Future<TaskResult> future;
    if (batchTask instanceof AbstractLinearQueue) {
      future = parallelBatchHousekeepingPool.submit(batchTask);
    } else {
      future = housekeepingPool.submit(batchTask);
    }
    return future;
  }

  /**
   * VOID: If a future is returned and the FXThread has to wait for the future, the UI freezes.
   * Hence, all waiting, coordination and post-processing must be done in the Queueing class.
   */
  public synchronized void queueToHousekeepingPool(BatchTask batchTask) {
    batchTotalCounter.incrementAndGet();
    waitingBatchTasks.add(batchTask);
  }


  public synchronized void notifyNewStorageFile() {
    storageFileCounter.incrementAndGet();
  }

  // ________________________________________________________
  public synchronized void stop() {
    for (Task t : runningWorkerTasks.keySet()) {
      t.stop();
    }
    for (Task t : runningWatchedBatchTasks.keySet()) {
      t.stop();
    }
    for (Task t : runningWatchedGraphTasks.keySet()) {
      t.stop();
    }
    waitingBatchTasks.clear();
  }


}