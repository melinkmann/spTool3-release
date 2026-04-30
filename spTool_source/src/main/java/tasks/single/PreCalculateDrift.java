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

import analysis.AnalysisUtils;
import analysis.NpPopulation;
import analysis.Population;
import analysis.PopulationID;
import dataModelNew.Sample;
import dataModelNew.Trace;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import math.stat.DriftFactor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;

public class PreCalculateDrift extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(PreCalculateDrift.class);

  private final AtomicReference<Sample> sampleRef;

  public PreCalculateDrift(AtomicReference<Sample> sampleRef) {
    super("Calculate drift");
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
       LOGGER.trace("Calculate drift values"
           + " in thread " + Thread.currentThread().getId());

      Sample sample = sampleRef.get();
      if (sample != null) {
        final List<PopulationID> allPops = sample.getTraces().stream()
            .map(Trace::getAllPopulations)
            .flatMap(Collection::stream)
            .map(Population::getId)
            .distinct()
            .collect(Collectors.toList());

        final List<Trace> allTraces = new ArrayList<>(sample.getTraces());

        //
        for (Trace trace : allTraces) {
          for (PopulationID popID : allPops) {
            // Note: hasType() excludes the simulated populations, which do not have BG data anyway.
            if (trace.hasType(popID)) {

              // NpPopulation.DEFAULT_DRIFT (= -2) indicates that the drift has never been calculated for the trace
              Population pop = trace.getPopulation(popID);
              double df = NpPopulation.DEFAULT_DRIFT;
              if (pop != null) {
                pop.getDrift();
                if (df == NpPopulation.DEFAULT_DRIFT) {
                  df = DriftFactor.calculateDriftFactor(trace, popID);
                  pop.setDrift(df);
                }
              }
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
    });
  }
}
