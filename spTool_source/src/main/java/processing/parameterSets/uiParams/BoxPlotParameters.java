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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.MathMod;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.SpectrumViewer;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.ImageDecoration;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;

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

public class BoxPlotParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "BoxPlotParameters";

  /*
  Abs/Rel freq.
  Bin Width (Decoration: Suggestion, set when changed)
  NP or BG
  log or nothing
  Type: area, height,
  Reduce BG, N=...
  Ruler, Ruler Pos...
  Axis Limits (number, percentile)
  y axis limit
   */

  /*
  histogramNormalization
      binWidthEstimator
  binWidth
      eventType
  mathModification
      eventParameter
  reduceBackground
      numberOfBackgroundEvents

  showRuler
      rulerPosition
  customPosition

      limitAxes
  upperYLimit
      upperXLimitPercentile
  lowerXLimit

      showEventMarkers
  showPopulationMarkers
      upperPointCountCutoff
  mergeIsotopes

   */

  private final Parameter<EventType> eventType;
  private final Parameter<Boolean> jitterBackground;
  private final Parameter<Integer> numberOfBackgroundEvents;

  private final Parameter<MathMod> mathModification;
  private final Parameter<EventParameter> eventParameter;

  private final Parameter<Boolean> showIDsOnPlot;

//  private final Parameter<Boolean> limitAxes;
//  private final Parameter<Double> upperYLimit;
//  private final Parameter<Double> upperXLimitPercentile;
//  private final Parameter<Double> lowerXLimit;


  public BoxPlotParameters() {
    super("Monte carlo histogram viewer parameters", XML_ELEMENT_TAG);

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
        false,
        true,
        "jitterBackground"
    );

    this.showIDsOnPlot = new BooleanParameter(
        "IDs",
        "Show box ID",
        "Adds an identifier to each box (sample number, population number, mz)",
        true,
        false,
        "showIDsOnPlot"
    );


    numberOfBackgroundEvents = new IntegerParameter(
        "# of BG",
        "Number of background (BG) events to be shown",
        500,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        true,
        "numberOfBackgroundEvents");


//    limitAxes = new BooleanParameter(
//        "Axis limits",
//        "Set limits",
//        "Set custom limits for the x and y axes of the plot",
//        false,
//        true,
//        "limitAxes"
//    );
//
//    upperYLimit = new DoubleParameter(
//        "Upper y",
//        "Choose the upper y-axis limit as a absolute value",
//        100d,
//        NF.D1C3,
//        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
//        false,
//        "upperYLimit");
//
//    upperXLimitPercentile = new DoubleParameter(
//        "Upper x [%]",
//        "Choose the upper x-axis limit as a percentile",
//        95d,
//        NF.D1C3,
//        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
//        false,
//        "upperXLimitPercentile");
//
//    lowerXLimit = new DoubleParameter(
//        "Lower x",
//        "Choose the lower x-axis limit as an absolute value",
//        0.00d,
//        NF.D1C3,
//        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
//        false,
//        "lowerXLimit");


    organize();
  }

  public BoxPlotParameters(BoxPlotParameters histParams) {
    super(histParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(histParams.getCommentParameter());

    this.mathModification = histParams.mathModification.copyWithoutChildren();
    this.eventParameter = histParams.eventParameter.copyWithoutChildren();
    this.eventType = histParams.eventType.copyWithoutChildren();

    this.jitterBackground = histParams.jitterBackground.copyWithoutChildren();
    this.numberOfBackgroundEvents = histParams.numberOfBackgroundEvents.copyWithoutChildren();

    this.showIDsOnPlot = histParams.showIDsOnPlot.copyWithoutChildren();


//    this.limitAxes = histParams.limitAxes.copyWithoutChildren();
//    this.upperYLimit = histParams.upperYLimit.copyWithoutChildren();
//    this.upperXLimitPercentile = histParams.upperXLimitPercentile.copyWithoutChildren();
//    this.lowerXLimit = histParams.lowerXLimit.copyWithoutChildren();


    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new BoxPlotParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new BoxPlotParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new BoxPlotParameters(this);
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
        showIDsOnPlot

//        limitAxes,

    );

    eventType.addConditionalChild(EventType.BG_NP, jitterBackground);


    jitterBackground.addConditionalChild(true, numberOfBackgroundEvents);

//    limitAxes.addConditionalChild(true, upperYLimit, lowerXLimit, upperXLimitPercentile);

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

          case "eventType" -> eventType;
          case "mathModification" -> mathModification;
          case "eventParameter" -> eventParameter;
          case "reduceBackground" -> jitterBackground;
          case "numberOfBackgroundEvents" -> numberOfBackgroundEvents;
          case "showIDsOnPlot" -> showIDsOnPlot;

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
    return new Viewers.BoxPlotViewer(this);
  }

  public Parameter<Boolean> getShowIDsOnPlot() {
    return showIDsOnPlot;
  }

  public Parameter<EventType> getEventType() {
    return eventType;
  }

  public EventParameter getEventParameter() {
    return eventParameter.getValue();
  }

  public Parameter<MathMod> getMathModification() {
    return mathModification;
  }

  public Parameter<Boolean> getJitterBackground() {
    return jitterBackground;
  }

  public Parameter<Integer> getNumberOfBackgroundEvents() {
    return numberOfBackgroundEvents;
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
                  // as of now, histograms have only one series and the overlay is done by adding datasets
                  to the XYPlot.

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

