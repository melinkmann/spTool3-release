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

import core.SpTool3Main;
import gui.util.UiString;

import java.io.Serializable;

public enum CsvInterpreters implements Serializable, UiString {

  AGILENT {
    @Override
    public String getUiString() {
      return "Agilent csv data";
    }
  },


  THERMO_XY {
    @Override
    public String getUiString() {
      return "Thermo Fisher csv data [Format XYYY]";
    }
  },


  THERMO_TOPDOWN {
    @Override
    public String getUiString() {
      return "Thermo Fisher csv data [Format: Y above Time]";
    }
  },

  ANALYTIK_JENA {
    @Override
    public String getUiString() {
      return "Analytik Jena";
    }
  },

  CUSTOM_PEAKS {
    @Override
    public String getUiString() {
      return "Peak import";
    }
  },

  LPC {
    @Override
    public String getUiString() {
      return "LPC 'single particle list' import";
    }
  },

  CUSTOM_TRA {
    @Override
    public String getUiString() {
      return "Time resolved data";
    }
  },


  MZMINE_TRA {
    @Override
    public String getUiString() {
      return "MZMine time-resolved EICs";
    }
  };

  public static CsvInterpreters[] getActive() {
    if (SpTool3Main.getRunTime().getConfParams().showAllParamsAsExpert()) {
      return new CsvInterpreters[]{AGILENT, THERMO_XY, ANALYTIK_JENA, CUSTOM_TRA,MZMINE_TRA, CUSTOM_PEAKS,
          LPC};
    } else {
      return new CsvInterpreters[]{AGILENT, THERMO_XY, ANALYTIK_JENA, CUSTOM_TRA};
    }

  }

//  CUSTOM_TRA {
//    @Override
//    public String getUiString() {
//      return "Custom time resolved";
//    }
//
//    @Override
//    public CsvInterpreter getInterpreter() {
//      return new CsvInterpreterCustomTimeResolved();
//    }
//  };

//  ANALYTIK_JENA {
//    @Override
//    public String getUiString() {
//      return "Analytik Jena - PlasmaQuant";
//    }
//
//    @Override
//    public CsvInterpreter getInterpreter() {
//      return new CsvInterpreterAnalytikJena();
//    }
//  },
//
//  AGILENT {
//    @Override
//    public String getUiString() {
//      return "Agilent";
//    }
//
//    @Override
//    public CsvInterpreter getInterpreter() {
//      return new CsvInterpreterAgilent();
//    }
//  };

  public abstract String getUiString();


}
