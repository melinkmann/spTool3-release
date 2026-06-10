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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ComputedChannel implements Channel, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final ChannelCategory category;

  private final String label;
  private final List<Channel> contributingChannels;

  public ComputedChannel() {
    this.category = StaticChannelCategory.UNKNOWN;
    this.label = "Empty";
    this.contributingChannels = new ArrayList<>();
  }

  // Copy
  public ComputedChannel(String label, List<Channel> contributingChannels) {
    this.category = StaticChannelCategory.UNKNOWN;
    this.label = label;
    this.contributingChannels = new ArrayList<>(contributingChannels);
  }

  public ComputedChannel(ChannelCategory category, String label, List<Channel> contributingChannels) {
    this.category = category;
    this.label = label;
    this.contributingChannels = new ArrayList<>(contributingChannels);
  }

  @Override
  public Channel copy() {
    List<Channel> copyContributingChannels = new ArrayList<>();
    for (Channel c : contributingChannels) {
      copyContributingChannels.add(c.copy());
    }
    return new ComputedChannel(category, label, copyContributingChannels);
  }

  @Override
  public String getUIString() {
    return label;
  }

  @Override
  public String getShortUIString() {
    return label;
  }

  @Override
  public ChannelCategory getCategory() {
    return category;
  }


  @Override
  public @Nullable Isotope getIsotope() {
    return null;
  }

  @Override
  public double getMZ() {
    // This confuses b/c the MZ is not defined for a ratio of MZs / isotopes.
//    double mzVal = Double.MAX_VALUE;
//    for (Channel channel : contributingChannels) {
//      double chMZ = channel.getMZ();
//      if (chMZ > 0) {
//        mzVal = Math.min(mzVal, chMZ);
//      }
//    }
//    if (Double.compare(mzVal, Double.MAX_VALUE) == 0) {
//      mzVal = 0;
//    }
//    return mzVal;
    return 0;
  }

  @Override
  public String getMZStr() {
    // This confuses b/c the MZ table would show 107Ag for the sum (which does not make sense)
//    String str = "";
//    double mzVal = Double.MAX_VALUE;
//    for (Channel channel : contributingChannels) {
//      double chMZ = channel.getMZ();
//      if (chMZ > 0) {
//        mzVal = Math.min(mzVal, chMZ);
//        str = channel.getMZStr();
//      }
//    }
//    if (Double.compare(mzVal, Double.MAX_VALUE) == 0) {
//      str = "0";
//    }
//    return str;
    return "0";
  }


  @Override
  public String getColorXmlIDString() {

    String conChList = "";
    for (Channel contributingChannel : contributingChannels) {
      conChList += contributingChannel.getColorXmlIDString();
    }
    String str = category.getUniqueXmlString()
        + Channel.wrap(label)
        + Channel.wrap(conChList);
    str = Channel.wrap(str);
    return str;
  }

  @Override
  public String getColorMatcherString() {
    return label;
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
    ComputedChannel channel = (ComputedChannel) o;
    return category == channel.category
        && Objects.equals(label, channel.label)
        && Objects.equals(contributingChannels, channel.contributingChannels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, label, contributingChannels);
  }

}
