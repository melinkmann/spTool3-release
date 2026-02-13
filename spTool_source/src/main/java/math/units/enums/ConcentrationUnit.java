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

package math.units.enums;

import java.io.Serializable;

import math.units.ConvertibleUnit;

public enum ConcentrationUnit implements ConvertibleUnit, Serializable {

  GRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "g/L";
    }
  },

  MILLIGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-3;
    }

    @Override
    public String getLiteralString() {
      return "mg/L (ppm)";
    }
  },

  MICROGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-6;
    }

    @Override
    public String getLiteralString() {
      return "µg/L (ppb)";
    }
  },

  NANOGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-9;
    }

    @Override
    public String getLiteralString() {
      return "ng/L (ppt)";
    }
  },

  PICOGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-12;
    }

    @Override
    public String getLiteralString() {
      return "pg/L (ppq)";
    }
  },

  FMETOGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-15;
    }

    @Override
    public String getLiteralString() {
      return "fg/L";
    }
  },

  ATTOGRAM_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E-18;
    }

    @Override
    public String getLiteralString() {
      return "ag/L";
    }
  },

  NP_PER_LITRE {
    @Override
    public double getMultiplierToSI() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "NP/L";
    }
  },

  NP_PER_MILLILITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E3;
    }

    @Override
    public String getLiteralString() {
      return "NP/mL";
    }
  },

  NP_PER_MICROLITRE {
    @Override
    public double getMultiplierToSI() {
      return 1E6;
    }

    @Override
    public String getLiteralString() {
      return "NP/µL";
    }
  };

  @Override
  public double toSIRoot(double value) {
    final double val = value * getMultiplierToSI();
    return val;
  }


  @Override
  public double toTarget(double siUnitValue) {
    final double val = siUnitValue / getMultiplierToSI();
    return val;
  }

  public abstract double getMultiplierToSI();

  @Override
  public boolean isCompatible(ConvertibleUnit unit) {
    return unit instanceof ConcentrationUnit;
  }

  public static ConcentrationUnit[] getIonic() {
    return new ConcentrationUnit[]{
        GRAM_PER_LITRE,
        MILLIGRAM_PER_LITRE,
        MICROGRAM_PER_LITRE,
        NANOGRAM_PER_LITRE,
        PICOGRAM_PER_LITRE,
        FMETOGRAM_PER_LITRE,
        ATTOGRAM_PER_LITRE
    };
  }

  public static ConcentrationUnit[] getNP() {
    return new ConcentrationUnit[]{
        NP_PER_LITRE,
        NP_PER_MILLILITRE,
        NP_PER_MICROLITRE
    };
  }

}
