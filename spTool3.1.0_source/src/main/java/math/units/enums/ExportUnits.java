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

import math.units.Unit;

import java.io.Serializable;

public enum ExportUnits implements Unit, Serializable {

  CTS {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return IntensityUnit.CTS;
    }
  },

  PICO_GRAM {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return MassUnit.PICO_GRAM;
    }
  },

  FEMTO_GRAM {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return MassUnit.FEMTO_GRAM;
    }
  },

  ATTO_GRAM {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return MassUnit.ATTO_GRAM;
    }
  },

  MICRO_METER {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return SizeUnit.MICRO_METER;
    }
  },

  NANO_METER {
    @Override
    public String getLiteralString() {
      return getUnit().getLiteralString();
    }

    @Override
    public Unit getUnit() {
      return SizeUnit.NANO_METER;
    }
  };


  @Override
  public abstract String

  getLiteralString();

  public abstract Unit getUnit();
}
