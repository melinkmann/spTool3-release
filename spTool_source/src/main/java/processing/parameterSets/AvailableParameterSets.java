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

package processing.parameterSets;

import core.SpTool3Main;
import gui.util.UiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import processing.parameterSets.impl.*;
import visualizer.styles.UiColors;

public enum AvailableParameterSets {

  /**
   * Remember to also register a new ParameterSet in the dictionary!
   *
   * @link {@link XmlInstanceDictionary}
   */

  CONFIGS {
    @Override
    public String toString() {
      return "Configuration";
    }

    @Override
    public ParamSet get() {
      return new ConfParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GRAY.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  EXPORT_CONFIGS {
    @Override
    public String toString() {
      return "Export configuration";
    }

    @Override
    public ParamSet get() {
      return new ExporterParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GRAY.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },


  EMPTY {
    @Override
    public String toString() {
      return "Empty sub method";
    }

    @Override
    public ParamSet get() {
      return new EmptyParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GRAY.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  QUANT {
    @Override
    public String toString() {
      return "Quantification sub method";
    }

    @Override
    public ParamSet get() {
      return new ExperimentalConditions();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },
  QUANT_SUB {
    @Override
    public String toString() {
      return "Quantification sub-sub method";
    }

    @Override
    public ParamSet get() {
      return new ExperimentalSubConditions();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  CSV_READER {
    @Override
    public String toString() {
      return "CSV import";
    }

    @Override
    public ParamSet get() {
      return new CsvInterpreterParams();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  SPICPMS_MC_SIMUL {
    @Override
    public String toString() {
      return "Data generator general parameters";
    }

    @Override
    public ParamSet get() {
      return new MCSimGeneralParams();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  SPICPMS_MC_SIMUL_NP_POPULATION {
    @Override
    public String toString() {
      return "Synthetic particle population";
    }

    @Override
    public ParamSet get() {
      return new MCSimParticleParams();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

//  SPICPMS_MC_TESTER {
//    @Override
//    public String toString() {
//      return "Peak shape tester";
//    }
//
//    @Override
//    public ParamSet get() {
//      return new PeakShapeTesterParams();
//    }
//
//    @Override
//    public Color getColor() {
//      return UiColors.BLACK.getFX();
//    }
//
//    @Override
//    public Node getShape() {
//      return UiUtil.getCircleForListView(getColor());
//    }
//  },


  BASELINE {
    @Override
    public String toString() {
      return "Baseline";
    }

    @Override
    public ParamSet get() {
      return new BaselineParams();
    }

    @Override
    public Color getColor() {
      return UiColors.BASELINE_BLUE.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  NORMAL_EVENT_SEARCH {
    @Override
    public String toString() {
      return "Event search";
    }

    @Override
    public ParamSet get() {
      return new NormalSearchParams();
    }

    @Override
    public Color getColor() {
      return UiColors.SEARCH_RED.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  ALIGNMENT {
    @Override
    public String toString() {
      return "Align isotopes";
    }

    @Override
    public ParamSet get() {
      return new AlignerParams();
    }

    @Override
    public Color getColor() {
      return UiColors.SEARCH_RED.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getCircleForListView(getColor());
    }
  },

  DT_GROUPING {
    @Override
    public String toString() {
      return "Group dwell time";
    }

    @Override
    public ParamSet get() {
      return new DTGroupParams();
    }

    @Override
    public Color getColor() {
      return UiColors.BLACK.getFX();
    }


    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },

  TIME_ROI {
    @Override
    public String toString() {
      return "Cut time region";
    }

    @Override
    public ParamSet get() {
      return new TimeRoiParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GRAY.getFX();
    }


    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  },


  GATE_FILTER {
    @Override
    public String toString() {
      return "Gate filtering";
    }

    @Override
    public ParamSet get() {
      return new GatingParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GATING_MUSTARD.getFX();
    }


    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }

  },

  GENERAL_FILTER {
    @Override
    public String toString() {
      return "General filtering";
    }

    @Override
    public ParamSet get() {
      return new FilterParams();
    }

    @Override
    public Color getColor() {
      return UiColors.FILTERING_MUSTARD.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }

  },


   SIGNIFICANCE_TEST {
    @Override
    public String toString() {
      return "Significance test";
    }

    @Override
    public ParamSet get() {
      return new SignificanceTestParams();
    }

    @Override
    public Color getColor() {
      return UiColors.GRAY.getFX();
    }

    @Override
    public Node getShape() {
      return UiUtil.getRectangleForListView(getColor());
    }
  };

  public abstract ParamSet get();

  public abstract Color getColor();

  public abstract Node getShape();


  public static List<AvailableParameterSets> getAllowedOptions() {
    List<AvailableParameterSets> sets = new ArrayList<>();

    if (SpTool3Main.getANALYZER()) {
      sets.add(CSV_READER);
      sets.add(SPICPMS_MC_SIMUL);
      sets.add(SPICPMS_MC_SIMUL_NP_POPULATION);
      // sets.add(SPICPMS_MC_TESTER);
      sets.add(TIME_ROI);
      // sets.add(DT_GROUPING);
      sets.add(BASELINE);
      sets.add(NORMAL_EVENT_SEARCH);
      // sets.add(ALIGNMENT);
      sets.add(GATE_FILTER);
      sets.add(GENERAL_FILTER);
    } else {
      sets.add(SPICPMS_MC_SIMUL);
      sets.add(SPICPMS_MC_SIMUL_NP_POPULATION);
    }

    return sets;
  }

  public static List<FxParamSet> getAllowedInstances() {
    return get(getAllowedOptions());
  }

  // Helpers
  public static List<FxParamSet> get(List<AvailableParameterSets> sets) {
    return sets.stream().map(AvailableParameterSets::get)
        .map(ParamSet::getObservableInstance).collect(Collectors.toList());
  }

  public static List<AvailableParameterSets> getOptionAsList(AvailableParameterSets set) {
    return Collections.singletonList(set);
  }
}
