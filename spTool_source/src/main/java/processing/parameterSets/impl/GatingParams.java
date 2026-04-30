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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.GatingMeasureOfSignificance;
import processing.options.GatingOption;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import util.NF;

public class GatingParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "GatingParams";

  private final Parameter<Boolean> enableBoolean;

  private final Parameter<GatingOption> gatingOption;

  private final Parameter<GatingMeasureOfSignificance> measureOfSignificance;
  private final Parameter<Double> alphaValue;
  private final Parameter<Double> zValue;

  private final Parameter<Boolean> factorBoolean;
  private final Parameter<Double> factor;

  private final Parameter<Double> absoluteCutoff;

  private Parameter<Double> peakDominancePct;

  private Parameter<Double> pValueAccumulationAlpha;

  private Parameter<Boolean> forceNewBGDefinition;

  public GatingParams() {
    super("Gating parameters", XML_ELEMENT_TAG);

    enableBoolean = new BooleanParameter("On/Off",
        "Enable",
        "Activate this sub method",
        true,
        false,
        "enableBoolean"
    );

    this.gatingOption = new ComboEnumParameter<>(
        "Gating mode",
        """
            Choose gating mode:
            
            Peak height:
            Keep events whose peak height exceeds the threshold.
            
            Peak area:
            Keep events whose peak area exceeds the threshold.
            Area is the sum of all intensity values across the event.
            
            Net peak area:
            Keep events whose net peak area exceeds the threshold.
            Net area subtracts the background level from the area, so only the signal above baseline is considered.
              - This option can be used to exclude events with apparently zero or negative area
                which can occur when aligning peaks across all isotopes and certain isotopes have virtually no signal.
            
            More points than:
            Keep events that have more than a minimum number of data points.
            Useful for removing very short spikes.
            
            Fewer points than:
            Keep events that span fewer than a maximum number of data points.
            Useful for removing very broad, smeared events."
            
            Mean signal of peak:
            Keep events whose average intensity across the peak profile exceeds the threshold.
            This option may help filtering peaks at very short dwell times when the peak height is not a good indicator.
            
            Peak dominance:
            Keep events where the signal is spread across multiple data points
            rather than concentrated in a single spike.
            Rejects sharp, narrow peaks that look like background noise.
            
            Accumulated probability:
            Keep events whose data points are collectively unlikely to come from the background based on probability""",
        GatingOption.HEIGHT,
        GatingOption.getActiveValues(),
        GatingOption.class,
        false,
        "gatingOption");

    this.measureOfSignificance = new ComboEnumParameter<>(
        "Type of significance level",
        "Decide how to set the level of significance",
        GatingMeasureOfSignificance.ALPHA,
        GatingMeasureOfSignificance.values(),
        GatingMeasureOfSignificance.class,
        false,
        "measureOfSignificance");

    this.alphaValue = new DoubleParameter(
        "False positive rate",
        "'alpha': Tolerated rate of false positive events",
        1E-10,
        NF.D1C3Exp,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        false,
        "alphaValue"
    );

    this.zValue = new DoubleParameter(
        "z-value",
        "Uses a z-value f instead of 'alpha' as for example in '3·sigma' where f=3",
        3D,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "zValue"
    );

    factorBoolean = new BooleanParameter("Multiply",
        """
            This option uses the specified significance (alpha or z)
            to calculate a detection limit according to the Currie formalism
            z(detection) = 2 · z(critical).
            It then multiplies this detection limit with a factor.
            The result is the gate""",
        false,
        false,
        "factorBoolean"
    );

    this.factor = new DoubleParameter(
        "Factor",
        """
            This option uses the specified significance (alpha or z)
            to calculate a detection limit according to the Currie formalism
            z(detection) = 2 · z(critical).
            It then multiplies this detection limit with a factor.
            The result is the gate,
            
            Why? This can help to achieve large enough values without\s
            running into problems with floating point precision of very small alpha values""",
        2D,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "factor"
    );

    this.absoluteCutoff = new DoubleParameter(
        "Absolute value",
        "Gate is defined via an absolute value set by the user",
        1D,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "absoluteCutoff"
    );

    this.peakDominancePct = new DoubleParameter(
        "Area percent",
        """
            At, e.g., DT = 10 µs, we can expect that peaks consist of many data points that are sometimes just
            slightly higher than the detection threshold.
            
            Hence, we want to filter and keep a peak where many data points contribute substantially to the peak area
            as this means that the certainty increases that it actually is a particle-related peak.
            Accordingly, we try to exclude events where most of the signal comes from 1 or 2 high outlier data points.
            
            We compute the ratio of the highest data point to the total area (both background-subtracted).
            An event is kept if this ratio, expressed as the percentage 'height/area',
            is below p%, indicating no single data point dominates the peak.
            
            Note that this filter may cause undesired effects at DT on the order of 100 µs""",
        10D,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "peakDominancePct"
    );

    this.pValueAccumulationAlpha = new DoubleParameter(
        "Combined alpha",
        """
            Combined probability of observing all data points in the peak based on their signal intensity
            """,
        1E-6,
        NF.D1C3Exp,
        TextFormatterOption.ASSURE_NONZERO_POS_EXP_DOUBLE,
        false,
        "pValueAccumulationAlpha"
    );

    this.forceNewBGDefinition = new BooleanParameter(
        "Background",
        "Define background data by this gate",
        """
            The BG is defined as 'all data points that are not part of a particle event'.
            
            Thus, when a gate is applied, this may change. However, may not actually be desired,
            e.g., when the gate restricts number of points: This does not necessarily change the true BG definition.
            
            Select this option when you think that the events that you exclude with this gate
            are truly background signal and that they should be used for index-based background subtraction.
            Note: When using the baseline mean for background subtraction, this has no effect.
            
            When you plot or export background, the data to plot or export are also defined by this option""",
        false,
        true,
        "forceNewBGDefinition"
    );

    organize();
  }

  public GatingParams(GatingParams gatingParams) {
    super(gatingParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(gatingParams.getCommentParameter());
    this.enableBoolean = gatingParams.enableBoolean.copyWithoutChildren();
    this.gatingOption = gatingParams.gatingOption.copyWithoutChildren();
    this.measureOfSignificance = gatingParams.measureOfSignificance.copyWithoutChildren();
    this.alphaValue = gatingParams.alphaValue.copyWithoutChildren();
    this.factorBoolean = gatingParams.factorBoolean.copyWithoutChildren();
    this.factor = gatingParams.factor.copyWithoutChildren();
    this.zValue = gatingParams.zValue.copyWithoutChildren();
    this.absoluteCutoff = gatingParams.absoluteCutoff.copyWithoutChildren();
    this.peakDominancePct = gatingParams.peakDominancePct.copyWithoutChildren();
    this.pValueAccumulationAlpha = gatingParams.pValueAccumulationAlpha.copyWithoutChildren();
    this.forceNewBGDefinition = gatingParams.forceNewBGDefinition.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new GatingParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new GatingParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new GatingParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Register parent
    super.setParentParameters(enableBoolean);

    enableBoolean.addConditionalChild(true, gatingOption, forceNewBGDefinition);

    // Depending children
    gatingOption.addConditionalChild(
        GatingOption.HEIGHT,
        measureOfSignificance);

    gatingOption.addConditionalChild(
        GatingOption.AREA,
        measureOfSignificance);

    gatingOption.addConditionalChild(
        GatingOption.NET_AREA,
        measureOfSignificance);

    gatingOption.addConditionalChild(
        GatingOption.PEAK_DOMINANCE,
        peakDominancePct);

    gatingOption.addConditionalChild(
        GatingOption.ACCUMULATED_P,
        pValueAccumulationAlpha);

    gatingOption.addConditionalChild(
        GatingOption.MEAN_SIGNAL,
        measureOfSignificance);

    measureOfSignificance.addConditionalChild(
        GatingMeasureOfSignificance.ALPHA,
        alphaValue, factorBoolean);
    measureOfSignificance.addConditionalChild(
        GatingMeasureOfSignificance.Z_VALUE,
        zValue, factorBoolean);

    factorBoolean.addConditionalChild(true, factor);

    measureOfSignificance.addConditionalChild(
        GatingMeasureOfSignificance.CUSTOM_VALUE,
        absoluteCutoff);

    gatingOption.addConditionalChild(
        GatingOption.FEWER_POINTS_THAN,
        absoluteCutoff);

    gatingOption.addConditionalChild(
        GatingOption.MORE_POINTS_THAN,
        absoluteCutoff);
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

          case "enableBoolean" -> enableBoolean;
          case "gatingOption" -> gatingOption;

          case "measureOfSignificance" -> measureOfSignificance;
          case "alphaValue" -> alphaValue;

          case "factorBoolean" -> factorBoolean;
          case "factor" -> factor;
          case "zValue" -> zValue;

          case "absoluteCutoff" -> absoluteCutoff;

          case "spikeBalancePercent" -> peakDominancePct;
          case "pValueAccumulationAlpha" -> pValueAccumulationAlpha;

          case "forceNewBGDefinition" -> forceNewBGDefinition;

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
    return AvailableParameterSets.GATE_FILTER;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////


  public Parameter<Boolean> getEnableBoolean() {
    return enableBoolean;
  }

  public Parameter<GatingOption> getGatingOption() {
    return gatingOption;
  }

  public Parameter<GatingMeasureOfSignificance> getMeasureOfSignificance() {
    return measureOfSignificance;
  }

  public Parameter<Boolean> getFactorBoolean() {
    return factorBoolean;
  }

  public Parameter<Double> getFactor() {
    return factor;
  }

  public Parameter<Double> getAlphaValue() {
    return alphaValue;
  }

  public Parameter<Double> getzValue() {
    return zValue;
  }

  public Parameter<Double> getAbsoluteCutoff() {
    return absoluteCutoff;
  }

  public Parameter<Double> getPeakDominancePct() {
    return peakDominancePct;
  }

  public Parameter<Double> getpValueAccumulationAlpha() {
    return pValueAccumulationAlpha;
  }

  public Parameter<Boolean> getForceNewBGDefinition() {
    return forceNewBGDefinition;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final GatingParams defaults = new GatingParams();

    // Fix missing fields from old serialized versions: we have to use
    if (peakDominancePct == null) {
      this.peakDominancePct = defaults.peakDominancePct;
    }

    if (forceNewBGDefinition == null) {
      this.forceNewBGDefinition = defaults.forceNewBGDefinition;
    }

    if (pValueAccumulationAlpha == null) {
      this.pValueAccumulationAlpha = defaults.pValueAccumulationAlpha;
    }

  }

}
