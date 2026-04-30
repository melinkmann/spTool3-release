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

import gui.Hotkeyable;
import gui.MethodView;
import gui.ParameterView;
import gui.StageFactory;
import gui.dialog.DialogUtil;
import gui.dialog.FxEntryFactory.ParamSetWithDateEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.FxStage;
import gui.dialog.FxStageButton;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javax.annotation.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSet;
import processing.parameterSets.ParamSet;
import processing.parameters.FxParameter;
import util.Functional;

public class ExecuteSubmethod extends Dialog<ParamSet> implements ParameterView, Hotkeyable,
    FxStage {

  protected static final double PREF_WIDTH = 1200;
  protected static final double PREF_HEIGHT = 750;

  // Allow daughters to be called upon change
  private final List<Functional> calls = new ArrayList<>();

  //Try to remember to scroll position and focus.
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();

  private final double user_width;
  private final double user_height;

  // Restrict the type of submethod to show
  private final List<AvailableParameterSets> exclusiveTypes;

  // Keep pointer to current method
  private ParamSet currentMethod;
  private FxParamSet currentFxInstance;

  // Ui Grid
  protected final BorderPane mainBorderPane;
  protected final BorderPane methodPane;
  // Do not add() all the time to a Grid. This will pile up and also cause duplicate child error
  protected final AnchorPane paramViewerPane = new AnchorPane();
  protected final BorderPane subContentPane;

  // Top Buttons to manage submethod selection
  private final ToolBar topToolbar = new ToolBar();
  private final Button newSubMethodBtn = UiUtil.getToolbarBtn(
      "/img/create.png",
      "Create new sub-method");
  private final Button subMethodFromListBtn = UiUtil.getToolbarBtn(
      "/img/searchList.png",
      "Add existing sub-method");

  // at the bottom
  protected final ButtonBar buttonBar;
  protected final FxStageButton primaryButtonType;
  protected final Button primaryButton;
  protected final Button cancelButton;

  /**
   * @param exclusiveParamSet If this parameter is null, we allow loading/creating new method
   *                          instances. Else, we treat this as the "only method", e.g. when you
   *                          just want to calculate a KS-test.
   * @param initialParamSet   When you want to initialize with a given param set.
   * @param exclusiveTypes    If you want to restrict the creation of new Method instances to a
   *                          certain type, e.g., only CsvReaders, put them here.
   * @param title             Title of the pane. If null, a default text is used.
   * @param primaryButtonType Next to the cancel button, this specifies what the user sees as the
   *                          main option (e.g., Run, Select, ...).
   */
  public ExecuteSubmethod(
      @Nullable ParamSet exclusiveParamSet,
      @Nullable ParamSet initialParamSet,
      List<AvailableParameterSets> exclusiveTypes,
      @Nullable String title,
      FxStageButton primaryButtonType,
      @Nullable Double width,
      @Nullable Double height
  ) {

    this.user_width = width == null ? PREF_WIDTH : width;
    this.user_height = height == null ? PREF_HEIGHT : height;

    this.primaryButtonType = primaryButtonType;
    this.exclusiveTypes = exclusiveTypes;

    // MAIN UI
    this.mainBorderPane = new BorderPane();
    this.methodPane = new BorderPane();
    this.subContentPane = new BorderPane();

    mainBorderPane.setCenter(UiUtil.putOnAnchorWithoutInsets(methodPane));
    mainBorderPane.setBottom(UiUtil.putOnAnchorWithoutInsets(subContentPane));

    // TOP TITLE: Customize appearance
    if (title != null) {
      setTopText(title);
    } else {
      setTopText("Execute submethod.");
    }

    // TOP TOOLBAR & METHOD PANE
    topToolbar.setOrientation(Orientation.VERTICAL);
    if (exclusiveParamSet == null) {
      topToolbar.getItems().addAll(newSubMethodBtn, subMethodFromListBtn);
      methodPane.setLeft(UiUtil.putOnAnchorWithoutInsets(topToolbar));
    }
    methodPane.setCenter(UiUtil.putOnAnchorWithoutInsets(paramViewerPane));

    subMethodFromListBtn.setOnAction(e -> {
      SubmethodViewer dialog = new SubmethodViewer(
          new ParamSetWithDateEntryFactory(),
          exclusiveTypes,
          FxStageButton.SELECT,
          null,
          true
      );

      Optional<List<FxParamSet>> result = dialog.showAndWait();

      if (result != null && result.isPresent()) {
        List<FxParamSet> results = result.get();
        if (!results.isEmpty()) {

          this.currentMethod = results.get(0).getPlainSet(); // the fx instance is uneditable!
          this.currentFxInstance = currentMethod.getObservableInstance();
          this.currentFxInstance.setController(this);
        }
      }
      updateParameterPane(currentFxInstance);
    });

    newSubMethodBtn.setOnAction(e -> {
      FxParamSet fxSet = null;
      ParamSet set = null;

      // If only one type is allowed, skip the dialog
      if (exclusiveTypes.size() == 1) {
        set = exclusiveTypes.get(0).get();
        fxSet = set.getObservableInstance();
        fxSet.setController(this);
      } else {
        List<FxParamSet> validSets = new ArrayList<>(AvailableParameterSets.get(exclusiveTypes));
        // Very important!
        validSets.forEach((fx -> fx.setController(this)));
        // Show selection dialog
        CreateSingleSubMethodDialog dialog = new CreateSingleSubMethodDialog(
            validSets, new SimpleEntryFactory<>());
        Optional<List<FxParamSet>> res = dialog.showAndWait();

        if (res.isPresent() && !res.get().isEmpty()) {
          fxSet = res.get().get(0);
          set = fxSet.getPlainSet();
        }
      }

      // Check results and set
      if (set != null) {
        this.currentMethod = set;
        this.currentFxInstance = fxSet;
      }
      updateParameterPane(currentFxInstance);
    });

    // BOTTOM TOOLBAR
    this.buttonBar = new ButtonBar();
    this.primaryButton = primaryButtonType.getBold(this);
    this.cancelButton = new Button("Cancel");
    this.buttonBar.getButtons().addAll(primaryButton, cancelButton);
    this.cancelButton.setOnAction(e -> closeAndCancelChanges());
    this.subContentPane.setBottom(buttonBar);

    makeStyles();

    // initialize with method
    if (exclusiveParamSet != null) {
      this.currentMethod = exclusiveParamSet.getCopyWithNewDate();
      this.currentFxInstance = currentMethod.getObservableInstance();
      this.currentFxInstance.setController(this);
      updateParameterPane(currentFxInstance); // dont forget this to show stuff...
    } else {
      if (initialParamSet != null) {
        this.currentMethod = initialParamSet;
        this.currentFxInstance = initialParamSet.getObservableInstance();
        this.currentFxInstance.setController(this);
        updateParameterPane(currentFxInstance);
      } else {
        // create/prompt the user to create a new instance.
        newSubMethodBtn.fire();
      }
    }

    // end of constructor
  }

  // Is called from the ParamSet if parameters need to be reloaded
  @Override
  public void notifyItemChange() {
    updateParameterPane(currentFxInstance);
  }

  @Override
  public void notifyValueChange() {
    // Is triggered e.g., by a change of custom delimiter
    updateParameterPane(currentFxInstance);
  }

  protected void notifyChangeInPlain() {
    // Force a reloading of the value from the plain parameter.
    currentFxInstance.notifyItemChange();
  }

  protected void updateParameterPane(FxParamSet selSet) {
    if (selSet != null) {

      // just some caution about pile-up of the map (which should not happen as there are few FxSets)
      if (viewMap.size() > 1E2) {
        viewMap.clear();
      }

      // Note that this map serves the purpose to maintain focus and scroll position when updating the pane :-)
      ListView<FxParameter<?>> view;
      if (viewMap.containsKey(selSet)) {
        view = viewMap.get(selSet);
        view.getItems().clear();
        view.getItems().addAll(selSet.getActiveFxParameters());
      } else {
        view = MethodView.createParamView(selSet);
        viewMap.put(selSet, view);
      }

      paramViewerPane.getChildren().clear();
      paramViewerPane.getChildren().add(view);
      AnchorPane.setTopAnchor(view, 0d);
      AnchorPane.setRightAnchor(view, 0d);
      AnchorPane.setBottomAnchor(view, 0d);
      AnchorPane.setLeftAnchor(view, 0d);

    } else {
      paramViewerPane.getChildren().clear();
    }

    // now call the daughters
    calls.forEach(f->f.proceed(getOwner()));
  }

  protected void makeStyles() {
    DialogUtil.makeEscapeClosable(this);

    //Important to actually fill the grid that gets a smaller size (see below)
    //listSearchView.getListView().setPrefSize(PREF_WIDTH_LIST, PREF_HEIGHT_LIST);

    final DialogPane dialogPane = getDialogPane();
    // Dialog
    //-- pane style
    setTitle("Execute submethod.");
    Stage dialogStage = (Stage) getDialogPane().getScene().getWindow();
    dialogStage.initModality(Modality.NONE); // Make it non-modal
    dialogStage.setAlwaysOnTop(true);
    dialogStage.getIcons().add(UiUtil.getImage("/img/start.png"));
    dialogStage.setResizable(true);
    // TODO: Manage whether this is used or the automatic fit
    dialogPane.setPrefSize(user_width, user_height);

    // clear defaults
    dialogPane.getButtonTypes().clear();
    dialogPane.setPadding(Insets.EMPTY);
    dialogPane.setContent(UiUtil.putOnAnchorWithInsets(mainBorderPane));

    // padding
    methodPane.setPadding(new Insets(2));
    subContentPane.setPadding(new Insets(2));

    // Close on Windows close symbol
    // Else: behaviour on exit via "x" is not defined
    dialogPane.getScene().getWindow().setOnCloseRequest(event -> {
      event.consume();
      closeAndCancelChanges();
    });

    activateHotkeys(dialogPane.getScene());
  }

  protected void setTopText(String topText) {
    Label top = new Label(topText);
    top.setStyle("-fx-font-weight: bold");
    top.setFont(new Font(15));
    top.setMinHeight(30);
    methodPane.setTop(top);
    BorderPane.setAlignment(top, Pos.CENTER);
  }

  // FxStage methods
  @Override
  public void closeAndKeepCurrentState() {
    super.setResult(null);
    killScene();
  }

  // Cancel button or little "x"
  @Override
  public void closeAndCancelChanges() {
    super.setResult(null);
    killScene();
  }

  @Override
  public void saveAndSetResults() {
    super.setResult(currentMethod);
    killScene();
  }

  @Override
  public void closeAndContinue() {
    super.setResult(currentMethod);
    killScene();
    // Idea: If, e.g., the calling class has a second step after THIS dialog,
    // call it by overriding this method in the respective dialog instance.
  }

  //////////////////////////////////////////////////////////////////////////////////

  protected void killScene() {
    Scene scene = getDialogPane().getScene();
    if (scene != null) {
      Stage dialogStage = (Stage) scene.getWindow();
      dialogStage.close();
    }
  }

  //////////////////////////////////////////////////////////////////////////////////

  @Override
  public void activateHotkeys(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_PRESSED, (KeyEvent event) -> {
      // Save on control s
      if (StageFactory.KEY_CTL_S.match(event)) {
        // do nothing

        // If "continue" like Button is present and control enter is hit, call "close and continue"
      } else if (StageFactory.KEY_CTL_ENTER.match(event)
          && (primaryButtonType.equals(FxStageButton.CONTINUE)
          || primaryButtonType.equals(FxStageButton.SELECT)
          || primaryButtonType.equals(FxStageButton.RUN))) {
        closeAndContinue();
      }
    });
  }


  ////////////////////////////////
  public ParamSet getCurrentMethod() {
    return currentMethod;
  }

  public void addTaskForRefresh(Functional functional) {
    calls.add(functional);
  }
}
