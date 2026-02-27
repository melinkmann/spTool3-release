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

package analysis;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

import processing.options.*;
import processing.parameterSets.impl.FilterParams;
import processing.parameters.Parameter;
import util.NF;
import util.SnF;

import javax.annotation.Nullable;

import static visualizer.ResultsTable.EMPTY_CELL;

public class PopParSummary implements Serializable {

  static final long serialVersionUID = 1L; //assign a long value

  private List<PopParSteps> allSteps;


  public PopParSummary() {
    this.allSteps = new ArrayList<>();
  }

  // Specific constructors
  public PopParSummary(FilterParams inputParams) {
    this.allSteps = new ArrayList<>();
    FilterOptions option = inputParams.getFilterOption().getValue();
    add("TYPE", inputParams.getFilterOption(), option.toString());

    if (option.equals(FilterOptions.OVERLAP)) {
      add("z-Intensity",
          inputParams.getzIntensity(),
          SnF.doubleToString(inputParams.getzIntensity().getValue(), NF.D1C3));
      add("z-Segment",
          inputParams.getzSegment(),
          SnF.doubleToString(inputParams.getzSegment().getValue(), NF.D1C3));
      add("MoAvPeriod",
          inputParams.getMoavPeriod(),
          SnF.doubleToString(inputParams.getMoavPeriod().getValue(), NF.D1C0));
    } else if (option.equals(FilterOptions.ROI_REGION)) {
      add("ROI", inputParams.roiCategory, inputParams.roiCategory.getValue().getShortLabel());
      add("Par", inputParams.eventParameter, inputParams.eventParameter.getValue().toString());
      add("unit", inputParams.unitConversion, inputParams.unitConversion.getValue().toString());
      add("InEx", inputParams.roiType, inputParams.roiType.getValue().toString());

      if (inputParams.roiCategory.getValue().equals(RoiCategory.IQR)
          || inputParams.roiCategory.getValue().equals(RoiCategory.MAD)) {
        add("fun", inputParams.mathConversion, inputParams.mathConversion.getValue().toString());
        add("f", inputParams.sigFactor, inputParams.sigFactor.getValueAsString());

      } else if (inputParams.roiCategory.getValue().equals(RoiCategory.PERCENTILES)
          || inputParams.roiCategory.getValue().equals(RoiCategory.ABSOLUTE_VALUES)) {
        add("fun", inputParams.mathConversion, inputParams.mathConversion.getValue().toString());
        add("start", inputParams.start, inputParams.start.getValueAsString());
        add("end", inputParams.end, inputParams.end.getValueAsString());

      } else if (inputParams.roiCategory.getValue().equals(RoiCategory.OTSU)) {
        add("fun", inputParams.mathConversion, inputParams.mathConversion.getValue().toString());
        add("reg", inputParams.getOtsuRegion(),
            inputParams.getOtsuRegion().getValueAsString());
        add("bin", inputParams.getBinWidthEstimator(),
            inputParams.getBinWidthEstimator().getValueAsString());
        if (inputParams.getBinWidthEstimator().getValue().equals(BinWidthEstimator.CUSTOM)) {
          add("bWidth", inputParams.getCustomBinWidth(),
              SnF.doubleToString(inputParams.getCustomBinWidth().getValue(), NF.D1C3));
        }
      } else if (inputParams.roiCategory.getValue().equals(RoiCategory.CHANGE_POINT)) {
        add("reg", inputParams.getOtsuRegion(),
            inputParams.getOtsuRegion().getValueAsString());
        add("bin", inputParams.getBinWidthEstimator(),
            inputParams.getBinWidthEstimator().getValueAsString());
        if (inputParams.getBinWidthEstimator().getValue().equals(BinWidthEstimator.CUSTOM)) {
          add("bWidth", inputParams.getCustomBinWidth(),
              SnF.doubleToString(inputParams.getCustomBinWidth().getValue(), NF.D1C3));
        }
        add("z", inputParams.getChangePointZ(),
            inputParams.getChangePointZ().getValueAsString());
      }
    }
  }


  // For the deep copies
  public PopParSummary(List<PopParSteps> steps) {
    this.allSteps = new ArrayList<>(steps);
  }

  public PopParSummary copy() {
    return new PopParSummary(allSteps);
  }

  public void mergeInto(PopParSummary summary) {
    allSteps.addAll(summary.allSteps);
  }

  public void add(String label, Parameter<?> parameter, String stringValue) {
    this.allSteps.add(new PopParSteps(label, stringValue, parameter));
  }

  public void add(String label, String stringValue) {
    this.allSteps.add(new PopParSteps(label, stringValue));
  }

  public String translate() {
    String result = EMPTY_CELL;
    if (!allSteps.isEmpty()) {
      StringJoiner joiner = new StringJoiner("_", "Inputs: ", "");
      for (PopParSteps step : allSteps) {
        joiner.add("(" + step.getLabel() + "=" + step.getStringValue() + ")");
      }
      result = joiner.toString();
    }
    return result;
  }

  //  Backwards compatible
  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    if (this.allSteps == null) {
      this.allSteps = new ArrayList<>();
    }

  }

  static class PopParSteps implements Serializable {

    static final long serialVersionUID = 1L; //assign a long value

    private final String label;
    private final String stringValue;
    @Nullable
    private final Parameter<?> par;

    public PopParSteps(String label, String stringValue, @Nullable Parameter<?> par) {
      this.label = label;
      this.stringValue = stringValue;
      this.par = par;
    }

    public PopParSteps(String label, String stringValue) {
      this.label = label;
      this.stringValue = stringValue;
      this.par = null;
    }

    public String getLabel() {
      return label;
    }

    public String getStringValue() {
      return stringValue;
    }

    @Nullable
    public Parameter<?> getPar() {
      return par;
    }
  }

}
