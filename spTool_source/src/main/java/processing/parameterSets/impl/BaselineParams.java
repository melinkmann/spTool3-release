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

import math.stat.MeasureOfLocation;
import math.stat.MeasureOfSpread;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.BaselineDynamic;
import processing.options.CompoundPoissonModel;
import processing.options.DistributionModel;
import processing.options.OutlierModel;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.*;
import util.NF;

public class BaselineParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "BaselineParams";

  private final Parameter<BaselineDynamic> baselineDynamics;
  private Parameter<Double> baselineWidthPerSegment;

  // Parameter: Only decide what model is to be used. Actual significance is chosen later at search.
  private final Parameter<DistributionModel> generalDistributionApproach;
  private final Parameter<Double> poissonNormalCutoff;
  private final Parameter<DistributionModel> poissonChoice;
  private final Parameter<DistributionModel> gaussianChoice;

  private final Parameter<MeasureOfLocation> measureOfLocationPoisson;
  private final Parameter<MeasureOfLocation> measureOfLocationGauss;
  private final Parameter<MeasureOfSpread> measureOfSpreadGauss;

  private final Parameter<CompoundPoissonModel> compoundPoissonModel;
  private final Parameter<Double> siaShape;
  private Parameter<Boolean> preferEmpiricalSIA;

  // Outlier test
  private final Parameter<OutlierModel> outlierTestTypeGauss;
  private final Parameter<Double> outlierFactorGaussStart;
  private final Parameter<Integer> outlierGaussMaxNonSmartIncrements;
  private final Parameter<Double> outlierFactorMax;
  private final Parameter<Double> outlierFineIncrements;
  private final Parameter<Double> outlierDefaultMeanLowerCap;
  private final Parameter<Double> outlierStdDevByMeanSmallerThan;
  private final Parameter<Double> outlierRosnerAlpha;
  private final Parameter<Double> outlierRosnerMaxPercent;

  private final Parameter<OutlierModel> outlierTestTypePoisson;
  private final Parameter<MeasureOfLocation> outlierMeasureOfLocationPoisson;
  private final Parameter<Double> outlierFactorPoissonStart;
  private final Parameter<Integer> outlierPoissonMaxNonSmartIncrements;
  private final Parameter<Boolean> outlierPoissonOffsetBoolean;
  private final Parameter<Double> outlierPoissonOffsetValue;
  private Parameter<Boolean> applyPoissonContinuityCorrection;
  private Parameter<Double> poissonContinuityCorrection;


  public BaselineParams() {
    super("Baseline parameters", XML_ELEMENT_TAG);

    this.baselineDynamics = new ComboEnumParameter<>(
        "Baseline",
        "Use interpolation or segments if there are fluctuations in the data",
        BaselineDynamic.CONSTANT,
        BaselineDynamic.values(),
        BaselineDynamic.class,
        false,
        "baselineDynamics"
    );

    this.baselineWidthPerSegment = new DoubleParameter(
        "Segment width [msId]",
        "Width defines data points per segment of the dynamic baseline [msId]",
        100d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "baselineWidthPerSegment"
    );

    // Parameter: Only decide what model is to be used. Actual significance is chosen later at search.
    this.generalDistributionApproach = new ComboEnumParameter<>(
        "Select model",
        "Specify how spTool decides which model it will use:" +
            "1) Automatic: Use Poisson model if mean BG µ < 10" +
            "2) Highest: Calculate Poisson and Gaussian model. Pick the one with the higher threshold" +
            "3) Gaussian: Force using Gaussian model." +
            "4) Poisson: Force using Poisson model",
        DistributionModel.THRESHOLD,
        DistributionModel.listGeneralOptions(),
        DistributionModel.class,
        false,
        "generalDistributionApproach"
    );

    this.poissonNormalCutoff = new DoubleParameter(
        "Poisson cutoff [cts/DT]",
        "If µ < cutoff: Poisson Model is chosen or else Gaussian.",
        10D,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "poissonNormalCutoff"
    );

    this.poissonChoice = new ComboEnumParameter<>(
        "Poisson model",
        "Baseline model in case Poisson distribution is chosen",
        DistributionModel.POISSON_CURRIE,
        DistributionModel.listOptionsForPoisson(),
        DistributionModel.class,
        false,
        "poissonChoice"
    );

    compoundPoissonModel = new ComboEnumParameter<>(
        "Compound P. algorithm",
        "Choose how the compound Poisson is calculated",
        CompoundPoissonModel.LOOKUP_TABLE,
        CompoundPoissonModel.values(),
        CompoundPoissonModel.class,
        true,
        "compoundPoissonModel"
    );

    this.siaShape = new DoubleParameter(
        "SIA shape",
        """
            Shape parameter of the single ion are (SIA) distribution of the detector
             assuming lognormal distribution.
             The shape parameter refers to the lognormal distribution
             and not the empirical standard deviation of the SIA data""",
        0.47,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "siaShape"
    );

    preferEmpiricalSIA = new BooleanParameter(
        "SIA",
        "Use empirical shape if available",
        """
            Shape parameter of the single ion are (SIA) distribution of the detector
             assuming lognormal distribution.
             The shape parameter refers to the lognormal distribution
             and not the empirical standard deviation of the SIA data""",
        false,
        false,
        "preferEmpiricalSIA"
    );

    this.gaussianChoice = new ComboEnumParameter<>(
        "Gaussian model",
        "Baseline model in case Gaussian distribution is chosen",
        DistributionModel.GAUSSIAN,
        DistributionModel.listOptionsForGaussian(),
        DistributionModel.class,
        false,
        "gaussianChoice"
    );

    this.measureOfLocationPoisson = new ComboEnumParameter<>(
        "Measure of location",
        "Measure of location for the Poisson distribution",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        true,
        "measureOfLocationPoisson"
    );

    this.measureOfLocationGauss = new ComboEnumParameter<>(
        "Measure of location",
        "Measure of location for the Gaussian distribution",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        true,
        "measureOfLocationGauss"
    );

    this.measureOfSpreadGauss = new ComboEnumParameter<>(
        "Measure of spread",
        "Measure of spread for the Gaussian distribution",
        MeasureOfSpread.SD,
        MeasureOfSpread.values(),
        MeasureOfSpread.class,
        true,
        "measureOfSpreadGauss"
    );

    // Outlier test
    this.outlierTestTypeGauss = new ComboEnumParameter<>(
        "Outlier procedure",
        "To estimate the baseline parameters, spTool removes outliers from the raw data",
        OutlierModel.SMART_INCREMENT_ITERATION,
        OutlierModel.getGaussianTests(),
        OutlierModel.class,
        false,
        "outlierTestTypeGauss"
    );

    this.outlierFactorGaussStart = new DoubleParameter(
        "Outlier factor",
        "Outliers are recognized using a factor f as a 'z-value'-like measure of significance (f·σ)."
            + "\nImportant note: For the smart-increment test, this value represents the initial factor " +
            "(f·σ)",
        1D,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "outlierFactorGaussStart"
    );

    this.outlierGaussMaxNonSmartIncrements = new IntegerParameter(
        "Fixed iterating number n",
        "Max. number of iterations at a fixed outlier factor f (f·σ)."
            + "\nThe algorithm stops as soon as n is reached or no data points are removed anymore",
        5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "outlierGaussMaxNonSmartIncrements"
    );

    this.outlierFactorMax = new DoubleParameter(
        "Max. factor f (f·σ)",
        "Smart outlier recognition by incrementing up to a certain factor f as in f·σ."
            + "\nThis parameter specifies the maximum value of factor f",
        10D,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "outlierFactorMax"
    );

    this.outlierFineIncrements = new DoubleParameter(
        "Fine increment",
        """
            Smart outlier recognition increments the outlier factor f (f·σ) until it finds stable conditions.
            The default coarse increment size is 1. If this does not reach stable conditions,
            another attempt is executed with a finer increment size.
            You may specify the finer step size here. Note that it must be smaller than one
            and will be capped by the algorithm to a value of 0.9. This means, you may enter
            values v [0 < v <= 0.9]. For more details, check out the 2023 JAAS publication 
            """,
        0.1,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "outlierFineIncrements"
    );

    this.outlierDefaultMeanLowerCap = new DoubleParameter(
        "Default µ",
        """
            This value specifies a smallest accepted value for the mean (location).
            If the empirical location is smaller than this value, no smart incrementation is carried out.
            If iteration fails entirely, this µ will be tested as a default mean in case of failure.
            
            For the Poisson model, this behavior is enforced using the epsilon continuity correction""",
        0.1,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "outlierDefaultMeanLowerCap"
    );

    this.outlierStdDevByMeanSmallerThan = new DoubleParameter(
        "(StdDev/Mean) <",
        "Large RSD indicate onset of outlier masking",
        1.5,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "outlierStdDevByMeanSmallerThan"
    );

    this.outlierRosnerAlpha = new DoubleParameter(
        "Rosner alpha",
        "Alpha value for the Rosner test (0.05 -> 5% false positives)",
        0.05,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "outlierRosnerAlpha"
    );

    this.outlierRosnerMaxPercent = new DoubleParameter(
        "Rosner max outliers",
        """
            Approximate expected fraction of outliers among all data points.
            The outlier test will stop once it exceeds this number.
            Hence, you are encouraged to give a rather large estimate.
            Essentially, you have to guess, how many data points of all are part of an event""",
        0.2,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "outlierRosnerMaxPercent"
    );

    this.outlierTestTypePoisson = new ComboEnumParameter<>(
        "Outlier procedure",
        "To estimate the baseline parameters, spTool removes outliers from the raw data",
        OutlierModel.ITERATE_TO_CONVERGENCE,
        OutlierModel.getPoissonTests(),
        OutlierModel.class,
        false,
        "outlierTestTypePoisson"
    );

    this.outlierPoissonOffsetBoolean = new BooleanParameter(
        "Apply constant offset",
        """
            Adding a constant stabilizes convergence during outlier testing.
            This approach is very similar to Currie's YD-type formula which has a constant offset (z^2)
            as described in https://doi.org/10.1039/D3JA00292F
            'Improving detection thresholds and robust event filtering in single-particle and single-cell ICP-MS analysis'
            by Elinkmann et al 2023.
            
            In spTool3, the outlier test is carried out based on left and right sided critical limits.
            In that case, the Currie formulae do not propose a z^2 constant offset.
            However, the stabilizing effect on outlier removal is desired and useful. As such, the implementation
            for spTool3 computes Yc and the computes
            lower outlier limit (LOL): LOL = Yc -  offset.
            upper outlier limit (UOL): UOL = Yc + offset.
            This differs from the original Currie publication in that sense that it does not consider
            a lower limit as the critical limit to distinguish between background and 'true positive events'
            is strictly on the upper limit. However, to avoid biasing the mean towards larger values,
            here the offset is subtracted for LOL.
            
            This is intended for 'Poisson' and 'Poisson Currie-1968'
            as well as the 'Poisson normal-approximation' modelling.
            
            The compound Poisson model does not seem to require is, as noise levels are generally higher
            on time-of-flight machines""",
        true,
        false,
        "outlierPoissonOffsetBoolean"
    );

    this.outlierPoissonOffsetValue = new DoubleParameter(
        "Constant offset [cts/DT]",
        "'YC = µ + f·SD' becomes 'YC = Offset + µ + f·SD'",
        2.71,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "outlierPoissonOffsetValue"
    );

    this.outlierMeasureOfLocationPoisson = new ComboEnumParameter<>(
        "Measure of location (outlier)",
        "Measure of location for the outlier test of the Poisson distribution",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.baseline(),
        MeasureOfLocation.class,
        true,
        "outlierMeasureOfLocationPoisson"
    );

    this.outlierFactorPoissonStart = new DoubleParameter(
        "Outlier factor",
        "Outliers are recognized using a factor f as a 'z-value'-like measure of significance (f·σ)."
            + "\nImportant note: For the smart-increment test, this value represents the initial factor " +
            "(f·σ)",
        3D,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "outlierFactorPoissonStart"
    );

    this.outlierPoissonMaxNonSmartIncrements = new IntegerParameter(
        "Fixed iterating number n",
        "Max. number of iterations at a fixed outlier factor f (f·σ)."
            + "\nThe algorithm stops as soon as n is reached or no data points are removed anymore",
        5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "outlierPoissonMaxNonSmartIncrements"
    );

    this.applyPoissonContinuityCorrection = new BooleanParameter(
        "Epsilon",
        "Apply continuity correction",
        """
            Apply continuity correction ('epsilon'): µBG' = µBG + e.
            
            This option is particularly helpful when the mean background signal is very low.
            Then, outlier may yield µ=0, which results in thresholds that are too low.
            While it's mathematical origin comes from the fact that the Poisson distribution is discrete
            (i.e., it only allows integer numbers), it's effect on data processing is similar to the
            parameter 'default µ' which is used for the Gaussian when the background approaches 0""",
        true,
        false,
        "applyPoissonContinuityCorrection"
    );
    this.poissonContinuityCorrection = new DoubleParameter(
        "Epsilon",
        """
            Apply continuity correction ('epsilon') c.f. Currie's work""",
        0.5,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "poissonContinuityCorrection"
    );

    organize();
  }

  // Copy
  public BaselineParams(BaselineParams baselineParams) {
    super(baselineParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(baselineParams.getCommentParameter());
    this.baselineDynamics = baselineParams.baselineDynamics.copyWithoutChildren();
    this.baselineWidthPerSegment = baselineParams.baselineWidthPerSegment.copyWithoutChildren();
    this.generalDistributionApproach = baselineParams.generalDistributionApproach
        .copyWithoutChildren();
    this.poissonChoice = baselineParams.poissonChoice.copyWithoutChildren();
    this.siaShape = baselineParams.siaShape.copyWithoutChildren();
    this.preferEmpiricalSIA = baselineParams.preferEmpiricalSIA.copyWithoutChildren();
    this.gaussianChoice = baselineParams.gaussianChoice.copyWithoutChildren();
    this.measureOfLocationPoisson = baselineParams.measureOfLocationPoisson
        .copyWithoutChildren();
    this.measureOfLocationGauss = baselineParams.measureOfLocationGauss
        .copyWithoutChildren();
    this.measureOfSpreadGauss = baselineParams.measureOfSpreadGauss
        .copyWithoutChildren();
    this.compoundPoissonModel = baselineParams.compoundPoissonModel.copyWithoutChildren();

    this.poissonNormalCutoff = baselineParams.poissonNormalCutoff.copyWithoutChildren();

    this.outlierTestTypeGauss = baselineParams.outlierTestTypeGauss.copyWithoutChildren();
    this.outlierFactorGaussStart = baselineParams.outlierFactorGaussStart.copyWithoutChildren();
    this.outlierGaussMaxNonSmartIncrements = baselineParams.outlierGaussMaxNonSmartIncrements
        .copyWithoutChildren();
    this.outlierFactorMax = baselineParams.outlierFactorMax.copyWithoutChildren();
    this.outlierFineIncrements = baselineParams.outlierFineIncrements.copyWithoutChildren();
    this.outlierDefaultMeanLowerCap = baselineParams.outlierDefaultMeanLowerCap
        .copyWithoutChildren();
    this.outlierStdDevByMeanSmallerThan = baselineParams.outlierStdDevByMeanSmallerThan
        .copyWithoutChildren();
    this.outlierRosnerAlpha = baselineParams.outlierRosnerAlpha.copyWithoutChildren();
    this.outlierRosnerMaxPercent = baselineParams.outlierRosnerMaxPercent.copyWithoutChildren();

    this.outlierTestTypePoisson = baselineParams.outlierTestTypePoisson.copyWithoutChildren();
    this.outlierMeasureOfLocationPoisson = baselineParams.outlierMeasureOfLocationPoisson
        .copyWithoutChildren();
    this.outlierFactorPoissonStart = baselineParams.outlierFactorPoissonStart.copyWithoutChildren();
    this.outlierPoissonMaxNonSmartIncrements = baselineParams.outlierPoissonMaxNonSmartIncrements
        .copyWithoutChildren();
    this.outlierPoissonOffsetBoolean = baselineParams.outlierPoissonOffsetBoolean
        .copyWithoutChildren();
    this.outlierPoissonOffsetValue = baselineParams.outlierPoissonOffsetValue.copyWithoutChildren();

    this.applyPoissonContinuityCorrection =
        baselineParams.applyPoissonContinuityCorrection.copyWithoutChildren();
    this.poissonContinuityCorrection = baselineParams.poissonContinuityCorrection.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new BaselineParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new BaselineParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    BaselineParams params = new BaselineParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }


  private void organize() {
    // Add all PARENT (not the depending) parameters!
    super.setParentParameters(
        baselineDynamics, // temporarily removed as the option is not there yet
        generalDistributionApproach
    );

    // Attach Children.
    baselineDynamics.addConditionalChild(
        BaselineDynamic.SEGMENTED,
        baselineWidthPerSegment);

    generalDistributionApproach.addConditionalChild(
        DistributionModel.THRESHOLD,
        poissonNormalCutoff);

    generalDistributionApproach.addConditionalChild(
        DistributionModel.listOptionsRequiringPoissonOption(),
        new SeparatorParameter(),
        poissonChoice);

    generalDistributionApproach.addConditionalChild(
        DistributionModel.listOptionsRequiringGaussianOption(),
        new SeparatorParameter(),
        gaussianChoice);

    poissonChoice.addUnconditionalChild(measureOfLocationPoisson);
    poissonChoice.addConditionalChild(DistributionModel.POISSON_COMPOUND,
        compoundPoissonModel, siaShape, preferEmpiricalSIA);
    poissonChoice.addConditionalChild(DistributionModel.POISSON,
        applyPoissonContinuityCorrection);
    poissonChoice.addConditionalChild(DistributionModel.POISSON_CURRIE,
        applyPoissonContinuityCorrection);
    poissonChoice.addConditionalChild(DistributionModel.POISSON_APPROXIMATION,
        applyPoissonContinuityCorrection);
    poissonChoice.addUnconditionalChild(outlierTestTypePoisson);

    applyPoissonContinuityCorrection.addConditionalChild(true,
        poissonContinuityCorrection);

    gaussianChoice.addUnconditionalChild(
        measureOfLocationGauss,
        measureOfSpreadGauss,
        outlierTestTypeGauss);

    // outlier gaussian
    outlierTestTypeGauss.addConditionalChild(OutlierModel.listIterativeTests(),
        outlierFactorGaussStart);

    outlierTestTypeGauss.addConditionalChild(
        OutlierModel.FIXED_NUMBER_ITERATION,
        outlierGaussMaxNonSmartIncrements);

    outlierTestTypeGauss.addConditionalChild(OutlierModel.SMART_INCREMENT_ITERATION,
        outlierFactorMax,
        outlierFineIncrements,
        outlierDefaultMeanLowerCap,
        outlierStdDevByMeanSmallerThan);

    outlierTestTypeGauss.addConditionalChild(OutlierModel.ROSNER,
        outlierRosnerAlpha, outlierRosnerMaxPercent);

    // outlier poisson
    outlierTestTypePoisson.addConditionalChild(OutlierModel.listIterativeTests(),
        outlierMeasureOfLocationPoisson,
        outlierFactorPoissonStart,
        outlierPoissonOffsetBoolean);

    outlierTestTypePoisson.addConditionalChild(
        OutlierModel.FIXED_NUMBER_ITERATION,
        outlierMeasureOfLocationPoisson,
        outlierPoissonMaxNonSmartIncrements);

    outlierPoissonOffsetBoolean.addConditionalChild(true, outlierPoissonOffsetValue);
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

          case "baselineDynamics" -> baselineDynamics;
          case "baselineWidthPerSegment" -> baselineWidthPerSegment;

          case "generalDistributionApproach" -> generalDistributionApproach;
          case "poissonNormalCutoff" -> poissonNormalCutoff;
          case "poissonChoice" -> poissonChoice;
          case "gaussianChoice" -> gaussianChoice;

          case "measureOfLocationPoisson" -> measureOfLocationPoisson;
          case "measureOfLocationGauss" -> measureOfLocationGauss;
          case "measureOfSpreadGauss" -> measureOfSpreadGauss;

          case "compoundPoissonModel" -> compoundPoissonModel;
          case "siaShape" -> siaShape;
          case "preferEmpiricalSIA" -> preferEmpiricalSIA;

          case "applyPoissonContinuityCorrection" -> applyPoissonContinuityCorrection;
          case "poissonContinuityCorrection" -> poissonContinuityCorrection;

          // outlier gauss
          case "outlierTestTypeGauss" -> outlierTestTypeGauss;
          case "outlierFactorGaussStart" -> outlierFactorGaussStart;
          case "outlierGaussMaxNonSmartIncrements" -> outlierGaussMaxNonSmartIncrements;
          case "outlierFactorMax" -> outlierFactorMax;
          case "outlierFineIncrements" -> outlierFineIncrements;
          case "outlierDefaultMeanLowerCap" -> outlierDefaultMeanLowerCap;
          case "outlierStdDevByMeanSmallerThan" -> outlierStdDevByMeanSmallerThan;
          case "outlierRosnerAlpha" -> outlierRosnerAlpha;
          case "outlierRosnerMaxPercent" -> outlierRosnerMaxPercent;

          // outlier poisson
          case "outlierTestTypePoisson" -> outlierTestTypePoisson;
          case "outlierMeasureOfLocationPoisson" -> outlierMeasureOfLocationPoisson;
          case "outlierFactorPoissonStart" -> outlierFactorPoissonStart;
          case "outlierPoissonOffsetBoolean" -> outlierPoissonOffsetBoolean;
          case "outlierPoissonOffsetValue" -> outlierPoissonOffsetValue;

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
    return AvailableParameterSets.BASELINE;
  }

  /// ///////////////////////////////////////////////////////////////////////////////////


  public Parameter<Double> getOutlierFactorGaussStart() {
    return outlierFactorGaussStart;
  }

  public Parameter<Double> getOutlierFactorMax() {
    return outlierFactorMax;
  }

  public Parameter<Double> getOutlierDefaultMeanLowerCap() {
    return outlierDefaultMeanLowerCap;
  }

  public Parameter<Integer> getOutlierGaussMaxNonSmartIncrements() {
    return outlierGaussMaxNonSmartIncrements;
  }

  public Parameter<Double> getOutlierStdDevByMeanSmallerThan() {
    return outlierStdDevByMeanSmallerThan;
  }

  public Parameter<BaselineDynamic> getBaselineDynamics() {
    return baselineDynamics;
  }

  public Parameter<Double> getBaselinePointsPerSegment() {
    return baselineWidthPerSegment;
  }

  public Parameter<DistributionModel> getGeneralDistributionApproach() {
    return generalDistributionApproach;
  }

  public Parameter<Double> getPoissonNormalCutoff() {
    return poissonNormalCutoff;
  }

  public Parameter<DistributionModel> getPoissonChoice() {
    return poissonChoice;
  }

  public Parameter<CompoundPoissonModel> getCompoundPoissonModel() {
    return compoundPoissonModel;
  }

  public Parameter<Double> getSiaShape() {
    return siaShape;
  }

  public Parameter<Boolean> getPreferEmpiricalSIA() {
    return preferEmpiricalSIA;
  }

  public Parameter<DistributionModel> getGaussianChoice() {
    return gaussianChoice;
  }

  public Parameter<MeasureOfLocation> getMeasureOfLocationPoisson() {
    return measureOfLocationPoisson;
  }

  public Parameter<MeasureOfLocation> getMeasureOfLocationGauss() {
    return measureOfLocationGauss;
  }

  public Parameter<MeasureOfSpread> getMeasureOfSpreadGauss() {
    return measureOfSpreadGauss;
  }

  public Parameter<OutlierModel> getOutlierTestTypeGauss() {
    return outlierTestTypeGauss;
  }

  public Parameter<Double> getOutlierFineIncrements() {
    return outlierFineIncrements;
  }

  public Parameter<Double> getOutlierRosnerAlpha() {
    return outlierRosnerAlpha;
  }

  public Parameter<Double> getOutlierRosnerMaxPercent() {
    return outlierRosnerMaxPercent;
  }

  // poisson outlier

  public Parameter<OutlierModel> getOutlierTestTypePoisson() {
    return outlierTestTypePoisson;
  }

  public Parameter<MeasureOfLocation> getOutlierMeasureOfLocationPoisson() {
    return outlierMeasureOfLocationPoisson;
  }

  public Parameter<Double> getOutlierFactorPoissonStart() {
    return outlierFactorPoissonStart;
  }

  public Parameter<Integer> getOutlierPoissonMaxNonSmartIncrements() {
    return outlierPoissonMaxNonSmartIncrements;
  }

  public Parameter<Boolean> getOutlierPoissonOffsetBoolean() {
    return outlierPoissonOffsetBoolean;
  }

  public Parameter<Double> getOutlierPoissonOffsetValue() {
    return outlierPoissonOffsetValue;
  }

  public Parameter<Boolean> getApplyPoissonContinuityCorrection() {
    return applyPoissonContinuityCorrection;
  }

  public Parameter<Double> getPoissonContinuityCorrection() {
    return poissonContinuityCorrection;
  }


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final BaselineParams defaults = new BaselineParams();

    // Fix missing fields from old serialized versions: we have to use,

    if (baselineWidthPerSegment == null) {
      this.baselineWidthPerSegment = defaults.baselineWidthPerSegment;
    }

    if (poissonContinuityCorrection == null) {
      this.poissonContinuityCorrection = defaults.poissonContinuityCorrection;
    }

    if (applyPoissonContinuityCorrection == null) {
      this.applyPoissonContinuityCorrection = defaults.applyPoissonContinuityCorrection;
    }

    if (preferEmpiricalSIA == null) {
      this.preferEmpiricalSIA = defaults.preferEmpiricalSIA;
    }
  }
}
