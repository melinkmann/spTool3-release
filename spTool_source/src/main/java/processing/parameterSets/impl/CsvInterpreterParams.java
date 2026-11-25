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

import gui.dialog.Fillable;
import gui.util.TextFormatterOption;
import io.XmlUtil;
import io.impl.*;

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.*;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.AutoFillParam;
import processing.parameters.BooleanParameter;
import processing.parameters.CharacterParameter;
import processing.parameters.ComboEnumParameter;
import processing.parameters.DoubleParameter;
import processing.parameters.IntegerParameter;
import processing.parameters.Parameter;
import sandbox.montecarlo.Isotope;
import util.NF;

public class CsvInterpreterParams extends AbstractParamSet implements Serializable, ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;

  private static final Logger LOGGER = LogManager.getLogger(CsvInterpreterParams.class);

  public static final String XML_ELEMENT_TAG = "FileInterpreterParams";

  // Parameter

  // Read the file
  private final Parameter<CharSet> charset;
  private final Parameter<Delimiter> delimiter;
  private final Parameter<Character> customDelimiter;

  // how to deal with the context of the csv: where to start, where is what?
  private final Parameter<CsvInterpreters> interpreter;
  private final Parameter<Integer> firstLine;
  private final Parameter<Boolean> hasXData;

  // are there certain meta data to read from the file?
  private final Parameter<Source> dwellTimeSource;
  private final Parameter<Double> customDwellTime;

  private final Parameter<Source> mzSource;
  private final Parameter<Fillable<Isotope>> isotopeParameter;

  private final Parameter<Source> rawUnitSource;
  private final Parameter<ConversionUnit> rawUnit;

  // Special cases
  private final Parameter<Double> agilentGasModeSwitchTime;
  private final Parameter<Boolean> isIonicQmsData;
  private final Parameter<SignalConversionOption> signalConversionOption;

  private final Parameter<Boolean> hasLineIndex;
  private final Parameter<TimeStampFormat> timeStampFormat;

  private final Parameter<OverRangeRecognition> overRangeRecognition;
  private final Parameter<Double> customOverRangeValue;

  // for the custom case
  private Parameter<LpcDimension> lpcImportParameter;

  // for mzmine case
  private Parameter<Integer> digitsPrecision;
  private Parameter<EICNormalisation> eicNormalisation;
  private Parameter<Boolean> specifyMeanAsGlobalAverage;
  private Parameter<Double> blankMeanSingleIonSignal;
  private Parameter<Double> blankMeanSingleIonSignalChannelLow;
  private Parameter<Double> blankMeanSingleIonSignalLow;
  private Parameter<Double> blankMeanSingleIonSignalChannelHigh;
  private Parameter<Double> blankMeanSingleIonSignalHigh;
  private Parameter<Double> electronicNoiseThreshold;
  private Parameter<SecondaryThresholdBruker> siaThresholdOption;
  private Parameter<Double> siaThresholdValue;
  private Parameter<Boolean> loadTruncatedData;
  private Parameter<Boolean> estimateSiaShapeViaMLE;

  public CsvInterpreterParams() {
    this("Csv import parameters");
  }

  public CsvInterpreterParams(String label) {
    super(label, XML_ELEMENT_TAG);

    // Parameters
    this.charset = new ComboEnumParameter<>("CSV encoding",
        "Character encoding",
        CharSet.UTF8,
        CharSet.values(),
        CharSet.class,
        false,
        "charset");

    this.delimiter = new ComboEnumParameter<>("Separator",
        "Separator character",
        Delimiter.COMMA,
        Delimiter.values(),
        Delimiter.class,
        false,
        "delimiter");

    this.customDelimiter = new CharacterParameter("Custom Separator",
        "",
        '|',
        false,
        "customDelimiter");

    this.interpreter = new ComboEnumParameter<>("Preset",
        "Choose from default interpreters for standard instrument manufacturer data formats",
        CsvInterpreters.AGILENT,
        CsvInterpreters.getActive(),
        CsvInterpreters.class,
        false,
        "interpreter");

    this.dwellTimeSource = new ComboEnumParameter<>(
        "Dwell time source",
        "Specify how the integration time per isotope is determined",
        Source.FROM_FILE,
        Source.values(),
        Source.class,
        false,
        "dwellTimeSource"
    );

    this.customDwellTime = new DoubleParameter(
        "Dwell time [µs]",
        "Give a custom value for the integration time per isotope if your data format does not supply it",
        50D,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "customDwellTime"
    );

    this.mzSource = new ComboEnumParameter<>(
        "Isotope (m/z) source",
        "Specify how the m/z of the respective isotope is determined",
        Source.FROM_FILE,
        Source.values(),
        Source.class,
        false,
        "mzSource"
    );

    this.rawUnitSource = new ComboEnumParameter<>(
        "Unit source",
        "Specify how the raw data unit is determined",
        Source.FROM_FILE,
        Source.values(),
        Source.class,
        false,
        "rawUnitSource"
    );

    this.isotopeParameter = new AutoFillParam<>("Isotope",
        "Chemical element and its isotope",
        dataModelNew.mz.Element.Au.getIsotopes().get(0),
        dataModelNew.mz.Element.Au.getIsotopes().get(0),
        TextFormatterOption.ALL_PASS,
        false,
        "isotopeParameter");

    this.rawUnit = new ComboEnumParameter<>(
        "Intensity unit of the raw data",
        """
            SpTool uses count-based intensity units, i.e., 'counts per dwell time', 'cts/DT'.
            Based on the unit of the raw data, it will convert 'counts per second, cps' data to cts/DT.
            You can select if your data are given in 'cps', 'cts/DT' or if SpTool shall try to determine the unit automatically""",
        ConversionUnit.CTS,
        ConversionUnit.values(),
        ConversionUnit.class,
        false,
        "rawUnit"
    );

    this.firstLine = new IntegerParameter(
        "Header line",
        "Specify the first line (as 1,2,3,...) to read the header (e.g., MS or intensity) and then data.",
        1,
        TextFormatterOption.ASSURE_POSITIVE_INTEGER,
        false,
        "firstLine"
    );

    this.hasXData = new BooleanParameter(
        "x data",
        "Has column with x data",
        "Specify if there is a column with x data (time)",
        true,
        false,
        "hasXData"
    );

    agilentGasModeSwitchTime = new DoubleParameter(
        "Gas mode switch",
        """
            When acquiring data with several gas modes,
            or using several gas modes as a replacement for Agilent single particle plugin, 
            raw data contain a continuous time series that has gaps when gas modes are switched.
            E.g., there are time stamps 0 - 60 s, then a gap, and following time stamps 70 - 120 s.
            For processing, we want to split the sections and only keep those where a given MS has 
            data. SpTool recognizes the gap using this parameter. Make sure that it is at least
            larger than the dwell time
            """,
        5D,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "agilentGasModeSwitchTime"
    );

    this.isIonicQmsData = new BooleanParameter(
        "Quadrupole",
        "Is ionic data",
        """
            When several MS are acquired using quadrupole MS within the same sample,
            usually with DT = 100 msId, only one time stamp is exported with 'total DT'.
            The 'total DT is' number of MS ('n') n · DT where DT is the actual DT used for the MS.
            
            This assumes and requires that the same DT was chosen in the acquisition software for all MS!
            
            For analysis, we need to calculate the DT per MS, i.e., compute 'total DT' / n 
            to get accurate results.
            
            However, we do not want to correct, when e.g., Agilent data were exported
            in sp mode using different gas modes or the sp plugin. For these cases,
            we also find multiple MS in the raw data but we do not want to correct the 'total DT'
            as data was acquired in sp mode. In sp mode, the instrument does not acquire data
            for all MS but only one MS at a time. In the export file, however, Agilent writes data 
            for all MS. Those that were not recorded get zeros.
            
            The problem here is that we (and thus SpTool)
            cannot reliably tell these cases apart unless the user specifies it""",
        false,
        false,
        "isIonicQmsData"
    );

    this.signalConversionOption = new ComboEnumParameter<>(
        "Signal conversion",
        "Decide if intensity is converted from cps to counts.",
        SignalConversionOption.AUTOMATIC,
        SignalConversionOption.values(),
        SignalConversionOption.class,
        false,
        "signalConversionOption"
    );

    timeStampFormat = new ComboEnumParameter<>(
        "Time format",
        "Specify which format the time stamp has",
        TimeStampFormat.DECIMAL,
        TimeStampFormat.values(),
        TimeStampFormat.class,
        false,
        "timeStampFormat"
    );

    this.hasLineIndex = new BooleanParameter(
        "Format",
        "Has line index [Index X YYYY]",
        """
            Specify if the data have a column with indices (ascending number 1, 2, 3 ...) before the time column
            """,
        false,
        false,
        "hasLineIndex"
    );

    this.overRangeRecognition = new ComboEnumParameter<>(
        "Over-range",
        "Specify how over-range signals are identified",
        OverRangeRecognition.ANALYTIK_JENA,
        OverRangeRecognition.listAllExceptNone(),
        OverRangeRecognition.class,
        true,
        "overRangeRecognition"
    );

    customOverRangeValue = new DoubleParameter(
        "Over-range value",
        """
            Flag value in the raw data to indicate detector over-ranges in the raw data.
            If the flag is positive (e.g., Analytik Jena), all values larger than or equal to the flag are considered over-range.
            If the flag is negative (e.g., Thermo Fisher), all values smaller or equal to the flag are considered over-range.
            """,
        268435456d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        true,
        "customOverRangeValue"
    );

    this.lpcImportParameter = new ComboEnumParameter<>(
        "Quantity",
        """
            What data shall be imported as 'Net height' option in spTool?
            Diameter will always be loaded as 'Net area'.
            This option just offers an additional parameter to load""",
        LpcDimension.CIRCLE_EQUIVALENT_DIAMETER,
        LpcDimension.values(),
        LpcDimension.class,
        true,
        "lpcImportParameter"
    );


    this.digitsPrecision = new IntegerParameter(
        "MZ digits",
        """
            Define how many digits after the comme will be shown""",
        3,
        TextFormatterOption.ASSURE_POSITIVE_INTEGER,
        true,
        "digitsPrecision"
    );

    eicNormalisation = new ComboEnumParameter<>(
        "Normalize",
        """
            Normalizes intensities by either
            a) subtracting (not recommended)
            b) dividing by the estimated single ion area
            c) do nothing via option 'None'
            """,
        EICNormalisation.DIVIDE,
        EICNormalisation.getActive(),
        EICNormalisation.class,
        true,
        "eicNormalisation"
    );

    specifyMeanAsGlobalAverage = new BooleanParameter(
        "Single ion mean",
        "Global average",
        """
            If selected, one mean single ion signal will be used for all m/h channels.
            Else, you max specify an upper and lower channel and its mean.
            Between these, linear interpolation is assumed;
            Outside of the 'calibrated' range, respective upper/lower values will be used
            (i.e., no extrapolation but the same value)""",
        true,
        false,
        "specifyMeanAsGlobalAverage"
    );

    blankMeanSingleIonSignal = new DoubleParameter(
        "Single ion mean",
        """
            Normalisation: all digitiser counts are divided by this number""",
        55d,
        NF.D1C2,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "blankMeanSingleIonSignal"
    );


    blankMeanSingleIonSignalChannelLow = new DoubleParameter(
        "Low channel",
        """
            The channel (m/z) of the lower single ion signal calibration point""",
        322d,
        NF.D1C4,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "blankMeanSingleIonSignalChannelLow"
    );

    blankMeanSingleIonSignalLow = new DoubleParameter(
        "Low Single ion mean",
        """
            The single ion signal of the lower channel (m/z) calibration point""",
        70d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "blankMeanSingleIonSignalLow"
    );

    blankMeanSingleIonSignalChannelHigh = new DoubleParameter(
        "High channel",
        """
            The channel (m/z) of the upper single ion signal calibration point""",
        2422d,
        NF.D1C4,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "blankMeanSingleIonSignalChannelHigh"
    );

    blankMeanSingleIonSignalHigh = new DoubleParameter(
        "High Single ion mean",
        """
            The single ion signal of the upper channel (m/z) calibration point""",
        50d,
        NF.D1C1,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "blankMeanSingleIonSignalHigh"
    );

    this.electronicNoiseThreshold = new DoubleParameter(
        "Electronic noise level",
        """
            All intensity values below this value are considered electronic noise.
            They will be excluded from the data""",
        18d,
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "electronicNoiseThreshold"
    );

    siaThresholdOption = new ComboEnumParameter<>(
        "2nd threshold",
        """
             Below the main single ion distribution, there is smaller distribution at roughly 1/2 of the
             mean single ion signal. You may filter out these event.
            
             - No removal
             - Static value: Give a static threshold, e.g., 30 digitiser counts
             - Dynamic factor: The factor will be multiplied with the 'Single ion mean' to obtain a threshold
            
            All data points below the threshold will be ignored for the SIA distribution's
            shape factor ('SIA sigma') computation.
            In addition, you may set 'Load truncated data' to true to load the truncated data
            (this is the recommended way). Uncheck if you want to load and inspect the untruncated data set""",
        SecondaryThresholdBruker.DYNAMIC_FROM_MEAN,
        SecondaryThresholdBruker.values(),
        SecondaryThresholdBruker.class,
        false,
        "siaThresholdOption"
    );

    this.siaThresholdValue = new DoubleParameter(
        "2nd threshold",
        """
            In case of
            Static value: recommended threshold is circa 30 digitiser counts
            Dynamic factor: recommended factor is circa 0.5, i.e., half of the mean ion signal""",
        0.50, //0.56
        NF.D1C2,
        TextFormatterOption.ASSURE_POSITIVE_DOUBLE,
        false,
        "siaThresholdValue"
    );

    this.loadTruncatedData = new BooleanParameter(
        "Import",
        "Load truncated data",
        """
            All data points below the SIA threshold, which is set by the '2nd threshold factor'
            and the single ion mean (see '2nd threshold factor'), are set to zero""",
        true,
        false,
        "loadTruncatedData"
    );

    this.estimateSiaShapeViaMLE = new BooleanParameter(
        "SIA shape",
        "Use MLE estimator (slow)",
        """
            If selected, the SIA shape factor will be estimated using maximum likelihood.
            This approach appears to provide more accurate estimates but is also substantially more slow""",
        true,
        false,
        "estimateSiaShapeViaMLE"
    );

    organize();
  }

  // Copy
  public CsvInterpreterParams(CsvInterpreterParams csvInterpreterParams) {
    super(csvInterpreterParams.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(csvInterpreterParams.getCommentParameter());
    this.charset = csvInterpreterParams.charset.copyWithoutChildren();
    this.delimiter = csvInterpreterParams.delimiter.copyWithoutChildren();
    this.customDelimiter = csvInterpreterParams.customDelimiter.copyWithoutChildren();
    this.interpreter = csvInterpreterParams.interpreter.copyWithoutChildren();
    this.dwellTimeSource = csvInterpreterParams.dwellTimeSource.copyWithoutChildren();
    this.customDwellTime = csvInterpreterParams.customDwellTime.copyWithoutChildren();
    this.mzSource = csvInterpreterParams.mzSource.copyWithoutChildren();
    this.isotopeParameter = csvInterpreterParams.isotopeParameter.copyWithoutChildren();
    this.rawUnitSource = csvInterpreterParams.rawUnitSource.copyWithoutChildren();
    this.rawUnit = csvInterpreterParams.rawUnit.copyWithoutChildren();
    this.firstLine = csvInterpreterParams.firstLine.copyWithoutChildren();
    this.hasXData = csvInterpreterParams.hasXData.copyWithoutChildren();
    this.agilentGasModeSwitchTime = csvInterpreterParams.agilentGasModeSwitchTime
        .copyWithoutChildren();
    this.isIonicQmsData = csvInterpreterParams.isIonicQmsData.copyWithoutChildren();
    this.signalConversionOption = csvInterpreterParams.signalConversionOption.copyWithoutChildren();
    this.timeStampFormat = csvInterpreterParams.timeStampFormat.copyWithoutChildren();
    this.hasLineIndex = csvInterpreterParams.hasLineIndex.copyWithoutChildren();
    this.overRangeRecognition = csvInterpreterParams.overRangeRecognition.copyWithoutChildren();
    this.customOverRangeValue = csvInterpreterParams.customOverRangeValue.copyWithoutChildren();
    this.lpcImportParameter = csvInterpreterParams.lpcImportParameter.copyWithoutChildren();
    this.digitsPrecision = csvInterpreterParams.digitsPrecision.copyWithoutChildren();
    this.eicNormalisation = csvInterpreterParams.eicNormalisation.copyWithoutChildren();
    this.specifyMeanAsGlobalAverage = csvInterpreterParams.specifyMeanAsGlobalAverage.copyWithoutChildren();
    this.blankMeanSingleIonSignal = csvInterpreterParams.blankMeanSingleIonSignal.copyWithoutChildren();
    this.blankMeanSingleIonSignalChannelLow =
        csvInterpreterParams.blankMeanSingleIonSignalChannelLow.copyWithoutChildren();
    this.blankMeanSingleIonSignalLow = csvInterpreterParams.blankMeanSingleIonSignalLow.copyWithoutChildren();
    this.blankMeanSingleIonSignalChannelHigh =
        csvInterpreterParams.blankMeanSingleIonSignalChannelHigh.copyWithoutChildren();
    this.blankMeanSingleIonSignalHigh =
        csvInterpreterParams.blankMeanSingleIonSignalHigh.copyWithoutChildren();

    this.electronicNoiseThreshold = csvInterpreterParams.electronicNoiseThreshold.copyWithoutChildren();
    this.siaThresholdOption = csvInterpreterParams.siaThresholdOption.copyWithoutChildren();
    this.siaThresholdValue = csvInterpreterParams.siaThresholdValue.copyWithoutChildren();
    this.loadTruncatedData = csvInterpreterParams.loadTruncatedData.copyWithoutChildren();
    this.estimateSiaShapeViaMLE = csvInterpreterParams.estimateSiaShapeViaMLE.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new CsvInterpreterParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new CsvInterpreterParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new CsvInterpreterParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Add all PARENT (not the depending) parameters!
    super.setParentParameters(
        interpreter,
        charset,
        delimiter
    );

    interpreter.addConditionalChild(CsvInterpreters.CUSTOM_PEAKS,
        firstLine,
        mzSource);

    interpreter.addConditionalChild(CsvInterpreters.CUSTOM_TRA,
        firstLine,
        hasXData,
        dwellTimeSource,
        rawUnitSource,
        mzSource);

    interpreter.addConditionalChild(CsvInterpreters.AGILENT,
        signalConversionOption,
        isIonicQmsData,
        agilentGasModeSwitchTime);

    interpreter.addConditionalChild(CsvInterpreters.ANALYTIK_JENA,
        overRangeRecognition);

    interpreter.addConditionalChild(CsvInterpreters.CUSTOM_PEAKS,
        isotopeParameter);

    interpreter.addConditionalChild(CsvInterpreters.LPC,
        isotopeParameter,
        lpcImportParameter,
        firstLine);

    overRangeRecognition.addConditionalChild(OverRangeRecognition.CUSTOM, customOverRangeValue);

    interpreter.addConditionalChild(CsvInterpreters.THERMO_XY,
        dwellTimeSource,
        signalConversionOption,
        isIonicQmsData,
        timeStampFormat,
        mzSource);

    interpreter.addConditionalChild(CsvInterpreters.MZMINE_TRA,
        digitsPrecision,
        eicNormalisation
    );

    eicNormalisation.addConditionalChild(EICNormalisation.DIVIDE,
        electronicNoiseThreshold,
        siaThresholdOption,
        specifyMeanAsGlobalAverage,
        estimateSiaShapeViaMLE);

    specifyMeanAsGlobalAverage.addConditionalChild(true, blankMeanSingleIonSignal);
    specifyMeanAsGlobalAverage.addConditionalChild(false,
        blankMeanSingleIonSignalChannelLow,
        blankMeanSingleIonSignalLow,
        blankMeanSingleIonSignalChannelHigh,
        blankMeanSingleIonSignalHigh);

    siaThresholdOption.addConditionalChild(SecondaryThresholdBruker.STATIC,
        siaThresholdValue,
        loadTruncatedData);
    siaThresholdOption.addConditionalChild(SecondaryThresholdBruker.DYNAMIC_FROM_MEAN,
        siaThresholdValue,
        loadTruncatedData);

    interpreter.addConditionalChild(CsvInterpreters.THERMO_TOPDOWN,
        signalConversionOption,
        isIonicQmsData,
        timeStampFormat);

    dwellTimeSource.addConditionalChild(Source.CUSTOM, customDwellTime);
    mzSource.addConditionalChild(Source.CUSTOM, isotopeParameter);
    rawUnitSource.addConditionalChild(Source.CUSTOM, rawUnit);

    // Attach Children.
    delimiter.addConditionalChild(Delimiter.CUSTOM, customDelimiter);

  }


  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.CSV_READER;
  }

  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    super.setAssociatedFileOnDrive(file);

    for (int temp = 0; temp < nodeList.getLength(); temp++) {
      Node node = nodeList.item(temp);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        Element element = (Element) node;

        // ID identifies the parameter (i.e., which variable)
        Parameter<?> par = switch (element.getAttribute(XmlUtil.PAR_XML_ID_ATTRIBUTE)) {
          case LABEL_PAR_XML_ID -> super.label;
          case COMMENT_PAR_XML_ID -> super.comment;
          case DATE_PAR_XML_ID -> super.dateCreated;
          case UUID_PAR_XML_ID -> super.uuidString;

          case "charset" -> charset;
          case "delimiter" -> delimiter;
          case "customDelimiter" -> customDelimiter;
          case "interpreter" -> interpreter;

          case "dwellTimeSource" -> dwellTimeSource;
          case "customDwellTime" -> customDwellTime;

          case "mzSource" -> mzSource;
          case "isotopeParameter" -> isotopeParameter;

          case "rawUnitSource" -> rawUnitSource;
          case "rawUnit" -> rawUnit;

          case "agilentGasModeSwitchTime" -> agilentGasModeSwitchTime;
          case "isIonicQmsData" -> isIonicQmsData;

          case "signalConversionOption" -> signalConversionOption;

          case "timeStampFormat" -> timeStampFormat;
          case "hasLineIndex" -> hasLineIndex;

          case "overRangeRecognition" -> overRangeRecognition;
          case "customOverRangeValue" -> customOverRangeValue;

          case "hasXData" -> hasXData;

          case "lpcImportParameter" -> lpcImportParameter;

          case "digitsPrecision" -> digitsPrecision;
          case "eicNormalisation" -> eicNormalisation;
          case "specifyMeanAsGlobalAverage" -> specifyMeanAsGlobalAverage;
          case "blankMeanSingleIonSignal" -> blankMeanSingleIonSignal;

          case "blankMeanSingleIonSignalChannelLow" -> blankMeanSingleIonSignalChannelLow;
          case "blankMeanSingleIonSignalLow" -> blankMeanSingleIonSignalLow;
          case "blankMeanSingleIonSignalChannelHigh" -> blankMeanSingleIonSignalChannelHigh;
          case "blankMeanSingleIonSignalHigh" -> blankMeanSingleIonSignalHigh;

          case "electronicNoiseThreshold" -> electronicNoiseThreshold;
          case "siaThresholdOption" -> siaThresholdOption;
          case "siaThresholdValue" -> siaThresholdValue;
          case "loadTruncatedData" -> loadTruncatedData;
          case "estimateSiaShapeViaMLE" -> estimateSiaShapeViaMLE;

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

  /// ///////////////////////////////////////////////////////////////

  public Character getDelimiter() {
    Character character = ',';
    if (delimiter.getValue().equals(Delimiter.CUSTOM)) {
      character = customDelimiter.getValue();
    } else {
      character = delimiter.getValue().getDelimiter();
    }
    return character;
  }

  public CharSet getCharSet() {
    return charset.getValue();
  }

  public CsvInterpreters getInterpreterOption() {
    return interpreter.getValue();
  }

  public CsvInterpreter getInterpreter() {
    return switch (interpreter.getValue()) {
      case CUSTOM_PEAKS -> new CsvInterpreterCustomPeaks(this);
      case LPC -> new CsvInterpreterLPC(this);
      case CUSTOM_TRA -> new CsvInterpreterCustomTimeResolved(this);
      case MZMINE_TRA -> new CsvInterpreterMZMine(this);
      case AGILENT -> new CsvInterpreterAgilent(this);
      case THERMO_XY -> new CsvInterpreterThermoXY(this);
      case THERMO_TOPDOWN -> new CsvInterpreterThermoTopDown(this);
      case ANALYTIK_JENA -> new CsvInterpreterAnalytikJena(this);
    };
  }

  public Parameter<Source> getMzSource() {
    return mzSource;
  }


  public Parameter<Fillable<Isotope>> getIsotopeParameter() {
    return isotopeParameter;
  }

  public Parameter<Integer> getFirstLine() {
    return firstLine;
  }

  public Parameter<Source> getDwellTimeSource() {
    return dwellTimeSource;
  }

  public Parameter<Double> getCustomDwellTime() {
    return customDwellTime;
  }

  public Parameter<Source> getRawUnitSource() {
    return rawUnitSource;
  }

  public Parameter<ConversionUnit> getRawUnit() {
    return rawUnit;
  }

  public Parameter<Boolean> getIsIonicQmsData() {
    return isIonicQmsData;
  }


  public Parameter<SignalConversionOption> getSignalConversionOption() {
    return signalConversionOption;
  }

  public Parameter<Boolean> getHasLineIndex() {
    return hasLineIndex;
  }

  public Parameter<TimeStampFormat> getTimeStampFormat() {
    return timeStampFormat;
  }

  public Parameter<Double> getAgilentGasModeSwitchTime() {
    return agilentGasModeSwitchTime;
  }

  public Parameter<Boolean> getHasXData() {
    return hasXData;
  }

  public Parameter<OverRangeRecognition> getOverRangeRecognition() {
    return overRangeRecognition;
  }

  public Parameter<Double> getCustomOverRangeValue() {
    return customOverRangeValue;
  }

  public Parameter<LpcDimension> getLpcImportParameter() {
    return lpcImportParameter;
  }

  public Parameter<Integer> getDigitsPrecision() {
    return digitsPrecision;
  }

  public Parameter<EICNormalisation> getEicNormalisation() {
    return eicNormalisation;
  }

  public double getBlankMeanSingleIonSignal(double mz) {
    if (!specifyMeanAsGlobalAverage.getValue()) {
      double channelLow = blankMeanSingleIonSignalChannelLow.getValue();
      double signalLow = blankMeanSingleIonSignalLow.getValue();

      double channelHigh = blankMeanSingleIonSignalChannelHigh.getValue();
      double signalHigh = blankMeanSingleIonSignalHigh.getValue();

      // check
      if (channelLow < channelHigh && signalHigh > 0 && signalLow > 0) {

        if (mz <= channelLow) {
          return signalLow;
        }

        if (mz >= channelHigh) {
          return signalHigh;
        }

        // Linear interpolation
        double fraction = (mz - channelLow) / (channelHigh - channelLow);
        return signalLow + fraction * (signalHigh - signalLow);
      } else {
        LOGGER.error("Lower channel must be smaller than upper channel (m/z)! " +
            "Mean ion signals must be strictly positive!");
        if (signalHigh > 0 && signalLow > 0) {
          return (signalHigh + signalLow) / 2d;
        } else if (signalHigh > 0) {
          LOGGER.error("Single ion means must be positive!");
          return signalHigh;
        } else if (signalLow > 0) {
          LOGGER.error("Single ion means must be positive!");
          return signalLow;
        } else if (blankMeanSingleIonSignal.getValue() > 0) {
          LOGGER.error("Single ion means must be positive!");
          return blankMeanSingleIonSignal.getValue();
        } else {
          LOGGER.error("Single ion means must be positive!");
          LOGGER.error("Global mean must be positive! Returning absolute value.");
          return Math.abs(blankMeanSingleIonSignal.getValue());
        }
      }
    } else {
      if (blankMeanSingleIonSignal.getValue() > 0) {
        return blankMeanSingleIonSignal.getValue();
      } else {
        LOGGER.error("Global mean must be positive! Returning absolute value.");
        return Math.abs(blankMeanSingleIonSignal.getValue());
      }
    }
  }

  public Parameter<Double> getElectronicNoiseThreshold() {
    return electronicNoiseThreshold;
  }

  public Parameter<Boolean> getEstimateSiaShapeViaMLE() {
    return estimateSiaShapeViaMLE;
  }

  public double getSiaThresholdValue() {
    if (siaThresholdOption.getValue().equals(SecondaryThresholdBruker.NONE)) {
      return 0;
    } else {
      return siaThresholdValue.getValue();
    }
  }

  public Parameter<SecondaryThresholdBruker> getSiaThresholdOption() {
    return siaThresholdOption;
  }

  public boolean getLoadTruncatedData() {
    if (siaThresholdOption.getValue().equals(SecondaryThresholdBruker.NONE)) {
      return false;
    } else {
      return loadTruncatedData.getValue();
    }
  }

  public double getORValue() {
    double or = switch (overRangeRecognition.getValue()) {
      case NONE -> 0.0;
      case CUSTOM -> customOverRangeValue.getValue();
      case THERMO_FISHER -> -33;
      case ANALYTIK_JENA -> 268435456;
    };
    return or;
  }

  @Serial
  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {

    in.defaultReadObject();

    // default supplier
    final CsvInterpreterParams defaults = new CsvInterpreterParams();

    // Fix missing fields from old serialized versions: we have to use
    if (lpcImportParameter == null) {
      this.lpcImportParameter = defaults.lpcImportParameter;
    }

    if (digitsPrecision == null) {
      this.digitsPrecision = defaults.digitsPrecision;
    }

    if (eicNormalisation == null) {
      this.eicNormalisation = defaults.eicNormalisation;
    }

    if (specifyMeanAsGlobalAverage == null) {
      this.specifyMeanAsGlobalAverage = defaults.specifyMeanAsGlobalAverage;
    }

    if (blankMeanSingleIonSignal == null) {
      this.blankMeanSingleIonSignal = defaults.blankMeanSingleIonSignal;
    }

    if (blankMeanSingleIonSignalChannelLow == null) {
      this.blankMeanSingleIonSignalChannelLow = defaults.blankMeanSingleIonSignalChannelLow;
    }

    if (blankMeanSingleIonSignalLow == null) {
      this.blankMeanSingleIonSignalLow = defaults.blankMeanSingleIonSignalLow;
    }

    if (blankMeanSingleIonSignalChannelHigh == null) {
      this.blankMeanSingleIonSignalChannelHigh = defaults.blankMeanSingleIonSignalChannelHigh;
    }

    if (blankMeanSingleIonSignalHigh == null) {
      this.blankMeanSingleIonSignalHigh = defaults.blankMeanSingleIonSignalHigh;
    }

    if (electronicNoiseThreshold == null) {
      this.electronicNoiseThreshold = defaults.electronicNoiseThreshold;
    }

    if (siaThresholdOption == null) {
      this.siaThresholdOption = defaults.siaThresholdOption;
    }

    if (siaThresholdValue == null) {
      this.siaThresholdValue = defaults.siaThresholdValue;
    }

    if (loadTruncatedData == null) {
      this.loadTruncatedData = defaults.loadTruncatedData;
    }

    if (estimateSiaShapeViaMLE == null) {
      this.estimateSiaShapeViaMLE = defaults.estimateSiaShapeViaMLE;
    }


  }


}
