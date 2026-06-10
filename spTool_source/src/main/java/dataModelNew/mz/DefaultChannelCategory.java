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
import java.util.Objects;

public class DefaultChannelCategory implements Serializable, ChannelCategory {
  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private final String uiStr;
  private final String xmlID;

  public DefaultChannelCategory(String uiStr, String xmlID) {
    this.uiStr = uiStr;
    this.xmlID = xmlID;
  }

  @Override
  public String getUIString() {
    return uiStr;
  }

  @Override
  public String getUniqueXmlString() {
    return Channel.wrap(xmlID);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uiStr, xmlID);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultChannelCategory cat = (DefaultChannelCategory) o;
    return Objects.equals(cat.uiStr, uiStr)
        && Objects.equals(cat.xmlID, xmlID);
  }
}
