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
import dataModelNew.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.parameterSets.impl.EventDataRangeParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;
import util.NF;
import util.SnF;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EventRangeRoiTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(EventRangeRoiTask.class);

  private final EventDataRangeParams eventRangeParams;
  private final List<Sample> selSamples;
  private final List<PopulationID> populationIDs;

  public EventRangeRoiTask(String label, EventDataRangeParams timeRoiParams, Sample selSample) {
    super(label);
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the
    // background
    EventDataRangeParams p = ((EventDataRangeParams) timeRoiParams.getCopyWithPreviousDateFileAndID());
    this.eventRangeParams = p;
    this.selSamples = Collections.singletonList(selSample);
    this.populationIDs = selSample.listAllPopulations();
  }

  public EventRangeRoiTask(EventDataRangeParams eventRangeParams, List<Sample> selSamples,
                           List<PopulationID> populationIDs) {
    super("EventDataRange");
    this.eventRangeParams = eventRangeParams;
    this.selSamples = selSamples;
    this.populationIDs = populationIDs;
  }

  @Override
  public TaskResult call() {

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.info("Time cutting starts."
          + " in thread " + Thread.currentThread().getId());

      double start = eventRangeParams.getStartValue().getValue();
      double stop = eventRangeParams.getEndValue().getValue();
      String label = eventRangeParams.getLabelParameter().getValue();
      EventParameter eventParameter = eventRangeParams.getEventParameter().getValue();

      for (Sample selSample : selSamples) {

        if (selSample instanceof SampleImpl) {
          for (Trace trace : selSample.getTraces()) {
            for (PopulationID populationID : populationIDs) {
              Population oldPop = trace.getPopulation(populationID);
              if (oldPop != null) {
                PopParSummary summary = oldPop.getInputSummary().copy();
                summary.add("s", SnF.doubleToString(start, NF.D1C1));
                summary.add("e", SnF.doubleToString(stop, NF.D1C1));
                List<Event> npEvents = selSample.getNPEvents(trace.getChannel(), populationID);
                List<Event> inRoi = new ArrayList<>();
                for (Event npEvent : npEvents) {
                  double value = npEvent.get(eventParameter);
                  if (start <= value && value <= stop) {
                    inRoi.add(npEvent);
                  }
                }

                /// ADD
                // (1) Remove existing population from the Trace to avoid pile up of Populations)
                // trace.removePopulation(popID);
                // (2) Update as new ID with the ROI;
                PopulationID idCopy = new PopulationID(populationID);
                // (2) append existing ID as new main of branch
                idCopy.append(new PopulationStep.ManualRoiSubtype(label, start, stop));
                // (3) Add the new Population to the trace
                trace.addOverridePopulation(idCopy,
                    new NpPopulation(
                        idCopy,
                        oldPop,
                        new SubEventCollection(trace, inRoi, oldPop),
                        oldPop.getInputSummary().copy()),
                    false);
                // (4) Updating the branch: not necessary as it still has the pointer and ID was only
                // "appended"
              }
            }
          }
        } else if (selSample instanceof IncompleteSample) {
          for (Trace trace : selSample.getTraces()) {
            for (PopulationID populationID : populationIDs) {
              Population oldPop = trace.getPopulation(populationID);
              if (oldPop != null) {
                PopParSummary summary = oldPop.getInputSummary().copy();
                summary.add("s", SnF.doubleToString(start, NF.D1C1));
                summary.add("e", SnF.doubleToString(stop, NF.D1C1));
                IncompleteParticleMatrix incompleteParticleMatrix = ((IncompleteSample) selSample).getMatrix();

                double[] data = AnalysisUtils.getFromIncomplete(incompleteParticleMatrix, eventParameter);
                List<Integer> validIndices = new ArrayList<>();

                for (int i = 0; i < data.length; i++) {
                  double value = data[i];
                  if (start <= value && value <= stop) {
                    validIndices.add(i);
                  }
                }

                if (!validIndices.isEmpty()){
                  IncompleteParticleMatrix roiMatrix = incompleteParticleMatrix.roi(validIndices);

                  // (1) Remove existing population from the Trace to avoid pile up of Populations)
                  // trace.removePopulation(popID);
                  // (2) Update as new ID with the ROI;
                  PopulationID idCopy = new PopulationID(populationID);
                  // (2) append existing ID as new main of branch
                  idCopy.append(new PopulationStep.ManualRoiSubtype(label, start, stop));
                  Population population = new NpPopulation(idCopy,
                      new IncompleteEventCollection(trace, roiMatrix), summary);
                  // (3) Add the new Population to the trace
                  trace.addOverridePopulation(idCopy, population,false);
                  // (4) Updating the branch: not necessary as it still has the pointer and ID was only
                  // "appended"
                }
              }
            }
          }
        }
      }

      setProgress(1);
      // END ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

    } catch (Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
    return new FunctionalTaskResult(() -> {
      // Empty
    });
  }
}
