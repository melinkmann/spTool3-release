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

package visualizer;

import analysis.PopulationID;
import core.RunTimeInstance;
import dataModelNew.Sample;
import io.TableIO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import sandbox.montecarlo.Isotope;
import visualizer.ResultsTable.TablePar;

public class ResultTableData {

  private static final Logger LOGGER = LogManager.getLogger(ResultTableData.class);

  private final HashMap<Sample, List<TraceCol>> entryMap = new LinkedHashMap<>();

  public ResultTableData(List<Sample> samples, List<Isotope> isotopes,
                         List<PopulationID> popIDs) {

    List<TablePar> sampleLevelPars = filterSelected(TablePar.getSampleInfo());
    List<TablePar> traceLevelPars = filterSelected(TablePar.getTraceInfo());
    List<TablePar> quantLevelPars = filterSelected(TablePar.getQuantInfo());
    List<TablePar> populationLevelPars = filterSelected(TablePar.getPopulationInfo());
    List<TablePar> populationBLNLevelPars = filterSelected(TablePar.getPopulationBLNInfo());

    // gates may have different row counts depending on number of gates
    HashMap<PopulationID, Integer> gateRowCounts = new HashMap<>();
    for (PopulationID popID : popIDs) {
      int gateRowCount = 0;
      for (Sample sample : samples) {
        for (Isotope isotope : isotopes) {
          gateRowCount = Math.max(gateRowCount, sample.tabGates(isotope, popID).size());
        }
      }
      gateRowCounts.put(popID, gateRowCount);
    }


    // Fill
    for (Sample sample : samples) {
      // Get selected traces

      // Make columns
      for (Isotope isotope : isotopes) {
        TraceCol col = new TraceCol(isotope.getName());
        if (!entryMap.containsKey(sample)) {
          entryMap.put(sample, new ArrayList<>());
        }
        entryMap.get(sample).add(col);

        // Sample level
        for (TablePar par : sampleLevelPars) {
          col.add(par, par.getValues(sample, isotope, null, 0));
        }

        // Trace level
        for (TablePar par : traceLevelPars) {
          col.add(par, par.getValues(sample, isotope, null, 0));
        }

        // Trace quant level
        for (TablePar par : quantLevelPars) {
          col.add(par, par.getValues(sample, isotope, null, 0));
        }


        // Population level
        for (PopulationID pop : popIDs) {

          // // Population level: the NP and BG population
          for (TablePar par : populationLevelPars) {
            col.add(par, par.getValues(sample, isotope, pop, 0));
          }

          // Population level: the background (Show label of the pop first, then the detection conditions)
          int fillRows = gateRowCounts.get(pop) != null ? gateRowCounts.get(pop) : 0;
          for (TablePar par : populationBLNLevelPars) {
            col.add(par, par.getValues(sample, isotope, pop, fillRows));
          }
        }
      }
    }
  }

  public HashMap<Sample, List<TraceCol>> getEntryMap() {
    return entryMap;
  }

  public List<TablePar> filterSelected(List<TablePar> inputPars) {
    List<TablePar> allPars = new ArrayList<>(inputPars);
    TableIO tableDefaults = RunTimeInstance.getParamTableDefaults();
    if (tableDefaults != null) {
      if (!tableDefaults.getActiveParameters().isEmpty()) {
        // only show what is selected
        List<TablePar> parsToShow = new ArrayList<>(tableDefaults.getActiveParameters());
        allPars.removeIf(tp -> !parsToShow.contains(tp));
      } else {
        LOGGER.info("Empty table was filled with all available options.");
      }
    } else {
      LOGGER.info("Selected table entries were not available - used all available options.");
    }
    /*
     * THIS IS WHERE WE DECIDE ORDER!
     * Sort by enum declaration order
     */
    allPars.sort(Comparator.comparingInt(Enum::ordinal));
    return allPars;
  }

  public static class TraceCol {

    // We will have the same TablePar parameter multiple times for different populations
    private final String label;
    private final List<TraceRowContainer> mappingList = new ArrayList<>();

    public TraceCol(String label) {
      this.label = label;
    }

    public void add(TablePar par, List<String> values) {
      for (int i = 0; i < values.size(); i++) {
        String value = values.get(i);
        mappingList.add(new TraceRowContainer(par, i + 1, value));
      }
    }

    public List<TraceRowContainer> getMappingList() {
      return mappingList;
    }


    public String getLabel() {
      return label;
    }
  }

  public static class TraceRowContainer {
    private final TablePar par;
    private final int incrementIdx;
    private final String value;

    public TraceRowContainer(TablePar par, int incrementIdx, String value) {
      this.par = par;
      this.incrementIdx = incrementIdx;
      this.value = value;
    }

    public TablePar getPar() {
      return par;
    }

    public String getRowLabel() {
      String lbl = par.rowLabel();
      if (lbl.contains("Gate")) {
        lbl = lbl + " (#" + incrementIdx + ")";
      }
      return lbl;
    }

    public String getValue() {
      return value;
    }
  }

}
