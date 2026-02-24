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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import math.units.ConvertibleUnit;

public class FxQuantity {

  private final Quantity plainQuantity;
  private final DoubleProperty valueProperty;
  private final ObjectProperty<ConvertibleUnit> unitProperty;

  public FxQuantity(Quantity quantity) {
    this.plainQuantity =quantity;

    this.valueProperty = new SimpleDoubleProperty(quantity.getValue());
    this.unitProperty = new SimpleObjectProperty<>(quantity.getUnit());

    // update plain instance
    valueProperty.addListener(new ChangeListener<Number>() {
      @Override
      public void changed(ObservableValue<? extends Number> observable, Number oldValue,
          Number newValue) {
        if (newValue != null && newValue != oldValue){
          quantity.change(newValue.doubleValue());
        }
      }
    });

    unitProperty.addListener(new ChangeListener<ConvertibleUnit>() {
      @Override
      public void changed(ObservableValue<? extends ConvertibleUnit> observable,
          ConvertibleUnit oldValue, ConvertibleUnit newValue) {
        if (newValue != null && newValue != oldValue){
          quantity.change(newValue);
          // when changing the unit, the numeric value also changes
          valueProperty.set(quantity.getValue());
        }
      }
    });
  }

  public Quantity getPlainQuantity() {
    return plainQuantity;
  }

  public DoubleProperty getValueProperty() {
    return valueProperty;
  }

  public ObjectProperty<ConvertibleUnit>  getUnitProperty() {
    return unitProperty;
  }
}


