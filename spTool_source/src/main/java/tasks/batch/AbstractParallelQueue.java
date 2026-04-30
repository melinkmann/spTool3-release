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

package tasks.batch;

import com.google.common.util.concurrent.AtomicDouble;
import core.SpTool3Main;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.BatchTask;
import tasks.Task;
import tasks.TaskManager;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.QueuedTaskResult;

public abstract class AbstractParallelQueue implements BatchTask {

  private static final Logger LOGGER = LogManager.getLogger(AbstractParallelQueue.class);

  private final TaskManager manager;

  private final String taskName;
  private final List<Task> tasks;

  private final boolean isRestricted;
  private final AtomicDouble progress;
  private final AtomicBoolean isStopped;


  public AbstractParallelQueue(String taskName, boolean isRestricted, List<Task> tasks) {
    this.taskName = taskName;
    this.tasks = Collections.synchronizedList(tasks);
    this.isRestricted = isRestricted;
    this.progress = new AtomicDouble(0);
    this.isStopped = new AtomicBoolean(false);
    this.manager = SpTool3Main.getRunTime().getTaskManager();
  }

  public void addTask(WorkingTask task) {
    this.tasks.add(task);
  }

  public String getTaskName() {
    return taskName;
  }

  // Progress notifier
  public AtomicDouble getProgress() {
    return progress;
  }

  protected void setProgress(double progress) {
    this.progress.set(progress);
  }

  // Stop, i.e. interrupt.
  public void stop() {
    isStopped.set(true);
  }

  public AtomicBoolean getIsStopped() {
    return isStopped;
  }

  //
  public boolean canProceed() {
    // Don't proceed if 1) this.isRestricted() AND 2) the manager.isWorking().
    boolean doNotProceed = isRestricted && manager.getIsWorking().get();
    boolean canProceed = !doNotProceed;
    return canProceed;
  }


  @Override
  public TaskResult call() {

    // Results are accessed via the pointer which makes them accessible in the QueuedTaskResult.
    List<TaskResult> results = new ArrayList<>();
    TaskResult taskResults = new QueuedTaskResult(results);

    //Helper & Tracker.
    List<Future<TaskResult>> futures = new ArrayList<>();
    int loggerTimer = 0;

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      // Estimate number of Iterations for Progress
      int iterations = tasks.size();

      // Submit all tasks to the executor
      for (int i = 0; i < iterations; i++) {
        // Submit
        Task task = tasks.get(i);
        Future<TaskResult> future;
        if (task instanceof BatchTask) {
          future = manager.forceSilentlyToHousekeepingPool((BatchTask) task);
        } else {
          future = manager.submit((WorkingTask) task);
        }
        futures.add(future);
      } // End of Submitting Iteration

      // START WAITING LOOP ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

      // Wait until finished or stopped.
      boolean isAllDone = false;
      // Loop.
      while (!isAllDone) {
        loggerTimer++;

        // Check stopping condition.
        if (isStopped.get()) {
          // Notify WorkingThreads to stop.
          tasks.forEach(Task::stop);
          futures.forEach(f -> f.cancel(false));
          LOGGER.info("Stopped Queue.");
          break;
        }

        // Calculate Progress
        double averageSubProgress = tasks.stream()
            .map(Task::getProgress)
            .map(AtomicDouble::get)
            .mapToDouble(Double::doubleValue)
            .average().orElse(0);
        setProgress(averageSubProgress);

        // Wait.
        try {
          Thread.sleep(TaskManager.HOUSEKEEPING_SLEEP);
        } catch (InterruptedException e) {
          LOGGER.warn(e);
        }

        // Status to console.
        if (loggerTimer % TaskManager.HOUSEKEEPING_MODULO == 0) {
          long finishedCount = futures.stream().filter(Future::isDone).count();
          LOGGER.trace("Parallel Queue completed "
              + (finishedCount) + "/" + (tasks.size())
              + " tasks. Time spent running: "
              + loggerTimer * TaskManager.HOUSEKEEPING_SLEEP / 1000 + " seconds."
              + " Queue is in thread " + Thread.currentThread().getId());
        }

        // End if no futures are there (i.e. add was never called due to checking conditions)
        if (futures.isEmpty()) {
          isAllDone = true;
        }
        // or wait for all futures to become true
        for (Future<?> future : futures) {
          if (!future.isDone()) {
            isAllDone = false;
            break;
          } else {
            isAllDone = true;
          }
        }

      } // End of While.

      // END WAITING LOOP ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

      // All Futures should be done. Extract the results.
      for (Future<TaskResult> future : futures) {
        if (!future.isCancelled()){
          results.add(future.get());
        }
      }

    } catch (Exception e) {
      LOGGER.warn(e);
    }

    if (isStopped.get()) {
      LOGGER.info("Parallel Batch called '" + getTaskName() + "' was stopped.");
    } else {
      LOGGER.info("Parallel Batch called '" + getTaskName() + "' finished all tasks.");
    }

    return taskResults;
  }


}
