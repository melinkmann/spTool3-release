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

package util;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChannelTableComparator implements Comparator<String> {


  /**
   * This has issues! Sorting does not work predictably if we mix 109Ag, 109, 109Ag/107Ag, SAg type strings.
   * However, currently I think we only have numbers and letters in the table, and MZ=0 for computed
   * channels usually.
   */
  @Override
  public int compare(String o1, String o2) {
    int comp = 0;

    boolean isNum1 = SnF.isValidDoubleSilent(o1);
    boolean isNum2 = SnF.isValidDoubleSilent(o2);

    if (isNum1 && isNum2) {
      comp = Double.compare(SnF.strToDoubleSilent(o1), SnF.strToDoubleSilent(o2));
    } else {
      // special isotope handling
      Matcher m1 = ISOTOPE_PATTERN.matcher(o1);
      Matcher m2 = ISOTOPE_PATTERN.matcher(o2);

      if (m1.matches() && m2.matches()) {

        int mass1 = Integer.parseInt(m1.group(1));
        int mass2 = Integer.parseInt(m2.group(1));

        // if equal, check element symbol
        comp = Integer.compare(mass1, mass2);
        if (comp == 0) {

          String element1 = m1.group(2);
          String element2 = m2.group(2);

          comp = element1.compareToIgnoreCase(element2);
        }
      } else {
        WindowsSorter.WindowsExplorerComparator winComp = new WindowsSorter.WindowsExplorerComparator();
        comp = winComp.compare(o1, o2);
      }
    }

    return comp;
  }

  private static final Pattern ISOTOPE_PATTERN =
      Pattern.compile("^(\\d+)([A-Za-z]+)$");

}
