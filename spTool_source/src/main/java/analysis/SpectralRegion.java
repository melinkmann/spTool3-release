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

import sandbox.montecarlo.Isotope;

import java.util.ArrayList;
import java.util.List;

public class SpectralRegion {

  final List<Isotope> isotopes;
  final List<String> names;
  final double[] mzs;
  final double[][] intensities;

  public SpectralRegion(List<SpectralArray> spectralArrays) {

    int mzCount = spectralArrays.size();
    int regionsCount = mzCount == 0 ? 0 : spectralArrays.get(0).getIntensity().length;

    isotopes = new ArrayList<>(mzCount);
    names = new ArrayList<>(mzCount);
    mzs = new double[mzCount];

    intensities = new double[regionsCount][mzCount];

    for (int i = 0; i < mzCount; i++) {

      SpectralArray sa = spectralArrays.get(i);

      isotopes.add(sa.getIsotope());
      names.add(sa.getName());
      mzs[i] = sa.getMz();

      double[] src = sa.getIntensity();

      for (int k = 0; k < regionsCount; k++) {
        intensities[k][i] = src[k];
      }
    }
  }
}