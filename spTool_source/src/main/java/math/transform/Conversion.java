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

package math.transform;

/**
 * Transform that is meant for mathematical calculations or enum selections only. It cannot be shown
 * in a popup window. Design philosophy: We cannot really calculate backwards to a parameter when
 * there are 3 or more parameters involved. E.g., when we use density and mass to calculate a
 * diameter, we need to specify WHICH parameter a backwards calculation should return (density or
 * mass?).
 * <p>
 * Edit: In spTool2, the additional parameters were set in a constructor and was hard-initialized
 * with only these values. In praxis, I believe we would rather assign an x value to a sample than
 * having a window with a full, spreadsheet like conversion using all variables.
 */

public interface Conversion<T> {

  T calc(T t);

  T invert(T t);

  String defineCalculationResult();

  String defineInversionResult();

}
