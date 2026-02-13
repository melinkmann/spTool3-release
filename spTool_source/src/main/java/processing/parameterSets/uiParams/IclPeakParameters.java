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
import processing.options.PDF;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSetImpl;
import processing.parameterSets.ParamSet;
import processing.parameterSets.XmlInstanceDictionary;
import processing.parameterSets.uiParams.Viewers.IclPeakViewer;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.DoubleSlideParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import sandbox.montecarlo.PeakFunction;
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
 */


/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class IclPeakParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "IclPeakViewer";

  private final Parameter<Double> diffusionCoefficient;
  private final Parameter<Double> plasmaVelocity;
  private final Parameter<Double> yPosition;

  private final Parameter<Double> peakPosition;
  private final Parameter<Double> dwellTime;
  private final Parameter<Double> viewerTimeWindow;

  private final Parameter<Double> signalLevel;
  private final Parameter<Double> backgroundSignalLevel;
  private final Parameter<Boolean> resample;
  private final Parameter<Boolean> usePoissonRngSeed;
  private final Parameter<Integer> poissonRngSeed;
  private final Parameter<PDF> samplingDistribution;
  private final Parameter<Double> siaShape;

  private final Parameter<Boolean> showDTGrid;
  private final Parameter<Double> gridIntegrationTime;

  public IclPeakParameters() {
    super("Ion cloud viewer parameters", XML_ELEMENT_TAG);

//    this.diffusionCoefficient = new DoubleParameter(
//        "D",
//        "Diffusion coefficient D. Good starting point: D=0.0008",
//        0.0008,
//        NF.D1C6,
//        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
//        "diffusionCoefficient");
//
//    this.plasmaVelocity = new DoubleParameter(
//        "v",
//        "Linear plasma velocity: v. Good starting point: v=24",
//        24d,
//        NF.D1C2,
//        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
//        "plasmaVelocity");
//
//    this.yPosition = new DoubleParameter(
//        "y",
//        "Distance from point of atomization to the sampler cone: y. "
//            + "Good starting point: y=0.015",
//        0.0150,
//        NF.D1C4,
//        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
//        "yPosition");

    this.diffusionCoefficient = new DoubleSlideParameter(
        "D [cm2/s]",
        "Diffusion coefficient D. Good starting point: D=60 cm2/s, (min=0.1, max=200, increment=2)",
        60d,
        NF.D1C1,
        2,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        0.1,
        200,
        false,
        "diffusionCoefficient");

    this.plasmaVelocity = new DoubleSlideParameter(
        "v [m/s]",
        "Linear plasma velocity: v. Good starting point: v=24 m/s, (min=1, max=100, increment=0.5)",
        24d,
        NF.D1C1,
        0.5,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        1,
        100,
        false,
        "plasmaVelocity"
    );

    this.yPosition = new DoubleSlideParameter(
        "y [mm]",
        "Distance from point of atomization to the sampler cone: y. "
            + "Good starting point: y=15mm, (min=0.5, max=100, increment=1)",
        15d,
        NF.D1C1,
        1d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        0.5,
        100d,
        false,
        "yPosition");

    this.peakPosition = new DoubleSlideParameter(
        "t Peak [µs]",
        "Relative time point at which the peak it detected, e.g., 5000 µs",
        5000d,
        NF.D1C0,
        5,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        1,
        10_000,
        false,
        "peakPosition");

    this.dwellTime = new DoubleParameter(
        "DT [µs]",
        "Time resolution of the plot",
        1d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "dwellTime");

    this.viewerTimeWindow = new DoubleParameter(
        "± Δt [µs]",
        "Time window around zero (±) that is shown of the plot",
        600d,
        NF.D1C1,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "viewerTimeWindow");

    this.resample = new BooleanParameter(
        "Signal",
        "Resample",
        "Resample the signal with a random number generator",
        false,
        false,
        "resample"
    );

    this.samplingDistribution = new ComboEnumParameter<>(
        "Distribution",
        "Choose the distribution for the random number resampling",
        PDF.POISSON,
        PDF.getPoissons(),
        PDF.class,
        false,
        "samplingDistribution"
    );

    this.usePoissonRngSeed = new BooleanParameter(
        "RNG",
        "Set seed",
        "Keep the view consistent by forcing the the Poisson random number generator (rng)"
            + "\nto always produce the same sequence of random numbers:"
            + "\nThs is done by setting the seed number of the Poisson random number generator (rng)."
            + "\nWhen a seed number is provided, the rng will still produce sequence of pseudo-random numbers."
            + "\nFor the same seed, however, the sequence will always contain the same numbers."
            + "\nThese numbers still reflect a random process but the order of the sequence becomes deterministic.",
        false,
        false,
        "usePoissonRngSeed"
    );

    this.poissonRngSeed = new IntegerParameter(
        "RNG seed",
        "Set a large integer number as a seed for the random number generator that resamples the peak."
            + "\nThis will lead to the same 'random' outcome each time",
        42,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "poissonRngSeed"
    );

    this.signalLevel = new DoubleParameter(
        "Area [cts]",
        "Specify the peak area",
        500d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "signalLevel"
    );

    this.backgroundSignalLevel = new DoubleParameter(
        "BG [cts/DT]",
        "Specify the background (bg) signal level",
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "backgroundSignalLevel"
    );

    this.siaShape = new DoubleParameter(
        "SIA shape",
        "Specify the single ion area shape parameter",
        0.47,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "siaShape"
    );

    this.showDTGrid = new BooleanParameter(
        "DT Grid",
        "Show",
        "Show the grid of the dwell times."
            + "\nNote that the true peak is shifted to the right when it is integrated"
            + "\nand shown on the grid of the dwell times."
            + "\nUse this option to visualize how the 'true' peak shape is transformed by integration",
        false,
        false,
        "showDTGrid"
    );

    this.gridIntegrationTime = new DoubleParameter(
        "Grid DT [µs]",
        "Time resolution of the highly resolved plot to indicate integration",
        dwellTime.getValue() / 2,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "gridIntegrationTime");

    organize();
  }

  public IclPeakParameters(IclPeakParameters iclPeakParameters) {
    super(iclPeakParameters.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakParameters.getCommentParameter());
    this.diffusionCoefficient = iclPeakParameters.diffusionCoefficient.copyWithoutChildren();
    this.plasmaVelocity = iclPeakParameters.plasmaVelocity.copyWithoutChildren();
    this.yPosition = iclPeakParameters.yPosition.copyWithoutChildren();
    this.peakPosition = iclPeakParameters.peakPosition.copyWithoutChildren();
    this.dwellTime = iclPeakParameters.dwellTime.copyWithoutChildren();
    this.viewerTimeWindow = iclPeakParameters.viewerTimeWindow.copyWithoutChildren();
    this.resample = iclPeakParameters.resample.copyWithoutChildren();
    this.samplingDistribution = iclPeakParameters.samplingDistribution.copyWithoutChildren();
    this.signalLevel = iclPeakParameters.signalLevel.copyWithoutChildren();
    this.backgroundSignalLevel = iclPeakParameters.backgroundSignalLevel.copyWithoutChildren();
    this.siaShape = iclPeakParameters.siaShape.copyWithoutChildren();
    this.showDTGrid = iclPeakParameters.showDTGrid.copyWithoutChildren();
    this.gridIntegrationTime = iclPeakParameters.gridIntegrationTime.copyWithoutChildren();
    this.usePoissonRngSeed = iclPeakParameters.usePoissonRngSeed.copyWithoutChildren();
    this.poissonRngSeed = iclPeakParameters.poissonRngSeed.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new IclPeakParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new IclPeakParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new IclPeakParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        diffusionCoefficient,
        plasmaVelocity,
        yPosition,
        peakPosition,
        dwellTime,
        viewerTimeWindow,
        signalLevel,
        backgroundSignalLevel,
        resample,
        showDTGrid
        );

    resample.addConditionalChild(true, samplingDistribution);
    samplingDistribution.addConditionalChild(PDF.COMPOUND_POISSON, siaShape);
    resample.addConditionalChild(true, usePoissonRngSeed);
    usePoissonRngSeed.addConditionalChild(true, poissonRngSeed);

    showDTGrid.addConditionalChild(true, gridIntegrationTime);
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

          case "diffusionCoefficient" -> diffusionCoefficient;
          case "plasmaVelocity" -> plasmaVelocity;
          case "yPosition" -> yPosition;
          case "dwellTime" -> dwellTime;

          case "peakPosition" -> peakPosition;

          case "viewerTimeWindow" -> viewerTimeWindow;

          case "signalLevel" -> signalLevel;
          case "resample" -> resample;
          case "samplingDistribution" -> samplingDistribution;
          case "siaShape" -> siaShape;

          case "usePoissonRngSeed" -> usePoissonRngSeed;
          case "poissonRngSeed" -> poissonRngSeed;

          case "showDTGrid" -> showDTGrid;
          case "gridIntegrationTime" -> gridIntegrationTime;

          case "backgroundSignalLevel" -> backgroundSignalLevel;

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
    return new IclPeakViewer(this);
  }

  //

  public PeakFunction getPeakFunction() {
    return new PeakFunction(
        diffusionCoefficient.getValue() / 1E4,
        yPosition.getValue() / 1E3,
        plasmaVelocity.getValue());
  }

  public double getArea() {
    return signalLevel.getValue();
  }

  public double getDwellTime() {
    return dwellTime.getValue();
  }


  public double getPeakPosition() {
    return peakPosition.getValue();
  }

  public double getViewerTimeWindow() {
    return viewerTimeWindow.getValue();
  }

  public PDF getPDF() {
    return samplingDistribution.getValue();
  }

  public boolean isResample() {
    return resample.getValue();
  }

  public double getSiaShape() {
    return siaShape.getValue();
  }

  public boolean isShowDTGrid() {
    return showDTGrid.getValue();
  }

  public double getGridIntegrationTime() {
    return gridIntegrationTime.getValue();
  }

  public boolean isUseRNGSeed() {
    return usePoissonRngSeed.getValue();
  }

  public long getSeed() {
    return (long) poissonRngSeed.getValue();
  }

  public double getBackgroundLevel() {
    return backgroundSignalLevel.getValue();
  }
}
