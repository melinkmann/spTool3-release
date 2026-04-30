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

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import core.RunTimeInstance;
import core.SampleRegister;
import core.SpTool3Main;
import gui.dialog.notification.NotificationFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tasks.BatchTask;
import tasks.batch.SimpleLinearBatch;
import tasks.results.EmptyTaskResult;
import tasks.results.FunctionalTaskResult;
import tasks.single.FunctionalTask;
import util.NF;
import util.SnF;

public abstract class Serializer {

  private static final Logger LOGGER = LogManager.getLogger(Serializer.class.getName());

  public static void saveSampleRegister() {
    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().addAll(new ExtensionFilter("Project files",
        "*" + GlobalIO.SERIALIZED_PROJECT_EXTENSION));

    // directoryChooser: set start point for the chooser
    Path path = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    Path lastProjectFile = SpTool3Main.getRunTime().getLastProjectFile();
    if (lastProjectFile != null && Files.isRegularFile(lastProjectFile) && Files.isReadable(lastProjectFile)) {
      chooser.setInitialDirectory(lastProjectFile.toFile().getParentFile());
      chooser.setInitialFileName(lastProjectFile.getFileName().toString());
    } else if (Files.isDirectory(path)) {
      chooser.setInitialDirectory(path.toFile());
    } else {
      PathUtil.createDir(path);
      if (Files.isDirectory(path)) {
        chooser.setInitialDirectory(path.toFile());
      }
    }
    // directoryChooser: open dialogue
    File returnedDir = chooser.showSaveDialog(SpTool3Main.getMainStage());
    // make sure thj returned directory is not null (e.g. user aborts choice)
    if (returnedDir != null) {
      NotificationFactory.openAutocloseInfo("Saving project...");
      SampleRegister reg = SpTool3Main.getRunTime().getSampleReg();

      SpTool3Main.getRunTime().setLastProjectFile(returnedDir.toPath());

      int compressionLevel = SpTool3Main.getRunTime().getConfParams()
          .getZstdCompressionLevel().getValue();
      // must be at least 1
      compressionLevel = Math.max(compressionLevel, Zstd.minCompressionLevel());
      // must be at largest 22
      compressionLevel = Math.min(compressionLevel, Zstd.maxCompressionLevel());
      int finalCompressionLevel = compressionLevel;

      int parallelThreads = SpTool3Main.getRunTime().getConfParams().calcNumberOfCompressorThreads();

      // THE HEAVY LIFTING
      FunctionalTask task = new FunctionalTask("Save project",
          () -> {
            /*
             Note: Conversion is now handled in each sample in read/write method
             as one cannot hold 20 samples with 30 isotopes in RAM
             */
            //reg.toRAM();
            reg.setVersionInfo(SpTool3Main.VERSION_ID);

            try {
            /*
             Asked ChatGPT about optimization... does not optimize so much.
             DefaultSettings yield 175 MB....

             e.g. Deflater.FILTERED: 180 MB, faster, best for small random stuff
                  Deflater.BEST_COMPRESSION: 175 MB (for 25 samples a 3 isotopes and 20s of 90 µs DT)

            int bufferSize = 512 ; // 512 ... 4096.... 8192 ... 16384
            GZIPOutputStream zipStream = new GZIPOutputStream(fileOutputStream, bufferSize) {
              {
                def.setLevel(Deflater.BEST_COMPRESSION); //BEST_COMPRESSION
              }
            };
             */
              /*
              older version: claude sonnet 4.6 recommends Zstd
              ObjectOutputStream objectOutputStream;
              FileOutputStream fileOutputStream = new FileOutputStream(returnedDir);
              GZIPOutputStream zipStream = new GZIPOutputStream(fileOutputStream);
              objectOutputStream = new ObjectOutputStream(zipStream);
              objectOutputStream.writeObject(reg);
              objectOutputStream.close();
              zipStream.close();
              fileOutputStream.close();
              */

              FileOutputStream fileOutputStream = new FileOutputStream(returnedDir);
              BufferedOutputStream buffered = new BufferedOutputStream(fileOutputStream);
              ZstdOutputStream zipStream =
                  new ZstdOutputStream(buffered).setLevel(finalCompressionLevel).setWorkers(parallelThreads);
              ObjectOutputStream objectOutputStream = new ObjectOutputStream(zipStream);
              objectOutputStream.writeObject(reg);
              objectOutputStream.close();
              zipStream.close();
              fileOutputStream.close();

              LOGGER.info("Wrote " + SnF
                  .doubleToString(Files.size(returnedDir.toPath()) / (1024.0 * 1024.0), NF.D1C1)
                  + " MB to file: " + returnedDir);

            } catch (Exception e) {
              NotificationFactory.openError("Cannot save project.");
             /*
             Note: Conversion is now handled in each sample in read/write method
             as one cannot hold 20 samples with 30 isotopes in RAM
             */
              //reg.toHDD();
              LOGGER.error(ExceptionUtils.getStackTrace(e));
            }

            /*
            I think this was due to the Median() implementation in SMILES which alters the input array!

             after saving put back to hdd - this seems to fix the weird bug where Raw data looks crazy
             I am not sure why, but the TISeries is in fact scrambled but apparently (first 100  DP)
             time is fine. Intensity, however, is scrambled and the mean changes (checked as a measure).
             It only happens when you hit reprocess. Reprocessing is done in a parallel fashion.
             Maybe, during the parallel accessing, something in the multithreading goes wrong.
             But I have no clue how and why. In particular, a RAM series should be more stable
             under multi-threading environment and during processing, we do not touch the TISeries,
             we only access it. What is going on there?
             Found it! The bug appears when calculating the baseline.
             We pass a pointer to the yData to the BaselineGenerator.
             When using RAM series, it receives the actual Pointer to the array.
             I do not get why they get scrambled.. If the outlier removal was at fault,
             I would expect to see fewer data points and some issues with length afterwards...
             I added a copy call to the baselineGenerator to ensure that we never pass the original
             pointer to a function that modifies arrays.
             But this explains why only yData was scrambled.

             It seems, it was due to the removeOutlier calls...
             Older versions of Median from Smiles library modified the original array. Now the wrapper
             function
             creates a copy when calling Median through measure of position.

             Anyway: We should convert to HDD here to save RAM!

             Note: Conversion is now handled in each sample in read/write method
             as one cannot hold 20 samples with 30 isotopes in RAM
             */
            //reg.toHDD();
          },
          new FunctionalTaskResult(() -> {
            Platform.runLater(() -> {
              NotificationFactory
                  .openAutocloseInfo("Finished saving project - check for potential error messages.");
            });
          }));

      // As it does not update internally, it feels better to show some progress in the UI...
      task.getProgress().set(0.5);

      if (reg != null) {
        BatchTask parallel = new SimpleLinearBatch<>("Save project",
            task, false, new EmptyTaskResult());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }
    }
  }

  public static void loadSampleRegister() {
    Path path = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    PathUtil.createDir(path);
    Path lastProjectFile = SpTool3Main.getRunTime().getLastProjectFile();

    FileChooser chooser = new FileChooser();
    chooser.getExtensionFilters().addAll(new ExtensionFilter("Project files",
        "*" + GlobalIO.SERIALIZED_PROJECT_EXTENSION));

    if (lastProjectFile != null && Files.isRegularFile(lastProjectFile) && Files.isReadable(lastProjectFile)) {
      chooser.setInitialDirectory(lastProjectFile.toFile().getParentFile());
      chooser.setInitialFileName(lastProjectFile.getFileName().toString());
    } else if (Files.isDirectory(path)) {
      chooser.setInitialDirectory(path.toFile());
    } else {
      PathUtil.createDir(path);
      if (Files.isDirectory(path)) {
        chooser.setInitialDirectory(path.toFile());
      }
    }
    //
    File returnedDirectory = chooser.showOpenDialog(SpTool3Main.getMainStage());
    loadSampleRegister(returnedDirectory);
  }

  public static void loadSampleRegister(File file) {
    // make sure the returned directory is not null (e.g. user aborts choice)
    if (file != null) {
      NotificationFactory.openAutocloseInfo("Loading project...");

      // THE HEAVY LIFTING
      FunctionalTask task = new FunctionalTask("Load project",
          () -> {
            try {
              FileInputStream fis = new FileInputStream(file);
              // Changed by recommendation of claude sonnet 4.6
              BufferedInputStream buffered = new BufferedInputStream(fis);

              // Peek at magic bytes to detect format
              buffered.mark(2);
              int byte1 = buffered.read();
              int byte2 = buffered.read();
              buffered.reset();

              InputStream decompressed;
              if (byte1 == 0x1f && byte2 == 0x8b) {
                // Legacy GZIP file
                LOGGER.info("Reading GZIP compressed project file.");
                decompressed = new GZIPInputStream(buffered);
              } else {
                // New Zstd file
                LOGGER.info("Reading Zstd compressed project file.");
                decompressed = new ZstdInputStream(buffered);
              }

              ObjectInputStream objectInputStream = new ObjectInputStream(decompressed);
              Object obj = objectInputStream.readObject();
              objectInputStream.close();
              decompressed.close();
              fis.close();

              if (obj instanceof SampleRegister) {
                SampleRegister reg = (SampleRegister) obj;
                LOGGER.info("Trying to load project that was saved with version " +
                    reg.getVersionInfo() + ".");

                /*
                  Note: Conversion is now handled in each sample in read/write method
                  as one cannot hold 20 samples with 30 isotopes in RAM
                */
                //reg.toHDD();
                RunTimeInstance.setSampleRegister(reg);

                // save and set as String in UI
                SpTool3Main.getRunTime().setLastProjectFile(file.toPath());


              }
            } catch (Exception e) {
              LOGGER.error(ExceptionUtils.getStackTrace(e));
            }
          },

          new FunctionalTaskResult(() -> {
            Platform.runLater(() -> {
              NotificationFactory
                  .openAutocloseInfo("Finished loading project - check for potential error messages.");
            });
          }));

      // As it does not update internally, it feels better to show some progress in the UI...
      task.getProgress().set(0.5);

      BatchTask parallel = new SimpleLinearBatch<>("Load project",
          task, false,
          () -> Platform.runLater(
              () -> SpTool3Main.getRunTime().getMainWindowCtl().selectFirstSampleSet()));
      SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
    }
  }

}
