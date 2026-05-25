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

package io.nu;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import core.SpTool3Main;
import gui.dialog.notification.NotificationFactory;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import util.ArrUtils;
import util.Functional;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class NuReader_new {

  private static final Logger LOGGER = LogManager.getLogger(NuReader_new.class.getName());

  private static final Map<Path, SoftReference<byte[]>> DATA_CACHE = new HashMap<>();
  private static final Map<Path, Path> FILE_CACHE = new HashMap<>();


  public static ParsedNuData getFromCacheOrParse(Path directory,
                                                 @Nullable Functional progressTicker,
                                                 @Nullable AtomicDouble progressValue,
                                                 @Nullable AtomicBoolean isStopped) {
    ParsedNuData data = new ParsedNuData();

    try {
      Path key = directory.toRealPath();
      String fileName = key.getFileName().toString();

      boolean hitFromCache = false;
      boolean hitFromTemp = false;

      // try reading from data cache
      SoftReference<byte[]> ref = null;
      synchronized (DATA_CACHE) {
        ref = DATA_CACHE.get(key);
      }
      if (ref != null) {
        byte[] cached = ref.get();
        if (cached != null) {
          LOGGER.trace("[Cache] Loading data of file: " + key + " from compressed cache...");

          ByteArrayInputStream bais = new ByteArrayInputStream(cached);
          BufferedInputStream bis = new BufferedInputStream(bais);
          ZstdInputStream zis = new ZstdInputStream(bis);
          LOGGER.trace("[Cache] Received data of file: " + fileName + " from cache. Path: " + key + ".");
          ObjectInputStream ois = new ObjectInputStream(zis);
          Object obj = ois.readObject();
          if (obj instanceof ParsedNuData) {
            data = ((ParsedNuData) obj);
            hitFromCache = true;
            LOGGER.trace("[Cache] Serialised object from data of file: " + fileName + " from cache. Path: " + key + ".");
          }
        }
      }


      // if we did not find anything in the data cache, try file cache
      if (!hitFromCache) {

        Path tempFile = null;
        synchronized (FILE_CACHE) {
          tempFile = FILE_CACHE.get(key);
        }
        if (tempFile != null) {
          LOGGER.trace("[Temp] Loading data of file: " + fileName + ". Path: " + key + " from compressed " +
              "temp file: " + tempFile + "...");
          byte[] compressedBytes = Files.readAllBytes(tempFile);
          DATA_CACHE.put(key, new SoftReference<>(compressedBytes));

          ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
          BufferedInputStream bufferedIn = new BufferedInputStream(bais);
          InputStream decompressed = new ZstdInputStream(bufferedIn);
          ObjectInputStream objectInputStream = new ObjectInputStream(decompressed);

          Object obj = objectInputStream.readObject();
          objectInputStream.close();
          decompressed.close();

          if (obj instanceof ParsedNuData) {
            data = (ParsedNuData) obj;
            hitFromTemp = true;
            LOGGER.trace("[Temp] Received data of file: " + fileName + ". Path: " + key
                + " from compressed temp file: " + tempFile + "...");
          }
        }
      }

      // we have to load!
      if (!hitFromCache && !hitFromTemp) {
        LOGGER.info("[Raw] Loading and parsing Nu data from binary raw data.");
        data = loadRunFromDisk(directory, progressTicker, progressValue, isStopped);

        // Store in the caches and temp for later purposes
        LOGGER.trace("Writing data of file: " + fileName + " to cache and temp file. Path: " + key + "...");

        // updates the progress inside the calling task if passed
        if (progressTicker != null && progressValue != null && isStopped != null) {
          progressValue.set(0.75);
          progressTicker.proceed();
          if (isStopped.get()) {
            return data;
          }
        }

        String fileID = "spTool3_tof_";
        UUID uuid = UUID.randomUUID();
        fileID += uuid.toString();
        File storageFileName = File.createTempFile(fileID, ".tmp");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedOutputStream buffered = new BufferedOutputStream(baos);

        ZstdOutputStream zipStream =
            new ZstdOutputStream(buffered)
                .setLevel(8)
                .setWorkers(8);

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(zipStream);

        // write
        objectOutputStream.writeObject(data);
        objectOutputStream.flush();
        zipStream.flush();
        buffered.flush();
        baos.flush();

        // extract compressed bytes
        byte[] compressedBytes = baos.toByteArray();

        // close manually
        objectOutputStream.close();
        zipStream.close();
        buffered.close();
        baos.close();

        // store in memory cache
        synchronized (DATA_CACHE) {
          DATA_CACHE.put(key, new SoftReference<>(compressedBytes));
        }

        // updates the progress inside the calling task if passed
        if (progressTicker != null && progressValue != null && isStopped != null) {
          progressValue.set(0.85);
          progressTicker.proceed();
          if (isStopped.get()) {
            return data;
          }
        }

        // write to temp file
        FileOutputStream fileOutputStream = new FileOutputStream(storageFileName);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);

        bufferedOutput.write(compressedBytes);
        bufferedOutput.flush();

        bufferedOutput.close();
        fileOutputStream.close();

        // update storage
        double sizeGB = compressedBytes.length / 1024.0 / 1024.0 / 1024.0;
        LOGGER.trace("Wrote data of file: " + fileName + " to cache and temp file. " +
            "Temp file size was: " + sizeGB + " GB. Full path: " + key);
        SpTool3Main.getRunTime().getTaskManager().notifyNewTempFile(sizeGB);

        try {
          double freeSpaceGB = storageFileName.getUsableSpace() / (1024.0 * 1024 * 1024);
          LOGGER.info("Remaining space on drive for temp files: " + Math.round(freeSpaceGB) + " GB.");
          if (freeSpaceGB < 5) {
            String warnStr = "The disk/drive for temp files is running low on memory!";
            warnStr += "\nRemaining space is: " + Math.round(freeSpaceGB) + " GB.";
            warnStr += "\nThe latest file is: " + storageFileName;
            warnStr += "\nPlease ensure there is enough on the respective drive!";

            NotificationFactory.openInfo(warnStr);
          }
        } catch (Exception e) {
          LOGGER.error("Cannot access remaining space on drive!");
        }

        // updates the progress inside the calling task if passed
        if (progressTicker != null && progressValue != null && isStopped != null) {
          progressValue.set(0.9);
          if (isStopped.get()) {
            return data;
          }
        }


        // store temp reference
        synchronized (DATA_CACHE) {
          FILE_CACHE.put(key, storageFileName.toPath());
        }
      }

      // finalise
      LOGGER.info("Finished data import for file: " + fileName + ".  Full path: " + key);

    } catch (IOException ioe) {
      LOGGER.error("Message: " + ioe.getMessage() + ". Stack trace: " + Arrays.toString(ioe.getStackTrace()));
    } catch (ClassNotFoundException e) {
      LOGGER.error("Message: " + e.getMessage() + ". Stack trace: " + Arrays.toString(e.getStackTrace()));

    }

    return data;
  }

  public static List<Double> listAvailableMZFromCacheOrParse(Path directory) {
    List<Double> mz = new ArrayList<>();

    try {
      Path key = directory.toRealPath();
      String fileName = key.getFileName().toString();

      boolean hitFromCache = false;
      boolean hitFromTemp = false;

      // try reading from data cache
      SoftReference<byte[]> ref = null;
      synchronized (DATA_CACHE) {
        ref = DATA_CACHE.get(key);
      }
      if (ref != null) {
        byte[] cached = ref.get();
        if (cached != null) {
          LOGGER.trace("[Cache] Loading data of file: " + key + " from compressed cache...");

          ByteArrayInputStream bais = new ByteArrayInputStream(cached);
          BufferedInputStream bis = new BufferedInputStream(bais);
          ZstdInputStream zis = new ZstdInputStream(bis);
          ObjectInputStream ois = new ObjectInputStream(zis);
          Object obj = ois.readObject();
          if (obj instanceof ParsedNuData) {
            ParsedNuData data = ((ParsedNuData) obj);
            mz.addAll(ArrUtils.arrToList(data.getMassToChargeRatios()));
            hitFromCache = true;
            LOGGER.trace("[Cache] Serialised object from data of file: " + fileName + " from cache. Path: " + key + ".");
          }
        }
      }


      // if we did not find anything in the data cache, try file cache
      if (!hitFromCache) {

        Path tempFile = null;
        synchronized (FILE_CACHE) {
          tempFile = FILE_CACHE.get(key);
        }
        if (tempFile != null) {
          LOGGER.trace("[Temp] Loading data of file: " + fileName + ". Path: " + key + " from compressed " +
              "temp file: " + tempFile + "...");
          byte[] compressedBytes = Files.readAllBytes(tempFile);
          DATA_CACHE.put(key, new SoftReference<>(compressedBytes));

          ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
          BufferedInputStream bufferedIn = new BufferedInputStream(bais);
          InputStream decompressed = new ZstdInputStream(bufferedIn);
          ObjectInputStream objectInputStream = new ObjectInputStream(decompressed);

          Object obj = objectInputStream.readObject();
          objectInputStream.close();
          decompressed.close();

          if (obj instanceof ParsedNuData) {
            ParsedNuData data = ((ParsedNuData) obj);
            mz.addAll(ArrUtils.arrToList(data.getMassToChargeRatios()));
            hitFromTemp = true;
            LOGGER.trace("[Temp] Received data of file: " + fileName + ". Path: " + key
                + " from compressed temp file: " + tempFile + "...");
          }
        }
      }

      // we have to load!
      if (!hitFromCache && !hitFromTemp) {
        LOGGER.info("[Raw] Listing available isotopes from Nu data from binary raw data.");
        mz = listIsotopesFromDisk(directory);

      }

      // finalise
      LOGGER.info("Finished listing available isotopes from file: " + fileName + ". Full path: " + key);

    } catch (IOException ioe) {
      LOGGER.error("Message: " + ioe.getMessage() + ". Stack trace: " + Arrays.toString(ioe.getStackTrace()));
    } catch (ClassNotFoundException e) {
      LOGGER.error("Message: " + e.getMessage() + ". Stack trace: " + Arrays.toString(e.getStackTrace()));

    }

    return mz;
  }

  /**
   * Default maximum allowed distance (Da) between a requested m/z and the nearest
   * recorded channel when no explicit tolerance is supplied.
   */
  private static final double DEFAULT_MAX_MASS_DIFF_DA = 0.1;

  /// =========================================================================
  /// ############# UTILITY METHODS ##################
  /// =========================================================================
  private static boolean isValidDirectory(Path directory) {
    // we expect dir here (not the run.info file!)
    boolean isValid = true;
    if (!Files.isDirectory(directory)) {
      LOGGER.error("NuReader: path is not a directory: " + directory);
      isValid = false;
    }

    // check if must-have files are present
    if (isValid) {
      String[] required = {"run.info", "integrated.index", "autob.index"};
      for (String name : required) {
        if (!Files.exists(directory.resolve(name))) {
          LOGGER.error("NuReader: missing required file '" + name + "' in " + directory);
          isValid = false;
        }
      }
    }
    return isValid;
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


  /**
   * Parses the JSON files that Nu uses to index its files, e.g., the .integ files.
   */

  private static List<JsonObject> parseJsonIndex(Path indexPath) {
    List<JsonObject> list = new ArrayList<>();
    try {
      String json = Files.readString(indexPath);
      JsonArray arr = new Gson().fromJson(json, JsonArray.class);
      for (int i = 0; i < arr.size(); i++) {
        list.add(arr.get(i).getAsJsonObject());
      }
    } catch (Exception e) {
      LOGGER.error("Cannot parse file: " + indexPath + ".");
    }
    return list;
  }

  private static List<AutobEvent_new> collectAllAutobEvents(
      Path directory,
      List<JsonObject> autobIndex) {

    List<AutobEvent_new> all = new ArrayList<>();

    try {
      for (JsonObject idx : autobIndex) {
        List<AutobEvent_new> batch = readAutobFile(
            directory.resolve(idx.get("FileNum").getAsInt() + ".autob"));
        all.addAll(batch);
      }
    } catch (Exception e) {
      LOGGER.error("Message: " + e.getMessage() + ". Stack trace: " + Arrays.toString(e.getStackTrace()));
    }


    return all;
  }

  private static List<AutobEvent_new> readAutobFile(Path filePath) throws IOException {
    List<AutobEvent_new> events = new ArrayList<>();

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

        events.add(new AutobEvent_new(cycNum, segNum, acqNum, type, edges));
      }
    }

    return events;
  }

  /**
   * Computes, for each m/z column index, the list of time-index intervals [tStart, tEnd]
   * during which the auto-blanker was active. Signal data is never modified — this is
   * purely metadata for the caller to use downstream (e.g. flagging or removing blanked points).
   *
   * <h2>How it works</h2>
   *
   * <p><b>1. Pairing open/close events.</b> Autob events come in pairs: type=0 means the
   * blanker opened, type=1 means it closed. The loop matches them like parentheses —
   * openEvent holds the pending open until a matching close arrives.
   *
   * <p><b>2. Computing the time range.</b> When a pair is found, tStart and tEnd are
   * derived from the open/close event's cycNumber and acqNumber using the same formula
   * as the integ time index.
   *
   * <p><b>3. Deriving the mass range from edges.</b> The open event carries raw ToF edge
   * positions. Each pair of edges [edges[2i], edges[2i+1]] defines a blanked mass window.
   * edgeToMass() converts a raw ToF position to Da using the blanker's own calibration
   * coefficients (BlMassCalStartCoef for the left edge, BlMassCalEndCoef for the right
   * edge — they differ). The conversion uses a fixed * 1.25 time scaling:
   * (coef[0] + coef[1] * edge * 1.25)². No segment delay offset is applied here — the
   * blanker operates on raw ToF edge positions with its own independent calibration, and
   * segment delays are specific to the integ acquisition timing only.
   *
   * <p><b>4. Finding affected columns.</b> For each mass window, the method finds which
   * column indices in masses[] fall within [massStart, massEnd). A fast lowerBound check
   * on the sorted mass array provides an early exit if no masses fall in the window.
   * Otherwise a linear scan over the unsorted masses[] finds the actual column indices.
   * Note: the linear scan is somewhat redundant with the lowerBound check and could be
   * replaced by iterating between sortedColStart and sortedColEnd directly, but as written
   * it is correct.
   *
   * <p><b>5. Storing the range.</b> Appends [tStart, tEnd] to the list for each affected
   * column index in the result map.
   */

  /**
   * Computes, for each mass column, the list of time-index intervals that
   * were active during auto-blanking events.  Signal data is never modified.
   * <p>
   * Important: the key is an integer, as is specifies the INDEX of the mz in the allMZs array/list!
   */
  private static Map<Integer, List<int[]>> computeBlankedRangesByColumn(
      List<AutobEvent_new> events,
      double[] masses,
      int numAcc,
      int acquisitionCount,
      double[] startCoef,
      double[] endCoef) {

    Map<Integer, List<int[]>> result = new HashMap<>();

    double[] sortedMasses = Arrays.copyOf(masses, masses.length);
    Arrays.sort(sortedMasses);

    AutobEvent_new openEvent = null;

    for (AutobEvent_new event : events) {
      if (event.type == 0 && openEvent == null) {
        openEvent = event;
      } else if (event.type == 1 && openEvent != null) {

        int tStart = ((openEvent.cycNumber - 1) * acquisitionCount + openEvent.acqNumber) / numAcc;
        int tEnd = ((event.cycNumber - 1) * acquisitionCount + event.acqNumber) / numAcc;

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
   * Builds a segment-delay array indexed 0-based (delay for segment N → index N-1).
   */
  private static double[] extractSegmentDelays(NuRunInfo_new runInfo) {

    List<Double> delayList = new ArrayList<>();
    for (NuRunInfo_new.SegmentInfo segInfo : runInfo.segmentInfoList) {
      delayList.add(segInfo.acquisitionTriggerDelayNs);
    }
    double[] delays = ArrUtils.doubleCollToArr(delayList);

    return delays;
  }

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

    double[] masses = new double[centres.length];

    // identify timing delay for the respective segment
    double delay = 0;

    if (segNumber >= 1 && segNumber <= segDelays.length) {
      delay = segDelays[segNumber - 1];
    }

    for (int i = 0; i < centres.length; i++) {
      double adjustedTime = centres[i] * 0.5 + delay;
      double sqrtMz = calCoef[0] + adjustedTime * calCoef[1];
      masses[i] = sqrtMz * sqrtMz;
    }
    return masses;
  }

  /// =========================================================================
  /// run.info parsing
  /// =========================================================================

  private static NuRunInfo_new parseRunInfo(Path infoPath) {
    NuRunInfo_new runInfo;
    try {
      String json = Files.readString(infoPath);
      JsonObject root = new Gson().fromJson(json, JsonObject.class);

      double[] massCal = jsonArrayToDoubleArray(root.getAsJsonArray("MassCalCoefficients"));
      double[] startCoef = jsonArrayToDoubleArray(root.getAsJsonArray("BlMassCalStartCoef"));
      double[] endCoef = jsonArrayToDoubleArray(root.getAsJsonArray("BlMassCalEndCoef"));
      int numAcc1 = root.get("NumAccumulations1").getAsInt();
      int numAcc2 = root.get("NumAccumulations2").getAsInt();
      double sia = root.get("AverageSingleIonArea").getAsDouble();

      JsonArray segJson = root.getAsJsonArray("SegmentInfo");
      List<NuRunInfo_new.SegmentInfo> segments = new ArrayList<>(segJson.size());
      for (int i = 0; i < segJson.size(); i++) {
        JsonObject s = segJson.get(i).getAsJsonObject();
        segments.add(new NuRunInfo_new.SegmentInfo(
            s.get("Num").getAsInt(),
            s.get("AcquisitionPeriodNs").getAsDouble(),
            s.get("AcquisitionTriggerDelayNs").getAsDouble(),
            s.get("AcquisitionCount").getAsInt()
        ));
      }

      runInfo = new NuRunInfo_new(massCal, startCoef, endCoef, numAcc1, numAcc2, sia, segments);
    } catch (IOException e) {
      runInfo = new NuRunInfo_new();
    }
    return runInfo;
  }

  /// =========================================================================
  /// data/.integ parsing
  /// =========================================================================

  public static ParsedNuData loadRunFromDisk(Path directory,
                                             @Nullable Functional progressTicker,
                                             @Nullable AtomicDouble progressValue,
                                             @Nullable AtomicBoolean isStopped) {
    ParsedNuData data = new ParsedNuData();

    // ensure path is dir and run.info, ..., are present
    boolean isValidDirectory = isValidDirectory(directory);

    mainBracket:
    if (isValidDirectory) {

      // (1) read the run.info file
      NuRunInfo_new runInfo = parseRunInfo(directory.resolve("run.info"));

      int numAcc = runInfo.totalAccumulations();
      double dwellTime = runInfo.dwellTimeSeconds();
      double sia = runInfo.averageSingleIonArea;
      double invSia = 1.0 / sia;

      // (2) read the indexing files (e.g., JSON file that contains reference of all .integ files)
      List<JsonObject> integratedIndex = parseJsonIndex(directory.resolve("integrated.index"));
      List<JsonObject> autobIndex = parseJsonIndex(directory.resolve("autob.index"));

      // (3) Store auto-blanking events
      List<AutobEvent_new> autobEvents = collectAllAutobEvents(directory, autobIndex);

      // (4) for each segment, gather delays (I believe, one delay value per segment)
      double[] segDelays = extractSegmentDelays(runInfo);

      if (!runInfo.segmentInfoList.isEmpty() && !integratedIndex.isEmpty() && sia > 0 && dwellTime > 0) {

        // (5) single pass over all .integ files
        // estimate capacity from index: stride between consecutive FirstAcqNum values divided by numAcc
        int recordsPerIntegFile;
        if (integratedIndex.size() == 1) {
          // can only take number from first entry if size == 1
          recordsPerIntegFile = integratedIndex.get(0).get("FirstAcqNum").getAsInt();
        } else {
          recordsPerIntegFile = integratedIndex.get(1).get("FirstAcqNum").getAsInt()
              - integratedIndex.get(0).get("FirstAcqNum").getAsInt();
        }
        int estimatedPointsPerMz = (recordsPerIntegFile / numAcc) * integratedIndex.size();

        // (6) read first record of first file to derive mass axis and numChannels
        //     this avoids the allMasses.length == 0 check inside the loop
        JsonObject firstIdx = integratedIndex.get(0);
        Path firstFile = directory.resolve(firstIdx.get("FileNum").getAsInt() + ".integ");
        int firstExpectedCyc = firstIdx.get("FirstCycNum").getAsInt();
        int firstExpectedSeg = firstIdx.get("FirstSegNum").getAsInt();
        int firstExpectedAcq = firstIdx.get("FirstAcqNum").getAsInt();


        double[] allMasses = null;
        int numChannels = 0;

        LOGGER.trace("Peeking/reading first .integ file to find all m/z...");

        try {
          byte[] firstBytes = Files.readAllBytes(firstFile);
          ByteBuffer firstBuf = ByteBuffer.wrap(firstBytes).order(ByteOrder.LITTLE_ENDIAN);

          if (firstBuf.remaining() >= 16) {
            int cyc = firstBuf.getInt();
            int seg = firstBuf.getInt();
            int acq = firstBuf.getInt();
            numChannels = firstBuf.getInt();

            if (cyc != firstExpectedCyc || seg != firstExpectedSeg || acq != firstExpectedAcq) {
              LOGGER.error("First integ file header mismatch: expected FirstCycNum/FirstSegNum/FirstAcqNum "
                  + firstExpectedCyc + "/" + firstExpectedSeg + "/" + firstExpectedAcq
                  + " but got " + cyc + "/" + seg + "/" + acq);
            } else {
              double[] centres = new double[numChannels];
              for (int colIdx = 0; colIdx < numChannels; colIdx++) {
                centres[colIdx] = firstBuf.getFloat();
                firstBuf.getFloat(); // signal - skip
                firstBuf.getFloat(); // unused
                firstBuf.get();      // unused
                // alternatively: skip in one go
                // firstBuf.position(firstBuf.position() + 9); // skip 2 floats and 1 byte
              }
              allMasses = convertCentresToMassesWithMassCal(seg, centres, runInfo.massCalCoefficients,
                  segDelays);
            }
          }
        } catch (Exception e) {
          LOGGER.error("Cannot read first integ file: " + firstFile
              + ". Message: " + e.getMessage()
              + ". Stack trace: " + Arrays.toString(e.getStackTrace()));
        }

        if (allMasses != null && numChannels > 0) {

          // (7) preallocate row-major signal matrix
          double[][] signals = new double[numChannels][estimatedPointsPerMz];
          int rowCount = 0;

          // (8) single pass over all .integ files
          // Each channel entry is 4 (centre) + 4 (signal) + 4 (unused float) + 1 (unused byte) = 13 bytes
          int recordHeaderBytes = 16; // cyc + seg + acq + nc
          int channelStride = 13;

          for (int i = 0; i < integratedIndex.size(); i++) {
            JsonObject idx = integratedIndex.get(i);
            Path filePath = directory.resolve(idx.get("FileNum").getAsInt() + ".integ");

            int expectedCyc = idx.get("FirstCycNum").getAsInt();
            int expectedSeg = idx.get("FirstSegNum").getAsInt();
            int expectedAcq = idx.get("FirstAcqNum").getAsInt();

            if (!Files.exists(filePath)) {
              LOGGER.warn("Nu Vitesse data import: file not found: " + filePath);
              continue;
            }

            if (i % 20 == 0) {
              LOGGER.trace("Loaded .integ file " + i + " (" + filePath + ")");
              // updates the progress inside the calling task if passed
              if (progressTicker != null && progressValue != null && isStopped != null) {
                double sz = integratedIndex.size();
                progressValue.set(0.7 * i / sz);
                progressTicker.proceed();
                if (isStopped.get()) {
                  break mainBracket;
                }
              }
            }

            byte[] bytes;
            try {
              bytes = Files.readAllBytes(filePath);
            } catch (Exception e) {
              LOGGER.error("Cannot read bytes from file: " + filePath
                  + ". Message: " + e.getMessage()
                  + ". Stack trace: " + Arrays.toString(e.getStackTrace()));
              continue;
            }

            ByteBuffer buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);

            // validate first record header of this file against index
            if (buf.remaining() >= 16) {
              buf.mark();
              int cyc = buf.getInt();
              int seg = buf.getInt();
              int acq = buf.getInt();
              buf.getInt(); // numChannels - skip
              if (cyc != expectedCyc || seg != expectedSeg || acq != expectedAcq) {
                LOGGER.error("Integ file header mismatch in " + filePath
                    + ": expected FirstCycNum/FirstSegNum/FirstAcqNum "
                    + expectedCyc + "/" + expectedSeg + "/" + expectedAcq
                    + " but got " + cyc + "/" + seg + "/" + acq);
                continue;
              }
              buf.reset();
            }

            while (buf.remaining() >= 16) {
              buf.getInt(); // cycNum   - not needed, time constructed on demand
              buf.getInt(); // segNum   - not needed
              buf.getInt(); // acqNum   - not needed
              int nc = buf.getInt(); // numChannels

              if (buf.remaining() < nc * 13) {
                break; // truncated record
              }

              if (rowCount >= estimatedPointsPerMz) {
                LOGGER.warn("rowCount exceeded estimatedPointsPerMz - this should not happen and indicates " +
                    "a problem with code or file format.");
                break;
              }

//              for (int p = 0; p < nc; p++) {
//                buf.getFloat();             // centre - discard
//                float signal = buf.getFloat(); // signal - keep
//                buf.getFloat();             // unused
//                buf.get();                  // unused
//                signals[p][rowCount] = signal * invSia;  // [channel][time] - division is 5x slower than ·
//              }

              // faster parsing by just extracting the bytes that actually contain signal, skipping the rest
              int recordStart = buf.position();
              for (int p = 0; p < nc; p++) {
                float signal = buf.getFloat(recordStart + p * channelStride + 4); // absolute get
                signals[p][rowCount] = signal * invSia;
              }
              buf.position(recordStart + nc * channelStride);
              rowCount++;  // increment AFTER writing the row
            }
          }

          // (9) crop matrix to actual number of rows written
          if (rowCount < estimatedPointsPerMz) {
            for (int c = 0; c < numChannels; c++) {
              signals[c] = Arrays.copyOfRange(signals[c], 0, rowCount);
            }
          }

          // (10) Having extracted the masses, we can now assign the raw-timing autoblanks to masses
          // --- Compute blanked ranges from the cached autob events ---
          // computeBlankedRangesByColumn is pure computation over the event list;
          // no file I/O needed.

          // use first segment to find acquisition count
          int acquisitionCount = runInfo.segmentInfoList.get(0).acquisitionCount;
          Map<Integer, List<int[]>> blankedRangePerMass = computeBlankedRangesByColumn(
              autobEvents, allMasses, numAcc, acquisitionCount,
              runInfo.blMassCalStartCoef, runInfo.blMassCalEndCoef);

          // Bundle the data
          data = new ParsedNuData(allMasses, signals, dwellTime, blankedRangePerMass);


          LOGGER.debug("loadRunFromDisk: loaded " + numChannels + " masses x " + rowCount + " time points " +
              "from " + directory);

        } else {
          LOGGER.error("Could not derive mass axis from first integ file.");
        }

      } else {
        LOGGER.error("Cannot parse data since run.info's segment list and/or integrated.index were empty " +
            "and/or SIA from run.info was zero and/or dwell time parsed from run.info was zero.");
      }

    } else {
      LOGGER.error("Cannot read NU Vitesse data because path is not a directory on drive or files are " +
          "missing.");
    }

    return data;
  }

  public static List<Double> listIsotopesFromDisk(Path directory) {
    List<Double> mz = new ArrayList<>();

    // ensure path is dir and run.info, ..., are present
    boolean isValidDirectory = isValidDirectory(directory);

    if (isValidDirectory) {

      // (1) read the run.info file
      NuRunInfo_new runInfo = parseRunInfo(directory.resolve("run.info"));

      // (2) read the indexing files (e.g., JSON file that contains reference of all .integ files)
      List<JsonObject> integratedIndex = parseJsonIndex(directory.resolve("integrated.index"));

      // (4) for each segment, gather delays (I believe, one delay value per segment)
      double[] segDelays = extractSegmentDelays(runInfo);

      if (!runInfo.segmentInfoList.isEmpty() && !integratedIndex.isEmpty()) {

        // (6) read first record of first file to derive mass axis and numChannels
        //     this avoids the allMasses.length == 0 check inside the loop
        JsonObject firstIdx = integratedIndex.get(0);
        Path firstFile = directory.resolve(firstIdx.get("FileNum").getAsInt() + ".integ");
        int firstExpectedCyc = firstIdx.get("FirstCycNum").getAsInt();
        int firstExpectedSeg = firstIdx.get("FirstSegNum").getAsInt();
        int firstExpectedAcq = firstIdx.get("FirstAcqNum").getAsInt();

        try {
          byte[] firstBytes = Files.readAllBytes(firstFile);
          ByteBuffer firstBuf = ByteBuffer.wrap(firstBytes).order(ByteOrder.LITTLE_ENDIAN);

          if (firstBuf.remaining() >= 16) {
            int cyc = firstBuf.getInt();
            int seg = firstBuf.getInt();
            int acq = firstBuf.getInt();
            int numChannels = firstBuf.getInt();

            if (cyc != firstExpectedCyc || seg != firstExpectedSeg || acq != firstExpectedAcq) {
              LOGGER.error("First integ file header mismatch: expected FirstCycNum/FirstSegNum/FirstAcqNum "
                  + firstExpectedCyc + "/" + firstExpectedSeg + "/" + firstExpectedAcq
                  + " but got " + cyc + "/" + seg + "/" + acq);
            } else {
              double[] centres = new double[numChannels];
              for (int colIdx = 0; colIdx < numChannels; colIdx++) {
                centres[colIdx] = firstBuf.getFloat();
                firstBuf.getFloat(); // signal - skip
                firstBuf.getFloat(); // unused
                firstBuf.get();      // unused
              }
              double[] allMasses = convertCentresToMassesWithMassCal(
                  seg,
                  centres,
                  runInfo.massCalCoefficients,
                  segDelays);
              mz.addAll(ArrUtils.arrToList(allMasses));
            }
          }
        } catch (Exception e) {
          LOGGER.error("Cannot read first integ file: " + firstFile
              + ". Message: " + e.getMessage()
              + ". Stack trace: " + Arrays.toString(e.getStackTrace()));
        }

      } else {
        LOGGER.error("Cannot parse isotopes since run.info's segment list and/or integrated.index were " +
            "empty.");
      }

    } else {
      LOGGER.error("Cannot read NU Vitesse isotopes because path is not a directory on drive or files are " +
          "missing.");
    }

    return mz;
  }

}
