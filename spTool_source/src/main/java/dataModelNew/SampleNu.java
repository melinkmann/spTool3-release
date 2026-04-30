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

public class SampleNu {
  /*
  TODO
    Add option to reader: store the sample as QMS or just a reference to file
    and reload based on isotopes?

    Maybe "TOF sample impl"
    that extends SamlpeImpl for all
    but overrides TOF: save project should not save raw data
    but ref to path, import settings and processing method (reprocess upon loading)

    This can be combined nicely with a select elements
    List<Isotope> availableIsotopes
    List<Isotope> selectedIsotopes

    Options:
      What to do with blanking?
      set to zero
      keep as is
      remove from data?
       */
}
