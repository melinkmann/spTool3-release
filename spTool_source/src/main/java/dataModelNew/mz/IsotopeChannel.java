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

package dataModelNew.mz;

import sandbox.montecarlo.Isotope;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class IsotopeChannel implements Channel, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final Isotope isotope;

  public IsotopeChannel(Isotope isotope) {
    this.isotope = isotope;
  }

  public Isotope getIsotope() {
    return isotope;
  }

  @Override
  public double getMZ() {
    return isotope.getIsotopicNumber();
  }

  @Override
  public String getMZStr() {
    return String.valueOf(isotope.getIsotopicNumber());
  }

  @Override
  public String getShortUIString() {
    return isotope.getNumberAndElement();
  }

  // 197Au - note that Isotope::getFullUIName -> "Iron: 56Fe"
  @Override
  public String getUIString() {
    return isotope.getNumberAndElement();
  }

  @Override
  public ChannelCategory getCategory() {
    return isotope.getElement();
  }

  @Override
  public String getColorXmlIDString() {
    return Channel.wrap(isotope.getXMLCode());
  }

  @Override
  public String getColorMatcherString() {
    return isotope.getFullUIName();
  }

  @Override
  public boolean isColorIDString(String str) {
    return Objects.equals(str, this.getColorXmlIDString());
  }

  @Override
  public Channel copy() {
    return new IsotopeChannel(isotope);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IsotopeChannel channel = (IsotopeChannel) o;
    return Objects.equals(isotope, channel.isotope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(isotope);
  }
}
