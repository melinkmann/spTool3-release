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

import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.DeadTimeModel;
import processing.options.IsotopeConflictOption;
import processing.options.MonteCarloOscillation;
import processing.options.PDF;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamBundle;
import processing.parameterSets.ParamSet;
import processing.parameterSets.bundle.SiaBundle;
import processing.parameters.*;
import processing.parameters.BundleSupplier.SiABundleSupplier;
import util.NF;

public class MCSimGeneralParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "MCSimGeneralParams";

  public final Parameter<String> sampleName;
  public final Parameter<Double> microDT;
  public final Parameter<Double> macroDT;
  public final Parameter<Double> duration;
  public final Parameter<DeadTimeModel> deadTimeModel;
  public final Parameter<Double> deadTime;
  public final Parameter<MonteCarloOscillation> oscillation;

  public final Parameter<Double> oscillationPeriod;
  public final Parameter<Double> oscillationAmplitude;
  public Parameter<Double> expRampFactor;

  public final Parameter<PDF> detectorDistribution;

  public final Parameter<Double> defaultSiaShapeParameter;
  public final Parameter<Integer> siaExceptions;

  public final Parameter<Double> flickerNoiseCoefficient;
  public final Parameter<Double> shotNoiseCoefficient;

  public final Parameter<IsotopeConflictOption> isotopeConflictOption;

  // Boolean: apply dilution  -- Double: dilution (10x = divide by ten)
  public Parameter<Boolean> applyDilution;
  public Parameter<Double> dilutionFactor;

  public Parameter<Boolean> evenPeakSpacing;
  public Parameter<Double> estimatedPeakWidth;

  public MCSimGeneralParams() {
    super("Data generator main parameters", XML_ELEMENT_TAG);

    this.sampleName = new StringParameter(
        "Sample name",
        "Name that will be assigned to the sample that is created from this method",
        "Synthetic data",
        TextFormatterOption.ALL_PASS,
        true,
        false,
        "sampleName"
    );

    this.microDT = new DoubleParameter(
        "Grid time (GT) [µs]",
        """
            Low-level time grid for the data generator. 
            This "micro-scale dwell time" is used to construct a time grid at the beginning of the data generation.
            On this grid, the fundamental calculations are performed which include
            a. calculating the effect of detector dead time
            b. sampling and integrating the mathematical equation of the event peaks.
            After performing these tasks, spTool merges the grid into the dwell time.
            For example: If GT = 1 µs and DT = 100 µs, spTool sums up 100 data points of the initial grid
            to generate data points on the dwell time level.
            
            For the math, a recommend value is GT = 1 - 10 µs.
            Note that the dwell time must be divisible by the grid time,
            e.g., DT = 105 µs --> grid time cannot be GT = 4 µs but should be 1 µs, 3 µs or 5 µs.
            
            Choose value closer to 10 µs if data generator runs slowly or encounters memory (RAM) issues""",
        1d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "microDT"
    );

    this.macroDT = new DoubleParameter(
        "Temporal resolution (TR) [µs]",
        """
            Temporal resolution (TR). For quadrupole instruments, this indicates the dwell time (DT).
            For time-of-flight instruments, TR indicates the time between data points in the output data.
            Choose this value to match the experiment that you want to simulate""",
        100d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "macroDT"
    );

    this.duration = new DoubleParameter(
        "Duration [s]",
        "Run time of the experiment that you want to simulate",
        60d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "duration"
    );

    this.deadTimeModel = new ComboEnumParameter<>(
        "Dead time model",
        "Choose the model that you would like to use to simulate detector dead time effects",
        DeadTimeModel.NON_PARALYZING,
        DeadTimeModel.values(),
        DeadTimeModel.class,
        true,
        "deadTimeModel"
    );

    this.deadTime = new DoubleParameter(
        "Dead time [ns]",
        "Detector dead time",
        40d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "deadTime"
    );

    this.oscillation = new ComboEnumParameter<>(
        "Fluctuation (BG)",
        "Add oscillation to the background or use a steady background signal",
        MonteCarloOscillation.STEADY,
        MonteCarloOscillation.values(),
        MonteCarloOscillation.class,
        true,
        "oscillation"
    );

    this.oscillationPeriod = new DoubleParameter(
        "Oscillation period [s]",
        "Oscillation period (full sine cycle) of the background",
        5d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "oscillationPeriod"
    );

    this.oscillationAmplitude = new DoubleParameter(
        "Oscillation amplitude [-]",
        "Adjust the oscillation amplitude of the background",
        0.025d,
        NF.D1C4,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "oscillationAmplitude"
    );

    this.expRampFactor = new DoubleParameter(
        "Ramp factor [-]",
        "Background will increase from the mean background level up to factor·mean." +
            "\nThen, the background is normalized so that the specified mean is obtained",
        2d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "expRampFactor"
    );

    this.detectorDistribution = new ComboEnumParameter<>(
        "Signal randomization",
        """
            Decide if the intensities are modeled as a Poisson distribution (quadrupole)
            or a Compound Poisson distribution for TOFMS.
            Overdispersion, i.e., excess variance that exceeds variance of a pure poisson model,
            can be included. The respective shot and flicker noise parameters 
            need to be estimated from raw data or taken from literature""",
        PDF.POISSON,
        PDF.getResampling(),
        PDF.class,
        false,
        "detectorDistribution"
    );

    this.defaultSiaShapeParameter = new DoubleParameter("Default SIA shape",
        "The single ion area histogram is assumed to have a lognormal distribution."
            + "\nIts location parameter mu is calculated to yield an expected value of one."
            + "\nThe shape parameter is usually called 'sigma' and is on the order of 0.47",
        0.47,
        NF.D1C3,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "defaultSiaShapeParameter");

    this.siaExceptions = new SpawnControlParameter(
        "Specific SIA shapes",
        "Give an element-specific single ion are (SIA) shape parameter,"
            + "\n i.e., the 'sigma' parameter in a lognormal distribution."
            + "\n The default/a good reference value is 0.47",
        0,
        new SiABundleSupplier(),
        true,
        "siaExceptions");

    shotNoiseCoefficient = new DoubleParameter("Shot noise (s)",
        "Shot noise coefficient ('s'), i.e., pure Poisson noise."
            + "\nThis parameter is supplemented by the flicker noise ('f')."
            + "\nShot noise represents Poisson noise, while flicker noise reflects overdispersion."
            + "\nBoth types of noise are used to model the standard deviation (SD)."
            + "\nSD = root{ (f·µ)^2 + s·µ }."
            + "\nIf f=0 and s=1, the model behaves as pure Poisson-Normal approximation."
            + "\n)f s=0, then a reflects the relative standard deviation (RSD, i.e., a=1.2 means RSD=120%",
        1d,
        NF.D1C4,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "shotNoiseCoefficient");

    flickerNoiseCoefficient = new DoubleParameter("Flicker noise",
        "Flicker noise coefficient ('f')."
            + "\nThis parameter is supplemented by the shot noise coefficient ('s'), i.e., pure Poisson " +
            "noise."
            + "\nShot noise represents Poisson noise, while flicker noise reflects overdispersion."
            + "\nBoth types of noise are used to model the standard deviation (SD)."
            + "\nSD = root{ (f·µ)^2 + s·µ }."
            + "\nIf f=0 and s=1, the model behaves as pure Poisson-Normal approximation."
            + "\n)f s=0, then a reflects the relative standard deviation (RSD, i.e., a=1.2 means RSD=120%",
        0.18,
        NF.D1C4,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "flickerNoiseCoefficient");

    this.isotopeConflictOption = new ComboEnumParameter<>(
        "Isotope conflict",
        """
            Specify how 'isotope conflicts' are resolved.
            These conflicts occur when 2 isotopes have the same isotopic number, i.e., nominal mass.
            You may 
            
            a. do nothing: spTool will keep both isotopes as if the
            instrument had sufficient spectral resolution to separate, e.g., 138Ba and 138La.
            
            b. combine and keep both: For all isotopes with the same isotopic number, 
            data are combined (i.e., intensities are added before the randomization).
            As a result, all conflicting isotopes share the same raw data series.
            This behavior is similar to an acquisition, where the spectral resolution was insufficient
            to resolve the nominal masses. Without further processing, e.g., isotope deconvolution,
            it is not possible to know the difference. For the data generation, spTool reports
            both 138Ba and 138La and lets the user decide what to 'assume' was actually there
            """,
        //        c. only use the 'default' isotope which is specified in the configuration of spTool.
        //        Note that the configuration is a global parameter and is not stored with each method.
        IsotopeConflictOption.COMBINE,
        IsotopeConflictOption.values(),
        IsotopeConflictOption.class,
        false,
        "isotopeConflictOption"
    );

    this.applyDilution = new BooleanParameter(
        "Dilution",
        "Apply dilution factor",
        "Dilute the sample virtually by reducing background and particle number",
        false,
        true,
        "applyDilution");

    this.dilutionFactor = new DoubleParameter("Dilution factor",
        """
            Dilution factor: 10 means a "10x dilution", 
            i.e., background signal and NP number is divided by 10""",
        1d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "dilutionFactor");

    this.evenPeakSpacing = new BooleanParameter(
        "Peak spacing",
        "High capacity mode",
        """
            When optimizing for peak shape, it is recommendable to use this option.
            It forces peaks to have an equal distance between each other instead of the
            usual Poisson process distances. This allows to have more peaks in the same time
            which accelerates computation. The most time consuming part for long sample durations
            is computation and handling of the millions of BG data points. By fitting more peaks into
            the same sample duration, the efficiency is increased""",
        false,
        true,
        "evenPeakSpacing");

    this.estimatedPeakWidth = new DoubleParameter("Estimated width [ms]",
        """
            The distance between equally spaced peaks will be roughly 1.5 x this estimated width.
            There is some randomness added to the exact spacing in order to allow the peaks
            to shift against the time grid""",
        10d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "estimatedPeakWidth");


    organize();
  }

  /**
   * Do not forget to call .copyWithoutChildren(); on each!
   */
  public MCSimGeneralParams(MCSimGeneralParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.sampleName = params.sampleName.copyWithoutChildren();
    this.microDT = params.microDT.copyWithoutChildren();
    this.macroDT = params.macroDT.copyWithoutChildren();
    this.duration = params.duration.copyWithoutChildren();
    this.deadTimeModel = params.deadTimeModel.copyWithoutChildren();
    this.deadTime = params.deadTime.copyWithoutChildren();
    this.oscillation = params.oscillation.copyWithoutChildren();
    this.oscillationPeriod = params.oscillationPeriod.copyWithoutChildren();
    this.oscillationAmplitude = params.oscillationAmplitude.copyWithoutChildren();
    this.expRampFactor = params.expRampFactor.copyWithoutChildren();
    this.detectorDistribution = params.detectorDistribution.copyWithoutChildren();
    this.defaultSiaShapeParameter = params.defaultSiaShapeParameter.copyWithoutChildren();
    this.siaExceptions = params.siaExceptions.copyWithoutChildren();
    this.shotNoiseCoefficient = params.shotNoiseCoefficient.copyWithoutChildren();
    this.flickerNoiseCoefficient = params.flickerNoiseCoefficient.copyWithoutChildren();
    this.isotopeConflictOption = params.isotopeConflictOption.copyWithoutChildren();
    this.applyDilution = params.applyDilution.copyWithoutChildren();
    this.dilutionFactor = params.dilutionFactor.copyWithoutChildren();
    this.evenPeakSpacing = params.evenPeakSpacing.copyWithoutChildren();
    this.estimatedPeakWidth = params.estimatedPeakWidth.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new MCSimGeneralParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MCSimGeneralParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MCSimGeneralParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Add all PARENT (not the depending) parameters!
    super.setParentParameters(
        sampleName,
        microDT,
        macroDT,
        duration,
        isotopeConflictOption,
        deadTimeModel,
        detectorDistribution,
        applyDilution,
        oscillation,
        evenPeakSpacing
    );

    applyDilution.addConditionalChild(true, dilutionFactor);

    deadTimeModel.addConditionalChild(DeadTimeModel.PARALYZING, deadTime);
    deadTimeModel.addConditionalChild(DeadTimeModel.NON_PARALYZING, deadTime);

    detectorDistribution.addConditionalChild(PDF.COMPOUND_POISSON,
        defaultSiaShapeParameter,
        siaExceptions);

    detectorDistribution.addConditionalChild(PDF.POISSON_OVERDISPERSED,
        shotNoiseCoefficient,
        flickerNoiseCoefficient
    );

    detectorDistribution.addConditionalChild(PDF.COMPOUND_POISSON_OVERDISPERSED,
        shotNoiseCoefficient,
        flickerNoiseCoefficient,
        defaultSiaShapeParameter,
        siaExceptions);

    oscillation.addConditionalChild(MonteCarloOscillation.SINE,
        oscillationPeriod,
        oscillationAmplitude);

    oscillation.addConditionalChild(MonteCarloOscillation.EXP,
        expRampFactor);

    oscillation.addConditionalChild(MonteCarloOscillation.LINE,
        expRampFactor);

    evenPeakSpacing.addConditionalChild(true, estimatedPeakWidth);

    isotopeConflictOption.setDecoration(new ImageDecoration<>("/img/combineIsotopes.png"));
  }

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.SPICPMS_MC_SIMUL;
  }

  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);

      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // Read the settings from the parameter
        String paramID = element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE);

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (paramID) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "sampleName" -> sampleName;
          case "microDT" -> microDT;
          case "macroDT" -> macroDT;
          case "duration" -> duration;
          case "deadTimeModel" -> deadTimeModel;
          case "detectorDistribution" -> detectorDistribution;
          case "siaExceptions" -> siaExceptions;
          case "defaultSiaShapeParameter" -> defaultSiaShapeParameter;
          case "oscillation" -> oscillation;
          case "oscillationPeriod" -> oscillationPeriod;
          case "oscillationAmplitude" -> oscillationAmplitude;
          case "expRampFactor" -> expRampFactor;
          case "shotNoiseCoefficient" -> shotNoiseCoefficient;
          case "flickerNoiseCoefficient" -> flickerNoiseCoefficient;
          case "isotopeConflictOption" -> isotopeConflictOption;
          case "applyDilution" -> applyDilution;
          case "dilutionFactor" -> dilutionFactor;
          case "evenPeakSpacing" -> evenPeakSpacing;
          case "estimatedPeakWidth" -> estimatedPeakWidth;


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

  // Get parameters for processing:

  public List<SiaBundle> getSiaBundles() {
    List<SiaBundle> elementBundles = new ArrayList<>();

    if (siaExceptions instanceof SpawnControlParameter) {
      SpawnControlParameter spawnParam = (SpawnControlParameter) siaExceptions;

      List<ParamBundle> bundles = spawnParam.getActiveBundlesForProcessing();

      for (ParamBundle bundle : bundles) {
        if (bundle instanceof SiaBundle) {
          elementBundles.add((SiaBundle) bundle);
        }
      }
    }
    return elementBundles;
  }

  @Override
  public String getTooltip() {
    StringBuilder builder = new StringBuilder("SiA exception elements in " + label.getValue() + ":\n");
    for (SiaBundle siaBundle : getSiaBundles()) {
      builder.append(siaBundle.elementHeaderParameter.getValueAsString()).append("\n");
    }
    builder.append("\n");
    return builder.toString();
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final MCSimGeneralParams defaults = new MCSimGeneralParams();

    // Fix missing fields from old serialized versions: we have to use
    if (applyDilution == null) {
      this.applyDilution = defaults.applyDilution;
    }

    if (dilutionFactor == null) {
      this.dilutionFactor = defaults.dilutionFactor;
    }

    if (expRampFactor == null) {
      this.expRampFactor = defaults.expRampFactor;
    }

    if (evenPeakSpacing == null) {
      this.evenPeakSpacing = defaults.evenPeakSpacing;
    }

    if (estimatedPeakWidth == null) {
      this.estimatedPeakWidth = defaults.estimatedPeakWidth;
    }


  }

}
