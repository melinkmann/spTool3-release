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

import io.GlobalIO;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SettingsSerializer {

  private static final Logger LOGGER = LogManager.getLogger(SettingsSerializer.class);

  public static ImportDefaults loadImportDefaults() {
    ImportDefaults id = new ImportDefaults();
    Path file = GlobalIO.makeImportWindow();

    Object loaded = load(file);
    if (loaded instanceof ImportDefaults) {
      id = (ImportDefaults) loaded;
    }

    id.cleanUp();
    return id;
  }


  @Nullable
  public static Object load(Path file) {
    Object o = null;
    if (Files.isReadable(file)) {
      try {
        if (Files.size(file) > GlobalIO.MIN_XML_SIZE_BYTES) {
          LOGGER.info("Reading from " + file);
          try {
            FileInputStream fileInputStream = new FileInputStream(file.toFile());
            ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
            o = objectInputStream.readObject();
            objectInputStream.close();
            fileInputStream.close();
          } catch (Exception e) {
            LOGGER.error(ExceptionUtils.getStackTrace(e));
          }
        }
      } catch (IOException ioException) {
        LOGGER.error(ExceptionUtils.getStackTrace(ioException));
      }
    }
    return o;
  }

  public static void write(Path file, Object o) {
    if (Files.isReadable(file)) {
      LOGGER.debug("Try to write to " + file);
    }
    try {
      FileOutputStream fileOutputStream = new FileOutputStream(file.toFile());
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(o);
      objectOutputStream.close();
      fileOutputStream.close();

      LOGGER.info("Wrote object of type " + o.getClass() + " to: " + file);

    } catch (
        Exception e) {
      LOGGER.error(ExceptionUtils.getStackTrace(e));
    }
  }


}
