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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.MathMod;
import processing.parameterSets.*;
import processing.parameters.*;
import util.NF;

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

public class TernaryPlotParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "TernaryPlotParameters";

  private final Parameter<Boolean> scatterIsotopes;

  private final Parameter<EventParameter> eventParameter;

  private final Parameter<EventParameter> eventParameterA;
  private final Parameter<EventParameter> eventParameterB;
  private final Parameter<EventParameter> eventParameterC;

  private final Parameter<MathMod> mathModification;

  private final Parameter<Boolean> logWithoutZeros;

  private final Parameter<Double> colorAlpha;
  private final Parameter<Integer> dotSize;


  public TernaryPlotParameters() {
    super("Monte carlo scatter plot viewer parameters", XML_ELEMENT_TAG);

    this.scatterIsotopes = new BooleanParameter(
        "Data",
        "Show 3 channels",
        """
            If selected, the first three channels from the channel table will be shown
            in order of appearance.
            Otherwise, we scatter different peak parameters for the same isotope""",
        true,
        true,
        "scatterIsotopes"
    );

    eventParameter = new ComboEnumParameter<>(
        "Parameter",
        "Choose which data shall be shown",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter"
    );

    eventParameterA = new ComboEnumParameter<>(
        "Comp A",
        "Choose which parameter data shall be shown for component A",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameterA"
    );

    eventParameterB = new ComboEnumParameter<>(
        "Comp B",
        "Choose which parameter data shall be shown for component B",
        EventParameter.NET_HEIGHT,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameterB"
    );

    eventParameterC = new ComboEnumParameter<>(
        "Comp C",
        "Choose which parameter data shall be shown for component C",
        EventParameter.DURATION,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameterC"
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

    this.logWithoutZeros = new BooleanParameter(
        "Trim",
        "Values > 0",
        "Exclude values smaller than or equal to zero when log10 transformation is applied",
        false,
        true,
        "logWithoutZeros"
    );


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

    organize();
  }

  public TernaryPlotParameters(TernaryPlotParameters params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.scatterIsotopes = params.scatterIsotopes.copyWithoutChildren();
    this.mathModification = params.mathModification.copyWithoutChildren();
    this.eventParameter = params.eventParameter.copyWithoutChildren();
    this.eventParameterA = params.eventParameterA.copyWithoutChildren();
    this.eventParameterB = params.eventParameterB.copyWithoutChildren();
    this.eventParameterC = params.eventParameterC.copyWithoutChildren();
    this.logWithoutZeros = params.logWithoutZeros.copyWithoutChildren();
    this.colorAlpha = params.colorAlpha.copyWithoutChildren();
    this.dotSize = params.dotSize.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new TernaryPlotParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new TernaryPlotParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new TernaryPlotParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        scatterIsotopes,
        // mathModification,
        // logWithoutZeros,
        colorAlpha,
        dotSize
    );

    scatterIsotopes.addConditionalChild(true, eventParameter);
    scatterIsotopes.addConditionalChild(false, eventParameterA, eventParameterB, eventParameterC);

    eventParameter.setDecoration(new ImageDecoration<>("/img/sumarea.png"));
    eventParameterA.setDecoration(new ImageDecoration<>("/img/sumarea.png"));
    eventParameterB.setDecoration(new ImageDecoration<>("/img/sumarea.png"));
    eventParameterC.setDecoration(new ImageDecoration<>("/img/sumarea.png"));

    // TODO: Long-term: add Textfield to enter Channel for components A, B, C (decoration: load)
    //  --> requires assigning isotope by String value (is that implemented correctly?)

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

          case "scatterIsotopes" -> scatterIsotopes;

          case "eventParameter" -> eventParameter;
          case "eventParameterA" -> eventParameterA;
          case "eventParameterB" -> eventParameterB;
          case "eventParameterC" -> eventParameterC;


          case "mathModificationX" -> mathModification;
          case "logWithoutZeros" -> logWithoutZeros;

          case "colorAlpha" -> colorAlpha;
          case "dotSize" -> dotSize;

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
    return new Viewers.TernaryPlotViewer(this);
  }


  /// ///////////////////////////////////////////////////////////

  public Parameter<Boolean> getScatterIsotopes() {
    return scatterIsotopes;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }

  public Parameter<EventParameter> getEventParameterA() {
    return eventParameterA;
  }

  public Parameter<EventParameter> getEventParameterB() {
    return eventParameterB;
  }

  public Parameter<EventParameter> getEventParameterC() {
    return eventParameterC;
  }

  public Parameter<MathMod> getMathModification() {
    return mathModification;
  }


  public boolean isComputeNonzero() {
    return logWithoutZeros.getValue()
        && (mathModification.getValue().equals(MathMod.LOG10));
  }

  public Parameter<Double> getColorAlpha() {
    return colorAlpha;
  }

  public Parameter<Integer> getDotSize() {
    return dotSize;
  }

}
