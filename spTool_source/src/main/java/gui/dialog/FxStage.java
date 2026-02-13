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

/**
 * Related to {@link FxStageButton} whose Button instances call these methods.
 */
public interface FxStage {

  /**
   * Closes stage and indicates that the operation is over. Nothing follows and nothing is saved.
   */
  void closeAndKeepCurrentState();

  /**
   * Closes stage and indicates that the operation is over. Nothing follows and nothing is saved. In
   * context of JavaFx Dialogs, this will return null instead of results.
   */
  void closeAndCancelChanges();

  /**
   * Explicitly states that something will be saved. Note: Closing must be done externally in the
   * SAVE? Dialog or more general, in the overriding classes.
   */
  void saveAndSetResults();

  /**
   * Closes stage but indicates that something will follow. Is not intended to save something.
   */
  void closeAndContinue();
}
