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

package gui;

public interface ParameterView {

  /**
   * An item has gotten new children or removed children an the entire pane needs to be rebuilt.
   * Note: This method forces the UI to build the parameter set view completely new!
   */
  void notifyItemChange();


  /**
   * A value has been changed and the "method was saved" icon in the main view provided by the
   * MethodView class needs updating. Note: This method only adjust for "method was saved" and other
   * "decorative" details. Update: it now also serves to trigger redrawing graphs.
   */
  void notifyValueChange();

}
