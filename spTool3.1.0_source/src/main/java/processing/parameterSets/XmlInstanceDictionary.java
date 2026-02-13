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

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.impl.*;
import processing.parameterSets.uiParams.*;

public abstract class XmlInstanceDictionary {

  private static final Logger LOGGER = LogManager.getLogger(XmlInstanceDictionary.class);

  @Nullable
  public static ParamSet lookup(String type) {
    ParamSet set = switch (type) {
      case BaselineParams.XML_ELEMENT_TAG -> new BaselineParams();
      case GatingParams.XML_ELEMENT_TAG -> new GatingParams();

      case FilterParams.XML_ELEMENT_TAG -> new FilterParams();
      case TimeRoiParams.XML_ELEMENT_TAG -> new TimeRoiParams();

      case NormalSearchParams.XML_ELEMENT_TAG -> new NormalSearchParams();
      case AlignerParams.XML_ELEMENT_TAG -> new AlignerParams();

      case CsvInterpreterParams.XML_ELEMENT_TAG -> new CsvInterpreterParams();
      case MCSimGeneralParams.XML_ELEMENT_TAG -> new MCSimGeneralParams();
      case MCSimParticleParams.XML_ELEMENT_TAG -> new MCSimParticleParams();

      case DTGroupParams.XML_ELEMENT_TAG -> new DTGroupParams();

      case SignificanceTestParams.XML_ELEMENT_TAG -> new SignificanceTestParams();

      case ConfParams.XML_ELEMENT_TAG -> new ConfParams();
      case ExporterParams.XML_ELEMENT_TAG -> new ExporterParams();
      case EmptyParams.XML_ELEMENT_TAG -> new EmptyParams();

      case AverageViewerParameters.XML_ELEMENT_TAG -> new AverageViewerParameters();
      case IclPeakParameters.XML_ELEMENT_TAG -> new IclPeakParameters();
      case MonteCarloHistoParameters.XML_ELEMENT_TAG -> new MonteCarloHistoParameters();
      case BoxPlotParameters.XML_ELEMENT_TAG -> new BoxPlotParameters();

      case MonteCarloRawDataParameters.XML_ELEMENT_TAG -> new MonteCarloRawDataParameters();
      case MonteCarloScatterPlotParameters.XML_ELEMENT_TAG -> new MonteCarloScatterPlotParameters();
      case SingleEventViewerParameters.XML_ELEMENT_TAG -> new SingleEventViewerParameters();
      case CompareHistoParams.XML_ELEMENT_TAG -> new CompareHistoParams();
      case ExperimentalConditions.XML_ELEMENT_TAG -> new ExperimentalConditions();

      case QuantViewerParams.XML_ELEMENT_TAG -> new QuantViewerParams();

      case TableParameters.XML_ELEMENT_TAG -> new TableParameters();

      case UiLayoutParameters.XML_ELEMENT_TAG -> new UiLayoutParameters();

      default -> null;
    };
    if (set == null) {
      LOGGER.error("Cannot find parameter set for " + type + ". "
          + "The instance must be added to the switch statement!");
    }
    return set;
  }

}
