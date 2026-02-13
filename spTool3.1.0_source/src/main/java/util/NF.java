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

package util;


/*
https://jenkov.com/tutorials/java-internationalization/decimalformat.html
0	    A digit - always displayed, even if number has less digits (then 0 is displayed)
#	    A digit, leading zeroes are omitted.
 */


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum NF {

  // Used in the TextFormatter to give some limit to what one can enter. (30 is also chosen in the Regex)
  D30C0TxtFormatter {
    @Override
    public String pattern() {
      return "#############################0";
    }

    @Override
    public String ensureExp() {
      return D30C0TxtFormatter.pattern();
    }
  },

  D6C0 {
    @Override
    public String pattern() {
      return "#####0";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },

  D4C0 {
    @Override
    public String pattern() {
      return "###0";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },

  D3C0 {
    @Override
    public String pattern() {
      return "##0";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },


  D2C0 {
    @Override
    public String pattern() {
      return "#0";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },

  D1C0 {
    @Override
    public String pattern() {
      return "0";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },

  // Here the idea is: always show 1 (0), but if there are more, show more (#):
  D1C1 {
    @Override
    public String pattern() {
      return "0.0";
    }

    @Override
    public String ensureExp() {
      return D1C1Exp.pattern();
    }
  },

  D1C2 {
    @Override
    public String pattern() {
      return "0.0#";
    }

    @Override
    public String ensureExp() {
      return D1C2Exp.pattern();
    }
  },

  D1C3 {
    @Override
    public String pattern() {
      return "0.0##";
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }

  },

  D1C4 {
    @Override
    public String pattern() {
      return "0.0###";
    }

    @Override
    public String ensureExp() {
      return D1C4Exp.pattern();
    }

  },

  D1C5 {
    @Override
    public String pattern() {
      return "0.0####";
    }

    @Override
    public String ensureExp() {
      return D1C6Exp.pattern();
    }
  },

  D1C6 {
    @Override
    public String pattern() {
      return "0.0#####";
    }

    @Override
    public String ensureExp() {
      return D1C6Exp.pattern();
    }

  },

  D1C9 {
    @Override
    public String pattern() {
      return "0.0########";
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }
  },

  D1C12 {
    @Override
    public String pattern() {
      return "0.0###########";
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }
  },

  D1C15 {
    @Override
    public String pattern() {
      return "0.0##############";
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }
  },

  D1C18 {
    @Override
    public String pattern() {
      return "0.0#################"; //n=18
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }

  },


  // Used in the TextFormatter to give some limit to what one can enter. (30 is also chosen in the Regex)
  D1C30TxtFormatter {
    @Override
    public String pattern() {
      return "0.0#############################";
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }
  },


  // Apparently, to get 6 decimals before the E, the ## after the E also count.
  // Thus, 4# + E0## == 1.123456E123
  // Well, note that
  // System.out.println(new DecimalFormat("00.0E0").format(123.253E125)); --> 1.2E127
  D1C1Exp {
    @Override
    public String pattern() {
      return "0.0E0";
    }

    @Override
    public String ensurePlain() {
      return D1C1.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C1Exp.pattern();
    }
  },

  D1C2Exp {
    @Override
    public String pattern() {
      return "0.0#E0";
    }

    @Override
    public String ensurePlain() {
      return D1C2.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C2Exp.pattern();
    }
  },

  D1C3Exp {
    @Override
    public String pattern() {
      return "0.0##E0";
    }

    @Override
    public String ensurePlain() {
      return D1C3.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C3Exp.pattern();
    }
  },

  D1C4Exp {
    @Override
    public String pattern() {
      return "0.0###E0";
    }

    @Override
    public String ensurePlain() {
      return D1C4.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C4Exp.pattern();
    }
  },

  // Apparently, to get 6 decimals before the E, the ## after the E also count.
  // Thus, 4# + E0## == 1.123456E123
  D1C6Exp {
    @Override
    public String pattern() {
      return "0.0#####E0";
    }

    @Override
    public String ensurePlain() {
      return D1C6.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C6Exp.pattern();
    }
  },

  D1C30ExpTxtFormatter {
    @Override
    public String pattern() {
      return "0.0#############################E0";
    }

    @Override
    public String ensurePlain() {
      return D1C30TxtFormatter.pattern();
    }

    @Override
    public String ensureExp() {
      return D1C30ExpTxtFormatter.pattern();
    }
  },

//  #########################################################################

  D6C0Exp {
    @Override
    public String pattern() {
      return "#####0E0";
    }

    @Override
    public String ensurePlain() {
      return D6C0.pattern();
    }

    @Override
    public String ensureExp() {
      return D6C0Exp.pattern();
    }
  },

  D30C0ExpTxtFormatter {
    @Override
    public String pattern() {
      return "#############################0E0";
    }

    @Override
    public String ensurePlain() {
      return D30C0TxtFormatter.pattern();
    }

    @Override
    public String ensureExp() {
      return D30C0ExpTxtFormatter.pattern();
    }
  };

  private static final Logger LOGGER = LogManager.getLogger(NF.class.getName());

  public abstract String pattern();

  // If no scientific is desired, just return standard.
  public String ensurePlain() {
    return pattern();
  }


  public String ensureExp() {
    return pattern();
  }



  // Get the respective maximum digits for display
  public static NF getMax(NF rootNF) {
    switch (rootNF) {

      case D6C0, D4C0, D3C0, D2C0, D1C0, D30C0TxtFormatter -> {
        return NF.D30C0TxtFormatter;
      }

      case D1C1, D1C2, D1C3, D1C4, D1C5, D1C6, D1C9, D1C12, D1C15, D1C18,
          D1C30TxtFormatter -> {
        return NF.D1C30TxtFormatter;
      }
      case D1C2Exp, D1C3Exp, D1C4Exp, D1C6Exp, D1C30ExpTxtFormatter -> {
        return NF.D1C30ExpTxtFormatter;
      }

      // Default could replace all of the standard cases above but lets keep them if sth changes..
      default -> {
        LOGGER.error("Reached default branch where it should not. Likely missed a newly added option.");
        return NF.D1C30TxtFormatter;
      }
    }
  }


}
