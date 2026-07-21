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

import dataModelNew.mz.Channel;
import dataModelNew.mz.MZValue;
import gui.util.TextFormatterOption;
import io.XmlUtil;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

public class NormalSearchParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "SearchParams";

  private Parameter<Boolean> enableBoolean;

  private final Parameter<PopulationType> targetPopulation;
  private final Parameter<SearchAlgorithm> searchAlgorithm;
  private Parameter<SmoothType> smoothType;
  private final ThresholdBundle startCriterium;
  private final ThresholdBundle stopCriterium;
  private final Parameter<Double> startStopOffset;
  private final ThresholdBundle heightCriterium;

  private Parameter<Double> windowBonusWidth;
  private Parameter<Double> windowWidth;

  private final Parameter<Double> smoothGaussianWidth;

  private ThresholdBundle prescreenStartStopCriterium;
  private ThresholdBundle prescreenHeightCriterium;
  private Parameter<Integer> prescreenMinEvents;
  private ThresholdBundle pValueStartStop;
  private ThresholdBundle pValueThreshold;
  private Parameter<Boolean> enableSingleComponentPValueThreshold;
  private ThresholdBundle singleComponentPValueThreshold;
  private Parameter<IsotopeSelection> isotopeSelection;
  private Parameter<String> excludedIsotopes;
  private Parameter<String> includedIsotopes;
  private Parameter<CombinedPStatistics> pStatistics;

  private final Parameter<NetCorrectionOption> netCorrectionOption;
  private final Parameter<Double> netTimeWindow;
  private Parameter<MeasureOfLocation> netTimeWindowLocation;
  private Parameter<Boolean> suppressNegativeValues;

  public NormalSearchParams() {
    super("Search parameters", XML_ELEMENT_TAG);

    this.enableBoolean = new BooleanParameter("On/Off",
        "Enable",
        "Activate this sub method",
        true,
        false,
        "enableBoolean"
    );

    this.targetPopulation = new ComboEnumParameter<>(
        "Population",
        """
            This option helps to add structure to the analysis workflow by assigning a role to each search operation:
            
            Each search submethod creates a new population.
            All gates and filters after that search submethod are applied to the corresponding population.
            Adding a new search operation further down in the submethod list allows to create another
            population tree for the same sample.
            
            Example:
            Create a 'mass' population and remove double events using the valley filter. By excluding double events,
            the particle mass (and size) will be more accurate but particle numbers are low.
            Create a 'number' population that does not remove double event to obtain more accurate particle numbers""",
        PopulationType.SIZE,
        PopulationType.getEvaluationCases(),
        PopulationType.class,
        false,
        "targetPopulation"
    );

    this.searchAlgorithm = new ComboEnumParameter<>(
        "Search algorithm",
        "Search strategy",
        SearchAlgorithm.SPLIT_CORRECTION,
        SearchAlgorithm.getActiveValues(),
        SearchAlgorithm.class,
        false,
        "searchAlgorithm"
    );

    this.smoothType = new ComboEnumParameter<>("Smooth type",
        "Decide the type of smoothing for the data",
        SmoothType.MOAV,
        new SmoothType[]{SmoothType.MOAV, SmoothType.SAVITZKY_GOLAY},
        SmoothType.class,
        false,
        "rawSmoothType");

    this.startCriterium = new ThresholdBundle(
        "Start threshold",
        "",
        SearchThresholdOption.MEAN,
        SearchThresholdOption.values(),
        "startCriterium");

    this.stopCriterium = new ThresholdBundle(
        "Stop threshold",
        "",
        SearchThresholdOption.MEAN,
        SearchThresholdOption.values(),
        "stopCriterium");

    this.startStopOffset = new DoubleParameter(
        "Start/stop offset",
        """
            Sometimes, rounding issues, csv-export/import issues or floating point arithmetic
            may lead to poor detection conditions. For instance, if the mean of the baseline
            is determined to be µ = 1.005 cts and this is used as the start/stop condition,
            (smoothed) event data points with I = 1 cts would be rejected. This may split peaks
            in half, yielding not ideal results. Hence, you may reduce (or increase) the start/stop
            threshold using this parameter. Negative values reduce the start/stop threshold""",
        0d,
        NF.D1C2,
        TextFormatterOption.ASSURE_DOUBLE,
        true,
        "startStopOffset");

    this.heightCriterium = new ThresholdBundle(
        "Event height filter",
        """
            If height filter is smaller than start and/or stop, it will have no effect.
            Else, events that are identified via the start/stop condition are only valid
            if their peak is above the event height filter.
            Note that this behaviour is similar to the gate filter approach""",
        SearchThresholdOption.FACTOR,
        SearchThresholdOption.getLargerThanMu(),
        "heightFilter");

    this.windowBonusWidth = new DoubleParameter(
        "Bonus points [µs]",
        """
            Each data point above the start threshold is extended in x direction (i.e., time)
            to improve detection for noisy peaks especially at low dwell times.
            This number gives 'bonus points' to add, depending on the signal intensity:
            At low signal (y = start threshold), spTool extends the peak around any given data point
            above the start threshold. The number of these points added is computed from this
            parameter and the dwell time (e.g., @ DT = 20 µs, 'Bonus points' = 60 µs adds 3 points).
            
            At higher signal intensity, fewer points will be added (scaling with: 1/intensity).
            Usually, peaks with high signal intensity have enough signal to be detected anyway
            and adding too many points increases the danger to cause peak overlap""",
        20d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "windowBonusWidth");

    windowWidth = new DoubleParameter(
        "Window [µs]",
        """
            Each data point above the start threshold is extended in x direction (i.e., time)
            to improve detection for noisy peaks especially at low dwell times.
            
            For each data point above the start threshold, 
            all data points a time window of the width specified here be will added to the peak.""",
        20d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "windowWidth");

    this.smoothGaussianWidth = new DoubleParameter(
        "Peak width [µs]",
        """
            Use moving average smoothing before search to identify indices of events.
            Important: this may induce artifacts if the background fluctuates heavily as little
            'bumps' in the background signal might be amplified by smoothing artifacts.
            However, it may as well recover very small events that would otherwise not be detected""",
        150d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "smoothGaussianWidth");
    /* Prev description of the gaussianSmooth
            Areas, heights, ..., will be calculated from the non-smoothed data.
            This width parameter corresponds to the term 'sigma' argument in a Gaussian peak,
            which determines the width. It is calculated by dividing the width user input
            by the dwell time to obtain a 'data point' (statistician may call it 'samples')
            based window for the width of the Gaussian kernel used in smoothing.
     */

    // ################################################

    this.prescreenStartStopCriterium = new ThresholdBundle(
        "Prescreen start/stop",
        """
            Prescreening for search using accumulated isotope probabilities via Fisher's method
            This is the start/stop criterion for a split corrected event search""",
        SearchThresholdOption.MEAN,
        SearchThresholdOption.values(),
        "prescreenStartStopCriterium");

    this.prescreenHeightCriterium = new ThresholdBundle(
        "Prescreen height",
        """
            Prescreening for search using accumulated isotope probabilities via Fisher's method
            This is the height criterion for a split corrected event search""",
        SearchThresholdOption.ALPHA,
        SearchThresholdOption.getLargerThanMu(),
        "prescreenHeightCriterium");

    // enforce defaults
    prescreenHeightCriterium.getAlpha().setValue(0.01);

    this.prescreenMinEvents = new IntegerParameter(
        "Prescreen events [#]",
        """
            Fisher's method to combine p values is used to compute
            a combined significance level for multiple isotopes.
            For Fisher's method, the p value accumulation works better when
            noise data, i.e., data where the null hypothesis holds true and no effect is present,
            is excluded right away. Otherwise, those isotopes without an effect would
            'dilute' the effect of the few isotopes that actually are present as particles.
            However, the decision which isotopes to include must not be made for each data point
            while computing the Fisher sum: This would bias the statistics.
            Instead, we must select interesting, i.e., non-noise isotopes before computing the score.
            This parameter specifies the minimum number of events required for an isotope to be included.
            The number of events is given with respect to 1 minute of acquisition
            (and adjusted for shorter or longer acquisitions)""",
        100,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "prescreenMinEvents");

    this.pValueThreshold = new ThresholdBundle(
        "Combined significance",
        """
            Use Fisher's method to accumulate p values across multiple isotopes.
            This thresholds sets the significance level for the combined statistics""",
        SearchThresholdOption.ALPHA,
        SearchThresholdOption.getAlphaZ(),
        "pValueThreshold");

    // override defaults here
    pValueThreshold.getAlpha().setValue(0.0005);
    pValueThreshold.getFactor().setValue(3.29);

    pValueStartStop = new ThresholdBundle(
        "Comb. sig. start/stop",
        """
            Use Fisher's method to accumulate p values across multiple isotopes.
            This thresholds sets the significance level for the event start and stop
            using the the combined statistics.
            Alpha = 0.5 indicates the median of the data, i.e., similar to
            normal search procedure that accumulates peaks from the baseline mean""",
        SearchThresholdOption.ALPHA,
        SearchThresholdOption.getAlphaZ(),
        "pValueStartStop");

    // override defaults here
    pValueStartStop.getAlpha().setValue(0.5);
    pValueStartStop.getFactor().setValue(0.0);

    this.enableSingleComponentPValueThreshold = new BooleanParameter("Single significance",
        "Allow single mz exceptions",
        """
            In case there are events that only occur in one element, in the worst case
            a monoisotopic element, Fisher's method may false exclude such elements.
            When e.g., 60 isotopes appear to be non-significant and only one isotope to be significant,
            the p-value combination likely does not capture this single isotope. Why? The p-value
            combination is good at picking up many 'slightly significant', i.e., 'hidden' significances
            but is not the preferred way to find one moderately significant data point
            among many non-significant.
            This, this thresholds is a secondary event detection condition: a region will be
            marked as an event if the lowest p-value of a single isotope is low enough
            without considering the combined p-value. This 'secondary single isotope' p-value should
            be high enough to avoid excess false-positive detections but low enough to capture
            the desired 'significant but single' isotopes""",
        false,
        false,
        "enableSingleComponentPValueThreshold"
    );
    this.singleComponentPValueThreshold = new ThresholdBundle(
        "Single component",
        """
            In case there are events that only occur in one element, in the worst case
            a monoisotopic element, Fisher's method may false exclude such elements.
            When e.g., 60 isotopes appear to be non-significant and only one isotope to be significant,
            the p-value combination likely does not capture this single isotope. Why? The p-value
            combination is good at picking up many 'slightly significant', i.e., 'hidden' significances
            but is not the preferred way to find one moderately significant data point
            among many non-significant.
            This, this thresholds is a secondary event detection condition: a region will be
            marked as an event if the lowest p-value of a single isotope is low enough
            without considering the combined p-value. This 'secondary single isotope' p-value should
            be high enough to avoid excess false-positive detections but low enough to capture
            the desired 'significant but single' isotopes""",
        SearchThresholdOption.ALPHA,
        SearchThresholdOption.getAlphaZ(),
        "singleComponentPValueThreshold");

    // override defaults here
    singleComponentPValueThreshold.getAlpha().setValue(0.0005);
    singleComponentPValueThreshold.getFactor().setValue(3.29);

    this.isotopeSelection = new ComboEnumParameter<>(
        "Rules",
        """
            For this search, all loaded isotopes can be considered.
            The pre-screen algorithm is intended to filter out isotopes that are noise-only.
            However, it is possible to guide this via this option:
            You may decide which isotopes are considered for the pValue computation.
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

    this.pStatistics = new ComboEnumParameter<>(
        "Statistics",
        """
            There are two ways to estimate the combined p-value statistic:
            a) Chi squared distribution.
            b) Gamma distribution.
            Chi square is generally recommended as it should provide better results from theory.
            Exceptions:
            (1) When the background data are very 'Poisson-like'
            (either truly integer Poisson data or simply very low count rates < 2 cts)
            the p-values of the respective isotopes do not follow a uniform distribution.
            This is expected for Poisson-like data but it violates the H0 assumptions for Fisher Chi-square test.
            (2) When the background is not well-know (e.g., it fluctuates, there are small NP in the baseline,
            there are too many NP and the baseline is biased), the uniformity of the background data also breaks down.
            In these cases, it may perform better, to take the combined p-value statistics 'H',
            cap it to a certain value, and fit an empirical Gamma distribution to the H.
            This tends to empirically calibrate the H to obtain uniform combined p-values""",
        CombinedPStatistics.CHI_SQUARE,
        CombinedPStatistics.values(),
        CombinedPStatistics.class,
        false,
        "pStatistics");

    // ################################################

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

  public NormalSearchParams(NormalSearchParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());
    this.enableBoolean = params.enableBoolean.copyWithoutChildren();
    this.targetPopulation = params.targetPopulation.copyWithoutChildren();
    this.searchAlgorithm = params.searchAlgorithm.copyWithoutChildren();
    this.smoothType = params.smoothType.copyWithoutChildren();
    this.startCriterium = params.startCriterium.getCopy();
    this.stopCriterium = params.stopCriterium.getCopy();
    this.startStopOffset = params.startStopOffset.copyWithoutChildren();
    this.heightCriterium = params.heightCriterium.getCopy();
    this.windowBonusWidth = params.windowBonusWidth.copyWithoutChildren();
    this.windowWidth = params.windowWidth.copyWithoutChildren();
    this.smoothGaussianWidth = params.smoothGaussianWidth.copyWithoutChildren();
    this.prescreenStartStopCriterium = params.prescreenStartStopCriterium.getCopy();
    this.prescreenHeightCriterium = params.prescreenHeightCriterium.getCopy();
    this.prescreenMinEvents = params.prescreenMinEvents.copyWithoutChildren();
    this.pValueThreshold = params.pValueThreshold.getCopy();
    this.pValueStartStop = params.pValueStartStop.getCopy();
    this.isotopeSelection = params.isotopeSelection.copyWithoutChildren();
    this.excludedIsotopes = params.excludedIsotopes.copyWithoutChildren();
    this.includedIsotopes = params.includedIsotopes.copyWithoutChildren();
    this.enableSingleComponentPValueThreshold =
        params.enableSingleComponentPValueThreshold.copyWithoutChildren();
    this.singleComponentPValueThreshold = params.singleComponentPValueThreshold.getCopy();
    this.pStatistics = params.pStatistics.copyWithoutChildren();
    this.netCorrectionOption = params.netCorrectionOption.copyWithoutChildren();
    this.netTimeWindow = params.netTimeWindow.copyWithoutChildren();
    this.netTimeWindowLocation = params.netTimeWindowLocation.copyWithoutChildren();
    this.suppressNegativeValues = params.suppressNegativeValues.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new NormalSearchParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new NormalSearchParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new NormalSearchParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Parent parameters
    super.setParentParameters(
        enableBoolean,
        targetPopulation,
        searchAlgorithm,
        netCorrectionOption
    );

    // Conditional children
    searchAlgorithm.addConditionalChild(
        SearchAlgorithm.SIMPLE,
        heightCriterium.getThresholdOption());

    searchAlgorithm.addConditionalChild(
        SearchAlgorithm.SPLIT_CORRECTION,
        startCriterium.getThresholdOption(),
        stopCriterium.getThresholdOption(),
        startStopOffset,
        heightCriterium.getThresholdOption());

    searchAlgorithm.addConditionalChild(
        SearchAlgorithm.SPLIT_CORRECTION_WINDOW,
        windowWidth,
        windowBonusWidth,
        startCriterium.getThresholdOption(),
        stopCriterium.getThresholdOption(),
        startStopOffset,
        heightCriterium.getThresholdOption());

    searchAlgorithm.addConditionalChild(
        SearchAlgorithm.SPLIT_CORRECTION_SMOOTH,
        smoothType,
        smoothGaussianWidth,
        startCriterium.getThresholdOption(),
        stopCriterium.getThresholdOption(),
        startStopOffset,
        heightCriterium.getThresholdOption());

    searchAlgorithm.addConditionalChild(
        SearchAlgorithm.P_VALUE_ACCUMULATION,
        pStatistics,
        isotopeSelection,
        prescreenStartStopCriterium.getThresholdOption(),
        prescreenHeightCriterium.getThresholdOption(),
        prescreenMinEvents,
        pValueStartStop.getThresholdOption(),
        pValueThreshold.getThresholdOption(),
        enableSingleComponentPValueThreshold
    );

    enableSingleComponentPValueThreshold.addConditionalChild(true,
        singleComponentPValueThreshold.getThresholdOption());

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
                if (isotope != null) {
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
                if (isotope != null) {
                  resultingIsotopes.add(isotope);
                }
              }
              includedIsotopes.setValue(NuInterpreterParams.isotopesToString(resultingIsotopes));
            }
          }
        }));


    //

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

          case "targetPopulation" -> targetPopulation;

          case "searchAlgorithm" -> searchAlgorithm;

          case "smoothType" -> smoothType;

          case "startStopOffset" -> startStopOffset;

          case "startCriterium" -> startCriterium.getThresholdOption();
          case "startCriterium" + ThresholdBundle.XML_ID_ALPHA -> startCriterium.getAlpha();
          case "startCriterium" + ThresholdBundle.XML_ID_FACTOR -> startCriterium.getFactor();

          case "stopCriterium" -> stopCriterium.getThresholdOption();
          case "stopCriterium" + ThresholdBundle.XML_ID_ALPHA -> stopCriterium.getAlpha();
          case "stopCriterium" + ThresholdBundle.XML_ID_FACTOR -> stopCriterium.getFactor();

          case "heightFilter" -> heightCriterium.getThresholdOption();
          case "heightFilter" + ThresholdBundle.XML_ID_ALPHA -> heightCriterium.getAlpha();
          case "heightFilter" + ThresholdBundle.XML_ID_FACTOR -> heightCriterium.getFactor();

          case "windowFactor" -> windowBonusWidth;
          case "windowPeakPercent" -> windowWidth;

          case "smoothGaussianWidth" -> smoothGaussianWidth;

          case "pStatistics" -> pStatistics;
          case "prescreenStartStopCriterium" -> prescreenStartStopCriterium.getThresholdOption();
          case "prescreenStartStopCriterium" + ThresholdBundle.XML_ID_ALPHA ->
              prescreenStartStopCriterium.getAlpha();
          case "prescreenStartStopCriterium" + ThresholdBundle.XML_ID_FACTOR ->
              prescreenStartStopCriterium.getFactor();

          case "prescreenHeightCriterium" -> prescreenHeightCriterium.getThresholdOption();
          case "prescreenHeightCriterium" + ThresholdBundle.XML_ID_ALPHA ->
              prescreenHeightCriterium.getAlpha();
          case "prescreenHeightCriterium" + ThresholdBundle.XML_ID_FACTOR ->
              prescreenHeightCriterium.getFactor();

          case "prescreenMinEvents" -> prescreenMinEvents;

          case "pValueThreshold" -> pValueThreshold.getThresholdOption();
          case "pValueThreshold" + ThresholdBundle.XML_ID_ALPHA -> pValueThreshold.getAlpha();
          case "pValueThreshold" + ThresholdBundle.XML_ID_FACTOR -> pValueThreshold.getFactor();

          case "pValueStartStop" -> pValueStartStop.getThresholdOption();
          case "pValueStartStop" + ThresholdBundle.XML_ID_ALPHA -> pValueStartStop.getAlpha();
          case "pValueStartStop" + ThresholdBundle.XML_ID_FACTOR -> pValueStartStop.getFactor();

          case "isotopeSelection" -> isotopeSelection;
          case "excludedIsotopes" -> excludedIsotopes;
          case "includedIsotopes" -> includedIsotopes;
          case "enableSingleComponentPValueThreshold" -> enableSingleComponentPValueThreshold;
          case "singleComponentPValueThreshold" -> singleComponentPValueThreshold.getThresholdOption();
          case "singleComponentPValueThreshold" + ThresholdBundle.XML_ID_ALPHA ->
              singleComponentPValueThreshold.getAlpha();
          case "singleComponentPValueThreshold" + ThresholdBundle.XML_ID_FACTOR ->
              singleComponentPValueThreshold.getFactor();

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
    return AvailableParameterSets.NORMAL_EVENT_SEARCH;
  }

  //------------------------------------------------------------------------------------------


  public Parameter<PopulationType> getTargetPopulation() {
    return targetPopulation;
  }

  public Parameter<SearchAlgorithm> getSearchAlgorithm() {
    return searchAlgorithm;
  }

  public Parameter<SmoothType> getSmoothType() {
    return smoothType;
  }

  public ThresholdBundle getStartCriterium() {
    return startCriterium;
  }

  public ThresholdBundle getStopCriterium() {
    return stopCriterium;
  }

  public ThresholdBundle getHeightCriterium() {
    return heightCriterium;
  }

  public Parameter<Double> getWindowBonusWidth() {
    return windowBonusWidth;
  }

  public Parameter<Double> getWindowWidth() {
    return windowWidth;
  }

  public Parameter<Double> getSmoothGaussianWidth() {
    return smoothGaussianWidth;
  }


  public Parameter<CombinedPStatistics> getPStatistics() {
    return pStatistics;
  }

  public ThresholdBundle getPrescreenStartStopCriterium() {
    return prescreenStartStopCriterium;
  }

  public ThresholdBundle getPrescreenHeightCriterium() {
    return prescreenHeightCriterium;
  }

  public Parameter<Integer> getPrescreenMinEvents() {
    return prescreenMinEvents;
  }

  public ThresholdBundle getpValueStartStop() {
    return pValueStartStop;
  }

  public ThresholdBundle getpValueThreshold() {
    return pValueThreshold;
  }

  public Parameter<Boolean> getEnableSingleComponentPValueThreshold() {
    return enableSingleComponentPValueThreshold;
  }

  public ThresholdBundle getSingleComponentPValueThreshold() {
    return singleComponentPValueThreshold;
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

  public Parameter<NetCorrectionOption> getNetCorrectionOption() {
    return netCorrectionOption;
  }

  public Parameter<Double> getStartStopOffset() {
    return startStopOffset;
  }

  public Parameter<Double> getNetTimeWindow() {
    return netTimeWindow;
  }

  public Parameter<MeasureOfLocation> getNetTimeWindowLocation() {
    return netTimeWindowLocation;
  }

  public Parameter<Boolean> getEnableBoolean() {
    return enableBoolean;
  }

  public Parameter<Boolean> getSuppressNegativeValues() {
    return suppressNegativeValues;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final NormalSearchParams defaults = new NormalSearchParams();

    // Fix missing fields from old serialized versions: we have to use
    if (enableBoolean == null) {
      this.enableBoolean = defaults.enableBoolean;
    }

    if (windowBonusWidth == null) {
      this.windowBonusWidth = defaults.windowBonusWidth;
    }

    if (windowWidth == null) {
      this.windowWidth = defaults.windowWidth;
    }

    if (smoothType == null) {
      this.smoothType = defaults.smoothType;
    }

    if (prescreenStartStopCriterium == null) {
      this.prescreenStartStopCriterium = defaults.prescreenStartStopCriterium;
    }

    if (prescreenHeightCriterium == null) {
      this.prescreenHeightCriterium = defaults.prescreenHeightCriterium;
    }

    if (prescreenMinEvents == null) {
      this.prescreenMinEvents = defaults.prescreenMinEvents;
    }

    if (pValueStartStop == null) {
      this.pValueStartStop = defaults.pValueStartStop;
    }

    if (pValueThreshold == null) {
      this.pValueThreshold = defaults.pValueThreshold;
    }

    if (pStatistics == null) {
      this.pStatistics = defaults.pStatistics;
    }

    if (enableSingleComponentPValueThreshold == null) {
      this.enableSingleComponentPValueThreshold = defaults.enableSingleComponentPValueThreshold;
    }

    if (singleComponentPValueThreshold == null) {
      this.singleComponentPValueThreshold = defaults.singleComponentPValueThreshold;
    }

    if (suppressNegativeValues == null) {
      this.suppressNegativeValues = defaults.suppressNegativeValues;
    }

    if (isotopeSelection == null) {
      this.isotopeSelection = defaults.isotopeSelection;
    }

    if (excludedIsotopes == null) {
      this.excludedIsotopes = defaults.excludedIsotopes;
    }

    if (includedIsotopes == null) {
      this.includedIsotopes = defaults.includedIsotopes;
    }

    if (netTimeWindowLocation == null) {
      this.netTimeWindowLocation = defaults.netTimeWindowLocation;
    }


  }


  //------------------------------------------------------------------------------------------

  public static class ThresholdBundle implements Serializable {

    @Serial
    static final long serialVersionUID = 1L; //assign a long value

    private final Parameter<SearchThresholdOption> thresholdOption;
    private final Parameter<Double> alphaValue;
    private final Parameter<Double> factor;
    private final Parameter<Double> customValue;

    public static final String XML_ID_ALPHA = "AlphaValue";
    public static final String XML_ID_FACTOR = "FactorValue";
    public static final String XML_ID_CUSTOM = "CustomValue";

    public ThresholdBundle(String label, String explanation, SearchThresholdOption option,
                           SearchThresholdOption[] options, String xmlID) {
      this.thresholdOption = new ComboEnumParameter<>(
          label,
          explanation,
          option,
          options,
          SearchThresholdOption.class,
          false,
          xmlID
      );

      this.alphaValue = new DoubleParameter(
          "Alpha",
          "False positive proportion [-] (Not in percent!)",
          5.0E-4, //0.00135 // 0.05
          NF.D1C4Exp,
          TextFormatterOption.ASSURE_POS_EXP_DOUBLE,
          false,
          xmlID + XML_ID_ALPHA
      );

      this.factor = new DoubleParameter(
          "z-Factor",
          "Instead of an alpha value, the corresponding 'f·SD' = 'z·σ' as a critical factor f can be given",
          3.29,// 1.645
          NF.D1C3,
          TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
          false,
          xmlID + XML_ID_FACTOR
      );

      this.customValue = new DoubleParameter(
          "Custom",
          "Give a custom threshold value [cts]",
          10d,
          NF.D1C2,
          TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
          false,
          xmlID + XML_ID_CUSTOM
      );

      organize();
    }

    // Copy
    public ThresholdBundle(ThresholdBundle bundle) {
      this.thresholdOption = bundle.thresholdOption.copyWithoutChildren();
      this.alphaValue = bundle.alphaValue.copyWithoutChildren();
      this.factor = bundle.factor.copyWithoutChildren();
      this.customValue = bundle.customValue.copyWithoutChildren();

      organize();
    }

    public ThresholdBundle getCopy() {
      return new ThresholdBundle(this);
    }

    private void organize() {
      thresholdOption.addConditionalChild(SearchThresholdOption.ALPHA, alphaValue);
      thresholdOption.addConditionalChild(SearchThresholdOption.FACTOR, factor);
      thresholdOption.addConditionalChild(SearchThresholdOption.CUSTOM_VALUE, customValue);

      // Add decoration: unsure if this was a smart idea here...
//      factor.setDecoration(new ConversionButtonDecoration<>(
//          "/img/calculator.png",
//          "Calculate z-values",
//          new AlphaToCritFactorConversion()));
    }

    public Parameter<SearchThresholdOption> getThresholdOption() {
      return thresholdOption;
    }

    public Parameter<Double> getAlpha() {
      return alphaValue;
    }

    public Parameter<Double> getFactor() {
      return factor;
    }

    public Parameter<Double> getCustomValue() {
      return customValue;
    }
  }


}
