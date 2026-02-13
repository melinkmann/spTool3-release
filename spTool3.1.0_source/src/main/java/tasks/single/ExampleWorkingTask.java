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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.DoubleListResult;

public class ExampleWorkingTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(ExampleWorkingTask.class);

  public ExampleWorkingTask(String taskName) {
    super(taskName);
  }

  @Override
  public TaskResult call() {

    // Define the Result
    List<Double> result = new ArrayList<>();
    TaskResult taskResult = new DoubleListResult(result);

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      // Estimate number of Iterations for Progress
      int iterations = 15;

      // Iterate
      for (int i = 0; i < iterations; i++) {

        // Check stopping condition.
        if (getIsStopped().get()) {
          break;
        }
        setProgress((double) (i + 1) / (double) iterations);

        // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
        Random random = new Random();
        int rand = random.nextInt(4 - 2 + 1) + 2; //random.nextInt(max - min + 1) + min
        Double d = rand * 1000d;
        result.add(d);

        // ## ## Simulate load.
        try {
          Thread.sleep(rand * 50L);
        } catch (InterruptedException e) {
          LOGGER.error(ExceptionUtils.getStackTrace(e));
        }

        // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

      } // End of Iterations
    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return taskResult;
  }

}
