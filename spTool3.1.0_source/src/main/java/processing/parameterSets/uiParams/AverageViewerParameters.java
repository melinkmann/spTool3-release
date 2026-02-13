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
import math.stat.PreFilter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.SmoothType;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.AverageViewer;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import util.NF;

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

public class AverageViewerParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "AverageViewerParameters";

  private final Parameter<Boolean> showRawData;

  private final Parameter<EventParameter> npEventParameter;
  private final Parameter<EventParameter> bgEventParameter;

  private final Parameter<Double> generalWindowMillisec;
  private final Parameter<Double> npWindowMillisec;

  private final Parameter<MeasureOfLocation> rawLocationMeasure;
  private final Parameter<MeasureOfLocation> eventsLocationMeasure;

  private final Parameter<PreFilter> rawPreFilter;

  private final Parameter<SmoothType> rawSmoothType;
  private final Parameter<Integer> rawSGDegree;
  private final Parameter<Integer> rawSGWidth;
  private final Parameter<Integer> rawMOAVWidth;
  private final Parameter<Double> rawKernelSigma;
  private final Parameter<Double> rawLoessBandwidth;

  private final Parameter<SmoothType> npSmoothType;
  private final Parameter<Integer> npSGDegree;
  private final Parameter<Integer> npSGWidth;
  private final Parameter<Integer> npMOAVWidth;
  private final Parameter<Double> npKernelSigma;
  private final Parameter<Double> npLoessBandwidth;

  private final Parameter<Boolean> smoothPositive;

  private final Parameter<Boolean> addRegression;
  private final Parameter<Double> regressionViewRatio;

  private final Parameter<Boolean> autorangeWithZero;


  public AverageViewerParameters() {
    super("Average rate viewer parameters", XML_ELEMENT_TAG);

    this.npEventParameter = new ComboEnumParameter<>("NP",
        "Event parameter for the particle events",
        EventParameter.NO_OF_EVENTS,
        EventParameter.smooth(),
        EventParameter.class,
        false,
        "npEventParameter");

    this.bgEventParameter = new ComboEnumParameter<>("BG",
        "Event parameter for the background data points."
            + "\nBG data points are all data points that are not part of an event",
        EventParameter.AREA,
        EventParameter.smooth(),
        EventParameter.class,
        false,
        "bgEventParameter");

    this.showRawData = new BooleanParameter(
        "Raw data",
        "Show",
        "Average and smooth the raw data",
        true,
        false,
        "showRawData");

    this.generalWindowMillisec = new DoubleParameter("Δt [ms]",
        "Raw are averaged into a time frame with this time resolution Δt before smoothing",
        200d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "generalWindowMillisec");

    this.npWindowMillisec = new DoubleParameter("NP Δt [ms]",
        "Event data are averaged into a time frame with this time resolution before smoothing",
        50d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "npWindowMillisec");

    rawLocationMeasure = new ComboEnumParameter<>("Location",
        "Raw data values within a frame of the Δt time frame"
            + "\nare averaged using this measure of location",
        MeasureOfLocation.MEDIAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        false,
        "rawLocationMeasure");

    rawPreFilter = new ComboEnumParameter<>("Pre-filter",
        "Raw data are filtered with to prevent outliers (the events) from distorting the smooth",
        PreFilter.MAD,
        PreFilter.values(),
        PreFilter.class,
        false,
        "rawPreFilter");

    eventsLocationMeasure = new ComboEnumParameter<>("Location",
        "Event data within a frame of the coarse time frame"
            + "\nare averaged using this measure of location",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        false,
        "eventsLocationMeasure");
///
    rawSmoothType = new ComboEnumParameter<>("Smooth",
        "Decide the type of smoothing for the raw data",
        SmoothType.NONE,
        SmoothType.values(),
        SmoothType.class,
        false,
        "rawSmoothType");

    rawSGDegree = new IntegerParameter("Degree",
        "Savitzky-Golay (SG) and Sinc smooth uses this polynomial degree for the raw data",
        2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "rawSGDegree");

    this.rawMOAVWidth= new IntegerParameter("Width",
        "Moving average (MOAV) width given as data points",
        5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "rawMOAVWidth");

    rawLoessBandwidth = new DoubleParameter("Bandwidth",
        """
            Locally estimated scatterplot smoothing (LOESS) bandwidth for the raw data.
            Values between 0 and 1. Note, value must at least be 2/n where n is the number of data points.
            SpTool will adjust bandwidth if out of bounds""",
        0.025,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "rawLoessBandwidth");

    this.rawSGWidth = new IntegerParameter("Window",
        "Savitzky-Golay (SG) and Sinc smooth uses this window width for the raw data",
        15,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "rawSGWidth");

    this.rawKernelSigma = new DoubleParameter("Width",
        "Sigma, i.e., width of the Gaussian kernel for raw data smoothing",
        1d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "rawKernelSigma");

    this.smoothPositive = new BooleanParameter(
        "SG",
        "Force >0",
        "If the smooth overshoots into the negative range, replace with a zero",
        true,
        false,
        "smoothPositive");
///
    npSmoothType = new ComboEnumParameter<>("Smooth",
        "Decide the type of smoothing for the particle data",
        SmoothType.NONE,
        SmoothType.values(),
        SmoothType.class,
        false,
        "npSmoothType");

    npSGDegree = new IntegerParameter("Degree",
        "Savitzky-Golay (SG) and Sinc  smooth uses this polynomial degree for the particle data",
        2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "npSGDegree");


    this.npSGWidth = new IntegerParameter("Window",
        "Savitzky-Golay (SG) and Sinc smooth uses this window width for the particle data",
        20,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "npSGWidth");

    this.npMOAVWidth= new IntegerParameter("Width",
        "Moving average (MOAV) width given as data points",
        5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "npMOAVWidth");

    npLoessBandwidth = new DoubleParameter("Bandwidth",
        """
            Locally estimated scatterplot smoothing (LOESS) bandwidth for the particle data.
            Values between 0 and 1. Note, value must at least be 2/n where n is the number of data points.
            SpTool will adjust bandwidth if out of bounds""",
        0.025,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "rawLoessBandwidth");

    this.npKernelSigma = new DoubleParameter("Width",
        "Sigma, i.e., width of the Gaussian kernel for particle data smoothing",
        1d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "npKernelSigma");

    this.autorangeWithZero = new BooleanParameter(
        "Auto-range",
        "Include 0",
        "Y-axis always includes zero",
        false,
        false,
        "autorangeWithZero");

    this.addRegression = new BooleanParameter(
        "Show",
        "OLS regression",
        "Add ordinary least square regression to reveal trends",
        false,
        false,
        "addRegression");

    this.regressionViewRatio = new DoubleParameter("Rel. Δt [-]",
        "Increase the view window for the regression relative to the x data [0-1],",
        0.10d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "regressionViewRatio");

    organize();
  }

  public AverageViewerParameters(AverageViewerParameters iclPeakViewer) {
    super(iclPeakViewer.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakViewer.getCommentParameter());

    this.showRawData = iclPeakViewer.showRawData.copyWithoutChildren();
    this.npEventParameter = iclPeakViewer.npEventParameter.copyWithoutChildren();
    this.bgEventParameter = iclPeakViewer.bgEventParameter.copyWithoutChildren();
    this.generalWindowMillisec = iclPeakViewer.generalWindowMillisec.copyWithoutChildren();
    this.npWindowMillisec = iclPeakViewer.npWindowMillisec.copyWithoutChildren();
    this.rawLocationMeasure = iclPeakViewer.rawLocationMeasure.copyWithoutChildren();
    this.eventsLocationMeasure = iclPeakViewer.eventsLocationMeasure.copyWithoutChildren();
    this.rawPreFilter = iclPeakViewer.rawPreFilter.copyWithoutChildren();
    this.rawSmoothType = iclPeakViewer.rawSmoothType.copyWithoutChildren();
    this.rawSGDegree = iclPeakViewer.rawSGDegree.copyWithoutChildren();
    this.rawSGWidth = iclPeakViewer.rawSGWidth.copyWithoutChildren();
    this.rawMOAVWidth = iclPeakViewer.rawMOAVWidth.copyWithoutChildren();
    this.smoothPositive = iclPeakViewer.smoothPositive.copyWithoutChildren();
    this.npSmoothType = iclPeakViewer.npSmoothType.copyWithoutChildren();
    this.npSGDegree = iclPeakViewer.npSGDegree.copyWithoutChildren();
    this.npSGWidth = iclPeakViewer.npSGWidth.copyWithoutChildren();
    this.npMOAVWidth = iclPeakViewer.npMOAVWidth.copyWithoutChildren();
    this.npKernelSigma = iclPeakViewer.npKernelSigma.copyWithoutChildren();
    this.rawKernelSigma = iclPeakViewer.rawKernelSigma.copyWithoutChildren();
    this.npLoessBandwidth = iclPeakViewer.npLoessBandwidth.copyWithoutChildren();
    this.rawLoessBandwidth = iclPeakViewer.rawLoessBandwidth.copyWithoutChildren();

    this.addRegression = iclPeakViewer.addRegression.copyWithoutChildren();
    this.regressionViewRatio = iclPeakViewer.regressionViewRatio.copyWithoutChildren();

    this.autorangeWithZero = iclPeakViewer.autorangeWithZero.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new AverageViewerParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new AverageViewerParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new AverageViewerParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        showRawData,
        npWindowMillisec,
        npEventParameter,
        bgEventParameter,
        eventsLocationMeasure,
        npSmoothType,
        addRegression,
        autorangeWithZero
    );

    showRawData.addConditionalChild(true,
        generalWindowMillisec,
        rawLocationMeasure,
        rawPreFilter,
        rawSmoothType);

    rawSmoothType.addConditionalChild(SmoothType.SAVITZKY_GOLAY,
        rawSGDegree,
        rawSGWidth,
        smoothPositive
    );

    rawSmoothType.addConditionalChild(SmoothType.MOAV,rawMOAVWidth);

    rawSmoothType.addConditionalChild(SmoothType.SINC_KERNEL,
        rawSGDegree,
        rawSGWidth,
        smoothPositive
    );

    rawSmoothType.addConditionalChild(SmoothType.LOESS,
        rawLoessBandwidth
    );

    rawSmoothType.addConditionalChild(SmoothType.GAUSSIAN_KERNEL,
        rawKernelSigma
    );

    npSmoothType.addConditionalChild(SmoothType.SAVITZKY_GOLAY,
        npSGDegree,
        npSGWidth,
        smoothPositive
    );

    npSmoothType.addConditionalChild(SmoothType.MOAV,npMOAVWidth);

    npSmoothType.addConditionalChild(SmoothType.SINC_KERNEL,
        npSGDegree,
        npSGWidth,
        smoothPositive
    );

    npSmoothType.addConditionalChild(SmoothType.LOESS,
        npLoessBandwidth);

    npSmoothType.addConditionalChild(SmoothType.GAUSSIAN_KERNEL,
        npKernelSigma
    );

    addRegression.addConditionalChild(true, regressionViewRatio);

    // sortBoolean.addConditionalChild(true, eventParameter);
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

          case "showRawData" -> showRawData;
          case "npEventParameter" -> npEventParameter;
          case "bgEventParameter" -> bgEventParameter;
          case "generalWindowMillisec" -> generalWindowMillisec;
          case "npWindowMillisec" -> npWindowMillisec;
          case "rawLocationMeasure" -> rawLocationMeasure;
          case "eventsLocationMeasure" -> eventsLocationMeasure;
          case "rawPreFilter" -> rawPreFilter;
          case "rawSmoothType" -> rawSmoothType;
          case "rawSGDegree" -> rawSGDegree;
          case "rawSGWidth" -> rawSGWidth;
          case "rawMOAVWidth" -> rawMOAVWidth;
          case "smoothPositive" -> smoothPositive;
          case "npSmoothType" -> npSmoothType;
          case "npSGDegree" -> npSGDegree;
          case "npMOAVWidth" -> npMOAVWidth;
          case "npKernelSigma" -> npKernelSigma;
          case "rawKernelSigma" -> rawKernelSigma;

          case "npLoessBandwidth"  ->npLoessBandwidth;
          case "rawLoessBandwidth" -> rawLoessBandwidth;

          case "addRegression" -> addRegression;
          case "regressionViewRatio" -> regressionViewRatio;

          case "autorangeWithZero" -> autorangeWithZero;

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
    return new AverageViewer(this);
  }

  public Parameter<Boolean> getShowRawData() {
    return showRawData;
  }

  public Parameter<EventParameter> getNpEventParameter() {
    return npEventParameter;
  }

  public Parameter<EventParameter> getBgEventParameter() {
    return bgEventParameter;
  }

  public Parameter<Double> getGeneralWindowMillisec() {
    return generalWindowMillisec;
  }

  public Parameter<Double> getNpWindowMillisec() {
    return npWindowMillisec;
  }

  public Parameter<MeasureOfLocation> getRawLocationMeasure() {
    return rawLocationMeasure;
  }

  public Parameter<MeasureOfLocation> getEventsLocationMeasure() {
    return eventsLocationMeasure;
  }

  public Parameter<PreFilter> getRawPreFilter() {
    return rawPreFilter;
  }

  public Parameter<SmoothType> getRawSmoothType() {
    return rawSmoothType;
  }

  public Parameter<Integer> getRawSGDegree() {
    return rawSGDegree;
  }

  public Parameter<Integer> getRawSGWidth() {
    return rawSGWidth;
  }

  public Parameter<Integer> getRawMOAVWidth() {
    return rawMOAVWidth;
  }

  public Parameter<Boolean> getSmoothPositive() {
    return smoothPositive;
  }

  public Parameter<SmoothType> getNpSmoothType() {
    return npSmoothType;
  }

  public Parameter<Integer> getNpSGDegree() {
    return npSGDegree;
  }

  public Parameter<Integer> getNpSGWidth() {
    return npSGWidth;
  }

  public Parameter<Integer> getNpMOAVWidth() {
    return npMOAVWidth;
  }

  public Parameter<Double> getNpKernelSigma() {
    return npKernelSigma;
  }

  public Parameter<Double> getRawKernelSigma() {
    return rawKernelSigma;
  }

  public Parameter<Double> getNpLoessBandwidth() {
    return npLoessBandwidth;
  }

  public Parameter<Double> getRawLoessBandwidth() {
    return rawLoessBandwidth;
  }

  public Parameter<Boolean> getAddRegression() {
    return addRegression;
  }

  public Parameter<Double> getRegressionViewRatio() {
    return regressionViewRatio;
  }

  public Parameter<Boolean> getAutorangeWithZero() {
    return autorangeWithZero;
  }
}
