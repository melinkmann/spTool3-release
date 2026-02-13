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

package sandbox.montecarlo;

import org.apache.commons.math3.distribution.ParetoDistribution;
import org.apache.commons.math3.exception.NotStrictlyPositiveException;
import org.apache.commons.math3.util.FastMath;

public class ParetoDistribution2 extends ParetoDistribution {

  /**
   * The scale parameter of this distribution.
   */
  private final double scale;

  /**
   * The shape parameter of this distribution.
   */
  private final double shape;

  public ParetoDistribution2(double scale, double shape)
      throws NotStrictlyPositiveException {
    super(scale, shape);
    this.scale = scale;
    this.shape = shape;
  }

  @Override
  public double sample() {
    final double n = random.nextDouble();
    // Uniform (0,1] excluding 0 (!). Otherwise, dividing by zero is bad.
    // Note that we are sampling random numbers and for the outcome it does not matter if we use u or 1-u.
    return scale / FastMath.pow((1 - n), 1 / shape);
  }

}
