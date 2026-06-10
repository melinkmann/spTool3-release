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

import org.jetbrains.annotations.Nullable;
import sandbox.montecarlo.Isotope;

import javax.annotation.Nonnull;
import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

public class MZChannel implements Channel, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private ChannelCategory category;

  private final MSID msId;

  // may be assigned if appropriate
  @Nullable
  private final Isotope isotope;

  // Dummy
  public MZChannel() {
    this.category = StaticChannelCategory.UNKNOWN;
    this.msId = new MSIDImpl();
    this.isotope = null;
  }


  //TODO: Check if we convert IsotopeChannels to MZChannels at some point implicitly in the processing
  // pipeline. If yes, we need instance checking and maintain class type (?)
  public MZChannel(MSID msId, @Nonnull Isotope isotope) {
    this.category = isotope.getElement();
    this.msId = msId;
    this.isotope = isotope;
  }

  // We have just an MZ and we cannot assign elements, e.g., because molecular MS data
  public MZChannel(MSID msId) {
    this.category = msId.getCategory();
    this.msId = msId;
    this.isotope = null;
  }

  // Copy
  public MZChannel(ChannelCategory category, MSID msId, @Nullable Isotope isotope) {
    this.category = category;
    this.msId = msId;
    this.isotope = isotope;
  }

  @Override
  public Channel copy() {
    return new MZChannel(category, msId.copy(), isotope);
  }

  @Override
  public double getMZ() {
    double mz = msId.getMainMZ().getMz();
    return mz;
  }

  @Override
  public String getMZStr() {
    return msId.getUIString();
  }

  @Override
  public String getShortUIString() {
    if (isotope != null) {
      return isotope.getNumberAndElement();
    } else {
      return getMZStr();
    }
  }

  @Override
  public @Nullable Isotope getIsotope() {
    return isotope;
  }

  @Override
  public String getUIString() {
    String mzStr = getMZStr();
    if (isotope != null && !Element.UNKNOWN.getIsotopes().get(0).equals(isotope)) {
      mzStr = isotope.getNumberAndElement() + "(" + mzStr + ")";
    }
    return mzStr;
  }

  @Override
  public ChannelCategory getCategory() {
    return category;
  }


  @Override
  public String getColorXmlIDString() {
    String str;
    if (isotope != null) {
      str = Channel.wrap(isotope.getXMLCode());
    } else {
      str = category.getUniqueXmlString()
          + msId.getUniqueXmlString();
      str = Channel.wrap(str);
    }
    return str;
  }

  @Override
  public String getColorMatcherString() {
    String str;
    if (isotope != null) {
      str = isotope.getFullUIName();
    } else {
      str = msId.getUIString();
    }
    return str;
  }

  @Override
  public boolean isColorIDString(String str) {
    return Objects.equals(str, this.getColorXmlIDString());
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MZChannel channel = (MZChannel) o;
    return Objects.equals(category, channel.category)
        && Objects.equals(msId, channel.msId)
        // true if both null:
        && Objects.equals(isotope, channel.isotope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, msId, isotope);
  }


}
