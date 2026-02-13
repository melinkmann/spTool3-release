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

package math.units;

import util.NF;
import util.SnF;

public class DoubleConvertibleUnit implements NumberConvertibleUnit {

  private final double value;
  private final ConvertibleUnit unit;

  public DoubleConvertibleUnit(double value, ConvertibleUnit unit) {
    this.value = value;
    this.unit = unit;
  }

  @Override
  public double getValue() {
    return value;
  }

  @Override
  public ConvertibleUnit getUnit() {
    return unit;
  }


  @Override
  public String translate(NF nf) {
    return SnF.doubleToString(value, nf) + " " + unit.getLiteralString();
  }

  @Override
  public NumberConvertibleUnit convert(ConvertibleUnit unitAfterConversion) {
    double thisAsRoot = unit.toSIRoot(value);
    double convertedValue = unitAfterConversion.toTarget(thisAsRoot);
    return new DoubleConvertibleUnit(convertedValue, unitAfterConversion);
  }


}
