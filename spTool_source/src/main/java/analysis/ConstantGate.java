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

package analysis;

import processing.options.GatingOption;

public class ConstantGate implements GateSupplier {

  private final double gate;
  private final GatingOption option;

  public ConstantGate(double gate, GatingOption option) {
    this.gate = gate;
    this.option = option;
  }

  @Override
  public double get(int i) {
    return gate;
  }

  @Override
  public GatingOption getOption() {
    return option;
  }
}
