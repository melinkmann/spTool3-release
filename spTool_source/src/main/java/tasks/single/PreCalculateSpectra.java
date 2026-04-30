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

import analysis.NpPopulation;
import analysis.Population;
import analysis.PopulationID;
import analysis.SpectralUtil;
import dataModelNew.Sample;
import dataModelNew.Trace;
import math.stat.DriftFactor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PreCalculateSpectra extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(PreCalculateSpectra.class);

  private final AtomicReference<Sample> sampleRef;

  public PreCalculateSpectra(AtomicReference<Sample> sampleRef) {
    super("Calculate spectra");
    this.sampleRef = sampleRef;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.trace("Fetch and store mass spectra"
          + " in thread " + Thread.currentThread().getId());

      Sample sample = sampleRef.get();
      if (sample != null) {
        final List<PopulationID> allPops = sample.listAllPopulations();

        for (int i = 0; i < allPops.size(); i++) {
          PopulationID popID = allPops.get(i);
          // Checking whether applicable or not is carried out inside the function (isP or isAligned)
          SpectralUtil.computeSpectra(sample, popID);

          setProgress((double) i / allPops.size());
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
