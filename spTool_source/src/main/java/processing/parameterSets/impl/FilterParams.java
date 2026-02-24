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

import org.checkerframework.checker.units.qual.C;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamBundle;
import processing.parameterSets.ParamSet;
import processing.parameterSets.bundle.RoiSigFactorBundle;
import processing.parameterSets.bundle.RoiStartStopBundle;
import processing.parameters.*;
import util.NF;

public class FilterParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "FilterParams";

  private final Parameter<Boolean> enableBoolean;

  private final Parameter<FilterOptions> filterOption;

  // Double events (overlap)
  private final Parameter<Double> zIntensity;
  private final Parameter<Double> zSegment;
  private Parameter<Integer> moavPeriod;
  private Parameter<OverlapSmoothOption> smoothTargetOption;
  private final Parameter<Boolean> listNumberOfEvents;

  // ROI
  /*
    ROI
  - target parameter (area, height, ...)
  - conversion (cuberoot, ...)
  - include/exclude
  - custom abs start/end
  - custom percentiles start/end
  - f IQR
  - f MAD
   */
  public Parameter<String> roiID;
  public Parameter<RoiCategory> roiCategory;
  public Parameter<EventParameter> eventParameter;
  public Parameter<MathMod> mathConversion;
  public Parameter<RoiType> roiType;

  public Parameter<Double> start;
  public Parameter<Double> end;
  public Parameter<Double> sigFactor;

  public Parameter<Integer> roiExceptionsStartStop;
  public Parameter<Integer> roiExceptionsSigFactor;

  private Parameter<Boolean> listMatches;
  private Parameter<Boolean> listFalsePositives;
  private Parameter<Boolean> listFalseNegatives;

  private Parameter<Boolean> suppressNegativeValues;
  private Parameter<Boolean> removeNegativeValues;

  public FilterParams() {
    super("Filter parameters", XML_ELEMENT_TAG);

    enableBoolean = new BooleanParameter("On/Off",
        "Enable",
        "Activate this sub method",
        true,
        false,
        "enableBoolean"
    );

    this.filterOption = new ComboEnumParameter<>(
        "Filter type",
        "Choose filter type",
        FilterOptions.OVERLAP,
        FilterOptions.values(),
        FilterOptions.class,
        false,
        "filterOption");

    ///////////////////////// OVERLAP ////////////////////////////////////

    this.zIntensity = new DoubleParameter(
        "z-Diff",
        "Magnitude of the threshold for the differences." +
            "Usually between 0.5 - 2",
        1.0, //1.5
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "zIntensity"
    );

    this.zSegment = new DoubleParameter(
        "z-Seg",
        """
            Magnitude of the threshold for the segments. Usually between 1 - 5.
            When z-Diff is (too low), z-Seg may require larger values of up to 10 - 50.
            
            z-Seg is computed from the background level.
            It may depend on detector gain and noise levels, instrument properties
            and in particular overdispersion from the ion source and sample introduction.
            Thus, z-seg may change depending on instrument, method and sample""",
        3.0d, //1.0
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "zSegment"
    );

    this.moavPeriod = new IntegerParameter(
        "Period",
        "Period of the moving average in points",
        2,  // 3
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        true,
        "moavPeriod"
    );

    this.smoothTargetOption = new ComboEnumParameter<>(
        "Smoothing",
        """
            Decide whether smoothing is applied to the difference between data points ('derivative')
            or the raw data. In case of very noisy peaks and/or flat and wider peak shapes,
            it may help to smooth the raw data and not the derivative.
            
            Smoothing makes data points depend on each other and thus affects statistics!
            When smoothing raw data, consider adjusting z-Diff and z-Seg""",
        OverlapSmoothOption.DERIVATIVE,
        OverlapSmoothOption.values(),
        OverlapSmoothOption.class,
        false,
        "smoothTargetOption"
    );


    this.listNumberOfEvents = new BooleanParameter(
        "Detail",
        "List each population",
        "If selected, you will get a list of all n = 1, 2, 3 ... subpopulations",
        false,
        false,
        "listNumberOfEvents"
    );

    //////////////////////////// ROI /////////////////////////////////////

    this.roiID = new StringParameter(
        "ROI ID",
        "Assign a unique ID to the ROI to identify it in the UI",
        "ROI_1",
        TextFormatterOption.ASSURE_EXTD_NUMERAL_OR_LETTER,
        false,
        false,
        "roiID"
    );

    roiCategory = new ComboEnumParameter<>("Type",
        "Choose the type of ROI",
        RoiCategory.ABSOLUTE_VALUES,
        RoiCategory.values(),
        RoiCategory.class,
        false,
        "roiCategory");

    eventParameter = new ComboEnumParameter<>("Event parameter",
        "ROI based on which parameter",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter");

    mathConversion = new ComboEnumParameter<>("Math",
        "Choose conversion",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "mathConversion");

    roiType = new ComboEnumParameter<>("Target",
        "Choose target of ROI",
        RoiType.INCLUDE,
        RoiType.values(),
        RoiType.class,
        false,
        "roiType");

    this.start = new DoubleParameter("Start",
        """
            Start of ROI. For percentiles, give value in % (e.g., 5)""",
        5d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "start");

    this.end = new DoubleParameter("End",
        """
            End of ROI. For percentiles, give value in % (e.g., 95)""",
        95d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "end");

    this.sigFactor = new DoubleParameter("Factor",
        """
            Multiply MAD or IQR with factor f, with usual values being f=3 or f=1.5, respectively""",
        1.5,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "sigFactor");


    this.roiExceptionsStartStop = new SpawnControlParameter(
        "Isotope ROIs",
        "Specify isotope-specific rules for the ROI",
        0,
        new BundleSupplier.RoiStartStopBundleSupplier(),
        true,
        "roiExceptionsStartStop");

    this.roiExceptionsSigFactor = new SpawnControlParameter(
        "Isotope ROIs",
        "Specify isotope-specific rules for the ROI",
        0,
        new BundleSupplier.RoiSigFactorBundleSupplier(),
        true,
        "roiExceptionsSigFactor");

    this.suppressNegativeValues = new BooleanParameter("Negative value handling",
        "Set areas <0 to 0",
        """
            In this method, events from the 'synthetic' population
            are extracted and fed into an 'evaluated' population.
            The start and end of the respective events (the false negatives 'Fneg')
            stem from the in-silico data generator.
            To estimate peak properties, the generator assumes background-free and noise-free event peaks
            to provide an best-case scenario. In this context, it assumes that an event starts when
            the signal exceeds 1 count and ends when the signal drops below 1 count.
            When these peaks are transferred into the context of noisy data, background subtraction may
            overshoot and subtract too much signal. Then negative peak areas may result.
            This checkbox makes sure that the lowest possible signal to obtain is zero
             and not something negative. The respective events may be removed with the follow-up
             check box""",
        false,
        true,
        "suppressNegativeValues"
    );

    removeNegativeValues = new BooleanParameter("Negative value handling",
        "Remove zero area events",
        """
            Removes all events with an area of zero (see 'Negative value handling')""",
        false,
        true,
        "removeNegativeValues"
    );

    listMatches = new BooleanParameter("Match 1/3",
        "List events that match synthetic population",
        """
            Lists all events that have a one-to-one match in the synthetic data series""",
        true,
        false,
        "listMatches"
    );

    listFalsePositives = new BooleanParameter("Match 2/3",
        "List false positive events",
        """
            Lists all events that do no exists in the synthetic data series,
            i.e., events that were falsely identified by the detection algorithm""",
        true,
        false,
        "listFalsePositives"
    );

    listFalseNegatives = new BooleanParameter("Match 3/3",
        "List false negative events",
        """
            Lists all events that only exists in the synthetic data series,
            i.e., events that were not identified by the detection algorithm
            although they do in fact exist""",
        true,
        false,
        "listFalseNegatives"
    );

    organize();
  }

  public FilterParams(FilterParams gatingParams) {
    super(gatingParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(gatingParams.getCommentParameter());
    this.enableBoolean = gatingParams.enableBoolean.copyWithoutChildren();
    this.filterOption = gatingParams.filterOption.copyWithoutChildren();
    this.zIntensity = gatingParams.zIntensity.copyWithoutChildren();
    this.zSegment = gatingParams.zSegment.copyWithoutChildren();
    this.listNumberOfEvents = gatingParams.listNumberOfEvents.copyWithoutChildren();
    this.moavPeriod = gatingParams.moavPeriod.copyWithoutChildren();
    this.smoothTargetOption = gatingParams.smoothTargetOption.copyWithoutChildren();
    //////////////////////
    this.roiID = gatingParams.roiID.copyWithoutChildren();
    this.roiCategory = gatingParams.roiCategory.copyWithoutChildren();
    this.eventParameter = gatingParams.eventParameter.copyWithoutChildren();
    this.mathConversion = gatingParams.mathConversion.copyWithoutChildren();
    this.roiType = gatingParams.roiType.copyWithoutChildren();
    this.start = gatingParams.start.copyWithoutChildren();
    this.end = gatingParams.end.copyWithoutChildren();
    this.sigFactor = gatingParams.sigFactor.copyWithoutChildren();

    this.roiExceptionsStartStop = gatingParams.roiExceptionsStartStop.copyWithoutChildren();
    this.roiExceptionsSigFactor = gatingParams.roiExceptionsSigFactor.copyWithoutChildren();

    this.suppressNegativeValues = gatingParams.suppressNegativeValues.copyWithoutChildren();
    this.removeNegativeValues = gatingParams.removeNegativeValues.copyWithoutChildren();
    this.listMatches = gatingParams.listMatches.copyWithoutChildren();
    this.listFalsePositives = gatingParams.listFalsePositives.copyWithoutChildren();
    this.listFalseNegatives = gatingParams.listFalseNegatives.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new FilterParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new FilterParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new FilterParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Register parent
    super.setParentParameters(enableBoolean);

    enableBoolean.addConditionalChild(true, filterOption);

    // Depending children
    filterOption.addConditionalChild(
        FilterOptions.OVERLAP,
        zIntensity, zSegment, smoothTargetOption, listNumberOfEvents);

    smoothTargetOption.addUnconditionalChild(moavPeriod);

    filterOption.addConditionalChild(
        FilterOptions.ROI_REGION,
        roiID,
        eventParameter,
        roiType,
        roiCategory);

    roiCategory.addConditionalChild(RoiCategory.ABSOLUTE_VALUES, mathConversion, start, end,
        roiExceptionsStartStop);
    roiCategory.addConditionalChild(RoiCategory.PERCENTILES, start, end, roiExceptionsStartStop);
    roiCategory.addConditionalChild(RoiCategory.IQR, mathConversion, sigFactor, roiExceptionsSigFactor);
    roiCategory.addConditionalChild(RoiCategory.MAD, mathConversion, sigFactor, roiExceptionsSigFactor);


    filterOption.addConditionalChild(FilterOptions.MATCH_SIM,
        listMatches,
        listFalsePositives,
        listFalseNegatives);
    listFalseNegatives.addConditionalChild(true, suppressNegativeValues);
    suppressNegativeValues.addConditionalChild(true, removeNegativeValues);
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
          case "filterOption" -> filterOption;
          /// ///////////////////////
          case "zIntensity" -> zIntensity;
          case "zSegment" -> zSegment;
          case "moavPeriod" -> moavPeriod;
          case "listNumberOfEvents" -> listNumberOfEvents;
          case "smoothTargetOption" -> smoothTargetOption;
          /// ///////////////////////
          case "roiID" -> roiID;
          case "roiCategory" -> roiCategory;
          case "eventParameter" -> eventParameter;
          case "mathConversion" -> mathConversion;
          case "roiType" -> roiType;
          case "start" -> start;
          case "end" -> end;
          case "sigFactor" -> sigFactor;
          case "roiExceptionsStartStop" -> roiExceptionsStartStop;
          case "roiExceptionsSigFactor" -> roiExceptionsSigFactor;
          ///
          case "suppressNegativeValues" -> suppressNegativeValues;
          case "removeNegativeValues" -> removeNegativeValues;
          case "listMatches" -> listMatches;
          case "listFalsePositives" -> listFalsePositives;
          case "listFalseNegatives" -> listFalseNegatives;

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
    return AvailableParameterSets.GENERAL_FILTER;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////


  public Parameter<Boolean> getEnableBoolean() {
    return enableBoolean;
  }

  public Parameter<FilterOptions> getFilterOption() {
    return filterOption;
  }

  public Parameter<Double> getzIntensity() {
    return zIntensity;
  }

  public Parameter<Double> getzSegment() {
    return zSegment;
  }

  public Parameter<Boolean> getListNumberOfEvents() {
    return listNumberOfEvents;
  }

  public Parameter<Integer> getMoavPeriod() {
    return moavPeriod;
  }

  public Parameter<OverlapSmoothOption> getSmoothTargetOption() {
    return smoothTargetOption;
  }

  public Parameter<String> getRoiID() {
    return roiID;
  }

  public Parameter<RoiCategory> getRoiCategory() {
    return roiCategory;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }

  public Parameter<MathMod> getMathConversion() {
    return mathConversion;
  }

  public Parameter<RoiType> getRoiType() {
    return roiType;
  }

  public Parameter<Double> getStart() {
    return start;
  }

  public Parameter<Double> getEnd() {
    return end;
  }

  public Parameter<Double> getSigFactor() {
    return sigFactor;
  }

  public List<RoiStartStopBundle> getRoiStartStopBundles() {
    List<RoiStartStopBundle> elementBundles = new ArrayList<>();

    if (roiExceptionsStartStop instanceof SpawnControlParameter) {
      SpawnControlParameter spawnParam = (SpawnControlParameter) roiExceptionsStartStop;

      List<ParamBundle> bundles = spawnParam.getActiveBundlesForProcessing();

      for (ParamBundle bundle : bundles) {
        if (bundle instanceof RoiStartStopBundle) {
          elementBundles.add((RoiStartStopBundle) bundle);
        }
      }
    }
    return elementBundles;
  }

  public List<RoiSigFactorBundle> getRoiSigFactorBundles() {
    List<RoiSigFactorBundle> elementBundles = new ArrayList<>();

    if (roiExceptionsSigFactor instanceof SpawnControlParameter) {
      SpawnControlParameter spawnParam = (SpawnControlParameter) roiExceptionsSigFactor;

      List<ParamBundle> bundles = spawnParam.getActiveBundlesForProcessing();

      for (ParamBundle bundle : bundles) {
        if (bundle instanceof RoiSigFactorBundle) {
          elementBundles.add((RoiSigFactorBundle) bundle);
        }
      }
    }
    return elementBundles;
  }

  public Parameter<Boolean> getSuppressNegativeValues() {
    return suppressNegativeValues;
  }

  public Parameter<Boolean> getRemoveNegativeValues() {
    return removeNegativeValues;
  }

  public Parameter<Boolean> getListMatches() {
    return listMatches;
  }

  public Parameter<Boolean> getListFalseNegatives() {
    return listFalseNegatives;
  }

  public Parameter<Boolean> getListFalsePositives() {
    return listFalsePositives;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final FilterParams defaults = new FilterParams();

    // Fix missing fields from old serialized versions: we have to use
    if (moavPeriod == null) {
      this.moavPeriod = defaults.moavPeriod;
    }
    if (smoothTargetOption == null) {
      this.smoothTargetOption = defaults.smoothTargetOption;
    }
    /////////////////////////////////////
    if (roiCategory == null) {
      this.roiCategory = defaults.roiCategory;
    }

    if (roiID == null) {
      this.roiID = defaults.roiID;
    }

    if (eventParameter == null) {
      this.eventParameter = defaults.eventParameter;
    }

    if (mathConversion == null) {
      this.mathConversion = defaults.mathConversion;
    }

    if (roiType == null) {
      this.roiType = defaults.roiType;
    }

    if (start == null) {
      this.start = defaults.start;
    }

    if (end == null) {
      this.end = defaults.end;
    }

    if (sigFactor == null) {
      this.sigFactor = defaults.sigFactor;
    }

    if (roiExceptionsStartStop == null) {
      this.roiExceptionsStartStop = defaults.roiExceptionsStartStop;
    }

    if (roiExceptionsSigFactor == null) {
      this.roiExceptionsSigFactor = defaults.roiExceptionsSigFactor;
    }

    ///
    if (suppressNegativeValues == null) {
      this.suppressNegativeValues = defaults.suppressNegativeValues;
    }

    if (removeNegativeValues == null) {
      this.removeNegativeValues = defaults.removeNegativeValues;
    }
    if (listMatches == null) {
      this.listMatches = defaults.listMatches;
    }

    if (listFalsePositives == null) {
      this.listFalsePositives = defaults.listFalsePositives;
    }

    if (listFalseNegatives == null) {
      this.listFalseNegatives = defaults.listFalseNegatives;
    }

  }

}
