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

package core;

import gui.StageFactory;
import gui.dialog.notification.NotificationFactory;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.Functional;
import util.storage.TmpFileCleanup;


public class SpTool3Main extends Application {

  private static final Logger LOGGER = LogManager.getLogger(SpTool3Main.class);

  // general
  private static Boolean ANALYZER = null; // this might cause problems...

  public static Boolean getANALYZER() {
    if (ANALYZER == null) {
      IllegalStateException ex = new IllegalStateException(
          "The configuration state was requested before setting its value. Starting in Analyzer mode.");
      LOGGER.error("ANALYZER requested before initialization!");
      LOGGER.error(ExceptionUtils.getMessage(ex) + ". Stack trace: " + ExceptionUtils.getStackTrace(ex));
      return true;
    } else {
      return ANALYZER;
    }
  }

  public static void setANALYZER(Boolean analyzer) {
    LOGGER.info("Configuration state was set successfully as analyser = " + analyzer);
    SpTool3Main.ANALYZER = analyzer;
  }

  public static final Boolean SHOW_DRIFT = false;
  public static final Boolean SHOW_ACCUMULATION = false;
  public static final Boolean SHOW_WINDOW = false;
  public static final Boolean SHOW_INTERFERENCE_DB = false;
  public static final Boolean SHOW_PEAK_MODEL = false;

  public static final String VERSION_ID = "3.1.0";
  public static final String AUTHOR_STATEMENT =
      """     
          
          Developed by Dr. Matthias Elinkmann,
          Nano Micro Lab of Prof. David Clases University of Graz, Austria,
          previously group of Prof. Uwe Karst, University of Münster, Germany.
          
          2019 - 2026 & beyond.""";

  public static final String ABOUT_STRING =
      """
          Single particle tool (spTool)
          
          for the analysis and generation of fast transient
          inductively coupled plasma-mass spectrometry (ICP-MS) data.
          
          Developed by Matthias Elinkmann.
          
          Applications: single particle ICP-MS (spICP-MS), single cell ICP-MS (scICP-MS)
          and further single event ICP-MS applications.
          
          Literature: https://doi.org/10.1039/D3JA00292F
          """;


  public static final String COAUTHORS_STRING =
      """ 
          
          User feedback was greatly appreciated (alphabetical order):
          David Ahlers, Dr. Lisa Balke, Dr. Niklas Bendieck, Dr. Julia Dressler, Helena Friedrich,
          Dr. Joshua Fuchs, Dr. Steffen Heuckeroth, Joakim Kauhanen, Dr. Alexander Köhrer
          Dr. Katharina Kronenberg, Dr. Ilona Nordhorn, Dr. Robin Schmid,
          Dr. Svenja Berit Seiffert, Dr. Karolin Sommer, Dr. Tim Steinwachs.
          
          Density data base and compound Poisson quantile look-up tables from SPCal (Dr. Thomas Lockwood)
          were greatly appreciated: https://github.com/djdt/spcal
          """;

  private static Stage mainStage = new Stage();

  private static final RunTimeInstance RUN_TIME_INSTANCE = new RunTimeInstance();

  // **Main - - Java**
  public static void main(String[] args) {
    // show more logging details
    //     System.setProperty("javafx.verbose", "true");

    // Try to log exceptions that are not explicitly caught
    Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
      LOGGER.fatal("Uncaught exception in thread {}", thread.getName(), throwable);
    });


    launch(args);
  } // main end

  @Override
  public void start(Stage stage) throws Exception {

    LOGGER.info("Starting spTool.");

    /**
     * storage: reset piled up temp files from MemoryMappedStorage
     */
    LOGGER.info("Delete temp files manually at temp directory: "
        + System.getProperty("java.io.tmpdir"));
    TmpFileCleanup tmpFileCleanup = new TmpFileCleanup();
    Thread executeTmpFileCleanup = new Thread(tmpFileCleanup);
    executeTmpFileCleanup.start();

    // Build window
    mainStage = stage;
    StageFactory.createMainWindow(mainStage);
    // Do this after creating the stage
    mainStage.getScene().getWindow().addEventFilter(
        WindowEvent.WINDOW_CLOSE_REQUEST, this::closeWindowEvent);

//    NumberConvertibleUnit num = new DoubleConvertibleUnit(50, MassUnit.MICRO_GRAM);
//    NumberConvertibleUnit conv = num.convert(MassUnit.GRAM);
//    System.out.println(conv.translate(NF.D1C6));
  }

  public static RunTimeInstance getRunTime() {
    return RUN_TIME_INSTANCE;
  }

  public static Stage getMainStage() {
    return mainStage;
  }

  // prevent window from closing:
  // https://stackoverflow.com/questions/26619566/javafx-stage-close-handler
  private void closeWindowEvent(WindowEvent event) {
    NotificationFactory.openYesNo(
        "You are about to close SpTool."
            + "\nIf you close without saving, progress may be lost."
            + "\nClose without saving?"
            + "\n"
            + "\n"
            + SpTool3Main.AUTHOR_STATEMENT,
        Functional.empty(), // Event is not consumed, i.e. stage closes
        () -> {
          event.consume();
          // e.g. Clear unsaved changes AND then write to store the defaults.
        });
  }


} // End of Main