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

package processing.parameterSets.action;

import analysis.*;
import core.SpTool3Main;
import dataModelNew.*;
import dataModelNew.fxImpl.FxSample;
import dataModelNew.mz.Channel;
import dataModelNew.mz.ComputedChannel;
import dataModelNew.mz.Element;
import dataModelNew.mz.IsotopeMZ;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.ListContainer;
import gui.dialog.caseImpl.ExecuteSubmethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import gui.dialog.mainImpl.ViewListDialog;
import gui.util.UiUtil;
import io.export.ClipboardWriter;
import io.export.DataExport;
import io.export.ExportWriter;
import io.fastExport.TabBlock;
import io.fastExport.TabBlockColl;
import javafx.event.ActionEvent;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import processing.options.PopulationType;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.ListMethod;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.*;
import sandbox.montecarlo.AutofillGenerator;
import sandbox.montecarlo.Isotope;
import tasks.BatchTask;
import tasks.Task;
import tasks.WorkingTask;
import tasks.batch.SimpleLinearBatch;
import tasks.batch.SimpleParallelBatch;
import tasks.results.EmptyTaskResult;
import tasks.single.*;
import util.ArrUtils;
import util.NF;
import util.SnF;

/*
- save processing history (at least store the method!)
- show baseline data (µ) and graph --> e.g. dashed for start/stop, thin for height, thick for gate
----- Add stroke, ..., to the baseline: http://www.java2s
.com/Code/Java/Chart/JFreeChartLineChartDemo5showingtheuseofacustomdrawingsupplier.htm
https://stackoverflow.com/questions/844988/dashed-line-in-jfreechart
----- 	Note that these plots need to store the respective significance values or get them from the stored
method....
	Easier: Just store a copy of the chosen submethod in e.g., the Baseline object, the Population (search,
	list, gate).

- put 2 default populations (one for size, one for number)
- a third one is possible, but only after running a Align instance --> thrid pop for cluster analysis which
 is created from the pipeline
  according to the current branch, i.e., if Aligner is placed in number branch, than we will get the
  aligned pop from that one.
  ## That makes no sense! Align should be an option on its own in the search, else the structure becomes
  confusing!
- clean-up the structure that translates instructions (Alpha, Z, ..) to the Thrshold supplier
 */

public abstract class Actions {

  private static final Logger LOGGER = LogManager.getLogger(Actions.class);


  public static void executeMethodCreateBtn(Method method, int n) {

    // Copy to make sure that concurrent changes in the UI do not affect processing.
    method = method.getCopyWithoutFile();

    /*
    Design: We wrap a sample in an atomic reference which is either initialized as null
    (in that case the method has to provide a sample, e.g., via Monte Carlo)
    or as a selected sample.

    Now, the work must be executed in a linear Batch to ensure "one task after the other" progression.
    Then, however, there should be no issue of having to return/pass the samples between Tasks.
     */

    List<Task> tasks = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      List<Task> sampleTasks = new ArrayList<>();
      // each iteration gives one sample
      AtomicReference<Sample> sampleRef = new AtomicReference<>(null);
      Task synthesizer = new MonteCarloGeneratorTask(
          "Data synthesizer " + Integer.toString(i + 1) + " of " + n,
          method, sampleRef);
      sampleTasks.add(synthesizer);
      sampleTasks.addAll(getProcessingSteps(method, sampleRef));

      BatchTask sampleBatchTask = new SimpleLinearBatch<>("Sample batch " + i + " of " + n,
          sampleTasks, false, new EmptyTaskResult());
      tasks.add(sampleBatchTask);
    }

    // Should not parallelize due to lack of RAM in case of Monte Carlo.
    // --> SimpleLinearBatch vs SimpleParallelBatch
    BatchTask task = new SimpleParallelBatch("Data synthesizer batch", tasks, false,
        () -> SpTool3Main.getRunTime().getSampleReg().flushWaitingList());
    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(task);
  }

  public static void executeMethodCreateBtn(List<Method> allMethods, int n) {

    List<List<Method>> subMethodLists = ArrUtils.split(allMethods, 10);

    List<Task> subsetBatchList = new ArrayList<>();
    for (List<Method> subMethodList : subMethodLists) {

      // All samples of all methods in this subset. Each of them is one linear batch.
      List<Task> sampleBatchTasks = new ArrayList<>();
      for (Method method : subMethodList) {
        // Copy to make sure that concurrent changes in the UI do not affect processing.
        method = method.getCopyWithoutFile();

      /*
      Design: We wrap a sample in an atomic reference which is either initialized as null
      (in that case the method has to provide a sample, e.g., via Monte Carlo)
      or as a selected sample.

      Now, the work must be executed in a linear Batch to ensure "one task after the other" progression.
      Then, however, there should be no issue of having to return/pass the samples between Tasks.
       */

        for (int i = 0; i < n; i++) {
          // each iteration gives one sample
          List<Task> sampleSubmethodTasks = new ArrayList<>();
          AtomicReference<Sample> sampleRef = new AtomicReference<>(null);
          Task synthesizer = new MonteCarloGeneratorTask(
              "Data synthesizer " + Integer.toString(i + 1) + " of " + n,
              method, sampleRef);
          sampleSubmethodTasks.add(synthesizer);
          sampleSubmethodTasks.addAll(getProcessingSteps(method, sampleRef));

          // Strictly one task after the other for this sample.
          BatchTask sampleBatchTask = new SimpleLinearBatch<>("Sample batch " + (i + 1) + " of " + n,
              sampleSubmethodTasks, false, new EmptyTaskResult());
          sampleBatchTasks.add(sampleBatchTask);
        }
      }

      // The samples of this subset run in parallel. Flush once the whole subset is done.
      BatchTask subsetBatchTask = new SimpleParallelBatch("Data synthesizer subset batch",
          sampleBatchTasks, false,
          () -> SpTool3Main.getRunTime().getSampleReg().flushWaitingList());
      subsetBatchList.add(subsetBatchTask);
    }

    if (subsetBatchList.isEmpty()) {
      return;
    }

    // The subsets themselves are called one after the other.
    BatchTask task = new SimpleLinearBatch<>("Data synthesizer main batch", subsetBatchList, false,
        new EmptyTaskResult());

    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(task);
  }


  public static void reprocess(Method currentMethodOnUI, List<FxSample> fxSamples) {

    // Make sure to copy the method: otherwise, we only store a pointer.
    // Use copy without file to keep sync with method file on drive
    currentMethodOnUI = currentMethodOnUI.getCopyWithoutFile();

    List<Sample> samples = new ArrayList<>();

    for (FxSample fxSample : fxSamples) {
      samples.addAll(fxSample.getPlainSample().getAllSamples());
    }

    for (Sample s : samples) {
      for (Trace trace : s.getTraces()) {
        trace.clearEvaluation();
      }
    }

    List<Task> sampleBatches = new ArrayList<>();

    for (Sample sample : samples) {

      // do not reprocess the incomplete sample case, ..
      if (sample instanceof SampleImpl) {

        AtomicReference<Sample> sampleRef = new AtomicReference<>(sample);

        List<Task> sampleSubTasks = new ArrayList<>(getProcessingSteps(currentMethodOnUI, sampleRef));

        // Skip reprocessing of raw data, else: unnecessary calc and Buffers)
        // sampleSubTasks.removeIf(t -> t instanceof RawDataProcessingTask); REALLY?

        BatchTask sampleBatch = new SimpleLinearBatch<>("Sample batch " + sample.getNickName(),
            sampleSubTasks, false, new EmptyTaskResult());
        sampleBatches.add(sampleBatch);


        /*
         Update the method that was used for processing here EXCEPT for Generator and CSV methods.
         Use try/catch: deserialization may fail when sample has older version of ParamSet.
         */

        Method oldMethodFromSample;
        try {
          oldMethodFromSample = sample.getMethod().getCopyWithoutFile();
        } catch (Exception e) {
          oldMethodFromSample = new ListMethod();
          LOGGER.error("Cannot copy method. Likely cause: sample is from project of older version."
              + " Message: " + ExceptionUtils.getMessage(e)
              + ". Details:" + ExceptionUtils.getStackTrace(e));
        }

        Method newMethod = currentMethodOnUI.getCopyWithoutFile();
        newMethod.clearSets();

        // refill with old generator methods and new all later other methods
        for (ParamSet oldSet : oldMethodFromSample.getSets()) {
          if (oldSet instanceof CsvInterpreterParams
              || oldSet instanceof NuInterpreterParams
              || oldSet instanceof MCSimGeneralParams
              || oldSet instanceof MCSimParticleParams
              || oldSet instanceof ExporterParams) {
            newMethod.addSet(oldSet);
          }
        }

        // Special case: when processing a real, i.e., loaded sample for the FIRST time, we should add csv!
        // This must be done before adding the other processing methods to have csv at the top
        if (newMethod.getSets().stream().noneMatch(s -> s instanceof CsvInterpreterParams)) {
          for (ParamSet currentSet : currentMethodOnUI.getSets()) {
            if (currentSet instanceof CsvInterpreterParams)
              newMethod.addSet(currentSet);
          }
        }

        // same logic for nu reader
        if (newMethod.getSets().stream().noneMatch(s -> s instanceof NuInterpreterParams)) {
          for (ParamSet currentSet : currentMethodOnUI.getSets()) {
            if (currentSet instanceof NuInterpreterParams)
              newMethod.addSet(currentSet);
          }
        }

        // add all processing param sets but not the Csv/Sim, ... sets. Why?
        // When the method in the UI changes, the sample has still been made/loaded with the old param
        // settings!!
        for (ParamSet currentSet : currentMethodOnUI.getSets()) {
          if (!(currentSet instanceof CsvInterpreterParams)
              && !(currentSet instanceof NuInterpreterParams)
              && !(currentSet instanceof MCSimGeneralParams)
              && !(currentSet instanceof MCSimParticleParams)
              && !(currentSet instanceof ExporterParams)) {
            newMethod.addSet(currentSet);
          }
        }

        sample.setMethod(newMethod);
      }
    }

    BatchTask task = new SimpleParallelBatch("Parallel sample processing batch",
        sampleBatches, false,
        // runlater() is called in updatePopulations()
        () -> {
          // this should refresh isotope view in case isotopes have been removed
          SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets();
          SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations();
        });

    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(task);
  }


  private static List<Task> getProcessingSteps(Method method, AtomicReference<Sample> sample) {

    boolean hasActiveSearchBlock = false;
    NormalSearchParams activeSearchParams = null;

    List<Task> tasks = new ArrayList<>();
    List<ParamSet> subMethods = method.getSets();
    List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

    // Manage populations
    PopulationBranch branch = new PopulationBranch();
    int searchBranchIndex = 0;

    for (ParamSet set : subMethods) {

      // This must be executed first!!
      if (set instanceof TimeRoiParams) {
        tasks.add(new TimeRoiTask("TimeRegion", (TimeRoiParams) set, sample.get()));
      }

      // Then this should be executed
      if (set instanceof SignalModificationParams) {
        tasks.add(new SignalModificationTask("Modify", branch, (SignalModificationParams) set, sample));
      }

      if (set instanceof BaselineParams) {
        tasks.add(new BaselineTask("Baseline", (BaselineParams) set, sample));
      }

      if (set instanceof NormalSearchParams) {
        branch.clear(); // resets branch track
        boolean isActive = ((NormalSearchParams) set).getEnableBoolean().getValue();
        if (isActive) {
          searchBranchIndex++;
          tasks.add(new SearchTask("Search", branch, (NormalSearchParams) set, searchBranchIndex,sample, selChannels));
          hasActiveSearchBlock = true;
          activeSearchParams = (NormalSearchParams) set;
        }
      }

      if (set instanceof GatingParams && hasActiveSearchBlock) {
        tasks.add(new GatingTask("Gating", branch, (GatingParams) set, sample));

        // check if need to recompute NP background
        GatingParams gatingParams = ((GatingParams) set);
        if (activeSearchParams != null && gatingParams.getForceNewBGDefinition().getValue()) {
          tasks.add(new ComputeBgPerNpTask("Background computation after gating",
              branch, activeSearchParams, sample));
        }
      }

      if (set instanceof IsotopeRemoverParams && hasActiveSearchBlock) {
        tasks.add(new IsotopeRemoverTask("Isotope removal", branch, (IsotopeRemoverParams) set, sample));
      }

      if (set instanceof AlignerParams && hasActiveSearchBlock) {
        tasks.add(new AlignTask("Align", branch, (AlignerParams) set, sample, selChannels));
      }

      if (set instanceof FilterParams && hasActiveSearchBlock) {
        tasks.add(new FilterTask("Filter", branch, (FilterParams) set, sample));
      }

      // This creates new samples - does not make much sense in the Method workflow.

      //      if (set instanceof DTGroupParams) {
      //        tasks.add(new DTGroupTask("DTGrouping", (DTGroupParams) set, sample.get()));
      //      }
      //
    }
    // Pre-calculate the drift factor
    PreCalculateDrift driftTask = new PreCalculateDrift(sample);
    tasks.add(driftTask);

    // Pre-calculate the spectral data for each population
    PreCalculateSpectra spectraTask = new PreCalculateSpectra(sample);
    tasks.add(spectraTask);


    return tasks;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static void makeIncreaseDTMenuItem(Menu actionMenu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Increase dwell time");
    actionMenu.getItems().add(item);
    item.setOnAction(e -> {
      lastRef.set(item);
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          SpTool3Main.getRunTime().dtGroupParams,
          SpTool3Main.getRunTime().dtGroupParams,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.DT_GROUPING),
          "Group data points to increase dwell time.",
          FxStageButton.RUN,
          600d,
          350d
      );

      // tell it to reset the parameters to default
      executeSubmethod.setResetMethodLatch(new Consumer<>() {
        @Override
        public void accept(ParamSet paramSet) {
          if (paramSet instanceof DTGroupParams) {
            LOGGER.info("Rejected parameter change due to cancel operation.");
            SpTool3Main.getRunTime().dtGroupParams = (DTGroupParams) paramSet;
          } else {
            LOGGER.error("Cannot cast {} to {}", paramSet.getClass(), DTGroupParams.class);
          }
        }
      });

      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      DTGroupParams dtGroupParams = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof DTGroupParams) {
          dtGroupParams = (DTGroupParams) params;
        }
      }

      if (dtGroupParams != null) {
        List<Task> importTasks = new ArrayList<>();

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();
        WorkingTask task = new DTGroupTask(dtGroupParams, selSamples, selChannels);
        importTasks.add(task);

        BatchTask parallel = new SimpleParallelBatch("DT grouping", importTasks, false,
            () -> SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }
    });
  }

  public static void makeClipboardIsotopeRatioMenu(Menu actionMenu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Copy isotope ratios");
    actionMenu.getItems().add(item);
    item.setOnAction(e -> {
      lastRef.set(item);

      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          new IsotopeCalculatorParams(),
          new IsotopeCalculatorParams(),
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.ISOTOPE_CALCULATOR),
          "Export isotope ratios to clipboard.",
          FxStageButton.RUN,
          600d,
          350d
      );


      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      IsotopeCalculatorParams isotopeParams = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof IsotopeCalculatorParams) {
          isotopeParams = (IsotopeCalculatorParams) params;
        }
      }

      if (isotopeParams != null) {

        ExportWriter writer = new ClipboardWriter();
        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

        // only export non-merge samples
        List<SampleImpl> samples = selSamples.stream()
            .map(Sample::getAllSamples)
            .flatMap(List::stream)
            .filter(s -> s instanceof SampleImpl)
            .map(s -> (SampleImpl) s)
            .toList();

        TabBlockColl coll = new TabBlockColl(writer, true);
        for (SampleImpl selSample : samples) {
          List<TabBlock> blocks = DataExport.extractIsotopeRatioData(selSample, selChannels,
              isotopeParams.getInvertIsotopeRatio().getValue());
          blocks.forEach(coll::add);
        }

        coll.write(DataExport.getShortMeta(null));
        coll.export();

      }
    });
  }

  public static void makeIsotopeRatioMenu(Menu actionMenu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Compute isotope ratio");
    actionMenu.getItems().add(item);
    item.setOnAction(e -> {
      lastRef.set(item);

      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          SpTool3Main.getRunTime().isotopeCalculatorParams,
          SpTool3Main.getRunTime().isotopeCalculatorParams,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.ISOTOPE_CALCULATOR),
          "Compute isotope ratio.",
          FxStageButton.RUN,
          600d,
          350d
      );

      // tell it to reset the parameters to default
      executeSubmethod.setResetMethodLatch(new Consumer<>() {
        @Override
        public void accept(ParamSet paramSet) {
          if (paramSet instanceof IsotopeCalculatorParams) {
            LOGGER.info("Rejected parameter change due to cancel operation.");
            SpTool3Main.getRunTime().isotopeCalculatorParams = (IsotopeCalculatorParams) paramSet;
          } else {
            LOGGER.error("Cannot cast {} to {}", paramSet.getClass(), IsotopeCalculatorParams.class);
          }
        }
      });

      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      IsotopeCalculatorParams isotopeParams = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof IsotopeCalculatorParams) {
          isotopeParams = (IsotopeCalculatorParams) params;
        }
      }

      if (isotopeParams != null) {

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

        // extract non-merged samples
        List<SampleImpl> samples = selSamples.stream()
            .map(Sample::getAllSamples)
            .flatMap(List::stream)
            .filter(s -> s instanceof SampleImpl)
            .map(s -> (SampleImpl) s)
            .toList();

        for (SampleImpl sample : samples) {

          List<Trace> traces = sample.getTraces(selChannels);

          // ensure we have 2 traces at least
          if (traces.size() > 1) {

            // we simply divide first by second
            if (isotopeParams.getInvertIsotopeRatio().getValue()) {
              Collections.reverse(traces);
            }

            // get data
            Trace upperFrac = traces.get(0);
            Trace lowerFrac = traces.get(1);

            // try to get element
            Element element = null;
            Isotope iso1 = upperFrac.getChannel().getIsotope();
            Isotope iso2 = lowerFrac.getChannel().getIsotope();
            if (iso1 != null && iso2 != null) {
              if (iso1.getElement() == iso2.getElement()) {
                element = iso1.getElement();
              }
            }

            // Store contributing channels
            List<Channel> contributingChannels = new ArrayList<>();
            contributingChannels.add(upperFrac.getChannel());
            contributingChannels.add(lowerFrac.getChannel());

            TISeries upperFracTiSeries = upperFrac.getTISeries();
            TISeries lowerFracTiSeries = lowerFrac.getTISeries();

            double[] upperFracTime = upperFracTiSeries.getTime();
            double[] upperFracIntensity = upperFracTiSeries.getIntensity();
            double[] lowerFracIntensity = lowerFracTiSeries.getIntensity();

            if (lowerFracIntensity.length == upperFracIntensity.length) {

              List<Double> ratioDoubles = new ArrayList<>(upperFracTiSeries.size());
              List<Double> ratioTimes = new ArrayList<>(upperFracTiSeries.size());

              for (int i = 0; i < lowerFracIntensity.length; i++) {
                double low = lowerFracIntensity[i];
                double up = upperFracIntensity[i];
                double time = upperFracTime[i];
                if (low > 0) {
                  double ratio = up / low;
                  ratioTimes.add(time);
                  ratioDoubles.add(ratio);
                } else {
                }
              }

              // store
              String ratioLabel =
                  upperFrac.getChannel().getShortUIString()
                      + "/"
                      + lowerFrac.getChannel().getShortUIString();

              Channel ratioChannel;
              if (element != null) {
                ratioChannel = new ComputedChannel(element, ratioLabel, contributingChannels);
              } else {
                ratioChannel = new ComputedChannel(ratioLabel, contributingChannels);
              }

              TISeries ratioSeries = new TISeriesHDD(ratioTimes, ratioDoubles);
              Trace sumTrace = new TraceImpl(sample, ratioChannel, ratioSeries);
              sample.addTrace(sumTrace);
            }
          }
        }
        SpTool3Main.getRunTime().getMainWindowCtl().updateIsotopes();
      }
    });
  }

  public static void makeTimeRoiMenuItem(Menu menu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Cut time region");
    menu.getItems().add(item);
    item.setOnAction(e -> {
      lastRef.set(item);
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          SpTool3Main.getRunTime().timeRoiParams,
          SpTool3Main.getRunTime().timeRoiParams,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.TIME_ROI),
          "Cut time region.",
          FxStageButton.RUN,
          600d,
          350d
      );

      // tell it to reset the parameters to default
      executeSubmethod.setResetMethodLatch(new Consumer<>() {
        @Override
        public void accept(ParamSet paramSet) {
          if (paramSet instanceof TimeRoiParams) {
            LOGGER.info("Rejected parameter change due to cancel operation.");
            SpTool3Main.getRunTime().timeRoiParams = (TimeRoiParams) paramSet;
          } else {
            LOGGER.error("Cannot cast {} to {}", paramSet.getClass(), TimeRoiParams.class);
          }
        }
      });

      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      TimeRoiParams method = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof TimeRoiParams) {
          method = (TimeRoiParams) params;
        }
      }

      if (method != null) {
        List<Task> importTasks = new ArrayList<>();

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        WorkingTask task = new TimeRoiTask(method, selSamples);
        importTasks.add(task);

        BatchTask parallel = new SimpleParallelBatch("Time roi", importTasks, false,
            () -> SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }
    });
  }

  public static void makeMethodDOEMenuItem(Menu menu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Run method DOE");
    menu.getItems().add(item);
    item.setOnAction(e -> {

      lastRef.set(item);
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          SpTool3Main.getRunTime().methodDoeParams,
          SpTool3Main.getRunTime().methodDoeParams,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.METHOD_DOE),
          "Run method design of experiment (DOE) for D, y, v.",
          FxStageButton.RUN,
          600d,
          650d,
          UiUtil.getImage("/img/wizardDOE.png")
      );

      // tell it to reset the parameters to default
      executeSubmethod.setResetMethodLatch(new Consumer<>() {
        @Override
        public void accept(ParamSet paramSet) {
          if (paramSet instanceof MethodDoeParams) {
            LOGGER.info("Rejected parameter change due to cancel operation.");
            SpTool3Main.getRunTime().methodDoeParams = (MethodDoeParams) paramSet;
          } else {
            LOGGER.error("Cannot cast {} to {}", paramSet.getClass(), MethodDoeParams.class);
          }
        }
      });

      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      MethodDoeParams method = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof MethodDoeParams) {
          SpTool3Main.getRunTime().saveParameterSets();
          method = (MethodDoeParams) params;
        }
      }

      if (method != null) {
        Method current = SpTool3Main.getRunTime().getMainWindowCtl().getMethodView().getCurrentMethod();
        if (current != null) {
          AutofillGenerator.scheduleDOE(current, method);
        }
      }
    });
  }

  public static void makeBasicRoiMenuItem(Menu menu, AtomicReference<MenuItem> lastRef) {
    MenuItem item = new MenuItem("Select event data range");
    menu.getItems().add(item);
    item.setOnAction(e -> {
      lastRef.set(item);
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          SpTool3Main.getRunTime().eventDataRangeParams,
          SpTool3Main.getRunTime().eventDataRangeParams,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.EVENT_DATA_ROI),
          "Select event data range.",
          FxStageButton.RUN,
          600d,
          350d
      );

      // tell it to reset the parameters to default
      executeSubmethod.setResetMethodLatch(new Consumer<>() {
        @Override
        public void accept(ParamSet paramSet) {
          if (paramSet instanceof EventDataRangeParams) {
            LOGGER.info("Rejected parameter change due to cancel operation.");
            SpTool3Main.getRunTime().eventDataRangeParams = (EventDataRangeParams) paramSet;
          } else {
            LOGGER.error("Cannot cast {} to {}", paramSet.getClass(), EventDataRangeParams.class);
          }
        }
      });

      // Automatically size to fit content
      executeSubmethod.getDialogPane().getScene().getWindow().sizeToScene();
      Optional<ParamSet> exe = executeSubmethod.showAndWait();
      EventDataRangeParams method = null;
      if (exe.isPresent()) {
        ParamSet params = exe.get();
        if (params instanceof EventDataRangeParams) {
          method = (EventDataRangeParams) params;
        }
      }

      if (method != null) {
        List<Task> importTasks = new ArrayList<>();

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
        List<PopulationID> selPops = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();

        WorkingTask task = new EventRangeRoiTask(method, selSamples, selPops);
        importTasks.add(task);

        BatchTask parallel = new SimpleParallelBatch("Time roi", importTasks, false,
            () -> SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }
    });
  }

  /// ///////////////////////////////////////////////////////////////////////////
  /// ///////////////////////////////////////////////////////////////////////////

  public static void makeAlphaBetaMenu(Menu menu) {

    MenuItem item = new MenuItem("Estimate a- and b-error");
    menu.getItems().add(item);

    item.setOnAction(Actions::handle);
  }


  private static @NotNull ViewListDialog<String> getStringViewListDialog(List<String> info) {
    ListContainer<String> container = new ListContainer<>() {
      @Override
      public List<FxEntry<String>> getList(FxEntryFactory<String> factory) {
        return factory.create(info);
      }

      @Override
      public ViewListDialog<String> getListDialog() {
        return new ViewListDialog<>(this, new FxEntryFactory.SimpleEntryFactory<>());
      }
    };

    ViewListDialog<String> view = new ViewListDialog<>(
        container, new FxEntryFactory.SimpleEntryFactory<>());
    return view;
  }


  private static void handle(ActionEvent e) {

    List<PopulationID> selIDs = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
    List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
    List<Channel> selChannels = SpTool3Main.getRunTime().getMainWindowCtl().getSelChannels();

    List<PopulationID> simIDs = selIDs.stream()
        .filter(id -> id.getType().equals(PopulationType.SIMULATION))
        .collect(Collectors.toList());

    List<PopulationID> evalIDs = new ArrayList<>(selIDs);
    evalIDs.removeIf(id -> id.getType().equals(PopulationType.SIMULATION));

    List<String> info = new ArrayList<>();
    if (!simIDs.isEmpty() && !evalIDs.isEmpty()) {

      for (Sample selSample : selSamples) {

        StringBuilder summary = new StringBuilder();

        summary.append("#### New sample ####").append("\nNick name: ").append(selSample.getNickName());

        for (Channel channel : selChannels) {

          summary.append("\n\nIsotope: ").append(channel.getShortUIString());

          List<Event> simEvts = selSample.getNPEvents(channel, simIDs.get(0));

          for (PopulationID evalID : evalIDs) {
            List<Event> evalEvts = selSample.getNPEvents(channel, evalID);

            List<Event> matchFromSim = new ArrayList<>();
            List<Event> matchFromEval = new ArrayList<>();
            List<Event> noMatchSim = new ArrayList<>();
            List<Event> noMatchEval = new ArrayList<>();

            AlphaBetaEvaluation.checkEvents(simEvts, evalEvts, matchFromSim, matchFromEval, noMatchSim,
                noMatchEval);

            // "only in eval", i.e., false positive
            double alpha = 100.0 * ((double) noMatchEval.size() / simEvts.size());
            // with respect to BG: dp in events / total dp
            int nDP = selSample.getTotalDataPoints(channel);
            // count data point of events that were not matched with simulation, i.e., falsely picked
            // and ignore all data points within an event
            double evtDP = noMatchEval.stream().mapToInt(Event::getNoOfPoints).sum();
            double alphaBG = 100.0 * (evtDP / nDP);

            // "only in sim", i.e., false negatives
            double beta = 100.0 * ((double) noMatchSim.size() / simEvts.size());

            // how many of the events in a series were matched at all?
            double coverageSim = 100.0 * ((double) matchFromSim.size() / simEvts.size());
            double coverageEval = 100.0 * ((double) matchFromEval.size() / evalEvts.size());

            summary.append("\nPopulation: ").append(evalID.toString())
                .append("\nalpha [%] :").append(SnF.doubleToString(alpha, NF.D1C3, NF.D1C3Exp))
                .append("\nbeta [%] :").append(SnF.doubleToString(beta, NF.D1C3, NF.D1C3Exp))
                .append("\nmatch syn [%] :").append(SnF.doubleToString(coverageSim, NF.D1C3, NF.D1C3Exp))
                .append("\nmatch eval [%] :").append(SnF.doubleToString(coverageEval, NF.D1C3, NF.D1C3Exp))
                .append("\nalpha by data pts [%] :").append(SnF.doubleToString(alphaBG, NF.D1C3, NF.D1C3Exp))

                .append("\nnSim [-] :").append(SnF.intToString(simEvts.size(), NF.D1C0))
                .append("\nnEval [-] :").append(SnF.intToString(evalEvts.size(), NF.D1C0))
                .append("\nnMatch Sim->Eval [-] :").append(SnF.intToString(matchFromSim.size(), NF.D1C0))
                .append("\nnSimNoMatch [-] :").append(SnF.intToString(noMatchSim.size(), NF.D1C0))
                .append("\nnEvalNoMatch [-] :").append(SnF.intToString(noMatchEval.size(), NF.D1C0))
            ;

          }
        }
        info.add(summary.toString());
      }

    } else {
      for (Sample selSample : selSamples) {

        StringBuilder summary = new StringBuilder("No in-silico data available!");

        summary.append("#### New sample ####").append("\nNick name: ").append(selSample.getNickName());

        List<Channel> matchedIsotope = new ArrayList<>();
        List<Channel> unMatchedIsotope = new ArrayList<>();

        for (PopulationID selID : selIDs) {

          for (Channel selChannel : selChannels) {
            List<Channel> singletonList = new ArrayList<>();
            singletonList.add(selChannel);
            List<PopulationID> subSamplePops = selSample.listPopulations(singletonList);

            if (subSamplePops.contains(selID)) {
              matchedIsotope.add(selChannel);
            } else {
              unMatchedIsotope.add(selChannel);
            }
          }

          for (Channel channel : matchedIsotope) {
            summary.append("\nPopulation: ").append(selID.toString()).append(" CONTAINS ").append(channel.getShortUIString());
          }
          for (Channel channel : unMatchedIsotope) {
            summary.append("\nPopulation: ").append(selID.toString()).append(" does not contain ").append(channel.getShortUIString());
          }

        }
        info.add(summary.toString());
      }
    }
    ViewListDialog<String> view = getStringViewListDialog(info);
    view.showAndWait();

  }
}