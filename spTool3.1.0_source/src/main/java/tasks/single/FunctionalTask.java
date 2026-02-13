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

package tasks.single;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;
import util.Functional;

public class FunctionalTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(FunctionalTask.class);

  private final Functional task;
  private final FunctionalTaskResult result;

  public FunctionalTask(String taskName, Functional task, FunctionalTaskResult result) {
    super(taskName);
    this.task = task;
    this.result = result;
  }

  @Override
  public TaskResult call() {

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

      task.proceed();

      setProgress(1);
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return result;
  }
}
