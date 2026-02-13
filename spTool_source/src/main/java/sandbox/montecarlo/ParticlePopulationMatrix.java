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

import dataModelNew.mz.Element;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import visualizer.styles.Colors;
import visualizer.styles.MarkerStyle;

public interface ParticlePopulationMatrix extends Serializable {

  ParticlePopulationMatrix copy();

  String getLabel();

  public Colors getColor();

  public MarkerStyle getMarker();

  int getNumberOfEvents();

  double[] getPlasmaVelocities();

  double[] getYPositions();

  HashMap<Element, double[]> getArrivalTimeMap();

  HashMap<Element, double[]> getPlasmaDiffusionDMap();

  HashMap<Isotope, double[]> getIntensityMap();

  List<Element> listElements();

  List<Isotope> listIsotopes();

  boolean hasIsotope(Isotope isotope);

  boolean hasElement(Element element);

  ParticlePopulationMatrixHDD getNewHddInstance();

  ParticlePopulationMatrixRAM getNewRamInstance();

}

