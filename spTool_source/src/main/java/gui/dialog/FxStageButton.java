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

import javafx.scene.control.Button;

public enum FxStageButton {

  /**
   * Closes stage but indicates that something will follow. Is not intended to save something.
   */

  CONTINUE {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Continue");
      button.setOnAction(e -> stage.closeAndContinue());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }
  },

  /**
   * Essentially the same as "Continue" but says "Select" literally to give the user a better idea.
   */
  SELECT {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Select");
      button.setOnAction(e -> stage.closeAndContinue());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }
  },

  /**
   * Essentially the same as "Continue" but says "Run" literally to give the user a better idea.
   */
  RUN {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Run");
      button.setOnAction(e -> stage.closeAndContinue());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }
  },

  /**
   * Closes stage and explicitly states that something will be saved.
   */

  SAVE {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Save & close");
      button.setOnAction(e -> stage.saveAndSetResults());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }

  },

  /**
   * Closes stage and indicates that the operation is over. Nothing follows and nothing is saved.
   */

  CLOSE {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Close");
      button.setOnAction(e -> stage.closeAndKeepCurrentState());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }

  },

  /**
   * Closes stage and indicates that the operation is over. Nothing follows and nothing is saved.
   * This has a stronger emphasis on "resetting" things to the state before.
   */

  CANCEL {
    @Override
    public Button get(FxStage stage) {
      Button button = new Button("Cancel");
      button.setOnAction(e -> stage.closeAndCancelChanges());
      return button;
    }

    @Override
    public Button getBold(FxStage stage) {
      Button button = get(stage);
      button.setStyle("-fx-font-weight: bold");
      return button;
    }

  };

  public abstract Button getBold(FxStage stage);

  public abstract Button get(FxStage stage);


}
