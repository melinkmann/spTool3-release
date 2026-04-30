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
import io.impl.CsvInterpreter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.CsvInterpreterParams;
import tasks.TaskResult;
import tasks.WorkingTask;
import tasks.results.FunctionalTaskResult;

public class CsvImportTask extends AbstractWorkingTask implements WorkingTask {

  private static final Logger LOGGER = LogManager.getLogger(CsvImportTask.class);

  private final CsvInterpreterParams params;
  private final Path path;

  public CsvImportTask(CsvInterpreterParams params, Path path) {
    super("CSV data import");
    // pass a copy to avoid changes in UI trickling down into multi thread environment when running in the
    // background
    CsvInterpreterParams p = ((CsvInterpreterParams) params.getCopyWithPreviousDateFileAndID());
    this.params = p;
    this.path = path;
  }

  @Override
  public TaskResult call() {

    List<Sample> result = new ArrayList<>();

    // Catch any Exception that may occur in the background that would not go through the stack.
    try {
      setProgress(0);
      // START ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##
      LOGGER.info("CSV import starts for " + path
          + " in thread " + Thread.currentThread().getId());

      CsvInterpreter csvInterpreter = params.getInterpreter();
      csvInterpreter.parse(path);
      result.addAll(csvInterpreter.getSamples());
      csvInterpreter = null; // idea: offer for GC?

      // make sure the method is stored in the sample (esp. for isotope selection)
      for (Sample sample : result) {
        if (!sample.getMethod().getSets().contains(params)) {
          sample.getMethod().getSets().add(params);
        }
      }


      LOGGER.info("Finished reading from " + path + ".");

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