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

package io.export;

import analysis.*;
import core.SpTool3Main;
import dataModelNew.Sample;
import dataModelNew.SampleImpl;
import dataModelNew.TISeries;
import dataModelNew.Trace;
import dataModelNew.TraceMC;
import dataModelNew.mz.Element;
import dataModelNew.mz.MZValue;
import gui.table.TableUtils;
import io.fastExport.TabBlock;
import io.fastExport.TabBlockColl;
import io.fastExport.TabCol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

import math.units.enums.ExportUnits;
import math.units.enums.IntensityUnit;
import math.units.enums.TimeUnit;
import math.units.enums.ViewUnits;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.CombinedPStatistics;
import processing.options.EventParameter;
import processing.options.EventType;
import processing.options.PopulationType;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.ConfParams;
import processing.parameterSets.impl.NormalSearchParams;
import processing.parameters.Parameter;
import sandbox.montecarlo.*;
import tasks.single.SearchTask;
import util.*;
import visualizer.charts.AxisLabel;

import static sandbox.montecarlo.Statistics.MIN_P_VALUE;

public abstract class DataExport {

  public static final Logger LOGGER = LogManager.getLogger(DataExport.class.getName());

  public static TabularBlock extractMethodSettings(@Nullable Sample sample, Method method) {
    ConfParams conf = SpTool3Main.getRunTime().getConfParams();

    TabularBlock hBlock = new TabularBlockHorizontal();
    List<String> labels = new ArrayList<>();
    List<String> values = new ArrayList<>();

    if (sample != null) {
      labels.add("Sample");
      values.add(sample.getSampleFile().getNameWithinFile());
      labels.add("Sample nick name");
      values.add(sample.getNickName());

      labels.add("Sample comment");
      values.add(sample.getComment());
    }

    labels.add("Method");
    values.add(method.getLabelParam().getValue());
    labels.add("Method comment");
    values.add(method.getCommentParam().getValue());
    labels.add("Method created date");
    values.add(method.getDateParam().getValue());

    for (ParamSet paramSet : method.getSets()) {

      labels.add(" ");
      values.add(" ");

      labels.add("Sub-method");
      values.add(paramSet.getLabelParameter().getValue());
      labels.add("Sub-method comment");
      values.add(paramSet.getCommentParameter().getValue());
      labels.add("Sub-method created date");
      values.add(paramSet.getDateParameter().getValue());

      for (Parameter<?> par : paramSet.listActiveParameters()) {
        labels.add(par.getLabel());
        values.add(par.getValueAsString());
      }
    }

    double default_v_mu = conf.getDefault_v_mu().getValue();
    double default_v_SD = conf.getDefault_v_SD().getValue();
    double default_y_mu = conf.getDefault_y_mu().getValue();
    double default_y_SD = conf.getDefault_y_SD().getValue();
    double default_D_mu = conf.getDefault_D_mu().getValue();
    double default_D_SD = conf.getDefault_D_SD().getValue();

    labels.add("Default diffusion coefficient (D) [cm2/s]");
    values.add(SnF.doubleToString(default_D_mu, NF.D1C3));
    labels.add("Default diffusion coefficient SD (D_SD) [cm2/s]");
    values.add(SnF.doubleToString(default_D_SD, NF.D1C3));

    labels.add("Default distance of vaporization (y) [mm]");
    values.add(SnF.doubleToString(default_y_mu, NF.D1C3));
    labels.add("Default distance of vaporization SD (y_SD) [mm]");
    values.add(SnF.doubleToString(default_y_SD, NF.D1C3));

    labels.add("Default plasma velocity (v) [m/s]");
    values.add(SnF.doubleToString(default_v_mu, NF.D1C3));
    labels.add("Default plasma velocity SD (v_SD) [m/s]");
    values.add(SnF.doubleToString(default_v_SD, NF.D1C3));

    hBlock.addColumn("Parameter", labels);
    hBlock.addColumn("Values", values);

    return hBlock;
  }

  public static List<TabularBlock> extractPopulationData(Sample sample, Isotope isotope,
                                                         PopulationID popID) {
    List<TabularBlock> blockList = new ArrayList<>();
    TabularBlock hBlock = new TabularBlockHorizontal();

    List<Event> events = sample.getNPEvents(isotope, popID);
    String nNP = SnF.intToString(events.size(), NF.D1C0);

    hBlock.addColumn("Evaluated population export.", "Name", popID.toString());
    hBlock.addColumn("Number of particles:", nNP);

    List<String> peakTime = new ArrayList<>();
    for (Event event : events) {
      double[] time = event.getCollection().getCheckedTISeries().getTime();
      peakTime.add(SnF.doubleToString(time[event.getPeak()], NF.D1C9));
    }

    List<String> grossArea = new ArrayList<>();
    List<String> netArea = new ArrayList<>();
    List<String> grossHeight = new ArrayList<>();
    List<String> netHeight = new ArrayList<>();
    List<String> duration = new ArrayList<>();
    List<String> noOfPoints = new ArrayList<>();
    List<String> bgPerNP = new ArrayList<>();

    grossArea.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.AREA), NF.D1C6));
    netArea.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.NET_AREA), NF.D1C6));
    grossHeight.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.HEIGHT), NF.D1C6));
    netHeight.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.NET_HEIGHT), NF.D1C6));
    duration.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.DURATION), NF.D1C6));
    noOfPoints.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.NO_OF_POINTS), NF.D1C6));
    bgPerNP.addAll(SnF.doubleToStrList(sample.getData(isotope, popID, EventType.NP,
        EventParameter.BACKGROUND_PER_NP), NF.D1C6));

    // Keep for when we switch back to using MZ instead of isotopes.
    //    List<String> elementLabel;
    //    MZValue mz = trace.getMzValue();
    //    if (mz.hasIsotope()) {
    //      elementLabel = Arrays.asList(mz.getIsotope().getElement().getSymbol(),
    //          mz.getIsotope().getName());
    //    } else {
    //      elementLabel = Collections.singletonList(mz.getElementTransition());
    //    }
    String elementLabel = isotope.getName();

    hBlock.addColumn(elementLabel,
        "Peak time [s]", peakTime);
    hBlock.addColumn(elementLabel,
        "Gross area ["+ IntensityUnit.CTS.getLiteralString()+"]", grossArea);
    hBlock.addColumn(elementLabel,
        "Net area ["+ IntensityUnit.CTS.getLiteralString()+"]", netArea);
    hBlock.addColumn(elementLabel,
        "Gross height ["+ IntensityUnit.CTS.getLiteralString()+"]", grossHeight);
    hBlock.addColumn(elementLabel,
        "Net height ["+ IntensityUnit.CTS.getLiteralString()+"]", netHeight);
    hBlock.addColumn(elementLabel,
        "Width ["+ TimeUnit.MICROSECOND.getLiteralString()+"]", duration);
    hBlock.addColumn(elementLabel,
        "Number of points ["+ ViewUnits.NONE.getLiteralString()+"]", noOfPoints);
    hBlock.addColumn(elementLabel,
        "BG per NP ["+ IntensityUnit.CTS.getLiteralString()+"]", bgPerNP);

    blockList.add(hBlock);
    return blockList;
  }

  public static List<TabularBlock> extractCustomPopulationData(Sample sample,
                                                               List<Isotope> isotopes,
                                                               List<PopulationID> popIDs,
                                                               boolean exportBG,
                                                               boolean applyJitter,
                                                               int noOfBgDP,
                                                               boolean exportNP,
                                                               EventParameter npPar,
                                                               ExportUnits exportUnits) {
    List<TabularBlock> blockList = new ArrayList<>();
    TabularBlock hBlock = new TabularBlockHorizontal();

    List<String> overheadDescriptors = new ArrayList<>();
    overheadDescriptors.add(sample.getNickName());
    overheadDescriptors.add(sample.getSampleFile().getNameWithinFile());
    overheadDescriptors.add(sample.getSampleFile().getFileName());

    for (Isotope isotope : isotopes) {
      List<String> isotopeOverheadDescriptors = new ArrayList<>(overheadDescriptors);
      isotopeOverheadDescriptors.add(isotope.getName());
      for (PopulationID popID : popIDs) {
        List<String> populationOverheadDescriptors = new ArrayList<>(isotopeOverheadDescriptors);
        populationOverheadDescriptors.add(popID.toString());
        if (exportBG) {
          double[] bg = sample.getData(isotope, popID, EventType.BG, EventParameter.AREA);
          if (applyJitter) {
            bg = Statistics.quantileSampleWithMurmurHashedJitter(bg, noOfBgDP, 2);
            AxisLabel bgLabel = AxisLabel.getUnit(EventParameter.AREA);
            String label = "BG @ " + bgLabel.getLabel() + " [" + bgLabel.getUnit().getLiteralString() + "]";
            hBlock.addColumn(populationOverheadDescriptors, label, SnF.doubleToStrList(bg, NF.D1C6));
          }
        }
        if (exportNP) {
          double[] np = sample.getData(isotope, popID, EventType.NP, npPar,exportUnits.getUnit());
          AxisLabel npLabel = AxisLabel.getUnit(npPar);
          String unitStr = npLabel.getUnit().getLiteralString();
          if (!exportUnits.equals(ExportUnits.CTS)){
            unitStr = exportUnits.getLiteralString();
          }
          String label = "NMPs @ " + npLabel.getLabel() + " [" + unitStr + "]";
          hBlock.addColumn(populationOverheadDescriptors, label, SnF.doubleToStrList(np, NF.D1C6));
        }

      }
    }

    blockList.add(hBlock);
    return blockList;
  }

  /**
   * Exports a synthetic population with isotopes in numeric order.
   */
  public static List<TabularBlock> extractSyntheticPopulationData(Sample sample,
                                                                  PopulationID popID) {

    List<TabularBlock> blockList = new ArrayList<>();
    TabularBlock hBlock = new TabularBlockHorizontal();

    List<ExportSimulationEventContainer> containers = sample.getSimExport();

    // Normal sample: each container is one syn. input population
    // Merged sample: attribution is not possible, hence, mixing all syn. input population
    for (ExportSimulationEventContainer cont : containers) {

      String nNP = SnF.intToString(cont.getEventCount(), NF.D1C0);

      hBlock.addColumn("Synthetic population export.", "Name", cont.getLabel());
      hBlock.addColumn("Number of particles:", nNP);

      List<Element> elements = cont.listElements();
      elements.sort(Comparator.comparingInt(Element::getAtomicNumber));

      hBlock.addColumn("All elements", "Linear plasma velocity [m/s]",
          SnF.doubleToStrList(cont.getVelocity(), NF.D1C6));

      hBlock.addColumn("All elements", "Sampling distance [mm]",
          SnF.doubleToStrList(ArrUtils.multiply(cont.getYPos(), 1e3), NF.D1C6));

      // ELEMENTS
      for (Element element : elements) {

        // Add peak time
        hBlock.addColumn(element.getSymbol(), "Peak time [s]",
            SnF.doubleToStrList(cont.getPeakTime().get(element), NF.D1C6));

        // Add icl & fwhm of the peak function
        hBlock.addColumn(element.getSymbol(), "Peak function FWHM [µs]",
            SnF.doubleToStrList(ArrUtils.multiply(cont.getFwhm().get(element), 1e6), NF.D1C6));

        hBlock.addColumn(element.getSymbol(), "Peak function ICL (integrated & h=5%) [µs]",
            SnF.doubleToStrList(ArrUtils.multiply(cont.getIcl().get(element), 1e6), NF.D1C6));

        hBlock.addColumn(element.getSymbol(), "Diffusion coefficient [cm2/s]",
            SnF.doubleToStrList(ArrUtils.multiply(cont.getDiffCoeff().get(element), 1e4),
                NF.D1C6));

        // ISOTOPES
        List<Isotope> isotopes = element.getIsotopes();
        for (Isotope isotope : isotopes) {

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Initial net signal ["+ IntensityUnit.CTS.getLiteralString()+"]",
              SnF.doubleToStrArr(cont.getRandNetSignal().get(isotope), NF.D1C6));

          if (cont.getDataMap().containsKey(isotope)) {
            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Gross area (integrated at >1cts & calculated) ["+ IntensityUnit.CTS.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.AREA),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Net area (integrated at >1cts) ["+ IntensityUnit.CTS.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.NET_AREA),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Gross height (integrated at >1cts & calculated) ["+ IntensityUnit.CTS.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.HEIGHT),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Net height (integrated at >1cts) ["+ IntensityUnit.CTS.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.NET_HEIGHT),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Width (integrated at >1cts) ["+ TimeUnit.MICROSECOND.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.DURATION),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "Number of points (integrated at >1cts) ["+ ViewUnits.NONE.getLiteralString()+"]",
                SnF.doubleToStrArr(cont.getDataMap().get(isotope).get(EventParameter.NO_OF_POINTS),
                    NF.D1C6));

            hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
                "BG per NP (summed at >1cts) ["+ IntensityUnit.CTS.getLiteralString()+"]",
                SnF.doubleToStrArr(
                    cont.getDataMap().get(isotope).get(EventParameter.BACKGROUND_PER_NP),
                    NF.D1C6));
          }

          // TODO: Decide if we want to show these as well? These give the respective value across all
          //  populations.
        /*
                 hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Gross area (integrated at >1cts & calculated) [cts]",
              SnF.doubleToStrArr(sample.getData(isotope, popID, EventType.NP, EventParameter.AREA),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Net area (integrated at >1cts) [cts]",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.NET_AREA),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Gross height (integrated at >1cts & calculated) [cts]",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.HEIGHT),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Net height (integrated at >1cts [cts])",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.NET_HEIGHT),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Width (integrated at >1cts) [µs]",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.DURATION),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "Number of points (integrated at >1cts) [-]",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.NO_OF_POINTS),
                  NF.D1C6));

          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
              "BG per NP (summed at >1cts) [cts]",
              SnF.doubleToStrArr(
                  sample.getData(isotope, popID, EventType.NP, EventParameter.BACKGROUND_PER_NP),
                  NF.D1C6));
           */
        }

      }
    }
    blockList.add(hBlock);
    return blockList;
  }

//  public static List<TabularBlock> extractCollectionPopulationData(PopulationID popID,
//      List<Trace> traces) {
//    List<TabularBlock> blockList = new ArrayList<>();
//    TabularBlock hBlock = new TabularBlockHorizontal();
//
//    for (Trace trace : traces) {
//      if (trace.hasType(popID)) {
//        Population population = trace.getPopulation(popID);
//        EventCollection mainEventCollection = population.getEvents();
//
//        String nNP = SnF.intToString(population.getEvents().size(), NF.D1C0);
//
//        hBlock.addColumn("Evaluated population export.", "Name", population.getName());
//        hBlock.addColumn("Number of particles:", nNP);
//
//        double[] time = trace.getTISeries().getTime();
//
//        List<String> peakTime = new ArrayList<>();
//        for (Event event : mainEventCollection.getNpEvents()) {
//          peakTime.add(SnF.doubleToString(time[event.getPeak()], NF.D1C9));
//        }
//
//        List<String> grossArea = new ArrayList<>();
//        List<String> netArea = new ArrayList<>();
//        List<String> grossHeight = new ArrayList<>();
//        List<String> netHeight = new ArrayList<>();
//        List<String> duration = new ArrayList<>();
//        List<String> noOfPoints = new ArrayList<>();
//        List<String> bgPerNP = new ArrayList<>();
//
//        grossArea.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.AREA), NF.D1C6));
//        netArea.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.NET_AREA), NF.D1C6));
//        grossHeight.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.HEIGHT), NF.D1C6));
//        netHeight.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.NET_HEIGHT), NF.D1C6));
//        duration.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.DURATION), NF.D1C6));
//        noOfPoints.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.NO_OF_POINTS), NF.D1C6));
//        bgPerNP.addAll(SnF.doubleToStrList(population.getEvents().get(EventType.NP,
//            EventParameter.BACKGROUND_PER_NP), NF.D1C6));
//
//        List<String> elementLabel;
//        MZValue mz = trace.getMzValue();
//        if (mz.hasIsotope()) {
//          elementLabel = Arrays.asList(mz.getIsotope().getElement().getSymbol(),
//              mz.getIsotope().getName());
//        } else {
//          elementLabel = Collections.singletonList(mz.getElementTransition());
//        }
//        hBlock.addColumn(elementLabel,
//            "Peak time [s]", peakTime);
//        hBlock.addColumn(elementLabel,
//            "Gross area [cts]", grossArea);
//        hBlock.addColumn(elementLabel,
//            "Net area [cts]", netArea);
//        hBlock.addColumn(elementLabel,
//            "Gross height [cts]", grossHeight);
//        hBlock.addColumn(elementLabel,
//            "Net height [cts]", netHeight);
//        hBlock.addColumn(elementLabel,
//            "Width [µs]", duration);
//        hBlock.addColumn(elementLabel,
//            "Number of points [-]", noOfPoints);
//        hBlock.addColumn(elementLabel,
//            "BG per NP [cts]", bgPerNP);
//      }
//    }
//    blockList.add(hBlock);
//    return blockList;
//  }
//
//
//  public static List<TabularBlock> extractMatrixPopulationData(
//      List<ParticlePopulationMatrix> populations,
//      HashMap<Isotope, TraceMC> traceIsotopeMap,
//      double macroDTSeconds,
//      double lastTimeStamp,
//      int tiSeriesDP) {
//
//    List<TabularBlock> blockList = new ArrayList<>();
//
//    for (ParticlePopulationMatrix hddPop : populations) {
//      TabularBlock hBlock = new TabularBlockHorizontal();
//
//      ParticlePopulationMatrixRAM p = hddPop.getNewRamInstance();
//
//      String nNP = SnF.intToString(p.getNumberOfEvents(), NF.D1C0);
//
//      hBlock.addColumn("Simulated population export.", "Name", p.getLabel());
//      hBlock.addColumn("Number of particles:", nNP);
//
//      List<String> velocities = SnF.doubleToStrList(p.getPlasmaVelocities(), NF.D1C6);
//      List<String> yPos = SnF.doubleToStrList(ArrUtils.multiply(p.getYPositions(), 1e3), NF.D1C6);
//
//      List<Element> elements = p.listElements();
//
//      for (Element element : elements) {
//        List<String> timePoints = SnF.doubleToStrList(p.getArrivalTimeMap().get(element),
//            NF.D1C6);
//
//        hBlock.addColumn(element.getSymbol(), "Time [s]", timePoints);
//
//        List<String> diffusion = SnF.doubleToStrList(
//            ArrUtils.multiply(p.getPlasmaDiffusionDMap().get(element), 1e4), NF.D1C6);
//
//        List<String> icls = new ArrayList<>();
//        List<String> fwhms = new ArrayList<>();
//        double[] v = p.getPlasmaVelocities();
//        double[] yPosArr = p.getYPositions();
//        double[] d = p.getPlasmaDiffusionDMap().get(element);
//        if (v.length == yPosArr.length && v.length == d.length) {
//          for (int i = 0; i < v.length; i++) {
//            PeakFunction pf = new PeakFunction(1, d[i], yPosArr[i], v[i]);
//            icls.add(SnF.doubleToString(1E6 * pf.getIcl(), NF.D1C6));
//            fwhms.add(SnF.doubleToString(1E6 * pf.getFwhm(), NF.D1C6));
//          }
//        }
//        hBlock.addColumn(element.getSymbol(), "Peak function FWHM [µs]", fwhms);
//        hBlock.addColumn(element.getSymbol(), "Peak function ICL (integrated & h=5%) [µs]", icls);
//
//        for (Isotope isotope : element.getIsotopes()) {
//          List<String> signal = SnF.doubleToStrList(p.getIntensityMap().get(isotope), NF.D1C6);
//
//          List<String> grossArea = new ArrayList<>();
//          List<String> netArea = new ArrayList<>();
//          List<String> grossHeight = new ArrayList<>();
//          List<String> netHeight = new ArrayList<>();
//          List<String> duration = new ArrayList<>();
//          List<String> noOfPoints = new ArrayList<>();
//          List<String> bgPerNP = new ArrayList<>();
//
//          // Mimic integration to check height (and duration at 1 cts)
//          // Iterate over the NP to get the data
//          for (int i = 0; i < v.length; i++) {
//            double[] hdp = p.integrateForHeightWidthAndPoints(
//                i,
//                macroDTSeconds,
//                lastTimeStamp,
//                tiSeriesDP,
//                traceIsotopeMap.get(isotope));
//
//            /*
//              0 grossArea,
//              1 netArea,
//              2 grossHeight,
//              3 netHeight,
//              4 duration,
//              5 noOfPoints,
//              6 bg * noOfPoints
//            */
//
//            grossArea.add(SnF.doubleToString(hdp[0], NF.D1C6));
//            netArea.add(SnF.doubleToString(hdp[1], NF.D1C6));
//            grossHeight.add(SnF.doubleToString(hdp[2], NF.D1C6));
//            netHeight.add(SnF.doubleToString(hdp[3], NF.D1C6));
//            duration.add(SnF.doubleToString(hdp[4], NF.D1C6));
//            noOfPoints.add(SnF.doubleToString(hdp[5], NF.D1C0));
//            bgPerNP.add(SnF.doubleToString(hdp[6], NF.D1C6));
//
//          }
//
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Initial net signal [cts/DT]", signal);
//
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Gross area (integrated at >1cts & calculated) [cts]", grossArea);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Net area (integrated at >1cts) [cts]", netArea);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Gross height (integrated at >1cts & calculated) [cts]", grossHeight);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Net height (integrated at >1cts [cts])", netHeight);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Width (integrated at >1cts) [µs]", duration);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "Number of points (integrated at >1cts) [-]", noOfPoints);
//          hBlock.addColumn(Arrays.asList(element.getSymbol(), isotope.getName()),
//              "BG per NP (summed at >1cts) [cts]", bgPerNP);
//
//        }
//
//        hBlock.addColumn(element.getSymbol(), "Diffusion coefficient [cm2/s]", diffusion);
//      }
//      hBlock.addColumn("All elements", "Linear plasma velocity [m/s]", velocities);
//      hBlock.addColumn("All elements", "Sampling distance [mm]", yPos);
//
//      blockList.add(hBlock);
//    }
//
//    return blockList;
//  }


  public static List<BlockCollection> extractExpectedValues(Sample sample) {

    List<BlockCollection> blockList = new ArrayList<>();

    BlockCollection sampleBlocks = new BlockCollectionHorizontal();
    BlockCollection generalPopBlocks = new BlockCollectionHorizontal();
    BlockCollection popBlocks = new BlockCollectionVertical();
    BlockCollection methodBlocks = new BlockCollectionHorizontal();

    blockList.add(sampleBlocks);
    blockList.add(generalPopBlocks);
    blockList.add(popBlocks);
    blockList.add(methodBlocks);

    //
    TabularBlock sampleBlock = new TabularBlockHorizontal();

    String sampleNickName = sample.getNickName();
    String sampleName = sample.getSampleFile().getNameWithinFile();
    String sampleComment = sample.getComment();

    sampleBlock.addColumn("Expected values of data generator.", "Sample nick name", sampleNickName);
    sampleBlock.addColumn("Sample name", sampleName);
    sampleBlock.addColumn("Sample comment", sampleComment);
    sampleBlock.addColumn("", sampleComment);

    sampleBlocks.addBlock(sampleBlock);

    List<TraceMC> traces = sample.getTraces().stream()
        .filter(t -> t instanceof TraceMC)
        .map(t -> (TraceMC) t)
        .collect(Collectors.toList());

    // Unique list of populations
    List<ParticlePopulationMatrix> uniquePopulations = new ArrayList<>(sample.getMatrices());
    uniquePopulations.sort(new Comparator<ParticlePopulationMatrix>() {
      @Override
      public int compare(ParticlePopulationMatrix o1, ParticlePopulationMatrix o2) {
        return o1.getLabel().compareTo(o2.getLabel());
      }
    });

    // Write down the population names and particle counts
    TabularBlock generalPopBlock = new TabularBlockHorizontal();

    List<String> popNames = new ArrayList<>();
    List<String> popNPCount = new ArrayList<>();
    for (ParticlePopulationMatrix hddPop : uniquePopulations) {
      popNames.add(hddPop.getLabel());
      popNPCount.add(SnF.intToString(hddPop.getNumberOfEvents()));
    }
    generalPopBlock.addColumn("Population label", popNames);
    generalPopBlock.addColumn("Number of events", popNPCount);

    generalPopBlocks.addBlock(generalPopBlock);

    // Get link between isotope and its trace
    HashMap<Isotope, TraceMC> traceMap = new LinkedHashMap<>();
    traces.forEach(t -> traceMap.put(t.getMzValue().getIsotope(), t));

    // Get a unique list of all isotopes
    HashSet<Isotope> isotopesSet = new HashSet<>();
    uniquePopulations.forEach(p -> isotopesSet.addAll(p.getIntensityMap().keySet()));

    List<Isotope> isotopes = new ArrayList<>(isotopesSet);
    isotopes.sort(new Comparator<Isotope>() {
      @Override
      public int compare(Isotope o1, Isotope o2) {
        return Double.compare(o1.getIsotopicNumber(), o2.getIsotopicNumber());
      }
    });

    // Now, top to bottom, iterate over the Populations
    for (ParticlePopulationMatrix hddPop : uniquePopulations) {
      TabularBlock popBlock = new TabularBlockVertical();

      ParticlePopulationMatrixRAM p = hddPop.getNewRamInstance();
      double[] velocities = p.getPlasmaVelocities();
      double[] yPos = p.getYPositions();

      List<Isotope> popIsotopes = new ArrayList<>(p.getIntensityMap().keySet());
      popIsotopes.sort(new Comparator<Isotope>() {
        @Override
        public int compare(Isotope o1, Isotope o2) {
          return Double.compare(o1.getIsotopicNumber(), o2.getIsotopicNumber());
        }
      });

      // For each population, prepare the containers
      List<String> isotopeNames = isotopes.stream()
          .map(Isotope::getName)
          .collect(Collectors.toList());

      List<String> points = new ArrayList<>();
      List<String> duration = new ArrayList<>();
      List<String> dwellTimeMicroSeconds = new ArrayList<>();
      List<String> muTrue = new ArrayList<>();
      List<String> muTrueObsv = new ArrayList<>();

      //
      List<String> muNP = new ArrayList<>();
      List<String> medianNP = new ArrayList<>();
      List<String> sdNP = new ArrayList<>();
      List<String> fwhmNP = new ArrayList<>();
      List<String> fwhmNPMedian = new ArrayList<>();
      List<String> fwhmNPMax = new ArrayList<>();
      List<String> fwhmNPMin = new ArrayList<>();
      List<String> fwhmNPSD = new ArrayList<>();
      List<String> iclNP = new ArrayList<>();
      List<String> iclNPMedian = new ArrayList<>();
      List<String> iclNPMax = new ArrayList<>();
      List<String> iclNPMin = new ArrayList<>();
      List<String> iclNPSD = new ArrayList<>();

      popBlock.addColumn(p.getLabel(), "Isotope", isotopeNames);
      popBlock.addColumn("Points [#]", points);
      popBlock.addColumn("Duration [s]", duration);
      popBlock.addColumn("Dwell time [µs]", dwellTimeMicroSeconds);
      popBlock.addColumn("Input BG mean [cts/DT]", muTrue);
      popBlock.addColumn("Empirical BG mean [cts/DT]", muTrueObsv);

      popBlock.addColumn("Mean NP signal [cts/DT]", muNP);
      popBlock.addColumn("Median NP signal [cts/DT]", medianNP);
      popBlock.addColumn("NP Signal StdDev [cts/DT]", sdNP);

      popBlock.addColumn("Mean FWHM [µs]", fwhmNP);
      popBlock.addColumn("Median FWHM [µs]", fwhmNPMedian);
      popBlock.addColumn("Max  FWHM [µs]", fwhmNPMax);
      popBlock.addColumn("Min FWHM [µs]", fwhmNPMin);
      popBlock.addColumn("StdDev FWHM [µs]", fwhmNPSD);

      popBlock.addColumn("Mean ICL (at 5%) [µs]", iclNP);
      popBlock.addColumn("Median ICL (at 5%) [µs]", iclNPMedian);
      popBlock.addColumn("Max  ICL (at 5%) [µs]", iclNPMax);
      popBlock.addColumn("Min ICL (at 5%) [µs]", iclNPMin);
      popBlock.addColumn("StdDev ICL (at 5%) [µs]", iclNPSD);

      // Iterate over each isotope, if it is missing, write a zero.
      for (Isotope isotope : isotopes) {
        if (popIsotopes.contains(isotope)) {
          TraceMC trace = traceMap.get(isotope);
          // add trace stuff
          points.add(SnF.intToString(trace.getTISeries().size()));
          duration.add(SnF.doubleToString(trace.getTISeries().getDuration(), NF.D1C6));
          dwellTimeMicroSeconds.add(
              SnF.doubleToString(1E6 * trace.getTISeries().getDT(), NF.D1C6));
          muTrue.add(SnF.doubleToString(trace.getExpectedMeanBG(), NF.D1C6));
          muTrueObsv.add(SnF.doubleToString(trace.getEmpiricalMeanBG(), NF.D1C6));
          //
          double[] diffusion = p.getPlasmaDiffusionDMap().get(isotope.getElement());
          double[] signal = p.getIntensityMap().get(isotope);

          String meanFWHM = TableUtils.EMPTY_STR;
          String medianFWHM = TableUtils.EMPTY_STR;
          String maxFWHM = TableUtils.EMPTY_STR;
          String minFWHM = TableUtils.EMPTY_STR;
          String sdFWHM = TableUtils.EMPTY_STR;
          String meanICL = TableUtils.EMPTY_STR;
          String medianICL = TableUtils.EMPTY_STR;
          String maxICL = TableUtils.EMPTY_STR;
          String minICL = TableUtils.EMPTY_STR;
          String sdICL = TableUtils.EMPTY_STR;

          if (diffusion.length > 0 &&
              diffusion.length == velocities.length && diffusion.length == yPos.length) {
            double[] fwhms = new double[diffusion.length];
            double[] icls = new double[diffusion.length];
            for (int i = 0; i < diffusion.length; i++) {
              PeakFunction pf = new PeakFunction(1, diffusion[i], yPos[i], velocities[i]);
              fwhms[i] = 1E6 * pf.getFwhm();
              icls[i] = 1E6 * pf.getIcl();
            }
            meanFWHM = SnF.doubleToString(StatUtils.mean(fwhms), NF.D1C6);
            meanICL = SnF.doubleToString(StatUtils.mean(icls), NF.D1C6);
            Percentile percentile = new Percentile();
            StandardDeviation sd = new StandardDeviation();

            medianFWHM = SnF.doubleToString(percentile.evaluate(fwhms, 50), NF.D1C6);
            maxFWHM = SnF.doubleToString(StatUtils.max(fwhms), NF.D1C6);
            minFWHM = SnF.doubleToString(StatUtils.min(fwhms), NF.D1C6);
            sdFWHM = SnF.doubleToString(sd.evaluate(fwhms), NF.D1C6);

            medianICL = SnF.doubleToString(percentile.evaluate(icls, 50), NF.D1C6);
            maxICL = SnF.doubleToString(StatUtils.max(icls), NF.D1C6);
            minICL = SnF.doubleToString(StatUtils.min(icls), NF.D1C6);
            sdICL = SnF.doubleToString(sd.evaluate(icls), NF.D1C6);
          }

          String meanNPStr = TableUtils.EMPTY_STR;
          String medianNPStr = TableUtils.EMPTY_STR;
          String sdNPStr = TableUtils.EMPTY_STR;
          if (signal.length > 0) {
            meanNPStr = SnF.doubleToString(StatUtils.mean(signal), NF.D1C6);
            Percentile percentile = new Percentile();
            medianNPStr = SnF.doubleToString(percentile.evaluate(signal, 50), NF.D1C6);
            StandardDeviation sd = new StandardDeviation();
            sdNPStr = SnF.doubleToString(sd.evaluate(signal, 50), NF.D1C6);
          }

          muNP.add(meanNPStr);
          medianNP.add(medianNPStr);
          sdNP.add(sdNPStr);

          fwhmNP.add(meanFWHM);
          fwhmNPMedian.add(medianFWHM);
          fwhmNPMax.add(maxFWHM);
          fwhmNPMin.add(minFWHM);
          fwhmNPSD.add(sdFWHM);

          iclNP.add(meanICL);
          iclNPMedian.add(medianICL);
          iclNPMax.add(maxICL);
          iclNPMin.add(minICL);
          iclNPSD.add(sdICL);
        } else {
          points.add(TableUtils.EMPTY_STR);
          duration.add(TableUtils.EMPTY_STR);
          dwellTimeMicroSeconds.add(TableUtils.EMPTY_STR);
          muTrue.add(TableUtils.EMPTY_STR);
          muTrueObsv.add(TableUtils.EMPTY_STR);
          //
          muNP.add(TableUtils.EMPTY_STR);
          medianNP.add(TableUtils.EMPTY_STR);
          sdNP.add(TableUtils.EMPTY_STR);
          fwhmNP.add(TableUtils.EMPTY_STR);
          fwhmNPMedian.add(TableUtils.EMPTY_STR);
          fwhmNPMax.add(TableUtils.EMPTY_STR);
          fwhmNPMin.add(TableUtils.EMPTY_STR);
          fwhmNPSD.add(TableUtils.EMPTY_STR);
          iclNP.add(TableUtils.EMPTY_STR);
          iclNPMedian.add(TableUtils.EMPTY_STR);
          iclNPMax.add(TableUtils.EMPTY_STR);
          iclNPMin.add(TableUtils.EMPTY_STR);
          iclNPSD.add(TableUtils.EMPTY_STR);
        }
      }

      popBlocks.addBlock(popBlock);
    }

    // Add method at the end
    TabularBlock methodBlock = extractMethodSettings(null, sample.getMethod());
    methodBlocks.addBlock(methodBlock);

    return blockList;
  }

  // TODO: When two peaks exactly align with peaks in the same DT, the marker should be 2x the height or
  //  sth like hat....
  //  At least in an export, it should appear twice!

  /**
   * Here, we only allow sampleImpls, i.e., not merges! MergedSamples are extracted previously.
   */

  public static List<TabBlock> extractRawData(SampleImpl sample, List<Isotope> selIsotopes,
                                              List<PopulationID> selPops,
                                              double height, boolean limitToSelIsotopes, boolean showPop,
                                              boolean spCalExport, boolean showPeaks) {

    List<TabBlock> blocks = new ArrayList<>();

    TabBlock xyBlock = new TabBlock();
    TabBlock eventMarkerBlock = new TabBlock();
    TabBlock populationMarkerBlock = new TabBlock();
    blocks.add(xyBlock);
    blocks.add(populationMarkerBlock);
    blocks.add(eventMarkerBlock);

    // Peak previews make mo sense for spCal export
    showPeaks = !spCalExport && showPeaks;

    // Raw data itself can be exported for synthetic and normal data :-)
    List<Trace> traces;
    if (limitToSelIsotopes) {
      traces = sample.getTraces(selIsotopes);
    } else {
      traces = sample.getTraces();
    }

    for (int i = 0; i < traces.size(); i++) {
      Trace trace = traces.get(i);
      String seriesLabel;
      if (trace.getMzValue().hasIsotope()) {
        seriesLabel = trace.getMzValue().getIsotope().getName();
      } else {
        seriesLabel = trace.getMzValue().getElementTransition();
      }
      TISeries tiSeries = trace.getTISeries();

      if (i == 0) {
        String timeLabel = "Time [s]";
        if (spCalExport) {
          timeLabel = "Time [Sec]";
        }
        xyBlock.addCol(new TabCol(timeLabel, SnF.doubleToStrArr(tiSeries.getTime(), NF.D1C6)));
      }
      xyBlock.addCol(new TabCol(seriesLabel, SnF.doubleToStrArr(tiSeries.getIntensity(),
          NF.D1C6)));
    }

    if (showPop || showPeaks) {

      // Extract Populations
      //      List<PopulationID> targetPops = traces.stream()
      //          .map(Trace::getAllPopulationsTypes)
      //          .flatMap(List::stream)
      //          .distinct()
      //          .collect(Collectors.toList());
      // Export according to UI selection
      List<PopulationID> targetPops = new ArrayList<>(selPops);

      // In this case, we only want to export the markers of populations that are simulated
      if (spCalExport) {
        targetPops.removeIf(Predicate.not(p -> p.getType().equals(PopulationType.SIMULATION)));
      }

      // If spCal export is executed, we do not export peak markers at all (SPCal has its own)
      List<PlottableEventMarkers> plottableEventMarkers = new ArrayList<>();
      if (!spCalExport) {
        plottableEventMarkers = AnalysisUtils.getEventMarkers(traces, targetPops);
      }
      List<PlottableSubPopulation> plottablePopulations =
          AnalysisUtils.getPopulationMarkers(traces, targetPops);

      // Next: check if we shall export POPULATION markers
      if (showPop && !plottablePopulations.isEmpty()) {
        for (int i = 0; i < plottablePopulations.size(); i++) {
          PlottableSubPopulation plotPop = plottablePopulations.get(i);
          String label = plotPop.getPopLabel();
          double[] timeMarkers = plotPop.getTimerMarkers();

          // We can only allow one time stamp column for spCal, hence put these into frame...
          if (spCalExport) {
            double dtSec = plotPop.getRawDwellTime();

            double[] populationMarkers = new double[plotPop.getRawDataSize()];

            for (double t : timeMarkers) {
              int idx = (int) Math.ceil(t / dtSec) - 1;
              idx = Math.min(idx, populationMarkers.length - 1); // This fixes a sporadic bug.
              populationMarkers[idx] = height;
            }

            xyBlock.addCol(new TabCol(label, SnF.doubleToStrArr(populationMarkers, NF.D1C6)));
          } else {
            populationMarkerBlock
                .addCol(new TabCol("Positions of " + label,
                    "Time", SnF.doubleToStrArr(timeMarkers, NF.D1C6)));
            populationMarkerBlock.addCol(new TabCol(plotPop.getMzID(),
                SnF.doubleToStrArr(ArrUtils.fillArray(-(2 + 2 * i), timeMarkers.length), NF.D1C6)));
          }
        }
      }

      // Next: check if we shall export PEAK markers
      if (showPeaks && !plottableEventMarkers.isEmpty()) {
        for (int i = 0; i < plottableEventMarkers.size(); i++) {
          PlottableEventMarkers peakTrace = plottableEventMarkers.get(i);

          eventMarkerBlock.addCol(new TabCol("Peak markers of " + peakTrace.getPopulationLabel(),
              "Time", SnF.doubleToStrArr(peakTrace.getEventMarkerData().getTime(),
              NF.D1C6)));

          eventMarkerBlock.addCol(new TabCol(peakTrace.getMzLabel(),
              SnF.doubleToStrArr(peakTrace.getEventMarkerData().getIntensity(), NF.D1C6)));
        }
      }


    }

    return blocks;
  }

  /**
   * Intended for the exporter if one wants to show the p-value approach.
   */

  public static void exportPValues(Sample sample, List<Isotope> selIsotopes,
                                   NormalSearchParams params, ExportWriter writer) {
    if (sample instanceof SampleImpl) {

      // prepare export
      TabBlockColl coll = new TabBlockColl(writer, true);
      List<TabBlock> blocks = new ArrayList<>();
      TabBlock metaBlock = new TabBlock();
      TabBlock pBlock = new TabBlock();
      TabBlock resultingPBlock = new TabBlock();
      blocks.add(metaBlock);
      blocks.add(resultingPBlock);
      blocks.add(pBlock);
      coll.write(DataExport.getShortMeta(sample));

      // unpack input parameters
      NormalSearchParams.ThresholdBundle pAccScreenStartBundle = params.getPrescreenStartStopCriterium();
      NormalSearchParams.ThresholdBundle pAccScreenStopBundle = params.getPrescreenStartStopCriterium();
      NormalSearchParams.ThresholdBundle pAccScreenHeightBundle = params.getPrescreenHeightCriterium();
      int pAccMinEvents = params.getPrescreenMinEvents().getValue();
      boolean considerSingleComponent = params.getEnableSingleComponentPValueThreshold().getValue();

      // Iterate
      List<Trace> traces = sample.getTraces();

      // ################################################################################
      // find interesting traces
      // ################################################################################
      LOGGER.trace("p-Value accumulation prescreening...");

      // storage map
      HashMap<Trace, EventCollection> preScreenMap = new HashMap<>();
      HashMap<Trace, ThresholdSupplierInstructions> screenStartThrMap = new HashMap<>();
      HashMap<Trace, ThresholdSupplierInstructions> preScreenStopThrMap = new HashMap<>();
      HashMap<Trace, ThresholdSupplierInstructions> preScreenHeightThrMap = new HashMap<>();

      // Export
      metaBlock.addCol(new TabCol("Search sub method used",
          new String[]{params.getLabelParameter().getValue()}));

      // for constructing a large enough p value array
      int rawDataMaxSize = 0;

      // check if we only want the selected isotopes?
      if (params.getOnlyUseSelectedIsotopesForPValue().getValue()) {
        traces.removeIf(t -> !selIsotopes.contains(t.getMzValue().getIsotope()));
      }

      for (Trace trace : traces) {

        Baseline bln = trace.getBaseline();

        // dummies
        ThresholdSupplierInstructions screenStartInstr = new ThresholdSupplierInstructions();
        ThresholdSupplierInstructions screenStopInstr = new ThresholdSupplierInstructions();
        ThresholdSupplierInstructions screenHeightInstr = new ThresholdSupplierInstructions();

        if (bln.hasBaseline()) {
          // decide thresholding for prescreening
          screenStartInstr = SearchTask.getSupplier("Prescreen start threshold", pAccScreenStartBundle, 0);
          screenStopInstr = SearchTask.getSupplier("Prescreen stop threshold", pAccScreenStopBundle, 0);
          screenHeightInstr = SearchTask.getSupplier("Prescreen search threshold", pAccScreenHeightBundle, 0);

          // This involves the computation of thresholds, which is heavy for compound Poisson
          ThresholdSupplier screenStart = screenStartInstr.get(bln.getBackgroundDistribution());
          ThresholdSupplier screenStop = screenStopInstr.get(bln.getBackgroundDistribution());
          ThresholdSupplier screenHeight = screenHeightInstr.get(bln.getBackgroundDistribution());

          // just perform a split correction search to find interesting traces
          EventCollection evtColl = SearchUtils.splitCorrectSearch(trace, trace.getTISeries(),
              screenStart, screenStop, screenHeight);

          // are there enough events to add? note: reference value is given "per minute long sample"
          if (evtColl.size() / trace.getTISeries().getDuration() > pAccMinEvents / 60d) {
            preScreenMap.put(trace, evtColl);
            // only update for added traces
            rawDataMaxSize = Math.max(rawDataMaxSize, trace.getTISeries().size());
          }

        }
        screenStartThrMap.put(trace, screenStartInstr);
        preScreenStopThrMap.put(trace, screenStopInstr);
        preScreenHeightThrMap.put(trace, screenHeightInstr);
      }

      // Export trace selection
      List<String> includedTraces = new ArrayList<>();
      List<String> excludedTraces = new ArrayList<>();
      for (Trace trace : traces) {
        if (preScreenMap.containsKey(trace)) {
          includedTraces.add(trace.getMzValue().getName());
        } else {
          excludedTraces.add(trace.getMzValue().getName());
        }
      }
      metaBlock.addCol(new TabCol("Included mz", ArrUtils.stringListToArr(includedTraces)));
      metaBlock.addCol(new TabCol("Excluded mz", ArrUtils.stringListToArr(excludedTraces)));

      // ################################################################################
      // now iterate over all interesting traces and perform the p-value estimation
      // ################################################################################
      LOGGER.trace("p-Value accumulation search starts. p-Value calculation...");

      // store results as p value computation is costly
      HashMap<Trace, HashMap<MultiKey, Double>> pValueMap = new HashMap<>();
      // keep access to bln distr
      HashMap<Trace, StatCollection> statCollectionMap = new HashMap<>();
      preScreenMap.keySet().forEach(t -> {
        Baseline bln = t.getBaseline();
        StatCollection bgDist = new StatCollectionRAM(bln.getBackgroundDistribution());
        statCollectionMap.put(t, bgDist);
        pValueMap.put(t, new HashMap<>());
      });
      // export all p values of a trace
      HashMap<Trace, double[]> pValuesInTraceMap = new HashMap<>();
      preScreenMap.keySet().forEach(t -> {
        int size = t.getTISeries().size();
        pValuesInTraceMap.put(t, new double[size]);
      });

      // p value container
      double[] lowestLocalPValues = new double[rawDataMaxSize]; // smallest uncombined p
      double[] testStatsH = new double[rawDataMaxSize];

      // prepare Fisher's method
      int k = preScreenMap.keySet().size();

      if (k > 0) {
        int mod = testStatsH.length / 10;
        for (int i = 0; i < testStatsH.length; i++) {

          if (i > 0 && i % mod == 0) {
            LOGGER.trace("p-Value computation at data point " + i + " / " + testStatsH.length);
          }

          // store p values across traces at data point i
          double[] localPValues = new double[preScreenMap.keySet().size()];

          // index to keep track of the traces
          int localTraceIdx = 0;

          for (Trace trace : preScreenMap.keySet()) {
            Baseline bln = trace.getBaseline();
            boolean hasSlices = bln.getBackgroundDistribution().size() > 1;
            int rawDataLen = trace.getTISeries().size();
            if (bln.hasBaseline()) {
              // .getIntensity()[i] should be fine as the access is done via SoftReference.
              // Else: load array here to combat bottleneck.
              double intensity = trace.getTISeries().getIntensity()[i];

              HashMap<MultiKey, Double> pStorage = pValueMap.get(trace);

            /*
             Compute key: this is heavy
             speed up computation by rounding the intensity; matters for TOF data;
             note that we must consider signal and the baseline as p values depend on both.
             */
              StatCollection bgDist = statCollectionMap.get(trace);
              StatDataSet distribution = bgDist.interpolate(i, rawDataLen);
              int distKey = distribution.getDistrID();
              double roundedIntensity;
              MultiKey key;
              // keep intensity as precise as possible for good p
              roundedIntensity = Math.round(intensity * 1000d) / 1000d;
              if (!hasSlices) {
                // roundedMean = 0;  mean is equal everywhere -> not part of key
                // roundedSpread = 0;  spread is equal everywhere -> not part of key
                key = new Key1(roundedIntensity);
              } else {
                double roundedMean = Math.round(distribution.getLocation() * 100d) / 100d;
                double roundedSpread;
                if (ResettableStatDataSet.Limit.isPoisson(distKey)) {
                  roundedSpread = 0; // not needed in Poisson
                } else {
                  roundedSpread = Math.round(distribution.getSpread() * 100d) / 100d;
                }
                key = new Key4(distKey, roundedMean, roundedSpread, roundedIntensity);
              }

              double p;
              if (pStorage.containsKey(key)) {
                p = pStorage.get(key);
              } else {
                p = distribution.calcPValue(roundedIntensity);
                pStorage.put(key, p);
              }

              // Clamp Fisher result but no the intermediate steps (unless the distribution class gives bad
              // results)
              // if (p < MIN_P_VALUE) {
              // p = MIN_P_VALUE;
              // }

              localPValues[localTraceIdx] = p;
              // for the export
              pValuesInTraceMap.get(trace)[i] = (p);
            } else {
              // if no baseline, put 1, i.e., the "no event" indicator
              localPValues[localTraceIdx] = 1;
            }
            localTraceIdx++;
          }

          // combine local pValues: Calculate -2 * sum(ln(p_i)) (Fisher's method): only if n>1
          if (localPValues.length > 1) {
            double chiSquareStat = 0;
            for (double p : localPValues) {
              if (p <= 0 || p > 1.0) {
                LOGGER.trace("p cannot be smaller or equal to zero or larger than one. p=" + p);
              }
              // prevent failure here by clamping p.
              p = Math.max(Math.nextUp(0d), p);
              p = Math.min(p, 1);
              chiSquareStat += -2.0 * Math.log(p);
            }
            double lowestLocalPValue = ArrUtils.getMin(localPValues);
            // we should not report p=0 here;
            lowestLocalPValue = Math.max(lowestLocalPValue, MIN_P_VALUE);
            lowestLocalPValues[i] = lowestLocalPValue;
            // test stat
            testStatsH[i] = chiSquareStat;
          } else {
            lowestLocalPValues[i] = localPValues[localPValues.length - 1];
          }
        }

        // if only one isotope was selected, do not compute
        double[] pValuesChiSquared; // combined p
        double[] pValuesGamma; // combined p
        if (preScreenMap.keySet().size() > 1) {
          // ALTERNATIVE: use gamma dist empirically to estimate p
          pValuesGamma = Statistics.estimateGammaP(preScreenMap.size(), testStatsH);
          pValuesChiSquared = Statistics.computeChiSquareP(preScreenMap.size(), testStatsH);
        } else {
          // Just one isotope: no need for testing, just use local p value
          pValuesChiSquared = lowestLocalPValues;
          pValuesGamma = lowestLocalPValues;
        }

        LOGGER.trace("Finished computing p-values. Adding values to export table...");

        // Export after i iteration but sort by isotope number
        List<Trace> exportTraces = new ArrayList<>(preScreenMap.keySet());
        exportTraces.sort(Comparator.comparingDouble(o -> o.getMzValue().getIsotope().getIsotopicNumber()));
        boolean needsTime = true;
        for (Trace trace : exportTraces) {
          if (needsTime) {
            pBlock.addCol(new TabCol("All", "Time [s]",
                SnF.doubleToStrArr(trace.getTISeries().getTime(), NF.D1C6)));
            needsTime = false;
          }
          if (selIsotopes.contains(trace.getMzValue().getIsotope())) {
            pBlock.addCol(new TabCol(trace.getMzValue().getName(), "Intensity [cts]",
                SnF.doubleToStrArr(trace.getTISeries().getIntensity()
                    , NF.D1C6)));
            pBlock.addCol(new TabCol("p value", SnF.doubleToStrArr(pValuesInTraceMap.get(trace),
                NF.D1C6Exp)));
          }
        }
        if (considerSingleComponent) {
          pBlock.addCol(new TabCol("lowest local p value", SnF.doubleToStrArr(lowestLocalPValues,
              NF.D1C6Exp)));
        }
        pBlock.addCol(new TabCol("Test H", SnF.doubleToStrArr(testStatsH, NF.D1C6)));
        pBlock.addCol(new TabCol("p value Chi Square", SnF.doubleToStrArr(pValuesChiSquared, NF.D1C6Exp)));
        pBlock.addCol(new TabCol("p value via Gamma dist", SnF.doubleToStrArr(pValuesGamma, NF.D1C6Exp)));

        // Finish export
        LOGGER.trace("Adding blocks to table...");
        blocks.forEach(coll::add);
        LOGGER.trace("Start writing table...");
        coll.export();
      }
    }
  }

  public static List<String> getShortMeta(@Nullable Sample sample) {
    List<String> strings = new ArrayList<>();
    strings.add("SpTool version " + SpTool3Main.VERSION_ID);
    strings.add("Date: " + Util.dateToString());
    if (sample != null) {
      strings.add("Nickname: " + sample.getNickName());
      strings.add("Name: " + sample.getSampleFile().getNameWithinFile());
      strings.add("Path: " + sample.getSampleFile().getPath() + sample.getSampleFile().getPath());
    }
    return strings;
  }

}
