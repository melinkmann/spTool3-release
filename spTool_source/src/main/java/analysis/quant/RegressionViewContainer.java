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

import analysis.PopulationID;
import com.google.common.primitives.Doubles;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.mz.Element;
import gui.dialog.notification.NotificationFactory;
import gui.util.UiUtil;
import javafx.geometry.Orientation;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import math.regression.RegressionUtils;
import math.stat.MeasureOfLocation;
import math.units.Unit;
import math.units.enums.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.JFreeChart;
import processing.options.*;
import processing.parameterSets.impl.ExperimentalConditions;
import processing.parameterSets.impl.ExperimentalSubConditions;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;
import visualizer.charts.SpChartFactory;
import visualizer.charts.SpChartFactory.ChartComponent;
import visualizer.styles.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class RegressionViewContainer {

  private static final Logger LOGGER = LogManager.getLogger(RegressionViewContainer.class);

  private final SpCalibrationSet responseCalibrationSet;
  private final List<Sample> selSamples;
  private final List<Isotope> selIsotopes;
  private final List<PopulationID> selPops;
  private final HashMap<Isotope, List<Double>> xData;
  private final HashMap<Isotope, List<Double>> yData;

  private String xLbl = "";
  private String yLbl = "";
  private Unit xUnit = ViewUnits.NONE;
  private Unit yUnit = ViewUnits.NONE;

  private final Iterable<MarkerStyle> markerStyles = MarkerStyle.getScatterAwtIterator();
  private final Iterator<MarkerStyle> markers = markerStyles.iterator();

  private final HashMap<Isotope, Double> slopes;
  private final List<ChartComponent> graphComponents;
  private final List<ChartComponent> legendComponents;

  private final LinRegType linRegType;

  public RegressionViewContainer(SpCalibrationSet responseCalibrationSet, LinRegType linRegType) {
    this(responseCalibrationSet, linRegType, new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
  }

  public RegressionViewContainer(SpCalibrationSet responseCalibrationSet,
                                 LinRegType linRegType,
                                 List<Sample> selSamples,
                                 List<Isotope> selIsotopes,
                                 List<PopulationID> selPops) {

    this.responseCalibrationSet = responseCalibrationSet;
    this.linRegType = linRegType;
    this.selIsotopes = selIsotopes;
    this.selSamples = selSamples;
    this.selPops = selPops;

    this.xData = new HashMap<>();
    this.yData = new HashMap<>();

    this.slopes = new HashMap<>();
    this.graphComponents = new ArrayList<>();
    this.legendComponents = new ArrayList<>();
  }


  public void refreshTableAndGraph(BorderPane graphPane, TableView<ResponseTableRow> table) {
    // refresh graph (if empty, nothing happens)
    JFreeChart chart = SpChartFactory.createLineChart(graphComponents);

    SpChartFactory.ChartContainer chartContainer = SpChartFactory.bundleChartLegend(
        chart,
        legendComponents,
        500, 500,
        Orientation.HORIZONTAL);

    graphPane.setCenter(UiUtil.putOnAnchorWithoutInsets(chartContainer.combinedPane));

    // refresh table
    table.refresh();
  }

  public void sendSlopesToCalSetAndTable(TableView<ResponseTableRow> table,
                                         FxSpCalibrationSetTableModel tableModel,
                                         ToggleButton npResponseActiveToggle,
                                         ToggleButton ionicResponseActiveToggle,
                                         ToggleButton teActiveToggle,
                                         ToggleButton pnTeActiveToggle) {

    // All currently selected isotopes will be added
    List<Isotope> isotopes = new ArrayList<>(selIsotopes);
    responseCalibrationSet.populateWithIsotopes(isotopes);

    ///  PARTICLE RESPONSE
    if (npResponseActiveToggle.isSelected()) {
      for (Isotope isotope : isotopes) {
        if (responseCalibrationSet.hasNpResponse(isotope) && slopes.containsKey(isotope)) {
          double s = slopes.get(isotope);
          if (Doubles.isFinite(s)) {
            responseCalibrationSet.getOrCreateNpResponse(isotope).change(slopes.get(isotope),
                SensitivityUnit.CTS_PER_FEMTOGRAM);
          }
        }
      }
      responseCalibrationSet.deriveAerosolTE();

      /// IONIC RESPONSE
    } else if (ionicResponseActiveToggle.isSelected()) {
      for (Isotope isotope : isotopes) {
        if (responseCalibrationSet.hasIonicResponse(isotope) && slopes.containsKey(isotope)) {
          double s = slopes.get(isotope);
          if (Doubles.isFinite(s)) {
            responseCalibrationSet.getOrCreateIonicResponse(isotope).change(s,
                SensitivityUnit.CTS_PER_FEMTOGRAM);
          }
        }
      }
      responseCalibrationSet.deriveAerosolTE();

      /// AEROSOL TRANSPORT EFFICIENCY
    } else if (teActiveToggle.isSelected()) {
      for (Isotope isotope : isotopes) {
        if (responseCalibrationSet.hasAerosolTE(isotope) && slopes.containsKey(isotope)) {
          double s = 100 * slopes.get(isotope);
          if (Double.isFinite(s)) {
            responseCalibrationSet.getOrCreateAerosolTE(isotope).change(s);
            if (responseCalibrationSet.hasParticleNumberTE(isotope)) {
              responseCalibrationSet.getOrCreateParticleNumberTE(isotope).change(s);
            }
          }
        }
      }

      /// PARTICLE NUMBER TRANSPORT EFFICIENCY
    } else if (pnTeActiveToggle.isSelected()) {
      for (Isotope isotope : isotopes) {
        if (responseCalibrationSet.hasParticleNumberTE(isotope) && slopes.containsKey(isotope)) {
          double s = 100 * slopes.get(isotope);
          if (Double.isFinite(s)) {
            responseCalibrationSet.getOrCreateParticleNumberTE(isotope).change(s);
          }
        }
      }
    }

    /// Fill (and sort: apparently order changes here)
    tableModel.fill(responseCalibrationSet);
    table.refresh();
    // sort by isotope column
    TableColumn<ResponseTableRow, ?> retrievedColumn = null;
    for (TableColumn<ResponseTableRow, ?> column : table.getColumns()) {
      if ("Isotope".equals(column.getText())) {
        retrievedColumn = column;
      }
    }

    if (retrievedColumn != null) {
      retrievedColumn.setSortType(TableColumn.SortType.ASCENDING);
      // Add the column to the sort order
      table.getSortOrder().add(retrievedColumn);
      // Apply the sort
      table.sort();
    }
  }

  public void refreshSlopes(
      ToggleButton npResponseActiveToggle,
      ToggleButton ionicResponseActiveToggle,
      ToggleButton teActiveToggle,
      ToggleButton pnTeActiveToggle) {

    graphComponents.clear();
    legendComponents.clear();
    xData.clear();
    yData.clear();
    slopes.clear();
    for (Isotope selIsotope : selIsotopes) {
      xData.put(selIsotope, new ArrayList<>());
      yData.put(selIsotope, new ArrayList<>());
    }

    // sel pops must be empty in case ionic sample
    if (!selSamples.isEmpty() && !selIsotopes.isEmpty()) {
      if (npResponseActiveToggle.isSelected()) {
        computeResponseForNP();
      } else if (ionicResponseActiveToggle.isSelected()) {
        computeIonicResponse();
      } else if (teActiveToggle.isSelected()) {
        computeTE();
      } else if (pnTeActiveToggle.isSelected()) {
        computeParticleNumberTE();
      }

      // add to graphs
      for (Isotope isotope : xData.keySet()) {
        // don't add if data is zero, e.g, b/c diameter was never set or so
        boolean nonzero = !ArrUtils.nonzero(xData.get(isotope)).isEmpty()
            && !ArrUtils.nonzero(xData.get(isotope)).isEmpty();
        if (nonzero) {
          RegressionUtils.addPlotsToListAndRegression(
              graphComponents,
              legendComponents,
              isotope.getName(),
              ArrUtils.doubleListToArr(xData.get(isotope)),
              ArrUtils.doubleListToArr(yData.get(isotope)),
              xLbl, xUnit,
              yLbl, yUnit,
              SpTool3Main.getRunTime().getConfParams().getColor(isotope),
              markers.next(),
              isotope,
              slopes,
              linRegType
          );
        }
      }
    }
  }


  private void computeResponseForNP() {
    // get data
    if (!selPops.isEmpty()) {
      for (Sample sample : selSamples) {
        ExperimentalConditions mainQuant = sample.getQuant().getExperimentalConditions();
        HashMap<Element, ExperimentalSubConditions> subQuants = mainQuant.getElementSpecificQuantParams();

        // mean area, median area, mean height, ....
        QuantParam targetEventPar = mainQuant.getSampleWideNPSourceParam().getValue();
        // mean of raw, median of raw
        QuantParam rawDataPar = mainQuant.getSampleWideIonicSourceParam().getValue();

        // Sample or calibrator
        CalibratorRole calibratorRole = mainQuant.getCalibratorRole().getValue();
        // Particle or ionic
        SampleType sampleType = mainQuant.getSampleType().getValue();

        // check if this is a particle sample and a calibrator
        if (calibratorRole.equals(CalibratorRole.CALIBRATOR) && sampleType.equals(SampleType.PARTICLE)) {

          for (Isotope selIsotope : selIsotopes) {
            Element element = selIsotope.getElement();
            ExperimentalSubConditions subQuant = subQuants.get(element);
            if (subQuant != null) {

              // find correct population: it is possible that we have different IDs (e.g. gating on/off)
              List<PopulationID> selAndAvailablePops = selPops.stream()
                  .filter(id -> sample.hasPopulation(id, selIsotope))
                  .collect(Collectors.toList());

              if (!selAndAvailablePops.isEmpty()) {
                PopulationID pop = selAndAvailablePops.get(0);

                if (selAndAvailablePops.size() > 1) {
                  LOGGER.info(
                      "More than one population is selected! Quantification step was calculated using: {}",
                      pop.toString());
                  NotificationFactory.openAutocloseInfo("More than one population is selected! " +
                      "Quantification step was calculated using: " + pop);
                }

                // find x value
                ParticleQuantApproach quantApproach = subQuant.getNpQuantificationApproach().getValue();

                // case A: we are given the mass (including fraction)
                double elementalMass = subQuant.getNpElementMass().getValue();
                MassUnit elementalMassUnit = subQuant.getNpElementMassUnit();

                // case B: we are given a size
                double diameter = subQuant.getNpSphericalDiameter().getValue();
                SizeUnit diameterUnit = subQuant.getNpSphericalDiameterUnit();
                double massFrac = subQuant.getNpMassFraction().getValue();
                double density = subQuant.getNpDensity().getValue();
                DensityUnit densityUnit = subQuant.getNpDensityUnit();

                if (quantApproach.equals(ParticleQuantApproach.ESD)) {
                  // override the elemental mass
                  double umDiameter = diameterUnit.convert(diameter, SizeUnit.MICRO_METER);

                  // g -> fg (gain 1E15)
                  // m-3 -> µm-3 (· (1E-6)^3
                  double fgUm3Density = 1E15 * Math.pow(1E-6, 3) * densityUnit.convert(density,
                      DensityUnit.GRAM_PER_M3);

                  double massFg = Math.PI / 6 * Math.pow(umDiameter, 3) * fgUm3Density;
                  massFg = massFrac * massFg;

                  elementalMass = massFg;
                  elementalMassUnit = MassUnit.FEMTO_GRAM;
                }

                double xVal = elementalMass;

                // find y value
                EventParameter eventPar;
                MeasureOfLocation loc;
                if (targetEventPar.equals(QuantParam.NP_AREA_MEAN)) {
                  eventPar = EventParameter.NET_AREA;
                  loc = MeasureOfLocation.MEAN;
                } else if (targetEventPar.equals(QuantParam.NP_AREA_MEDIAN)) {
                  eventPar = EventParameter.NET_AREA;
                  loc = MeasureOfLocation.MEDIAN;
                } else if (targetEventPar.equals(QuantParam.NP_HEIGHT_MEAN)) {
                  eventPar = EventParameter.NET_HEIGHT;
                  loc = MeasureOfLocation.MEAN;
                } else {
                  eventPar = EventParameter.NET_HEIGHT;
                  loc = MeasureOfLocation.MEDIAN;
                }
                double[] npData = sample.getData(selIsotope, pop, EventType.NP, eventPar);
                double yVal = loc.calc(npData);

                xData.get(selIsotope).add(xVal);
                xUnit = elementalMassUnit;
                xLbl = "Elemental mass";

                yData.get(selIsotope).add(yVal);
                yUnit = (IntensityUnit.JUST_CTS);
                yLbl = "Intensity";
              }
            }
          }
        }
      }
    }
  }

  private void computeIonicResponse() {
    // get data
    for (Sample sample : selSamples) {
      ExperimentalConditions mainQuant = sample.getQuant().getExperimentalConditions();
      HashMap<Element, ExperimentalSubConditions> subQuants = mainQuant.getElementSpecificQuantParams();

      // mean area, median area, mean height, ....
      QuantParam targetEventPar = mainQuant.getSampleWideNPSourceParam().getValue();
      // mean of raw, median of raw
      QuantParam rawDataPar = mainQuant.getSampleWideIonicSourceParam().getValue();

      // Sample or calibrator
      CalibratorRole calibratorRole = mainQuant.getCalibratorRole().getValue();
      // Particle or ionic
      SampleType sampleType = mainQuant.getSampleType().getValue();

      // check if this is a particle sample and a calibrator
      if (calibratorRole.equals(CalibratorRole.CALIBRATOR) && sampleType.equals(SampleType.IONIC)) {
        double flow = sample.getQuant().getExperimentalConditions().getFlowRate(FlowUnit.LITRE_PER_SECOND);

        for (Isotope isotope : selIsotopes) {

          // get raw signal
          double rawCps;
          if (rawDataPar.equals(QuantParam.RAW_MEDIAN)) {
            rawCps = sample.getRawMedianCPS(isotope);
          } else {
            rawCps = sample.getRawMeanCPS(isotope);
          }

          // get x value of sample (flow rate)
          final ConcentrationUnit targetConcUnit = ConcentrationUnit.FMETOGRAM_PER_LITRE;
          Element traceEle = isotope.getElement();
          if (sample.getQuant().getExperimentalConditions().getElementSpecificQuantParams().containsKey(traceEle)) {
            double ionicConc = sample.getQuant().getExperimentalConditions().getElementSpecificQuantParams()
                .get(traceEle).getIonicConc(targetConcUnit);
            double massFlow = ionicConc * flow;

            xData.get(isotope).add(massFlow);
            xUnit = MassFlowUnit.FMETOGRAM_PER_SECOND;
            xLbl = "Elemental mass flow";

            yData.get(isotope).add(rawCps);
            yUnit = (IntensityUnit.CPS);
            yLbl = "Intensity";
          }
        }
      }
    }
  }

  private void computeTE() {
    // get data
    if (!selPops.isEmpty()) {
      for (Sample sample : selSamples) {
        ExperimentalConditions mainQuant = sample.getQuant().getExperimentalConditions();
        HashMap<Element, ExperimentalSubConditions> subQuants = mainQuant.getElementSpecificQuantParams();

        // mean area, median area, mean height, ....
        QuantParam targetEventPar = mainQuant.getSampleWideNPSourceParam().getValue();
        // mean of raw, median of raw
        QuantParam rawDataPar = mainQuant.getSampleWideIonicSourceParam().getValue();

        // Sample or calibrator
        CalibratorRole calibratorRole = mainQuant.getCalibratorRole().getValue();
        // Particle or ionic
        SampleType sampleType = mainQuant.getSampleType().getValue();

        // check if this is a particle sample and a calibrator
        if (calibratorRole.equals(CalibratorRole.CALIBRATOR) && sampleType.equals(SampleType.PARTICLE)) {
          double flowLitrePerSec =
              sample.getQuant().getExperimentalConditions().getFlowRate(FlowUnit.LITRE_PER_SECOND);

          for (Isotope selIsotope : selIsotopes) {
            Element element = selIsotope.getElement();
            ExperimentalSubConditions subQuant = subQuants.get(element);

            // Make sure the element has data assigned and that we actually have NP conc
            if (subQuant != null) {

              // find correct population: it is possible that we have different IDs (e.g. gating on/off)
              List<PopulationID> selAndAvailablePops = selPops.stream()
                  .filter(id -> sample.hasPopulation(id, selIsotope))
                  .collect(Collectors.toList());

              if (!selAndAvailablePops.isEmpty()) {
                PopulationID pop = selAndAvailablePops.get(0);

                if (selAndAvailablePops.size() > 1) {
                  LOGGER.info(
                      "More than one population is selected! Quantification step was calculated using: {}",
                      pop.toString());
                  NotificationFactory.openAutocloseInfo("More than one population is selected! " +
                      "Quantification step was calculated using: " + pop);
                }

                // find x value
                ConcentrationUnit targetConcUnit = ConcentrationUnit.NP_PER_LITRE;
                double npNumConc = subQuant.getNpConcentration().getValue();
                ConcentrationUnit npNumConcUnit = subQuant.getNpConcentrationUnit();
                double npNumConcPerLitre = npNumConcUnit.convert(npNumConc, targetConcUnit);
                double npRatePerSec = npNumConcPerLitre * flowLitrePerSec;
                double xVal = npRatePerSec;

                // find y val
                double npPerSecDetected = sample.getEventRate(selIsotope, pop);

                xData.get(selIsotope).add(xVal);
                xUnit = ViewUnits.NP_PER_SECOND;
                xLbl = "Delivered NP rate";

                yData.get(selIsotope).add(npPerSecDetected);
                yUnit = ViewUnits.NP_PER_SECOND;
                yLbl = "Detected NP rate";
              }
            }
          }
        }
      }
    }

  }


  private void computeParticleNumberTE() {
    // get data
    if (!selPops.isEmpty()) {
      for (Sample sample : selSamples) {
        ExperimentalConditions mainQuant = sample.getQuant().getExperimentalConditions();
        HashMap<Element, ExperimentalSubConditions> subQuants = mainQuant.getElementSpecificQuantParams();

        // mean area, median area, mean height, ....
        QuantParam targetEventPar = mainQuant.getSampleWideNPSourceParam().getValue();
        // mean of raw, median of raw
        QuantParam rawDataPar = mainQuant.getSampleWideIonicSourceParam().getValue();

        // Sample or calibrator
        CalibratorRole calibratorRole = mainQuant.getCalibratorRole().getValue();
        // Particle or ionic
        SampleType sampleType = mainQuant.getSampleType().getValue();

        // check if this is a particle sample and a calibrator
        if (calibratorRole.equals(CalibratorRole.CALIBRATOR) && sampleType.equals(SampleType.PARTICLE)) {
          double flowLitrePerSec =
              sample.getQuant().getExperimentalConditions().getFlowRate(FlowUnit.LITRE_PER_SECOND);

          for (Isotope selIsotope : selIsotopes) {
            Element element = selIsotope.getElement();
            ExperimentalSubConditions subQuant = subQuants.get(element);

            // Make sure the element has data assigned and that we actually have NP conc
            if (subQuant != null) {

              // find correct population: it is possible that we have different IDs (e.g. gating on/off)
              List<PopulationID> selAndAvailablePops = selPops.stream()
                  .filter(id -> sample.hasPopulation(id, selIsotope))
                  .collect(Collectors.toList());

              if (!selAndAvailablePops.isEmpty()) {
                PopulationID pop = selAndAvailablePops.get(0);

                if (selAndAvailablePops.size() > 1) {
                  LOGGER.info(
                      "More than one population is selected! Quantification step was calculated using: {}",
                      pop.toString());
                  NotificationFactory.openAutocloseInfo("More than one population is selected! " +
                      "Quantification step was calculated using: " + pop);
                }

                // find x value
                ConcentrationUnit targetConcUnit = ConcentrationUnit.NP_PER_LITRE;
                double npNumConc = subQuant.getNpConcentration().getValue();
                ConcentrationUnit npNumConcUnit = subQuant.getNpConcentrationUnit();
                double npNumConcPerLitre = npNumConcUnit.convert(npNumConc, targetConcUnit);
                double npRatePerSec = npNumConcPerLitre * flowLitrePerSec;
                double xVal = npRatePerSec;

                // find y val
                double npPerSecDetected = sample.getEventRate(selIsotope, pop);

                xData.get(selIsotope).add(xVal);
                xUnit = ViewUnits.NP_PER_SECOND;
                xLbl = "Delivered NP rate";

                yData.get(selIsotope).add(npPerSecDetected);
                yUnit = ViewUnits.NP_PER_SECOND;
                yLbl = "Detected NP rate";
              }
            }
          }
        }
      }
    }
  }


}