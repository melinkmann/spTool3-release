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

import java.io.IOException;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;


public class FileSet implements Serializable {

  @Serial
  private static final long serialVersionUID = 3L;

  private String label;
  private final Date date;
  private boolean isFav = false;
  private final List<URI> files;


  public FileSet(Date date, List<Path> files) {
    this.date = date;
    this.files = files.stream().map(Path::toUri).collect(Collectors.toList());
    this.label = date.toString();
  }

  public FileSet(List<Path> files) {
    this.date = new Date();
    this.files = files.stream().map(Path::toUri).collect(Collectors.toList());
    this.label = "Recently imported files";
  }

  public FileSet(FileSet set) {
    this.date = set.getDate();
    this.files = set.getFiles();
    this.label = set.label;
    this.isFav = set.isFav();
  }

  public List<URI> getFiles() {
    return files;
  }

  public FxFileSet getObservableInstance() {
    return new FxFileSet(this);
  }


  public Date getDate() {
    return date;
  }

  public void setFav(boolean fav) {
    isFav = fav;
  }

  public boolean isFav() {
    return isFav;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getExampleFolder() {
    String folder = "";
    if (!files.isEmpty()) {
      Path firstHit = Path.of(files.get(0));
      folder = PathUtil.getParentFolderNameFromFile(firstHit);
    }
    return folder;
  }

  public String getExampleFile() {
    String name = "";
    if (!files.isEmpty()) {
      Path firstHit = Path.of(files.get(0));
      name = PathUtil.getFileNameWithExtension(firstHit);
    }
    return name;
  }


  // THIS IS HOW TO INSTANTIATE A TRANSIENT FIELD or ONE THAT REQUIRES SPECIAL CONSTRUCTOR like here
  // Note: Commented since the Property was moved to the FX class.
  @Serial
  private void readObject(@Nonnull java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    // this.cellLabel = new SerialStringProperty(label);
  }

}
