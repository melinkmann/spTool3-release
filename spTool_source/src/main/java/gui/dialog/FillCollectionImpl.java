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

package gui.dialog;

import java.io.Serializable;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class FillCollectionImpl<T extends Serializable> implements FillCollection<T> {

  static final Logger LOGGER = LogManager.getLogger(FillCollectionImpl.class.getName());

  private final List<Fillable<T>> items;

  public FillCollectionImpl(List<Fillable<T>> items) {
    this.items = items;
  }

  @Override
  public List<Fillable<T>> getItems() {
    return items;
  }

  @Nullable
  @Override
  public Fillable<T> getMatch(String inputString, boolean muteError) {
    Fillable<T> match = null;
    boolean parserFailed = true;
    for (Fillable<T> item : items) {
      if (inputString.equals(item.getStringValue())) {
        match = item;
        parserFailed = false;
        break;
      }
    }
    if (!muteError && parserFailed) {
      LOGGER.debug("Unable to parse AutoFillable instance for class Element. "
          + "Input string was: '" + inputString + "'.");
    }
    return match;
  }
}