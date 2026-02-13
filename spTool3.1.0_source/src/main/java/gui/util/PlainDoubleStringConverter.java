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
import util.NF;
import util.SnF;

public class PlainDoubleStringConverter extends StringConverter<Double> {

  private final NF nf;

  public PlainDoubleStringConverter() {
    this.nf = NF.D1C30TxtFormatter;
  }

  public PlainDoubleStringConverter(NF nf) {
    this.nf = nf;
  }

  public Double fromString(String var1) {
    if (var1 == null) {
      return null;
    } else {
      var1 = var1.trim();
      return var1.length() < 1 ? null : Double.valueOf(var1);
    }
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
