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

import core.SpTool3Main;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.SimpleFxEntry;
import gui.dialog.mainImpl.ChooseSingleFromListDialog;
import gui.util.TextFormatterOption;
import gui.util.UiUtil;
import io.FxFileSet;
import io.SpCalDensity;
import io.XmlUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.util.Pair;
import math.units.enums.ConcentrationUnit;
import math.units.enums.DensityUnit;
import math.units.enums.MassUnit;
import math.units.enums.SizeUnit;
import org.checkerframework.checker.units.qual.C;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.CalibratorRole;
import processing.options.ParticleQuantApproach;
import processing.options.SampleType;
import processing.parameterSets.*;
import processing.parameters.*;
import util.ArrUtils;
import util.Functional;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;


public class ExperimentalSubConditions extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  private dataModelNew.mz.Element element;

  private Parameter<Double> npConcentration;
  private Parameter<ParticleQuantApproach> npQuantificationApproach;
  private Parameter<Double> npDensity;
  private Parameter<Double> npMassFraction;

  private Parameter<Double> npElementMass;
  private Parameter<Double> npSphericalDiameter;

  private Parameter<Double> ionicConcentration;


  public static final String XML_ELEMENT_TAG = "QuantificationSubParams";

  public ExperimentalSubConditions() {
    super("Quantification sub-parameters", XML_ELEMENT_TAG);

    this.element = dataModelNew.mz.Element.UNKNOWN;

    this.npConcentration
        = new QuantParameter<>("PNC",
        "Particle number concentration",
        0d,
        NF.D1C3Exp,
        TextFormatterOption.ASSURE_POS_EXP_DOUBLE,
        ConcentrationUnit.NP_PER_MILLILITRE,
        ConcentrationUnit.getNP(),
        ConcentrationUnit.class,
        false,
        false,
        "npConcentration");

    this.npQuantificationApproach = new ComboEnumParameter<>(
        "d | m",
        "Decide if particle is described by size or mass",
        ParticleQuantApproach.ESD,
        ParticleQuantApproach.values(),
        ParticleQuantApproach.class,
        false,
        "npQuantificationApproach"
    );

    this.npDensity = new QuantParameter<>("Density",
        "NP density",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        DensityUnit.GRAM_PER_CM3,
        DensityUnit.values(),
        DensityUnit.class,
        false,
        false,
        "npDensity");

    npElementMass = new QuantParameter<>("Element mass",
        "Mass of the respective element per particle",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        MassUnit.FEMTO_GRAM,
        MassUnit.values(),
        MassUnit.class,
        false,
        false,
        "npElementMass");

    npSphericalDiameter = new QuantParameter<>("Diameter",
        "Equivalent spherical diameter of the particle",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        SizeUnit.NANO_METER,
        SizeUnit.values(),
        SizeUnit.class,
        false,
        false,
        "npSphericalDiameter");

    this.npMassFraction
        = new DoubleParameter("Mass fraction",
        "Mass fraction of an element",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "npMassFraction");

    this.ionicConcentration
        = new QuantParameter<>("Ionic conc.",
        "Ionic standard concentration",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        ConcentrationUnit.MICROGRAM_PER_LITRE,
        ConcentrationUnit.getIonic(),
        ConcentrationUnit.class,
        false,
        false,
        "ionicConcentration");

    organize();
  }

  public ExperimentalSubConditions(ExperimentalSubConditions params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.element = params.element;
    this.npConcentration = params.npConcentration.copyWithoutChildren();
    this.npQuantificationApproach = params.npQuantificationApproach.copyWithoutChildren();
    this.npDensity = params.npDensity.copyWithoutChildren();
    this.npMassFraction = params.npMassFraction.copyWithoutChildren();
    this.npElementMass = params.npElementMass.copyWithoutChildren();
    this.npSphericalDiameter = params.npSphericalDiameter.copyWithoutChildren();

    this.ionicConcentration = params.ionicConcentration.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new ExperimentalSubConditions();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new ExperimentalSubConditions(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new ExperimentalSubConditions(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());

    return params;
  }

  public void setAllValues(ExperimentalSubConditions sub) {
    // dont copy all, just copy relevant!
    List<Parameter<?>> activePars = sub.listActiveParameters();

    if (activePars.contains(sub.npConcentration)) {
      npConcentration.trySetValue(sub.npConcentration);
    }
    if (activePars.contains(sub.npQuantificationApproach)) {
      npQuantificationApproach.trySetValue(sub.npQuantificationApproach);
    }
    if (activePars.contains(sub.npDensity)) {
      npDensity.trySetValue(sub.npDensity);
    }
    if (activePars.contains(sub.npMassFraction)) {
      npMassFraction.trySetValue(sub.npMassFraction);
    }
    if (activePars.contains(sub.npElementMass)) {
      npElementMass.trySetValue(sub.npElementMass);
    }
    if (activePars.contains(sub.npSphericalDiameter)) {
      npSphericalDiameter.trySetValue(sub.npSphericalDiameter);
    }
    if (activePars.contains(sub.ionicConcentration)) {
      ionicConcentration.trySetValue(sub.ionicConcentration);
    }
  }

  private void organize() {
    npDensity.setDecoration(new ButtonDecoration<>("Lookup density", "/img/density.png",
        new Functional() {
          @Override
          public void proceed() {

            SpCalDensity db = SpTool3Main.getRunTime().getDensityDatabase();
            List<SpCalDensity.Container> containers = db.lookup();

            FxEntryFactory<SpCalDensity.Container> factory = new FxEntryFactory.ContainerFactory();
            Dialog<List<SpCalDensity.Container>> dialog = new ChooseSingleFromListDialog<>(
                factory.create(containers),
                factory,
                false,
                true,
                false,
                "Values from SPCal density data base https://github.com/djdt/spcal",
                element.getSymbol(),
                FxStageButton.SELECT
            );

            Optional<List<SpCalDensity.Container>> result = dialog.showAndWait();
            if (result.isPresent()) {
              List<SpCalDensity.Container> results = result.get();
              if (!results.isEmpty()) {
                SpCalDensity.Container container = results.get(0);
                double ro = container.getDensity();
                if (ro > 0) {
                  QuantParameter<DensityUnit> npDensityQuant = ((QuantParameter<DensityUnit>) npDensity);
                  DensityUnit targetUnit = npDensityQuant.getUnit();
                  DensityUnit sourceUnit = DensityUnit.GRAM_PER_CM3;
                  double targetRo = sourceUnit.convert(ro, targetUnit);
                  npDensityQuant.setValue(targetRo);
                  SpTool3Main.getRunTime().getGuiParameterManager().notifySampleOrPopulationSelectionChange();
                }
              }
            }
          }
        }));

//    npMassFraction.setDecoration(new ButtonDecoration<>("Calculate mass fraction",
//        "/img/sumFormula.png",
//        new Functional() {
//          @Override
//          public void proceed() {
//
//          }
//        }));
  }


  public void organize(CalibratorRole role, SampleType sampleType) {

    /*
    Here, we must use the PUT methods, otherwise parameters will pile-up internally
     */

    // Clear old children or else we may mix role as sample and calibrator when switching between roles
    npQuantificationApproach.clearChildren();

    if (CalibratorRole.SAMPLE.equals(role)) {
      setParentParameters(npQuantificationApproach);

      npQuantificationApproach.putConditionalChild(ParticleQuantApproach.ESD,
          npDensity,
          npMassFraction);

    } else if (CalibratorRole.CALIBRATOR.equals(role)) {

      if (SampleType.IONIC.equals(sampleType)) {
        setParentParameters(
            ionicConcentration
        );

      } else if (SampleType.PARTICLE.equals(sampleType)) {

        setParentParameters(
            npQuantificationApproach,
            npConcentration
        );

        npQuantificationApproach.putConditionalChild(ParticleQuantApproach.ESD,
            npSphericalDiameter,
            npDensity,
            npMassFraction);

        npQuantificationApproach.putConditionalChild(ParticleQuantApproach.MASS,
            npElementMass);
      }
    }

    // I think we should update the child levels here and not wait for external call
    // or else the arrow are shown correctly only at second call
    for (Parameter<?> parentParameter : parentParameters) {
      parentParameter.determineChildLevels(0);
    }
  }

  @Override
  public FxParamSetImpl getObservableInstance() {
    // Without comment, date, ...
    return new FxParamSetSlimImpl(this);
  }

  @Override
  public FxParamSet getUneditableObservableInstance() {
    // Without comment, date, ...
    FxParamSetImpl fxParamSet = new FxParamSetSlimImpl(this);
    fxParamSet.setUneditable();
    return fxParamSet;
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

          case "npConcentration" -> npConcentration;
          case "npQuantificationApproach" -> npQuantificationApproach;
          case "npDensity" -> npDensity;
          case "npMassFraction" -> npMassFraction;
          case "npElementMass" -> npElementMass;
          case "npSphericalDiameter" -> npSphericalDiameter;

          case "ionicConcentration" -> ionicConcentration;

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
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.QUANT_SUB;
  }

  public Parameter<Double> getNpConcentration() {
    return npConcentration;
  }

  public ConcentrationUnit getNpConcentrationUnit() {
    return ((QuantParameter<ConcentrationUnit>) npConcentration).getUnit();
  }

  public void setNpConcentrationUnit(ConcentrationUnit unit) {
    ((QuantParameter<ConcentrationUnit>) npConcentration).setUnit(unit);
  }

  public Parameter<ParticleQuantApproach> getNpQuantificationApproach() {
    return npQuantificationApproach;
  }

  public Parameter<Double> getNpDensity() {
    return npDensity;
  }

  public DensityUnit getNpDensityUnit() {
    return ((QuantParameter<DensityUnit>) npDensity).getUnit();
  }

  public Parameter<Double> getNpSphericalDiameter() {
    return npSphericalDiameter;
  }

  public SizeUnit getNpSphericalDiameterUnit() {
    return ((QuantParameter<SizeUnit>) npSphericalDiameter).getUnit();
  }

  public void setNpSphericalDiameterUnit(SizeUnit unit) {
    ((QuantParameter<SizeUnit>) npSphericalDiameter).setUnit(unit);
  }

  public Parameter<Double> getNpElementMass() {
    return npElementMass;
  }

  public MassUnit getNpElementMassUnit() {
    return ((QuantParameter<MassUnit>) npElementMass).getUnit();
  }

  public Parameter<Double> getNpMassFraction() {
    return npMassFraction;
  }

  public Parameter<Double> getIonicConcentration() {
    return ionicConcentration;
  }

  public void setIonicConcentration(ConcentrationUnit unit) {
    ((QuantParameter<ConcentrationUnit>) ionicConcentration).setUnit(unit);
  }

  public double getIonicConc(ConcentrationUnit targetUnit) {
    ConcentrationUnit unit = (ConcentrationUnit) ((QuantParameter<?>) ionicConcentration).getUnit();
    double value = ionicConcentration.getValue();
    double result = unit.convert(value, targetUnit);
    return result;
  }


  public void setElement(dataModelNew.mz.Element element) {
    this.element = element;
  }

  ///  Check organize(CalibratorRole role, SampleType sampleType) as a reference what to cover here.
  public void setStyle(CalibratorRole role, SampleType sampleType, Label elementLabel) {
    elementLabel.setAlignment(Pos.CENTER);

    if (CalibratorRole.SAMPLE.equals(role)) {
      if (npQuantificationApproach.getValue().equals(ParticleQuantApproach.MASS)) {
        UiUtil.formatElementFieldGreen(elementLabel);
      } else {
        if (npDensity.getValue() > 0) {
          UiUtil.formatElementFieldGreen(elementLabel);
        } else {
          elementLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 18");
        }
      }

    } else if (CalibratorRole.CALIBRATOR.equals(role)) {

      if (SampleType.IONIC.equals(sampleType)) {
        if (ionicConcentration.getValue() > 0) {
          UiUtil.formatElementFieldGreen(elementLabel);
        } else {
          elementLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 18");
        }

      } else if (SampleType.PARTICLE.equals(sampleType)) {

        setParentParameters(npQuantificationApproach, npConcentration);

        if (npQuantificationApproach.getValue().equals(ParticleQuantApproach.ESD)) {
          if (npConcentration.getValue() > 0 && npSphericalDiameter.getValue() > 0 && npDensity.getValue() > 0) {
            UiUtil.formatElementFieldGreen(elementLabel);
          } else if (npSphericalDiameter.getValue() > 0 && npDensity.getValue() > 0) {
            UiUtil.formatElementFieldBlue(elementLabel);
          } else {
            elementLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 18");
          }
        } else {
          if (npConcentration.getValue() > 0 && npElementMass.getValue() > 0) {
            UiUtil.formatElementFieldGreen(elementLabel);
          } else if (npElementMass.getValue() > 0) {
            UiUtil.formatElementFieldBlue(elementLabel);
          } else {
            elementLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; -fx-font-size: 18");
          }
        }
      }
    }
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final ExperimentalSubConditions defaults = new ExperimentalSubConditions();

    if (npConcentration == null) {
      npConcentration = defaults.npConcentration;
    }
    if (npQuantificationApproach == null) {
      npQuantificationApproach = defaults.npQuantificationApproach;
    }

    if (npDensity == null) {
      npDensity = defaults.npDensity;
    }

    if (npMassFraction == null) {
      npMassFraction = defaults.npMassFraction;
    }

    if (ionicConcentration == null) {
      ionicConcentration = defaults.ionicConcentration;
    }

    if (npSphericalDiameter == null) {
      npSphericalDiameter = defaults.npSphericalDiameter;
    }

    if (npElementMass == null) {
      npElementMass = defaults.npElementMass;
    }

    if (element == null) {
      element = dataModelNew.mz.Element.UNKNOWN;
    }

    // adds the decoration
    organize();

  }


}
