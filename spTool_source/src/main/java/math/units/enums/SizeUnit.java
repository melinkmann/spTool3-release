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

public enum SizeUnit implements ConvertibleUnit, Serializable {

  MILLI_METER {
    @Override
    public double getExponent() {
      return 1E-3;
    }

    @Override
    public String getLiteralString() {
      return "mm";
    }
  },

  MICRO_METER {
    @Override
    public double getExponent() {
      return 1E-6;
    }

    @Override
    public String getLiteralString() {
      return "µm";
    }
  },

  NANO_METER {
    @Override
    public double getExponent() {
      return 1E-9;
    }


    @Override
    public String getLiteralString() {
      return "nm";
    }

  };

  @Override
  public double toSIRoot(double value) {
    final double val = value * getExponent();
    return val;
  }


  @Override
  public double toTarget(double siUnitValue) {
    final double val = siUnitValue / getExponent();
    return val;
  }

  public abstract double getExponent();

  @Override
  public boolean isCompatible(ConvertibleUnit unit) {
    return unit instanceof SizeUnit;
  }

}
