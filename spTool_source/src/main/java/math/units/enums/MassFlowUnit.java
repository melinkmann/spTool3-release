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

public enum MassFlowUnit implements ConvertibleUnit, Serializable {

  GRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "g/s";
    }
  },

  MILLIGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-3;
    }

    @Override
    public String getLiteralString() {
      return "mg/s";
    }
  },

  MICROGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-6;
    }

    @Override
    public String getLiteralString() {
      return "µg/s";
    }
  },

  NANOGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-9;
    }

    @Override
    public String getLiteralString() {
      return "ng/s";
    }
  },

  PICOGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-12;
    }

    @Override
    public String getLiteralString() {
      return "pg/s";
    }
  },

  FMETOGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-15;
    }

    @Override
    public String getLiteralString() {
      return "fg/s";
    }
  },

  ATTOGRAM_PER_SECOND {
    @Override
    public double getMultiplierToRoot() {
      return 1E-18;
    }

    @Override
    public String getLiteralString() {
      return "ag/s";
    }
  };


  @Override
  public double toSIRoot(double value) {
    final double val = value * getMultiplierToRoot();
    return val;
  }


  @Override
  public double toTarget(double siUnitValue) {
    final double val = siUnitValue / getMultiplierToRoot();
    return val;
  }

  public abstract double getMultiplierToRoot();

  @Override
  public boolean isCompatible(ConvertibleUnit unit) {
    return unit instanceof MassFlowUnit;
  }
}