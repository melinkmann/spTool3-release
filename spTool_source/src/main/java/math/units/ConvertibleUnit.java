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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import math.units.enums.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;


public interface ConvertibleUnit extends Serializable, Unit {

  static final Logger LOGGER = LogManager.getLogger(ConvertibleUnit.class);

  double toSIRoot(double value);

  double toTarget(double siUnitValue);

  boolean isCompatible(ConvertibleUnit unit);

  public default Double convert(double value, Unit target) {
    Double result = value;
    if (target instanceof ConvertibleUnit) {
      ConvertibleUnit convertibleTarget = (ConvertibleUnit) target;
      if (isCompatible(convertibleTarget)) {
        double root = toSIRoot(value);
        result = convertibleTarget.toTarget(root);
      } else {
        LOGGER.error("API error: unit is not compatible for conversion!");
      }
    } else {
      LOGGER.error("API error: unit cannot be converted at all!");
    }
    return result;
  }

  public static List<ConvertibleUnit> listAll() {
    List<ConvertibleUnit> all = new ArrayList<>();
    all.addAll(Arrays.asList(MassUnit.values()));
    all.addAll(Arrays.asList(SizeUnit.values()));
    all.addAll(Arrays.asList(QuantityUnit.values()));
    all.addAll(Arrays.asList(TimeUnit.values()));
    all.addAll(Arrays.asList(ConcentrationUnit.values()));
    all.addAll(Arrays.asList(SensitivityUnit.values()));
    all.addAll(Arrays.asList(DensityUnit.values()));
    return all;
  }


}
