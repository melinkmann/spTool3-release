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

package processing.options;

public enum PDF {

  NONE {
    @Override
    public String toString() {
      return "None";
    }
  },

  POISSON {
    @Override
    public String toString() {
      return "Poisson";
    }
  },

  COMPOUND_POISSON {
    @Override
    public String toString() {
      return "Compound Poisson";
    }
  },

  POISSON_OVERDISPERSED {
    @Override
    public String toString() {
      return "Overdispersed Poisson";
    }
  },

  COMPOUND_POISSON_OVERDISPERSED {
    @Override
    public String toString() {
      return "Overdispersed compound Poisson";
    }
  },


  NORMAL {
    @Override
    public String toString() {
      return "Normal";
    }
  },

  LOGNORMAL {
    @Override
    public String toString() {
      return "Lognormal";
    }
  };


  public static PDF[] getGaussians() {
    return new PDF[]{NORMAL,LOGNORMAL};
  }

  public static PDF[] getPoissons() {
    return new PDF[]{POISSON,COMPOUND_POISSON};
  }

  public static PDF[] getResampling() {
    return new PDF[]{NONE, POISSON,COMPOUND_POISSON, POISSON_OVERDISPERSED, COMPOUND_POISSON_OVERDISPERSED};
  }

  public static PDF[] getOverdispersed() {
    return new PDF[]{POISSON_OVERDISPERSED, COMPOUND_POISSON_OVERDISPERSED};
  }

}
