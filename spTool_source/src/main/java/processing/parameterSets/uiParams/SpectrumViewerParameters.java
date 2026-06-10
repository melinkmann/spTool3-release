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
import dataModelNew.mz.Channel;
import dataModelNew.mz.MZValue;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.nu.IsotopePtoeDialog;
import javafx.stage.Window;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.NormalizationType;
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
 * --> Do the same also for WHICH viewers are shown in the UI :)
 */


/**
 * Remember to also register a new ParameterSet in the dictionary!
 *
 * @link {@link XmlInstanceDictionary}
 */

public class SpectrumViewerParameters extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public static final String XML_ELEMENT_TAG = "SpectrumViewerParameters";


  private final Parameter<Boolean> normalizeSignal;
  private final Parameter<NormalizationType> normalizeSignalType;
  private final Parameter<Boolean> yLog;

  private final Parameter<Boolean> showLabels;
  private final Parameter<Boolean> excludeIsotopes;
  private final Parameter<String> excludedIsotopes;
  private final Parameter<Boolean> filterAbundance;
  private final Parameter<Double> minAbundancePct;
  private final Parameter<Boolean> filterSignal;
  // just normalize from 0-1 and cut all below 5%?
  private final Parameter<Double> minSignalNormalized;

  private final Parameter<Double> stickWidth;

  private final Parameter<Boolean> limitAxes;
  private final Parameter<Double> lowerYLimit;
  private final Parameter<Double> upperYLimit;
  private final Parameter<Double> lowerXLimit;
  private final Parameter<Double> upperXLimit;

  private final Parameter<Double> colorAlpha;


  public SpectrumViewerParameters() {
    super("Spectral viewer parameters", XML_ELEMENT_TAG);

    normalizeSignal = new BooleanParameter(
        "Normalize",
        "Normalize signal",
        """
            """,
        false,
        false,
        "normalizeSignal"
    );

    normalizeSignalType = new ComboEnumParameter<>(
        "Type",
        """
            """,
        NormalizationType.SUM,
        NormalizationType.values(),
        NormalizationType.class,
        false,
        "normalizeSignalType");


    yLog = new BooleanParameter(
        "Scale",
        "y-axis log scale",
        """
            """,
        false,
        false,
        "yLog"
    );

    showLabels = new BooleanParameter(
        "Labels",
        "Show elements",
        """
            """,
        true,
        false,
        "showLabels"
    );

    excludeIsotopes = new BooleanParameter(
        "Rules",
        "Exclude labels",
        """
            """,
        true,
        false,
        "excludeIsotopes"
    );

    excludedIsotopes = new FeedbackStringParameter(
        "",
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

    filterAbundance = new BooleanParameter(
        "Abundance",
        "Hide labels",
        """
            """,
        false,
        false,
        "filterAbundance"
    );

    minAbundancePct = new DoubleParameter(
        "Min. [%]",
        """
            """,
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "minAbundancePct");

    this.filterSignal = new BooleanParameter(
        "Signal",
        "Hide labels",
        """
            """,
        true,
        false,
        "filterSignal"
    );


    minSignalNormalized = new DoubleParameter(
        "Min. [‰]",
        """
            """,
        1d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "minSignalNormalized");


    limitAxes = new BooleanParameter(
        "Axis limits",
        "Set limits",
        """
            """,
        false,
        false,
        "limitAxes"
    );

    lowerYLimit = new DoubleParameter(
        "Lower y",
        """
            """,
        0d,
        NF.D1C6Exp,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "lowerYLimit");

    upperYLimit = new DoubleParameter(
        "Upper y",
        """
            """,
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "upperYLimit");

    lowerXLimit = new DoubleParameter(
        "Lower x",
        """
            """,
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "lowerXLimit");

    upperXLimit = new DoubleParameter(
        "Upper x",
        """
            """,
        0d,
        NF.D1C1,
        TextFormatterOption.ASSURE_DOUBLE,
        false,
        "upperXLimit");

    this.colorAlpha = new DoubleParameter(
        "Alpha",
        "Set the alpha value (transparency) for the dots",
        0.9d,
        NF.D1C3,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "colorAlpha");


    this.stickWidth = new DoubleParameter(
        "Bar width",
        "Set width of the bars/sticks",
        0.5d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "stickWidth");

    organize();
  }

  public SpectrumViewerParameters(SpectrumViewerParameters iclPeakViewer) {
    super(iclPeakViewer.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(iclPeakViewer.getCommentParameter());

    normalizeSignal = iclPeakViewer.normalizeSignal.copyWithoutChildren();
    normalizeSignalType = iclPeakViewer.normalizeSignalType.copyWithoutChildren();
    yLog = iclPeakViewer.yLog.copyWithoutChildren();
    showLabels = iclPeakViewer.showLabels.copyWithoutChildren();
    this.excludeIsotopes = iclPeakViewer.excludeIsotopes.copyWithoutChildren();
    this.excludedIsotopes = iclPeakViewer.excludedIsotopes.copyWithoutChildren();
    filterAbundance = iclPeakViewer.filterAbundance.copyWithoutChildren();
    minAbundancePct = iclPeakViewer.minAbundancePct.copyWithoutChildren();
    minSignalNormalized = iclPeakViewer.minSignalNormalized.copyWithoutChildren();
    filterSignal = iclPeakViewer.filterSignal.copyWithoutChildren();
    this.stickWidth = iclPeakViewer.stickWidth.copyWithoutChildren();
    limitAxes = iclPeakViewer.limitAxes.copyWithoutChildren();
    lowerYLimit = iclPeakViewer.lowerYLimit.copyWithoutChildren();
    upperYLimit = iclPeakViewer.upperYLimit.copyWithoutChildren();
    lowerXLimit = iclPeakViewer.lowerXLimit.copyWithoutChildren();
    upperXLimit = iclPeakViewer.upperXLimit.copyWithoutChildren();

    colorAlpha = iclPeakViewer.colorAlpha.copyWithoutChildren();

    organize();
  }


  @Override
  public ParamSet getNewInstance() {
    return new SpectrumViewerParameters();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new SpectrumViewerParameters(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new SpectrumViewerParameters(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {

    // Register parent
    super.setParentParameters(
        normalizeSignal,
        yLog,
        showLabels,
        stickWidth,
        limitAxes,
        colorAlpha
    );

    normalizeSignal.addConditionalChild(true, normalizeSignalType);

    showLabels.addConditionalChild(true, filterAbundance, filterSignal, excludeIsotopes);
    filterAbundance.addConditionalChild(true, minAbundancePct);
    excludeIsotopes.addConditionalChild(true, excludedIsotopes);
    filterSignal.addConditionalChild(true, minSignalNormalized);

    limitAxes.addConditionalChild(true,
        lowerYLimit,
        upperYLimit,
        lowerXLimit,
        upperXLimit
    );

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

          case "normalizeSignal" -> normalizeSignal;
          case "normalizeSignalType" -> normalizeSignalType;
          case "yLog" -> yLog;
          case "showLabels" -> showLabels;
          case "excludeIsotopes" -> excludeIsotopes;
          case "excludedIsotopes" -> excludedIsotopes;
          case "filterAbundance" -> filterAbundance;
          case "minAbundancePct" -> minAbundancePct;
          case "filterSignal" -> filterSignal;
          case "minSignalNormalized" -> minSignalNormalized;
          case "stickWidth"->stickWidth;
          case "limitAxes" -> limitAxes;
          case "lowerYLimit" -> lowerYLimit;
          case "upperYLimit" -> upperYLimit;
          case "lowerXLimit" -> lowerXLimit;
          case "upperXLimit" -> upperXLimit;

          case "colorAlpha" -> colorAlpha;
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
    return new Viewers.SpectrumViewer(this);
  }

  public Parameter<Boolean> getNormalizeSignal() {
    return normalizeSignal;
  }

  public Parameter<NormalizationType> getNormalizeSignalType() {
    return normalizeSignalType;
  }

  public Parameter<Boolean> getyLog() {
    return yLog;
  }

  public Parameter<Boolean> getShowLabels() {
    return showLabels;
  }

  public Parameter<Boolean> getExcludeIsotopes() {
    return excludeIsotopes;
  }

  public Parameter<String> getExcludedIsotopes() {
    return excludedIsotopes;
  }

  public List<Isotope> listExcludedIsotopes() {
    return NuInterpreterParams.isotopeFromString(excludedIsotopes.getValue());
  }

  public Parameter<Boolean> getFilterAbundance() {
    return filterAbundance;
  }

  public Parameter<Double> getMinAbundancePct() {
    return minAbundancePct;
  }

  public Parameter<Boolean> getFilterSignal() {
    return filterSignal;
  }

  public Parameter<Double> getMinSignalNormalized() {
    return minSignalNormalized;
  }

  public Parameter<Double> getStickWidth() {
    return stickWidth;
  }

  public Parameter<Boolean> getLimitAxes() {
    return limitAxes;
  }

  public Parameter<Double> getLowerYLimit() {
    return lowerYLimit;
  }

  public Parameter<Double> getUpperYLimit() {
    return upperYLimit;
  }

  public Parameter<Double> getLowerXLimit() {
    return lowerXLimit;
  }

  public Parameter<Double> getUpperXLimit() {
    return upperXLimit;
  }

  public Parameter<Double> getColorAlpha() {
    return colorAlpha;
  }
}
