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

package visualizer.charts;

import analysis.quant.Cal;
import core.SpTool3Main;
import dataModelNew.Sample;
import javafx.css.Size;
import math.units.Unit;
import math.units.enums.*;
import processing.options.EventParameter;
import processing.options.HistogramNormalization;

import javax.swing.text.View;
import java.util.ArrayList;
import java.util.List;

public interface AxisLabel {

  String getLabel();

  Unit getUnit();

  void setUnit(Unit unit);

  static AxisLabel getYUnit(HistogramNormalization histogramNormalization) {
    String label;
    Unit unit;
    switch (histogramNormalization) {
      case FREQUENCY:
        label = "Frequency";
        unit = ViewUnits.ABS_FREQUENCY;
        break;
      case RELATIVE_FREQUENCY:
        label = "Relative frequency";
        unit = ViewUnits.NONE;
        break;
      case SCALE_AREA_TO_1:
        label = "Probability density";
        unit = ViewUnits.NONE;
        break;
      default:
        label = "Frequency";
        unit = ViewUnits.NONE;
    }
    return new PlainLabel(label, unit);
  }

  static AxisLabel getUnit(EventParameter eventParameter) {
    return getUnit(eventParameter, IntensityUnit.JUST_CTS, new ArrayList<>());
  }

  static AxisLabel getUnit(EventParameter eventParameter, Unit quantUnit, List<Sample> samples) {
    String label = "Unknown";
    Unit unit = ViewUnits.NONE;

    // check if any of the samples suggest quant data
    boolean sampleIndicatesQuantData = false;
    // for loop only triggers if not empty
    for (Sample sample : samples) {
      sampleIndicatesQuantData = sample.getQuant()
          .getExperimentalConditions().getEventPar().equals(eventParameter);
      if (sampleIndicatesQuantData) break;
    }

    if ((IntensityUnit.CTS.equals(quantUnit) || IntensityUnit.JUST_CTS.equals(quantUnit)) || !EventParameter.canQuantify(eventParameter)
        || !sampleIndicatesQuantData) {
      switch (eventParameter) {
        case AREA:
          label = "Gross area";
          unit = IntensityUnit.JUST_CTS;
          break;
        case NET_AREA:
          label = "Net area";
          unit = IntensityUnit.JUST_CTS;
          break;
        case HEIGHT:
          label = "Gross height";
          unit = IntensityUnit.CTS;
          break;
        case NET_HEIGHT:
          label = "Net height";
          unit = IntensityUnit.CTS;
          break;
        case BACKGROUND_PER_NP:
          label = "BG per NP";
          unit = IntensityUnit.JUST_CTS;
          break;
        case DURATION:
          label = "Duration";
          unit = TimeUnit.MICROSECOND;
          break;
        case NO_OF_POINTS:
          label = "Points";
          unit = ViewUnits.NONE;
          break;
        case NO_OF_EVENTS:
          label = "Events";
          unit = ViewUnits.NP;
          break;
        case ASYMMETRY_FACTOR:
          label = "Symmetry";
          unit = ViewUnits.NONE;
          break;
        case START_INDEX:
          label = "Start";
          unit = ViewUnits.NONE;
          break;
        case END_INDEX:
          label = "End";
          unit = ViewUnits.NONE;
          break;
        case CENTER_TIME:
          label = "Center time";
          unit = TimeUnit.SECOND;
          break;
        default:
          label = "Area";
          unit = IntensityUnit.JUST_CTS;
      }
    } else {
      if (quantUnit instanceof SizeUnit) {
        label = "Particle size";
        unit = quantUnit;
      } else if (quantUnit instanceof MassUnit) {
        label = "Elemental mass";
        unit = quantUnit;
      } else if (quantUnit instanceof MolarUnit) {
        label = "Number of moles";
        unit = quantUnit;
      }
    }
    return new PlainLabel(label, unit);
  }

  static AxisLabel getUnit(EventParameter eventParameter, Unit quantUnit) {
    String label = "Unknown";
    Unit unit = ViewUnits.NONE;

    // check if any of the samples suggest quant data
    boolean sampleIndicatesQuantData = true;

    if ((IntensityUnit.CTS.equals(quantUnit) || IntensityUnit.JUST_CTS.equals(quantUnit)) || !EventParameter.canQuantify(eventParameter)
        || !sampleIndicatesQuantData) {
      switch (eventParameter) {
        case AREA:
          label = "Gross area";
          unit = IntensityUnit.JUST_CTS;
          break;
        case NET_AREA:
          label = "Net area";
          unit = IntensityUnit.JUST_CTS;
          break;
        case HEIGHT:
          label = "Gross height";
          unit = IntensityUnit.CTS;
          break;
        case NET_HEIGHT:
          label = "Net height";
          unit = IntensityUnit.CTS;
          break;
        case BACKGROUND_PER_NP:
          label = "BG per NP";
          unit = IntensityUnit.JUST_CTS;
          break;
        case DURATION:
          label = "Duration";
          unit = TimeUnit.MICROSECOND;
          break;
        case NO_OF_POINTS:
          label = "Points";
          unit = ViewUnits.NONE;
          break;
        case NO_OF_EVENTS:
          label = "Events";
          unit = ViewUnits.NP;
          break;
        case ASYMMETRY_FACTOR:
          label = "Symmetry";
          unit = ViewUnits.NONE;
          break;
        case START_INDEX:
          label = "Start";
          unit = ViewUnits.NONE;
          break;
        case END_INDEX:
          label = "End";
          unit = ViewUnits.NONE;
          break;
        case CENTER_TIME:
          label = "Center time";
          unit = TimeUnit.SECOND;
          break;
        default:
          label = "Area";
          unit = IntensityUnit.JUST_CTS;
      }
    } else {
      if (quantUnit instanceof SizeUnit) {
        label = "Particle size";
        unit = quantUnit;
      } else if (quantUnit instanceof MassUnit) {
        label = "Elemental mass";
        unit = quantUnit;
      } else if (quantUnit instanceof MolarUnit) {
        label = "Number of moles";
        unit = quantUnit;
      }
    }
    return new PlainLabel(label, unit);
  }

  public static class PlainLabel implements AxisLabel {

    private final String label;
    private Unit unit;

    public PlainLabel(String label, Unit unit) {
      this.label = label;
      this.unit = unit;
    }

    @Override
    public String getLabel() {
      return label;
    }

    @Override
    public Unit getUnit() {
      return unit;
    }

    @Override
    public void setUnit(Unit unit) {
      this.unit = unit;
    }
  }


}



