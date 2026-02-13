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

package gui.util;

import processing.parameters.CommentFxParameter;
import processing.parameters.FxParameter;

public abstract class GlobalFields {

  public static final double FX_COMMENT_HEIGHT = 100;
  public static final double FX_LABEL_WIDTH = 250;
  public static final double FX_ITEM_WIDTH = 300;
  public static final double FX_PATH_WIDTH = 600;
  public static final double FX_ITEM_HEIGHT = 25;
  //
  public static final double METHOD_LIST_WIDTH = 250;

  public static double getGridWidth() {
    return FX_ITEM_WIDTH + FX_PATH_WIDTH + 10;
  }

  public static double getScrollWidth() {
    return getGridWidth() + 35;
  }

  public static double getViewerWindowWidth() {
    return getScrollWidth() + 35;
  }

  public static double getEditorViewerWindowWidth() {
    return getScrollWidth() + METHOD_LIST_WIDTH + 35;
  }


  public static double getFxParHeight(FxParameter<?> par) {
    double prefRowHeight = FX_ITEM_HEIGHT;
    if (par instanceof CommentFxParameter) {
      prefRowHeight = FX_COMMENT_HEIGHT;
    }
    return prefRowHeight;
  }

}
