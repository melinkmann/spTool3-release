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

import analysis.PopulationID;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.Trace;
import gui.dialog.FxStageButton;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import math.stat.sigTest.SignificanceTests;
import processing.options.EventType;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.impl.SignificanceTestParams;
import processing.parameterSets.uiParams.MonteCarloHistoParameters;
import sandbox.montecarlo.Isotope;

public class SignificanceTester extends ExecuteSubmethod {

  protected static final double USER_WIDTH = 600;

  private final TextArea textArea = new TextArea("p = ... ");

  public SignificanceTester() {
    super(new SignificanceTestParams(),
        null,
        AvailableParameterSets.getOptionAsList(AvailableParameterSets.SIGNIFICANCE_TEST),
        "Significance test",
        FxStageButton.CLOSE,
        USER_WIDTH,
        null);

    // Try to initialize with values from the histogram UI
    MonteCarloHistoParameters histoController = SpTool3Main.getRunTime().getGuiParameterManager().
        getMonteCarloHistoParameters();
    if (super.getCurrentMethod() instanceof SignificanceTestParams && histoController != null) {
      SignificanceTestParams thisParams = ((SignificanceTestParams) super.getCurrentMethod());
      thisParams.getEventParameter().setValue(histoController.getEventParameter());
      thisParams.getMathModification().setValue(histoController.getMathMod());
      // We only set in plainSet, not FX-instance. Hence, force reload.
      super.notifyChangeInPlain();
    }

    // Force super to call this function when its own update method is called
    super.addTaskForRefresh(this::update);

    setTopText("Select two samples to test for significant differences");

    // specials
    VBox container = new VBox(5);
    subContentPane.setCenter(UiUtil.putOnAnchorWithInsets(container));

    Button calcBtn = new Button("Calculate");
    calcBtn.setOnAction(e -> update());

    calcBtn.setPrefWidth(150);
    textArea.setPrefWidth(USER_WIDTH);

    container.getChildren().addAll(calcBtn, textArea);

    //
    distributeCurrentHeightToViews(textArea, super.getHeight());
    super.getDialogPane().heightProperty().addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue,
          Number newValue) {
        distributeCurrentHeightToViews(textArea, newValue.doubleValue());
      }
    });

    update();
  } // end of constructor

  public void update() {

    List<Sample> samples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
    List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
    List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

    if (samples != null && samples.size() > 0 && selIsotopes != null && !selIsotopes.isEmpty()
        && selPops != null && selPops.size() > 0) {
      // so far we can only show one sample in the MC raw view.
      if (super.getCurrentMethod() instanceof SignificanceTestParams) {
        SignificanceTestParams parSet = ((SignificanceTestParams) super.getCurrentMethod());

        // only compare isotope with isotope (without quantification, makes not much sense for most cases)
        Isotope mainIsotope = selIsotopes.get(0);

        // Note Only allow 2 data sets (x and y) to be tested
        List<double[]> xyList = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        // So far, only allow comparison across one isotope
        Isotope selIsotope = selIsotopes.get(0);

        // Either: compare across samples or Populations
        if (samples.size() > 1) {
          Sample sample1 = samples.get(0);
          Sample sample2 = samples.get(1);

          PopulationID selPop = selPops.get(0);

          labels.add("Nick name: '" + sample1.getNickName() +
              "' Sample name: '" + sample1.getSampleFile().getNameWithinFile() +
              "' @ '" + mainIsotope.getFullUIName()
              + "' of '" + selPop.toString()
              + "'");

          labels.add("Nick name: '" + sample2.getNickName() +
              "' Sample name: '" + sample1.getSampleFile().getNameWithinFile() +
              "' @ '" + mainIsotope.getFullUIName()
              + "' of '" + selPop.toString()
              + "'");

          xyList.add(sample1.getData(
              selIsotope, selPop, EventType.NP, parSet.getEventParameter().getValue()));

          xyList.add(sample2.getData(
              selIsotope, selPop, EventType.NP, parSet.getEventParameter().getValue()));

          xyList.removeIf(d -> d.length < 1);

        } else if (samples.size() == 1 && selPops.size() > 1) {
          PopulationID selPop1 = selPops.get(0);
          PopulationID selPop2 = selPops.get(1);

          Sample sample = samples.get(0);

          labels.add("Nick name: '" + sample.getNickName() +
              "' Sample name: '" + sample.getSampleFile().getNameWithinFile() +
              "' @ '" + mainIsotope.getFullUIName()
              + "' of '" + selPop1.toString()
              + "'");

          labels.add("Nick name: '" + sample.getNickName() +
              "' Sample name: '" + sample.getSampleFile().getNameWithinFile() +
              " @ '" + mainIsotope.getFullUIName()
              + "' of '" + selPop2.toString()
              + "'");

          xyList.add(sample.getData(
              selIsotope, selPop1, EventType.NP, parSet.getEventParameter().getValue()));

          xyList.add(sample.getData(
              selIsotope, selPop2, EventType.NP, parSet.getEventParameter().getValue()));

          xyList.removeIf(d -> d.length < 1);
        }

        // Do the actual testing
        String checkStr = SignificanceTests
            .checkAndWarn(parSet.getTestType().getValue(), xyList);
        if (xyList.size() > 1) {
          double p = SignificanceTests.calculate(parSet.getTestType().getValue(), xyList);
          String pString;
          if (p < 0.001) {
            pString = "*** \t p<0.001";
          } else if (p < 0.01) {
            pString = "** \t p<0.01";
          } else if (p < 0.05) {
            pString = "* \t p<0.05";
          } else {
            pString = " -- No significant difference: accept H0!";
          }
          StringBuilder feedback = new StringBuilder();
          feedback.append(checkStr)
              .append(parSet.getTestType().getValue().toString())
              .append("\n").append(pString)
              .append("\n\n").append(" p = ").append(String.valueOf(p))
              .append("\n\nBased on the following samples (n=").append(samples.size())
              .append("):");
          labels.forEach(s -> feedback.append("\n")
              .append(s));
          textArea.setText(feedback.toString());
        } else {
          textArea.setText("No enough data.");
        }
      }
    }

  }

  /*
 On a GridPane / Dialog, we need to specify the prefWidth.
 Normally, we simply put the viewer on an anchor pane and let the UI find its way.
 Also, to keep the submethod list wide enough, we need to specify some minimum width.
 */
  private void distributeCurrentHeightToViews(Control bottom, double totalHeight) {

    // only the bottom half is available
    double topHeight = 0.60 * totalHeight;
    double bottomHeight = 0.40 * totalHeight;

    paramViewerPane.setPrefHeight(topHeight);

    // Also, to keep the submethod list wide enough, we need to specify some minimum width.
    if (bottom != null) {
      bottom.setPrefHeight(bottomHeight);
    }

  }

}
