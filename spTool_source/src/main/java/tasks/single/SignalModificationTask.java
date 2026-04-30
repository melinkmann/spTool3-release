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

import analysis.*;
import dataModelNew.Sample;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.SignalModificationParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;

import java.util.concurrent.atomic.AtomicReference;

public class SignalModificationTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(SignalModificationTask.class);

  private final PopulationBranch branch;
  private final SignalModificationParams params;
  private final AtomicReference<Sample> sampleRef;

  public SignalModificationTask(String taskName, PopulationBranch branch, SignalModificationParams params,
                                AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (SignalModificationParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new EmptyTaskResult();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null) {

        LOGGER.info("Signal modification starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          if (params.getIsotopeSumBoolean().getValue()) {
            SignalModificationUtil.addIsotopeSum(sampleRef.get(), params.getExcludeIsobars().getValue(),
                params.getOnlyUseSelectedIsotopesForSum().getValue());
          }

          setProgress(1);
          break mainLoop;
        }
      }


    } catch (
        Exception e) {
      LOGGER.error("{}. Stack trace: {}", e.getMessage(), ExceptionUtils.getStackTrace(e));
    }

    setProgress(1);
    return taskResult;
  }


}
