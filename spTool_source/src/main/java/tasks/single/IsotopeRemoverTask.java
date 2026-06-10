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
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.parameterSets.impl.IsotopeRemoverParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;
import util.ArrUtils;
import util.NF;
import util.SnF;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class IsotopeRemoverTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(IsotopeRemoverTask.class);

  private final PopulationBranch branch;
  private final IsotopeRemoverParams params;
  private final AtomicReference<Sample> sampleRef;
  private final AtomicBoolean hasChanged = new AtomicBoolean(false);

  public IsotopeRemoverTask(String taskName, PopulationBranch branch, IsotopeRemoverParams params,
                            AtomicReference<Sample> sampleRef) {
    super(taskName);
    this.branch = branch;
    this.params = (IsotopeRemoverParams) params.getCopyWithPreviousDateFileAndID();
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {
    // Define the Result (here, only dummy that is overwritten later)
    TaskResult taskResult = new FunctionalTaskResult(() -> {
      // We changed traces
      if (hasChanged.get()) {
        LOGGER.debug("Isotopes have been filtered and some were removed.");
      } else {
        LOGGER.debug("Did not remove any isotopes.");
      }
    });

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {

      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      if (sampleRef.get() != null) {

        LOGGER.info("Signal modification starts for " + sampleRef.get().getNickName()
            + " in thread " + Thread.currentThread().getId());
        double counter = 0;
        mainLoop:
        while (!getIsStopped().get()) {

          if (params.getEnableBoolean().getValue()) {

            HashMap<Trace, String> badTraces = new HashMap<>();
            Sample sample = sampleRef.get();
            if (sample instanceof SampleImpl) {

              List<Trace> traces = sample.getTraces();
              for (Trace trace : traces) {

                PopulationID popID = branch.getID(trace);
                if (popID != null) {

                  if (getIsStopped().get()) {
                    break;
                  }

                  Population pop = trace.getPopulation(popID);
                  if (pop != null) {
                    int nEvt = trace.getNoOfEvents(popID);
                    double duration = trace.getTISeries().getDuration();
                    double rate = nEvt / duration;
                    double minNetArea = ArrUtils.getMin(trace.get(popID, EventType.NP, EventParameter.NET_AREA));

                    int lowerCutoff = params.getLowerIsotopeAbsoluteCutoff().getValue();
                    double upperCutoff = params.getUpperIsotopeRateCutoff().getValue();
                    double lowerAreaCutoff = params.getNetAreaThreshold().getValue();
                    if (rate > upperCutoff && params.getRemoveMoreIsotopesThan().getValue()) {
                      badTraces.merge(trace,
                          trace.getChannel().getUIString() + ">" + SnF.doubleToString(upperCutoff, NF.D1C1),
                          (existing, newVal) -> existing + "&" + newVal);
                      hasChanged.set(true);
                    }
                    if (nEvt < lowerCutoff && params.getRemoveFewerIsotopesThan().getValue()) {
                      badTraces.merge(trace,
                          trace.getChannel().getUIString() + "<" + lowerCutoff,
                          (existing, newVal) -> existing + "&" + newVal);
                      hasChanged.set(true);
                    }
                    if (minNetArea < lowerAreaCutoff && params.getRemoveLessIntenseIsotopesThan().getValue()) {
                      badTraces.merge(trace,
                          trace.getChannel().getUIString() + "<" + minNetArea + "cts",
                          (existing, newVal) -> existing + "&" + newVal);
                      hasChanged.set(true);
                    }
                  } else {
                    // pop does not event exist -> remove
                    badTraces.put(trace, trace.getChannel().getUIString() + ":N/A");
                  }

                }
              }
            }

            // do remove
            for (Trace trace : badTraces.keySet()) {
              sample.removeTrace(trace, badTraces.get(trace));
            }
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
