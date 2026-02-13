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
import gui.dialog.notification.NotificationFactory;
import gui.util.TextFormatterOption;
import io.GlobalIO;
import io.XmlUtil;

import java.io.File;
import java.io.Serial;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import math.units.Unit;
import math.units.enums.ExportUnits;
import math.units.enums.IntensityUnit;
import math.units.enums.MassUnit;
import math.units.enums.SizeUnit;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import processing.options.EventParameter;
import processing.options.MathMod;
import processing.parameterSets.AbstractParamSet;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ParamSet;
import processing.parameters.*;
import util.NF;
import util.SupplierSerializable;

public class ExporterParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1000_000_000;


  public enum ExportTarget {
    CSV {
      @Override
      public String toString() {
        return "csv file";
      }
    },

    CLIPBOARD {
      @Override
      public String toString() {
        return "Clipboard (copy/paste)";
      }
    }
  }

  public static final File EXPORT_FILE = GlobalIO.makeExporterFile().toFile();

  public static final String XML_ELEMENT_TAG = "exporterConfiguration";

  private final Parameter<String> currentExportPath;
  private final Parameter<ExportTarget> exportFormat;

  public final Parameter<String> exportRawDataBtnPar;
  private final Parameter<Boolean> includePopulationMarkers;
  private final Parameter<Boolean> includeEventMarkers;
  private final Parameter<Boolean> spCalCompatible;
  private final Parameter<Boolean> selectedIsotopesForRaw;
  public final Parameter<Double> spCalHeight;

  public final Parameter<String> exportCustomEventDataBtnPar;
  private final Parameter<Boolean> exportBackgroundData;
  private final Parameter<Boolean> applyJitterSampling;
  public final Parameter<Integer> jitterDataPoints;
  private final Parameter<Boolean> exportParticleData;
  private final Parameter<EventParameter> particleEventParameter;
  private final Parameter<ExportUnits> particleUnitParameter;

  public final Parameter<String> exportEMDBtnPar;
  private final Parameter<EventParameter> emdEventParameter;
  private final Parameter<MathMod> emdMathParameter;

  public ExporterParams() {
    super("SpTool export parameters", XML_ELEMENT_TAG);

    this.currentExportPath = new PathParameter(
        "Export path",
        "Current path for export",
        "",
        false,
        "currentExportPath");

    this.exportFormat = new ComboEnumParameter<>(
        "Format",
        "Decide how the data is written, either as a csv file or copied to the clipboard",
        ExportTarget.CSV,
        ExportTarget.values(),
        ExportTarget.class,
        false,
        "exportFormat");

    this.exportRawDataBtnPar = new ButtonParameter("Export",
        "Run export of raw data",
        new SupplierSerializable<Button>() {
          @Override
          public Button get() {
            return new Button("Export raw data");
          }
        },
        false,
        "exportRawDataBtnPar"
    );

    this.includePopulationMarkers = new BooleanParameter(
        "Population markers",
        "Export",
        """
            Include population markers in the RAW DATA export file.
            
            Note that the format changes when SpCal-compatible export is chosen:
            In that case, for each population, you will get an additional series of y data
            that contains zeros and a single data point at the time stamp
            where an event is known to occur from data generator. 
            This export will write a specific intensity value at each of these positions, e.g., y = 1000.
            The height of the marker can be chosen below in the SpCal-section of the export. 
            This format will force SpCal to include a marker to highlight true peak positions
            """,
        false,
        false,
        "includePopulationMarkers");

    this.includeEventMarkers = new BooleanParameter(
        "Event markers",
        "Export",
        """
            Include markers of the theoretical peak position in the RAW DATA export file.
            
            Note that this is only possible if the SpCal-compatible check box is not selected""",
        false,
        false,
        "includeEventMarkers");

    this.spCalCompatible = new BooleanParameter(
        "SpCal csv-format",
        "Compatible",
        "Remove labels and data to produce a csv file that can be read via SpCal",
        false,
        false,
        "spCalCompatible");

    this.selectedIsotopesForRaw = new BooleanParameter(
        "Isotopes",
        "Export selected isotopes only",
        "Only exports the selected isotopes",
        false,
        false,
        "selectedIsotopesForRaw");

    this.spCalHeight = new DoubleParameter(
        "Marker height [cts]",
        "For SpCal, populations are marked with a single spike at the time point."
            + "\nHere you can specify the height of that marker",
        1000d,
        NF.D1C1,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_DOUBLE,
        false,
        "spCalHeight"
    );

    this.exportCustomEventDataBtnPar = new ButtonParameter("Export",
        "Run export of raw data",
        new SupplierSerializable<Button>() {
          @Override
          public Button get() {
            return new Button("Export event data");
          }
        },
        false,
        "exportCustomEventDataBtnPar"
    );

    this.exportBackgroundData = new BooleanParameter(
        "Background",
        "Export",
        """
            Export the background data, i.e., all data points that are not considered an event
            """,
        false,
        false,
        "exportBackgroundData");

    this.applyJitterSampling = new BooleanParameter(
        "Resample",
        "Reduce number of data points ",
        """
            Take a random sample from all background data points
            in order to reduce the amount of data points and file sizes.
            Note that this procedure may produce a biased sample depending on
            how many data points are exported and how much the background actually varies
            """,
        false,
        false,
        "applyJitterSampling");

    this.jitterDataPoints = new IntegerParameter(
        "Number of points",
        """
            Number of data points that are resampled from the background
            """,
        (int) 1E5,
        TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER,
        false,
        "jitterDataPoints");

    this.exportParticleData = new BooleanParameter(
        "Events",
        "Export",
        """
            Export particle/cell event data
            """,
        true,
        false,
        "exportParticleData");

    this.particleEventParameter = new ComboEnumParameter<>(
        "Event data",
        "Choose which type of data shall be exported",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "particleEventParameter"
    );

    this.particleUnitParameter = new ComboEnumParameter<>(
        "Unit",
        "Choose which unit shall be exported",
        ExportUnits.CTS,
        ExportUnits.values(),
        ExportUnits.class,
        false,
        "particleUnitParameter"
    );


    this.exportEMDBtnPar = new ButtonParameter("Export",
        "Run export of raw data",
        new SupplierSerializable<Button>() {
          @Override
          public Button get() {
            return new Button("Export raw data");
          }
        },
        false,
        "exportEMDBtnPar"
    );

    emdMathParameter = new ComboEnumParameter<>(
        "Math",
        "Transform the data before plotting",
        MathMod.NONE,
        MathMod.values(),
        MathMod.class,
        false,
        "emdMathParameter"
    );

    emdEventParameter = new ComboEnumParameter<>(
        "Data",
        "Choose which data shall be compared",
        EventParameter.NET_AREA,
        EventParameter.histo(),
        EventParameter.class,
        false,
        "emdEventParameter"
    );

    organize();
  }

  // Copy
  public ExporterParams(ExporterParams params) {
    super(params.getLabelParameter().getValue(), XML_ELEMENT_TAG);
    super.setComment(params.getCommentParameter());

    this.currentExportPath = params.currentExportPath.copyWithoutChildren();
    this.exportFormat = params.exportFormat.copyWithoutChildren();

    this.exportRawDataBtnPar = params.exportRawDataBtnPar.copyWithoutChildren();

    this.includePopulationMarkers = params.includePopulationMarkers.copyWithoutChildren();
    this.includeEventMarkers = params.includeEventMarkers.copyWithoutChildren();

    this.spCalCompatible = params.spCalCompatible.copyWithoutChildren();
    this.spCalHeight = params.spCalHeight.copyWithoutChildren();

    this.selectedIsotopesForRaw = params.selectedIsotopesForRaw.copyWithoutChildren();

    this.exportCustomEventDataBtnPar = params.exportCustomEventDataBtnPar.copyWithoutChildren();
    this.exportBackgroundData = params.exportBackgroundData.copyWithoutChildren();
    this.applyJitterSampling = params.applyJitterSampling.copyWithoutChildren();
    this.jitterDataPoints = params.jitterDataPoints.copyWithoutChildren();
    this.exportParticleData = params.exportParticleData.copyWithoutChildren();
    this.particleEventParameter = params.particleEventParameter.copyWithoutChildren();

    this.exportEMDBtnPar = params.exportEMDBtnPar.copyWithoutChildren();
    this.emdEventParameter = params.emdEventParameter.copyWithoutChildren();
    this.emdMathParameter = params.emdMathParameter.copyWithoutChildren();

    this.particleUnitParameter = params.particleUnitParameter.copyWithoutChildren();
    organize();
  }

  @Override
  public ParamSet getNewInstance() {
    return new ExporterParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new ExporterParams(this);
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new ExporterParams(this);
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    params.setAssociatedFileOnDrive(getAssociatedFileOndDrive());
    return params;
  }

  private void organize() {
    // Register Parents
    super.setParentParameters(
        exportFormat,
        exportRawDataBtnPar,
        exportCustomEventDataBtnPar,
        exportEMDBtnPar
    );

    // Attach Children.
    exportFormat.addConditionalChild(ExportTarget.CSV, currentExportPath);

    exportRawDataBtnPar.addUnconditionalChild(includeEventMarkers,
        includePopulationMarkers,
        spCalCompatible,
        selectedIsotopesForRaw);

    spCalCompatible.addConditionalChild(true,
        spCalHeight
    );

    // Set decoration
    includePopulationMarkers.setDecoration(new ImageDecoration<>("/img/populationMarker.png"));
    includeEventMarkers.setDecoration(new ImageDecoration<>("/img/eventMarker.png"));
    spCalCompatible.setDecoration(new ImageDecoration<>("/img/spcal.png"));

    exportCustomEventDataBtnPar.addUnconditionalChild(exportBackgroundData,
        exportParticleData);

    exportBackgroundData.addConditionalChild(true, applyJitterSampling);
    applyJitterSampling.addConditionalChild(true, jitterDataPoints);
    exportParticleData.addUnconditionalChild(particleEventParameter,particleUnitParameter);

    exportEMDBtnPar.addUnconditionalChild(emdEventParameter, emdMathParameter);

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

          case "exportFormat" -> exportFormat;
          case "currentExportPath" -> currentExportPath;

          case "includePopulationMarkers" -> includePopulationMarkers;
          case "includeEventMarkers" -> includeEventMarkers;
          case "spCalCompatible" -> spCalCompatible;
          case "spCalHeight" -> spCalHeight;
          case "selectedIsotopesForRaw" -> selectedIsotopesForRaw;

          case "exportBackgroundData" -> exportBackgroundData;
          case "applyJitterSampling" -> applyJitterSampling;
          case "jitterDataPoints" -> jitterDataPoints;
          case "exportParticleData" -> exportParticleData;
          case "particleEventParameter" -> particleEventParameter;
          case "particleUnitParameter" -> particleUnitParameter;

          case "emdEventParameter" -> emdEventParameter;
          case "emdMathParameter " -> emdMathParameter;
          default -> null;
        };

        if (par != null) {
          par.readFromXmlElement(element);
        }
      }
    }
  }

  @Override
  public void executeOverridingSave() {
    XmlUtil.writeToXml(this, ExporterParams.EXPORT_FILE.toPath());
  }

  // Special case: this file is meant to be stored in only one place
  @Override
  public void executeSaveAs(Path file) {
    XmlUtil.writeToXml(this, file);
    XmlUtil.writeToXml(this, ExporterParams.EXPORT_FILE.toPath());
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

  public void setRawExportButton(SupplierSerializable<Button> button) {
    ((ButtonParameter) exportRawDataBtnPar).setButtonSupplier(button);
  }

  public void setEMDExportButton(SupplierSerializable<Button> button) {
    ((ButtonParameter) exportEMDBtnPar).setButtonSupplier(button);
  }

  public void setCustomEventExportButton(SupplierSerializable<Button> button) {
    ((ButtonParameter) exportCustomEventDataBtnPar).setButtonSupplier(button);
  }

  public Parameter<ExportTarget> getExportFormat() {
    return exportFormat;
  }

  public Parameter<String> getCurrentExportPath() {
    return currentExportPath;
  }

  public Parameter<Boolean> getIncludeEventMarkers() {
    return includeEventMarkers;
  }

  public Parameter<Boolean> getIncludePopulationMarkers() {
    return includePopulationMarkers;
  }

  public Parameter<Boolean> getSpCalCompatible() {
    return spCalCompatible;
  }

  public Double getSpCalHeightMarker() {
    return spCalHeight.getValue();
  }

  public Boolean isSelectedIsotopesForRaw() {
    return selectedIsotopesForRaw.getValue();
  }

  public Parameter<String> getExportCustomEventDataBtnPar() {
    return exportCustomEventDataBtnPar;
  }

  public Parameter<Boolean> getExportBackgroundData() {
    return exportBackgroundData;
  }

  public Parameter<Boolean> getApplyJitterSampling() {
    return applyJitterSampling;
  }

  public Parameter<Integer> getJitterDataPoints() {
    return jitterDataPoints;
  }

  public Parameter<Boolean> getExportParticleData() {
    return exportParticleData;
  }

  public Parameter<EventParameter> getParticleEventParameter() {
    return particleEventParameter;
  }

  public Parameter<ExportUnits> getParticleUnitParameter() {
    return particleUnitParameter;
  }

  public Parameter<EventParameter> getEmdEventParameter() {
    return emdEventParameter;
  }

  public Parameter<MathMod> getEmdMathParameter() {
    return emdMathParameter;
  }



  ////////////////////////////////////////////////////////////

}
