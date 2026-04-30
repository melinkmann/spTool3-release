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

/*
 *  Part of the Nu Instruments ICP-ToF import pipeline – spTool3.
 *
 *  Modal dialog presenting a periodic table for selecting which m/z channels
 *  to import from a Nu Instruments data directory.
 *
 *  Each element whose isotopes appear in the recorded m/z channels is shown
 *  as an active ToggleButton.  Elements absent from the data are shown as
 *  disabled gray labels.  Elements that share a nominal mass with another
 *  element (e.g. Ca-48 / Ti-48) are highlighted in yellow and start
 *  unselected; the user must right-click to resolve them.
 *
 *  Right-click on any active element opens an isotope picker.
 *  Double-clicking the isotope list selects all isotopes for that element.
 *
 *  Usage:
 *      NuReaderResult scan   = NuReader.readAvailableChannels(directory);
 *      List<Double>   chosen = new NuChannelSelectorDialog(owner, scan.mzValues)
 *                                  .showAndWait();   // null → cancelled
 *      if (chosen != null) {
 *          NuReaderResult full = NuReader.readSelectedChannels(directory, chosen);
 *      }
 */

package io.nu;

import core.SpTool3Main;
import dataModelNew.mz.Element;

import gui.util.UiUtil;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;
import javafx.util.Duration;
import sandbox.montecarlo.Isotope;
import util.NF;
import util.SnF;

import javax.annotation.Nullable;
import java.util.*;

public final class IsotopePtoeDialog {

  // -------------------------------------------------------------------------
  // Periodic table grid positions: { atomicNumber, row (1-based), col (1-based) }
  // Rows 1–7 = main table; rows 8–9 = lanthanide / actinide breakout
  // -------------------------------------------------------------------------
  private static final int[][] ELEMENT_GRID = {
      {1, 1, 1}, {2, 1, 18},
      {3, 2, 1}, {4, 2, 2},
      {5, 2, 13}, {6, 2, 14}, {7, 2, 15}, {8, 2, 16}, {9, 2, 17}, {10, 2, 18},
      {11, 3, 1}, {12, 3, 2},
      {13, 3, 13}, {14, 3, 14}, {15, 3, 15}, {16, 3, 16}, {17, 3, 17}, {18, 3, 18},
      {19, 4, 1}, {20, 4, 2},
      {21, 4, 3}, {22, 4, 4}, {23, 4, 5}, {24, 4, 6}, {25, 4, 7},
      {26, 4, 8}, {27, 4, 9}, {28, 4, 10}, {29, 4, 11}, {30, 4, 12},
      {31, 4, 13}, {32, 4, 14}, {33, 4, 15}, {34, 4, 16}, {35, 4, 17}, {36, 4, 18},
      {37, 5, 1}, {38, 5, 2},
      {39, 5, 3}, {40, 5, 4}, {41, 5, 5}, {42, 5, 6}, {43, 5, 7},
      {44, 5, 8}, {45, 5, 9}, {46, 5, 10}, {47, 5, 11}, {48, 5, 12},
      {49, 5, 13}, {50, 5, 14}, {51, 5, 15}, {52, 5, 16}, {53, 5, 17}, {54, 5, 18},
      {55, 6, 1}, {56, 6, 2},
      // Z=57 (La) is the lanthanide series placeholder in col 3
      {72, 6, 4}, {73, 6, 5}, {74, 6, 6}, {75, 6, 7}, {76, 6, 8},
      {77, 6, 9}, {78, 6, 10}, {79, 6, 11}, {80, 6, 12},
      {81, 6, 13}, {82, 6, 14}, {83, 6, 15}, {84, 6, 16}, {85, 6, 17}, {86, 6, 18},
      {87, 7, 1}, {88, 7, 2},
      // Z=89 (Ac) is the actinide series placeholder in col 3
      {104, 7, 4}, {105, 7, 5}, {106, 7, 6}, {107, 7, 7}, {108, 7, 8},
      {109, 7, 9}, {110, 7, 10}, {111, 7, 11}, {112, 7, 12},
      {113, 7, 13}, {114, 7, 14}, {115, 7, 15}, {116, 7, 16}, {117, 7, 17}, {118, 7, 18},
      // Lanthanides – row 8, cols 4–17
      {57, 8, 4}, {58, 8, 5}, {59, 8, 6}, {60, 8, 7}, {61, 8, 8}, {62, 8, 9},
      {63, 8, 10}, {64, 8, 11}, {65, 8, 12}, {66, 8, 13}, {67, 8, 14}, {68, 8, 15},
      {69, 8, 16}, {70, 8, 17}, {71, 8, 18},
      // Actinides – row 9, cols 4–17
      {89, 9, 4}, {90, 9, 5}, {91, 9, 6}, {92, 9, 7}, {93, 9, 8}, {94, 9, 9},
      {95, 9, 10}, {96, 9, 11}, {97, 9, 12}, {98, 9, 13}, {99, 9, 14}, {100, 9, 15},
      {101, 9, 16}, {102, 9, 17}, {103, 9, 18},
  };

  // CSS colour constants
  private static final String COLOR_ABSENT = "-fx-background-color:#e8e8e8; -fx-text-fill:#b0b0b0;";
  private static final String COLOR_NORMAL_OFF = "-fx-background-color:#8DEFAE; -fx-text-fill:#1a3a1a;";
  private static final String COLOR_NORMAL_ON = "-fx-background-color:#117733; -fx-text-fill:white;";
  private static final String COLOR_AMBIGUOUS_OFF = "-fx-background-color:#C7BFEF; -fx-text-fill:#3a2a00;";
  private static final String COLOR_AMBIGUOUS_ON = "-fx-background-color:#332288; -fx-text-fill:white;";
  private static final String BORDER_COMMON = "-fx-border-radius:3; -fx-background-radius:3;"
      + "-fx-border-color:#999; -fx-font-size:9;";

  // -------------------------------------------------------------------------
  // Lookup tables built once in the constructor
  // -------------------------------------------------------------------------

  /**
   * Rounded nominal mass → list of exact m/z values present in the data.
   */
  private final Map<Integer, List<Double>> mzByNominalMass;

  /**
   * Element → list of exact m/z values present in the data for that element.
   */
  private final Map<Element, List<Double>> mzByElement;

  /**
   * Nominal masses that are shared by more than one element in this dataset.
   */
  private final Set<Integer> ambiguousMasses;

  // -------------------------------------------------------------------------
  // Mutable selection state
  // -------------------------------------------------------------------------

  /**
   * Currently selected items keyed by Isotope so that isobaric pairs
   * (e.g. 48Ti and 48Ca) can both be present simultaneously.
   * Value is the exact recorded m/z for that isotope from the data file.
   */
  private final Map<Isotope, Double> selectedItems = new LinkedHashMap<>();

  /**
   * Isotopes selected in a previous import session, used to restore the
   * selection state when the dialog opens.  May be null or empty.
   */
  private final Set<Isotope> previousIsotopeSelection;

  /**
   * All active ToggleButtons keyed by element, so the "Select all" button
   * can update them programmatically after modifying selectedItems.
   */
  private final Map<Element, ToggleButton> toggleByElement = new LinkedHashMap<>();

  // -------------------------------------------------------------------------
  // Dialog infrastructure
  // -------------------------------------------------------------------------

  private final Stage stage;
  private List<MZValue> dialogResult = null;   // null = cancelled

  // =========================================================================
  // Constructor
  // =========================================================================

  /**
   * Builds and prepares the dialog.  Call {@link #showAndWait()} to display it.
   *
   * @param owner                    parent window
   * @param availableMz              sorted list of exact m/z values from
   *                                 {@link NuReader_v1#readAvailableMZ}
   * @param previousIsotopeSelection isotopes selected in a previous import session,
   *                                 used to restore state on open.  Pass null or
   *                                 an empty list to open with nothing selected.
   */
  /**
   * Case B constructor: available channels come from recorded m/z values in a
   * Nu Instruments data file (i.e. from {@link NuReader_v1#readAvailableMZ}).
   *
   * @param owner                    parent window
   * @param availableMz              sorted list of recorded m/z values
   * @param previousIsotopeSelection isotopes to pre-select; null or empty = open blank
   */
  public IsotopePtoeDialog(@Nullable Window owner, List<Double> availableMz,
                           List<Isotope> previousIsotopeSelection) {
    this(owner,
        buildMzByNominalMass(availableMz),
        buildMzByElement(buildMzByNominalMass(availableMz)),
        previousIsotopeSelection);
  }

  /**
   * Case A factory method: available channels are defined by an explicit list of
   * isotopes rather than recorded m/z values from a data file.
   *
   * <p>Each isotope is owned exclusively by its element using its theoretical
   * mass, so the nominal-mass grouping logic is bypassed entirely.  This avoids
   * the duplicate-entry problem that occurs when two isobaric isotopes (e.g.
   * 48Ti and 48Ca) both have theoretical masses that round to the same integer
   * and would otherwise both appear in each other's listview.
   *
   * <p>A static factory is used instead of a second constructor because Java
   * erases generic type parameters at runtime, making
   * {@code IsotopePtoeDialog(Window, List<Isotope>, List<Isotope>)} and
   * {@code IsotopePtoeDialog(Window, List<Double>, List<Isotope>)} identical
   * after erasure and therefore unresolvable by the compiler.
   *
   * <p>Call site:
   * <pre>{@code
   *   IsotopePtoeDialog dlg = IsotopePtoeDialog.forIsotopeSelection(
   *           owner, Element.getAllIsotopes(), prevSel);
   * }</pre>
   *
   * @param owner                    parent window
   * @param availableIsotopes        full set of isotopes to offer in the table
   * @param previousIsotopeSelection isotopes to pre-select; null or empty = open blank
   */
  public static IsotopePtoeDialog forIsotopeSelection(
      @Nullable Window owner,
      List<Isotope> availableIsotopes,
      List<Isotope> previousIsotopeSelection) {

    return new IsotopePtoeDialog(
        owner,
        buildMzByNominalMassFromIsotopes(availableIsotopes),
        buildMzByElementFromIsotopes(availableIsotopes),
        previousIsotopeSelection);
  }

  /**
   * Private constructor used by both {@link #forIsotopeSelection} (Case A) and
   * the public {@code List<Double>} constructor (Case B).
   * Accepts pre-built lookup maps so each entry point can supply its own.
   */
  private IsotopePtoeDialog(
      @Nullable Window owner,
      Map<Integer, List<Double>> mzByNominalMass,
      Map<Element, List<Double>> mzByElement,
      List<Isotope> previousIsotopeSelection) {

    this.previousIsotopeSelection = (previousIsotopeSelection != null)
        ? new HashSet<>(previousIsotopeSelection)
        : new HashSet<>();
    this.mzByNominalMass = mzByNominalMass;
    this.mzByElement = mzByElement;
    this.ambiguousMasses = buildAmbiguousMasses();
    this.stage = buildStage(owner);
  }

  // =========================================================================
  // Public API
  // =========================================================================

  /**
   * Shows the dialog and blocks until it is closed.
   *
   * @return list of {@link MZValue} (each wrapping an exact m/z and its assigned
   * {@link Isotope}), or {@code null} if the user cancelled.
   * Isobaric pairs (e.g. 48Ti and 48Ca) each produce a separate entry pointing
   * to the same recorded m/z value.
   */
  public List<MZValue> showAndWait() {
    stage.showAndWait();
    return dialogResult;
  }

  // =========================================================================
  // Lookup-table construction
  // =========================================================================

  private static Map<Integer, List<Double>> buildMzByNominalMass(List<Double> availableMz) {
    Map<Integer, List<Double>> map = new LinkedHashMap<>();
    for (double mz : availableMz) {
      int nominal = (int) Math.round(mz);
      if (!map.containsKey(nominal)) {
        map.put(nominal, new ArrayList<>());
      }
      map.get(nominal).add(mz);
    }
    return map;
  }

  /**
   * Uses {@link Element#getAllConflictingIsotopicNumbers()} to find every
   * nominal mass that maps to more than one element, then keeps only those
   * that are actually present in the current dataset.
   */
  private Set<Integer> buildAmbiguousMasses() {
    Set<Integer> ambiguous = new HashSet<>();
    for (Integer nominalMass : Element.getAllConflictingIsotopicNumbers().keySet()) {
      if (this.mzByNominalMass.containsKey(nominalMass)) {
        ambiguous.add(nominalMass);
      }
    }
    return ambiguous;
  }

  /**
   * For each element, collects every available exact m/z whose nominal mass
   * matches one of the element's isotopes.
   */
  private static Map<Element, List<Double>> buildMzByElement(
      Map<Integer, List<Double>> mzByNominalMass) {
    Map<Element, List<Double>> map = new LinkedHashMap<>();
    for (Element element : Element.values()) {
      List<Double> matched = new ArrayList<>();
      for (Isotope isotope : element.getIsotopes()) {
        int nominal = isotope.getIsotopicNumber();
        List<Double> hits = mzByNominalMass.get(nominal);
        if (hits != null) {
          matched.addAll(hits);
        }
      }
      if (!matched.isEmpty()) {
        map.put(element, matched);
      }
    }
    return map;
  }

  /**
   * Case A variant of {@link #buildMzByNominalMass}: builds the nominal-mass
   * lookup from an explicit isotope list using theoretical masses.
   * Used only to feed {@link #buildAmbiguousMasses()} — ownership is handled
   * separately by {@link #buildMzByElementFromIsotopes}.
   */
  private static Map<Integer, List<Double>> buildMzByNominalMassFromIsotopes(
      List<Isotope> isotopes) {
    Map<Integer, List<Double>> map = new LinkedHashMap<>();
    for (Isotope iso : isotopes) {
      int nominal = iso.getIsotopicNumber();
      if (!map.containsKey(nominal)) {
        map.put(nominal, new ArrayList<>());
      }
      map.get(nominal).add(iso.getTheoreticalMass());
    }
    return map;
  }

  /**
   * Case A variant of {@link #buildMzByElement}: assigns each isotope to its
   * own element using its theoretical mass as the mz value.
   * Each mz appears in exactly one element's list — no cross-element duplication.
   */
  private static Map<Element, List<Double>> buildMzByElementFromIsotopes(
      List<Isotope> isotopes) {
    Map<Element, List<Double>> map = new LinkedHashMap<>();
    for (Isotope iso : isotopes) {
      Element el = iso.getElement();
      if (!map.containsKey(el)) {
        map.put(el, new ArrayList<>());
      }
      map.get(el).add(iso.getTheoreticalMass());
    }
    return map;
  }

  // =========================================================================
  // Stage / scene construction
  // =========================================================================

  private Stage buildStage(@Nullable Window owner) {
    Stage s = new Stage();
    if (owner!=null){
      s.initOwner(owner);
    }
    s.initModality(Modality.WINDOW_MODAL);
    s.setTitle("Select isotopes");

    GridPane mainGrid = buildPeriodicTableGrid();

    // Gap row between main table and lanthanide/actinide breakout
    Label breakoutLabel = new Label("  * Lanthanides (row 6, col 3)   ** Actinides (row 7, col 3)");
    breakoutLabel.setStyle("-fx-text-fill:#999; -fx-font-size:9;");

    VBox center = new VBox(4, mainGrid, breakoutLabel);
    center.setPadding(new Insets(10));

    // Legend
    Label legendGreen = styledLabel("    ", COLOR_NORMAL_ON + BORDER_COMMON);
    Label legendYellow = styledLabel("    ", COLOR_AMBIGUOUS_ON + BORDER_COMMON);
    Label legendGray = styledLabel("    ", COLOR_ABSENT + BORDER_COMMON);
    Label lgText = new Label(" present   ");
    Label lyText = new Label(" isobaric/ambiguous (right-click to resolve)   ");
    Label laText = new Label(" not in data");
    HBox legend = new HBox(2, legendGreen, lgText, legendYellow, lyText, legendGray, laText);
    legend.setPadding(new Insets(0, 10, 4, 10));
    legend.setAlignment(Pos.CENTER_LEFT);
    legend.setStyle("-fx-font-size:10;");

    // Bottom button bar
    Label hint = new Label("Right-click element for isotope picker  •  Double-click isotope list to select " +
        "all");
    hint.setStyle("-fx-text-fill:#888; -fx-font-size:10;");

    Button okBtn = new Button("Continue");
    Button selectAllBtn = new Button("Select all");
    Button selectAllMZBtn = new Button("Select all m/z");
    Button loadTableBtn = new Button("Add from table");
    Button deselectAllBtn = new Button("Deselect all");
    Button cancelBtn = new Button("Cancel");
    okBtn.setDefaultButton(true);
    cancelBtn.setCancelButton(true);

    okBtn.setOnAction(e -> {
      dialogResult = new ArrayList<>();
      for (Map.Entry<Isotope, Double> entry : selectedItems.entrySet()) {
        dialogResult.add(new SQmz(entry.getValue(), entry.getKey()));
      }
      s.close();
    });
    cancelBtn.setOnAction(e -> {
      dialogResult = null;
      s.close();
    });

    // Select all: for every present element pick its default isotope.
    // Clears any existing selection first so there are no leftover entries
    // from manual right-click picks that the user may have changed.
    selectAllBtn.setOnAction(e -> {
      selectedItems.clear();
      for (Map.Entry<Element, List<Double>> entry : mzByElement.entrySet()) {
        Element el = entry.getKey();
        List<Double> mzList = entry.getValue();
        double defaultMz = resolveDefaultMz(el, mzList);
        Isotope defaultIso = findIsotope(el, defaultMz);
        if (defaultIso != null) {
          selectedItems.put(defaultIso, defaultMz);
        }
        ToggleButton tb = toggleByElement.get(el);
        if (tb != null) {
          tb.setSelected(true);
        }
      }
    });

    selectAllMZBtn.setOnAction(e -> {
      selectedItems.clear();
      for (Map.Entry<Element, List<Double>> entry : mzByElement.entrySet()) {
        Element el = entry.getKey();
        for (double mz : entry.getValue()) {
          Isotope iso = findIsotope(el, mz);
          if (iso != null) {
            selectedItems.put(iso, mz);
          }
        }
        ToggleButton tb = toggleByElement.get(el);
        if (tb != null) {
          tb.setSelected(true);
        }
      }
    });

    deselectAllBtn.setOnAction(e -> {
      selectedItems.clear();
      for (ToggleButton tb : toggleByElement.values()) {
        tb.setSelected(false);
      }
    });

    loadTableBtn.setOnAction(e -> {
      List<Isotope> selIsotopes = SpTool3Main.getRunTime().getMainWindowCtl().getSelIsotopes();
      if (selIsotopes == null || selIsotopes.isEmpty()) {
        return;
      }

      for (Isotope iso : selIsotopes) {
        List<Double> elementMz = mzByElement.get(iso.getElement());
        if (elementMz == null) {
          continue;
        }
        for (double mz : elementMz) {
          Isotope matched = findIsotope(iso.getElement(), mz);
          if (matched != null && matched.equals(iso)) {
            selectedItems.put(iso, mz);   // correct recorded mz in Case B, theoretical in Case A
            break;
          }
        }
        ToggleButton tb = toggleByElement.get(iso.getElement());
        if (tb != null) {
          tb.setSelected(true);
        }
      }
    });

    HBox buttonBar = new HBox(8, hint, selectAllBtn, selectAllMZBtn,loadTableBtn, deselectAllBtn,
        new Separator(Orientation.VERTICAL),
        okBtn, cancelBtn);
    buttonBar.setAlignment(Pos.CENTER_RIGHT);
    buttonBar.setPadding(new Insets(6, 10, 8, 10));

    BorderPane root = new BorderPane();
    root.setCenter(center);
    root.setTop(legend);
    root.setBottom(buttonBar);

    s.setScene(new Scene(root));
    s.getIcons().add(UiUtil.getImage("/img/20240418_symbol.png"));
    s.getScene().setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ESCAPE && e.isShiftDown()) {
        cancelBtn.fire();
      } else if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
        okBtn.fire();
      }
    });
    s.sizeToScene();
    return s;
  }

  private GridPane buildPeriodicTableGrid() {
    GridPane grid = new GridPane();
    grid.setHgap(2);
    grid.setVgap(2);

    // Lanthanide / actinide series placeholder labels in column 3 (0-based col 2)
    grid.add(makePlaceholderLabel("*La"), 2, 5);   // row 6 → 0-based 5
    grid.add(makePlaceholderLabel("**Ac"), 2, 6);   // row 7 → 0-based 6

    // Blank separator row between main table and breakout (row index 7, 0-based)
    // – nothing added; GridPane leaves the gap naturally

    for (int[] pos : ELEMENT_GRID) {
      int atomicNumber = pos[0];
      int gridRow = pos[1] - 1;   // 0-based; rows 7/8 (0-based) are breakout
      int gridCol = pos[2] - 1;

      Element element = elementByAtomicNumber(atomicNumber);
      if (element == null) {
        continue;
      }

      List<Double> elementMz = mzByElement.get(element);

      if (elementMz == null) {
        grid.add(makeAbsentLabel(element), gridCol, gridRow);
      } else {
        grid.add(makeElementToggle(element, elementMz), gridCol, gridRow);
      }
    }

    return grid;
  }

  // =========================================================================
  // Individual cell factories
  // =========================================================================

  private ToggleButton makeElementToggle(Element element, List<Double> elementMz) {
    boolean ambiguous = isAmbiguous(element);

    ToggleButton btn = new ToggleButton(element.getShortName());
    btn.setMinSize(34, 26);
    btn.setMaxSize(34, 26);
    btn.setFont(Font.font(null, FontWeight.NORMAL, 9));

    applyToggleStyle(btn, ambiguous, false);
    btn.selectedProperty().addListener((obs, wasOn, isOn) ->
        applyToggleStyle(btn, ambiguous, isOn));

    // Restore from a previous session: match each available mz against the
    // previously selected isotopes by finding the closest theoretical mass.
    // If no previous selection exists the dialog opens with nothing selected.
    if (!previousIsotopeSelection.isEmpty()) {
      boolean anyRestored = false;
      for (double mz : elementMz) {
        Isotope matched = findIsotope(element, mz);
        if (matched != null && previousIsotopeSelection.contains(matched)) {
          selectedItems.put(matched, mz);
          anyRestored = true;
        }
      }
      btn.setSelected(anyRestored);
    }

    // Register toggle so the "Select all" button can reach it
    toggleByElement.put(element, btn);

    // Left-click: toggle the default isotope
    btn.setOnAction(e -> {
      if (btn.isSelected()) {
        double defaultMz = resolveDefaultMz(element, elementMz);
        Isotope defaultIso = findIsotope(element, defaultMz);
        if (defaultIso != null) {
          selectedItems.put(defaultIso, defaultMz);
        }
      } else {
        // Deselect all isotopes belonging to this element
        for (Isotope iso : element.getIsotopes()) {
          selectedItems.remove(iso);
        }
      }
    });

    // Right-click: open the isotope picker (fires regardless of toggle state)
    btn.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.SECONDARY) {
        e.consume();
        openIsotopePicker(element, elementMz, btn);
      }
    });

    Tooltip.install(btn, new Tooltip(buildTooltipText(element, elementMz, ambiguous)));

    return btn;
  }

  private Label makeAbsentLabel(Element element) {
    Label lbl = new Label(element.getShortName());
    lbl.setMinSize(34, 26);
    lbl.setMaxSize(34, 26);
    lbl.setAlignment(Pos.CENTER);
    lbl.setStyle(COLOR_ABSENT + BORDER_COMMON);
    return lbl;
  }

  private Label makePlaceholderLabel(String text) {
    Label lbl = new Label(text);
    lbl.setMinSize(34, 26);
    lbl.setMaxSize(34, 26);
    lbl.setAlignment(Pos.CENTER);
    lbl.setStyle("-fx-background-color:#f0f0f0; -fx-text-fill:#aaa;"
        + "-fx-font-size:8; -fx-border-color:#ccc;"
        + "-fx-border-radius:3; -fx-background-radius:3;");
    return lbl;
  }

  private static Label styledLabel(String text, String style) {
    Label l = new Label(text);
    l.setStyle(style);
    return l;
  }

  // =========================================================================
  // Isotope picker popup
  // =========================================================================

  /**
   * Opens a small modal window listing all available isotopes for
   * {@code element}.  The user can multi-select; double-clicking selects all.
   * Pressing OK commits the selection back into selectedItems and syncs
   * the parent toggle button.
   */
  private void openIsotopePicker(
      Element element,
      List<Double> elementMz,
      ToggleButton parentBtn) {

    Stage popup = new Stage();
    popup.initOwner(stage);
    popup.initModality(Modality.WINDOW_MODAL);
    popup.setTitle(element.getShortName() + " – choose isotopes");

    // Build one display item per available exact m/z.
    // If other elements share the same nominal mass (isobaric overlap), append
    // them in curly brackets so the user can see the interference at a glance,
    // e.g. "48Ti  (47.9480 Da)  {48Ca, 48V}".
    ObservableList<IsotopeItem> items = FXCollections.observableArrayList();
    for (double mz : elementMz) {
      Isotope iso = findIsotope(element, mz);
      String label;
      if (iso != null) {
        label = iso.getFullUIName() + "   (" + String.format("%.2f", mz) + " Da, "
            + SnF.doubleToString(iso.getAbundance() * 100, NF.D1C1) + "%)"
            + buildIsobaricSuffix(iso, element);
      } else {
        label = (int) Math.round(mz) + "  –  " + String.format("%.4f", mz) + " Da";
      }
      items.add(new IsotopeItem(label, mz, iso));
    }

    ListView<IsotopeItem> listView = new ListView<>(items);
    listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    listView.setPrefSize(260, Math.min(items.size() * 26 + 6, 220));

    // Pre-tick items already in selectedItems
    for (int i = 0; i < items.size(); i++) {
      IsotopeItem item = items.get(i);
      if (item.iso != null && selectedItems.containsKey(item.iso)) {
        listView.getSelectionModel().select(i);
      }
    }

    // Shared commit logic – updates selectedItems for this element's isotopes,
    // syncs the parent toggle, and closes the popup.
    Runnable commit = () -> {
      // Remove all isotopes belonging to this element, then add back the selection.
      for (Isotope iso : element.getIsotopes()) {
        selectedItems.remove(iso);
      }
      for (IsotopeItem item : listView.getSelectionModel().getSelectedItems()) {
        if (item.iso != null) {
          selectedItems.put(item.iso, item.mz);
        }
      }
      parentBtn.setSelected(!listView.getSelectionModel().getSelectedItems().isEmpty());
      popup.close();
    };

    Runnable commitWithDelay = () -> {
      listView.getSelectionModel().selectAll();
      PauseTransition pause = new PauseTransition(Duration.millis(100));
      pause.setOnFinished(ev -> commit.run());
      pause.play();
    };

    /*
     * Why we need a custom cell factory with setOnMousePressed:
     *
     * JavaFX fires mouse events in this order: PRESSED → RELEASED → CLICKED.
     * The ListView's built-in selection model updates on PRESSED (primary button),
     * so by the time a CLICKED event fires, getSelectedItem() already reflects
     * the left-clicked row.
     *
     * For secondary (right) clicks however, JavaFX does NOT move the selection
     * on PRESSED – it only opens a context menu.  This means that when our
     * setOnMouseClicked handler fires for a right-click, getSelectedItem() still
     * returns whichever row was previously highlighted, not the one under the
     * cursor.
     *
     * The fix: install a custom cell factory where each cell handles
     * setOnMousePressed for SECONDARY button, manually selecting that cell's
     * item before the CLICKED event fires and our commit logic reads the
     * selection.  We must use PRESSED (not CLICKED) inside the cell so the
     * selection is already updated when the outer CLICKED handler runs.
     */
    listView.setCellFactory(lv -> {
      ListCell<IsotopeItem> cell = new ListCell<>() {
        @Override
        protected void updateItem(IsotopeItem item, boolean empty) {
          super.updateItem(item, empty);
          setText((item == null || empty) ? null : item.toString());
        }
      };
      cell.setOnMousePressed(e -> {
        if (e.getButton() == MouseButton.SECONDARY && !cell.isEmpty()) {
          // Move ListView selection to the cell under the cursor so that
          // the subsequent CLICKED handler reads the correct item.
          listView.getSelectionModel().clearSelection();
          listView.getSelectionModel().select(cell.getItem());
        }
      });
      return cell;
    });

    listView.setOnMouseClicked(e -> {
      if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
        // Double left-click: select all, stay open
        listView.getSelectionModel().selectAll();

      } else if (e.getButton() == MouseButton.SECONDARY && e.getClickCount() == 1) {
        // Single right-click: commit the item under cursor and close
        IsotopeItem item = listView.getSelectionModel().getSelectedItem();
        if (item != null) {
          commit.run();
        }

      } else if (e.getButton() == MouseButton.SECONDARY && e.getClickCount() == 2) {
        // Double right-click: select all, stay open
        listView.getSelectionModel().selectAll();
      }
    });

    Button allBtn = new Button("All");
    Button okBtn = new Button("OK");
    Button cancelBtn = new Button("Cancel");
    okBtn.setDefaultButton(true);
    cancelBtn.setCancelButton(true);

    allBtn.setOnAction(e -> commitWithDelay.run());
    okBtn.setOnAction(e -> commit.run());
    cancelBtn.setOnAction(e -> popup.close());

    HBox btnBar = new HBox(8, allBtn, okBtn, cancelBtn);
    btnBar.setAlignment(Pos.CENTER_RIGHT);
    btnBar.setPadding(new Insets(4, 0, 0, 0));

    Label hint = new Label(
        "Double-click: select all  •  Right-click: select & close  •  Double right-click: select all  •  " +
            "'All': select all & close");
    hint.setStyle("-fx-text-fill:#888; -fx-font-size:10;");

    VBox root = new VBox(8, listView, hint, btnBar);
    root.setPadding(new Insets(10));

    popup.setScene(new Scene(root));
    popup.getIcons().add(UiUtil.getImage("/img/20240418_symbol.png"));
    // Shift+Escape → cancel;  Ctrl+Enter → fire "OK"
    popup.getScene().setOnKeyPressed(e -> {
      if (e.getCode() == KeyCode.ESCAPE && e.isShiftDown()) {
        cancelBtn.fire();
      } else if (e.getCode() == KeyCode.ENTER && e.isControlDown()) {
        okBtn.fire();
      }
    });
    popup.sizeToScene();
    popup.showAndWait();
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  /**
   * Finds the exact m/z in {@code elementMz} that is closest to the default
   * isotope for {@code element} as returned by
   * {@code SpTool3Main.getRunTime().getConfParams().getDefaultIsotope(element)}.
   * Falls back to the first entry in {@code elementMz} if lookup fails.
   */
  private double resolveDefaultMz(Element element, List<Double> elementMz) {
    double result = elementMz.get(0);
    try {
      Isotope defaultIso = SpTool3Main.getRunTime()
          .getConfParams()
          .getDefaultIsotope(element);
      if (defaultIso != null) {
        double target = defaultIso.getTheoreticalMass();
        double bestDiff = Double.MAX_VALUE;
        for (double mz : elementMz) {
          double diff = Math.abs(mz - target);
          if (diff < bestDiff) {
            bestDiff = diff;
            result = mz;
          }
        }
      }
    } catch (Exception ignored) {
      // Fall through to the first-entry fallback set above.
    }
    return result;
  }

  /**
   * Returns true if any of this element's isotopes have an ambiguous nominal mass.
   */
  private boolean isAmbiguous(Element element) {
    boolean found = false;
    for (Isotope iso : element.getIsotopes()) {
      if (ambiguousMasses.contains(iso.getIsotopicNumber())) {
        found = true;
        break;
      }
    }
    return found;
  }

  /**
   * Finds the {@link Isotope} belonging to {@code element} whose mass is
   * closest to {@code mz}.  Returns {@code null} if none found.
   */
  private static Isotope findIsotope(Element element, double mz) {
    Isotope best = null;
    double bestDiff = Double.MAX_VALUE;
    for (Isotope iso : element.getIsotopes()) {
      double diff = Math.abs(iso.getTheoreticalMass() - mz);
      if (diff < bestDiff) {
        bestDiff = diff;
        best = iso;
      }
    }
    return best;
  }

  /**
   * Looks up an {@link Element} by atomic number.
   */
  private static Element elementByAtomicNumber(int z) {
    Element result = null;
    for (Element e : Element.values()) {
      if (e.getAtomicNumber() == z) {
        result = e;
        break;
      }
    }
    return result;
  }

  private static void applyToggleStyle(ToggleButton btn, boolean ambiguous, boolean selected) {
    String color;
    if (ambiguous) {
      color = selected ? COLOR_AMBIGUOUS_ON : COLOR_AMBIGUOUS_OFF;
    } else {
      color = selected ? COLOR_NORMAL_ON : COLOR_NORMAL_OFF;
    }
    btn.setStyle(color + BORDER_COMMON);
  }

  private static String buildTooltipText(
      Element element,
      List<Double> elementMz,
      boolean ambiguous) {

    StringBuilder sb = new StringBuilder(element.getShortName());
    sb.append(" – ").append(element.getLongName());
    if (ambiguous) {
      sb.append("\n⚠ Shares a nominal mass with another element.");
      sb.append("\n  Right-click to pick which isotopes to import.");
    }
    sb.append("\nAvailable channels:");
    for (double mz : elementMz) {
      sb.append(String.format("%n  %d  –  %.4f Da", (int) Math.round(mz), mz));
    }
    return sb.toString();
  }

  /**
   * Builds the isobaric suffix for an isotope list row.
   * <p>
   * Looks up every other element that has an isotope with the same nominal mass
   * (isotopic number) as {@code iso}, then formats them as "  {48Ca, 48V}".
   * Returns an empty string if there are no isobaric partners.
   * <p>
   * We use {@link Element#getAllConflictingIsotopicNumbers()} which already has
   * the full conflict map; we just filter out the element being displayed.
   */
  private static String buildIsobaricSuffix(Isotope iso, Element ownElement) {
    int nominalMass = iso.getIsotopicNumber();
    Element[] conflicting = Element.getAllConflictingIsotopicNumbers().get(nominalMass);
    if (conflicting == null || conflicting.length == 0) {
      return "";
    }
    StringBuilder sb = new StringBuilder("  {");
    boolean first = true;
    for (Element other : conflicting) {
      if (other == ownElement) {
        continue;   // skip self
      }
      if (!first) {
        sb.append(", ");
      }
      sb.append(nominalMass).append(other.getShortName());
      first = false;
    }
    if (first) {
      return "";   // only self was in the list – no real conflict
    }
    sb.append("}");
    return sb.toString();
  }

  // =========================================================================
  // Inner record
  // =========================================================================

  /**
   * One row in the isotope picker ListView.
   * Carries both the exact recorded m/z and the resolved Isotope object so
   * that commit() can build a proper SQmz without a second lookup.
   */
  private static final class IsotopeItem {

    final String label;
    final double mz;
    final Isotope iso;   // may be null for unresolved entries

    IsotopeItem(String label, double mz, Isotope iso) {
      this.label = label;
      this.mz = mz;
      this.iso = iso;
    }

    @Override
    public String toString() {
      return label;
    }
  }
}
