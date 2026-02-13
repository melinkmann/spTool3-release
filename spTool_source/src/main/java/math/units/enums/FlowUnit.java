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

public enum FlowUnit implements ConvertibleUnit, Serializable {

  LITRE_PER_MINUTE {
    @Override
    public double getMultiplier() {
      return 1;
    }

    @Override
    public String getLiteralString() {
      return "L/min";
    }
  },

  MILLILITRE_PER_MINUTE {
    @Override
    public double getMultiplier() {
      return 1E-3;
    }

    @Override
    public String getLiteralString() {
      return "mL/min";
    }
  },

  MICROLITRE_PER_MINUTE {
    @Override
    public double getMultiplier() {
      return 1E-6;
    }

    @Override
    public String getLiteralString() {
      return "µL/min";
    }
  },

  LITRE_PER_SECOND {
    @Override
    public double getMultiplier() {
      return 60;
    }

    @Override
    public String getLiteralString() {
      return "L/s";
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
    return unit instanceof FlowUnit;
  }

}
