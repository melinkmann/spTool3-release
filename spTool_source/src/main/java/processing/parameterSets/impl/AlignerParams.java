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
import dataModelNew.mz.Channel;
import dataModelNew.mz.MZValue;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.nu.IsotopePtoeDialog;
import javafx.stage.Window;
import math.stat.MeasureOfLocation;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.*;
import sandbox.montecarlo.Isotope;
import util.Functional;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AlignerParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "AlignParams";

  private Parameter<Boolean> enableBoolean;

  private final Parameter<AlignAlgorithm> alignAlgorithm;
  private Parameter<IsotopeSelection> isotopeSelection;
  private final Parameter<String> excludedIsotopes;
  private Parameter<String> includedIsotopes;

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
            
            Example: Three events with their data point indices in the respective MS A, B, and C:
            A=[1-3] B=[3-5] C=[7-9].
            
            - 'Region' yields two regions [1-5] and [7-9].
              Result of align would be that these data points are considered an event across all MS (A, B, C).
              They are split into 2 separate regions because no event in any MS covers index 6.
            
            - 'Contact' yields one connected region [1-9].
             Rules: For small events (≤3 indices), a single shared index is enough to trigger a merge.
             For larger events (>3 indices), at least 2 shared indices are required.
             In the example, events for A and B share index 3.
             B connects to C despite the gap at index 6.""",
        AlignAlgorithm.REGION_COVERAGE,
        AlignAlgorithm.values(),
        AlignAlgorithm.class,
        true,
        "alignAlgorithm");

    this.isotopeSelection = new ComboEnumParameter<>(
        "Rules",
        """
            You may decide which isotopes are considered for the align operation:
            a) use all loaded isotopes (table on the right-hand side of the screen),
            b) use the selected isotopes (in table on the right-hand side of the screen),
            c) 'Set list': create a selection in the submethod that is not affected by
            selecting different isotopes in table on the right-hand side of the screen,
            d) 'Exclude': create a 'blacklist' of isotopes that shall not be included
             (besides these, all loaded isotopes will be used)""",
        IsotopeSelection.ALL_LOADED,
        IsotopeSelection.getUI(),
        IsotopeSelection.class,
        false,
        "isotopeSelection");

    excludedIsotopes = new FeedbackStringParameter(
        "Excluded",
        """
            """,
        "ISOTOPE[m=19,e=F];ISOTOPE[m=20,e=Ne];ISOTOPE[m=21,e=Ne];ISOTOPE[m=22,e=Ne];ISOTOPE[m=78,e=Kr];" +
            "ISOTOPE[m=80,e=Kr];ISOTOPE[m=82,e=Kr];ISOTOPE[m=83,e=Kr];ISOTOPE[m=84,e=Kr];ISOTOPE[m=86," +
            "e=Kr];ISOTOPE[m=124,e=Xe];ISOTOPE[m=126,e=Xe];ISOTOPE[m=128,e=Xe];ISOTOPE[m=129,e=Xe];" +
            "ISOTOPE[m=130,e=Xe];ISOTOPE[m=131,e=Xe];ISOTOPE[m=132,e=Xe];ISOTOPE[m=134,e=Xe];ISOTOPE[m=136," +
            "e=Xe];ISOTOPE[m=209,e=Po];ISOTOPE[m=210,e=At];ISOTOPE[m=222,e=Rn];ISOTOPE[m=223,e=Fr];" +
            "ISOTOPE[m=263,e=Rf];ISOTOPE[m=262,e=Db];ISOTOPE[m=266,e=Sg];ISOTOPE[m=264,e=Bh];ISOTOPE[m=269," +
            "e=Hs];ISOTOPE[m=268,e=Mt];ISOTOPE[m=281,e=Ds];ISOTOPE[m=280,e=Rg];ISOTOPE[m=285,e=Cn];" +
            "ISOTOPE[m=284,e=Nh];ISOTOPE[m=289,e=Fl];ISOTOPE[m=288,e=Mc];ISOTOPE[m=293,e=Lv];ISOTOPE[m=294," +
            "e=Ts];ISOTOPE[m=294,e=Og];ISOTOPE[m=147,e=Pm];ISOTOPE[m=247,e=Cm];ISOTOPE[m=247,e=Bk];" +
            "ISOTOPE[m=251,e=Cf];ISOTOPE[m=252,e=Es];ISOTOPE[m=257,e=Fm];ISOTOPE[m=258,e=Md];ISOTOPE[m=259," +
            "e=No];ISOTOPE[m=262,e=Lr];ISOTOPE[m=98,e=Tc]",
        TextFormatterOption.ALL_PASS,
        false,
        "excludedIsotopes"
    );

    includedIsotopes = new FeedbackStringParameter(
        "Selection list",
        """
            """,
        "",
        TextFormatterOption.ALL_PASS,
        false,
        "includedIsotopes"
    );

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
    this.isotopeSelection = params.isotopeSelection.copyWithoutChildren();
    this.excludedIsotopes = params.excludedIsotopes.copyWithoutChildren();
    this.includedIsotopes = params.includedIsotopes.copyWithoutChildren();
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

    enableBoolean.addConditionalChild(true, alignAlgorithm, isotopeSelection, netCorrectionOption);

    isotopeSelection.addConditionalChild(IsotopeSelection.NEGATIVE_LIST_EXCLUSION, excludedIsotopes);
    excludedIsotopes.setDecoration(new ParamSetterButtonDecoration<>("Select isotopes", "/img/tableTrace.png",
        new Functional() {

          @Override
          public void proceed() {
            proceed(null);
          }

          @Override
          public void proceed(Window window) {
            List<Isotope> prevSel = NuInterpreterParams.isotopeFromString(excludedIsotopes.getValue());

            IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
                window,
                dataModelNew.mz.Element.getAllIsotopes(),   // all isotopes available
                // SpTool3Main.getRunTime().getMainWindowCtl().getAllIsotopes(),
                prevSel);                  // null or empty = open blank

            List<Channel> resultingChannels = dlg.showAndWait();
            if (resultingChannels != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (Channel channel : resultingChannels) {
                Isotope isotope = channel.getIsotope();
                if (isotope != null){
                  resultingIsotopes.add(isotope);
                }
              }
              excludedIsotopes.setValue(NuInterpreterParams.isotopesToString(resultingIsotopes));
            }
          }
        }));

    isotopeSelection.addConditionalChild(IsotopeSelection.POSITIVE_LIST_SELECTION, includedIsotopes);
    includedIsotopes.setDecoration(new ParamSetterButtonDecoration<>("Select isotopes", "/img/tableTrace.png",
        new Functional() {

          @Override
          public void proceed() {
            proceed(null);
          }

          @Override
          public void proceed(Window window) {
            List<Isotope> prevSel = NuInterpreterParams.isotopeFromString(includedIsotopes.getValue());

            IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
                window,
                dataModelNew.mz.Element.getAllIsotopes(),   // all isotopes available
                // SpTool3Main.getRunTime().getMainWindowCtl().getAllIsotopes(),
                prevSel);                  // null or empty = open blank

            List<Channel> resultingChannels = dlg.showAndWait();
            if (resultingChannels != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (Channel channel : resultingChannels) {
                Isotope isotope = channel.getIsotope();
                if (isotope != null){
                  resultingIsotopes.add(isotope);
                }
              }
              includedIsotopes.setValue(NuInterpreterParams.isotopesToString(resultingIsotopes));
            }
          }
        }));

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
          case "isotopeSelection" -> isotopeSelection;
          case "excludedIsotopes" -> excludedIsotopes;
          case "includedIsotopes" -> includedIsotopes;

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

  public Parameter<IsotopeSelection> getIsotopeSelection() {
    return isotopeSelection;
  }

  public List<Isotope> listExcludedIsotopes() {
    return NuInterpreterParams.isotopeFromString(excludedIsotopes.getValue());
  }

  public List<Isotope> listIncludedIsotopes() {
    return NuInterpreterParams.isotopeFromString(includedIsotopes.getValue());
  }

  //------------------------------------------------------------------------------------------


  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final AlignerParams defaults = new AlignerParams();

    if (isotopeSelection == null) {
      isotopeSelection = defaults.isotopeSelection;
    }

    if (includedIsotopes == null) {
      includedIsotopes = defaults.includedIsotopes;
    }

  }

}
