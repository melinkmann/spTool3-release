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

package analysis;

import core.SpTool3Main;
import dataModelNew.IncompleteParticleMatrix;
import dataModelNew.IncompleteSample;
import dataModelNew.Sample;
import dataModelNew.TISeries;
import dataModelNew.TISeriesRAM;
import dataModelNew.Trace;
import dataModelNew.TraceMC;
import dataModelNew.mz.*;

import java.util.*;
import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.PopulationType;
import processing.options.SearchAlgorithm;
import sandbox.montecarlo.Isotope;
import sandbox.montecarlo.ParticlePopulationMatrix;
import sandbox.montecarlo.ParticlePopulationMatrixRAM;
import util.ArrUtils;
import util.NF;
import util.SnF;
import visualizer.styles.Colors;
import visualizer.styles.Colors.SpColor;

public abstract class AnalysisUtils {

  private static final Logger LOGGER = LogManager.getLogger(AnalysisUtils.class);

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////// LABELS //////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  public static String getShortCodeNameForPlots(Sample sample, List<Sample> samples,
                                                @Nullable Channel channel, PopulationID popID,
                                                List<PopulationID> popIDs) {

    String shortHand = "S" + SnF.intToString(samples.indexOf(sample) + 1, NF.D1C0) +
        "\nP" + SnF.intToString(popIDs.indexOf(popID) + 1, NF.D1C0);

    // computed usually has no MZ
    if (channel instanceof ComputedChannel) {
      shortHand = shortHand + "\n" + channel.getShortUIString();
    } else if (channel != null) {
      shortHand = shortHand + "\n" + channel.getMZStr();
    }

    return shortHand;
  }


  /**
   * Returns a name that contains nick name - as well as isotope and population description of
   * available.
   */
  public static String getLabelForPlots(Sample sample, @Nullable Channel channel,
                                        @Nullable PopulationID populationID, @Nullable EventType eventType) {
    String name = "";

    name += getLabelString(eventType, sample);
    name += getLabelString(channel, sample);
    name += getLabelString(sample);
    name += getLabelString(populationID);

    return name;
  }

  /////////////////////////////////////
  // Sub methods in order of appearance
  /////////////////////////////////////

  /**
   * Extracts events type, no leading space, adds "data" keyword if needed.
   */
  private static String getLabelString(@Nullable EventType eventType, Sample sample) {
    String part = "";
    if (eventType != null) {
      part = eventType.getShortString();
      if (sample instanceof IncompleteSample) {
        part += " data";
      }
    }
    return part;
  }

  /**
   * Extracts mz value or isotope label adding space.
   */
  private static String getLabelString(@Nullable Channel channel, Sample sample) {
    String part = "";
    if (sample instanceof IncompleteSample) {
      part = " " + ((IncompleteSample) sample).getChannel();
    } else if (channel != null) {
      Isotope isotope = channel.getIsotope();
      if (isotope != null) {
        part = " " + isotope.getFullUIName();
      } else {
        part = " " + channel.getUIString();
      }
    }
    return part;
  }

  /**
   * Extracts sample name for ayn type of sample adding "from".
   */
  private static String getLabelString(Sample sample) {
    String part = "";
    if (sample != null) {
      part = " from " + sample.getNickName();
    }
    return part;
  }


  /**
   * Extracts population ID, adding "@".
   */
  private static String getLabelString(@Nullable PopulationID populationID) {
    String part = "";
    if (populationID != null) {
      part = " @ " + populationID.toString();
    }
    return part;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////// COLORS //////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param sample       the main sample. in case of merged sample, we indicate the merged samples
   *                     color.
   * @param sampleCount  number of selected samples.
   * @param isotopeCount number of selected isotopes.
   * @return the color for the given plot.
   */
  public static Colors getColor(Sample sample, Channel channel, int sampleCount, int isotopeCount) {
    // Start with sample color as the base
    Colors color = new SpColor(sample.getColor());

    // Just one sample and isotope -> we should stick to the sample's color
    if (sampleCount == 1 && isotopeCount == 1) {
      color = new SpColor(sample.getColor());
    } else if (sampleCount == 1 && isotopeCount > 1) {
      color = SpTool3Main.getRunTime().getConfParams().getColor(sample, channel);
    } else if (sampleCount > 1 && isotopeCount == 1) {
      // Just one trace --> we should assert that we use the sample color to compare samples
      color = new SpColor(sample.getColor());
    } else if (sampleCount > 1 && isotopeCount > 1) {
      // multiple traces and samples -> merge
      Colors traceColor = SpTool3Main.getRunTime().getConfParams().getColor(sample, channel);
      Colors sampleColor = new SpColor(sample.getColor());
      color = Colors.averageColorLAB(sampleColor, traceColor, 0.8);
    }

    // One could adjust color for population -- likely sth like a marker variation is better to see.
    // color = Colors.variation(new SpColor(sample.getColor()), color);
    return color;
  }

//  /**
//   * More basic version of the color getter that simply returns the "sample" color or the Trace
//   * color.
//   */
//  public static Colors getColor(Trace trace) {
//    Colors color;
//    if (trace.getMzValue().hasIsotope()) {
//      Isotope isotope = trace.getMzValue().getIsotope();
//      color = SpTool3Main.getRunTime().getConfParams().getColor(isotope);
//    } else {
//      color = new SpColor(trace.getSample().getColor());
//    }
//    return color;
//  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// ////////////////////////////////// Small helper methods //////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  public static List<Isotope> getIsotopes(List<Channel> channels) {
    return channels.stream()
        .map(Channel::getIsotope)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
  }

  // This list will have multiple entries for each Element, e.g., Ag -> 109 107, Sum
  public static List<CalChannel> getCalChannel(List<Channel> channels) {
    List<CalChannel> ec = new ArrayList<>();
    for (Channel channel : channels) {
      // Easiest: we get a category that is element
      ChannelCategory cat = channel.getCategory();
      if (cat instanceof Element) {
        ec.add(new CalChannel((Element) cat, channel));
      } else {
        // check if at least there is an isotope that we can parse
        Isotope isotope = channel.getIsotope();
        if (isotope != null) {
          ec.add(new CalChannel(isotope.getElement(), channel));
        }
      }
    }
    return ec;
  }

  @Nullable
  public static CalChannel getCalChannel(Channel channel) {
    // Easiest: we get a category that is element
    ChannelCategory cat = channel.getCategory();
    if (cat instanceof Element) {
      return new CalChannel((Element) cat, channel);
    } else {
      // check if at least there is an isotope that we can parse
      Isotope isotope = channel.getIsotope();
      if (isotope != null) {
        return new CalChannel(isotope.getElement(), channel);
      } else {
        return null;
      }
    }
  }


  public static boolean containsIsotope(List<Channel> channels, Isotope isotope) {
    return channels.stream()
        .map(Channel::getIsotope)
        .filter(Objects::nonNull)
        .anyMatch(i -> i.equals(isotope));
  }

  public static boolean notContainsIsotope(List<Channel> channels, Isotope isotope) {
    return channels.stream()
        .map(Channel::getIsotope)
        .filter(Objects::nonNull)
        .noneMatch(i -> i.equals(isotope));
  }

  public static HashMap<Isotope, Channel> getMatchMap(List<Channel> channels) {
    HashMap<Isotope, Channel> matchMap = new LinkedHashMap<>();
    for (Channel channel : channels) {
      Isotope isotope = channel.getIsotope();
      if (isotope != null) {
        matchMap.put(isotope, channel);
      }
    }
    return matchMap;
  }

  @Nullable
  public static Channel getChannel(List<Channel> channels, Isotope isotope) {
    Channel match = null;
    for (Channel channel : channels) {
      if (Objects.equals(channel.getIsotope(), isotope)) {
        match = channel;
        break;
      }
    }
    return match;
  }

  public static List<Channel> createChannels(List<Isotope> isotopes) {
    return isotopes.stream()
        .map(IsotopeChannel::new)
        .map(iCh -> ((Channel) iCh))
        .toList();
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////////// Applying to List of Samples //////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////
  /**
   * @return a complete and unique list of all Populations (by ID) that are present in the samples
   * in the Traces corresponding to the isotopes.
   */
  public static List<PopulationID> listPopulations(List<Sample> samples, List<Channel> channels) {
    List<PopulationID> pops = new ArrayList<>();
    for (Sample sample : samples) {
      List<PopulationID> subSamplePops = sample.listPopulations(channels);
      for (PopulationID pop : subSamplePops) {
        if (!pops.contains(pop)) {
          pops.add(pop);
        }
      }
    }
    return pops;
  }

  /**
   * Returns a list of Isotopes from the Samples, including extracting grouped samples, that are
   * unique, i.e., even if two traces contain the same Isotope, we get a unique list where no
   * duplicates exist.
   */

  public static List<Channel> listIsotopes(List<Sample> samples) {
    List<Channel> channelsUnique = new ArrayList<>();
    for (Sample sample : samples) {
      List<Channel> channels = sample.listChannels();
      for (Channel channel : channels) {
        if (!channelsUnique.contains(channel)) {
          channelsUnique.add(channel);
        }
      }
    }
    return channelsUnique;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// ////////////////////////////// Check if Aligned /////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * Check if we are dealing with aligned population by align or pValue
   */
  public static boolean isAlignedOrPValOrSim(PopulationID id) {
    return isAligned(id) || isPVal(id) || isSim(id);
  }

  public static boolean isAnyAlignedOrPValOrSim(List<PopulationID> ids) {
    return ids.stream().anyMatch(id -> isAligned(id) || isPVal(id) || isSim(id));
  }

  public static boolean isAllAlignedOrPValOrSim(List<PopulationID> ids) {
    return ids.stream().allMatch(id -> isAligned(id) || isPVal(id) || isSim(id));
  }

  public static boolean isSim(PopulationID id){
    return id.getType().equals(PopulationType.SIMULATION);
  }

  public static boolean isAligned(PopulationID id) {

    boolean isAligned = id.getSteps().stream()
        .anyMatch(popStep -> popStep instanceof PopulationStep.AlignSubtype);

    return isAligned;
  }

  public static boolean isPVal(PopulationID id) {
    boolean isPValSearch = false;
    for (PopulationStep popStep : id.getSteps()) {
      if (popStep instanceof PopulationStep.SearchSubtype) {
        if (((PopulationStep.SearchSubtype) popStep).getSearchAlgorithm()
            .equals(SearchAlgorithm.P_VALUE_ACCUMULATION)) {
          isPValSearch = true;
          // one is enough
          break;
        }
      }
    }

    return isPValSearch;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ///////////////////////////////// Data getters /////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////


  /**
   * Special case in the export: only get the data for ONE of the nested simulated populations.
   */
  public static HashMap<EventParameter, double[]> getFromSimulation(Sample sample, Isotope isotope,
                                                                    ParticlePopulationMatrix populationMatrix) {

    HashMap<EventParameter, double[]> dataMap = new HashMap<>();

    // We cannot just convert isotope to IsotopeChannel since maybe the identity of the channel has changed
    // from isotope channel to MZChannel during some conversion of trace.
    // TODO: check if keeping class type consistently would be better in general!!
    Channel channel = AnalysisUtils.getChannel(sample.listChannels(), isotope);
    if (channel != null) {

      // Check that there is no mismatch between Trace and Matrix.
      List<ParticlePopulationMatrix> allMatrices = sample.getMatrices(channel);
      if (allMatrices.contains(populationMatrix)) {

        // Get the respective trace and check if OK
        Trace trace = sample.getTrace(channel);
        if (trace instanceof TraceMC) {
          TraceMC traceMC = ((TraceMC) trace);

          // Get a RAM instance from the drive
          ParticlePopulationMatrixRAM ramMatrix = populationMatrix.getNewRamInstance();

          double[] area = new double[ramMatrix.getNumberOfEvents()];
          double[] netArea = new double[ramMatrix.getNumberOfEvents()];
          double[] height = new double[ramMatrix.getNumberOfEvents()];
          double[] netHeight = new double[ramMatrix.getNumberOfEvents()];
          double[] duration = new double[ramMatrix.getNumberOfEvents()];
          double[] noOfPoints = new double[ramMatrix.getNumberOfEvents()];
          double[] bgPerNP = new double[ramMatrix.getNumberOfEvents()];
          double[] startIndices = new double[ramMatrix.getNumberOfEvents()];
          double[] endIndices = new double[ramMatrix.getNumberOfEvents()];
          double[] centerIndices = new double[ramMatrix.getNumberOfEvents()];
          double[] asymmetry = new double[ramMatrix.getNumberOfEvents()];

          for (int i = 0; i < ramMatrix.getNumberOfEvents(); i++) {
            double[] integResult = ramMatrix.integrateForHeightWidthAndPoints(
                i,
                // if rounding issues in TISeries, make sure they match (dont use getMacroDT())
                traceMC.getTISeries().getDT(),
                // we have to use the original time series in case time roi was cut
                traceMC.getOriginalTISeries().getLastTimeStamp(),
                traceMC.getOriginalTISeries().size(),
                traceMC);

            area[i] = integResult[0];
            netArea[i] = integResult[1];
            height[i] = integResult[2];
            netHeight[i] = integResult[3];
            duration[i] = integResult[4];
            noOfPoints[i] = integResult[5];
            bgPerNP[i] = integResult[6];
            startIndices[i] = integResult[7];
            endIndices[i] = integResult[8];
            centerIndices[i] = integResult[9];
            asymmetry[i] = integResult[10];
          }

          // Here we have to make sure that we do not use the cut TISeries as the simulation is relative to
          // full time series
          double[] time = traceMC.getOriginalTISeries().getTime();

          dataMap.put(EventParameter.AREA, area);
          dataMap.put(EventParameter.NET_AREA, netArea);
          dataMap.put(EventParameter.HEIGHT, height);
          dataMap.put(EventParameter.NET_HEIGHT, netHeight);
          dataMap.put(EventParameter.DURATION, duration);
          dataMap.put(EventParameter.NO_OF_POINTS, noOfPoints);
          dataMap.put(EventParameter.NO_OF_EVENTS, ArrUtils.fillArray(1, area.length));
          dataMap.put(EventParameter.BACKGROUND_PER_NP, bgPerNP);
          dataMap.put(EventParameter.START_INDEX, startIndices);
          dataMap.put(EventParameter.END_INDEX, endIndices);
          dataMap.put(EventParameter.CENTER_TIME,
              Arrays.stream(centerIndices).map(i -> time[(int) i]).toArray());
          dataMap.put(EventParameter.ASYMMETRY_FACTOR, asymmetry);
        }
      }
    }
    return dataMap;
  }


  /**
   * Calculates event parameters based on the simulated peaks.
   */

  public static HashMap<EventParameter, double[]> getFromSimulation(TraceMC traceMC) {

    HashMap<EventParameter, double[]> dataMap = new HashMap<>();

    HashMap<EventParameter, List<double[]>> subDataMap = new HashMap<>();

    List<ParticlePopulationMatrix> particleMatrices = traceMC.getSample().getMatrices(traceMC);

    // iterate over the matrices (each matrix is one population)
    for (ParticlePopulationMatrix populationMatrix : particleMatrices) {
      // Get a RAM instance from the drive
      ParticlePopulationMatrixRAM ramMatrix = populationMatrix.getNewRamInstance();

      double[] area = new double[ramMatrix.getNumberOfEvents()];
      double[] netArea = new double[ramMatrix.getNumberOfEvents()];
      double[] height = new double[ramMatrix.getNumberOfEvents()];
      double[] netHeight = new double[ramMatrix.getNumberOfEvents()];
      double[] duration = new double[ramMatrix.getNumberOfEvents()];
      double[] noOfPoints = new double[ramMatrix.getNumberOfEvents()];
      double[] bgPerNP = new double[ramMatrix.getNumberOfEvents()];
      double[] startIndices = new double[ramMatrix.getNumberOfEvents()];
      double[] endIndices = new double[ramMatrix.getNumberOfEvents()];
      double[] centerIndices = new double[ramMatrix.getNumberOfEvents()];
      double[] asymmetry = new double[ramMatrix.getNumberOfEvents()];

      for (int i = 0; i < ramMatrix.getNumberOfEvents(); i++) {
        double[] integResult = ramMatrix.integrateForHeightWidthAndPoints(
            i,
            // if rounding issues in TISeries, make sure they match (dont use getMacroDT())
            traceMC.getTISeries().getDT(),
            // we have to use the original time series in case time roi was cut
            traceMC.getOriginalTISeries().getLastTimeStamp(),
            traceMC.getOriginalTISeries().size(),
            traceMC);

        area[i] = integResult[0];
        netArea[i] = integResult[1];
        height[i] = integResult[2];
        netHeight[i] = integResult[3];
        duration[i] = integResult[4];
        noOfPoints[i] = integResult[5];
        bgPerNP[i] = integResult[6];
        startIndices[i] = integResult[7];
        endIndices[i] = integResult[8];
        centerIndices[i] = integResult[9];
        asymmetry[i] = integResult[10];
      }

      // Here we have to make sure that we do not use the cut TISeries as the simulation is relative to
      // full time series
      double[] time = traceMC.getOriginalTISeries().getTime();

      subDataMap.computeIfAbsent(EventParameter.AREA, k -> new ArrayList<>()).add(area);
      subDataMap.computeIfAbsent(EventParameter.NET_AREA, k -> new ArrayList<>()).add(netArea);
      subDataMap.computeIfAbsent(EventParameter.HEIGHT, k -> new ArrayList<>()).add(height);
      subDataMap.computeIfAbsent(EventParameter.NET_HEIGHT,
          k -> new ArrayList<>()).add(netHeight);
      subDataMap.computeIfAbsent(EventParameter.DURATION, k -> new ArrayList<>()).add(duration);
      subDataMap.computeIfAbsent(EventParameter.NO_OF_POINTS,
          k -> new ArrayList<>()).add(noOfPoints);
      subDataMap.computeIfAbsent(EventParameter.NO_OF_EVENTS,
          k -> new ArrayList<>()).add(ArrUtils.fillArray(1, area.length));
      subDataMap.computeIfAbsent(EventParameter.BACKGROUND_PER_NP,
          k -> new ArrayList<>()).add(bgPerNP);
      subDataMap.computeIfAbsent(EventParameter.START_INDEX,
          k -> new ArrayList<>()).add(startIndices);
      subDataMap.computeIfAbsent(EventParameter.END_INDEX,
          k -> new ArrayList<>()).add(endIndices);
      subDataMap.computeIfAbsent(EventParameter.CENTER_TIME,
          k -> new ArrayList<>()).add(Arrays.stream(centerIndices)
          .map(i -> time[(int) i])
          .toArray());
      subDataMap.computeIfAbsent(EventParameter.ASYMMETRY_FACTOR,
          k -> new ArrayList<>()).add(asymmetry);
    }

    for (EventParameter param : subDataMap.keySet()) {
      double[] mergedData = ArrUtils.merge(subDataMap.get(param));
      dataMap.put(param, mergedData);
    }

    /// Now, make sure we get events across all Pops in chronological order (else Pop1->Pop2->...)
    double[] timeArr = dataMap.get(EventParameter.CENTER_TIME);
    if (timeArr != null) {
      List<double[]> allArr = new ArrayList<>(dataMap.values());
      HashMap<double[], double[]> refMap = ArrUtils.sort(timeArr, allArr);
      for (EventParameter eventParameter : dataMap.keySet()) {
        double[] unsorted = dataMap.get(eventParameter);
        if (unsorted != null) {
          double[] sorted = refMap.get(unsorted);
          if (sorted != null) {
            dataMap.put(eventParameter, sorted);
          }
        }
      }
    }

    return dataMap;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static double[] getFromIncomplete(IncompleteParticleMatrix matrix, EventParameter param) {
     /*
     Ideally, a single zero would probably be more accurate. data = new double[0];
     If nothing is present and only zeros are returned, one may be tempted
     to believe that there actually are zeros.
     However, to be safe in iterations, may be smart to return something of the same size.
     */
    double[] data = new double[matrix.size()];
    switch (param) {
      case NET_AREA -> data = ArrUtils.doubleListToArr(matrix.getAreas());
      case NET_HEIGHT -> data = ArrUtils.doubleListToArr(matrix.getHeights());
      case DURATION -> data = ArrUtils.doubleListToArr(matrix.getDurations());
      case NO_OF_POINTS -> data = ArrUtils.doubleListToArr(matrix.getPoints());
      default -> LOGGER.trace("Event collection does not provide this parameter type.");
    }
    return data;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static double[] getNPsFromEvaluation(EventCollection events, EventParameter param) {

    double[] result;

    double[] time = events.getCheckedTISeries().getTime();
    double[] intensity = events.getCheckedTISeries().getIntensity();
    double dt = events.getCheckedTISeries().getDT();

    List<Event> npEvents = events.getNpEvents();

    result = new double[events.size()];

    for (int i = 0; i < npEvents.size(); i++) {
      result[i] = getNPFromEvaluation(npEvents.get(i), param, time, intensity, dt);
    }

    return result;
  }

  /**
   * For just ONE event.
   */
  public static double getNPFromEvaluation(Event event, EventParameter param, double[] time,
                                           double[] intensity, double dwellTime) {
    int[] indices = event.getIndices();

    double val = 0;

    switch (param) {
      case AREA -> {
        double sum = 0;
        for (int i = 0; i < indices.length; i++) {
          sum += intensity[indices[i]];
        }
        val = sum;
      }

      case NET_AREA -> {
        double sum = 0;
        for (int i = 0; i < indices.length; i++) {
          sum += intensity[indices[i]];
        }
        val = sum - event.getBgPerNP();
      }

      case HEIGHT -> {
        double max = 0;
        for (int i = 0; i < indices.length; i++) {
          max = Math.max(intensity[indices[i]], max);
        }
        val = max;
      }

      case NET_HEIGHT -> {
        double max = 0;
        for (int i = 0; i < indices.length; i++) {
          max = Math.max(intensity[indices[i]], max);
        }
        // Calc BG per DT
        val = max - event.getBgPerNP() / indices.length;
      }

      case DURATION -> {
        int startIdx = indices[0];
        int stopIdx = indices[indices.length - 1];

        // if event includes the first reading
        if (startIdx == 0) {
          stopIdx++;
          // rare edge case where all data is the event...
          if (stopIdx > time.length - 1) {
            val = 1E6 * dwellTime;
            break;
          }
        } else {
          // if standard event or last event
          startIdx--;
        }
        double start = time[startIdx];
        double endTime = time[stopIdx];
        double diff = endTime - start;
        val = 1E6 * diff;
      }
      case BACKGROUND_PER_NP -> val = event.getBgPerNP();
      case NO_OF_POINTS -> val = indices.length;
      case ASYMMETRY_FACTOR -> val = computeAsymmetryFactor(indices.length,
          indices[0], indices[indices.length - 1], event.getPeak());
      case NO_OF_EVENTS -> val = 1;
      case CENTER_TIME -> val = time[event.getCenter()];
    }
    return val;
  }

  /**
   * Typically, this is defined as a/b with a: distance from start to apex,
   * abd b: distance from apex to end.
   * However, this may lead to division by zero when b=0, which is likely
   * for events with few points. Hence, the best idea is to divide 2*b by a+b.
   * Why? When a=b, 2b/(a+b) = 1. When b gets more pronounced, then we get larger
   * asymmetry.
   */
  public static double computeAsymmetryFactor(int length, double startInclusive, double endInclusive,
                                              double apex) {
    double asymmetry = 1;
    if (length > 2) {
      double a = apex - startInclusive;
      double b = endInclusive - apex;
      asymmetry = 2d * b / (a + b);
    }
    return asymmetry;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * For ALL background events, i.e., the entire collection's indices.
   */
  public static double[] getBGsFromEvaluation(List<Integer> bgIndices, EventParameter param,
                                              double[] time, double[] intensity) {
    double[] values = new double[bgIndices.size()];

    for (int v = 0; v < bgIndices.size(); v++) {
      int idx = bgIndices.get(v);
      switch (param) {
        case AREA, NET_AREA, HEIGHT, NET_HEIGHT -> values[v] = intensity[idx];

        case DURATION -> {
          if (idx == 0) {
            values[v] = time[idx];
          } else {
            values[v] = 1E6 * (time[idx] - time[idx - 1]);
          }
        }
        case BACKGROUND_PER_NP -> values[v] = 0;
        case NO_OF_POINTS, ASYMMETRY_FACTOR, NO_OF_EVENTS -> values[v] = 1;
        case CENTER_TIME -> values[v] = time[idx];
      }
    }

    return values;
  }

  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////
  /// /////////////////////////////////////////////////////////////////////////////////////////////

  public static List<PlottableEventMarkers> getEventMarkers(List<Trace> selTraces,
                                                            List<PopulationID> selPops) {
    List<PlottableEventMarkers> eventMarkers = new ArrayList<>();

    for (PopulationID popID : selPops) {
      // do not add populations doubly if they share isotopes (mostly for aligned / simulated)
      for (Trace trace : selTraces) {
        if (trace.hasType(popID) && trace.getPopulation(popID) != null) {
          Population pop = trace.getPopulation(popID);
          PlottableEventMarkers eventMarker = pop.getPeakMarkers();
          eventMarkers.add(eventMarker);
        }
      }
    }
    return eventMarkers;
  }

  public static List<PlottableSubPopulation> getPopulationMarkers(List<Trace> selTraces,
                                                                  List<PopulationID> selPops) {
    List<PlottableSubPopulation> resultPopulations = new ArrayList<>();

    for (PopulationID popID : selPops) {

      // do not add populations doubly if they share isotopes (mostly for aligned / simulated)

      traceLoop:
      for (Trace trace : selTraces) {
        if (trace.hasType(popID) && trace.getPopulation(popID) != null) {
          Population pop = trace.getPopulation(popID);
          List<PlottableSubPopulation> popMarkers = pop.getPopulationMarkers();
          for (PlottableSubPopulation popMarker : popMarkers) {
            // only add one marker for the Aligned/pValue-aligned populations
            if (popID.getSteps().stream().anyMatch(step -> step instanceof PopulationStep.AlignSubtype)) {
              // add once, then break
              resultPopulations.add(popMarker);
              break traceLoop;
            }
            boolean isPValSearch = false;
            for (PopulationStep step : popID.getSteps()) {
              if (step instanceof PopulationStep.SearchSubtype) {
                isPValSearch = ((PopulationStep.SearchSubtype) step).getSearchAlgorithm()
                    .equals(SearchAlgorithm.P_VALUE_ACCUMULATION);
                if (isPValSearch) break;
              }
            }
            if (isPValSearch) {
              // add once, then break
              resultPopulations.add(popMarker);
              break traceLoop;
            }

            if (resultPopulations.stream().noneMatch(p -> p.isSameMatrix(popMarker))) {
              resultPopulations.add(popMarker);
            }
          }
        }
      }
    }
    return resultPopulations;
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * XYData as a Time Series to plot a threshold.
   */
  public static TISeries getThresholdXY(ThresholdSupplier thr, TISeries tiSeries,
                                        double[] globalTime) {
    int[] anchorIndices = thr.getAnchorIndices(tiSeries.size());
    double[] time = new double[anchorIndices.length];
    double[] values = new double[anchorIndices.length];
    for (int i = 0; i < anchorIndices.length; i++) {
      time[i] = globalTime[anchorIndices[i]];
      values[i] = thr.interpolateProtected(anchorIndices[i], tiSeries.size());
    }
    return new TISeriesRAM(time, values);
  }

  ////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////
  ////////////////////////////////////////////////////////////////////////////////////////////////


}
