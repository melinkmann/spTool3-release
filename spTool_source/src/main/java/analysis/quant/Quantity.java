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

package analysis.quant;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;

import io.impl.CsvInterpreterAgilent;
import math.units.ConvertibleUnit;
import math.units.enums.SensitivityUnit;
import math.units.enums.ViewUnits;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Quantity implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;
  private static final Logger LOGGER = LogManager.getLogger(Quantity.class);

  private ConvertibleUnit unit;
  private double value;
  private boolean isDummy;

  public Quantity() {
    this.unit = SensitivityUnit.CTS_PER_MICROGRAM;
    this.value = 0;
    this.isDummy = true;
  }

  public Quantity(double value, ConvertibleUnit unit) {
    this.unit = unit;
    this.value = value;
    this.isDummy = false;
  }


  // copy
  public Quantity(Quantity quantity) {
    this.unit = quantity.unit;
    this.value = quantity.value;
    this.isDummy = quantity.isDummy;
  }

  public Quantity copy() {
    return new Quantity(this);
  }

  public void change(ConvertibleUnit newUnit) {
    double rootValue = unit.toSIRoot(value);
    double newValue = newUnit.toTarget(rootValue);
    this.unit = newUnit;
    this.value = newValue;
  }

  public void change(double newValue, ConvertibleUnit unitOfNewValue) {
    double rootValue = unitOfNewValue.toSIRoot(newValue);
    double newValueWithUnit = unit.toTarget(rootValue);
    this.value = newValueWithUnit;
  }


  public void change(double newValue) {
    this.value = newValue;
  }

  public ConvertibleUnit getUnit() {
    return unit;
  }

  public double getValue() {
    return value;
  }


  private void readObject(ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    try {
      ObjectInputStream.GetField fields = in.readFields();

      this.value = fields.get("value", 0.0);

      Object rawUnit = fields.get("unit", null);
      if (rawUnit instanceof ConvertibleUnit) {
        this.unit = (ConvertibleUnit) rawUnit;
      } else {
        this.unit = SensitivityUnit.CTS_PER_MICROGRAM;
        LOGGER.trace("Incompatible unit during loading (deserialization): {}. " +
            "Instead, continued with: {}.", rawUnit, unit);
      }

      this.isDummy = fields.get("isDummy", false);

    } catch (Exception e) {
      LOGGER.error("Failed to load (deserialize) Quantity", e);

      // safe fallback object state
      this.value = 0.0;
      this.unit = SensitivityUnit.CTS_PER_MICROGRAM;
      this.isDummy = true;
    }
  }

}
