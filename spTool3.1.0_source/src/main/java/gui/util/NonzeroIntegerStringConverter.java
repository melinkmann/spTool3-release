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

public class NonzeroIntegerStringConverter extends StringConverter<Integer> {

  private static final Logger LOGGER = LogManager
      .getLogger(NonzeroIntegerStringConverter.class.getName());

  private final NF nf;

  public NonzeroIntegerStringConverter() {
    this.nf = NF.D30C0TxtFormatter;
  }

  public NonzeroIntegerStringConverter(NF nf) {
    this.nf = nf;
  }

  public Integer fromString(String var1) {
    Integer result;
    if (var1 == null) {
      result = null;
    } else {
      var1 = var1.trim();
      if (var1.length() < 1) {
        result = null;
      } else {
        result = Integer.parseInt(var1);
        if (result == 0) {
          result = 1;
          LOGGER.debug("Value cannot be zero. Continued with value=" + result + " instead.");
          // This gets stuck in inf loop as the UI never gets updated with the popup open!!!
          // DialogFactory.openOK("Value cannot be zero.");
        }
      }
    }

    //  System.out.println("Int converter=" + result + ".");
    return result;
  }

  @Override
  public String toString(Integer var1) {
    // If the specified value is null, return a zero-length String
    if (var1 == null) {
      return "";
    } else {
      String returnVal = SnF.intToString(var1, nf);
      // System.out.println("Int String converter toString=" + var1);
      return returnVal;
    }
  }
}
