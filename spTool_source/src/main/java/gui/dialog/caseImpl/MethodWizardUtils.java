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

package gui.dialog.caseImpl;

import core.SpTool3Main;
import gui.MethodView;
import gui.util.TextFormatterOption;
import gui.util.TextFormatterSupplier;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import processing.options.*;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.impl.*;
import util.SnF;

import javax.swing.text.html.Option;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class MethodWizardUtils {

  public static void show(MethodView methodView) {

    // TODO: ideally, we should have a list of default sub methods and pass the values
    // from those; that way, we'd only have to read from template and not have
    // important logic buried in these if statements here.

    Method method = new ListMethod("Default method");

    final ComboBox<String> instrumentType = new ComboBox<>();
    instrumentType.setItems(FXCollections.observableArrayList(
        "Quadrupole (QMS)", "Time-of-flight (TOF)"
    ));
    instrumentType.getSelectionModel().select("Quadrupole (QMS)");

    final CheckBox csvImport = new CheckBox("Add csv import");
    csvImport.setSelected(SpTool3Main.getANALYZER());
    final CheckBox addSimulation = new CheckBox("Add data generator");
    final TextField numOfPopFld = new TextField();
    numOfPopFld.setTextFormatter(TextFormatterSupplier.get(TextFormatterOption.ASSURE_NONZERO_POSITIVE_INTEGER, 1));
    addSimulation.setSelected(!SpTool3Main.getANALYZER());
    final CheckBox addGating = new CheckBox("Add gating options");
    final CheckBox addValleyFilter = new CheckBox("Add valley filter");

    VBox box = new VBox(8);
    box.setAlignment(Pos.CENTER_LEFT);

    // bind visibility
    HBox popNumBox = new HBox(2, new Label("Number of populations"), numOfPopFld);
    popNumBox.visibleProperty().bind(addSimulation.selectedProperty());

    if (SpTool3Main.getANALYZER()) {
      box.getChildren().addAll(
          new Label("Quick start method"),
          instrumentType,
          csvImport,
          addSimulation,
          popNumBox,
          addGating,
          addValleyFilter);
    } else {
      box.getChildren().addAll(
          new Label("Quick start method"),
          instrumentType,
          addSimulation,
          new HBox(2, new Label("Number of populations"), numOfPopFld)
      );
    }

    // orientation
    for (Node child : box.getChildren()) {
      if (child instanceof HBox) {
        ((HBox) child).setAlignment(Pos.CENTER_LEFT);
      }
    }

    List<Node> nodeList = new ArrayList<>();
    nodeList.add(box);
    Dialog<Boolean> dia = new ListPaneDialog(nodeList);
    Optional<Boolean> res = dia.showAndWait();

    if (res.isPresent()) {

      if (csvImport.selectedProperty().get()) {
        CsvInterpreterParams csvParams = new CsvInterpreterParams();
        method.getSets().add(csvParams);
      }

      if (addSimulation.selectedProperty().get()) {
        MCSimGeneralParams generalParams = new MCSimGeneralParams();

        if (Objects.equals(instrumentType.getSelectionModel().getSelectedItem(), "Time-of-flight (TOF)")) {
          generalParams.detectorDistribution.setValue(PDF.COMPOUND_POISSON);
        } else {
          generalParams.detectorDistribution.setValue(PDF.POISSON);
        }

        method.getSets().add(generalParams);

        String numPop = numOfPopFld.getText();
        int numPopInt = 1;
        if (SnF.isValidIntSilently(numPop)) {
          numPopInt = SnF.strToInt(numPop);
        }
        for (int i = 0; i < numPopInt; i++) {
          MCSimParticleParams pop = new MCSimParticleParams();
          pop.getLabelParameter().setValue("Population " + (i + 1));
          method.getSets().add(pop);
        }

      }


      // only add BLN and search if analyzer
      if (SpTool3Main.getANALYZER()) {
        // BASELINE
        BaselineParams baselineParams = new BaselineParams();
        method.getSets().add(baselineParams);

        if (Objects.equals(instrumentType.getSelectionModel().getSelectedItem(), "Time-of-flight (TOF)")) {
          baselineParams.getPoissonChoice().setValue(DistributionModel.POISSON_COMPOUND);
        } else {
          baselineParams.getPoissonChoice().setValue(DistributionModel.POISSON_CURRIE);
        }


        // SEARCH
        NormalSearchParams searchParams = new NormalSearchParams();
        method.getSets().add(searchParams);

        searchParams.getHeightCriterium().getThresholdOption().setValue(SearchThresholdOption.ALPHA);
        searchParams.getHeightCriterium().getAlpha().setValue(1E-6);

        // GATING
        if (addGating.selectedProperty().get()) {
          GatingParams gatingPar = new GatingParams();
          gatingPar.getGatingOption().setValue(GatingOption.MORE_POINTS_THAN);
          gatingPar.getAbsoluteCutoff().setValue(1d);
          method.getSets().add(gatingPar);
        }

        // FILTERING
        FilterParams filterParams = new FilterParams();
        filterParams.getFilterOption().setValue(FilterOptions.OVERLAP);
        if (addValleyFilter.selectedProperty().get()) {
          method.getSets().add(filterParams);
        }
      } // is analyzer
    }
    methodView.setMethod(method);
  }


}
