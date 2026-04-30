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
import java.io.ObjectInputStream;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.MCSimIclShapeParameters;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamBundle;
import processing.parameterSets.ParamSet;
import processing.parameterSets.bundle.ElementBundle;
import processing.parameters.*;
import processing.parameters.BundleSupplier.ElementBundleSupplier;
import sandbox.montecarlo.GettablePDF;
import util.NF;

public class MCSimParticleParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "MCSimParticleParams";

  public final Parameter<Double> eventRate;
  public final Parameter<Double> meanEventSignal;
  public final Parameter<Double> eventSignalSD;
  public final Parameter<GettablePDF> particleMassDistribution;
  public final Parameter<Double> minimumEventSignal;
  public final Parameter<Double> paretoShape;
  public final Parameter<Double> paretoScale;

  private final Parameter<Integer> numberOfParticlePopulations;

  public final Parameter<MCSimIclShapeParameters> peakShape;
  public final Parameter<Double> v_mu;
  public final Parameter<Double> v_SD;
  public final Parameter<Double> y_mu;
  public final Parameter<Double> y_SD;
  public final Parameter<GettablePDF> y_Distribution;

  public final Parameter<Double> elementFractionUncertainty;

  public final Parameter<Double> isotopicFractionUncertainty;

  public final Parameter<Double> peakDelayUncertainty;

  public MCSimParticleParams() {
    super("Particle population parameters ", XML_ELEMENT_TAG);

    this.numberOfParticlePopulations = new SpawnControlParameter(
        "Particle composition",
        "Number of elements in this particle-type",
        1,
        new ElementBundleSupplier(),
        false,
        "numberOfParticlePopulations");

    this.eventRate = new DoubleParameter(
        "Event rate [NP/s]",
        "Specify the event rate of the population in events/second",
        15d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "eventRate"
    );

    this.meanEventSignal = new DoubleParameter(
        "Particle signal [cts/NP]",
        "Mean signal per particle in counts per event."
            + "\nNote that this value refers to the sum of all elements that a particle has",
        750d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "meanEventSignal"
    );

    this.eventSignalSD = new DoubleParameter(
        "Particle signal SD [cts/NP]",
        "Standard deviation of the mean signal per particle in counts per event",
        50d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "eventSignalSD"
    );

    minimumEventSignal = new DoubleParameter(
        "Signal cutoff [cts/NP]",
        """
            When using an exponential distribution, you may want to exclude
            peak heights below a certain value, e.g., peaks have to be at least 5 cts high.
            Note that this cutoff is normalized so that the most abundant isotope of all elements
            present in the particle will yield at least this many counts""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "minimumEventSignal"
    );

    // Ranville-like approach with a "power Law": a*exp(bx): DOI: https://doi.org/10.1039/d3en00425b - or
    // just a Pareto distribution.
    paretoScale = new DoubleParameter(
        "Scale parameter (k) [cts/NP]",
        """
            Scale parameter ('k' or 'x_min') of the Pareto distribution [cts/NP].
            This is the minimum possible value that the signal intensity can take for the ENTIRE particle.
            
            For a particle of mixed elemental and isotopic composition,
            the sum of all elements and their isotopes will be greater than k cts/NP.
            This definition is a consequence of how the Pareto distribution is parameterized,
            and also reflects the fact that environmental particles follow a Pareto distribution
            with respect to their size and not with respect to each element. 
            For the data generator, we replaced the size with the signal intensity (i.e., the mass)
            as otherwise we would have to assume the density, shape and composition of the particle.
            This may be replaced in a future release.
            
            Important: If you are using the particle distribution's mean and this scale parameter 
            to describe the Pareto distribution, take 2 notes:
            
            (1) Note that the distribution may take very long to actually converge to the expected value (the mean).
            Due to its exponential nature, it may take 1E9 and more events to approach the expected value.
            
            (2) Note that in the background the shape parameter 'a' is calculated as a = µ / (µ-k),
            where µ is the specified mean and k is the scale parameter.
            When 'a' approaches 1, the expected value gets more and more volatile and at a <= 1,
            it tends towards infinity. If you desire a better match between the specified mean µ
            and the simulated mean particle signal, you should try to increase the value of a.
            Usually, this can be achieved by increasing the scale parameter k or by lowering the mean µ""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "paretoScale"
    );

    paretoShape = new DoubleParameter(
        "Shape parameter (a) [-]",
        """
            Shape parameter (a) of the Pareto distribution [-].
            This is also known as the 'tail index'.
            
            Larger values of a cause the distribution to decay faster, 
            i.e., there will be more particles of smaller sizes.
            
            Note that for a<=1, the mean of the distribution is not defined and becomes infinitely large.
            It is recommended to choose a>1""",
        2.01d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "paretoShape"
    );

    this.particleMassDistribution = new ComboEnumParameter<>(
        "Particle mass distribution",
        "Distribution to model the particle mass",
        GettablePDF.LOGNORMAL,
        GettablePDF.values(),
        GettablePDF.class,
        false,
        "particleMassDistribution"
    );

    peakShape = new ComboEnumParameter<>("Peak shape (v, y)",
        """
            Choose how the ion cloud profiles are generated.
            The key parameters are:
            Linear plasma flow velocity 'v', diffusion coefficient 'D', distance of vaporization 'y'.\
            Each parameter has a mean value ± a standard deviation (SD) to simulate random ion clouds.
            The default values can be changed in the configuration of spTool
            (Edit -> Configuration -> *scroll down*).
            Alternatively, it is also possible to select custom values here in this method""",
        MCSimIclShapeParameters.DEFAULT,
        MCSimIclShapeParameters.validOptions(),
        MCSimIclShapeParameters.class,
        false,
        "peakShape");

    v_mu = new DoubleParameter("Plasma velocity (v) [m/s]",
        """
            Mean plasma gas linear velocity.
            Higher values lead to narrower and more symmetric peaks.
            Why? Higher velocities lead to a shorter residence time in the plasma
            which means that there is less time available for the ions to diffuse.
            A good starting point is v=15 m/s
            but values closer to 24 m/s are also plausible (to obtain narrower and symmetric peaks)""",
        15d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "v_mu");

    v_SD = new DoubleParameter("Plasma velocity (±SD) [m/s]",
        """
            Standard deviation of v.
            This number specifies how broadly the random numbers vary.
            A good starting point is SD=5""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "v_SD");

    y_mu = new DoubleParameter("Distance of vaporization (y) [mm]",
        """
            Mean distance from the sampler orifice where a particle is completely ionized.
            Higher values lead the broader peaks.
            Why? The model assumes an instantaneous vaporization of the particle.
            After that, the ions start to diffuse into the surrounding plasma, causing peak broadening.
            When the distance from the orifice is greater, the residence time in the plasma increases,
            which means that there is more time available for the ions to diffuse.
            A good starting point is y=10 mm""",
        9.5d,
        NF.D1C4,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "y_mu");

    y_SD = new DoubleParameter("Distance of vaporization (±SD) [mm]",
        """
            Standard deviation of y.
            This number specifies how broadly the random numbers vary.
            A good starting point is SD=3 mm""",
        3d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "y_SD");

    y_Distribution = new ComboEnumParameter<>("Probability for distance of vaporization",
        "Probability density function for the distance of vaporization",
        GettablePDF.LOGNORMAL,
        GettablePDF.getGaussianFamily(),
        GettablePDF.class,
        false,
        "y_Distribution");

    elementFractionUncertainty = new DoubleParameter("Intensity fraction uncertainty",
        """
            Relative uncertainty of the 'mass' fraction of the respective element.
            For the scope of the data generator, we use the intensity fraction as a proxy for the mass fraction
            because the relationship of mass and intensity strongly depends on instrument tuning and element
            A good starting point could be u=0.05.
            What does this parameter mean?
            Assuming a mass fraction of 0.75, a relative uncertainty of 5%
            implies that simulated values are normally distributed around 0.75,
            with 99.99% expected to fall between 0.731 and 0.769,
            which is calculated based on ±u/2, i.e., ±(5% / 2)  (i.e., ±0.01875) """,
        0.05,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "elementFractionUncertainty");

    isotopicFractionUncertainty = new DoubleParameter("Isotopic uncertainty",
        """
            Relative uncertainty of the isotopic composition of the respective element.
            A good starting point could be u=0.01.
            What does this parameter mean?
            Assuming a mass fraction of 0.75, a relative uncertainty of 5%  
            implies that simulated values are normally distributed around 0.75,
            with 99.99% expected to fall between 0.731 and 0.769,
            which is calculated based on ±u/2, i.e., ±(5% / 2)  (i.e., ±0.01875) """,
        0.01,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "isotopicFractionUncertainty");

    peakDelayUncertainty = new DoubleParameter("Peak delay uncertainty",
        "Absolute uncertainty for the peak delay [µs]",
        10d,
        NF.D1C1,
        TextFormatterOption.ASSURE_DOUBLE,
        true,
        "peakDelayUncertainty");

    organize();
  }

  /**
   * Do not forget to call .copyWithoutChildren(); on each!
   */
  public MCSimParticleParams(MCSimParticleParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.eventRate = params.eventRate.copyWithoutChildren();
    this.meanEventSignal = params.meanEventSignal.copyWithoutChildren();
    this.eventSignalSD = params.eventSignalSD.copyWithoutChildren();
    this.particleMassDistribution = params.particleMassDistribution.copyWithoutChildren();
    this.minimumEventSignal = params.minimumEventSignal.copyWithoutChildren();
    this.paretoScale = params.paretoScale.copyWithoutChildren();
    this.paretoShape = params.paretoShape.copyWithoutChildren();

    this.numberOfParticlePopulations = params.numberOfParticlePopulations.copyWithoutChildren();

    this.peakShape = params.peakShape.copyWithoutChildren();
    this.v_mu = params.v_mu.copyWithoutChildren();
    this.v_SD = params.v_SD.copyWithoutChildren();
    this.y_mu = params.y_mu.copyWithoutChildren();
    this.y_SD = params.y_SD.copyWithoutChildren();
    this.y_Distribution = params.y_Distribution.copyWithoutChildren();

    this.elementFractionUncertainty = params.elementFractionUncertainty.copyWithoutChildren();
    this.isotopicFractionUncertainty = params.isotopicFractionUncertainty.copyWithoutChildren();
    this.peakDelayUncertainty = params.peakDelayUncertainty.copyWithoutChildren();

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new MCSimParticleParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new MCSimParticleParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new MCSimParticleParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Add all PARENT (not the depending) parameters!
    super.setParentParameters(
        eventRate,
        particleMassDistribution,
        numberOfParticlePopulations,
        peakShape,
        y_Distribution,
        elementFractionUncertainty,
        isotopicFractionUncertainty);

    particleMassDistribution.addConditionalChild(GettablePDF.GAUSSIAN,
        meanEventSignal, eventSignalSD);
    particleMassDistribution.addConditionalChild(GettablePDF.LOGNORMAL,
        meanEventSignal, eventSignalSD);

    particleMassDistribution.addConditionalChild(GettablePDF.EXPONENTIAL,
        meanEventSignal, minimumEventSignal);

    particleMassDistribution.addConditionalChild(GettablePDF.PARETO,
        paretoScale, paretoShape);

    particleMassDistribution.addConditionalChild(GettablePDF.PARETO_MU,
        meanEventSignal, paretoScale);

    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, v_mu);
    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, v_SD);
    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, y_mu);
    peakShape.addConditionalChild(MCSimIclShapeParameters.CUSTOM, y_SD);
  }

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.SPICPMS_MC_SIMUL_NP_POPULATION;
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

          case "eventRate" -> eventRate;
          case "meanEventSignal" -> meanEventSignal;
          case "eventSignalSD" -> eventSignalSD;
          case "particleMassDistribution" -> particleMassDistribution;
          case "minimumEventSignal" -> minimumEventSignal;
          case "paretoShape" -> paretoShape;
          case "paretoScale" -> paretoScale;

          case "peakShape" -> peakShape;
          case "v_mu" -> v_mu;
          case "v_SD" -> v_SD;
          case "y_mu" -> y_mu;
          case "y_SD" -> y_SD;
          case "y_Distribution" -> y_Distribution;

          case "numberOfParticlePopulations" -> numberOfParticlePopulations;

          case "massFractionUncertainty" -> elementFractionUncertainty;
          case "isotopicFractionUncertainty" -> isotopicFractionUncertainty;
          case "peakDelayUncertainty" -> peakDelayUncertainty;

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

  public List<ElementBundle> getElementBundles() {
    List<ElementBundle> elementBundles = new ArrayList<>();

    if (numberOfParticlePopulations instanceof SpawnControlParameter) {
      SpawnControlParameter spawnParam = (SpawnControlParameter) numberOfParticlePopulations;

      List<ParamBundle> bundles = spawnParam.getActiveBundlesForProcessing();

      for (ParamBundle bundle : bundles) {
        if (bundle instanceof ElementBundle) {
          elementBundles.add((ElementBundle) bundle);
        }
      }
    }
    return elementBundles;
  }


  @Override
  public String getTooltip() {
    StringBuilder builder = new StringBuilder("Input elements in " + label.getValue() + ":\n");
    for (ElementBundle elementBundle : getElementBundles()) {
      builder.append(elementBundle.elementHeaderParameter.getValueAsString()).append("\n");
    }
    builder.append("\n");
    return builder.toString();
  }


  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default deserialization
    in.defaultReadObject();

    // default supplier
    final MCSimParticleParams defaults = new MCSimParticleParams();

    // backwards compatibility
    if (peakShape != null) {
      if (MCSimIclShapeParameters.isDeprecatedEnum(peakShape.getValue())) {
        peakShape.setValue(MCSimIclShapeParameters.DEFAULT);
      }
      if (MCSimIclShapeParameters.isDeprecatedEnum(peakShape.getDefaultValue())) {
        peakShape.setValue(MCSimIclShapeParameters.DEFAULT);
      }


    }

  }

}
