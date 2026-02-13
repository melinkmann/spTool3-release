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

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;
import java.util.Random;
import org.apache.commons.math3.random.RandomGenerator;

public class XoRoShiRo128PlusRandomCustom implements RandomGenerator {

  Random xoroRandom = new XoRoShiRo128PlusRandom();

  @Override
  public void setSeed(int seed) {
    xoroRandom.setSeed(seed);
  }

  @Override
  public void setSeed(int[] seed) {
    // Taken from Mersenne Twister. Essentially,
    setSeed(System.currentTimeMillis() + System.identityHashCode(this));
  }

  @Override
  public void setSeed(long seed) {
    xoroRandom.setSeed(seed);
  }

  @Override
  public void nextBytes(byte[] bytes) {
    xoroRandom.nextBytes(bytes);
  }

  @Override
  public int nextInt() {
    return xoroRandom.nextInt();
  }

  @Override
  public int nextInt(int n) {
    return xoroRandom.nextInt(n);
  }

  @Override
  public long nextLong() {
    return xoroRandom.nextLong();
  }

  @Override
  public boolean nextBoolean() {
    return xoroRandom.nextBoolean();
  }

  @Override
  public float nextFloat() {
    return xoroRandom.nextFloat();
  }

  @Override
  public double nextDouble() {
    return nextDouble();
  }

  @Override
  public double nextGaussian() {
    return xoroRandom.nextGaussian();
  }

}
