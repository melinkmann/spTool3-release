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

package visualizer.styles;

import javax.annotation.Nullable;

public interface LineDash {

  @Nullable
  float[] get();

  public static class CustomLLineDash implements LineDash {

    @Nullable
    private final float[] dashPattern;

    public CustomLLineDash(@Nullable float[] dashPattern) {
      this.dashPattern = dashPattern;
    }

    // No dashes: solid line
    public CustomLLineDash() {
      this.dashPattern = null;
    }

    @Override
    @Nullable
    public float[] get() {
      return dashPattern;
    }
  }

}
