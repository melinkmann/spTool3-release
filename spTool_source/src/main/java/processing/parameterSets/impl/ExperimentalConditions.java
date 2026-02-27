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

package processing.parameterSets.impl;

import analysis.quant.Cal;
import core.SpTool3Main;
import dataModelNew.Sample;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import math.units.enums.ConcentrationUnit;
import math.units.enums.FlowUnit;
import math.units.enums.SizeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.*;
import processing.parameters.*;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

public class ExperimentalConditions extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  private Parameter<Double> sampleIntroductionFlowRate;

  private Parameter<CalibratorRole> calibratorRole;
  private Parameter<SampleType> sampleType;

  private Parameter<QuantParam> sampleWideNPSourceParam;
  private Parameter<QuantParam> sampleWideIonicSourceParam;

  private final HashMap<dataModelNew.mz.Element, ExperimentalSubConditions> elementSpecificQuantParams;

  public static final String XML_ELEMENT_TAG = "GlobalQuantificationParams";

  public ExperimentalConditions() {
    this("Global quantification parameters");
  }

  // Idea: add sample label
  public ExperimentalConditions(String label) {
    super(label, XML_ELEMENT_TAG);

    this.elementSpecificQuantParams = new LinkedHashMap<>();

    this.sampleIntroductionFlowRate
        = new QuantParameter<>("Flow rate",
        "Liquid sample introduction flow rate",
        10d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        FlowUnit.MICROLITRE_PER_MINUTE,
        FlowUnit.values(),
        FlowUnit.class,
        false,
        false,
        "sampleIntroductionFlowRate");

    this.calibratorRole = new ComboEnumParameter<>(
        "Role",
        "Decide if this is a sample or calibrator",
        CalibratorRole.SAMPLE,
        CalibratorRole.values(),
        CalibratorRole.class,
        false,
        "calibratorRole"
    );

    this.sampleType = new ComboEnumParameter<>(
        "Type",
        "Decide if this is an ionic or particle type",
        SampleType.IONIC,
        SampleType.values(),
        SampleType.class,
        false,
        "sampleType"
    );

    this.sampleWideNPSourceParam = new ComboEnumParameter<>(
        "Particle",
        """
            Decide which parameter is used for intensity:
            Whenever a 'particle signal' is needed, e.g., to create a calibration curve
            based on particle intensity or to calculate particle masses/sizes, ...
            this parameter defines how the particulate signal is estimated""",
        QuantParam.NP_AREA_MEAN,
        QuantParam.getNP(),
        QuantParam.class,
        true,
        "sampleWideNPSourceParam"
    );

    this.sampleWideIonicSourceParam = new ComboEnumParameter<>(
        "Ionic",
        """
            Decide which parameter is used for intensity:
            Whenever an 'ionic signal' is needed, e.g., to create a calibration curve
            this parameter defines how the ionic signal is estimated""",
        QuantParam.RAW_MEAN,
        QuantParam.getIonic(),
        QuantParam.class,
        true,
        "sampleWideIonicSourceParam"
    );

    organize();
  }

  public ExperimentalConditions(ExperimentalConditions params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.elementSpecificQuantParams = new LinkedHashMap<>();
    for (dataModelNew.mz.Element element : params.elementSpecificQuantParams.keySet()) {
      ExperimentalSubConditions subPar = params.elementSpecificQuantParams.get(element);
      subPar.setElement(element);
      // copy each sub parameter
      this.elementSpecificQuantParams.put(element, new ExperimentalSubConditions(subPar));
    }

    this.sampleIntroductionFlowRate = params.sampleIntroductionFlowRate.copyWithoutChildren();
    this.calibratorRole = params.calibratorRole.copyWithoutChildren();
    this.sampleType = params.sampleType.copyWithoutChildren();
    this.sampleWideNPSourceParam = params.sampleWideNPSourceParam.copyWithoutChildren();
    this.sampleWideIonicSourceParam = params.sampleWideIonicSourceParam.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new ExperimentalConditions();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new ExperimentalConditions(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new ExperimentalConditions(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());

    return params;
  }

  private void organize() {
    // Register parent
    super.setParentParameters(
        sampleIntroductionFlowRate,
        calibratorRole,
        sampleWideIonicSourceParam,
        sampleWideNPSourceParam
    );

    calibratorRole.addConditionalChild(CalibratorRole.CALIBRATOR,
        sampleType);

    setCopyDecoration(calibratorRole);
    setCopyDecoration(sampleType);
    setCopyDecoration(sampleWideIonicSourceParam);
    setCopyDecoration(sampleWideNPSourceParam);
  }

  public void organizeSubParams() {
    elementSpecificQuantParams.values().forEach(p -> {
      p.organize(calibratorRole.getValue(), sampleType.getValue());
    });
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

          case "sampleIntroductionFlowRate" -> sampleIntroductionFlowRate;
          case "calibratorRole" -> calibratorRole;
          case "sampleType" -> sampleType;

          case "sampleWideIonicSourceParam" -> sampleWideIonicSourceParam;
          case "sampleWideNPSourceParam" -> sampleWideNPSourceParam;


/*
For the xml reader:
classic switch statement for all single instances

for the others: bundle.
bundle has method parse()
checks for keyword
if good, reads parameters
else, returns false;
added to HashMap only if returning true

OR

just pass the hashmap as argument and add itself if found
  e.g. that correct element is present and ID assigns as density
 */

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

  @Override
  public FxParamSet getObservableInstance() {
    // return new Viewers.QuantificationViewer(this);
    return new FxParamSetImpl(this);
  }

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.QUANT;
  }


  public HashMap<dataModelNew.mz.Element, ExperimentalSubConditions> getElementSpecificQuantParams() {
    return elementSpecificQuantParams;
  }

  public ExperimentalSubConditions getOrCreateElementSpecificQuantParams(dataModelNew.mz.Element element) {
    ExperimentalSubConditions result;
    if (!elementSpecificQuantParams.containsKey(element)) {
      result = new ExperimentalSubConditions();
      result.setElement(element);
      elementSpecificQuantParams.put(element, result);
    } else {
      result = elementSpecificQuantParams.get(element);
    }
//    result.organize(calibratorRole.getValue(), sampleType.getValue());
    return result;
  }


  public Parameter<CalibratorRole> getCalibratorRole() {
    return calibratorRole;
  }

  public Parameter<SampleType> getSampleType() {
    return sampleType;
  }

  public EventParameter getEventPar() {
    if (getSampleWideNPSourceParam().equals(QuantParam.NP_HEIGHT_MEAN)
        || getSampleWideNPSourceParam().equals(QuantParam.NP_HEIGHT_MEDIAN)) {
      return EventParameter.NET_HEIGHT;
    } else {
      return EventParameter.NET_AREA;
    }
  }

  public Parameter<QuantParam> getSampleWideIonicSourceParam() {
    return sampleWideIonicSourceParam;
  }

  public Parameter<QuantParam> getSampleWideNPSourceParam() {
    return sampleWideNPSourceParam;
  }

  public double getFlowRate(FlowUnit targetUnit) {
    FlowUnit unit = (FlowUnit) ((QuantParameter<?>) sampleIntroductionFlowRate).getUnit();
    double value = sampleIntroductionFlowRate.getValue();
    double result = unit.convert(value, targetUnit);
    return result;
  }


  public Parameter<Double> getSampleIntroductionFlowRate() {
    return sampleIntroductionFlowRate;
  }

  public void setSampleIntroductionFlowRate(FlowUnit unit) {
    ((QuantParameter<FlowUnit>) sampleIntroductionFlowRate).setUnit(unit);
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final ExperimentalConditions defaults = new ExperimentalConditions();

    if (sampleIntroductionFlowRate == null) {
      sampleIntroductionFlowRate = defaults.sampleIntroductionFlowRate;
    }
    if (calibratorRole == null) {
      calibratorRole = defaults.calibratorRole;
    }
    if (sampleType == null) {
      sampleType = defaults.sampleType;
    }

    if (sampleWideIonicSourceParam == null) {
      sampleWideIonicSourceParam = defaults.sampleWideIonicSourceParam;
    }

    if (sampleWideNPSourceParam == null) {
      sampleWideNPSourceParam = defaults.sampleWideNPSourceParam;
    }

    // adds the decoration
    organize();
  }

  private <P extends Serializable> void setCopyDecoration(Parameter<P> par) {
    par.setDecoration(new ButtonDecoration<>(
        "Set this value to all selected samples",
        "/img/grouped.png",
        () -> {
          List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
          for (Sample selSample : selSamples) {
            Cal quant = selSample.getQuant();
            for (Parameter<?> qPar : quant.getExperimentalConditions().listAllParameters()) {
              if (qPar.getXmlID().equals(par.getXmlID())) {
                try {
                  qPar.trySetValue(par);
                } catch (Exception e) {
                  Parameter.LOGGER.error("Cannot set value as types do not match!");
                }
              }
            }
          }

        }));
  }

}
