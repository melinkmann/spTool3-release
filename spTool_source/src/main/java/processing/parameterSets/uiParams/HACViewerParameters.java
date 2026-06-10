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

import dataModelNew.mz.Channel;
import dataModelNew.mz.MZValue;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.nu.IsotopePtoeDialog;
import javafx.stage.Window;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.DistanceOptions;
import processing.options.IsotopeSelection;
import processing.options.ZScoreTarget;
import processing.parameterSets.*;
import processing.parameterSets.impl.NuInterpreterParams;
import processing.parameters.*;
import sandbox.montecarlo.Isotope;
import util.Functional;
import util.NF;

import java.io.IOException;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class HACViewerParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  public static final String XML_ELEMENT_TAG = "HACViewerParameters";

  private final Parameter<Double> minFractionPct;
  private final Parameter<Integer> minClusterSizePie;

  private Parameter<IsotopeSelection> isotopeSelection;
  private Parameter<String> excludedIsotopes;
  private Parameter<String> includedIsotopes;
  private Parameter<Boolean> flattenIsobarsToDefaults;

  private final Parameter<Boolean> useLog2;
  private Parameter<ZScoreTarget> zScoreType;
  private final Parameter<Double> intensityThreshold;

  private final Parameter<DistanceOptions> thresholdOption;
  private final Parameter<Double> distanceThreshold;

  private final Parameter<Boolean> useLogScaleDendrogram;
  private final Parameter<Boolean> showClusterNumbers;

  private final Parameter<Boolean> applyCosineFlattening;
  private final Parameter<Boolean> useLog2ForCosine;
  private final Parameter<Double> cosineScore;


  public HACViewerParameters() {
    super("HAC viewer parameters", XML_ELEMENT_TAG);

    this.zScoreType = new ComboEnumParameter<>(
        "z-score",
        """
            The intensity data will be normalized by computing the z-score
            before executing the HAC computation.
            You may normalize per particle (compute mean and SD of element signal per particle)
            or per isotope (normalize each isotope with its mean and SD across all particles)""",
        ZScoreTarget.PARTICLES,
        ZScoreTarget.values(),
        ZScoreTarget.class,
        false,
        "zScoreType");

    useLog2 = new BooleanParameter(
        "Data",
        "log2 transformation",
        """
            The intensity data will be transformed by computing log2(intensity)
            before executing the HAC computation
            """,
        true,
        true,
        "useLog2"
    );

    thresholdOption = new ComboEnumParameter<>(
        "Threshold",
        "Decide how to compute the distance threshold",
        DistanceOptions.CURVATURE,
        DistanceOptions.values(),
        DistanceOptions.class,
        false,
        "thresholdOption"
    );

    distanceThreshold = new DoubleParameter(
        "Distance THR",
        "Custom distance threshold for HAC clustering",
        3d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "distanceThreshold"
    );


    minFractionPct = new DoubleParameter(
        "Fraction [%]",
        "Minimum fraction to show in pie charts as an element, else shown as 'other'",
        2d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "minFractionPct"
    );

    this.intensityThreshold = new DoubleParameter(
        "Int > [cts]",
        "If an isotope has fewer counts than this number in all particles, " +
            "it will not be fed into the clustering",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "intensityThreshold");

    minClusterSizePie = new IntegerParameter(
        "Cluster size",
        """
            Minimum cluster size for the pie charts:
            Any cluster with fewer particles than this number will not be shown as pie chart.
            The cluster itself will exist in the dendrogram and data set,
            i.e., it will not be merged into other clusters. It is simply hidden from the
            set of visible pie charts
            """,
        50,
        TextFormatterOption.ASSURE_POSITIVE_INTEGER,
        false,
        "minClusterSizePie"
    );

    this.isotopeSelection = new ComboEnumParameter<>(
        "Rules",
        """
            You may decide which isotopes are considered for the align operation:
            a) use all isotopes in the raw data file
            b) use all loaded isotopes (table on the right-hand side of the screen),
            c) use the selected isotopes (in table on the right-hand side of the screen),
            d) 'Set list': create a selection in the submethod that is not affected by
            selecting different isotopes in table on the right-hand side of the screen,
            e) 'Exclude': create a 'blacklist' of isotopes that shall not be included
             (besides these, all loaded isotopes will be used)""",
        IsotopeSelection.ALL_LOADED,
        IsotopeSelection.values(),
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

    this.flattenIsobarsToDefaults = new BooleanParameter("Isobars",
        "Only use default",
        """
            When two isotopes share the same m/z,
            only the 'default' isotope will be shown.
            Defaults are set in the configuration""",
        true,
        false,
        "flattenIsobarsToDefaults"
    );


    useLogScaleDendrogram = new BooleanParameter(
        "y-Axis",
        "Dendrogram: ln-scale",
        """
            """,
        false,
        false,
        "useLogScaleDendrogram"
    );

    this.showClusterNumbers = new BooleanParameter(
        "Cluster #",
        "Identify clusters",
        """
            Show the cluster numbers in the respective region in the dendrogram
            """,
        true,
        true,
        "showClusterNumbers"
    );


    this.applyCosineFlattening = new BooleanParameter(
        "Flatten",
        "Cosine similarity",
        """
            For each cluster from HAC analysis,
            compute cosine-score and merge those cluster that are similar
            """,
        false,
        true,
        "applyCosineFlattening"
    );

    cosineScore = new DoubleParameter(
        "Score",
        "Similarity of 2 clusters must be larger than this value in order to be merged",
        0.95d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        true,
        "cosineScore"
    );

    useLog2ForCosine = new BooleanParameter(
        "Cosine",
        "log2 transformation",
        """
            The intensity data will be transformed by computing log2(intensity)
            before executing the cosine similarity analysis
            """,
        true,
        true,
        "useLog2ForCosine"
    );


    organize();
  }

  public HACViewerParameters(HACViewerParameters iclPeakViewer) {
    super(iclPeakViewer.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakViewer.getCommentParameter());

    minFractionPct = iclPeakViewer.minFractionPct.copyWithoutChildren();
    minClusterSizePie = iclPeakViewer.minClusterSizePie.copyWithoutChildren();
    isotopeSelection = iclPeakViewer.isotopeSelection.copyWithoutChildren();
    excludedIsotopes = iclPeakViewer.excludedIsotopes.copyWithoutChildren();
    includedIsotopes = iclPeakViewer.includedIsotopes.copyWithoutChildren();
    flattenIsobarsToDefaults = iclPeakViewer.flattenIsobarsToDefaults.copyWithoutChildren();
    useLog2 = iclPeakViewer.useLog2.copyWithoutChildren();
    zScoreType = iclPeakViewer.zScoreType.copyWithoutChildren();
    thresholdOption = iclPeakViewer.thresholdOption.copyWithoutChildren();
    distanceThreshold = iclPeakViewer.distanceThreshold.copyWithoutChildren();
    this.useLogScaleDendrogram = iclPeakViewer.useLogScaleDendrogram.copyWithoutChildren();
    showClusterNumbers = iclPeakViewer.showClusterNumbers.copyWithoutChildren();
    this.intensityThreshold = iclPeakViewer.intensityThreshold.copyWithoutChildren();
    applyCosineFlattening = iclPeakViewer.applyCosineFlattening.copyWithoutChildren();
    cosineScore = iclPeakViewer.cosineScore.copyWithoutChildren();
    useLog2ForCosine = iclPeakViewer.useLog2ForCosine.copyWithoutChildren();
    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new HACViewerParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new HACViewerParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new HACViewerParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    super.setParentParameters(
        new NoFieldTextParameter("Prepare data", ""),
        intensityThreshold,
        isotopeSelection,
        // flattenIsobarsToDefaults, // this should occur at tof import...
        useLog2,
        zScoreType,
        thresholdOption,
        applyCosineFlattening,
        new NoFieldTextParameter("Dendrogram & HAC", ""),
        showClusterNumbers,
        useLogScaleDendrogram,
        new NoFieldTextParameter("Pie charts", ""),
        minFractionPct,
        minClusterSizePie
    );

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

            List<Channel> resultingCh = dlg.showAndWait();
            if (resultingCh != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (Channel channel : resultingCh) {
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

            List<Channel> resultingCh = dlg.showAndWait();
            if (resultingCh != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (Channel channel : resultingCh) {
                Isotope isotope = channel.getIsotope();
                if (isotope != null){
                  resultingIsotopes.add(isotope);
                }
              }
              includedIsotopes.setValue(NuInterpreterParams.isotopesToString(resultingIsotopes));
            }
          }
        }));

    thresholdOption.addConditionalChild(DistanceOptions.CUSTOM, distanceThreshold);

    applyCosineFlattening.addConditionalChild(true, useLog2ForCosine, cosineScore);
  }


  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "intensityThreshold" -> intensityThreshold;
          case "minFractionPct" -> minFractionPct;
          case "minClusterSizePie" -> minClusterSizePie;

          case "isotopeSelection" -> isotopeSelection;
          case "excludedIsotopes" -> excludedIsotopes;
          case "includedIsotopes" -> includedIsotopes;
          case "flattenIsobarsToDefaults" -> flattenIsobarsToDefaults;

          case "useLog2" -> useLog2;
          case "zScoreType" -> zScoreType;
          case "thresholdOption" -> thresholdOption;
          case "distanceThreshold" -> distanceThreshold;
          case "useLogScaleDendrogram" -> useLogScaleDendrogram;
          case "showClusterNumbers" -> showClusterNumbers;

          case "applyCosineFlattening" -> applyCosineFlattening;
          case "cosineScore" -> cosineScore;
          case "useLog2ForCosine" -> useLog2ForCosine;

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
    return AvailableParameterSets.EMPTY;
  }

  @Override
  public FxParamSetImpl getObservableInstance() {
    return new Viewers.HACViewer(this);
  }

  // --- Getters ---

  public Parameter<Double> getMinFractionPct() {
    return minFractionPct;
  }

  public Parameter<Integer> getMinClusterSizePie() {
    return minClusterSizePie;
  }

  public Parameter<Double> getIntensityThreshold() {
    return intensityThreshold;
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

  public Parameter<Boolean> getFlattenIsobarsToDefaults() {
    return flattenIsobarsToDefaults;
  }

  public Parameter<Boolean> getUseLog2() {
    return useLog2;
  }

  public Parameter<ZScoreTarget> getzScoreType() {
    return zScoreType;
  }

  public Parameter<DistanceOptions> getOverrideThreshold() {
    return thresholdOption;
  }

  public Parameter<Double> getDistanceThreshold() {
    return distanceThreshold;
  }

  public Parameter<Boolean> getUseLogScaleDendrogram() {
    return useLogScaleDendrogram;
  }

  public Parameter<Boolean> getShowClusterNumbers() {
    return showClusterNumbers;
  }

  public Parameter<Boolean> getApplyCosineFlattening() {
    return applyCosineFlattening;
  }

  public Parameter<Double> getCosineScore() {
    return cosineScore;
  }

  public Parameter<Boolean> getUseLog2ForCosine() {
    return useLog2ForCosine;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final HACViewerParameters defaults = new HACViewerParameters();

    if (isotopeSelection == null) {
      isotopeSelection = defaults.isotopeSelection;
    }

    if (includedIsotopes == null) {
      includedIsotopes = defaults.includedIsotopes;
    }

    if (excludedIsotopes == null) {
      excludedIsotopes = defaults.excludedIsotopes;
    }

    if (flattenIsobarsToDefaults == null) {
      flattenIsobarsToDefaults = defaults.flattenIsobarsToDefaults;
    }

    if (zScoreType == null) {
      zScoreType = defaults.zScoreType;
    }
  }
}