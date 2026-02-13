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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.BatchTask;
import tasks.Task;
import tasks.TaskManager;
import tasks.TaskResult;
import tasks.WorkingTask;

public abstract class AbstractLinearQueue implements BatchTask {

  private static final Logger LOGGER = LogManager.getLogger(AbstractLinearQueue.class);

  private final TaskManager manager;

  private final String taskName;
  private final List<Task> tasks;

  private final boolean isRestricted;
  private final AtomicDouble progress;
  private final AtomicBoolean isStopped;

  public AbstractLinearQueue(String taskName, boolean isRestricted, List<Task> tasks) {
    this.taskName = taskName;
    this.tasks = Collections.synchronizedList(tasks);
    this.isRestricted = isRestricted;
    this.progress = new AtomicDouble(0);
    this.isStopped = new AtomicBoolean(false);
    this.manager = SpTool3Main.getRunTime().getTaskManager();
  }

  public void addTask(Task task) {
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


  public void callWithoutResult() {
    // Results are accessed via the pointer which makes them accessible in the QueuedTaskResult.
    List<TaskResult> results = new ArrayList<>();

    //Helper & Tracker.
    List<Future<TaskResult>> futures = new ArrayList<>();
    int loggerTimer = 0;
    int stepCounter = 0;

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      // Estimate number of Iterations for Progress
      int iterations = tasks.size();

      // Submit all task by task to the executor -->  Iterator is way more simple than looping.
      Iterator<Task> taskIterator = tasks.listIterator();
      while (taskIterator.hasNext()) {
        // Check stopping condition first.
        if (getIsStopped().get()) {
          // Notify WorkingThreads to stop.
          tasks.forEach(Task::stop);
          futures.forEach(f -> f.cancel(false));
          LOGGER.info("Stopped Queue.");
          break;
        }

        // Go on.
        stepCounter++;
        Task task = taskIterator.next();

        // Submit
        Future<TaskResult> future;
        if (task instanceof BatchTask) {
          future = manager.forceSilentlyToHousekeepingPool((BatchTask) task);
        } else {
          future = manager.submit((WorkingTask) task);
        }
        futures.add(future);
        LOGGER.debug("Submitted task: <" + task.getTaskName()
            + "> linearly as step number " + (stepCounter) + " / " + (iterations)
            + ". Queue has thread " + Thread.currentThread());

        // START WAITING LOOP ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
        // Loop.
        boolean isDone = false;
        while (!isDone && !getIsStopped().get()) {
          loggerTimer++;

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
            LOGGER.error(ExceptionUtils.getStackTrace(e));
          }

          // Status to console.
          if (loggerTimer % TaskManager.HOUSEKEEPING_MODULO == 0) {
            long finishedCount = futures.stream().filter(Future::isDone).count();
            LOGGER.trace("Linear Batch completed "
                + (finishedCount) + "/" + (tasks.size())
                + " tasks. Time spent running: "
                + loggerTimer * TaskManager.HOUSEKEEPING_SLEEP / 1000 + " seconds."
                + " Queue has thread " + Thread.currentThread());
          }

          isDone = future.isDone();

          // END WAITING LOOP ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
        }// End of Waiting While.

        // The Future should be done except for cancellation event. Extract the results.
        if (future.isDone() && !future.isCancelled()) {
          TaskResult result = future.get();
          result.process();
          results.add(result);
        }
      } // End of Iterator While.

    } catch (
        Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }

    if (isStopped.get()) {
      LOGGER.info("Linear Batch called '" + getTaskName() + "' was stopped.");
    } else {
      LOGGER.info("Linear Batch called '" + getTaskName() + "' finished all tasks.");
    }
  }


}
