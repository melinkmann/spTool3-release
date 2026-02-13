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

import gui.MethodView;
import gui.ParameterView;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.mainImpl.ChooseMultipleFromListDialog;
import gui.viewerCells.ParamSetListCell;
import io.GlobalIO;
import io.XmlUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import javax.annotation.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.FxParamSet;
import processing.parameters.FxParameter;

public class SubmethodViewer extends ChooseMultipleFromListDialog<FxParamSet> implements
    ParameterView {


  protected static final double PREF_WIDTH = 1200;
  protected static final double PREF_HEIGHT = 750;

  // Restrict the type to show
  private final List<AvailableParameterSets> exclusiveTypes = new ArrayList<>();

  //Try to remember to scroll position and focus - works!!!
  private final HashMap<FxParamSet, ListView<FxParameter<?>>> viewMap = new HashMap<>();

  // Do not add() all the time to a Grid. This will pile up and also cause duplicate child error
  private final AnchorPane paramViewerPane = new AnchorPane();

  public SubmethodViewer(
      FxEntryFactory<FxParamSet> entryFactory,
      List<AvailableParameterSets> exclusiveTypes,
      FxStageButton primaryButton,
      @Nullable String title,
      boolean closeOnDoubleClick) {
    super(entryFactory.create(readContentAndFilter(exclusiveTypes)),
        entryFactory,
        false,
        false,
        false,
        closeOnDoubleClick,
        false,
        primaryButton);

    // store for later access
    this.exclusiveTypes.addAll(exclusiveTypes);

    // Customize appearance
    super.setTitle("Submethod library."); // Override
    if (title != null) {
      super.setTopText(title);
    } else {
      super.setTopText("View and select submethods.");
    }

    // When the user is supposed to only select something
    if (closeOnDoubleClick) {
      listSearchView.getListView().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
    }

    // Make Larger
    final DialogPane dialogPane = getDialogPane();
    dialogPane.setPrefSize(PREF_WIDTH, PREF_HEIGHT);

    // put anchor pane for the parameter view in place
    super.contentGrid.add(paramViewerPane, 2, 1, 1, 1);

    // Set listening Controller for each submethod
    listSearchView.getAllOptionsUnmodifiable().stream()
        .map(FxEntry::unwrap)
        .forEach(fxParamSet -> fxParamSet.setController(this));

    listSearchView.setCellFactory(l -> new ParamSetListCell());

    listSearchView.getListView().getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, newValue) -> {
          updateParameterPane(newValue);
        });

    // When I edit the label in the grid on the right, I want the list view to change immediately:
    // https://stackoverflow.com/questions/28285507/is-there-a-way-to-bind-the-content-of-a-listproperty-in-javafx
    // https://stackoverflow.com/questions/31687642/callback-and-extractors-for-javafx-observablelist
    this.listSearchView.getListView().setItems(FXCollections.observableArrayList(
        new Callback<FxEntry<FxParamSet>, Observable[]>() {
          @Override
          public Observable[] call(FxEntry<FxParamSet> param) {
            return new Observable[]{param.getCellLabelProperty()};
          }
        }));

    // After "overriding" the setItems() method, we need to set the already existing items again.
    listSearchView.filterList();

    // ListView preferences
    listSearchView.getListView().getSelectionModel().selectFirst();

    // Initialize with content
    notifyItemChange();
  }


  // Is called from the ParamSet if parameters need to be reloaded
  @Override
  public void notifyItemChange() {
    FxEntry<FxParamSet> selSet = listSearchView.getListView().getSelectionModel().getSelectedItem();
    updateParameterPane(selSet);
  }

  // This class does not allow changes, it is readonly essentially.
  @Override
  public void notifyValueChange() {
    notifyItemChange(); // Is this a good call?? Todo: check!
  }


  // Private helper methods
  @Nullable
  private FxParamSet getSelectedFxSet() {
    FxParamSet fxParamSet = null;
    FxEntry<FxParamSet> entry = listSearchView.getListView().getSelectionModel()
        .getSelectedItem();
    if (entry != null) {
      fxParamSet = entry.unwrap();
    }
    return fxParamSet;
  }

  private List<FxParamSet> getSelectedFxSets() {
    List<FxEntry<FxParamSet>> entries = listSearchView.getListView().getSelectionModel()
        .getSelectedItems();
    List<FxParamSet> fxParamSets = entries.stream()
        .filter(Objects::nonNull)
        .map(FxEntry::unwrap)
        .collect(Collectors.toList());
    return fxParamSets;
  }

  // Re-read from submethod file while respecting potential filters
  private static List<FxParamSet> readContentAndFilter(List<AvailableParameterSets> exclsvTypes) {

    List<FxParamSet> all = XmlUtil.getSubMethodsFromFile(GlobalIO.makeSubMethodsFile());
    List<FxParamSet> matches;

    // Only filter if a filter is given, else ignore them
    if (exclsvTypes.isEmpty()) {
      matches = all;
    } else {
      matches = all.stream()
          .filter(fxSet -> exclsvTypes.contains(fxSet.getPlainSet().getEnum()))
          .collect(Collectors.toList());
    }

    // Make read-only
    matches.forEach(FxParamSet::setUneditable);

    return matches;
  }

  private void updateParameterPane(FxEntry<FxParamSet> selSet) {
    if (selSet != null) {

      // just some caution about pile-up of the map (which should not happen as there are few FxSets)
      if (viewMap.size() > 1E5) {
        viewMap.clear();
      }

      ListView<FxParameter<?>> view;
      if (viewMap.containsKey(selSet.unwrap())) {
        view = viewMap.get(selSet.unwrap());
        view.getItems().clear();
        view.getItems().addAll(selSet.unwrap().getActiveFxParameters());
      } else {
        view = MethodView.createParamView(selSet.unwrap());
        viewMap.put(selSet.unwrap(), view);
      }

      paramViewerPane.getChildren().clear();
      paramViewerPane.getChildren().add(view);
      AnchorPane.setTopAnchor(view, 0d);
      AnchorPane.setRightAnchor(view, 0d);
      AnchorPane.setBottomAnchor(view, 0d);
      AnchorPane.setLeftAnchor(view, 0d);

      final ListView<FxEntry<FxParamSet>> submethodListView = super.listSearchView.getListView();
      distributeCurrentWidthToViews(submethodListView, view, super.getWidth());

      super.getDialogPane().widthProperty().addListener(new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observable, Number oldValue,
            Number newValue) {
          distributeCurrentWidthToViews(submethodListView, view, newValue.doubleValue());
        }
      });

    } else {
      paramViewerPane.getChildren().clear();
    }
  }

  /*
 On a GridPane / Dialog, we need to specify the prefWidth.
 Normally, we simply put the viewer on an anchor pane and let the UI find its way.
 Also, to keep the submethod list wide enough, we need to specify some minimum width.
 */
  private void distributeCurrentWidthToViews(ListView<FxEntry<FxParamSet>> submethodListView,
      ListView<FxParameter<?>> parameterList, double totalWidth) {

    double methodListWidth = 0.4 * totalWidth;
    double parameterListWidth = 0.6 * totalWidth;

    // Also, to keep the submethod list wide enough, we need to specify some minimum width.
    if (submethodListView != null) {
      submethodListView.setPrefWidth(methodListWidth);
    }

    if (parameterList != null) {
      parameterList.setPrefWidth(parameterListWidth);
    }
  }

}
