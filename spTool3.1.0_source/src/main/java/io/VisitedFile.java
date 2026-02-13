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

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.Date;

public class VisitedFile implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private final Date date;
  private boolean isFav;
  private final URI location;

  public VisitedFile(Date date, Path location) {
    this.date = date;
    this.location = location.toUri();
  }

  public VisitedFile(Path location) {
    this.date = new Date();
    this.location = location.toUri();
  }

  public VisitedFile(VisitedFile file) {
    this.date = file.getDate();
    this.location = file.getPath().toUri();
  }


  public Path getPath() {
    return Path.of(location);
  }

  public FxVisitedFile getObservableInstance() {
    return new FxVisitedFile(this);
  }

  public Date getDate() {
    return date;
  }

  public boolean isFav() {
    return isFav;
  }

  public void setFav(boolean fav) {
    isFav = fav;
  }
}