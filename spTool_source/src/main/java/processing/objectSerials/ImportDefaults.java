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

package processing.objectSerials;

import core.SpTool3Main;
import io.FileSet;
import io.VisitedFile;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.SystemUtils;

public class ImportDefaults implements Serializable {

  @Serial
  private static final long serialVersionUID = 2;

  /// //////////////////////////////////////////////////////////////////////////////////////////

  private URI currentDir;
  private List<VisitedFile> previousLocations;

  private boolean limitIsSelected;
  private String limitType;
  private boolean browseSubdirectoryIsSelected;

  private List<FileSet> previousFileSets;

  /// //////////////////////////////////////////////////////////////////////////////////////////

  public ImportDefaults() {
    this.currentDir = Path.of(SystemUtils.getUserHome().getAbsolutePath()).toUri();
    this.previousLocations = new ArrayList<>();
    this.limitIsSelected = false;
    this.limitType = "csv";
    this.browseSubdirectoryIsSelected = false;
    this.previousFileSets = new ArrayList<>();
  }


  // Clean-Up
  public void cleanUp() {
    if (previousLocations.size() > 2000) {
      previousLocations = previousLocations.subList(previousLocations.size() - 1000,
          previousLocations.size());
    }

    if (previousFileSets.size() > 2000) {
      previousFileSets = previousFileSets.subList(previousFileSets.size() - 1000,
          previousFileSets.size());
    }
  }


  public Path getCurrentDir() {
    Path result;
    Path home = Path.of(SystemUtils.getUserHome().getAbsolutePath());
    Path current = Path.of(currentDir);
    if (current.toString().equals(home.toString())) {
      result = SpTool3Main.getRunTime().getConfParams().getDefaultImportPath();
      File resFile = result.toFile();
      if (!resFile.isDirectory() && resFile.exists()) {
        result = current;
      }
    } else {
      result = current;
    }
    return result;
  }

  public List<VisitedFile> getCopyOfPreviousLocations() {
    return previousLocations.stream()
        .map(VisitedFile::new)
        .collect(Collectors.toList());
  }

  public void overridePreviousLocations(List<VisitedFile> previousLocations) {
    this.previousLocations = previousLocations;
  }

  public List<Path> getPreviousLocationsAsPath() {
    List<Path> aSetAsPath = new ArrayList<>();
    previousLocations.forEach(s -> aSetAsPath.add(s.getPath()));
    return aSetAsPath;
  }

  public boolean isLimitIsSelected() {
    return limitIsSelected;
  }

  public String getLimitType() {
    return limitType;
  }

  public boolean isBrowseSubdirectoryIsSelected() {
    return browseSubdirectoryIsSelected;
  }

  public List<FileSet> getCopyOfPreviousFileSets() {
    return previousFileSets.stream()
        .map(FileSet::new)
        .collect(Collectors.toList());
  }

  public void overridePreviousSets(List<FileSet> sets) {
    previousFileSets.clear();
    previousFileSets.addAll(sets);
  }

  /// ////////////////////////////////////////////////////////////////////////////////

  public void setCurrentDir(Path currentDir) {
    this.currentDir = currentDir.toUri();
  }

  public void setLimitIsSelected(boolean limitIsSelected) {
    this.limitIsSelected = limitIsSelected;
  }

  public void setLimitType(String limitType) {
    this.limitType = limitType;
  }

  public void setBrowseSubdirectoryIsSelected(boolean browseSubdirectoryIsSelected) {
    this.browseSubdirectoryIsSelected = browseSubdirectoryIsSelected;
  }

  // Desired Behaviour: if already present, remove old instance with old date and put new "at top".
  public void addPreviousLocation(Path path) {
    VisitedFile newInstance = new VisitedFile(path);
    VisitedFile alreadyPresent = null;
    for (VisitedFile previousLocation : previousLocations) {
      if (previousLocation.getPath().equals(path)) {
        alreadyPresent = previousLocation;
      }
    }
    if (alreadyPresent != null) {
      previousLocations.remove(alreadyPresent);
      // Remember if is favourite
      newInstance.setFav(alreadyPresent.isFav());
    }
    previousLocations.add(newInstance);
  }

  public void remPreviousLocationPath(List<Path> paths) {
    for (Path path : paths) {
      VisitedFile alreadyPresent = null;
      for (VisitedFile previousLocation : previousLocations) {
        if (previousLocation.getPath().equals(path)) {
          alreadyPresent = previousLocation;
        }
      }
      if (alreadyPresent != null) {
        previousLocations.remove(alreadyPresent);
      }
    }
  }

  public void remPreviousLocationVisitedFile(List<VisitedFile> paths) {
    previousLocations.removeAll(paths);
  }

  public void remPreviousSet(List<FileSet> sets) {
    previousFileSets.removeAll(sets);
  }

  public void addPreviousFileSet(FileSet set) {
    previousFileSets.add(set);
  }


}
