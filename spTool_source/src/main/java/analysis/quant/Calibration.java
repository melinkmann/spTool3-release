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

package analysis.quant;

import processing.options.CalibrationStrategy;
import processing.parameterSets.impl.ExperimentalConditions;
import sandbox.montecarlo.Isotope;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;

public class Calibration implements Cal, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private ExperimentalConditions experimentalConditions;

  private SpCalibrationSet spCalibrationSet;

  public Calibration() {
    this.experimentalConditions = new ExperimentalConditions();
    this.spCalibrationSet = new SpCalibrationSet();
  }

  public Calibration(String sampleLabel) {
    this.experimentalConditions = new ExperimentalConditions(sampleLabel);
    this.spCalibrationSet = new SpCalibrationSet();
  }

  // Copy: copy is done in the calling method
  public Calibration(SpCalibrationSet spCalibrationSet,
                     ExperimentalConditions experimentalConditions) {
    this.spCalibrationSet = spCalibrationSet;
    this.experimentalConditions = experimentalConditions;
  }

  @Override
  public Cal copy() {
    return new Calibration(
        spCalibrationSet.copy(),
        (ExperimentalConditions) experimentalConditions.getCopyWithPreviousDateFileAndID()
    );
  }

  @Override
  public ExperimentalConditions getExperimentalConditions() {
    return experimentalConditions;
  }

  @Override
  public void setCalibrationStrategy(CalibrationStrategy calibrationStrategy) {
    this.spCalibrationSet.setCalibrationStrategy(calibrationStrategy);
  }

  @Override
  public CalibrationStrategy getCalibrationStrategy() {
    return spCalibrationSet.getCalibrationStrategy();
  }

  @Override
  public SpCalibrationSet getResponses() {
    return spCalibrationSet;
  }

  @Override
  public void setResponses(SpCalibrationSet responses) {
    this.spCalibrationSet = responses;
  }

  @Override
  public List<Isotope> listIsotopes() {
    return spCalibrationSet.listIsotopes();
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    if (experimentalConditions == null) {
      this.experimentalConditions = new ExperimentalConditions();
    }

    if (spCalibrationSet == null) {
      this.spCalibrationSet = new SpCalibrationSet();
    }
  }


}
