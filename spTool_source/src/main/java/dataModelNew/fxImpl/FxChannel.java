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

package dataModelNew.fxImpl;

import dataModelNew.mz.Channel;
import dataModelNew.mz.MZChannel;
import gui.listAndSearch.FxWrapper;
import javafx.beans.property.SimpleStringProperty;

import java.util.Objects;

public class FxChannel implements FxWrapper {

  private final Channel channel;

  private final SimpleStringProperty muBGProperty;
  private final SimpleStringProperty nmpCountProperty;
  private final SimpleStringProperty netAreaProperty;
  private final SimpleStringProperty snProperty;


  public FxChannel(Channel channel) {
    this.channel = channel;
    this.muBGProperty = new SimpleStringProperty(" ");
    this.nmpCountProperty = new SimpleStringProperty(" ");
    this.netAreaProperty = new SimpleStringProperty(" ");
    this.snProperty = new SimpleStringProperty(" ");
  }

  public Channel getChannel() {
    return channel;
  }

  public SimpleStringProperty getMuBGProperty() {
    return muBGProperty;
  }

  public SimpleStringProperty getNmpCountProperty() {
    return nmpCountProperty;
  }

  public SimpleStringProperty getNetAreaProperty() {
    return netAreaProperty;
  }

  public SimpleStringProperty getSignalToNoiseProperty() {
    return snProperty;
  }

  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxChannel) {
      isEqual = this.getChannel().equals(((FxChannel) that).getChannel());
    }
    return isEqual;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FxChannel fx = (FxChannel) o;
    return Objects.equals(channel, fx.channel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(channel);
  }
}
