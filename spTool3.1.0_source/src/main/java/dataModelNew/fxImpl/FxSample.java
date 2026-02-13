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

import dataModelNew.Sample;
import gui.dialog.EditableComment;
import gui.dialog.EditableLabel;
import gui.dialog.ListableFavourite;
import gui.listAndSearch.FxWrapper;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.NF;
import util.SnF;

public class FxSample implements FxWrapper, EditableLabel, EditableComment, ListableFavourite {

  private final Sample sample;
  private final SimpleStringProperty nickNameProperty;
  private final SimpleStringProperty commentProperty;
  private final BooleanProperty highlightedProperty;
  private final SimpleStringProperty nameProperty;
  private final SimpleStringProperty driftFactorProperty;
  private final SimpleStringProperty eventCountProperty;
  private final SimpleStringProperty pathProperty;


  public FxSample(Sample sample) {
    this.sample = sample;
    this.nickNameProperty = new SimpleStringProperty(sample.getNickName());
    this.commentProperty = new SimpleStringProperty(sample.getComment());
    this.highlightedProperty = new SimpleBooleanProperty(sample.isHighlight());
    this.pathProperty = new SimpleStringProperty(sample.getSampleFile().getPath());
    this.nameProperty = new SimpleStringProperty(sample.getSampleFile().getNameWithinFile());
    this.driftFactorProperty = new SimpleStringProperty(" ");
    this.eventCountProperty = new SimpleStringProperty(" ");

    // make sure changes are sent to the plain sample
    nickNameProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue != null && !newValue.isEmpty()) {
        sample.setNickName(newValue);
      }
    });
    commentProperty.addListener((observable, oldValue, newValue) -> {
      if (newValue != null && !newValue.isEmpty()) {
        sample.setComment(newValue);
      }
    });
    highlightedProperty.addListener((observable, oldValue, newValue) -> {
      sample.setHighlight(newValue);
    });


  }

  public Sample getPlainSample() {
    return sample;
  }

  public StringProperty getNickNameProperty() {
    return nickNameProperty;
  }

  public StringProperty getCommentProperty() {
    return commentProperty;
  }

  public StringProperty getPathProperty() {
    return pathProperty;
  }

  public BooleanProperty highlightedProperty() {
    return highlightedProperty;
  }

  public StringProperty getSampleNameProperty() {
    return nameProperty;
  }

  public StringProperty getDriftFactorProperty() {
    return driftFactorProperty;
  }

  public void setDriftFactor(double drift) {
    if (drift > 10) {
      driftFactorProperty.setValue(SnF.doubleToString(drift, NF.D1C0));
    } else {
      driftFactorProperty.setValue(SnF.doubleToString(drift, NF.D1C1));
    }
  }

  public StringProperty getEventCountProperty() {
    return eventCountProperty;
  }

  public void setEventCount(int count) {
    eventCountProperty.setValue(Integer.toString(count));
  }


  @Override
  public String getLabel() {
    return sample.getNickName();
  }

  @Override
  public void setLabel(String nickName) {
    // This will trigger the listener of the property.
    nickNameProperty.set(nickName);
  }


  @Override
  public String getComment() {
    return sample.getComment();
  }


  @Override
  public void setComment(String comment) {
    // This will trigger the listener of the property.
    commentProperty.set(comment);
  }

  @Override
  public boolean isFavorite() {
    return sample.isHighlight();
  }

  @Override
  public void setFavorite(boolean isFavourite) {
    // This will trigger the listener of the property which sets fav=true in the sample itself.
    highlightedProperty.set(isFavourite);
  }

  ////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxSample) {
      isEqual = this.getPlainSample().equals(((FxSample) that).getPlainSample());
    }
    return isEqual;
  }


}
