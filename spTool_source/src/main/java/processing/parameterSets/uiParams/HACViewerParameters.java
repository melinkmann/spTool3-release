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

import core.SpTool3Main;
import dataModelNew.mz.MZValue;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.nu.IsotopePtoeDialog;
import javafx.stage.Window;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.DistanceOptions;
import processing.parameterSets.*;
import processing.parameterSets.impl.NuInterpreterParams;
import processing.parameters.*;
import sandbox.montecarlo.Isotope;
import util.Functional;
import util.NF;

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

  private final Parameter<Boolean> excludeElements;
  private final Parameter<String> excludedElements;

  private final Parameter<Boolean> useLog2;
  private final Parameter<Boolean> useZScore;
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

    useZScore = new BooleanParameter(
        "Data",
        "z-score normalization",
        """
            The intensity data will be normalized by computing the z-score
            before executing the HAC computation
            """,
        true,
        true,
        "useZScore"
    );

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

    excludeElements = new BooleanParameter(
        "Isotopes",
        "Set exclusion list",
        """
            Any isotope that is selected here will not be considered for the HAC
            """,
        false,
        false,
        "excludeElements"
    );

    excludedElements = new FeedbackStringParameter(
        "",
        """
            Use the m/z button on the right to select the excluded isotopes
            """,
        "",
        TextFormatterOption.ALL_PASS,
        false,
        "excludedElements"
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

    useLog2ForCosine= new BooleanParameter(
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
    excludeElements = iclPeakViewer.excludeElements.copyWithoutChildren();
    excludedElements = iclPeakViewer.excludedElements.copyWithoutChildren();
    useLog2 = iclPeakViewer.useLog2.copyWithoutChildren();
    useZScore = iclPeakViewer.useZScore.copyWithoutChildren();
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
        excludeElements,
        useLog2,
        useZScore,
        thresholdOption,
        applyCosineFlattening,
        new NoFieldTextParameter("Dendrogram & HAC", ""),
        showClusterNumbers,
        useLogScaleDendrogram,
        new NoFieldTextParameter("Pie charts", ""),
        minFractionPct,
        minClusterSizePie
    );

    excludeElements.addConditionalChild(true, excludedElements);

    excludedElements.setDecoration(new ParamSetterButtonDecoration<>("Select elements", "/img/tableTrace.png",
        new Functional() {

          @Override
          public void proceed() {
            proceed(null);
          }

          @Override
          public void proceed(Window window) {
            List<Isotope> prevSel = NuInterpreterParams.isotopeFromString(excludedElements.getValue());

            IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
                window,
                dataModelNew.mz.Element.getAllIsotopes(),
                prevSel);

            List<MZValue> resultingMZ = dlg.showAndWait();
            if (resultingMZ != null) {
              List<Isotope> resultingIsotopes = new ArrayList<>();
              for (MZValue mzValue : resultingMZ) {
                resultingIsotopes.add(mzValue.getIsotope());
              }
              excludedElements.setValue(NuInterpreterParams.isotopesToString(resultingIsotopes));
            }
          }
        }));

    thresholdOption.addConditionalChild(DistanceOptions.CUSTOM, distanceThreshold);

    applyCosineFlattening.addConditionalChild(true, useLog2ForCosine,cosineScore);
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
          case "excludeElements" -> excludeElements;
          case "excludedElements" -> excludedElements;
          case "useLog2" -> useLog2;
          case "useZScore" -> useZScore;
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

  public Parameter<Boolean> getExcludeElements() {
    return excludeElements;
  }

  public Parameter<String> getExcludedElements() {
    return excludedElements;
  }

  public Parameter<Double> getIntensityThreshold() {
    return intensityThreshold;
  }

  public List<Isotope> listExcludedElements() {
    return NuInterpreterParams.isotopeFromString(excludedElements.getValue());
  }

  public Parameter<Boolean> getUseLog2() {
    return useLog2;
  }

  public Parameter<Boolean> getUseZScore() {
    return useZScore;
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
}