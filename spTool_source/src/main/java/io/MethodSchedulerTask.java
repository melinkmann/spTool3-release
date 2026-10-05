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

import com.google.common.util.concurrent.AtomicDouble;
import core.SpTool3Main;
import javafx.application.Platform;
import javafx.scene.control.ToggleButton;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.Method;
import processing.parameterSets.action.Actions;
import tasks.BatchTask;
import tasks.TaskResult;
import tasks.results.EmptyTaskResult;
import util.Util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MethodSchedulerTask {

  private static final Logger LOGGER = LogManager.getLogger(MethodSchedulerTask.class);

  private final AtomicReference<Path> currentPath = new AtomicReference<>(null);
  private final AtomicBoolean isActive = new AtomicBoolean(false);

  private ToggleButton masterButton;

  public synchronized void arm(Path path, ToggleButton button) {

    this.masterButton = button;

    // read this outside of the multithreading!
    int nRep = SpTool3Main.getRunTime().getMainWindowCtl().repetitionSpinner.getValue();

    if (!isActive.get()) {
      isActive.set(true);
      currentPath.set(path);

      BatchTask watchTask = new BatchTask() {
        @Override
        public String getTaskName() {
          return "Method folder watch";
        }

        @Override
        public AtomicDouble getProgress() {
          return new AtomicDouble(1);
        }

        @Override
        public void stop() {
          // call this before the isActive boolean == false kills the while loop
          Platform.runLater(()->{
            if (masterButton != null) {
              masterButton.setSelected(false);
            }
          });
          isActive.set(false);
        }

        @Override
        public TaskResult call() throws Exception {

          Path dir = currentPath.get();

          // Prepare:
          // "Take the list of files, move it to a subdirectory "finished", if "finished" does not exist,
          // create it"
          Path finished = dir.resolve("scheduled");
          Files.createDirectories(finished); // no-op if it already exists

          while (isActive.get()) {

            // Sleep for 1s
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt(); // preserve interrupt flag
              Platform.runLater(()->{
                if (masterButton != null) {
                  isActive.set(false);
                  masterButton.setSelected(false);
                }
              });
              return new EmptyTaskResult();
            }

            // Check if isActive, else finish
            if (!isActive.get()) {
              break;
            }

            // Check if currentPath.get() is not null and isDirectory and exists and can read, else finish
            if (dir == null || dir.toString().isEmpty() || !Files.exists(dir) || !Files.isDirectory(dir) || !Files.isReadable(dir)) {
              break;
            }

            // List all files in the directory
            List<Path> methodPaths = new ArrayList<>(PathUtil.listFiles(dir, false));

            for (Path methodPath : methodPaths) {
              if (Files.isRegularFile(methodPath) && Files.isReadable(methodPath)
                  && PathUtil.getExtensionWithDot(methodPath).equals(GlobalIO.METHOD_EXTENSION)) {

                // read from xml also checks for ".spm"
                Method method = XmlUtil.readMethodFromXml(methodPath);

                if (method != null && !method.getSets().isEmpty()) {
                  Actions.executeMethodCreateBtn(method, nRep);

                  // Take the list of files, move it to a subdirectory "finished", if "finished" does not
                  // exist,
                  // create it

                  if (!isActive.get()) {
                    break; // stop() was called mid-way
                  }
                  String dateTag = Util.getYearMonthDateDayHourMinuteSecond();
                  Path target = finished.resolve(dateTag + "_" + methodPath.getFileName());
                  try {
                    Files.move(methodPath, target, StandardCopyOption.REPLACE_EXISTING);
                    LOGGER.trace("Moved method file from {} to {}",
                        methodPath.toAbsolutePath().normalize(),
                        target.toAbsolutePath().normalize());
                  } catch (NoSuchFileException e) {
                    // another instance already moved it - fine
                    LOGGER.trace("Method file {} already moved by another instance",
                        methodPath.toAbsolutePath().normalize());
                  } catch (IOException e) {
                    // e.g. file still locked by the sender; it stays in place and is retried next run
                    LOGGER.trace("Could not move method file {} to {}: {}",
                        methodPath.toAbsolutePath().normalize(),
                        target.toAbsolutePath().normalize(), e.getMessage());
                  }

                }
              }
            }

          } // end of while - break sends us here
          Platform.runLater(()->{
            if (masterButton != null) {
              masterButton.setSelected(false);
            }
            isActive.set(false); // e.g., from path being empty (sends us here without settings to false)
          });
          return new EmptyTaskResult();
        }
      };

      SpTool3Main.getRunTime().getTaskManager().forceToTinyPool(watchTask);
    }
  }


  public synchronized void disarm() {
    isActive.set(false);
    currentPath.set(null);
    Platform.runLater(()->{
      if (masterButton != null) {
        masterButton.setSelected(false);
      }
    });
  }

}
