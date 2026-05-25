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

package dataModelNew;

import math.units.ConvertibleUnit;
import math.units.enums.SensitivityUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SampleFile implements Serializable {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private static final Logger LOGGER = LogManager.getLogger(SampleFile.class);

  private URI filePath;
  private final String fileName;
  private final String nameWithinFile;

  private InstrumentID instrumentID = InstrumentID.UNKNOWN;

  public SampleFile(Path filePath, String nameWithinFile) {
    this.filePath = filePath.toUri();
    this.fileName = filePath.getFileName().toString();
    this.nameWithinFile = nameWithinFile;
  }

  public SampleFile(Path filePath, InstrumentID instrumentID) {
    this.filePath = filePath.toUri();
    this.fileName = filePath.getFileName().toString();
    this.nameWithinFile = filePath.getFileName().toString();
    this.instrumentID = instrumentID;
  }

  public SampleFile(String nameWithinFile) {
    this.filePath = URI.create("");
    this.fileName = "N/A";
    this.nameWithinFile = nameWithinFile;
  }

  public SampleFile() {
    this.filePath = URI.create("");
    this.fileName = "N/A";
    this.nameWithinFile = "N/A";
  }

  // Copy
  public SampleFile(SampleFile sampleFile) {
    this.filePath = URI.create(sampleFile.filePath.toString());
    this.fileName = new String(sampleFile.fileName);
    this.nameWithinFile = new String(sampleFile.nameWithinFile);
  }

  public boolean hasFile() {
    return !filePath.toString().equals("N/A");
  }

  public String getFileName() {
    return fileName;
  }

  public String getNameWithinFile() {
    return nameWithinFile;
  }

  public Path getFilePath() {
    Path path = Path.of("");
    if (filePath == null) return path;
    try {
      return Path.of(filePath);
    } catch (IllegalArgumentException e) {
      return path;
    }
  }

  public String getFolder() {
    String folder = "N/A";  // default value

    if (filePath != null) {
      try {
        Path path = Paths.get(filePath);

        if (Files.isRegularFile(path)) {
          Path parent = path.getParent();
          if (parent != null) {
            folder = parent.getFileName().toString();
          }
        } else {
          Path folderPath = path.getFileName();
          if (folderPath != null) {
            folder = folderPath.toString();
          }
        }
      } catch (Exception e) {
        // keep folder as "N/A"
      }
    }

    return folder;
  }

  public String getPath() {
    String pathStr = "N/A";  // default value

    if (filePath != null) {
      try {
        Path path = Paths.get(filePath);
        pathStr = path.toString();

        // This prevents file name from showing in UI if file is not on sys anymore
        //        if (Files.isRegularFile(path)) {
        //          pathStr = path.toString();
        //        }
      } catch (Exception e) {
        // keep folder as "N/A"
      }
    }

    return pathStr;
  }

  public URI getUriFile() {
    return filePath;
  }

  public void setUriFile(URI filePath) {
    this.filePath = filePath;
    LOGGER.info("Updated sample path: {}", filePath);
  }

  public void setInstrumentID(InstrumentID instrumentID) {
    this.instrumentID = instrumentID;
  }

  public InstrumentID getInstrumentID() {
    return instrumentID;
  }

  @Serial
  private void readObject(@Nonnull java.io.ObjectInputStream in)
      throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (this.instrumentID == null) {
      this.instrumentID = InstrumentID.UNKNOWN;
    }
  }
}