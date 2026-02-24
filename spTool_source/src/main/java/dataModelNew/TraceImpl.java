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

package dataModelNew;

import analysis.AnalysisUtils;
import analysis.Baseline;
import analysis.Population;
import analysis.PopulationID;
import analysis.RawProcessingUtils;
import com.google.common.math.DoubleMath;
import dataModelNew.mz.MZValue;
import dataModelNew.mz.SQmz;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.lang.ref.SoftReference;
import java.util.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import processing.options.DataFlag;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.PopulationType;
import util.ArrUtils;

import javax.annotation.Nullable;


public class TraceImpl implements Trace, Serializable {

  static final long serialVersionUID = 1L; //assign a long value

  private static final Logger LOGGER = LogManager.getLogger(TraceImpl.class.getName());

  private final Sample parentSample;
  protected final MZValue mzValue;
  protected TISeries tiSeries;
  protected TISeries tiSeriesCopy;
  protected HashMap<DataFlag, List<Integer>> rawDataFlags;

  protected Baseline baseline;
  protected final HashMap<PopulationID, Population> populations;
  //
  private transient SoftReference<XYSeries> xySeriesPlotCache;


  // Dummy
  public TraceImpl() {
    this.parentSample = new SampleImpl();
    this.mzValue = new SQmz();
    this.tiSeries = new TISeriesRAM();
    this.tiSeriesCopy = tiSeries;
    this.rawDataFlags = new HashMap<>();
    this.baseline = new Baseline();
    this.populations = new LinkedHashMap<>();
    this.xySeriesPlotCache = new SoftReference<>(null);
  }

  public TraceImpl(Sample sample, MZValue mzValue, TISeries tiSeries) {
    this.parentSample = sample;
    this.mzValue = mzValue;
    this.tiSeries = tiSeries;
    this.tiSeriesCopy = tiSeries;
    this.rawDataFlags = new HashMap<>();
    this.baseline = new Baseline();
    this.populations = new LinkedHashMap<>();
    xySeriesPlotCache = new SoftReference<>(null);
  }

  // Copy
  public TraceImpl(Sample parentSample, MZValue mzValue, TISeries tiSeries,
                   TISeries tiSeriesCopy, HashMap<DataFlag, List<Integer>> rawDataFlags,
                   Baseline baseline,
                   HashMap<PopulationID, Population> populations) {
    this.parentSample = parentSample;
    this.mzValue = mzValue.copy();
    this.tiSeries = tiSeries.copy();
    this.tiSeriesCopy = tiSeriesCopy.copy();
    this.rawDataFlags = new HashMap<>();
    for (DataFlag flag : rawDataFlags.keySet()) {
      List<Integer> values = rawDataFlags.get(flag);
      this.rawDataFlags.put(flag, new ArrayList<>(values));
    }
    this.baseline = baseline.copy();
    this.populations = new LinkedHashMap<>();
    for (PopulationID id : populations.keySet()) {
      Population oldPop = populations.get(id);
      this.populations.put(id, oldPop.copy(this));
    }
    this.xySeriesPlotCache = new SoftReference<>(null);
  }

  @Override
  public Trace copy(Sample newSample) {
    Trace newTrace = new TraceImpl(newSample,
        mzValue, tiSeries, tiSeriesCopy, rawDataFlags, baseline, populations);
    return newTrace;
  }

  /// //////////////////////////////////////////////////////////////////////////////////////


  @Serial
  private void writeObject(ObjectOutputStream out) throws IOException {
    // Load data into RAM
    toRAM();

    // Default serialization
    out.defaultWriteObject();

    // Offload data back to HDD
    toHDD();
  }

  @Serial
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    // Default deserialization
    in.defaultReadObject();

    // backwards compatibility
    if (rawDataFlags == null) {
      this.rawDataFlags = new HashMap<>();
    }

    // Offload to HDD immediately after reading
    toHDD();
  }

  public void toRAM() {
    if (tiSeries instanceof TISeriesHDD) {
      if (tiSeriesCopy == tiSeries) {
        this.tiSeries = new TISeriesRAM(tiSeries);
        this.tiSeriesCopy = tiSeries;
      } else {
        this.tiSeries = new TISeriesRAM(tiSeries);
      }
    }

    if (tiSeriesCopy instanceof TISeriesHDD) {
      this.tiSeriesCopy = new TISeriesRAM(tiSeriesCopy);
    }

    this.baseline.toRAM();
    this.xySeriesPlotCache = new SoftReference<>(null);
  }

  public void toHDD() {
    if (tiSeries instanceof TISeriesRAM) {
      TISeries hddSeries = new TISeriesHDD(tiSeries);
      if (tiSeriesCopy == tiSeries) {
        this.tiSeries = hddSeries;
        this.tiSeriesCopy = hddSeries;
      } else {
        this.tiSeries = hddSeries;
      }
    }

    if (tiSeriesCopy instanceof TISeriesRAM) {
      this.tiSeriesCopy = new TISeriesHDD(tiSeriesCopy);
    }

    this.baseline.toHDD();
    this.xySeriesPlotCache = new SoftReference<>(null);
  }

  /// //////////////////////////////////////////////////////////////////////////////////////

  @Override
  public Sample getSample() {
    return parentSample;
  }

  @Override
  public MZValue getMzValue() {
    return mzValue;
  }

  @Override
  public TISeries getTISeries() {
    return tiSeries;
  }

  @Override
  public TISeries getOriginalTISeries() {
    return tiSeriesCopy;
  }

  @Override
  public HashSet<Integer> getFlags(DataFlag flag) {
    HashSet<Integer> values = new HashSet<>();
    if (rawDataFlags.containsKey(flag)) {
      values.addAll(rawDataFlags.get(flag));
    }
    return values;
  }

  @Override
  public void setFlags(DataFlag flag, List<Integer> indices) {
    rawDataFlags.put(flag, indices);
  }

  @Override
  public void setBaseline(Baseline baseline) {
    this.baseline = baseline;
  }

  @Override
  public Baseline getBaseline() {
    return baseline;
  }

  @Override
  public boolean hasType(PopulationID id) {
    return populations.containsKey(id);
  }

  @Override
  public List<PopulationID> getAllPopulationsTypes() {
    return new ArrayList<>(populations.keySet());
  }

  @Override
  public List<Population> getAllPopulations() {
    return new ArrayList<>(populations.values());
  }

  @Override
  @Nullable
  public Population getPopulation(PopulationID id) {
    // Returns dummy if not present
    return populations.getOrDefault(id, null);
  }

  @Override
  public int getNoOfEvents(PopulationID id) {
    int no = 0;
    if (hasType(id) && getPopulation(id) != null) {
      no = getPopulation(id).getEvents().size();
    }
    return no;
  }

  @Override
  public double[] get(PopulationID id, EventType eventType, EventParameter parameter) {
    double[] result = new double[0];
    if (eventType.equals(EventType.RAW)) {
      // return copy as else, modifications may trickle back
      result = ArrUtils.copy(tiSeries.getIntensity());
    } else {
      if (hasType(id) && getPopulation(id) != null) {
        Population pop = getPopulation(id);
        result = pop.getEvents().get(eventType, parameter);
      }
    }
    return result;
  }

  @Override
  public void addOverridePopulation(PopulationID id, Population population) {
//    System.out.println("\nnew :"+id.toString());
//    for (PopulationID populationID : populations.keySet()) {
//      System.out.println("old :"+populationID.toString());
//      if (Objects.equals(id.toString(), populationID.toString())) {
//        System.out.println("Hash old: " + populationID.hashCode());
//        System.out.println("Hash new: " + id.hashCode()+"\n");
//        System.out.println("equal: " + (id.equals(populationID)));
//      }
//    }
    this.populations.put(id, population);
  }

  @Override
  public void removePopulation(PopulationID id) {
    if (id.getType() == PopulationType.SIMULATION
        || id.getType() == PopulationType.EXTERNAL) {
      // Do nothing
    } else {
      this.populations.remove(id);
    }
  }

  @Override
  public void clearEvaluation() {
    this.baseline = new Baseline();
    this.populations.clear();
    this.xySeriesPlotCache = new SoftReference<>(null);
  }

  @Override
  public XYSeries getXYSeries() {
    XYSeries series;
    if (xySeriesPlotCache != null && xySeriesPlotCache.get() != null) {
      series = xySeriesPlotCache.get();
      //LOGGER.trace("Received plot from cache");
    } else {
      String seriesName = AnalysisUtils.getLabelForPlots(parentSample, this.mzValue, null, null);
      double[] xData = tiSeries.getTime();
      double[] yData = tiSeries.getIntensity();

      final XYSeries xySeries = new XYSeries(seriesName, true, true);
      if (xData.length == yData.length) {
        for (int i = 0; i < xData.length; i++) {
          xySeries.add(xData[i], yData[i]);
        }
      }
      series = xySeries;
      this.xySeriesPlotCache = new SoftReference<>(series);
      //LOGGER.trace("Built plot from data");;
    }
    return series;
  }


  @Override
  public void clearXYSeriesCache() {
    xySeriesPlotCache.clear();
  }

  public void setTISeriesLimits(double lower, double upper) {
    /*
    IMPORTANT: initially, tiSeries and tiSeriesCopy are THE SAME OBJECT.
    The copy is always the "full & initial" series.
    When cutting time series here, we must not call: tiSeriesCopy = tiSeries.copy().
    When setting time limit twice in succession, the copy is replaced with the cut instance stored in
    tiSeries!
    So, what should we do?
    1) When tiSeries is the same object as tiSeriesCopy, they are ref to the same object.
    In that case, we must create a copy or else the time cutting would affect the shared object.
    2) Otherwise, the copy is not the same ref and not copy needs to be created.
       This could be the case, a. when initial state or b. after resetting.
    3) In any case, always call tiSeriesCopy = copy of the copy.
     */

    if (tiSeries == tiSeriesCopy) {
      this.tiSeriesCopy = tiSeriesCopy.copy();
    }
    this.tiSeries = RawProcessingUtils.cutTime(tiSeries, lower, upper);
    // when we do this, full reset has to occur!
    clearEvaluation();
  }

  public void resetTISeriesLimits() {
    this.tiSeries = tiSeriesCopy;
    // when we do this, full reset has to occur!
    clearEvaluation();
  }

  @Override
  public boolean hasLimits() {
    return !DoubleMath.fuzzyEquals(tiSeries.getDuration(), tiSeriesCopy.getDuration(), 1E-6);
  }


}
