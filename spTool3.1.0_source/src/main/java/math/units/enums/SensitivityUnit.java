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

import math.units.ConvertibleUnit;

import java.io.Serializable;

public enum SensitivityUnit implements ConvertibleUnit, Serializable {

  CTS_PER_GRAM {
    @Override
    public double getMultiplier() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "cts/g";
    }
  },

  CTS_PER_MILLIGRAM {
    @Override
    public double getMultiplier() {
      return 1E3;
    }

    @Override
    public String getLiteralString() {
      return "cts/mg";
    }
  },


  CTS_PER_MICROGRAM {
    @Override
    public double getMultiplier() {
      return 1E6;
    }

    @Override
    public String getLiteralString() {
      return "cts/µg";
    }
  },

  CTS_PER_NANOGRAM {
    @Override
    public double getMultiplier() {
      return 1E9;
    }

    @Override
    public String getLiteralString() {
      return "cts/ng";
    }
  },

  CTS_PER_PICOGRAM {
    @Override
    public double getMultiplier() {
      return 1E12;
    }

    @Override
    public String getLiteralString() {
      return "cts/pg";
    }
  },

  CTS_PER_FEMTOGRAM {
    @Override
    public double getMultiplier() {
      return 1E15;
    }

    @Override
    public String getLiteralString() {
      return "cts/fg";
    }
  },

  CTS_PER_ATTOGRAM {
    @Override
    public double getMultiplier() {
      return 1E18;
    }

    @Override
    public String getLiteralString() {
      return "cts/ag";
    }
  };


  @Override
  public double toSIRoot(double value) {
    final double val = value * getMultiplier();
    return val;
  }


  @Override
  public double toTarget(double siUnitValue) {
    final double val = siUnitValue / getMultiplier();
    return val;
  }

  public abstract double getMultiplier();

  @Override
  public boolean isCompatible(ConvertibleUnit unit) {
    return unit instanceof SensitivityUnit;
  }


}
