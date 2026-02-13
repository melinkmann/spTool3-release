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
import java.util.Objects;

import com.orsonpdf.filter.FilterType;
import processing.options.GatingOption;
import processing.options.SearchAlgorithm;
import tasks.single.FilterTask;

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
        isEquiv = paramSetLabel == ((RoiSubtype) other).paramSetLabel;
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
        isEquiv = isEquiv && description == ((FilterSubtype) other).description;
      }
      return isEquiv;
    }

    public String getDescription() {
      return description;
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
      }else {
        isEquiv = false;
      }
      return isEquiv;
    }
  }
  final class SearchSubtype implements PopulationStep, Serializable {

    @Serial
    private static final long serialVersionUID = 1_000_000L;

    SearchAlgorithm searchAlgorithm;

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

}