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
import math.stat.MeasureOfLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.BooleanParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.Parameter;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;

public class AlignerParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "AlignParams";

  private Parameter<Boolean> enableBoolean;

  private final Parameter<AlignAlgorithm> alignAlgorithm;

  private final Parameter<NetCorrectionOption> netCorrectionOption;
  private final Parameter<Double> netTimeWindow;
  private Parameter<MeasureOfLocation> netTimeWindowLocation;
  private Parameter<Boolean> suppressNegativeValues;


  public AlignerParams() {
    super("Align isotopes parameters", XML_ELEMENT_TAG);

    this.enableBoolean = new BooleanParameter("On/Off",
        "Enable",
        "Activate this sub method",
        true,
        false,
        "enableBoolean"
    );

    this.alignAlgorithm = new ComboEnumParameter<>(
        "Procedure",
        """
            Choose how to align:
            
            Connection: looks at whether two events share any index positions,
            and if they do, merges them together, even if there are gaps.
            'Connection' chains transitively, so if A shares with B and B shares with C, all three become one.
            
            Coverage: groups events that are neighbors with no gaps between them.
            If there is a gap, you get two separate regions.
            
            Example: Three events with their data point indices: A=[1-2] B=[2-5] C=[5-9].
            
            - 'Coverage' yields two regions [1-2] and [5-9].
             Aligning means that these data points are considered an event across all mz.
             They are split in 2 separate regions because no event covers the indices 3 and 4.
            
            - 'Connection' yields one connected region [1-9]
             because A and B share index 2, and B and C share index 5""",
        AlignAlgorithm.REGION_COVERAGE,
        AlignAlgorithm.values(),
        AlignAlgorithm.class,
        true,
        "alignAlgorithm");

    this.netCorrectionOption = new ComboEnumParameter<>(
        "Net correction",
        """
            There are different ways to estimate how much background signal is present
            during an event and needs to be subtracted.
            (1) Use the mean of the baseline (fastest)
            (2) Use a time window based on the dwell time and the resulting index (faster)
            (3) Use a time window based on an exact time stamp comparison (slower)""",
        NetCorrectionOption.BLN_MEAN,
        NetCorrectionOption.values(),
        NetCorrectionOption.class,
        true,
        "netCorrectionOption");

    this.netTimeWindow = new DoubleParameter(
        "Time window [s]",
        """
            SpTool will take the peak of an event
            and use a time period of one time window to the right
            from which it estimates how much background signal
            needs to be subtracted from the net event signal""",
        0.5,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "netTimeWindow");

    this.netTimeWindowLocation = new ComboEnumParameter<>(
        "Measure of location",
        """
            Measure of location for the background subtraction.
            Choose median if the data are very noisy due to many small particles
            or similar reasons. Else, use the mean. Note that the median is slower to compute than the
            mean!
            The median may be zero: When the median is zero, which
            is the case for background levels around 1 cts or below,
            effectively no background subtraction is carried out.
            Thus, it is recommended to use the mean in that case (very low BG signal)""",
        MeasureOfLocation.MEAN,
        MeasureOfLocation.window(),
        MeasureOfLocation.class,
        true,
        "netTimeWindowLocation"
    );

    this.suppressNegativeValues = new BooleanParameter("Negative value handling",
        "Suppress negative areas",
        """
            When the net correction subtracts too much signal,
            negative peak areas may result. This checkbox makes sure
            that the lowest possible signal to obtain is zero
            and not something negative.
            The respective events may be removed,
            e.g., by setting a gate 'net area > 0.1'""",
        false,
        true,
        "suppressNegativeValues"
    );
    organize();
  }

  public AlignerParams(AlignerParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.enableBoolean = params.enableBoolean.copyWithoutChildren();

    this.alignAlgorithm = params.alignAlgorithm.copyWithoutChildren();
    this.netCorrectionOption = params.netCorrectionOption.copyWithoutChildren();
    this.netTimeWindow = params.netTimeWindow.copyWithoutChildren();
    this.netTimeWindowLocation = params.netTimeWindowLocation.copyWithoutChildren();
    this.suppressNegativeValues = params.suppressNegativeValues.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new AlignerParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new AlignerParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new AlignerParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Parent parameters
    super.setParentParameters(
        enableBoolean
    );

    enableBoolean.addConditionalChild(true, alignAlgorithm, netCorrectionOption);

    netCorrectionOption.addConditionalChild(NetCorrectionOption.INDEX_APPROXIMATION, netTimeWindow,
        netTimeWindowLocation);
    netCorrectionOption.addConditionalChild(NetCorrectionOption.TIME_EXACT, netTimeWindow,
        netTimeWindowLocation);
    netCorrectionOption.addUnconditionalChild(suppressNegativeValues);

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

          case "alignAlgorithm" -> alignAlgorithm;
          case "timeWindow" -> netTimeWindow;
          case "netCorrectionOption" -> netCorrectionOption;
          case "netTimeWindowLocation" -> netTimeWindowLocation;
          case "suppressNegativeValues" -> suppressNegativeValues;
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
    return AvailableParameterSets.ALIGNMENT;
  }

  //------------------------------------------------------------------------------------------

  public Parameter<Boolean> getEnableBoolean() {
    return enableBoolean;
  }

  public Parameter<AlignAlgorithm> getAlignAlgorithm() {
    return alignAlgorithm;
  }

  public Parameter<NetCorrectionOption> getNetCorrectionOption() {
    return netCorrectionOption;
  }


  public Parameter<Double> getNetTimeWindow() {
    return netTimeWindow;
  }

  public Parameter<MeasureOfLocation> getNetTimeWindowLocation() {
    return netTimeWindowLocation;
  }

  public Parameter<Boolean> getSuppressNegativeValues() {
    return suppressNegativeValues;
  }

  //------------------------------------------------------------------------------------------


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final AlignerParams defaults = new AlignerParams();


  }

}
