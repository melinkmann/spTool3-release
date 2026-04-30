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
 *  Java port of spcal/io/nu.py  (https://github.com/djdt/spcal)
 *  Original Python by djdt – ported to Java 15 for spTool3.
 *
 *  This version is accelerated compared to "v1"
 *
 *  v2 changes (perf):
 *   - readIntegFile / collectAllIntegRecords replaced by
 *     fillSignalMatrixFromIntegFiles which writes directly into a
 *     preallocated signals[][] matrix – no IntegRecord boxing, no
 *     intermediate List<IntegRecord>, no second copy pass.
 *   - numTimePoints derived from the last acqNumber in the last .integ
 *     file (readLastAcqNumber) instead of a full scan over all records.
 *   - integIndex.indexOf(idx) O(n) call inside loop replaced by an
 *     explicit counter.
 *   - Stage-1 (readAvailableMZ) still uses a minimal single-record read
 *     (readFirstIntegRecord) to get the mass axis cheaply.
 *
 *  v2 bug fixes vs first v2 draft:
 *   - numTimePoints was incorrectly computed as totalAccumulations()/numAcc
 *     (always 1). Now correctly derived via readLastAcqNumber().
 *   - Inner loop in fillSignalMatrixFromIntegFiles read centre into `sig`
 *     and discarded the actual signal. Field order corrected to match
 *     binary layout: [centre(f32), signal(f32), unused(f32), unused(i8)].
 *
 *  v3 changes (caching):
 *   - Static SoftReference<CachedRun> cache keyed on canonical Path.
 *   - All public entry points call getOrLoadCachedRun() first.
 *   - Cache miss: all columns are loaded and stored (never a partial load).
 *   - Cache hit: a strong reference is extracted inside a synchronized block
 *     and held for the duration of the call so the GC cannot clear it mid-use.
 *   - All data returned to callers is a deep copy so the cache entry has no
 *     shared mutable state and callers cannot corrupt each other's views.
 *   - A manual invalidateCache(Path) method is provided for cases where the
 *     underlying files change between calls in the same JVM session.
 */

package io.nu;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * Static utility class for reading Nu Instruments ICP-ToF raw data directories.
 *
 * <h2>Required directory contents</h2>
 * <ul>
 *   <li>{@code run.info}         – JSON metadata</li>
 *   <li>{@code integrated.index} – JSON index of {@code .integ} binary files</li>
 *   <li>{@code autob.index}      – JSON index of {@code .autob} binary files</li>
 *   <li>One or more {@code *.integ} and {@code *.autob} binary files</li>
 * </ul>
 *
 * <h2>Two-stage API</h2>
 * <pre>{@code
 *   // Stage 1 – discover all recorded m/z channels
 *   NuReaderResult scan = NuReader.readAvailableChannels(directory);
 *   List<Double> allMz  = scan.mzValues;
 *
 *   // Stage 2 – load a subset of channels as TISeries
 *   NuReaderResult full = NuReader.readSelectedChannels(directory, List.of(197.0, 208.0));
 *
 *   TISeries au197            = full.channelData.get(197.0);
 *   List<int[]> blankedRanges = full.blankedTimeRanges.get(197.0);
 * }</pre>
 *
 * <p>Intensity values are in ion counts (raw ADC / {@code AverageSingleIonArea})
 * and are <em>never modified</em> by the blanking step.  Blanked time-index
 * ranges are stored in {@link NuReaderResult#blankedTimeRanges} so callers can
 * remove or flag those points at their discretion.
 *
 * <h2>Caching</h2>
 * <p>The first call for any directory loads and ion-count-converts the full
 * signal matrix and caches it as a {@link SoftReference}.  Subsequent calls
 * (whether for all channels or a subset) resolve immediately from the cache
 * without any further file I/O.  The GC may evict the entry under memory
 * pressure, in which case the next call transparently reloads from disk.
 *
 * <p>Use {@link #invalidateCache(Path)} to force a reload if the underlying
 * files change between calls in the same JVM session.
 *
 * <p>Cannot be instantiated.
 */
public abstract class NuReader {

  private static final Logger LOGGER = LogManager.getLogger(NuReader.class.getName());

  /**
   * Default maximum allowed distance (Da) between a requested m/z and the nearest
   * recorded channel when no explicit tolerance is supplied.
   */
  private static final double DEFAULT_MAX_MASS_DIFF_DA = 0.1;

  // =========================================================================
  // Cache
  // =========================================================================

  /**
   * Everything derived from a single Nu run directory that is expensive to
   * compute.  Stored as a {@link SoftReference} value so the GC can reclaim
   * it under memory pressure.
   *
   * <p>All fields are effectively final after construction and are never
   * modified.  Callers always receive <em>deep copies</em> of mutable arrays
   * so this object cannot be corrupted through external references.
   */
  private static final class CachedRun {

    /** Parsed run metadata. */
    final NuRunInfo runInfo;

    /**
     * Full mass axis in original column order (unsorted).
     * Length == numMasses.
     */
    final double[] allMasses;

    /**
     * Full signal matrix [numTimePoints][numMasses], already converted to ion
     * counts (divided by AverageSingleIonArea).  Never modified after caching.
     */
    final double[][] signals;

    /** All auto-blank events across the entire run. */
    final List<AutobEvent> autobEvents;

    /** Dwell-time spacing in seconds. */
    final double dwellTime;

    /** Accumulations per acquisition, used for time-index derivation. */
    final int numAcc;

    final int acquisitionCount;

    CachedRun(
        NuRunInfo runInfo,
        double[] allMasses,
        double[][] signals,
        List<AutobEvent> autobEvents,
        double dwellTime,
        int numAcc, int acquisitionCount) {
      this.runInfo = runInfo;
      this.allMasses = allMasses;
      this.signals = signals;
      this.autobEvents = autobEvents;
      this.dwellTime = dwellTime;
      this.numAcc = numAcc;
      this.acquisitionCount = acquisitionCount;
    }
  }

  /**
   * Cache keyed on canonical (real) path.  Access must be performed inside a
   * {@code synchronized(CACHE)} block.
   */
  private static final Map<Path, SoftReference<CachedRun>> CACHE = new HashMap<>();

  /**
   * Removes the cached entry for {@code directory}, if any.  The next call
   * for this directory will reload from disk.  Useful when the underlying
   * files change between calls in the same JVM session.
   *
   * @param directory path to the Nu Instruments data directory
   * @throws IOException if the canonical path cannot be resolved
   */
  public static void invalidateCache(Path directory) throws IOException {
    Path key = directory.toRealPath();
    synchronized (CACHE) {
      CACHE.remove(key);
    }
    LOGGER.debug("invalidateCache: evicted entry for " + key);
  }

  /**
   * Returns the {@link CachedRun} for {@code directory}, loading from disk if
   * necessary.
   *
   * <h3>Concurrency contract</h3>
   * <ol>
   *   <li>The map lookup and the cache miss load are both performed inside a
   *       single {@code synchronized(CACHE)} block, preventing two threads from
   *       simultaneously discovering a miss and performing a double load.</li>
   *   <li>The strong reference returned by this method is held by the caller
   *       for the duration of its call, preventing the GC from clearing the
   *       {@link SoftReference} between the lookup and the use of the data.</li>
   * </ol>
   *
   * @param directory path to the Nu Instruments data directory
   * @return a strong reference to the cached run data
   * @throws IOException              on any file-read error
   * @throws IllegalArgumentException if a required file is missing
   */
  private static CachedRun getOrLoadCachedRun(Path directory) throws IOException {
    Path key = directory.toRealPath();

    synchronized (CACHE) {
      SoftReference<CachedRun> ref = CACHE.get(key);
      if (ref != null) {
        CachedRun cached = ref.get();
        if (cached != null) {
          LOGGER.debug("getOrLoadCachedRun: cache hit for " + key);
          // Return the strong reference while still inside synchronized so the
          // caller holds it before we leave the block.  The GC cannot clear a
          // SoftReference that has a live strong referent.
          return cached;
        }
        LOGGER.debug("getOrLoadCachedRun: SoftReference was cleared for " + key
            + " – reloading from disk");
      }

      // Cache miss (or cleared ref): load all data from disk.
      LOGGER.debug("getOrLoadCachedRun: cache miss for " + key + " – loading from disk");
      CachedRun fresh = loadRunFromDisk(directory);
      CACHE.put(key, new SoftReference<>(fresh));
      return fresh;  // strong ref returned while still inside synchronized
    }
  }

  /**
   * Performs the full file I/O for a run directory and returns a
   * {@link CachedRun}.  Called only on cache miss.  Must be called from within
   * the {@code synchronized(CACHE)} block in {@link #getOrLoadCachedRun}.
   */
  private static CachedRun loadRunFromDisk(Path directory) throws IOException {
    validateDirectory(directory);

    NuRunInfo runInfo = parseRunInfo(directory.resolve("run.info"));
    List<JsonObject> integIndex = parseJsonIndex(directory.resolve("integrated.index"));
    List<JsonObject> autobIndex = parseJsonIndex(directory.resolve("autob.index"));
    double[] segDelays = buildSegmentDelays(runInfo);
    int numAcc = runInfo.totalAccumulations();
    double dwellTime = runInfo.dwellTimeSeconds();
    double sia = runInfo.averageSingleIonArea;

    // --- Derive the full m/z axis from the first record ---
    JsonObject firstIndexEntry = integIndex.get(0);
    MassAxisSeed seed = readFirstIntegRecord(
        directory.resolve(firstIndexEntry.get("FileNum").getAsInt() + ".integ"),
        firstIndexEntry.get("FirstCycNum").getAsInt(),
        firstIndexEntry.get("FirstSegNum").getAsInt(),
        firstIndexEntry.get("FirstAcqNum").getAsInt()
    );

    // --- Derive numTimePoints ---
    JsonObject lastIndexEntry = integIndex.get(integIndex.size() - 1);
    int acquisitionCount = runInfo.segmentInfoList.get(0).acquisitionCount;
    int numTimePoints = 0;
    for (JsonObject idxEntry : integIndex) {
      int candidate = readLastTimeIndex(
          directory.resolve(idxEntry.get("FileNum").getAsInt() + ".integ"),
          numAcc,
          acquisitionCount
      );
      if (candidate > numTimePoints) numTimePoints = candidate;
    }
    numTimePoints += 1; // readLastTimeIndex returns 0-based index, size = index + 1

    if (seed == null) {
      LOGGER.warn("loadRunFromDisk: first integ file is empty; returning empty CachedRun");
      return new CachedRun(runInfo, new double[0], new double[0][], new ArrayList<>(),
          dwellTime, numAcc, acquisitionCount);
    }

    double[] allMasses = convertCentresToMassesWithMassCal(
        seed.segNumber, seed.centres, runInfo.massCalCoefficients, segDelays);
    int numMasses = allMasses.length;

    // --- Preallocate and fill the full signal matrix ---
    double[][] signals = new double[numTimePoints][numMasses];
    fillSignalMatrixFromIntegFiles(directory, integIndex, signals, numTimePoints, numMasses, numAcc,acquisitionCount);

    // --- Convert raw ADC counts → ion counts in-place ---
    if (sia > 0.0) {
      for (int t = 0; t < numTimePoints; t++) {
        for (int c = 0; c < numMasses; c++) {
          signals[t][c] /= sia;
        }
      }
    } else {
      LOGGER.warn("loadRunFromDisk: AverageSingleIonArea is 0. Skipping ion-count conversion");
    }

    // --- Collect autob events ---
    List<AutobEvent> autobEvents = collectAllAutobEvents(directory, autobIndex);

    LOGGER.debug("loadRunFromDisk: loaded " + numMasses + " masses × "
        + numTimePoints + " time points from " + directory);

    return new CachedRun(runInfo, allMasses, signals, autobEvents, dwellTime, numAcc, acquisitionCount);
  }

  // =========================================================================
  // Public entry points
  // =========================================================================

  /**
   * Stage 1: returns every m/z channel present in the run together with the
   * parsed {@code run.info} metadata.
   *
   * <p>Populates the cache on the first call; subsequent calls are served from
   * the cache without any file I/O.
   *
   * @param directory path to the Nu Instruments data directory
   * @return {@link NuReaderResult} with {@link NuReaderResult#mzValues} populated;
   *         {@link NuReaderResult#channelData} and
   *         {@link NuReaderResult#blankedTimeRanges} are empty at this stage
   * @throws IOException              on any file-read error
   * @throws IllegalArgumentException if a required file is missing
   */
  public static NuReaderResult readAvailableMZ(Path directory) throws IOException {
    // Hold a strong reference for the duration of this call.
    CachedRun run = getOrLoadCachedRun(directory);

    // Deep-copy the mass array so callers cannot mutate the cached value.
    double[] massesCopy = Arrays.copyOf(run.allMasses, run.allMasses.length);
    Arrays.sort(massesCopy);

    List<Double> mzValues = new ArrayList<>(massesCopy.length);
    for (double m : massesCopy) {
      mzValues.add(m);
    }

    return new NuReaderResult(mzValues, run.runInfo);
  }

  /**
   * Stage 2 – convenience overload using the default mass tolerance
   * ({@value #DEFAULT_MAX_MASS_DIFF_DA} Da).
   */
  public static NuReaderResult readSelectedChannels(
      Path directory,
      List<Double> requestedMz)
      throws IOException {
    return readSelectedChannels(directory, requestedMz, DEFAULT_MAX_MASS_DIFF_DA);
  }

  /**
   * Stage 2: reads signal data for the requested m/z channels from the cache
   * (loading from disk on first access), computes auto-blanked time-index
   * ranges, and returns a fully populated {@link NuReaderResult}.
   *
   * <p>The cache always stores the <em>full</em> signal matrix regardless of
   * which channels are requested here, so requesting a different subset on a
   * subsequent call is served entirely from cached data.
   *
   * <p>Each requested m/z is matched to the nearest recorded channel within
   * {@code maxMassDiffDa}. When a match fails, a warning is logged.
   *
   * @param directory     path to the Nu Instruments data directory
   * @param requestedMz   m/z values (Da) to load
   * @param maxMassDiffDa maximum allowed distance (Da) between a requested m/z
   *                      and the nearest recorded channel
   * @return {@link NuReaderResult} with all fields populated
   * @throws IOException              on any file-read error
   * @throws IllegalArgumentException if a required file is missing
   */
  public static NuReaderResult readSelectedChannels(
      Path directory,
      List<Double> requestedMz,
      double maxMassDiffDa)
      throws IOException {

    // Hold a strong reference for the duration of this call so the GC cannot
    // clear the SoftReference while we are still working with the cached data.
    CachedRun run = getOrLoadCachedRun(directory);

    int numMasses = run.allMasses.length;
    int numTimePoints = run.signals.length;

    // Sorted m/z list for NuReaderResult.mzValues (deep copy, then sort).
    double[] sortedMasses = Arrays.copyOf(run.allMasses, numMasses);
    Arrays.sort(sortedMasses);
    List<Double> mzValuesList = new ArrayList<>(numMasses);
    for (double m : sortedMasses) {
      mzValuesList.add(m);
    }

    // --- Match requested m/z to column indices ---
    int[] columnIndices = matchMzToIndices(requestedMz, run.allMasses, maxMassDiffDa);

    // --- Compute blanked ranges from the cached autob events ---
    // computeBlankedRangesByColumn is pure computation over the event list;
    // no file I/O needed.
    Map<Integer, List<int[]>> blankedByCol = computeBlankedRangesByColumn(
        run.autobEvents, run.allMasses, run.numAcc,run.acquisitionCount,
        run.runInfo.blMassCalStartCoef, run.runInfo.blMassCalEndCoef);

    // --- Build time axis ---
    double[] timeAxis = buildTimeAxis(numTimePoints, run.dwellTime);

    // --- Assemble per-channel TISeries and blanking metadata ---
    // extractColumnFrom2DArray produces a fresh copy each time, so the caller
    // cannot corrupt the cached signals matrix.
    Map<Double, TISeries> channelData = new HashMap<>();
    Map<Double, List<int[]>> blankedTimeRanges = new HashMap<>();

    for (int r = 0; r < requestedMz.size(); r++) {
      int colIdx = columnIndices[r];
      if (colIdx < 0) {
        double nearest = nearestMass(requestedMz.get(r), run.allMasses);
        double actualGap = Math.abs(nearest - requestedMz.get(r));
        LOGGER.warn("readSelectedChannels: no match for requested m/z "
            + requestedMz.get(r) + " Da"
            + " – nearest channel is " + nearest + " Da"
            + " (gap " + String.format("%.4f", actualGap) + " Da,"
            + " tolerance " + maxMassDiffDa + " Da); skipping");
        continue;
      }

      double matchedMz = run.allMasses[colIdx];

      // Deep copy: the column slice is independent of the cached matrix.
      double[] channelIntensity = extractColumnFrom2DArray(run.signals, colIdx);
      TISeries series = new TISeriesRAM(timeAxis, channelIntensity);

      channelData.put(matchedMz, series);

      List<int[]> colRanges = blankedByCol.getOrDefault(colIdx, new ArrayList<>());
      blankedTimeRanges.put(matchedMz, Collections.unmodifiableList(colRanges));
    }

    return new NuReaderResult(mzValuesList, run.runInfo, channelData, blankedTimeRanges);
  }

  // =========================================================================
  // Directory validation
  // =========================================================================

  private static void validateDirectory(Path directory) {
    if (!Files.isDirectory(directory)) {
      throw new IllegalArgumentException(
          "NuReader: path is not a directory: " + directory);
    }
    String[] required = {"run.info", "integrated.index", "autob.index"};
    for (String name : required) {
      if (!Files.exists(directory.resolve(name))) {
        throw new IllegalArgumentException(
            "NuReader: missing required file '" + name + "' in " + directory);
      }
    }
  }

  // =========================================================================
  // run.info parsing
  // =========================================================================

  private static NuRunInfo parseRunInfo(Path infoPath) throws IOException {
    String json = Files.readString(infoPath);
    JsonObject root = new Gson().fromJson(json, JsonObject.class);

    double[] massCal = jsonArrayToDoubleArray(root.getAsJsonArray("MassCalCoefficients"));
    double[] startCoef = jsonArrayToDoubleArray(root.getAsJsonArray("BlMassCalStartCoef"));
    double[] endCoef = jsonArrayToDoubleArray(root.getAsJsonArray("BlMassCalEndCoef"));
    int numAcc1 = root.get("NumAccumulations1").getAsInt();
    int numAcc2 = root.get("NumAccumulations2").getAsInt();
    double sia = root.get("AverageSingleIonArea").getAsDouble();

    JsonArray segJson = root.getAsJsonArray("SegmentInfo");
    List<NuRunInfo.SegmentInfo> segments = new ArrayList<>(segJson.size());
    for (int i = 0; i < segJson.size(); i++) {
      JsonObject s = segJson.get(i).getAsJsonObject();
      segments.add(new NuRunInfo.SegmentInfo(
          s.get("Num").getAsInt(),
          s.get("AcquisitionPeriodNs").getAsDouble(),
          s.get("AcquisitionTriggerDelayNs").getAsDouble(),
          s.get("AcquisitionCount").getAsInt()
      ));
    }

    return new NuRunInfo(massCal, startCoef, endCoef, numAcc1, numAcc2, sia, segments);
  }

  // =========================================================================
  // JSON index parsing
  // =========================================================================

  private static List<JsonObject> parseJsonIndex(Path indexPath) throws IOException {
    String json = Files.readString(indexPath);
    JsonArray arr = new Gson().fromJson(json, JsonArray.class);
    List<JsonObject> list = new ArrayList<>(arr.size());
    for (int i = 0; i < arr.size(); i++) {
      list.add(arr.get(i).getAsJsonObject());
    }
    return list;
  }

  // =========================================================================
  // .integ binary reading  (v2: no IntegRecord boxing)
  // =========================================================================

  /**
   * Lightweight carrier for the mass-axis seed data read from the first record
   * of the first {@code .integ} file.
   */
  private static final class MassAxisSeed {
    final int segNumber;
    final double[] centres;

    MassAxisSeed(int segNumber, double[] centres) {
      this.segNumber = segNumber;
      this.centres = centres;
    }
  }

  /**
   * Reads only the very first acquisition record from a {@code .integ} file.
   *
   * <p>Binary layout per record (little-endian):
   * <pre>
   *   uint32  cycNumber
   *   uint32  segNumber
   *   uint32  acqNumber
   *   uint32  numResults
   *   numResults × 13 bytes:
   *     float32  centre
   *     float32  signal   (discarded)
   *     float32  unused
   *     int8     unused
   * </pre>
   */
  private static MassAxisSeed readFirstIntegRecord(
      Path filePath,
      int expectedCyc,
      int expectedSeg,
      int expectedAcq)
      throws IOException {

    if (!Files.exists(filePath)) {
      LOGGER.warn("readFirstIntegRecord: file not found: " + filePath);
      return null;
    }

    byte[] bytes = Files.readAllBytes(filePath);
    ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

    if (buf.remaining() < 16) {
      return null;
    }

    int firstCyc = buf.getInt();
    int firstSeg = buf.getInt();
    int firstAcq = buf.getInt();
    int numResults = buf.getInt();

    if (firstCyc != expectedCyc) {
      throw new IOException("readFirstIntegRecord: FirstCycNum mismatch in " + filePath
          + " (expected " + expectedCyc + ", got " + firstCyc + ")");
    }
    if (firstSeg != expectedSeg) {
      throw new IOException("readFirstIntegRecord: FirstSegNum mismatch in " + filePath
          + " (expected " + expectedSeg + ", got " + firstSeg + ")");
    }
    if (firstAcq != expectedAcq) {
      throw new IOException("readFirstIntegRecord: FirstAcqNum mismatch in " + filePath
          + " (expected " + expectedAcq + ", got " + firstAcq + ")");
    }

    if (buf.remaining() < numResults * 13) {
      return null;
    }

    double[] centres = new double[numResults];
    for (int p = 0; p < numResults; p++) {
      centres[p] = buf.getFloat();  // centre – keep
      buf.getFloat();               // signal  – discard
      buf.getFloat();               // unused  – discard
      buf.get();                    // unused  – discard
    }

    return new MassAxisSeed(firstSeg, centres);
  }

  /**
   * Reads the acqNumber from the last complete record in a {@code .integ} file.
   */
  private static int readLastTimeIndex(
      Path filePath,
      int numAcc,
      int acquisitionCount)
      throws IOException {

    if (!Files.exists(filePath)) {
      LOGGER.warn("readLastTimeIndex: file not found: " + filePath);
      return 0;
    }

    byte[] bytes = Files.readAllBytes(filePath);
    ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

    if (buf.remaining() < 16) return 0;

    // Read first record header to get stride
    buf.getInt(); // cycNumber
    buf.getInt(); // segNumber
    buf.getInt(); // acqNumber
    int numResults = buf.getInt();
    int bytesPerRecord = 16 + numResults * 13;

    int numRecords = bytes.length / bytesPerRecord;
    if (numRecords == 0) return 0;

    buf.position((numRecords - 1) * bytesPerRecord);
    int lastCyc = buf.getInt();
    buf.getInt(); // segNumber
    int lastAcq = buf.getInt();

    return ((lastCyc - 1) * acquisitionCount + lastAcq) / numAcc;
  }

  /**
   * Core v2 reader: iterates all {@code .integ} files and writes signal values
   * directly into the preallocated {@code signals} matrix.
   *
   * <p>Binary layout per peak (little-endian):
   * <pre>
   *   float32  centre   (discarded)
   *   float32  signal   (written into signals[timeIdx][p])
   *   float32  unused
   *   int8     unused
   * </pre>
   */
  private static void fillSignalMatrixFromIntegFiles(
      Path directory,
      List<JsonObject> integIndex,
      double[][] signals,
      int numTimePoints,
      int numMasses,
      int numAcc,
      int acquisitionCount)
      throws IOException {

    int fileCounter = 0;

    for (JsonObject idx : integIndex) {
      fileCounter++;

      Path filePath = directory.resolve(idx.get("FileNum").getAsInt() + ".integ");
      int expectedCyc = idx.get("FirstCycNum").getAsInt();
      int expectedSeg = idx.get("FirstSegNum").getAsInt();
      int expectedAcq = idx.get("FirstAcqNum").getAsInt();

      if (!Files.exists(filePath)) {
        LOGGER.warn("fillSignalMatrix: file not found: " + filePath);
        continue;
      }

      byte[] bytes = Files.readAllBytes(filePath);
      ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

      if (buf.remaining() < 16) {
        continue;
      }

      int firstCyc = buf.getInt();
      int firstSeg = buf.getInt();
      int firstAcq = buf.getInt();
      int numResults = buf.getInt();

      if (firstCyc != expectedCyc) {
        throw new IOException("fillSignalMatrix: FirstCycNum mismatch in " + filePath
            + " (expected " + expectedCyc + ", got " + firstCyc + ")");
      }
      if (firstSeg != expectedSeg) {
        throw new IOException("fillSignalMatrix: FirstSegNum mismatch in " + filePath
            + " (expected " + expectedSeg + ", got " + firstSeg + ")");
      }
      if (firstAcq != expectedAcq) {
        throw new IOException("fillSignalMatrix: FirstAcqNum mismatch in " + filePath
            + " (expected " + expectedAcq + ", got " + firstAcq + ")");
      }

      buf.rewind();

      int bytesPerRecord = 16 + numResults * 13;
      int colsToWrite = Math.min(numResults, numMasses);

      while (buf.remaining() >= bytesPerRecord) {
        int cycNum = buf.getInt();
        buf.getInt();  // segNumber
        int acqNum = buf.getInt();
        buf.getInt();  // numResults

        int timeIdx = ((cycNum - 1) * acquisitionCount + acqNum) / numAcc;

        boolean inBounds = timeIdx >= 0 && timeIdx < numTimePoints;
        double[] row = inBounds ? signals[timeIdx] : null;

        for (int p = 0; p < numResults; p++) {
          buf.getFloat();               // centre – discard
          float sig = buf.getFloat();   // signal – keep
          buf.getFloat();               // unused – discard
          buf.get();                    // unused – discard

          if (row != null && p < colsToWrite) {
            row[p] = sig;
          }
        }
      }

      if (fileCounter % 20 == 0) {
        LOGGER.trace("fillSignalMatrix: processed file " + fileCounter
            + " (" + filePath + ")");
      }
    }
  }

  // =========================================================================
  // .autob binary reading
  // =========================================================================

  /**
   * Internal: one blanking event from a {@code .autob} file.
   *
   * <p>Binary layout per event (little-endian):
   * <pre>
   *   uint32  cycNumber
   *   uint32  segNumber
   *   uint32  acqNumber
   *   uint32  trigStartTime    (skipped)
   *   uint32  trigEndTime      (skipped)
   *   uint8   type             0 = blanker opened, 1 = blanker closed
   *   int32   numEdges
   *   numEdges × uint32        raw ToF edge positions
   * </pre>
   */
  private static final class AutobEvent {
    final int cycNumber;
    final int segNumber;
    final int acqNumber;
    final int type;
    final long[] edges;

    AutobEvent(int cycNumber, int segNumber, int acqNumber, int type, long[] edges) {
      this.cycNumber = cycNumber;
      this.segNumber = segNumber;
      this.acqNumber = acqNumber;
      this.type = type;
      this.edges = edges;
    }
  }

  private static List<AutobEvent> readAutobFile(Path filePath) throws IOException {
    List<AutobEvent> events = new ArrayList<>();

    if (!Files.exists(filePath)) {
      LOGGER.warn("readAutobFile: file not found: " + filePath);
      return events;
    }

    byte[] fixedBuf = new byte[25];

    try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {
      while (raf.read(fixedBuf) == fixedBuf.length) {
        ByteBuffer bb = ByteBuffer.wrap(fixedBuf).order(ByteOrder.LITTLE_ENDIAN);

        int cycNum = bb.getInt();
        int segNum = bb.getInt();
        int acqNum = bb.getInt();
        bb.getInt(); // trigStartTime – skip
        bb.getInt(); // trigEndTime   – skip

        int type = bb.get() & 0xFF;
        int numEdges = bb.getInt();

        long[] edges = new long[numEdges];
        if (numEdges > 0) {
          byte[] edgeBuf = new byte[numEdges * 4];
          int bytesRead = raf.read(edgeBuf);
          if (bytesRead != edgeBuf.length) {
            break;
          }
          ByteBuffer eb = ByteBuffer.wrap(edgeBuf).order(ByteOrder.LITTLE_ENDIAN);
          for (int i = 0; i < numEdges; i++) {
            edges[i] = Integer.toUnsignedLong(eb.getInt());
          }
        }

        events.add(new AutobEvent(cycNum, segNum, acqNum, type, edges));
      }
    }

    return events;
  }

  private static List<AutobEvent> collectAllAutobEvents(
      Path directory,
      List<JsonObject> autobIndex)
      throws IOException {

    List<AutobEvent> all = new ArrayList<>();
    for (JsonObject idx : autobIndex) {
      List<AutobEvent> batch = readAutobFile(
          directory.resolve(idx.get("FileNum").getAsInt() + ".autob"));
      all.addAll(batch);
    }
    return all;
  }

  // =========================================================================
  // Blanking range computation
  // =========================================================================

  /**
   * Computes, for each mass column, the list of time-index intervals that
   * were active during auto-blanking events.  Signal data is never modified.
   */
  private static Map<Integer, List<int[]>> computeBlankedRangesByColumn(
      List<AutobEvent> events,
      double[] masses,
      int numAcc,
      int acquisitionCount,
      double[] startCoef,
      double[] endCoef) {

    Map<Integer, List<int[]>> result = new HashMap<>();

    double[] sortedMasses = Arrays.copyOf(masses, masses.length);
    Arrays.sort(sortedMasses);

    AutobEvent openEvent = null;

    for (AutobEvent event : events) {
      if (event.type == 0 && openEvent == null) {
        openEvent = event;
      } else if (event.type == 1 && openEvent != null) {

        int tStart = ((openEvent.cycNumber - 1) * acquisitionCount + openEvent.acqNumber) / numAcc;
        int tEnd   = ((event.cycNumber - 1) * acquisitionCount + event.acqNumber) / numAcc;

        long[] edges = openEvent.edges;
        int numPairs = edges.length / 2;

        for (int pair = 0; pair < numPairs; pair++) {
          double massStart = edgeToMass(edges[pair * 2], startCoef);
          double massEnd = edgeToMass(edges[pair * 2 + 1], endCoef);

          if (massStart >= massEnd) {
            continue;
          }

          int sortedColStart = lowerBound(sortedMasses, massStart);
          int sortedColEnd = lowerBound(sortedMasses, massEnd);

          if (sortedColStart == sortedColEnd) {
            continue;
          }

          List<Integer> affectedColumns = new ArrayList<>();
          for (int c = 0; c < masses.length; c++) {
            if (masses[c] >= massStart && masses[c] < massEnd) {
              affectedColumns.add(c);
            }
          }

          int[] range = new int[]{tStart, tEnd};
          for (int colIdx : affectedColumns) {
            if (!result.containsKey(colIdx)) {
              result.put(colIdx, new ArrayList<>());
            }
            result.get(colIdx).add(range);
          }
        }

        openEvent = null;
      }
    }

    return result;
  }

  /**
   * Converts a raw ToF edge position to a mass value using a two-coefficient
   * blanker calibration.
   *
   * <p>Python equivalent: {@code (coef[0] + coef[1] * edge * 1.25) ** 2}
   */
  private static double edgeToMass(long edgeRaw, double[] coef) {
    double t = coef[0] + coef[1] * edgeRaw * 1.25;
    return t * t;
  }

  // =========================================================================
  // Mass-calibration helpers
  // =========================================================================

  /**
   * Converts peak-centre times to m/z values.
   *
   * <p>Python equivalent ({@code get_masses_from_nu_data}):
   * <pre>
   *   adjustedTime = centre * 0.5 + segmentDelay
   *   mz = (a + adjustedTime * b)²
   * </pre>
   */
  private static double[] convertCentresToMassesWithMassCal(
      int segNumber,
      double[] centres,
      double[] calCoef,
      double[] segDelays) {

    double delay = (segNumber >= 1 && segNumber <= segDelays.length)
        ? segDelays[segNumber - 1] : 0.0;

    double[] masses = new double[centres.length];
    for (int i = 0; i < centres.length; i++) {
      double adjustedTime = centres[i] * 0.5 + delay;
      double sqrtMz = calCoef[0] + adjustedTime * calCoef[1];
      masses[i] = sqrtMz * sqrtMz;
    }
    return masses;
  }

  /**
   * Builds a segment-delay array indexed 0-based (delay for segment N → index N-1).
   */
  private static double[] buildSegmentDelays(NuRunInfo runInfo) {
    int maxSegNum = 0;
    for (NuRunInfo.SegmentInfo s : runInfo.segmentInfoList) {
      if (s.num > maxSegNum) {
        maxSegNum = s.num;
      }
    }
    double[] delays = new double[maxSegNum];
    for (NuRunInfo.SegmentInfo s : runInfo.segmentInfoList) {
      if (s.num >= 1 && s.num <= maxSegNum) {
        delays[s.num - 1] = s.acquisitionTriggerDelayNs;
      }
    }
    return delays;
  }

  // =========================================================================
  // Channel matching
  // =========================================================================

  /**
   * Matches each requested m/z to the nearest column index in {@code allMasses}.
   * Returns {@code -1} for any m/z with no match within {@code maxMassDiffDa}.
   */
  private static int[] matchMzToIndices(
      List<Double> requestedMz,
      double[] allMasses,
      double maxMassDiffDa) {

    int[] indices = new int[requestedMz.size()];
    for (int r = 0; r < requestedMz.size(); r++) {
      double target = requestedMz.get(r);
      int bestIdx = -1;
      double bestDiff = Double.MAX_VALUE;

      for (int c = 0; c < allMasses.length; c++) {
        double diff = Math.abs(allMasses[c] - target);
        if (diff < bestDiff) {
          bestDiff = diff;
          bestIdx = c;
        }
      }

      indices[r] = (bestIdx >= 0 && bestDiff <= maxMassDiffDa) ? bestIdx : -1;
    }
    return indices;
  }

  /**
   * Returns the value in {@code allMasses} closest to {@code target}.
   * Used exclusively for diagnostic logging.
   */
  private static double nearestMass(double target, double[] allMasses) {
    double nearest = Double.NaN;
    double bestDiff = Double.MAX_VALUE;
    for (double m : allMasses) {
      double diff = Math.abs(m - target);
      if (diff < bestDiff) {
        bestDiff = diff;
        nearest = m;
      }
    }
    return nearest;
  }

  // =========================================================================
  // Time-axis construction
  // =========================================================================

  /**
   * Builds an index-based time axis: {@code [dt, 2*dt, …, n*dt]}.
   */
  private static double[] buildTimeAxis(int numTimePoints, double dwellTime) {
    double[] time = new double[numTimePoints];
    for (int i = 0; i < numTimePoints; i++) {
      time[i] = (i + 1) * dwellTime;
    }
    return time;
  }

  // =========================================================================
  // Small array utilities
  // =========================================================================

  /**
   * Extracts one column from a row-major 2-D array as a fresh copy.
   *
   * @param matrix [row][col]
   * @param col    column index to extract
   * @return independent 1-D array of length {@code matrix.length}
   */
  private static double[] extractColumnFrom2DArray(double[][] matrix, int col) {
    double[] column = new double[matrix.length];
    for (int i = 0; i < matrix.length; i++) {
      column[i] = matrix[i][col];
    }
    return column;
  }

  /**
   * Returns the first index in {@code sorted} whose value is &ge; {@code value}
   * ({@code std::lower_bound} semantics, array must be sorted ascending).
   */
  private static int lowerBound(double[] sorted, double value) {
    int lo = 0;
    int hi = sorted.length;
    while (lo < hi) {
      int mid = (lo + hi) / 2;
      if (sorted[mid] < value) {
        lo = mid + 1;
      } else {
        hi = mid;
      }
    }
    return lo;
  }

  /**
   * Converts a Gson {@link JsonArray} of numbers to a primitive {@code double[]}.
   */
  private static double[] jsonArrayToDoubleArray(JsonArray arr) {
    double[] out = new double[arr.size()];
    for (int i = 0; i < arr.size(); i++) {
      out[i] = arr.get(i).getAsDouble();
    }
    return out;
  }
}