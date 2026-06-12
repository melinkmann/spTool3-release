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

import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.CsvInterpreterParams;
import processing.parameterSets.impl.TimeRoiParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;

public class TimeRoiTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(TimeRoiTask.class);

  private final TimeRoiParams timeRoiParams;
  private final List<Sample> selSamples;

  public TimeRoiTask(String label, TimeRoiParams timeRoiParams, Sample selSample) {
    super(label);
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the background
    TimeRoiParams p = ((TimeRoiParams) timeRoiParams.getCopyWithPreviousDateFileAndID());
    this.timeRoiParams = p;
    this.selSamples = Collections.singletonList(selSample);
  }

  public TimeRoiTask(TimeRoiParams timeRoiParams, List<Sample> selSamples) {
    super("TimeRegion");
    this.timeRoiParams = timeRoiParams;
    this.selSamples = selSamples;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.info("Time cutting starts."
          + " in thread " + Thread.currentThread().getId());

      if (timeRoiParams.getReset().getValue()) {
        for (Sample selSample : selSamples) {
           /*
           Do not follow this idea:
           a) we would also add this in normal method execution (bad)
           b) we add the DT grouping at the end (does not make sense, should happen before)
           c) when we process, the method is overridden
           "add the submethod the method within the sample
            selSample.getMethod().addSet(timeRoiParams);
           */

          if (selSample instanceof SampleImpl) {
            for (Trace trace : selSample.getTraces()) {
              trace.resetTISeriesLimits();
            }
          }
        }

      } else {
        double start = timeRoiParams.getStartTime().getValue();
        double stop = timeRoiParams.getStopTime().getValue();

        for (Sample selSample : selSamples) {
           /*
           Do not follow this idea:
           a) we would also add this in normal method execution (bad)
           b) we add the DT grouping at the end (does not make sense, should happen before)
           c) when we process, the method is overridden
           "add the submethod the method within the sample
            selSample.getMethod().addSet(timeRoiParams);
           */
          if (selSample instanceof SampleImpl) {
            for (Trace trace : selSample.getTraces()) {
              // Applying this to monte carlo traces gets just messy
              //if (trace instanceof TraceImpl) {
              trace.setTISeriesLimits(start, stop, selSample);
              //}
            }
          }
        }
      }

      setProgress(1);
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

    } catch (Exception e) {
      result.clear();
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return new FunctionalTaskResult(() -> {
      // We override in the trace - no need for new sample
    //      List<Sample> samples = new ArrayList<>(result);
    //      SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(samples);
    });
  }
}
