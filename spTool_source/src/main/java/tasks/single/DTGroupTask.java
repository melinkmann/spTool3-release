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

import analysis.RawProcessingUtils;
import core.SpTool3Main;
import dataModelNew.Sample;
import java.util.ArrayList;
import java.util.List;

import dataModelNew.mz.Channel;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.DTGroupParams;
import sandbox.montecarlo.Isotope;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;

public class DTGroupTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(DTGroupTask.class);

  private final DTGroupParams dtGroupParams;
  private final List<Sample> selSamples;
  private final List<Channel> selChannels;

  // create new samples based on selected: check for NULL!
  public DTGroupTask(DTGroupParams dtGroupParams, List<Sample> selSamples,
                     List<Channel> selChannels) {
    super("DT grouping");
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the background
    DTGroupParams p = ((DTGroupParams) dtGroupParams.getCopyWithPreviousDateFileAndID());
    this.dtGroupParams = p;
    this.selSamples = selSamples;
    this.selChannels = selChannels;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.info("Starting dwell time change operation"
          + " in thread " + Thread.currentThread().getId());

      RawProcessingUtils.groupDT(selSamples,
          selChannels,
          dtGroupParams.getTargetDwellTime().getValue() / 1000,
          dtGroupParams.getExportIntermediateSteps().getValue());

      setProgress(1);
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

    } catch (Exception e) {
      result.clear();
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return new FunctionalTaskResult(() -> {
      List<Sample> samples = new ArrayList<>(result);
      SpTool3Main.getRunTime().getSampleReg().addNewSampleToWaitingList(samples);
    });
  }
}
