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

package processing.options;

public enum EventType {

  NP {
    @Override
    public String toString() {
      return "Particles";
    }

    @Override
    public String getShortString() {
      return "NP";
    }
  },

  BG {
    @Override
    public String toString() {
      return "Background";
    }

    @Override
    public String getShortString() {
      return "BG";
    }
  },

  BG_NP {
    @Override
    public String toString() {
      return "Background & NP";
    }

    @Override
    public String getShortString() {
      return "BG & NP";
    }
  },

  RAW {
    @Override
    public String toString() {
      return "Raw data";
    }

    @Override
    public String getShortString() {
      return "Raw";
    }
  };

  public abstract String getShortString();


}
