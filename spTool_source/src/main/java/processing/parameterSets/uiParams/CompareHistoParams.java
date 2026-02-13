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
import math.stat.MeasureOfLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.*;
import processing.parameterSets.uiParams.Viewers.MonteCarloHistoViewer;
import processing.parameters.*;
import util.NF;
import util.SnF;

import java.io.Serial;
import java.nio.file.Path;

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

public class CompareHistoParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "CompareHistoParams";

  private final Parameter<Boolean> showKernelDensity;
  private final Parameter<Boolean> showHistogram;
  private final Parameter<Boolean> showCumulative;

  private final Parameter<HistogramNormalization> histogramNormalization;
  private final Parameter<BinWidthEstimator> binWidthEstimator;
  private final Parameter<Double> customBinWidth;
  private final Parameter<String> estimatedBinWidth;
  private final Parameter<Double> customKernelBandwidth;

  private final Parameter<MathMod> mathModification;

  private final Parameter<Boolean> limitAxes;
  private final Parameter<Double> upperYLimit;
  private final Parameter<Double> upperXLimitAbsolut;
  private final Parameter<Boolean> usePercentileForX;
  private final Parameter<Double> upperXLimitPercentile;
  private final Parameter<Double> lowerXLimit;

  private final Parameter<Double> colorAlpha;

  public CompareHistoParams() {
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
        BinWidthEstimator.RICE,
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

    mathModification = new ComboEnumParameter<>(
        "Math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathModification"
    );

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

  public CompareHistoParams(CompareHistoParams histParams) {
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
    return new CompareHistoParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new CompareHistoParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new CompareHistoParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        mathModification,

        showHistogram,
        showKernelDensity,
        showCumulative,

        limitAxes,

        colorAlpha
    );

    showHistogram.addConditionalChild(true, binWidthEstimator, histogramNormalization);
    showKernelDensity.addConditionalChild(true, customKernelBandwidth);

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

    mathModification.setDecoration(new ImageDecoration<>("/img/linlog.png"));
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
          case "mathModification" -> mathModification;

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
    return new Viewers.HistoCompareViewer(this);
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


  public MathMod getMathMod() {
    return mathModification.getValue();
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




