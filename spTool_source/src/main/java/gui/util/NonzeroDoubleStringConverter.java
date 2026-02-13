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

package gui.util;

import javafx.util.StringConverter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.NF;
import util.SnF;

public class NonzeroDoubleStringConverter extends StringConverter<Double> {

  private static final Logger LOGGER = LogManager.getLogger(NonzeroDoubleStringConverter.class.getName());

  private final NF nf;

  public NonzeroDoubleStringConverter() {
    this.nf = NF.D1C30TxtFormatter;
  }

  public NonzeroDoubleStringConverter(NF nf) {
    this.nf = nf;
  }

  public Double fromString(String var1) {
    Double result;
    boolean wasZero = false;
    if (var1 == null) {
      result= null;
    } else {
      var1 = var1.trim();
      if (var1.length() < 1) {
        result= null;
      } else {
        if (Double.parseDouble(var1) == 0) {
          wasZero = true;
          result= 1d;
        } else {
          result= Double.valueOf(var1);
        }

      }

    }
    if (wasZero){
      LOGGER.debug("Value cannot be zero. Continued with value =" + result + " instead.");
      // This gets stuck in inf loop as the UI never gets updated with the popup open
      // DialogFactory.openOK("Value cannot be zero.");
    }
    return result;
  }

  @Override
  public String toString(Double var1) {
    if (var1 == null) {
      return "";
    } else {
      String returnVal = SnF.doubleToString(var1, nf);
      return returnVal;
    }
  }
}
