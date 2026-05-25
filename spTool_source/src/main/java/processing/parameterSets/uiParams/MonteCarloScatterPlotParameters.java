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
import processing.options.MathMod;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.MonteCarloScatterPlotViewer;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.ImageDecoration;
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

public class MonteCarloScatterPlotParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "MonteCarloScatterPlotParameters";

  // private final Parameter<Boolean> useIsotopes; TODO: possible, once alignment is done, e.g.;
  /*
          private final CheckBox scatterIsotopeBox = new CheckBox("Set isotopes");
        private final ComboBox<Isotope> scatterX = new ComboBox<>();
        private final ComboBox<Isotope> scatterY = new ComboBox<>();
        HBox comboHBox = new HBox(5);
        comboHBox.setAlignment(Pos.CENTER);
        comboHBox.setPadding(new Insets(5));
        comboHBox.setPrefHeight(35);
        CheckBox selectIsotopeBox = SpTool3Main.getRunTime().getGuiParameterManager().getScatterIsotopeBox();
        comboHBox.getChildren().add(selectIsotopeBox);
        ComboBox<Isotope> xCombo = SpTool3Main.getRunTime().getGuiParameterManager().getScatterX();
        ComboBox<Isotope> yCombo = SpTool3Main.getRunTime().getGuiParameterManager().getScatterY();
        selectIsotopeBox.setOnAction(e -> {
          if (selectIsotopeBox.isSelected()) {
            Isotope lastX = xCombo.getSelectionModel().getSelectedItem();
            Isotope lastY = yCombo.getSelectionModel().getSelectedItem();

            xCombo.getItems().clear();
            yCombo.getItems().clear();

            xCombo.getItems().addAll(selIsotopes);
            yCombo.getItems().addAll(selIsotopes);

            if (lastX != null && xCombo.getItems().contains(lastX)) {
              xCombo.getSelectionModel().select(lastX);
            } else {
              xCombo.getSelectionModel().selectFirst();
            }

            if (lastY != null && yCombo.getItems().contains(lastY)) {
              yCombo.getSelectionModel().select(lastY);
            } else {
              yCombo.getSelectionModel().selectFirst();
            }

            comboHBox.getChildren().clear();
            comboHBox.getChildren().addAll(selectIsotopeBox, xCombo, yCombo);
          } else {
            comboHBox.getChildren().clear();
            comboHBox.getChildren().addAll(selectIsotopeBox);
          }
        });
        targetPane.setTop(comboHBox);
        if (selectIsotopeBox.isSelected()) {
          tracesToShow = new ArrayList<>();
          List<Trace> xTraces = AnalysisUtils.getTracesForIsotopes(samples,
              Collections.singletonList(xCombo.getSelectionModel().getSelectedItem()), false);
          List<Trace> yTraces = AnalysisUtils.getTracesForIsotopes(samples,
              Collections.singletonList(yCombo.getSelectionModel().getSelectedItem()), false);
          tracesToShow.addAll(xTraces);
          tracesToShow.addAll(yTraces);
        }
   */


  private final Parameter<Boolean> scatterIsotopes;

  private final Parameter<EventParameter> eventParameterX;
  private final Parameter<MathMod> mathModificationX;

  private final Parameter<EventParameter> eventParameterY;
  private final Parameter<MathMod> mathModificationY;

  private final Parameter<Boolean> logWithoutZeros;

  private final Parameter<Boolean> addRegression;
  private final Parameter<Double> regressionViewRatio;

  private final Parameter<Double> colorAlpha;
  private final Parameter<Integer> dotSize;

  private final Parameter<Boolean> limitAxes;
  private final Parameter<Double> upperYLimit;
  private final Parameter<Double> upperXLimit;
  private final Parameter<Double> lowerYLimit;
  private final Parameter<Double> lowerXLimit;


  public MonteCarloScatterPlotParameters() {
    super("Monte carlo scatter plot viewer parameters", XML_ELEMENT_TAG);

    this.scatterIsotopes = new BooleanParameter(
        "Data",
        "2 Isotopes",
        """
            If selected, the first selected isotope is put on the x-axis
            and the second selected isotope is put on the y-axis.
            Otherwise, we scatter different peak parameters for the same isotope""",
        false,
        true,
        "scatterIsotopes"
    );

    eventParameterX = new ComboEnumParameter<>(
        "x data",
        "Choose which data shall be shown",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameterX"
    );

    mathModificationX = new ComboEnumParameter<>(
        "x math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathModificationX"
    );

    eventParameterY = new ComboEnumParameter<>(
        "y data",
        "Choose which data shall be shown",
        EventParameter.NET_HEIGHT,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameterY"
    );

    mathModificationY = new ComboEnumParameter<>(
        "y math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathModificationY"
    );

    this.logWithoutZeros = new BooleanParameter(
        "Trim",
        "Values > 0",
        "Exclude values smaller than or equal to zero when log10 transformation is applied",
        false,
        true,
        "logWithoutZeros"
    );


    this.addRegression = new BooleanParameter(
        "Show",
        "OLS regression",
        "Add ordinary least square regression fit to data",
        false,
        false,
        "addRegression");

    this.regressionViewRatio = new DoubleParameter("View Δx [-]",
        "Increase the view window for the regression relative to the x data [0-1],",
        0.10d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "regressionViewRatio");

    this.colorAlpha = new DoubleParameter(
        "Alpha",
        "Set the alpha value (transparency) for the dots",
        0.9d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "colorAlpha");

    this.dotSize = new IntegerParameter(
        "Size",
        "Set the size value for the dots",
        6,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "dotSize");

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
        0d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperYLimit");

    upperXLimit = new DoubleParameter(
        "Upper x",
        "Choose the upper x-axis limit as an absolute value",
        0.00d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "upperXLimit");

    lowerYLimit = new DoubleParameter(
        "Lower y",
        "Choose the lower y-axis limit as a absolute value",
        0d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "lowerYLimit");


    lowerXLimit = new DoubleParameter(
        "Lower x",
        "Choose the lower x-axis limit as an absolute value",
        0.00d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "lowerXLimit");

    organize();
  }

  public MonteCarloScatterPlotParameters(MonteCarloScatterPlotParameters params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.scatterIsotopes = params.scatterIsotopes.copyWithoutChildren();
    this.mathModificationX = params.mathModificationX.copyWithoutChildren();
    this.eventParameterX = params.eventParameterX.copyWithoutChildren();
    this.mathModificationY = params.mathModificationY.copyWithoutChildren();
    this.eventParameterY = params.eventParameterY.copyWithoutChildren();
    this.logWithoutZeros=params.logWithoutZeros.copyWithoutChildren();
    this.addRegression = params.addRegression.copyWithoutChildren();
    this.regressionViewRatio = params.regressionViewRatio.copyWithoutChildren();
    this.colorAlpha = params.colorAlpha.copyWithoutChildren();
    this.dotSize = params.dotSize.copyWithoutChildren();
    this.limitAxes = params.limitAxes.copyWithoutChildren();
    this.upperXLimit = params.upperXLimit.copyWithoutChildren();
    this.upperYLimit = params.upperYLimit.copyWithoutChildren();
    this.lowerXLimit = params.lowerXLimit.copyWithoutChildren();
    this.lowerYLimit = params.lowerYLimit.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new MonteCarloScatterPlotParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MonteCarloScatterPlotParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MonteCarloScatterPlotParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        scatterIsotopes,
        eventParameterX,
        mathModificationX,
        eventParameterY,
        mathModificationY,
        logWithoutZeros,
        addRegression,
        colorAlpha,
        dotSize,
        limitAxes);

    addRegression.addConditionalChild(true, regressionViewRatio);

    limitAxes.addConditionalChild(true, lowerXLimit, upperXLimit, lowerYLimit, upperYLimit);

    eventParameterX.setDecoration(new ImageDecoration<>("/img/sumareaX.png"));
    mathModificationX.setDecoration(new ImageDecoration<>("/img/linlogX.png"));
    eventParameterY.setDecoration(new ImageDecoration<>("/img/sumareaY.png"));
    mathModificationY.setDecoration(new ImageDecoration<>("/img/linlogY.png"));
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

          case "scatterIsotopes" -> scatterIsotopes;

          case "eventParameterX" -> eventParameterX;
          case "mathModificationX" -> mathModificationX;
          case "eventParameterY" -> eventParameterY;
          case "mathModificationY" -> mathModificationY;
          case "logWithoutZeros" -> logWithoutZeros;

          case "colorAlpha" -> colorAlpha;
          case "dotSize" -> dotSize;

          case "addRegression" -> addRegression;
          case "regressionViewRatio" -> regressionViewRatio;

          case "limitAxes" -> limitAxes;
          case "upperXLimit" -> upperXLimit;
          case "upperYLimit" -> upperYLimit;

          case "lowerXLimit" -> lowerXLimit;
          case "lowerYLimit" -> lowerYLimit;

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
    return new MonteCarloScatterPlotViewer(this);
  }


  /// ///////////////////////////////////////////////////////////

  public Parameter<Boolean> getScatterIsotopes() {
    return scatterIsotopes;
  }

  public Parameter<EventParameter> getEventParameterX() {
    return eventParameterX;
  }

  public Parameter<EventParameter> getEventParameterY() {
    return eventParameterY;
  }

  public Parameter<MathMod> getMathModificationX() {
    return mathModificationX;
  }

  public Parameter<MathMod> getMathModificationY() {
    return mathModificationY;
  }


  public boolean isComputeNonzero() {
    return logWithoutZeros.getValue()
        && (mathModificationX.getValue().equals(MathMod.LOG10)
        || mathModificationY.getValue().equals(MathMod.LOG10));
  }

  public Parameter<Boolean> getAddRegression() {
    return addRegression;
  }

  public Parameter<Double> getRegressionViewRatio() {
    return regressionViewRatio;
  }

  public Parameter<Double> getColorAlpha() {
    return colorAlpha;
  }

  public Parameter<Integer> getDotSize() {
    return dotSize;
  }

  public Parameter<Boolean> getLimitAxes() {
    return limitAxes;
  }

  public Parameter<Double> getUpperXLimit() {
    return upperXLimit;
  }

  public Parameter<Double> getUpperYLimit() {
    return upperYLimit;
  }

  public Parameter<Double> getLowerXLimit() {
    return lowerXLimit;
  }

  public Parameter<Double> getLowerYLimit() {
    return lowerYLimit;
  }
}
