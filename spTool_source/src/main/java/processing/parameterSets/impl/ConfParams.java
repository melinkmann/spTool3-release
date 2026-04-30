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

import core.CpuThreadOption;
import core.SpTool3Main;
import dataModelNew.Sample;
import gui.dialog.notification.NotificationFactory;
import gui.util.TextFormatterOption;
import io.GlobalIO;
import io.XmlUtil;

import java.io.File;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javafx.stage.FileChooser.ExtensionFilter;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.LogLevel;
import processing.options.MathMod;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.*;
import processing.parameters.ComboStringParameter.Matcher;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;
import util.NF;
import util.SnF;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.OkabeItoColors;

public class ConfParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  private static final Logger LOGGER = LogManager.getLogger(ConfParams.class.getName());


  public static final File CONFIG_FILE = GlobalIO.makeConfFile().toFile();
  private static final File METHODS_PATH = GlobalIO.makeMethodsFolder().toFile();

  private static final String RGB_KEY = "isotopeRgbUiColorMap";
  private static final String ISOTOPE_KEY = "defaultIsotopeMap";
  private static final String ISOTOPE_CONFLICT_KEY = "isotopeConflictMap";


  public static final String XML_ELEMENT_TAG = "configuration";

  private final Parameter<String> defaultProjectPath;
  private final Parameter<String> defaultImportPath;
  private final Parameter<Boolean> useMethodsCsvReader;
  private final Parameter<Boolean> createNewSampleSetOnImport;
  private final Parameter<Boolean> incrementNewSampleSetOnImport;
  private final Parameter<String> defaultMethodPath;
  private final Parameter<String> currentMethodFile;
  private final Parameter<CpuThreadOption> numberOfThreadsModel;
  private final Parameter<Integer> numberOfThreads;
  private final Parameter<Integer> dragDropImportFolderDepth;
  private final Parameter<String> dragDropImportFileType;
  private final Parameter<LogLevel> logLevel;
  private final Parameter<Integer> zstdCompressionLevel;
  private final Parameter<Boolean> allowMultithreadingCompression;
  private final Parameter<Double> imageExportDpiLevel;
  private final Parameter<Double> axisLabelSpacing;
  private final Parameter<Boolean> expertMode;

  private final Parameter<Integer> axisFontSize;
  private final Parameter<Boolean> jfreePlotsWhiteOnly;
  private final Parameter<Boolean> lockZoomInGraphs;
  private final Parameter<Boolean> refocusGraphGraphs;

  private final Parameter<Boolean> showReadmeToggle;
  private final Parameter<Boolean> showMethodToggle;
  private final Parameter<Boolean> showTableToggle;
  private final Parameter<Boolean> showRawMCToggle;
  private final Parameter<Boolean> showHistoMCToggle;
  private final Parameter<Boolean> showScatterMCToggle;
  private final Parameter<Boolean> showBoxPlotToggle;
  private final Parameter<Boolean> showSpectrumToggle;
  private final Parameter<Boolean> showHACToggle;
  private final Parameter<Boolean> showSingleEventView;
  private final Parameter<Boolean> showAverageView;
  private final Parameter<Boolean> showIclToggle;
  private final Parameter<Boolean> showCompareHistoToggle;
  private final Parameter<Boolean> showQuantToggle;
  private final Parameter<Boolean> showLoggerToggle;
  private final Parameter<Boolean> loadDockingSizes;

  private final Parameter<String> nuImportSelectedIsotopes;

  private final Parameter<String> isotopeColorIsotopePar;
  // Uses the String value, that is shown in the isotopeNamePar as key to retrieve the parameters.
  private final HashMap<String, ColorParameter> isotopeRgbUiColorMap;
  private final HashMap<String, String> rgbXmlToUiDictionary;

  private final Parameter<String> defaultIsotopeElementPar;
  private final HashMap<String, Parameter<String>> defaultIsotopeMap;

  private final Parameter<String> resolveIsotopeConflictPar;
  private final HashMap<String, Parameter<dataModelNew.mz.Element>> isotopeConflictMap;

  private final Parameter<EventParameter> eventParameter;
  private final Parameter<MathMod> eventMathModification;

  private final Parameter<Double> default_D_mu;
  private final Parameter<Double> default_D_SD;
  private final Parameter<Double> default_v_mu;
  private final Parameter<Double> default_v_SD;
  private final Parameter<Double> default_y_mu;
  private final Parameter<Double> default_y_SD;

  private final Parameter<Double> siaLowerLimit;
  private final Parameter<Double> siaUpperLimit;
  private final Parameter<Double> siaSigmaClip;

  public ConfParams() {
    super("SpTool configuration", XML_ELEMENT_TAG);
    this.defaultProjectPath = new PathParameter(
        "Project path",
        "Default path for project data such as saving projects, exporting data",
        "C:\\",
        false,
        "defaultProjectPath");

    this.defaultImportPath = new PathParameter(
        "Import path",
        "Default path for data import",
        "C:\\",
        false,
        "defaultImportPath");

    this.useMethodsCsvReader = new BooleanParameter(
        "CSV",
        "Use csv instruction of method",
        "When the current method has csv reader instructions, use these for import",
        true,
        false,
        "useMethodsCsvReader");

    this.createNewSampleSetOnImport = new BooleanParameter(
        "Import",
        "Create new 'Import' sample set",
        "Activate this option to create a new sample set called 'Import' after import.",
        true,
        false,
        "createNewSampleSetOnImport");

    this.incrementNewSampleSetOnImport = new BooleanParameter(
        "Name policy",
        "Add incrementing number to each new import",
        """
            Everytime an import is executed, a new 'Import' sample set
            with increasing counter will be created""",
        false,
        false,
        "incrementNewSampleSetOnImport");

    this.defaultMethodPath = new PathParameter(
        "Method path",
        "Root path for methods",
        METHODS_PATH.toString(),
        false,
        "defaultMethodPath");

    this.currentMethodFile = new FileParameter(
        "Current method",
        "Path of the method file that is shown and used as the current method",
        METHODS_PATH.toString(),
        false,
        "currentMethodFile");
    ((FileParameter) currentMethodFile).addFileExtension(new ExtensionFilter("Method files",
        "*" + GlobalIO.METHOD_EXTENSION));

    this.numberOfThreadsModel = new ComboEnumParameter<>(
        "Multithreading",
        "Method to set the number of threads allowed to run in parallel. "
            + "Takes effect after restart",
        CpuThreadOption.CUSTOM,
        CpuThreadOption.values(),
        CpuThreadOption.class,
        false,
        "numberOfThreadsModel");

    this.numberOfThreads = new IntegerParameter(
        "Number of threads",
        "Set custom number of threads allowed to run in parallel."
            + "Takes effect after restart",
        2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "numberOfThreads");

    this.dragDropImportFolderDepth = new IntegerParameter(
        "Drop import depth",
        """
            When drag/dropping a folder into the main window,
            content of the folder will be browsed to find importable files.
            Here, specify how many folder levels you wish to traverse
            """,
        5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "dragDropImportFolderDepth");

    dragDropImportFileType = new StringParameter("File type",
        """
            Limit drag/drop import to a file of this type.
            If you leave this empty, all files will be accepted.
            spTool-specific files such as '.spm' are not affected by this filter""",
        ".csv",
        TextFormatterOption.ALL_PASS,
        true,
        false,
        "dragDropImportFileType"
    );

    this.axisFontSize = new IntegerParameter(
        "Axis font size",
        "Axis font size in graphs",
        13,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "axisFontSize"
    );
    this.jfreePlotsWhiteOnly = new BooleanParameter(
        "Graphs",
        "Make background white",
        """
            Makes region around chart white
            """,
        false,
        false,
        "jfreePlotsWhiteOnly");


    lockZoomInGraphs = new BooleanParameter(
        "Zoom",
        "Lock zoom of graphs",
        "When changing sample/isotope/population selection,"
            + "\nGraphs are redrawn. If you do not want the zoom to change, check this box",
        false,
        false,
        "lockZoomInGraphs");

    refocusGraphGraphs = new BooleanParameter(
        "Focus",
        "Refocus graphs after creation",
        """
            Activate this option if you want to navigate graphs quickly.
            After each redraw, the graph will be focused by the UI so that arrow navigation and so forth
            will be available.
            Deactivate this option if you want to keep text fields and other settings in focus instead.
            """,
        false,
        true,
        "refocusGraphGraphs");

    logLevel = new ComboEnumParameter<>(
        "Logger level",
        "Initial level of the logger in the logger view",
        LogLevel.TRACE,
        LogLevel.values(),
        LogLevel.class,
        false,
        "logLevel");

    zstdCompressionLevel = new IntegerParameter(
        "Compression level",
        """
            Pick a compression level for project files:
                1: fastest
                3: default
                10-15: smaller files at slower speed
                22: highest compression level but very slow""",
        10,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "zstdCompressionLevel");

    allowMultithreadingCompression = new BooleanParameter(
        "Compression",
        "Enable multithreading",
        """
            This may speed up saving projects depending on your system
            CPS and RAM""",
        false,
        false,
        "allowMultithreadingCompression");

    imageExportDpiLevel = new DoubleParameter("Figure DPI",
        """
            For the export, choose how strongly the image is scaled up.
            At one, resolution will be as is on screen.
            Larger numbers significantly increase quality,
            change plot area size,
            and increase image file size
            """,
        10d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "imageExportDpiLevel");

    this.axisLabelSpacing = new DoubleParameter(
        "Axis label space",
        """
            Spacing between the main tick labels of axis in pts.
            Reduce this number to, e.g., 5 whe small cropped figures show not enough main tick labels""",
        10d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "axisLabelSpacing");

    this.expertMode = new BooleanParameter(
        "Expert mode",
        "Show advanced options",
        "Show all advanced parameters and options in the user interface",
        false,
        false,
        "expertMode");

    //
    this.showReadmeToggle = new BooleanParameter(
        "Graphs",
        "Show readme",
        "Show the readme section in the user interface",
        true,
        false,
        "showReadmeToggle");

    this.showMethodToggle = new BooleanParameter(
        "Graphs",
        "Show method",
        "Show the method section in the user interface",
        true,
        false,
        "showMethodToggle");

    this.showTableToggle = new BooleanParameter(
        "Graphs",
        "Show table",
        "Show the table section in the user interface",
        true,
        false,
        "showTableToggle");

    this.showRawMCToggle = new BooleanParameter(
        "Graphs",
        "Show raw data view",
        "Show the monte carlo raw data section in the user interface",
        true,
        false,
        "showRawMCToggle");

    this.showHistoMCToggle = new BooleanParameter(
        "Graphs",
        "Show histogram view",
        "Show the monte carlo histogram section in the user interface",
        true,
        false,
        "showHistoMCToggle");

    this.showBoxPlotToggle = new BooleanParameter(
        "Graphs",
        "Show box plots",
        "Visualize data as box plots",
        false,
        false,
        "showBoxPlotToggle");

    this.showSpectrumToggle = new BooleanParameter(
        "Graphs",
        "Show spectrum",
        "Visualize mass spectrum of population",
        false,
        false,
        "showSpectrumToggle");

    this.showHACToggle = new BooleanParameter(
        "Graphs",
        "Show HAC clustering",
        "Visualize composition of population",
        false,
        false,
        "showHACToggle");

    this.showSingleEventView = new BooleanParameter(
        "Graphs",
        "Show single event view",
        "Visualize single picked event peaks",
        false,
        false,
        "showSingleEventView");

    this.showAverageView = new BooleanParameter(
        "Graphs",
        "Show average event view",
        "Visualize averaged time series",
        false,
        false,
        "showAverageView");

    this.showScatterMCToggle = new BooleanParameter(
        "Graphs",
        "Show scatter plot view",
        "Show the monte carlo scatter plot section in the user interface",
        false,
        false,
        "showScatterMCToggle");

    this.showIclToggle = new BooleanParameter(
        "Graphs",
        "Show ion cloud viewer",
        "Show the ion cloud function section in the user interface",
        false,
        false,
        "showIclToggle");

    this.showCompareHistoToggle = new BooleanParameter(
        "Graphs",
        "Show histograms for peak model tuning",
        "Show section to compare peak shapes as histograms in the user interface",
        !SpTool3Main.getANALYZER(),
        false,
        "showCompareHistoToggle");

    this.showQuantToggle = new BooleanParameter(
        "Graphs",
        "Show quantification manager",
        "Show the quantification section in the user interface",
        SpTool3Main.getANALYZER(),
        false,
        "showQuantToggle");

    this.showLoggerToggle = new BooleanParameter(
        "Graphs",
        "Show logger",
        "Show the logger section in the user interface",
        true,
        false,
        "showLoggerToggle");

    this.loadDockingSizes = new BooleanParameter(
        "Docking",
        "Load sizes and positions",
        """
            Restore the previous positions and sizes
            of undocked windows. Uncheck the box if you changed
            screen setup or if no undocked windows are showing.
            Potentially they are outside of the current screen view.
            Note that this is only possible before undocking is requested
            for the first time. After that, software restart is necessary""",
        true,
        false,
        "loadDockingSizes");

    this.nuImportSelectedIsotopes = new FeedbackStringParameter(
        "NU isotopes",
        """
            For the NU import when using the option 'Prompt with dialog'.
            Here, you may set/store/change the default isotopes that are used in the dialog.
            Note that this does not affect the 'Select' option""",
        "",
        TextFormatterOption.ALL_PASS,
        false,
        "nuImportSelectedIsotopes"
    );


    String[] isotopeNames = dataModelNew.mz.Element.getAllIsotopeFullUINames();

    this.isotopeColorIsotopePar = new ComboStringParameter(
        "Isotope color",
        "Specify the color used for a specific isotope",
        isotopeNames[0],
        isotopeNames,
        true,
        Matcher.getIsotopeMatcher(),
        false,
        "isotopeColorIsotopePar"
    );

    isotopeRgbUiColorMap = new LinkedHashMap<>();
    rgbXmlToUiDictionary = new LinkedHashMap<>();
    for (Isotope isotope : dataModelNew.mz.Element.getAllIsotopes()) {
      Colors defaultColor = Colors.getColor(isotope);
      String rgb = Colors.colorToRgbForXML(defaultColor.getFX());
      isotopeRgbUiColorMap.put(isotope.getFullUIName(), new ColorParameter(
          "Color",
          "RGB Color code",
          rgb,
          false,
          RGB_KEY + "_" + isotope.getXMLCode()
      ));
      // Translate
      rgbXmlToUiDictionary.put(isotope.getXMLCode(), isotope.getFullUIName());
    }

    defaultIsotopeElementPar = new ComboStringParameter(
        "Default isotope",
        """
            Specify the default isotope for each element.
            The default isotope is used in the following cases:
            
            1) It defines which isotopes are selected in the isotope table in the main window
               when you click 'Default'.
            
            2) In the periodic table popup, e.g., used for TOF data import,
               when clicking 'Select all' all elements are selected.
               However, it does not pick all isotopes but only the default isotope.
            
            3) When importing data, e.g., from .csv files, spTool tries to assign an isotope to the data.
               If only an element is given or the isotope cannot be identified due to formatting issues,
               the default isotope will be used to continue""",
        dataModelNew.mz.Element.H.getLongName(),
        dataModelNew.mz.Element.getAllElementNames(),
        true,
        null,
        false,
        "defaultIsotopeElementPar"
    );

    defaultIsotopeMap = new LinkedHashMap<>();
    for (dataModelNew.mz.Element element : dataModelNew.mz.Element.values()) {
      defaultIsotopeMap.put(element.getLongName(), new ComboStringParameter(
          "Isotope",
          "Default isotope for the element",
          element.getMostAbundant().getFullUIName(),
          ArrUtils.stringListToArr(element.getIsotopes().stream()
              .map(Isotope::getFullUIName)
              .collect(Collectors.toList())),
          false,
          null,
          false,
          ISOTOPE_KEY + "_" + element.getLongName()
      ));
    }

    HashMap<Integer, dataModelNew.mz.Element[]> potentialConflictIsotopes =
        dataModelNew.mz.Element.getAllConflictingIsotopicNumbers();
    List<String> potentialConflictMasses = potentialConflictIsotopes.keySet()
        .stream()
        .map(i -> Integer.toString(i))
        .collect(Collectors.toList());
    resolveIsotopeConflictPar = new ComboStringParameter(
        "Conflicting isotopes",
        """
            Specify the default isotope instances of equal nominal masses.
            This is used to resolve isobaric isotope conflicts in the following cases:
            
            1) In the TOF data import using the 'Threshold' option,
               the spectral raw data obviously cannot specify if, e.g., m/z 48 is Ca or Ti.
               In the Nu import submethod you may specify to either load all conflicting isotopes
               or to resolve by using the conflict resolution specified here in the configuration.
               As an example, you can use this parameter to state that m/z 48 shall be loaded as 48Ti.
            
            2) When importing data, e.g., from .csv files, spTool tries to assign an isotope to the data.
               If only an isotopic number is given or the element cannot be identified due to formatting issues,
               the default isotope will be used to continue""",
        potentialConflictMasses.get(0),
        ArrUtils.stringListToArr(potentialConflictMasses),
        true,
        null,
        false,
        "resolveIsotopeConflictPar"
    );

    isotopeConflictMap = new LinkedHashMap<>();
    for (Integer integer : potentialConflictIsotopes.keySet()) {
      String str = Integer.toString(integer);
      isotopeConflictMap.put(str, new ComboEnumParameter<>(
          "Preferred element",
          "Specify the default element for a nominal isotopic number ",
          potentialConflictIsotopes.get(integer)[0],
          potentialConflictIsotopes.get(integer),
          dataModelNew.mz.Element.class,
          false,
          ISOTOPE_CONFLICT_KEY + "_" + str
      ));
    }

    eventParameter = new ComboEnumParameter<>(
        "Table: Event parameter",
        "Choose a custom event parameter that is shown in the results table",
        EventParameter.BACKGROUND_PER_NP,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "eventParameter"
    );

    eventMathModification = new ComboEnumParameter<>(
        "Table: Math",
        "Choose a custom data transformation that is shown in the results table",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "eventMathModification"
    );


    default_D_mu = new DoubleParameter("Diffusion coefficient (D) [cm2/s]",
        """
            Default value for the mean diffusion coefficient of the element in the plasma.
            Higher values lead to broader peaks.
            Why? When the diffusion coefficient of an ion is greater,
            the peak will spread out more strongly (i.e., the ion cloud become more diffuse).
            A good starting point is D=20 cm2/s
            but values closer to 100 cm2/s are also plausible (to obtain broader peaks)""",
        21d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "default_D_mu");

    default_D_SD = new DoubleParameter("Diffusion coefficient (SD)",
        """
            Default value for the standard deviation of D.
            This number specifies how broadly the random numbers vary.
            A good starting point is SD=5 cm2/s""",
        4d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "default_D_SD");

    default_v_mu = new DoubleParameter("Plasma velocity (v) [m/s]",
        """
            Default value for the mean plasma gas linear velocity.
            Higher values lead to narrower peaks.
            Why? Higher velocities lead to a shorter residence time in the plasma
            which means that there is less time available for the ions to diffuse.
            A good starting point is v=15 m/s""",
        15d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "default_v_mu");

    default_v_SD = new DoubleParameter("Plasma velocity (SD) [m/s]",
        """
            Default value for the standard deviation of v.
            This number specifies how broadly the random numbers vary.
            A good starting point is SD=5 m/s""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "default_v_SD");

    default_y_mu = new DoubleParameter("Distance of vaporization (y) [mm]",
        """
            Default value for the mean distance from the sampler orifice where a particle is completely ionized.
            Higher values lead the broader peaks.
            Why? The model assumes an instantaneous vaporization of the particle.
            After that, the ions start to diffuse into the surrounding plasma, causing peak broadening.
            When the distance from the orifice is greater, the residence time in the plasma increases,
            which means that there is more time available for the ions to diffuse.
            A good starting point is y=10 mm""",
        9.5d,
        NF.D1C4,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "default_y_mu");

    default_y_SD = new DoubleParameter("Distance of vaporization (SD) [mm]",
        """
            Default value for the standard deviation of y.
            This number specifies how broadly the random numbers vary.
            A good starting point is SD=3 mm""",
        3d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "default_y_SD");


    siaLowerLimit = new DoubleParameter("SIA lower limit",
        """
            Lower tolerated limit of the SIA shape factor for baseline computation.
            If exceeded, an average of all isotopes is used
            or if that is ill, too, the user specified parameter""",
        0.2d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "siaLowerLimit");

    siaUpperLimit = new DoubleParameter("SIA upper limit",
        """
            Lower tolerated limit of the SIA shape factor for baseline computation.
            If exceeded, an average of all isotopes is used
            or if that is ill, too, the user specified parameter""",
        1d,
        NF.D1C3,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "siaUpperLimit");


    siaSigmaClip = new DoubleParameter("SIA sigma clip",
        """
            Tightens the intensity range for SIA determination.
            Larger values tend to yield larger SIA shape estimates""",
        5d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "siaSigmaClip");

    organize();
  }

  // Copy
  public ConfParams(ConfParams confParams) {
    super(confParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(confParams.getCommentParameter());
    this.defaultProjectPath = confParams.defaultProjectPath.copyWithoutChildren();
    this.defaultImportPath = confParams.defaultImportPath.copyWithoutChildren();
    useMethodsCsvReader = confParams.useMethodsCsvReader.copyWithoutChildren();
    this.createNewSampleSetOnImport = confParams.createNewSampleSetOnImport.copyWithoutChildren();
    this.incrementNewSampleSetOnImport = confParams.incrementNewSampleSetOnImport.copyWithoutChildren();
    this.defaultMethodPath = confParams.defaultMethodPath.copyWithoutChildren();
    this.currentMethodFile = confParams.currentMethodFile.copyWithoutChildren();
    ((FileParameter) this.currentMethodFile).addFileExtension(new ExtensionFilter("Method files",
        "*" + GlobalIO.METHOD_EXTENSION));

    this.numberOfThreadsModel = confParams.numberOfThreadsModel.copyWithoutChildren();
    this.numberOfThreads = confParams.numberOfThreads.copyWithoutChildren();
    this.dragDropImportFolderDepth = confParams.dragDropImportFolderDepth.copyWithoutChildren();
    this.dragDropImportFileType = confParams.dragDropImportFileType.copyWithoutChildren();

    this.logLevel = confParams.logLevel.copyWithoutChildren();
    this.zstdCompressionLevel = confParams.zstdCompressionLevel.copyWithoutChildren();
    this.allowMultithreadingCompression = confParams.allowMultithreadingCompression.copyWithoutChildren();
    this.imageExportDpiLevel = confParams.imageExportDpiLevel.copyWithoutChildren();
    this.axisLabelSpacing = confParams.axisLabelSpacing.copyWithoutChildren();

    this.expertMode = confParams.expertMode.copyWithoutChildren();

    this.axisFontSize = confParams.axisFontSize.copyWithoutChildren();
    this.jfreePlotsWhiteOnly = confParams.jfreePlotsWhiteOnly.copyWithoutChildren();
    this.lockZoomInGraphs = confParams.lockZoomInGraphs.copyWithoutChildren();
    this.refocusGraphGraphs = confParams.refocusGraphGraphs.copyWithoutChildren();
    this.showReadmeToggle = confParams.showReadmeToggle.copyWithoutChildren();
    this.showMethodToggle = confParams.showMethodToggle.copyWithoutChildren();
    this.showTableToggle = confParams.showTableToggle.copyWithoutChildren();
    this.showRawMCToggle = confParams.showRawMCToggle.copyWithoutChildren();
    this.showHistoMCToggle = confParams.showHistoMCToggle.copyWithoutChildren();
    this.showScatterMCToggle = confParams.showScatterMCToggle.copyWithoutChildren();
    this.showBoxPlotToggle = confParams.showBoxPlotToggle.copyWithoutChildren();
    this.showSpectrumToggle = confParams.showSpectrumToggle.copyWithoutChildren();
    this.showHACToggle = confParams.showHACToggle.copyWithoutChildren();
    this.showIclToggle = confParams.showIclToggle.copyWithoutChildren();
    this.showLoggerToggle = confParams.showLoggerToggle.copyWithoutChildren();
    this.showSingleEventView = confParams.showSingleEventView.copyWithoutChildren();
    this.showCompareHistoToggle = confParams.showCompareHistoToggle.copyWithoutChildren();
    this.showAverageView = confParams.showAverageView.copyWithoutChildren();
    this.showQuantToggle = confParams.showQuantToggle.copyWithoutChildren();
    this.loadDockingSizes = confParams.loadDockingSizes.copyWithoutChildren();

    this.eventParameter = confParams.eventParameter.copyWithoutChildren();
    this.eventMathModification = confParams.eventMathModification.copyWithoutChildren();
    this.default_D_mu = confParams.default_D_mu.copyWithoutChildren();
    this.default_D_SD = confParams.default_D_SD.copyWithoutChildren();
    this.default_v_mu = confParams.default_v_mu.copyWithoutChildren();
    this.default_v_SD = confParams.default_v_SD.copyWithoutChildren();
    this.default_y_mu = confParams.default_y_mu.copyWithoutChildren();
    this.default_y_SD = confParams.default_y_SD.copyWithoutChildren();

    this.siaLowerLimit = confParams.siaLowerLimit.copyWithoutChildren();
    this.siaUpperLimit = confParams.siaUpperLimit.copyWithoutChildren();
    this.siaSigmaClip = confParams.siaSigmaClip.copyWithoutChildren();

    this.nuImportSelectedIsotopes = confParams.nuImportSelectedIsotopes.copyWithoutChildren();

    // Default colors

    this.isotopeColorIsotopePar = confParams.isotopeColorIsotopePar.copyWithoutChildren();

    this.isotopeRgbUiColorMap = new LinkedHashMap<>();
    HashMap<String, ColorParameter> thatCodes = confParams.getIsotopeColorCodeMap();
    for (String key : thatCodes.keySet()) {
      if (thatCodes.get(key) != null) {
        isotopeRgbUiColorMap.put(key, (ColorParameter) thatCodes.get(key).copyWithoutChildren());
      }
    }

    // Dictionary
    this.rgbXmlToUiDictionary = new LinkedHashMap<>();
    dataModelNew.mz.Element.getAllIsotopes().forEach(iso ->
        this.rgbXmlToUiDictionary.put(iso.getXMLCode(), iso.getFullUIName()));

    // Default isotopes
    this.defaultIsotopeElementPar = confParams.defaultIsotopeElementPar.copyWithoutChildren();

    defaultIsotopeMap = new LinkedHashMap<>();

    HashMap<String, Parameter<String>> thatIsotopes = confParams.getDefaultIsotopeMap();
    for (String key : thatIsotopes.keySet()) {
      if (thatIsotopes.get(key) != null) {
        defaultIsotopeMap.put(key, thatIsotopes.get(key).copyWithoutChildren());
      }
    }

    this.resolveIsotopeConflictPar = confParams.resolveIsotopeConflictPar.copyWithoutChildren();
    isotopeConflictMap = new LinkedHashMap<>();

    for (String s : confParams.getIsotopeConflictMap().keySet()) {
      isotopeConflictMap.put(s, confParams.getIsotopeConflictMap().get(s).copyWithoutChildren());
    }

    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new ConfParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new ConfParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new ConfParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Register Parents
    if (SpTool3Main.getANALYZER()) {
      super.setParentParameters(
          expertMode,
          defaultProjectPath,
          defaultImportPath,
          dragDropImportFolderDepth,
          dragDropImportFileType,
          useMethodsCsvReader,
          createNewSampleSetOnImport,
          nuImportSelectedIsotopes,
          defaultMethodPath,
          currentMethodFile,
          new SeparatorParameter(),
          isotopeColorIsotopePar,
          defaultIsotopeElementPar,
          resolveIsotopeConflictPar,
          new SeparatorParameter(),
          lockZoomInGraphs,
          axisFontSize,
          jfreePlotsWhiteOnly,
          refocusGraphGraphs,
          imageExportDpiLevel,
          axisLabelSpacing,
          showReadmeToggle,
          showMethodToggle,
          showTableToggle,
          showRawMCToggle,
          showHistoMCToggle,
          showScatterMCToggle,
          showBoxPlotToggle,
          showSpectrumToggle,
          showHACToggle,
          showSingleEventView,
          showCompareHistoToggle,
          showAverageView,
          showQuantToggle,
          showIclToggle,
          showLoggerToggle,
          loadDockingSizes,
          eventParameter,
          eventMathModification,
          new SeparatorParameter(),
          default_D_mu,
          default_D_SD,
          default_v_mu,
          default_v_SD,
          default_y_mu,
          default_y_SD,
          new SeparatorParameter(),
          siaLowerLimit,
          siaUpperLimit,
          siaSigmaClip,
          new SeparatorParameter(),
          logLevel,
          numberOfThreadsModel,
          zstdCompressionLevel,
          allowMultithreadingCompression
      );
    } else {
      super.setParentParameters(
          expertMode,
          defaultProjectPath,
          defaultMethodPath,
          currentMethodFile,
          isotopeColorIsotopePar,
          defaultIsotopeElementPar,
          resolveIsotopeConflictPar,
          lockZoomInGraphs,
          axisFontSize,
          jfreePlotsWhiteOnly,
          refocusGraphGraphs,
          imageExportDpiLevel,
          axisLabelSpacing,
          showReadmeToggle,
          showMethodToggle,
          showTableToggle,
          showRawMCToggle,
          showHistoMCToggle,
          showScatterMCToggle,
          showBoxPlotToggle,
          showSingleEventView,
          showIclToggle,
          showCompareHistoToggle,
          showAverageView,
          showLoggerToggle,
          loadDockingSizes,
          eventParameter,
          eventMathModification,
          default_D_mu,
          default_D_SD,
          default_v_mu,
          default_v_SD,
          default_y_mu,
          default_y_SD,
          siaLowerLimit,
          siaUpperLimit,
          siaSigmaClip,
          logLevel,
          numberOfThreadsModel,
          zstdCompressionLevel,
          allowMultithreadingCompression
      );
    }
    if (!SpTool3Main.SHOW_PEAK_MODEL) {
      super.parentParameters.remove(showIclToggle);
    }

    if (!SpTool3Main.SHOW_PVALUE_EXPORT) {
      super.parentParameters.remove(showSpectrumToggle);
      super.parentParameters.remove(showHACToggle);
    }


    // Attach Children.
    numberOfThreadsModel.addConditionalChild(CpuThreadOption.CUSTOM, numberOfThreads);

    createNewSampleSetOnImport.addConditionalChild(true, incrementNewSampleSetOnImport);

    // RGB tuples
    for (Isotope isotope : dataModelNew.mz.Element.getAllIsotopes()) {
      Parameter<String> rgbPar = isotopeRgbUiColorMap.get(isotope.getFullUIName());
      if (rgbPar != null) {
        isotopeColorIsotopePar.addConditionalChild(isotope.getFullUIName(), rgbPar);
        rgbPar.setDecoration(new ButtonDecoration<>(
            "Reset all colors to their default values",
            "/img/ignorechange.png",
            () -> {
              for (String s : isotopeRgbUiColorMap.keySet()) {
                ColorParameter par = isotopeRgbUiColorMap.get(s);
                if (par != null) {
                  Isotope parsedIsotope = Isotope.getFromFullUIName(s);
                  // Allow "unknown" to have a color
                  if (parsedIsotope != null) {
                    Colors defaultColor = Colors.getColor(parsedIsotope);
                    par.setColor(defaultColor);
                  }
                }
              }
            }));
      }
    }

    // Default isotopes
    for (dataModelNew.mz.Element element : dataModelNew.mz.Element.values()) {
      Parameter<String> isoPar = defaultIsotopeMap.get(element.getLongName());
      if (isoPar != null) {
        defaultIsotopeElementPar.addConditionalChild(element.getLongName(), isoPar);
      }
    }

    // Isotope conflicts
    for (String s : isotopeConflictMap.keySet()) {
      Parameter<dataModelNew.mz.Element> par = isotopeConflictMap.get(s);
      if (par != null) {
        resolveIsotopeConflictPar.addConditionalChild(s, par);
      }
    }

    // Set decoration
    showReadmeToggle.setDecoration(new ImageDecoration<>("/img/readme2.png"));
    showMethodToggle.setDecoration(new ImageDecoration<>("/img/method.png"));
    showRawMCToggle.setDecoration(new ImageDecoration<>("/img/rawViewMC.png"));
    showHistoMCToggle.setDecoration(new ImageDecoration<>("/img/histoViewMC.png"));
    showScatterMCToggle.setDecoration(new ImageDecoration<>("/img/scatter.png"));
    showBoxPlotToggle.setDecoration(new ImageDecoration<>("/img/boxplot.png"));
    showSpectrumToggle.setDecoration(new ImageDecoration<>("/img/spectrumView.png"));
    showHACToggle.setDecoration(new ImageDecoration<>("/img/clusterDendrogram.png"));
    showSingleEventView.setDecoration(new ImageDecoration<>("/img/singlePeakView.png"));
    showIclToggle.setDecoration(new ImageDecoration<>("/img/ICL.png"));
    showLoggerToggle.setDecoration(new ImageDecoration<>("/img/warnInfo.png"));
    showQuantToggle.setDecoration(new ImageDecoration<>("/img/quantTab.png"));
    showTableToggle.setDecoration(new ImageDecoration<>("/img/tab.png"));
    showCompareHistoToggle.setDecoration(new ImageDecoration<>("/img/4by4histo.png"));
    showAverageView.setDecoration(new ImageDecoration<>("/img/averageView.png"));
  }

  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int i = 0; i < nodeList.getLength(); i++) {
      Node node = nodeList.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        String key = element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE);

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (key) {
          // case LABEL_PAR_XML_ID -> super.label; // keep as is!
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "defaultProjectPath" -> defaultProjectPath;
          case "defaultImportPath" -> defaultImportPath;
          case "useMethodsCsvReader" -> useMethodsCsvReader;
          case "defaultMethodPath" -> defaultMethodPath;
          case "currentMethodFile" -> currentMethodFile;
          case "createNewSampleSetOnImport" -> createNewSampleSetOnImport;
          case "incrementNewSampleSetOnImport" -> incrementNewSampleSetOnImport;
          case "numberOfThreadsModel" -> numberOfThreadsModel;
          case "numberOfThreads" -> numberOfThreads;
          case "dragDropImportFolderDepth" -> dragDropImportFolderDepth;
          case "dragDropImportFileType" -> dragDropImportFileType;
          case "logLevel" -> logLevel;
          case "zstdCompressionLevel" -> zstdCompressionLevel;
          case "allowMultithreadingCompression" -> allowMultithreadingCompression;
          case "expertMode" -> expertMode;
          case "axisFontSize" -> axisFontSize;
          case "jfreePlotsWhiteOnly" -> jfreePlotsWhiteOnly;
          case "lockZoomInGraphs" -> lockZoomInGraphs;
          case "imageExportDpiLevel" -> imageExportDpiLevel;
          case "axisLabelSpacing" -> axisLabelSpacing;
          case "refocusGraphGraphs" -> refocusGraphGraphs;
          case "showReadmeToggle" -> showReadmeToggle;
          case "showMethodToggle" -> showMethodToggle;
          case "showTableToggle" -> showTableToggle;
          case "showRawMCToggle" -> showRawMCToggle;
          case "showHistoMCToggle" -> showHistoMCToggle;
          case "showSingleEventView" -> showSingleEventView;
          case "showScatterMCToggle" -> showScatterMCToggle;
          case "showBoxPlotToggle" -> showBoxPlotToggle;
          case "showSpectrumToggle" -> showSpectrumToggle;
          case "showHACToggle" -> showHACToggle;
          case "showIclToggle" -> showIclToggle;
          case "showAverageView" -> showAverageView;
          case "showLoggerToggle" -> showLoggerToggle;
          case "isotopeColorIsotopePar" -> isotopeColorIsotopePar;
          case "defaultIsotopeElementPar" -> defaultIsotopeElementPar;
          case "resolveIsotopeConflictPar" -> resolveIsotopeConflictPar;
          case "showQuantToggle" -> showQuantToggle;
          case "showCompareHistoToggle" -> showCompareHistoToggle;
          case "loadDockingSizes" -> loadDockingSizes;

          case "eventParameter" -> eventParameter;
          case "eventMathModification" -> eventMathModification;
          case "default_D_mu" -> default_D_mu;
          case "default_D_SD" -> default_D_SD;
          case "default_v_mu" -> default_v_mu;
          case "default_v_SD" -> default_v_SD;
          case "default_y_mu" -> default_y_mu;
          case "default_y_SD" -> default_y_SD;

          case "siaLowerLimit" -> siaLowerLimit;
          case "siaUpperLimit" -> siaUpperLimit;
          case "siaSigmaClip" -> siaSigmaClip;

          case "nuImportSelectedIsotopes" -> nuImportSelectedIsotopes;

          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        }

        // Special case: check RGB tuples
        if (key.toLowerCase(Locale.ROOT).contains(RGB_KEY.toLowerCase(Locale.ROOT))) {
          String[] segments = key.split("_");
          // Grab the last segment, i.e., the one after the "_"
          if (segments.length > 0) {
            String xmlRgbKey = segments[segments.length - 1];
            String uiRgbKey = rgbXmlToUiDictionary.get(xmlRgbKey);
            if (uiRgbKey != null) {
              Parameter<String> rgbParam = isotopeRgbUiColorMap.get(uiRgbKey);
              if (rgbParam != null) {
                rgbParam.readFromXmlElement(element);
              }
            }
          }
        }

        // Special case: check default isotopes
        if (key.toLowerCase(Locale.ROOT).contains(ISOTOPE_KEY.toLowerCase(Locale.ROOT))) {
          String[] segments = key.split("_");
          // Grab the last segment, i.e., the one after the "_"
          if (segments.length > 0) {
            String elementKey = segments[segments.length - 1];
            if (elementKey != null) {
              Parameter<String> isoParam = defaultIsotopeMap.get(elementKey);
              if (isoParam != null) {
                isoParam.readFromXmlElement(element);
              }
            }
          }
        }

        // Special case: check isotope conflicts
        if (key.toLowerCase(Locale.ROOT).contains(ISOTOPE_CONFLICT_KEY.toLowerCase(Locale.ROOT))) {
          String[] segments = key.split("_");
          // Grab the last segment, i.e., the one after the "_"
          if (segments.length > 0) {
            String elementKey = segments[segments.length - 1];
            if (elementKey != null) {
              Parameter<dataModelNew.mz.Element> isoParam = isotopeConflictMap.get(elementKey);
              if (isoParam != null) {
                isoParam.readFromXmlElement(element);
              }
            }
          }
        }

        //
      }
    }
  }

  @Override
  public void executeOverridingSave() {
    XmlUtil.writeToXml(this, ConfParams.CONFIG_FILE.toPath());
  }

  // Special case: this file is meant to be stored in only one place
  @Override
  public void executeSaveAs(Path file) {
    XmlUtil.writeToXml(this, file);
    XmlUtil.writeToXml(this, ConfParams.CONFIG_FILE.toPath());
    // We should never get here.
    NotificationFactory.openError("""
        User request to store configuration in custom directory.
        Cannot re-open configuration file from custom directories.
        The configuration was written to the default path.
        If you see this message please contact the SpTool support.""");
  }

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.CONFIGS;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////

  // What to do with the information:
  public int calcParallelThreads() {
    final int recommendedThreads;
    if (numberOfThreadsModel.getValue().equals(CpuThreadOption.CUSTOM)) {
      recommendedThreads = numberOfThreads.getValue();
    } else {
      // inspired by parallel garbage collector's rules
      final int runtimeCores = Runtime.getRuntime().availableProcessors();
      if (runtimeCores < 6) {
        recommendedThreads = runtimeCores;
      } else {
        // recommendedThreads = (int) (0.625 * runtimeCores);
        recommendedThreads = (int) Math.floor(0.84 * runtimeCores);
      }
    }
    return recommendedThreads;
  }

  public Parameter<Integer> getDragDropFolderDepth() {
    return dragDropImportFolderDepth;
  }

  public Parameter<String> getDragDropImportFileType() {
    return dragDropImportFileType;
  }

  public Path getDefaultImportPath() {
    Path path = Path.of(defaultImportPath.getValue());
    return path;
  }

  public Path getDefaultProjectPath() {
    Path path = Path.of(defaultProjectPath.getValue());
    return path;
  }


  public Path getDefaultMethodPath() {
    Path path = Path.of(defaultMethodPath.getValue());
    return path;
  }

  public Path getCurrentMethodFile() {
    return Path.of(currentMethodFile.getValue());
  }

  public void setCurrentMethodFile(Path file) {
    if (file != null && Files.isReadable(file)) {
      currentMethodFile.setValue(file.toString());
    }
  }

  public Parameter<Boolean> getUseMethodsCsvReader() {
    return useMethodsCsvReader;
  }

  public Parameter<Boolean> getCreateNewSampleSet() {
    return createNewSampleSetOnImport;
  }

  public Parameter<Boolean> getIncrementNewSampleSet() {
    return incrementNewSampleSetOnImport;
  }

  public LogLevel getLogLevel() {
    return logLevel.getValue();
  }

  public Parameter<Integer> getZstdCompressionLevel() {
    return zstdCompressionLevel;
  }

  public int calcNumberOfCompressorThreads() {
    int threads = 1;

    if (allowMultithreadingCompression.getValue()) {
      // inspired by parallel garbage collector's rules
      final int runtimeCores = Runtime.getRuntime().availableProcessors();
      if (runtimeCores < 4) {
        threads = runtimeCores;
      } else {
        // recommendedThreads = (int) (0.625 * runtimeCores);
        threads = (int) Math.floor(0.625 * runtimeCores);
      }
    }

    return threads;
  }

  public int getAxisFontSize() {
    return axisFontSize.getValue();
  }

  public Parameter<Boolean> getJfreePlotsWhiteOnly() {
    return jfreePlotsWhiteOnly;
  }

  public boolean isLockZoomInGraphs() {
    return lockZoomInGraphs.getValue();
  }

  public synchronized Boolean getRefocusGraphGraphs() {
    return refocusGraphGraphs.getValue();
  }

  public Parameter<Double> getImageExportDpiLevel() {
    return imageExportDpiLevel;
  }

  public float getImageExportDpiLevelFloat() {
    return imageExportDpiLevel.getValue().floatValue();
  }

  public double getAxisLabelSpacing() {
    return axisLabelSpacing.getValue();
  }

  public boolean showAllParamsAsExpert() {
    return expertMode.getValue();
  }

  public boolean isShowReadme() {
    return showReadmeToggle.getValue();
  }

  public boolean isShowMethod() {
    return showMethodToggle.getValue();
  }

  public boolean isShowTable() {
    return showTableToggle.getValue();
  }

  public boolean isShowRawMC() {
    return showRawMCToggle.getValue();
  }

  public boolean isShowHistoMC() {
    return showHistoMCToggle.getValue();
  }

  public boolean isShowSpectrum() {
    return showSpectrumToggle.getValue();
  }

  public boolean isShowHAC() {
    return showHACToggle.getValue();
  }

  public boolean isShowScatterMC() {
    return showScatterMCToggle.getValue();
  }

  public boolean isShowBoxPlot() {
    return showBoxPlotToggle.getValue();
  }

  public boolean isShowQuantToggle() {
    return showQuantToggle.getValue();
  }

  public boolean isShowIcl() {
    return showIclToggle.getValue();
  }

  public Parameter<Boolean> getShowCompareHistoToggle() {
    return showCompareHistoToggle;
  }

  public Parameter<Boolean> getLoadDockingSizes() {
    return loadDockingSizes;
  }

  public boolean isShowSingleEventView() {
    return showSingleEventView.getValue();
  }

  public Parameter<Boolean> getShowAverageView() {
    return showAverageView;
  }

  public boolean isShowLogger() {
    return showLoggerToggle.getValue();
  }

  public HashMap<String, ColorParameter> getIsotopeColorCodeMap() {
    return isotopeRgbUiColorMap;
  }

  public HashMap<String, Parameter<String>> getDefaultIsotopeMap() {
    return defaultIsotopeMap;
  }

  public HashMap<String, Parameter<dataModelNew.mz.Element>> getIsotopeConflictMap() {
    return isotopeConflictMap;
  }

  public Colors getColor(Sample sample, Isotope isotope) {
    Colors color;
    if (isotope != null) {
      color = getColor(isotope);
    } else {
      color = new SpColor(sample.getColor());
    }
    return color;
  }

  public Colors getColor(Isotope isotope) {
    if (isotope != null) {
      ColorParameter colorParameter = isotopeRgbUiColorMap.get(isotope.getFullUIName());
      if (colorParameter != null) {
        return colorParameter.getColor();
      } else {
        return Colors.getColor(isotope);
      }
    } else {
      return OkabeItoColors.BLACK;
    }
  }

  public void setColor(Isotope isotope, Colors color) {
    if (isotope != null && color != null) {
      ColorParameter colorParameter = isotopeRgbUiColorMap.get(isotope.getFullUIName());
      if (colorParameter != null) {
        colorParameter.setColor(color);
      }
    }
  }

  public Parameter<String> getNuImportSelectedIsotopes() {
    return nuImportSelectedIsotopes;
  }

  public Isotope getDefaultIsotope(dataModelNew.mz.Element element) {
    // Default (Backup initialization)
    Isotope defaultIsotope = element.getMostAbundant();
    // Try to read parameter
    String elementKey = element.getLongName();
    Parameter<String> isotopeParameter = defaultIsotopeMap.get(elementKey);
    if (isotopeParameter != null) {
      String isoParValue = isotopeParameter.getValue();
      isoParValue = isoParValue.replaceAll("[^0-9]", "");
      for (Isotope candidate : element.getIsotopes()) {
        if (candidate.getIsotopicNumber() == SnF.strToInt(isoParValue)) {
          defaultIsotope = candidate;
          break;
        }
      }
    }
    return defaultIsotope;
  }

  @Nullable
  public Isotope resolveConflictOrGet(int isotopicNumber) {
    Isotope suggestion = dataModelNew.mz.Element.UNKNOWN.getMostAbundant();
    String nominalMassStr = Integer.toString(isotopicNumber);
    Parameter<dataModelNew.mz.Element> resolvePar = isotopeConflictMap.get(nominalMassStr);
    if (resolvePar != null) {
      dataModelNew.mz.Element prefElement = resolvePar.getValue();
      for (Isotope isotope : prefElement.getIsotopes()) {
        if (isotope.getIsotopicNumber() == isotopicNumber) {
          suggestion = isotope;
          break;
        }
      }
    } else {
      double maxAbundant = 0;
      for (dataModelNew.mz.Element ele : dataModelNew.mz.Element.values()) {
        for (Isotope isotope : ele.getIsotopes()) {
          if (isotope.getIsotopicNumber() == isotopicNumber && isotope.getAbundance() > maxAbundant) {
            maxAbundant = isotope.getAbundance();
            suggestion = isotope;
          }
        }
      }
      LOGGER.trace("Used most abundant isotope " + suggestion.getName() + " for mz " + isotopicNumber);
      //} else {
      //  // there was no entry in the conflict map with that key --> isotope does not conflict
      //  for (Isotope candidate : dataModelNew.mz.Element.getAllIsotopes()) {
      //    if (isotopicNumber == candidate.getIsotopicNumber()) {
      //      suggestion = candidate;
      //      LOGGER.trace("Had to read/guess isotope as first match for " + isotopicNumber);
      //      break;
      //    }
      //  }
    }
    return suggestion;
  }

  public Parameter<EventParameter> getEventParameter() {
    return eventParameter;
  }

  public Parameter<MathMod> getEventMathModification() {
    return eventMathModification;
  }

  public Parameter<Double> getDefault_D_mu() {
    return default_D_mu;
  }

  public Parameter<Double> getDefault_D_SD() {
    return default_D_SD;
  }

  public Parameter<Double> getDefault_v_mu() {
    return default_v_mu;
  }

  public Parameter<Double> getDefault_v_SD() {
    return default_v_SD;
  }

  public Parameter<Double> getDefault_y_mu() {
    return default_y_mu;
  }

  public Parameter<Double> getDefault_y_SD() {
    return default_y_SD;
  }


  public Parameter<Double> getSiaLowerLimit() {
    return siaLowerLimit;
  }

  public Parameter<Double> getSiaUpperLimit() {
    return siaUpperLimit;
  }

  public Parameter<Double> getSiaSigmaClip() {
    return siaSigmaClip;
  }
}
