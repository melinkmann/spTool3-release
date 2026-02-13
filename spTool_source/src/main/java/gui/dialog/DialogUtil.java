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

package gui.dialog;

import gui.dialog.notification.NotificationFactory;
import gui.listAndSearch.ListAndSearchView;
import gui.util.UiUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import processing.parameterSets.FxParamSet;
import processing.parameters.Parameter;

public abstract class DialogUtil {



  // Utils
  public static double[] estimateTextWidthHeight(String message) {

    double[] wh = new double[2];
    if (message != null) {

      String[] lines = message.split(System.lineSeparator());
      // Maybe separator was wrong?
      if (lines.length == 1) {
        lines = message.split("\n");
      }
      if (lines.length == 1) {
        lines = message.split("\r\n");
      }

      int maxLen = Arrays.stream(lines).mapToInt(String::length).summaryStatistics().getMax();
      wh = new double[]{maxLen * 6, lines.length * 19};
    }
    return wh;
  }

  public static double[] estimateWindowWidthHeight(String message) {
    double[] wh = estimateTextWidthHeight(message);
    wh[0] += 50;
    wh[1] += 300;
    return wh;
  }


  // General
  public static void makeEscapeClosable(Dialog<?> dialog) {
    DialogPane dialogPane = dialog.getDialogPane();
    makeEscapeClosable(dialogPane.getScene());
  }

  public static void makeEscapeClosable(Scene scene) {
    scene.addEventHandler(KeyEvent.KEY_RELEASED, (KeyEvent event) -> {
      if (KeyCode.ESCAPE == event.getCode() && event.isShiftDown()) {
        Stage dialogStage = (Stage) scene.getWindow();
        dialogStage.close();
      }
    });
  }

  // Buttons that appear all the time:
  public static MenuItem getSetParameterSetAsDefaultMenuItem(
      Supplier<List<FxParamSet>> fxParamSetSupplier) {
    MenuItem asDefault = UiUtil.getImageMenuItem("Set", "/img/overridechange.png");
    asDefault.setOnAction(e -> {
      List<FxParamSet> fxParamSets = fxParamSetSupplier.get();
      if (!fxParamSets.isEmpty()) {

        NotificationFactory
            .openYesNo("Override default with current settings. This is irreversible.",
                () -> {
                  for (FxParamSet fxSet : fxParamSets) {
                    if (fxSet != null) {
                      fxSet.getPlainSet().setCurrentValuesAsDefault();
                      // If the date is shown as part of the label, we have to update that.
                      // (A bit clumsy, a proper listener for the date value may be better, but it should work like this as well).
                      fxSet.notifyLabelChange();
                    }
                  }
                });
      }
    });
    return asDefault;
  }

  public static Button getSetParameterSetAsDefaultButton(
      Supplier<List<FxParamSet>> fxParamSetSupplier) {
    Button asDefault = UiUtil.getToolbarBtn("/img/overridechange.png",
        "Set the current settings as the new default");

    asDefault.setOnAction(e -> {
      List<FxParamSet> fxParamSets = fxParamSetSupplier.get();
      if (!fxParamSets.isEmpty()) {

        NotificationFactory
            .openYesNo("Override default with current settings. This is irreversible.",
                () -> {
                  for (FxParamSet fxSet : fxParamSets) {
                    if (fxSet != null) {
                      fxSet.getPlainSet().setCurrentValuesAsDefault();
                      // If the date is shown as part of the label, we have to update that.
                      // (A bit clumsy, a proper listener for the date value may be better, but it should work like this as well).
                      fxSet.notifyLabelChange();
                    }
                  }
                });
      }
    });
    return asDefault;
  }


  public static MenuItem getRestoreParameterSetMenuItem(Supplier<List<FxParamSet>> fxParamSetSupplier) {
    MenuItem restoreDefault = UiUtil.getImageMenuItem("Reset", "/img/ignorechange.png");
    restoreDefault.setOnAction(e -> {
      List<FxParamSet> fxParamSets = fxParamSetSupplier.get();
      if (!fxParamSets.isEmpty()) {
        NotificationFactory.openYesNo(
            "Restore default state and override current settings. This is irreversible.",
            () -> {
              for (FxParamSet fxSet : fxParamSets) {
                if (fxSet != null) {
                  fxSet.getPlainSet().listActiveParameters().forEach(Parameter::resetToDefault);
                  fxSet.notifyItemChange();
                }
              }
            });
      }
    });
    return restoreDefault;
  }

  public static Button getRestoreParameterSetButton(Supplier<List<FxParamSet>> fxParamSetSupplier) {
    Button restoreDefault = UiUtil.getToolbarBtn( "/img/ignorechange.png",
        "Reset the current settings back to the default values");
    restoreDefault.setOnAction(e -> {
      List<FxParamSet> fxParamSets = fxParamSetSupplier.get();
      if (!fxParamSets.isEmpty()) {
        NotificationFactory.openYesNo(
            "Restore default state and override current settings. This is irreversible.",
            () -> {
              for (FxParamSet fxSet : fxParamSets) {
                if (fxSet != null) {
                  fxSet.getPlainSet().listActiveParameters().forEach(Parameter::resetToDefault);
                  fxSet.notifyItemChange();
                }
              }
            });
      }
    });
    return restoreDefault;
  }

  public static <T> MenuItem getDeleteSelectedItemsFromView(ListAndSearchView<T> listSearchView) {
    MenuItem deleteMenu = UiUtil.getImageMenuItem("Delete template", "/img/delete.png");
    deleteMenu.setOnAction(e -> NotificationFactory.openYesNo("Delete selected items.",
        () -> {
          List<FxEntry<T>> toBeDeleted = new ArrayList<>(listSearchView.getListView()
              .getSelectionModel().getSelectedItems());

          // Remove from Storage List in Super Class
          listSearchView.removeContent(toBeDeleted);

          // Refresh ListView (based on the "AllOptions" list)
          listSearchView.filterList();
        }));

    return deleteMenu;
  }


}
