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

import analysis.Baseline;
import analysis.BaselineGenerator;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.BaselineParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.EmptyTaskResult;

public class BaselineTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(BaselineTask.class);

  private final BaselineParams params;
  private final AtomicReference<Sample> sampleRef;

  public BaselineTask(String taskName, BaselineParams params, AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.params = (BaselineParams) params.getCopyWithPreviousDateFileAndID();
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
        LOGGER.info("Baseline computation starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          Sample sample = sampleRef.get();

          if (sample instanceof SampleImpl) {

            List<Trace> traces = sample.getTraces();
            for (Trace trace : traces) {
              counter++;

              if (getIsStopped().get()) {
                break;
              }

              double[] data = trace.getTISeries().getIntensity();
              Baseline bln = BaselineGenerator.generateBaseline(params, data);
              trace.setBaseline(bln);

              setProgress(counter / traces.size());
            }
          }
          setProgress(1);
          break mainLoop;
        }
      }

    } catch (Exception e) {
      LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
    }

    return taskResult;
  }


}
