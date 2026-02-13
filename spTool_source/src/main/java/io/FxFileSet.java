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

package io;

import gui.dialog.EditableLabel;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.SimpleEntryFactory;
import gui.dialog.ListContainer;
import gui.dialog.ListableDate;
import gui.dialog.ListableFavourite;
import gui.dialog.mainImpl.ViewListDialog;
import gui.listAndSearch.FxWrapper;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class FxFileSet implements FxWrapper, EditableLabel, ListableDate,
    ListableFavourite, ListContainer<Path> {

  private final FileSet fileSet;

  public FxFileSet(FileSet fileSet) {
    this.fileSet = fileSet;
  }

  public FileSet getPlainFileSet() {
    return fileSet;
  }

  // Returns URI-Directory as Path object is better readable than URI
  @Override
  public List<FxEntry<Path>> getList(FxEntryFactory<Path> factory) {
    return factory.create(fileSet.getFiles().stream()
        .map(Paths::get)
        .collect(Collectors.toList()));
  }

  @Override
  public ViewListDialog<Path> getListDialog() {
    return new ViewListDialog<>(this, new SimpleEntryFactory<>());
  }


  @Override
  public String getLabel() {
    return fileSet.getLabel();
  }

  @Override
  public void setLabel(String label) {
    fileSet.setLabel(label);
  }

  @Override
  public void setFavorite(boolean fav) {
    getPlainFileSet().setFav(fav);
  }

  @Override
  public boolean isFavorite() {
    return getPlainFileSet().isFav();
  }

  @Override
  public Date getDate() {
    return getPlainFileSet().getDate();
  }

  @Override
  public boolean hasDate() {
    return true;
  }

  ////////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxFileSet) {
      isEqual = this.getPlainFileSet().equals(((FxFileSet) that).getPlainFileSet());
    }
    return isEqual;
  }
}
