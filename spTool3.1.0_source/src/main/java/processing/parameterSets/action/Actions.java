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
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.Trace;
import dataModelNew.fxImpl.FxSample;
import dataModelNew.mz.MZValue;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxStageButton;
import gui.dialog.ListContainer;
import gui.dialog.caseImpl.ExecuteSubmethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import gui.dialog.mainImpl.ViewListDialog;
import gui.util.UiUtil;
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
import processing.parameterSets.impl.BaselineParams;
import processing.parameterSets.impl.CsvInterpreterParams;
import processing.parameterSets.impl.DTGroupParams;
import processing.parameterSets.impl.ExporterParams;
import processing.parameterSets.impl.FilterParams;
import processing.parameterSets.impl.GatingParams;
import processing.parameterSets.impl.MCSimGeneralParams;
import processing.parameterSets.impl.MCSimParticleParams;
import processing.parameterSets.impl.NormalSearchParams;
import processing.parameterSets.impl.TimeRoiParams;
import sandbox.montecarlo.Isotope;
import tasks.BatchTask;
import tasks.Task;
import tasks.WorkingTask;
import tasks.batch.SimpleLinearBatch;
import tasks.batch.SimpleParallelBatch;
import tasks.results.EmptyTaskResult;
import tasks.single.BaselineTask;
import tasks.single.DTGroupTask;
import tasks.single.FilterTask;
import tasks.single.GatingTask;
import tasks.single.MonteCarloGeneratorTask;
import tasks.single.PreCalculateDrift;
import tasks.single.SearchTask;
import tasks.single.TimeRoiTask;
import util.NF;
import util.SnF;

import static dataModelNew.mz.Element.Au;

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


  public static void executeMethod(Method method, int n) {

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
      // each iteration gives one sample
      AtomicReference<Sample> sampleRef = new AtomicReference<>(null);
      Task synthesizer = new MonteCarloGeneratorTask(
          "Data synthesizer " + i + 1 + " of " + n,
          method, sampleRef);
      tasks.add(synthesizer);

      tasks.addAll(getProcessingSteps(method, sampleRef));
    }

    // Cannot parallelize due to lack of RAM in case of Monte Carlo. --> SimpleLinearBatch
    // SimpleParallelBatch
    BatchTask task = new SimpleLinearBatch<>("Data synthesizer batch", tasks, false,
        () -> SpTool3Main.getRunTime().getSampleReg().flushWaitingList());
    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(task);
  }


  public static void reprocess(Method method, List<FxSample> fxSamples) {

    // Make sure to copy the method: otherwise, we only store a pointer.
    // Use copy without file to keep sync with method file on drive
    method = method.getCopyWithoutFile();

    List<Sample> samples = new ArrayList<>();

    for (FxSample fxSample : fxSamples) {
      samples.addAll(fxSample.getPlainSample().getAllSamples());
    }

    for (Sample s : samples) {
      for (Trace trace : s.getTraces()) {
        trace.voidClearEvaluation();
      }
    }

    List<Task> sampleBatches = new ArrayList<>();

    for (Sample sample : samples) {

      // do not reprocess the incomplete sample case, ..
      if (sample instanceof SampleImpl) {

        AtomicReference<Sample> sampleRef = new AtomicReference<>(sample);

        List<Task> sampleSubTasks = new ArrayList<>(getProcessingSteps(method, sampleRef));

        // Skip reprocessing of raw data, else: unnecessary calc and Buffers)
        // sampleSubTasks.removeIf(t -> t instanceof RawDataProcessingTask); REALLY?

        BatchTask sampleBatch = new SimpleLinearBatch<>("Sample batch " + sample.getNickName(),
            sampleSubTasks, false, new EmptyTaskResult());
        sampleBatches.add(sampleBatch);


        /*
         Update the method that was used for processing here EXCEPT for Generator and CSV methods.
         Use try/catch: deserialization may fail when sample has older version of ParamSet.
         */

        Method oldMethod;
        try {
          oldMethod = sample.getMethod().getCopyWithoutFile();
        } catch (Exception e) {
          oldMethod = new ListMethod();
          LOGGER.error("Cannot copy method. Likely cause: sample is from project of older version."
              + " Message: " + ExceptionUtils.getMessage(e)
              + ". Details:" + ExceptionUtils.getStackTrace(e));
        }

        Method newMethod = method.getCopyWithoutFile();
        newMethod.clearSets();
        // refill with old generator methods and new all other methods
        for (ParamSet oldSet : oldMethod.getSets()) {
          if (oldSet instanceof CsvInterpreterParams
              || oldSet instanceof MCSimGeneralParams
              || oldSet instanceof MCSimParticleParams
              || oldSet instanceof ExporterParams) {
            newMethod.addSet(oldSet);
          }
        }
        for (ParamSet currentSet : method.getSets()) {
          if (!(currentSet instanceof CsvInterpreterParams)
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
        () -> SpTool3Main.getRunTime().getMainWindowCtl().updatePopulations());

    SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(task);
  }


  private static List<Task> getProcessingSteps(Method method, AtomicReference<Sample> sample) {

    boolean hasActiveSearchBlock = false;

    List<Task> tasks = new ArrayList<>();
    List<ParamSet> subMethods = method.getSets();

    // Manage populations
    PopulationBranch branch = new PopulationBranch();

    for (ParamSet set : subMethods) {

      // This must be executed first!!
      if (set instanceof TimeRoiParams) {
        tasks.add(new TimeRoiTask("TimeRegion", (TimeRoiParams) set, sample.get()));
      }

      if (set instanceof BaselineParams) {
        tasks.add(new BaselineTask("Baseline", (BaselineParams) set, sample));
      }

      if (set instanceof NormalSearchParams) {
        branch.clear(); // resets branch track
        boolean isActive = ((NormalSearchParams) set).getEnableBoolean().getValue();
        hasActiveSearchBlock = isActive;
        if (isActive) {
          tasks.add(new SearchTask("Search", branch, (NormalSearchParams) set, sample));
        }
      }

      if (set instanceof GatingParams && hasActiveSearchBlock) {
        tasks.add(new GatingTask("Gating", branch, (GatingParams) set, sample));
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

    return tasks;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static void makeIncreaseDTMenuItem(Menu actionMenu) {
    MenuItem groupDTITem = new MenuItem("Increase dwell time");
    actionMenu.getItems().add(groupDTITem);
    groupDTITem.setOnAction(e -> {
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          new DTGroupParams(),
          null,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.DT_GROUPING),
          "Group data points to increase dwell time.",
          FxStageButton.RUN,
          600d,
          350d
      );

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
        List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
        WorkingTask task = new DTGroupTask(dtGroupParams, selSamples, selIsotopes);
        importTasks.add(task);

        BatchTask parallel = new SimpleParallelBatch("DT grouping", importTasks, false,
            () -> SpTool3Main.getRunTime().getMainWindowCtl().updateSampleSets());
        SpTool3Main.getRunTime().getTaskManager().queueToHousekeepingPool(parallel);
      }
    });
  }

  public static void makeTimeRoiMenuItem(Menu menu) {
    MenuItem groupDTITem = new MenuItem("Cut time region");
    menu.getItems().add(groupDTITem);
    groupDTITem.setOnAction(e -> {
      ExecuteSubmethod executeSubmethod = new ExecuteSubmethod(
          new TimeRoiParams(),
          null,
          AvailableParameterSets.getOptionAsList(AvailableParameterSets.TIME_ROI),
          "Cut time region.",
          FxStageButton.RUN,
          600d,
          350d
      );

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

        List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSamples();
        WorkingTask task = new TimeRoiTask(method, selSamples);
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

    item.setOnAction(e -> {

      List<PopulationID> selIDs = SpTool3Main.getRunTime().getMainWindowCtl().getSelPops();
      List<Sample> selSamples = SpTool3Main.getRunTime().getMainWindowCtl().getSelSamples();
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();

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

          for (Isotope selIsotope : selIsotopes) {

            summary.append("\n\nIsotope: ").append(selIsotope.getName());

            List<Event> simEvts = selSample.getNPEvents(selIsotope, simIDs.get(0));

            for (PopulationID evalID : evalIDs) {
              List<Event> evalEvts = selSample.getNPEvents(selIsotope, evalID);

              List<Event> matchFromSim = new ArrayList<>();
              List<Event> matchFromEval = new ArrayList<>();
              List<Event> noMatchSim = new ArrayList<>();
              List<Event> noMatchEval = new ArrayList<>();

              AlphaBetaEvaluation.checkEvents(simEvts, evalEvts, matchFromSim, matchFromEval, noMatchSim,
                  noMatchEval);

              // "only in eval", i.e., false positive
              double alpha = 100.0 * ((double) noMatchEval.size() / simEvts.size());
              // with respect to BG: dp in events / total dp
              int nDP = selSample.getTotalDataPoints(selIsotope);
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
                  .append("\nalpha pts [%] :").append(SnF.doubleToString(alphaBG, NF.D1C3, NF.D1C3Exp))

                  .append("\nnSim [-] :").append(SnF.intToString(simEvts.size(), NF.D1C0))
                  .append("\nnEval [-] :").append(SnF.intToString(evalEvts.size(), NF.D1C0))
                  .append("\nnMatch [-] :").append(SnF.intToString(matchFromSim.size(), NF.D1C0))
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

          List<Isotope> matchedIsotope = new ArrayList<>();
          List<Isotope> unMatchedIsotope = new ArrayList<>();

          for (PopulationID selID : selIDs) {

            for (Isotope selIsotope : selIsotopes) {
              List<Isotope> singletonList = new ArrayList<>();
              singletonList.add(selIsotope);
              List<PopulationID> subSamplePops = selSample.listPopulations(singletonList);

              if (subSamplePops.contains(selID)) {
                matchedIsotope.add(selIsotope);
              } else {
                unMatchedIsotope.add(selIsotope);
              }
            }

            for (Isotope isotope : matchedIsotope) {
              summary.append("\nPopulation: ").append(selID.toString()).append(" CONTAINS ").append(isotope.getName());
            }
            for (Isotope isotope : unMatchedIsotope) {
              summary.append("\nPopulation: ").append(selID.toString()).append(" does not contain ").append(isotope.getName());
            }

          }
          info.add(summary.toString());
        }
      }
      ViewListDialog<String> view = getStringViewListDialog(info);
      view.showAndWait();

    });
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


}