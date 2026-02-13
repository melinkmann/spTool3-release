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

public enum LineLineDashDefaults implements LineDash {


  STRAIGHT {
    @Override
    public float[] get() {
      return null;
    }
  },

  L {
    @Override
    public float[] get() {
      return new float[]{10.0f, LARGE_GAP};
    }
  },

  L_2 {
    @Override
    public float[] get() {
      return new float[]{10.0f, LARGE_GAP + 3};
    }
  },

  L_3 {
    @Override
    public float[] get() {
      return new float[]{10.0f, LARGE_GAP - 3};
    }
  },

  M {
    @Override
    public float[] get() {
      return new float[]{6.0f, NORMAL_GAP};
    }
  },

  S {
    @Override
    public float[] get() {
      return new float[]{2.0f, NORMAL_GAP};
    }
  },

  TINY {
    @Override
    public float[] get() {
      return new float[]{0.1f, LARGE_GAP};
    }
  },


  LS {
    @Override
    public float[] get() {
      return new float[]{10.0f, NORMAL_GAP, 2.0f, LARGE_GAP};
    }
  };


  public static final float LARGE_GAP = 10.0f;
  public static final float NORMAL_GAP = 6.0f;


  @Override
  public abstract float[] get();
}
