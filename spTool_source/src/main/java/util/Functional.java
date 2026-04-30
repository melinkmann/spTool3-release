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

package util;

import javafx.stage.Window;

import java.io.Serializable;

public interface Functional extends Serializable {

  public void proceed();

  // for functions that need a window/parent (e.g., the PTOE popup)
  default void proceed(Window parent) {
    proceed(); // fall back to no-window version by default
  }

  static Functional empty() {
    return new Functional() {
      @Override
      public void proceed() {
        // do nothing
      }

      @Override
      public void proceed(Window parent) {
        // do nothing
      }
    };
  }

}
