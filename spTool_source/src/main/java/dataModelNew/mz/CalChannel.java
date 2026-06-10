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

import javax.annotation.Nullable;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public record CalChannel(Element element, Channel channel) implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  @Nullable
  public Isotope getIsotope() {
    return channel.getIsotope();
  }

  public int getOrdinalNumber() {
    if (getIsotope() != null) {
      return getIsotope().getIsotopicNumber();
    } else {
      return element.getIsotopes().stream()
          .map(Isotope::getIsotopicNumber)
          .mapToInt(Integer::intValue)
          .max()
          .orElse(element.getAtomicNumber());
    }
  }

  // Only one isotope per instance or if isotope == null (e.g. sum) then just one element!
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    CalChannel other = (CalChannel) obj;
    boolean equal = Objects.equals(getIsotope(), ((CalChannel) obj).getIsotope());
    if (equal) {
      return true;
    } else {
      return Objects.equals(element, other.element);
    }
  }

  @Override
  public int hashCode() {
    if (channel.getIsotope() != null) {
      return Objects.hashCode(channel.getIsotope());
    } else {
      return Objects.hashCode(element);
    }
  }
}