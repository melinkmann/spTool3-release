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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

// Chat GPTs attempt at porting NU import. Does not work. However, seems to parse parts correctly.
// Main issue: stopped time way too early (just some seconds not 70s)
// AND: identified just ONE element

public class NuICPToF {

  private static final Logger logger = Logger.getLogger(NuICPToF.class.getName());
  private static final ObjectMapper mapper = new ObjectMapper();

  // ================= Export CSV =================
  public static void exportNuTimeResolvedCSV(String dataDir, String outputCsv,
      List<Integer> selectedMasses) throws IOException {
    Path dataPath = Paths.get(dataDir);
    Path csvPath = Paths.get(outputCsv);

    System.out.println("Loading: " + dataPath);
    System.out.println("Saving to: " + csvPath);

    Tuple<double[], float[][], Map<String, Object>> nuData = readNuDirectory(dataPath, true, null,
        null, false);
    double[] masses = nuData.first;
    float[][] signals = nuData.second;
    Map<String, Object> runInfo = nuData.third;

    double dwellTime = getDwellTimeFromInfo(runInfo);
    double[] time = new double[signals.length];
    for (int i = 0; i < signals.length; i++) {
      time[i] = (i + 1) * dwellTime;
    }

    List<Integer> filteredIndices = new ArrayList<>();
    if (selectedMasses != null) {
      for (int i = 0; i < masses.length; i++) {
        if (selectedMasses.contains((int) Math.round(masses[i]))) {
          filteredIndices.add(i);
        }
      }
    } else {
      for (int i = 0; i < masses.length; i++) {
        filteredIndices.add(i);
      }
    }

    try (CSVWriter writer = new CSVWriter(new FileWriter(csvPath.toFile()))) {
      String[] header = new String[filteredIndices.size() + 1];
      header[0] = "time_s";
      for (int i = 0; i < filteredIndices.size(); i++) {
        header[i + 1] = String.valueOf(Math.round(masses[filteredIndices.get(i)]));
      }
      writer.writeNext(header);

      for (int i = 0; i < signals.length; i++) {
        String[] row = new String[filteredIndices.size() + 1];
        row[0] = String.format("%.6f", time[i]);
        for (int j = 0; j < filteredIndices.size(); j++) {
          row[j + 1] = String.format("%.3f", signals[i][filteredIndices.get(j)]);
        }
        writer.writeNext(row);
      }
    }

    System.out.println("✅ Exported time-resolved data to " + csvPath);
  }

  // ================= Read Nu Directory =================
  public static Tuple<double[], float[][], Map<String, Object>> readNuDirectory(Path path,
      boolean autoblank,
      Integer cycle,
      Integer segment,
      boolean raw) throws IOException {

    if (!Files.isDirectory(path) || !Files.exists(path.resolve("run.info")) || !Files
        .exists(path.resolve("integrated.index"))) {
      throw new IOException("Missing 'run.info' or 'integrated.index'");
    }

    Map<String, Object> runInfo = mapper
        .readValue(path.resolve("run.info").toFile(), new TypeReference<>() {
        });
    List<Map<String, Object>> integIndex = mapper
        .readValue(path.resolve("integrated.index").toFile(), new TypeReference<>() {
        });
    List<Map<String, Object>> autobIndex = mapper
        .readValue(path.resolve("autob.index").toFile(), new TypeReference<>() {
        });

    List<IntegData> integs = collectNuIntegData(path, integIndex, cycle, segment);

    int accumulations =
        (int) runInfo.get("NumAccumulations1") * (int) runInfo.get("NumAccumulations2");

    double[] masses = getMassesFromNuData(integs.get(0),
        (List<Double>) runInfo.get("MassCalCoefficients"),
        ((List<Map<String, Object>>) runInfo.get("SegmentInfo")).stream().collect(Collectors.toMap(
            s -> (Integer) s.get("Num"),
            s -> ((Number) s.get("AcquisitionTriggerDelayNs")).doubleValue()
        )));

    float[][] signals = getSignalsFromNuData(integs, accumulations);

    if (!raw) {
      double avgSingleIonArea = ((Number) runInfo.get("AverageSingleIonArea")).doubleValue();
      for (int i = 0; i < signals.length; i++) {
        for (int j = 0; j < signals[i].length; j++) {
          signals[i][j] /= avgSingleIonArea;
        }
      }
    }

    if (autoblank) {
      List<AutobEvent> autobs = collectNuAutobData(path, autobIndex, cycle, segment);
      signals = blankNuSignalData(autobs, signals, masses, accumulations,
          (List<Double>) runInfo.get("BlMassCalStartCoef"),
          (List<Double>) runInfo.get("BlMassCalEndCoef"));
    }

    return new Tuple<>(masses, signals, runInfo);
  }

  // ================= Dwell Time =================
  private static double getDwellTimeFromInfo(Map<String, Object> info) {
    Map<String, Object> seg = ((List<Map<String, Object>>) info.get("SegmentInfo")).get(0);
    double acqTime = ((Number) seg.get("AcquisitionPeriodNs")).doubleValue() * 1e-9;
    int accumulations = (int) info.get("NumAccumulations1") * (int) info.get("NumAccumulations2");
    return Math.round(acqTime * accumulations * 1e9) / 1e9;
  }

  // ================= Signals =================
  private static float[][] getSignalsFromNuData(List<IntegData> integs, int numAcc) {
    int totalRows = integs.stream().mapToInt(integ -> integ.signals.length).sum();
    int numCols = integs.get(0).signals[0].length;
    float[][] signals = new float[totalRows][numCols];
    for (float[] row : signals) {
      Arrays.fill(row, Float.NaN);
    }

    int offset = 0;
    for (IntegData integ : integs) {
      for (int i = 0; i < integ.signals.length; i++) {
        signals[offset + i] = Arrays.copyOf(integ.signals[i], numCols);
      }
      offset += integ.signals.length;
    }
    return signals;
  }

  // ================= Mass Calculation =================
  private static double[] getMassesFromNuData(IntegData integ, List<Double> calCoef,
      Map<Integer, Double> segmentDelays) {
    double[] masses = new double[integ.signals[0].length];
    double a = calCoef.get(0);
    double b = calCoef.get(1);
    for (int i = 0; i < masses.length; i++) {
      double delay = segmentDelays.getOrDefault(integ.segNumber, 0.0);
      masses[i] = Math.pow(a + (integ.centers[i] + delay) * b, 2);
    }
    return masses;
  }

  // ================= Collect Integ Data =================
  private static List<IntegData> collectNuIntegData(Path root, List<Map<String, Object>> index,
      Integer cycle, Integer segment) throws IOException {
    List<IntegData> result = new ArrayList<>();
    for (Map<String, Object> idx : index) {
      Path path = root.resolve(idx.get("FileNum") + ".integ");
      if (!Files.exists(path)) {
        logger.warning("Missing integ " + idx.get("FileNum"));
        continue;
      }
      result.add(readNuIntegBinary(path));
    }
    return result;
  }

  // ================= Read Integ Binary =================
  private static IntegData readNuIntegBinary(Path path) throws IOException {
    try (DataInputStream dis = new DataInputStream(Files.newInputStream(path))) {
      int cycNumber = Integer.reverseBytes(dis.readInt());
      int segNumber = Integer.reverseBytes(dis.readInt());
      int acqNumber = Integer.reverseBytes(dis.readInt());
      int numResults = Integer.reverseBytes(dis.readInt());

      float[][] signals = new float[numResults][1];
      float[] centers = new float[numResults];
      for (int i = 0; i < numResults; i++) {
        centers[i] = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
        signals[i][0] = Float.intBitsToFloat(Integer.reverseBytes(dis.readInt()));
        dis.skipBytes(5); // skip unused
      }
      return new IntegData(cycNumber, segNumber, acqNumber, signals, centers);
    }
  }

  // ================= Autob =================
  private static List<AutobEvent> collectNuAutobData(Path root, List<Map<String, Object>> index,
      Integer cycle, Integer segment) throws IOException {
    List<AutobEvent> events = new ArrayList<>();
    for (Map<String, Object> idx : index) {
      Path path = root.resolve(idx.get("FileNum") + ".autob");
      if (!Files.exists(path)) {
        logger.warning("Missing autob " + idx.get("FileNum"));
        continue;
      }
      events.addAll(readNuAutobBinary(path));
    }
    return events;
  }

  private static List<AutobEvent> readNuAutobBinary(Path path) throws IOException {
    List<AutobEvent> events = new ArrayList<>();
    try (DataInputStream dis = new DataInputStream(Files.newInputStream(path))) {
      while (dis.available() > 0) {
        int cycNumber = Integer.reverseBytes(dis.readInt());
        int segNumber = Integer.reverseBytes(dis.readInt());
        int acqNumber = Integer.reverseBytes(dis.readInt());
        int trigStart = Integer.reverseBytes(dis.readInt());
        int trigEnd = Integer.reverseBytes(dis.readInt());
        byte type = dis.readByte();
        int numEdges = Integer.reverseBytes(dis.readInt());
        long[] edges = new long[numEdges];
        for (int i = 0; i < numEdges; i++) {
          edges[i] = Integer.toUnsignedLong(Integer.reverseBytes(dis.readInt()));
        }
        events.add(new AutobEvent(cycNumber, segNumber, acqNumber, type, edges));
      }
    }
    return events;
  }

  // ================= Blanking =================
  private static float[][] blankNuSignalData(List<AutobEvent> events, float[][] signals,
      double[] masses,
      int numAcc, List<Double> startCoef, List<Double> endCoef) {

    List<Tuple2<int[], double[][]>> blankRegions = getBlankingRegions(events, numAcc, startCoef,
        endCoef);

    for (Tuple2<int[], double[][]> region : blankRegions) {
      int startRow = region.first[0];
      int endRow = region.first[1];
      double[][] massRanges = region.second;

      for (double[] massRange : massRanges) {
        int startCol = searchMassIndex(masses, massRange[0]);
        int endCol = searchMassIndex(masses, massRange[1]);
        for (int i = startRow; i < endRow; i++) {
          for (int j = startCol; j < endCol; j++) {
            signals[i][j] = Float.NaN;
          }
        }
      }
    }
    return signals;
  }

  private static List<Tuple2<int[], double[][]>> getBlankingRegions(List<AutobEvent> events,
      int numAcc,
      List<Double> startCoef, List<Double> endCoef) {
    List<Tuple2<int[], double[][]>> regions = new ArrayList<>();
    AutobEvent startEvent = null;

    for (AutobEvent ev : events) {
      if (ev.type == 0 && startEvent == null) {
        startEvent = ev;
      } else if (ev.type == 1 && startEvent != null) {
        int startRow = Math.max(0, startEvent.acqNumber / numAcc - 1);
        int endRow = Math.max(0, ev.acqNumber / numAcc - 1);

        List<Double> startMasses = new ArrayList<>();
        List<Double> endMasses = new ArrayList<>();
        for (int i = 0; i < startEvent.edges.length / 2; i++) {
          double sm = Math
              .pow(startCoef.get(0) + startCoef.get(1) * startEvent.edges[i * 2] * 1.25, 2);
          double em = Math
              .pow(endCoef.get(0) + endCoef.get(1) * startEvent.edges[i * 2 + 1] * 1.25, 2);
          if (sm < em) {
            startMasses.add(sm);
            endMasses.add(em);
          }
        }

        double[][] massRanges = new double[startMasses.size()][2];
        for (int i = 0; i < startMasses.size(); i++) {
          massRanges[i][0] = startMasses.get(i);
          massRanges[i][1] = endMasses.get(i);
        }

        regions.add(new Tuple2<>(new int[]{startRow, endRow}, massRanges));
        startEvent = null;
      }
    }
    return regions;
  }

  private static int searchMassIndex(double[] masses, double targetMass) {
    int idx = Arrays.binarySearch(masses, targetMass);
    if (idx < 0) {
      idx = -idx - 1;
    }
    return Math.min(idx, masses.length - 1);
  }

  // ================= Helper Classes =================
  public static class Tuple<T1, T2, T3> {

    public final T1 first;
    public final T2 second;
    public final T3 third;

    public Tuple(T1 f, T2 s, T3 t) {
      first = f;
      second = s;
      third = t;
    }
  }

  // 2-element tuple for blanking regions
  public static class Tuple2<T1, T2> {
    public final T1 first;
    public final T2 second;
    public Tuple2(T1 f, T2 s){ first = f; second = s; }
  }

  public static class IntegData {

    public int cycNumber, segNumber, acqNumber;
    public float[][] signals;
    public float[] centers;

    public IntegData(int c, int s, int a, float[][] sig, float[] cen) {
      cycNumber = c;
      segNumber = s;
      acqNumber = a;
      signals = sig;
      centers = cen;
    }
  }

  public static class AutobEvent {

    public int cycNumber, segNumber, acqNumber;
    public byte type;
    public long[] edges;

    public AutobEvent(int c, int s, int a, byte t, long[] e) {
      cycNumber = c;
      segNumber = s;
      acqNumber = a;
      type = t;
      edges = e;
    }
  }

  // ================= Main =================
  public static void main(String[] args) throws IOException {
    exportNuTimeResolvedCSV(
        "F:/Meine Dateien/UNI/012 Promotion/20210215_spTool2_privatPC/TESTCASE_data/22-01-18 Abs_19_5_v1",
        "F:/Meine Dateien/UNI/013_PostDoc/Writing/2025_3_Mahder_Paper/20250830_final/SPCal_Export/nu_signals.csv",
        Arrays.asList(140, 163, 166, 153, 157, 165, 139, 93, 146, 141, 147, 88, 159, 169, 89,
            172, 138, 133, 178, 208, 85, 181, 232, 238, 90, 59, 52, 63, 56, 55, 48,
            51, 66, 197)
    );
  }
}
