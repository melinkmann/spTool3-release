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

package gui.listAndSearch;

import analysis.*;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.Trace;
import dataModelNew.fxImpl.FxSample;
import dataModelNew.mz.Element;
import dataModelNew.mz.MZValue;
import gui.dialog.FxEntry;
import gui.dialog.FxEntryFactory;
import gui.dialog.FxEntryFactory.SampleSetEntryFactory;
import gui.dialog.ListContainer;
import gui.dialog.mainImpl.ViewListDialog;
import gui.dialog.notification.NotificationFactory;
import gui.table.TableFactory;
import gui.util.UiUtil;
import gui.viewerCells.SampleSetListCell;
import io.SampleSet;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.nu.IsotopePtoeDialog;
import javafx.animation.PauseTransition;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Isotope;
import util.ArrUtils;
import util.ClipboardUtils;
import util.NF;
import util.SnF;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;
import visualizer.styles.CustomColorPicker;

public class SampleListAndTable {

  // (1) SampleSetList

  // (2) Sample Table

  // (3) TraceTable
  /*
  Element, MZ; maybe: Drift, Population Popup-ListView
   */

  protected final PauseTransition slowPause = new PauseTransition(Duration.seconds(0.5));
  private final PauseTransition isotopePause = new PauseTransition(Duration.millis(200));
  private final PauseTransition populationPause = new PauseTransition(Duration.millis(200));

  private static final Logger LOGGER = LogManager.getLogger(SampleListAndTable.class);

  private final ListView<FxEntry<SampleSet>> sampleSetListView;
  private final TextField sampleSetSearchField = new TextField();

  // Editable (nick name, comment)
  private final TableView<FxSample> sampleTableView;
  private final TextField sampleSearchField = new TextField();

  // Not editable!
  private final TableView<Isotope> isotopeTableView;
  private final TextField isotopeSearchField = new TextField();
  private final List<Isotope> prevSelIsotopes = new ArrayList<>();

  private final Text sampleSizeTxt = new Text("0 / 0");
  private final StackPane sampleViewStackPane = new StackPane();

  private final FxEntryFactory<SampleSet> sampleSetFactory = new SampleSetEntryFactory();

  private final ListView<PopulationID> populationListView;
  private final List<PopulationID> lockedPopulations = new ArrayList<>();
  private final TextField populationSearchField = new TextField();

  /*
   FLAG: I fired, (if you fire, you will first ignore and then clear the flag;
   if you fire and there is no flag, you can go!
   */
  private final AtomicBoolean sampleChangeActive = new AtomicBoolean(false);
  private final AtomicBoolean populationIsBlocked = new AtomicBoolean(false);


  public SampleListAndTable(
      ListView<FxEntry<SampleSet>> view,
      TableView<FxSample> sampleTableView,
      TableView<Isotope> isotopeTableView,
      ListView<PopulationID> populationListView,
      SelectionMode sampleSetSelectionMode,
      boolean isEditable) {

    this.sampleSetListView = view;

    this.sampleTableView = sampleTableView;

    this.isotopeTableView = isotopeTableView;

    this.populationListView = populationListView;

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // // // // // SAMPLE SET VIEW // // // // // // // // // // // // // // // // // // // // // //
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

    // General features
    ClipboardUtils.installCopyHandler(sampleSetListView);

    // Callback to make this react to changes of the labelProperty
    //https://stackoverflow.com/questions/28285507/is-there-a-way-to-bind-the-content-of-a-listproperty-in-javafx
    this.sampleSetListView.setItems(FXCollections.observableArrayList(
        fxEntry -> new Observable[]{
            fxEntry.getCellLabelProperty()
        }));

    // Fill Sample Sets from the register
    sampleSetListView.getItems().addAll(sampleSetFactory.create(
        new ArrayList<>(SpTool3Main.getRunTime().getSampleReg().getAllSets())));

    sampleSetListView.getSelectionModel().setSelectionMode(sampleSetSelectionMode);

    // Sort the List View
    UiUtil.sortListAndSearchView(sampleSetListView);
    // Set first AFTER sorting
    sampleSetListView.getSelectionModel().selectFirst();

    // Use default implementation of Selectable List Cell
    sampleSetListView.setCellFactory(l -> new SampleSetListCell(this, sampleTableView));
    sampleSetListView.setEditable(isEditable);

    sampleSetListView.getSelectionModel().selectedItemProperty().addListener(
        (observable, oldValue, nV) -> filterSamples());

    // Context Menu
    provideContextMenu(sampleSetListView);
    addAddMenu();
    // addRemoveMenu(); -> moved to the TableFactory which is called from SampleListAndTable
    // addDeleteSampleMenu(); -> moved to the TableFactory which is called from SampleListAndTable
    addDeleteSetMenu();

    // Fill
    filterSampleSets();

    /*
    Stuff concerning the TextField
     */

    sampleSetSearchField.setEditable(true);
    sampleSetSearchField.setPromptText("Search for sets");
    // Change listener: what we do is defined in the pause
    slowPause.setOnFinished(event -> filterSampleSets());
    sampleSetSearchField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        slowPause.stop();
        slowPause.playFromStart();
      }
    });

    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    // SAMPLE TABLE
    // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
    sampleSearchField.setPromptText("Search for samples");

    sampleSearchField.textProperty().addListener(new ChangeListener<String>() {
      @Override
      public void changed(ObservableValue<? extends String> observable, String oldValue,
                          String newValue) {
        filterSamples();
      }
    });

    sampleSizeTxt.setStyle("-fx-font-size: 10");

    //     isotopeTableView.getSelectionModel().getSelectedCells().addListener(
    // Listener to refresh the size of sampleTableView text field
    sampleTableView.getSelectionModel().getSelectedIndices().addListener(
        new ListChangeListener<Integer>() {
          @Override
          public void onChanged(Change<? extends Integer> c) {
            fireSampleChange();
          }
        });

    // Change listener: what we do is defined in the pause
    populationPause.setOnFinished(e -> {
      // refresh the UI -> tell the manager that change occurred
      SpTool3Main.getRunTime().getGuiParameterManager()
          .notifySampleOrPopulationSelectionChange();
      updateSampleTableValues();
    });

    populationListView.getSelectionModel().getSelectedIndices().addListener(
        new ListChangeListener<Integer>() {
          @Override
          public void onChanged(Change<? extends Integer> c) {
            if (!sampleChangeActive.get() && !populationIsBlocked.get()) {
              populationPause.stop(); // restart debounce timer
              populationPause.playFromStart();
            }
          }
        });

    // Set a custom cell factory:
    populationListView.setCellFactory(lv -> new ListCell<>() {
      @Override
      protected void updateItem(PopulationID item, boolean empty) {

        // Can be reused, text is updated
        // final Tooltip tooltip = UiUtil.getDefaultStyleTooltip();

        super.updateItem(item, empty);
        if (empty || item == null) {
          setText(null);
          setGraphic(null); // reset graphic if not locked, else random symbol appear
          //setTooltip(null);
        } else {
          setText(item.toString());
          //tooltip.setText(String.valueOf(getItem().getOrder()));
          //setTooltip(tooltip);
          if (lockedPopulations.contains(item)) {
            ImageView view = UiUtil.getViewer("/img/lock.png");
            this.setGraphic(view);
          } else {
            setGraphic(null); // reset graphic if not locked, else random symbol appear
          }
          // check if no sample contains
          boolean anySampleContains;
          List<Sample> selSamples = getSelSamples();
          List<Isotope> selIsotopes = getSelIsotopes();
          List<PopulationID> availableIDs = AnalysisUtils.listPopulations(selSamples,
              selIsotopes);
          anySampleContains = availableIDs.contains(item);
          if (!anySampleContains) {
            ImageView view = UiUtil.getViewer("/img/softIssue.png");
            setGraphic(view);
          }
        }
      }
    });

    provideContextMenu(populationListView);
    UiUtil.installDoubleClickSelect(populationListView);
    addLockMenu();
    addUnlockMenu();
    addDeleteMenu();
    addViewMZMenu();

    populationListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    // Change listener: what we do sits in the pause
    isotopePause.setOnFinished(e -> {
      // I think we should call this as some isotopes may be empty, i.e, they do not have certain
      // populations
      fillAndReselectPopulations();

      // update current selection
      if (!isotopeTableView.getSelectionModel().getSelectedIndices().isEmpty()) {
        prevSelIsotopes.clear();
        prevSelIsotopes.addAll(isotopeTableView.getSelectionModel().getSelectedItems());
      }

      // These are obsolete if population reselect triggers,
      // but make sure that sth was reselected at all. If not, likely no Pop found yet.
      // In that case refresh the UI -> tell the manager that change occurred
      if (populationListView.getItems().isEmpty()) {
        SpTool3Main.getRunTime().getGuiParameterManager()
            .notifySampleOrPopulationSelectionChange();
        updateSampleTableValues();
      }
    });

    // change triggers pause
    isotopeTableView.getSelectionModel().getSelectedCells().addListener(
        new ListChangeListener<TablePosition>() {
          @Override
          public void onChanged(Change<? extends TablePosition> c) {
            if (!sampleChangeActive.get()) {
              isotopePause.stop(); // restart debounce timer
              isotopePause.playFromStart();
            }
          }
        });

    // Layout AND empty list
    TableFactory.setupSampleTable(sampleTableView,
        this, sampleSearchField);

    TableFactory.setupIsotopeTable(isotopeTableView);
    // this is the cleaner version of double click select:
    isotopeTableView.setOnMouseClicked(new EventHandler<MouseEvent>() {
      @Override
      public void handle(MouseEvent click) {
        // @ double click
        if (click.getClickCount() == 2) {
          if (click.isControlDown()) {
            isotopeTableView.getSelectionModel().clearSelection();
            getSampleDefaultIsotopes().forEach(iso -> {
              isotopeTableView.getSelectionModel().select(iso);
            });
          } else {
            selectDefaultIsotopes();
          }
        }
      }
    });

    provideContextMenu(isotopeTableView);
    addSelectMostAbundantMenu();
    addSelectAllMenu();
    addDefaultList();
    addIsotopeColorPicker();
    addIsotopeRemover();
  }
  // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //
  // Methods
  // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // // //

  private List<Isotope> getSampleDefaultIsotopes() {
    Set<Isotope> isotopes = new HashSet<>();
    for (Sample selSample : getSelSamples()) {
      isotopes.addAll(selSample.getSampleDefaultIsotopes());
    }
    return new ArrayList<>(isotopes);
  }

  private void provideContextMenu(Control control) {
    if (control.getContextMenu() == null) {
      control.setContextMenu(new ContextMenu());
    }
  }

  public void addDeleteSetMenu() {
    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Delete Set", "/img/delete.png");
    notFavouriteMenu.setOnAction(e ->
        NotificationFactory.openYesCancel("Delete? This is irreversible.", () -> {
          List<SampleSet> sets = sampleSetListView.getSelectionModel().getSelectedItems().stream()
              .filter(s -> s.unwrap() != null)
              .map(FxEntry::unwrap)
              .collect(Collectors.toList());
          SpTool3Main.getRunTime().getSampleReg().removeSetDirectly(sets);
          filterSampleSets();
        }));
    sampleSetListView.getContextMenu().getItems().add(new SeparatorMenuItem());
    sampleSetListView.getContextMenu().getItems().add(notFavouriteMenu);
    sampleSetListView.getContextMenu().getItems().add(new SeparatorMenuItem());
  }

  public void addAddMenu() {
    MenuItem notFavouriteMenu = UiUtil.getImageMenuItem("Create", "/img/create.png");
    notFavouriteMenu.setOnAction(e -> {
      SampleSet set = new SampleSet();
      SpTool3Main.getRunTime().getSampleReg().addSetDirectly(set);
      filterSampleSets();
    });
    sampleSetListView.getContextMenu().getItems().add(notFavouriteMenu);
  }

  public void addLockMenu() {
    MenuItem lock = UiUtil.getImageMenuItem("Lock", "/img/lock.png");
    lock.setOnAction(e -> {
      // Avoid duplicates
      for (PopulationID selPop : populationListView.getSelectionModel().getSelectedItems()) {
        if (!lockedPopulations.contains(selPop)) {
          lockedPopulations.add(selPop);
        }
      }
      populationListView.refresh(); // show lock symbol immediately
    });
    populationListView.getContextMenu().getItems().add(lock);
  }

  public void addUnlockMenu() {
    MenuItem unlock = UiUtil.getImageMenuItem("Unlock", "/img/unlock.png");
    unlock.setOnAction(e -> {
      lockedPopulations.removeAll(populationListView.getSelectionModel().getSelectedItems());
      fillAndReselectPopulations(); // when previously locked was removed and has to go
      populationListView.refresh();// show lock symbol immediately
    });
    populationListView.getContextMenu().getItems().add(unlock);
  }

  public void addDeleteMenu() {
    MenuItem delete = UiUtil.getImageMenuItem("Delete", "/img/delete.png");
    delete.setOnAction(e -> {

      NotificationFactory.openYesCancel("Delete population(s)? This is irreversible.", () -> {

        List<PopulationID> selIDs = getSelPopulations();
        List<Sample> selSamples = getSelSamples();
        List<Isotope> selIsotopes = getSelIsotopes();
        for (Sample selSample : selSamples) {
          for (PopulationID selID : selIDs) {
            selSample.removePopulations(selIsotopes, selID);
          }
        }
        fillAndReselectPopulations(); // when previously locked was removed and has to go
      });

    });
    populationListView.getContextMenu().getItems().add(delete);
  }

  public void addViewMZMenu() {
    MenuItem show = UiUtil.getImageMenuItem("View mz", "/img/search.png");
    show.setOnAction(e -> {

      /*
       TODO
        - create function "getExcludedMZ()" which does the all.remove(contributing) call
        - tidy this up a bit maybe by making one function that creates the string (recycle for export too)
       */

      List<PopulationID> selIDs = getSelPopulations();
      List<Sample> selSamples = getSelSamples();

      List<String> info = new ArrayList<>();

      for (Sample selSample : selSamples) {
        // in case of merged samples
        for (Sample s : selSample.getAllSamples()) {
          List<Trace> tracesInSample = s.getTraces();
          for (PopulationID id : selIDs) {
            List<MZValue> excludedMZ = tracesInSample.stream()
                .map(Trace::getMzValue)
                .collect(Collectors.toList());
            for (Trace trace : tracesInSample) {
              Population pop = trace.getPopulation(id);
              if (pop != null && !pop.getContributingMZs().isEmpty()) {
                StringBuilder str = new StringBuilder();
                str.append("Nick name: ").append(s.getNickName())
                    .append("\n\t\tPopulation: ").append(pop.getId().toString())
                    .append("\n\t\tContributing MZ:");
                for (MZValue contributingMZ : pop.getContributingMZs()) {
                  str.append("\n\t\t").append(contributingMZ.getName());
                }
                if (pop.getContributingMZs().isEmpty()) {
                  str.append("\n\t\tNone contribute.");
                }

                excludedMZ.removeAll(pop.getContributingMZs());
                str.append("\n\t\tExcluded MZ:");
                for (MZValue excluded : excludedMZ) {
                  str.append("\n\t\t").append(excluded.getName());
                }
                if (excludedMZ.isEmpty()) {
                  str.append("\n\t\tNone excluded.");
                }

                info.add(str.toString());
                // For the same ID, due to alignment, finding one trace is enough.
                break;
              }
            }
          }
        }
      }

      ListContainer<String> container = new ListContainer<String>() {
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


      view.showAndWait();
    });
    populationListView.getContextMenu().getItems().add(show);
  }

  public StackPane getViewAndLengthPane() {
    if (!sampleViewStackPane.getChildren().contains(sampleSizeTxt)) {
      sampleViewStackPane.getChildren().addAll(sampleTableView, sampleSizeTxt);
      StackPane.setAlignment(sampleSizeTxt, Pos.BOTTOM_RIGHT);
      StackPane.setMargin(sampleSizeTxt, new Insets(0, 22, 5, 0));
    }
    return sampleViewStackPane;
  }

  private void updateSizeText() {
    String size =
        SnF.doubleToString(sampleTableView.getSelectionModel().getSelectedItems().size(), NF.D1C0)
            + " / "
            + SnF.doubleToString(sampleTableView.getItems().size(), NF.D1C0);
    sampleSizeTxt.setText(size);
  }

  public void addListenerForSize() {
    sampleTableView.getItems().addListener(
        (ListChangeListener<FxSample>) c -> updateSizeText());
  }

  // Easier access in sub classes
  public void filterSampleSets() {
    List<SampleSet> allSets = new ArrayList<>(SpTool3Main.getRunTime().getSampleReg().getAllSets());
    List<FxEntry<SampleSet>> allEntries = sampleSetFactory.create(allSets);
    // remember selected samples
    List<FxSample> prevSelSamples = new ArrayList<>(sampleTableView.getSelectionModel()
        .getSelectedItems());

    UiUtil.filterListAndSearchViewEntries(sampleSetSearchField, sampleSetListView, allEntries);
    UiUtil.reselect(sampleTableView, prevSelSamples);

    // fill Populations
    fillAndReselectPopulations();
  }

  public void filterSamples() {

    // get the selected sample sets
    List<SampleSet> selSets = sampleSetListView.getSelectionModel().getSelectedItems().stream()
        .filter(Objects::nonNull)
        .map(FxEntry::unwrap)
        .collect(Collectors.toList());

    List<Sample> allSamples = new ArrayList<>();

    // Make a List of all unique samples in the currently selected sets
    for (SampleSet selSet : selSets) {
      List<Sample> samplesInSet = selSet.getSamples();
      for (Sample sample : samplesInSet) {
        // Avoid duplicates
        if (!allSamples.contains(sample)) {
          allSamples.add(sample);
        }
      }
    }

    // get previously selected samples in the table
    List<Sample> prevSel = sampleTableView.getSelectionModel().getSelectedItems().stream()
        .map(FxSample::getPlainSample)
        .collect(Collectors.toList());

    // Filter by the sample name
    String searchText = sampleSearchField.getText();
    if (searchText.isEmpty()) {
      sampleTableView.getItems().clear();
      // Add all available samples
      sampleTableView.getItems().addAll(allSamples.stream()
          .map(FxSample::new)
          .collect(Collectors.toList()));
    } else {
      sampleTableView.getItems().clear();
      // Add filtered samples
      sampleTableView.getItems().addAll(allSamples.stream()
          .filter(s -> UiUtil.searchStringIncludeNot(searchText, s.getNickName()))
          .map(FxSample::new)
          .collect(Collectors.toList()));
    }

    // sort according to nick name
    TableColumn<FxSample, ?> retrievedColumn = null;
    for (TableColumn<FxSample, ?> column : sampleTableView.getColumns()) {
      if ("Sample nick name".equals(column.getText())) {
        retrievedColumn = column;
      }
    }

    if (retrievedColumn != null) {
      retrievedColumn.setSortType(TableColumn.SortType.ASCENDING);
      sampleTableView.getSortOrder().add(retrievedColumn); // Add the column to the sort order
      sampleTableView.sort(); // Apply the sort
    }

   /*
    Restore original selection only if fewer than 1000 instances or we get really slow.
    Also see comment below regarding the select() method!
    Why indices? Reselection using prevSel.forEach(listView.getSelectionModel()::select);
    becomes quite slow at large list sizes, likely due to indexOf() calls or sth like that.
    Hence: https://stackoverflow.com/questions/44981917/how-do-i-program-select-multiple-items-in-a
    -listview-in-javafx
    */
    List<Integer> prevSelIndices = new ArrayList<>();
    if (sampleTableView.getSelectionModel().getSelectedItems().size() < 1000) {
      // Contains is probably fastest on HashSets.
      HashSet<Sample> previouslySelected = new HashSet<>(prevSel);
      for (int i = 0; i < sampleTableView.getItems().size(); i++) {
        Sample sample = sampleTableView.getItems().get(i).getPlainSample();
        if (previouslySelected.contains(sample)) {
          prevSelIndices.add(i);
        }
      }
    }
    sampleTableView.getSelectionModel()
        .selectIndices(-1, ArrUtils.integerListToArr(prevSelIndices));

    updateSampleTableValues();
  }

  public void fireSampleChange() {
    // set flag that change comes from here
    sampleChangeActive.set(true);
    // update size

    updateSizeText();

    // fill isotope table

    // keep previous selection: via global list ---// List<Isotope> prevSelIsotopes = getSelIsotopes();

    // clear
    isotopeTableView.getItems().clear();

    List<Sample> selSamples = getSelSamples();

    List<Isotope> isotopes = AnalysisUtils.listIsotopes(selSamples);

    // TODO: Filter these according to the "fill()" method using a Selectable FxEntry
    //  and also the textfield.
    isotopeTableView.getItems().addAll(isotopes);

    // sort according to isotope
    TableColumn<Isotope, ?> retrievedColumn = null;
    for (TableColumn<Isotope, ?> column : isotopeTableView.getColumns()) {
      if ("Isotope".equals(column.getText())) {
        retrievedColumn = column;
      }
    }

    if (retrievedColumn != null) {
      retrievedColumn.setSortType(TableColumn.SortType.ASCENDING);
      // Add the column to the sort order
      isotopeTableView.getSortOrder().add(retrievedColumn);
      // Apply the sort
      isotopeTableView.sort();
    }

    // reselect
    List<Integer> prevSelIndices = new ArrayList<>();
    List<Isotope> availableIsotopes = new ArrayList<>(isotopeTableView.getItems());
    if (!prevSelIsotopes.isEmpty()) {
      for (int i = 0; i < availableIsotopes.size(); i++) {
        Isotope isotope = availableIsotopes.get(i);
        if (prevSelIsotopes.contains(isotope)) {
          prevSelIndices.add(i);
        }
      }
    }

    // select "always selected": This must be added here since below selectDefaultIsotopes();
    // only fires if there is no prevSel --> MOVED THIS TO CTL DOUBLE CLICK
    // if (!pseSelIsotopes.isEmpty()) {
    //   for (int i = 0; i < availableIsotopes.size(); i++) {
    //     Isotope isotope = availableIsotopes.get(i);
    //     if (pseSelIsotopes.contains(isotope) && !prevSelIndices.contains(i)) {
    //       prevSelIndices.add(i);
    //     }
    //   }
    // }

//    isotopeTableView.getSelectionModel()
//        .selectIndices(-1, ArrUtils.integerListToArr(prevSelIndices));

    // Clear previous selection
    isotopeTableView.getSelectionModel().clearSelection();

    // Select previously selected indices
    if (!prevSelIndices.isEmpty()) {
      // Select previously selected indices using a normal for loop
      for (int index : prevSelIndices) {
        if (isotopeTableView.getItems().size() > index) {
          isotopeTableView.getSelectionModel().select(index);
        }
      }
    } else {
      // else, select default case or else most abundant
      selectDefaultIsotopes();
    }


    // then based on selection, fill the population
    fillAndReselectPopulations();

    updateSampleTableValues();

    // refresh the UI -> tell the manager that change occurred
    SpTool3Main.getRunTime().getGuiParameterManager()
        .notifySampleOrPopulationSelectionChange();

    // Else, table fires results table and graphs twice
    sampleChangeActive.set(false);

  }

  public void updateSampleTableValues() {
    List<FxSample> samples = new ArrayList<>(sampleTableView.getItems());
    List<PopulationID> allPops = new ArrayList<>(populationListView.getItems());
    List<PopulationID> selPops = new ArrayList<>(populationListView.getSelectionModel()
        .getSelectedItems());
    List<Isotope> allIsotopes = new ArrayList<>(isotopeTableView.getItems());
    List<Isotope> selIsotopes = new ArrayList<>(isotopeTableView.getSelectionModel()
        .getSelectedItems());

    // Easy to compute
    for (FxSample fxSample : samples) {
      Sample sample = fxSample.getPlainSample();
      double nEvt = sample.getAverageNoOfEvents(selIsotopes, selPops);
      fxSample.setEventCount((int) nEvt);
    }

    // Time-consuming but we prepare the Drift factor at the multi-threaded processing
    for (FxSample fxSample : samples) {
      Sample sample = fxSample.getPlainSample();
      double drift = sample.getAverageDrift(allIsotopes, allPops);
      fxSample.setDriftFactor(drift);
    }
  }

  private void addSelectMostAbundantMenu() {
    MenuItem menu = UiUtil.getImageMenuItem("Default", "/img/mostAbundant.png");
    menu.setOnAction(e -> {
      selectDefaultIsotopes();
    });
    isotopeTableView.getContextMenu().getItems().add(menu);
  }

  private void addSelectAllMenu() {
    MenuItem menu = UiUtil.getImageMenuItem("Select all", "/img/grouped.png");
    menu.setOnAction(e -> {
      selectAllIsotopes();
    });
    isotopeTableView.getContextMenu().getItems().add(menu);
  }

  private void addIsotopeRemover() {
    MenuItem menu = UiUtil.getImageMenuItem("Delete isotopes", "/img/delete.png");
    menu.setOnAction(e -> {
      NotificationFactory.openYesCancel("""
          Delete isotope(s)?
          This is irreversible.""", () -> {

        List<Isotope> selIsotopes = getSelIsotopes();
        List<Sample> selSamples = getAllSamples();
        for (Sample selSample : selSamples) {
          selSample.removeIsotopes(selIsotopes);
        }
        prevSelIsotopes.clear(); // force reselect from default: else, index-based reselection fails
        fireSampleChange(); // essentially, refresh all!
      });

    });
    isotopeTableView.getContextMenu().getItems().add(new SeparatorMenuItem());
    isotopeTableView.getContextMenu().getItems().add(menu);
  }

  private void addDefaultList() {
    MenuItem menu = UiUtil.getImageMenuItem("Fix isotopes", "/img/tableTrace.png");
    menu.setOnAction(e -> {

      List<Isotope> sampleDefaultIsotopes = getSampleDefaultIsotopes();

      // needed for the PTOE popup
      Window owner = menu.getParentPopup().getOwnerWindow();
      Stage parent = null;
      if (owner != null) {
        parent = (Stage) owner;
      }

      IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
          parent,
          dataModelNew.mz.Element.getAllIsotopes(),   // all isotopes available
          sampleDefaultIsotopes);                  // null or empty = open blank

      List<MZValue> resultingMZ = dlg.showAndWait();
      if (resultingMZ != null) {
        List<Isotope> resultingIsotopes = new ArrayList<>();
        for (MZValue mzValue : resultingMZ) {
          resultingIsotopes.add(mzValue.getIsotope());
        }
        sampleDefaultIsotopes.clear();
        sampleDefaultIsotopes.addAll(resultingIsotopes);
      }
      // set to samples
      for (Sample selSample : getSelSamples()) {
        selSample.setSampleDefaultIsotopes(sampleDefaultIsotopes);
      }

      fireSampleChange(); // refresh
    });
    isotopeTableView.getContextMenu().getItems().add(new SeparatorMenuItem());
    isotopeTableView.getContextMenu().getItems().add(menu);
  }

  private void selectDefaultIsotopes() {
    // Deselect all, else the right-clicked isotope remains.
    isotopeTableView.getSelectionModel().clearSelection();
    List<Element> elements = isotopeTableView.getItems().stream()
        .map(Isotope::getElement)
        .distinct()
        .collect(Collectors.toList());

    List<Isotope> defaultIsotopes = new ArrayList<>();
    for (Element element : elements) {
      Isotope defaultIsotope = SpTool3Main.getRunTime().getConfParams().getDefaultIsotope(element);

      // Check if isotope is present in table. If not, go through by abundance
      if (isotopeTableView.getItems().contains(defaultIsotope)) {
        defaultIsotopes.add(defaultIsotope);
      } else {
        // by abundance
        List<Isotope> allIsoOfEle = element.getIsotopes();
        allIsoOfEle.sort(Comparator.comparingDouble(Isotope::getAbundance));
        Collections.reverse(allIsoOfEle); // select MOST abundant not LEAST
        for (Isotope isotope : allIsoOfEle) {
          if (isotopeTableView.getItems().contains(isotope)) {
            defaultIsotopes.add(isotope);
            break;
          }
        }
      }
    }

    // check if the always on top isotopes are also to be selected. Works but feels a bit annoying.
    // for (Isotope alwaysOnTopIsotope : alwaysOnTopIsotopes) {
    //   if (!defaultIsotopes.contains(alwaysOnTopIsotope)) {
    //     defaultIsotopes.add(alwaysOnTopIsotope);
    //   }
    // }

    defaultIsotopes.forEach(iso -> {
      isotopeTableView.getSelectionModel().select(iso);
    });
  }

  private void selectAllIsotopes() {
    // Deselect all, else the right-clicked isotope remains.
    isotopeTableView.getSelectionModel().selectAll();
  }


  public void addIsotopeColorPicker() {
    MenuItem view = UiUtil.getImageMenuItem("Color", "/img/pickColor.png");
    isotopeTableView.getContextMenu().getItems().add(view);

    view.setOnAction(e -> {
      if (!isotopeTableView.getSelectionModel().getSelectedItems().isEmpty()) {
        Isotope isotope = isotopeTableView.getSelectionModel().getSelectedItems().get(0);
        Colors currentColor = SpTool3Main.getRunTime().getConfParams().getColor(isotope);
        CustomColorPicker custom = new CustomColorPicker(
            currentColor.getFX(),
            Colors.getDefaultColors(),
            new Consumer<Color>() {
              @Override
              public void accept(Color color) {
                SpTool3Main.getRunTime().getConfParams().setColor(isotope, new SpColor(color));
              }
            });
        custom.show();
      }
    });
  }


  public void fillAndReselectPopulations() {

    // prevents refresh from "clear"
    populationIsBlocked.set(true);

    // previous selection, then clear
    List<PopulationID> prevSel = getSelPopulations();

    // locked  selection
    prevSel.addAll(lockedPopulations);

    // Always add locked pops
    HashSet<PopulationID> allPopulationIDs = new LinkedHashSet<>(lockedPopulations);

    List<Sample> selSamples = getSelSamples();
    List<Isotope> selIsotopes = getSelIsotopes();

    // get all population IDs
    allPopulationIDs.addAll(AnalysisUtils.listPopulations(selSamples, selIsotopes));

    // Note: LinkedHashSet cannot sort, but insertion order is maintained!
    List<PopulationID> uniquePopulations = new ArrayList<>(allPopulationIDs);

    // Sort
    uniquePopulations.sort(Comparator.comparingDouble(PopulationID::getOrder));

    populationListView.getItems().clear();
    // Allow change firing from new selection
    populationIsBlocked.set(false);
    populationListView.getItems().addAll(uniquePopulations);

    populationListView.getSelectionModel().clearSelection();
    for (PopulationID id : prevSel) {
      populationListView.getSelectionModel().select(id);
    }

    if (populationListView.getSelectionModel().getSelectedItems().isEmpty()) {
      populationListView.getSelectionModel().selectFirst();
    }
  }

  public void incrementSelectedSample() {
    int prevSelIdx = sampleTableView.getSelectionModel().getSelectedIndex();
    int selIdx = prevSelIdx;
    selIdx--;
    if (selIdx < 0) {
      selIdx = sampleTableView.getItems().size() - 1;
    }
    try {
      sampleTableView.getSelectionModel().clearSelection();
      sampleTableView.getSelectionModel().select(selIdx);
      // Force scroll to selected row
      sampleTableView.scrollTo(selIdx);
    } catch (Exception e) {
      sampleTableView.getSelectionModel().select(prevSelIdx);
      LOGGER.error("Cannot increment sample index. " +
          "Message: " + ExceptionUtils.getMessage(e) +
          " Message: " + ExceptionUtils.getStackTrace(e)
      );
    }
  }

  public void decrementSelectedSample() {
    int prevSelIdx = sampleTableView.getSelectionModel().getSelectedIndex();
    int selIdx = prevSelIdx;
    selIdx++;
    if (selIdx >= sampleTableView.getItems().size()) {
      selIdx = 0;
    }
    try {
      sampleTableView.getSelectionModel().clearSelection();
      sampleTableView.getSelectionModel().select(selIdx);
      // Force scroll to selected row
      sampleTableView.scrollTo(selIdx);
    } catch (Exception e) {
      sampleTableView.getSelectionModel().select(prevSelIdx);
      LOGGER.error("Cannot increment sample index. " +
          "Message: " + ExceptionUtils.getMessage(e) +
          " Message: " + ExceptionUtils.getStackTrace(e)
      );
    }
  }

  public ListView<FxEntry<SampleSet>> getSampleSetListView() {
    return sampleSetListView;
  }

  public TextField getSearchFld() {
    return sampleSetSearchField;
  }

  public TableView<FxSample> getSampleTableView() {
    return sampleTableView;
  }

  //
  public List<PopulationID> getSelPopulations() {
    return new ArrayList<>(populationListView.getSelectionModel()
        .getSelectedItems());
  }

  public List<Isotope> getSelIsotopes() {
    return new ArrayList<>(isotopeTableView.getSelectionModel()
        .getSelectedItems());
  }

  public List<Sample> getSelSamples() {
    return sampleTableView.getSelectionModel().getSelectedItems().stream()
        .map(FxSample::getPlainSample)
        .collect(Collectors.toList());
  }

  public List<PopulationID> getAllPopulations() {
    return new ArrayList<>(populationListView.getItems());
  }

  public List<Isotope> getAllIsotopes() {
    return new ArrayList<>(isotopeTableView.getItems());
  }

  public List<Sample> getAllSamples() {
    return sampleTableView.getItems().stream()
        .map(FxSample::getPlainSample)
        .collect(Collectors.toList());
  }

}
