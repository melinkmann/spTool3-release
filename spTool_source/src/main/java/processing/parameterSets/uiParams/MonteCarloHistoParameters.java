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

package processing.parameterSets.uiParams;

import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.Serial;
import java.nio.file.Path;

import math.stat.MeasureOfLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.BackgroundHighlight;
import processing.options.BinWidthEstimator;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.HistogramNormalization;
import processing.options.MathMod;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.MonteCarloHistoViewer;
import processing.parameters.*;
import util.NF;
import util.SnF;

/**
 * This should be a viewer class. It has 3 main principles:
 * <p>
 * 1) All options are parameters hat can be stored, i.e., this class behaves like a parameter set.
 * <p>
 * 2) There has to be UI instance, that listens to changes in the parameters and refreshes the
 * viewer.
 * <p>
 * 3) The FX class contains a viewer instance which the MainWindowController accesses when the
 * respective pane is shown.
 * <p>
 * <p>
 * --> Do the same also for WHICH viewers are shown in the UI :)
 */


/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class MonteCarloHistoParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "MonteCarloHistoViewer";

  private final Parameter<Boolean> showKernelDensity;
  private final Parameter<Boolean> showHistogram;
  private final Parameter<Boolean> showCumulative;

  private final Parameter<HistogramNormalization> histogramNormalization;
  private final Parameter<BinWidthEstimator> binWidthEstimator;
  private final Parameter<Double> customBinWidth;
  private final Parameter<String> estimatedBinWidth;
  private final Parameter<Double> customKernelBandwidth;

  private final Parameter<EventType> eventType;
  private final Parameter<BackgroundHighlight> backgroundHighlightOption;
  private final Parameter<Boolean> jitterBackground;
  private final Parameter<Integer> numberOfBackgroundEvents;

  private final Parameter<MathMod> mathModification;

  private final Parameter<EventParameter> eventParameter;

  private final Parameter<Boolean> showMaxThrLOD;

  private final Parameter<Boolean> showRuler;
  private final Parameter<MeasureOfLocation> rulerPosition;
  private final Parameter<Double> customRulerPosition;

  private final Parameter<Boolean> limitAxes;
  private final Parameter<Double> upperYLimit;
  private final Parameter<Double> upperXLimitAbsolut;
  private final Parameter<Boolean> usePercentileForX;
  private final Parameter<Double> upperXLimitPercentile;
  private final Parameter<Double> lowerXLimit;

  private final Parameter<Double> colorAlpha;

  public MonteCarloHistoParameters() {
    super("Monte carlo histogram viewer parameters", XML_ELEMENT_TAG);

    showHistogram = new BooleanParameter(
        "Histogram",
        "Show",
        "Plot histograms",
        true,
        false,
        "showHistogram"
    );

    showCumulative = new BooleanParameter(
        "Cumulative",
        "Show",
        "Plot cumulative probability function (CDF)",
        false,
        true,
        "showCumulative"
    );

    showKernelDensity = new BooleanParameter(
        "Kernel",
        "Show",
        "Plot kernel density estimates",
        false,
        true,
        "showKernelDensity"
    );

    this.histogramNormalization = new ComboEnumParameter<>(
        "Histogram",
        "Histogram y-axis scaling",
        HistogramNormalization.FREQUENCY,
        HistogramNormalization.values(),
        HistogramNormalization.class,
        false,
        "histogramNormalization"
    );

    binWidthEstimator = new ComboEnumParameter<>(
        "Binning",
        "Choose the model for calculating the histogram bin width",
        BinWidthEstimator.SHIMAZAKI_AND_SHINOMOTO,
        BinWidthEstimator.values(),
        BinWidthEstimator.class,
        false,
        "binWidthEstimator"
    );

    estimatedBinWidth = new FeedbackStringParameter(
        "Result",
        "Result of the bin width estimation",
        SnF.doubleToString(0.00, NF.D1C3),
        TextFormatterOption.ALL_PASS,
        true,
        "estimatedBinWidth"
    );

    this.customBinWidth = new DoubleParameter(
        "Bin width",
        "Choose the bin width",
        0.05d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        false,
        "customBinWidth");

    this.customKernelBandwidth = new DoubleParameter(
        "Bandwidth f",
        """
            Choose the bandwidth h for the kernel density estimation.
            The default estimator for h is Silverman's Rule of Thumb.
            Use this parameter f to multiply (f·h) for adjustment""",
        1d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "customKernelBandwidth");

    eventType = new ComboEnumParameter<>(
        "NP | BG",
        "Show particles or background",
        EventType.NP,
        //new EventType[]{EventType.NP},
        EventType.values(),
        EventType.class,
        false,
        "eventType"
    );

    this.backgroundHighlightOption = new ComboEnumParameter<>(
        "BG highlight",
        "Show particles or background",
        BackgroundHighlight.DARKER,
        BackgroundHighlight.values(),
        BackgroundHighlight.class,
        true,
        "backgroundHighlightOption"
    );

    mathModification = new ComboEnumParameter<>(
        "Math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathModification"
    );

    eventParameter = new ComboEnumParameter<>(
        "Data",
        "Choose which data shall be shown",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter"
    );

    this.jitterBackground = new BooleanParameter(
        "Reduce BG",
        "Limit",
        "Reduce the number of background data points, i.e., n(BG) to be shown",
        true,
        false,
        "jitterBackground"
    );

    numberOfBackgroundEvents = new IntegerParameter(
        "# of BG",
        "Number of background (BG) events to be shown",
        5000,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "numberOfBackgroundEvents");

    this.showMaxThrLOD = new BooleanParameter(
        "Threshold",
        "Show",
        "Include the highest detection threshold with respect to peak height",
        false,
        true,
        "showMaxThrLOD"
    );

    showRuler = new BooleanParameter(
        "Ruler",
        "Show",
        "Show a size-based ruler to indicate where the volume-based (cubic) multiples are situated",
        false,
        true,
        "showRuler"
    );

    rulerPosition = new ComboEnumParameter<>(
        "Position",
        "Choose which data shall be shown",
        MeasureOfLocation.CUSTOM,
        MeasureOfLocation.values(),
        MeasureOfLocation.class,
        true,
        "rulerPosition"
    );

    customRulerPosition = new DoubleParameter(
        "Ruler value",
        "Choose the bin width",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "customRulerPosition");

    limitAxes = new BooleanParameter(
        "Axis limits",
        "Set limits",
        "Set custom limits for the x and y axes of the plot",
        false,
        true,
        "limitAxes"
    );

    upperYLimit = new DoubleParameter(
        "Upper y",
        "Choose the upper y-axis limit as a absolute value",
        100d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperYLimit");

    upperXLimitPercentile = new DoubleParameter(
        "Upper x [%]",
        "Choose the upper x-axis limit as a percentile",
        95d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "upperXLimitPercentile");

    lowerXLimit = new DoubleParameter(
        "Lower x",
        "Choose the lower x-axis limit as an absolute value",
        0.00d,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "lowerXLimit");

    usePercentileForX = new BooleanParameter(
        "Upper X",
        "As percentile",
        "Give upper X axis limit as percentile",
        false,
        true,
        "usePercentileForX"
    );

    upperXLimitAbsolut = new DoubleParameter(
        "Upper x",
        "Choose the upper x-axis limit as an absolute value",
        0.00d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperXLimitAbsolut");

    this.colorAlpha = new DoubleParameter(
        "Alpha",
        "Set the alpha value (transparency) for the dots",
        0.9d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "colorAlpha");

    organize();
  }

  public MonteCarloHistoParameters(MonteCarloHistoParameters histParams) {
    super(histParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(histParams.getCommentParameter());

    this.showKernelDensity = histParams.showKernelDensity.copyWithoutChildren();
    this.showHistogram = histParams.showHistogram.copyWithoutChildren();
    this.showCumulative = histParams.showCumulative.copyWithoutChildren();

    this.histogramNormalization = histParams.histogramNormalization.copyWithoutChildren();
    this.binWidthEstimator = histParams.binWidthEstimator.copyWithoutChildren();
    this.customBinWidth = histParams.customBinWidth.copyWithoutChildren();
    this.customKernelBandwidth = histParams.customKernelBandwidth.copyWithoutChildren();
    this.estimatedBinWidth = histParams.estimatedBinWidth.copyWithoutChildren();
    this.mathModification = histParams.mathModification.copyWithoutChildren();
    this.eventParameter = histParams.eventParameter.copyWithoutChildren();
    this.eventType = histParams.eventType.copyWithoutChildren();
    this.backgroundHighlightOption = histParams.backgroundHighlightOption.copyWithoutChildren();

    this.jitterBackground = histParams.jitterBackground.copyWithoutChildren();
    this.numberOfBackgroundEvents = histParams.numberOfBackgroundEvents.copyWithoutChildren();

    this.showMaxThrLOD = histParams.showMaxThrLOD.copyWithoutChildren();

    this.showRuler = histParams.showRuler.copyWithoutChildren();
    this.rulerPosition = histParams.rulerPosition.copyWithoutChildren();
    this.customRulerPosition = histParams.customRulerPosition.copyWithoutChildren();

    this.limitAxes = histParams.limitAxes.copyWithoutChildren();
    this.upperYLimit = histParams.upperYLimit.copyWithoutChildren();
    this.upperXLimitPercentile = histParams.upperXLimitPercentile.copyWithoutChildren();
    this.lowerXLimit = histParams.lowerXLimit.copyWithoutChildren();
    this.usePercentileForX = histParams.usePercentileForX.copyWithoutChildren();
    this.upperXLimitAbsolut = histParams.upperXLimitAbsolut.copyWithoutChildren();

    this.colorAlpha = histParams.colorAlpha.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new MonteCarloHistoParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MonteCarloHistoParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MonteCarloHistoParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(

        eventParameter,
        mathModification,
        eventType,

        showHistogram,
        showKernelDensity,
        showCumulative,

        showMaxThrLOD,

        showRuler,
        limitAxes,

        colorAlpha
    );

    eventType.addConditionalChild(EventType.BG_NP, jitterBackground, backgroundHighlightOption);

    showHistogram.addConditionalChild(true, binWidthEstimator, histogramNormalization);
    showKernelDensity.addConditionalChild(true, customKernelBandwidth);

    jitterBackground.addConditionalChild(true, numberOfBackgroundEvents);
    showRuler.addConditionalChild(true, rulerPosition);
    rulerPosition.addConditionalChild(MeasureOfLocation.CUSTOM, customRulerPosition);

    limitAxes.addConditionalChild(true, upperYLimit, lowerXLimit, usePercentileForX);
    usePercentileForX.addConditionalChild(true, upperXLimitPercentile);
    usePercentileForX.addConditionalChild(false, upperXLimitAbsolut);

    binWidthEstimator.addConditionalChild(BinWidthEstimator.CUSTOM, customBinWidth);
    binWidthEstimator.addConditionalChild(BinWidthEstimator.getAllNonCustom(), estimatedBinWidth);

    /* This does not work yet. I think, this would take too much effort.
    binWidthEstimator.setDecoration(new ButtonDecoration<>(
        "Set the bin width according to the estimator",
        "/img/calculator.png",
        () -> {
          if (viewer != null) {
            double width = viewer.getBinWidth();
            binWidth.setValue(width);
          }
        }));
     */

    eventParameter.setDecoration(new ImageDecoration<>("/img/sumarea.png"));
    mathModification.setDecoration(new ImageDecoration<>("/img/linlog.png"));
    eventType.setDecoration(new ImageDecoration<>("/img/npbg.png"));
  }


  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "showHistogram" -> showHistogram;
          case "showKernelDensity" -> showKernelDensity;
          case "showCumulative" -> showCumulative;
          case "histogramNormalization" -> histogramNormalization;
          case "binWidthEstimator" -> binWidthEstimator;
          case "estimatedBinWidth" -> estimatedBinWidth;
          case "customBinWidth" -> customBinWidth;
          case "customKernelBandwidth" -> customKernelBandwidth;
          case "eventType" -> eventType;
          case "backgroundHighlightOption" -> backgroundHighlightOption;
          case "mathModification" -> mathModification;
          case "eventParameter" -> eventParameter;
          case "reduceBackground" -> jitterBackground;
          case "numberOfBackgroundEvents" -> numberOfBackgroundEvents;

          case "showMaxThrLOD" -> showMaxThrLOD;

          case "showRuler" -> showRuler;
          case "rulerPosition" -> rulerPosition;
          case "customRulerPosition" -> customRulerPosition;

          case "limitAxes" -> limitAxes;
          case "upperYLimit" -> upperYLimit;
          case "upperXLimitPercentile" -> upperXLimitPercentile;
          case "lowerXLimit" -> lowerXLimit;

          case "usePercentileForX" -> usePercentileForX;
          case "upperXLimitAbsolut" -> upperXLimitAbsolut;

          case "colorAlpha" -> colorAlpha;

          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        }
      }
    }
  }

  @Override
  public void executeSaveAs(Path file) {
    super.setAssociatedFileOnDrive(file);
    XmlUtil.writeToXml(this, file);
  }


  // Empty should be fine since they are not used to instantiate.
  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.EMPTY;
  }

  @Override
  public FxParamSetImpl getObservableInstance() {
    return new MonteCarloHistoViewer(this);
  }


  public Parameter<Boolean> getShowHistogram() {
    return showHistogram;
  }

  public Parameter<Boolean> getShowKernelDensity() {
    return showKernelDensity;
  }

  public Parameter<Boolean> getShowCumulative() {
    return showCumulative;
  }

  public HistogramNormalization getHistoNormalization() {
    return histogramNormalization.getValue();
  }

  public BinWidthEstimator getBinWidthEstimator() {
    return binWidthEstimator.getValue();
  }

  public double getCustomBinWidth() {
    return customBinWidth.getValue();
  }

  public double getCustomKernelBandwidth() {
    return customKernelBandwidth.getValue();
  }

  public EventType getEventType() {
    return eventType.getValue();
  }

  public Parameter<BackgroundHighlight> bgHighlight() {
    return backgroundHighlightOption;
  }

  public MathMod getMathMod() {
    return mathModification.getValue();
  }

  public EventParameter getEventParameter() {
    return eventParameter.getValue();
  }

  public boolean isJitterBG() {
    return jitterBackground.getValue();
  }

  public int getNumberOfBGEvents() {
    return numberOfBackgroundEvents.getValue();
  }

  public Boolean isShowMaxThrLOD() {
    return showMaxThrLOD.getValue();
  }

  public boolean isShowRuler() {
    return showRuler.getValue();
  }

  public MeasureOfLocation getRulerPosition() {
    return rulerPosition.getValue();
  }

  public double getCustomRulerPosition() {
    return customRulerPosition.getValue();
  }

  public boolean isLimitAxes() {
    return limitAxes.getValue();
  }

  public double getUpperYLimit() {
    return upperYLimit.getValue();
  }


  public double getUpperXLimitPercentile() {
    return upperXLimitPercentile.getValue();
  }

  public Parameter<Double> getUpperXLimitAbsolut() {
    return upperXLimitAbsolut;
  }

  public Parameter<Boolean> getUsePercentileForX() {
    return usePercentileForX;
  }

  public double getLowerXLimit() {
    return lowerXLimit.getValue();
  }

  public double getColorAlpha() {
    return Math.min(1, colorAlpha.getValue());
  }

  public void setEstimatedBinWidth(double width, FxParamSet fxParamSet) {
    ((FeedbackStringParameter) this.estimatedBinWidth).setFeedbackValueAndUpdate(
        SnF.doubleToString(width, NF.D1C3, NF.D1C3Exp),
        fxParamSet);
  }
}


/*
COPY HISTOGRAM DATA

if (histoChart != null) {
            XYPlot plot = histoChart.getXYPlot();
            if (plot != null) {

              // create empty storage array
              double[][] arr = new double[1][1];
              arr[0][0] = 0.0;
              // List for "sample names" i.e. labels
              List<String> labels = new ArrayList<>();
              int labelCount = plot.getLegendItems().getItemCount();

              // define dimensions (all > 0 to avoid null pointers)
              int datSetCount = plot
                  .getDatasetCount(); // if histograms are overlays: 1 dataset each
              int maxBarCount = 0; // items = bars per histogram
              for (int plotSet = 0; plotSet < datSetCount; plotSet++) {
                XYDataset dataset = plot.getDataset(plotSet);

                if (dataset instanceof HistogramDataset) {
                  HistogramDataset histogramDataset = (HistogramDataset) dataset;
                  // as of now, histograms have only one series and the overlay is done by adding datasets to the XYPlot.
                  int seriesCount = histogramDataset.getSeriesCount();
                  if (seriesCount > 0) {
                    int itmCount = histogramDataset.getItemCount(0); // items = bars per histogram
                    maxBarCount = Math.max(maxBarCount, itmCount);
                  }
                }
              }

              // override empty storage array
              arr = new double[maxBarCount][datSetCount * 2];

              // fill the array
              // plot set --> different histograms stored as datasets in the plot
              HistogramType histoFrequencyType = HistogramType.FREQUENCY; // assumed default
              int additionalCounter = 0;
              for (int datSetIdx = 0; datSetIdx < datSetCount; datSetIdx++) {
                XYDataset dataset = plot.getDataset(datSetIdx);
                if (datSetIdx < labelCount) { // just crash safety
                  labels.add(plot.getLegendItems().get(datSetIdx).getLabel());
                }

                if (dataset instanceof HistogramDataset) {
                  HistogramDataset histogramDataset = (HistogramDataset) dataset;
                  histoFrequencyType = histogramDataset
                      .getType(); // assume all are the same, 1 is enough

                  // fill array --> series count of histogram = 1 (should be the case for all)
                  int seriesCount = histogramDataset.getSeriesCount();
                  // counting the bins: item=bar=bin
                  int barCount = 0;
                  if (seriesCount > 0) { // just crash safety
                    barCount = histogramDataset.getItemCount(0);
                  }

                  for (int barIdx = 0; barIdx < barCount; barIdx++) {
                    arr[barIdx][datSetIdx + additionalCounter] = (double) histogramDataset
                        .getX(0, barIdx);
                    arr[barIdx][datSetIdx + additionalCounter + 1] = (double) histogramDataset
                        .getY(0, barIdx);
                  }
                  /// just checking
                  additionalCounter++;
                }

                // add labels
                String[][] stringArr = ClipboardUtil.createArray(arr, NF.D1C15);
                String[][] labelledArr = new String[stringArr.length + 2][
                    stringArr[stringArr.length - 1].length + 1];

                // add labels to new array
                for (int i = 0; i < labels.size(); i++) {
                  if (i * 2 < labelledArr[0].length) {
                    labelledArr[0][(i * 2)] = "Name in Histogram= ";
                    labelledArr[1][(i * 2)] = "x=center of bar";
                    labelledArr[0][(i * 2) + 1] = labels.get(i);
                    labelledArr[1][(i * 2) + 1] = histoFrequencyType.toString();
                  }
                }
                // add the data
                for (int n = 2; n < labelledArr.length; n++) {
                  labelledArr[n] = stringArr[n - 2];
                }

                ClipboardUtil.copy(labelledArr);
              }
            }
          }
 */

