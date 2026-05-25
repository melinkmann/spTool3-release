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

import java.io.Serial;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import dataModelNew.Sample;
import processing.options.GatingOption;
import processing.options.SearchAlgorithm;
import sandbox.montecarlo.Isotope;

public interface PopulationStep extends Serializable {


  public int customHash();

  boolean isEquivalent(PopulationStep other);

  public String translate();

  final class NoSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;


    public NoSubtype() {
    }

    @Override
    public String translate() {
      return "";
    }

    @Override
    public int customHash() {
      return 0;
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      return other instanceof NoSubtype;
    }
  }

  final class RoiSubtype implements PopulationStep, Serializable {

    /*
    Note:
    Equality is determined by INPUTs not by RESULTS.
    Example: We want to select the same percentiles
    but not what actual numbers these correspond to.
     */

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String paramSetLabel;

    public RoiSubtype(String paramSetLabel) {
      this.paramSetLabel = paramSetLabel;
    }

    @Override
    public int customHash() {
      return Objects.hash(paramSetLabel);
    }

    @Override
    public String translate() {
      return "Roi(" + paramSetLabel + ")";
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof RoiSubtype) {
        isEquiv = Objects.equals(paramSetLabel, ((RoiSubtype) other).paramSetLabel);
      }
      return isEquiv;
    }
  }

  final class ManualRoiSubtype implements PopulationStep, Serializable {

    /*
    Note:
    Equality is determined by INPUTs not by RESULTS.
    Example: We want to select the same percentiles
    but not what actual numbers these correspond to.
     */

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String paramSetLabel;
    private final double start;
    private final double end;

    public ManualRoiSubtype(String paramSetLabel, double start, double end) {
      this.paramSetLabel = paramSetLabel;
      this.start = start;
      this.end = end;
    }

    @Override
    public int customHash() {
      return Objects.hash(paramSetLabel);
    }

    @Override
    public String translate() {
      return "MRoi(" + paramSetLabel + ")";
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof ManualRoiSubtype) {
        isEquiv = isEquiv && Objects.equals(paramSetLabel, ((ManualRoiSubtype) other).paramSetLabel);
        isEquiv = isEquiv && Objects.equals(start, ((ManualRoiSubtype) other).start);
        isEquiv = isEquiv && Objects.equals(end, ((ManualRoiSubtype) other).end);
      }
      return isEquiv;
    }
  }

  class FilterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String description;

    public FilterSubtype(String description) {
      this.description = description;
    }

    @Override
    public int customHash() {
      return Objects.hash(description);
    }

    @Override
    public String translate() {
      return "Filter()";
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof FilterSubtype) {
        isEquiv = true;
        isEquiv = isEquiv && Objects.equals(description, ((FilterSubtype) other).description);
      }
      return isEquiv;
    }

    public String getDescription() {
      return description;
    }
  }

  final class ManualAlignFilterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String paramSetLabel;
    private final List<Isotope> isotopes;

    public ManualAlignFilterSubtype(String paramSetLabel,List<Isotope> isotopes) {
      this.paramSetLabel = paramSetLabel;
      this.isotopes = isotopes;
    }

    @Override
    public int customHash() {
      return Objects.hash(paramSetLabel,isotopes);
    }

    @Override
    public String translate() {
      return "Filter(" + paramSetLabel + ")";
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof ManualAlignFilterSubtype) {
        isEquiv = Objects.equals(paramSetLabel, ((ManualAlignFilterSubtype) other).paramSetLabel);
        //list equals actually checks entries :)
        isEquiv = isEquiv && isotopes.equals(((ManualAlignFilterSubtype) other).isotopes);
      }
      return isEquiv;
    }
  }


  final class MultiEventSubtype extends FilterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String nEvents;

    public MultiEventSubtype(String description, String nEvents) {
      super(description);
      this.nEvents = nEvents;
    }

    @Override
    public int customHash() {
      return Objects.hash(getDescription(), nEvents);
    }

    @Override
    public String translate() {
      return "Multi(n=" + nEvents + ")";
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = true;
      if (other instanceof MultiEventSubtype) {
        isEquiv = isEquiv && super.getDescription().equals(((MultiEventSubtype) other).getDescription());
        isEquiv = isEquiv && nEvents.equals(((MultiEventSubtype) other).nEvents);
      }
      return isEquiv;
    }
  }

  final class OverRangeSubtype extends FilterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String overRangeLabel;

    public OverRangeSubtype(String description, String overRangeLabel) {
      super(description);
      this.overRangeLabel = overRangeLabel;
    }

    @Override
    public int customHash() {
      return Objects.hash(getDescription(), overRangeLabel);
    }

    @Override
    public String translate() {
      return overRangeLabel;
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = true;
      if (other instanceof OverRangeSubtype) {
        isEquiv = isEquiv && super.getDescription().equals(((OverRangeSubtype) other).getDescription());
        isEquiv = isEquiv && overRangeLabel.equals(((OverRangeSubtype) other).overRangeLabel);
      }
      return isEquiv;
    }
  }

  final class SimMatchSubtype extends FilterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String matchLabel;

    public SimMatchSubtype(String description, String matchLabel) {
      super(description);
      this.matchLabel = matchLabel;
    }

    @Override
    public int customHash() {
      return Objects.hash(getDescription(), matchLabel);
    }

    @Override
    public String translate() {
      return matchLabel;
    }


    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = true;
      if (other instanceof SimMatchSubtype) {
        isEquiv = isEquiv && super.getDescription().equals(((SimMatchSubtype) other).getDescription());
        isEquiv = isEquiv && matchLabel.equals(((SimMatchSubtype) other).matchLabel);
      } else {
        isEquiv = false;
      }
      return isEquiv;
    }
  }

  final class SearchSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final SearchAlgorithm searchAlgorithm;

    public SearchSubtype(SearchAlgorithm searchAlgorithm) {
      this.searchAlgorithm = searchAlgorithm;
    }

    @Override
    public int customHash() {
      return Objects.hash(searchAlgorithm);
    }

    @Override
    public String translate() {
      return searchAlgorithm.shortString();
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof SearchSubtype) {
        isEquiv = searchAlgorithm == ((SearchSubtype) other).searchAlgorithm;
      }
      return isEquiv;
    }

    public SearchAlgorithm getSearchAlgorithm() {
      return searchAlgorithm;
    }
  }

  final class GateSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    GatingOption gatingOption;

    public GateSubtype(GatingOption gatingOption) {
      this.gatingOption = gatingOption;
    }

    @Override
    public int customHash() {
      return Objects.hash(gatingOption);
    }

    @Override
    public String translate() {
      return "Gate(" + gatingOption.shortString() + ")";
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof GateSubtype) {
        isEquiv = gatingOption == ((GateSubtype) other).gatingOption;
      }
      return isEquiv;
    }
  }

  final class AlignSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    public AlignSubtype() {
    }

    @Override
    public int customHash() {
      //return Objects.hash(... if there were fields ...);
      return AlignSubtype.class.hashCode();
    }

    @Override
    public String translate() {
      return "ALN";
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof AlignSubtype) {
        isEquiv = true;
      }
      return isEquiv;
    }
  }

  final class ClusterSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String uniqueSampleString;
    private final int clusterIndex;
    private final String clusterName;


    public ClusterSubtype(Sample sample, int clusterIndex, String clusterName) {
      this.uniqueSampleString = sample.getNickName() + sample.hashCode(); // should be unique?!
      this.clusterIndex = clusterIndex;
      this.clusterName = clusterName;
    }

    @Override
    public int customHash() {
      return Objects.hash(uniqueSampleString, clusterIndex, clusterName);
    }

    @Override
    public String translate() {
      return clusterName;
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof ClusterSubtype) {
        isEquiv = Objects.equals(uniqueSampleString, ((ClusterSubtype) other).uniqueSampleString);
        isEquiv = isEquiv && clusterIndex == ((ClusterSubtype) other).clusterIndex;
        isEquiv = isEquiv && Objects.equals(clusterName, ((ClusterSubtype) other).clusterName);
      }
      return isEquiv;
    }
  }

  final class PolygonSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    private final String labelString;
    private final double[] xVertices;
    private final double[] yVertices;

    public PolygonSubtype(String labelString, double[] xVertices, double[] yVertices) {
      this.labelString = labelString;
      this.xVertices = xVertices;
      this.yVertices = yVertices;
    }


    @Override
    public int customHash() {
      return Objects.hash(labelString, Arrays.hashCode(xVertices), Arrays.hashCode(yVertices));
    }

    @Override
    public String translate() {
      return labelString;
    }

    @Override
    public boolean isEquivalent(PopulationStep other) {
      boolean isEquiv = false;
      if (other instanceof PolygonSubtype) {
        isEquiv = Objects.equals(labelString, ((PolygonSubtype) other).labelString);
        isEquiv = isEquiv && Arrays.equals(xVertices, ((PolygonSubtype) other).xVertices);
        isEquiv = isEquiv && Arrays.equals(yVertices, ((PolygonSubtype) other).yVertices);
      }
      return isEquiv;
    }
  }

}