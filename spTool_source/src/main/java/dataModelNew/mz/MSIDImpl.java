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

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MSIDImpl implements MSID, Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final List<MZ> mzList;

  //Dummy
  public MSIDImpl() {
    this.mzList = new ArrayList<>();
  }

  public MSIDImpl(MZ q1mz) {
    this.mzList = new ArrayList<>();
    mzList.add(q1mz);
  }

  public MSIDImpl(MZ q1mz, MZ q3mz) {
    this.mzList = new ArrayList<>();
    mzList.add(q1mz);
    mzList.add(q3mz);
  }

  // Copy constructor (and potentially if list is provided)
  public MSIDImpl(List<MZ> mzList) {
    this.mzList = new ArrayList<>(mzList);
  }

  public MZ getMainMZ() {
    MZ mz = new MZImpl();
    if (!mzList.isEmpty()) {
      mz = mzList.get(0);
    }
    return mz;
  }

  @Override
  public MSID copy() {
    // Copy here!
    List<MZ> mzListCopy = new ArrayList<>();
    for (MZ mz : mzList) {
      mzListCopy.add(mz.copy());
    }
    return new MSIDImpl(mzListCopy);
  }

  @Override
  public ChannelCategory getCategory() {
    return new DefaultChannelCategory(getUIString(), getUniqueXmlString());
  }

  @Override
  public String getUIString() {
    String str = "N/A";
    if (mzList.size() == 1) {
      str = mzList.get(0).getUIString();
    } else if (!mzList.isEmpty()) {
      str = mzList.stream()
          .map(MZ::getUIString)
          .collect(Collectors.joining("-"));
    }
    return str;
  }

  @Override
  public String getUniqueXmlString() {

    String str = mzList.stream()
        .map(MZ::getUniqueXmlString)
        .collect(Collectors.joining(""));

    return Channel.wrap(str);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MSIDImpl ms = (MSIDImpl) o;
    return Objects.equals(ms.mzList, mzList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mzList);
  }
}
