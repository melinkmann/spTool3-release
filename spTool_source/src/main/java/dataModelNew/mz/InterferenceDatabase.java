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
  Please note and cite the original literature!
  https://doi.org/10.1016/j.sab.2021.106098
 */

package dataModelNew.mz;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InterferenceDatabase {

  private static final Logger LOGGER = LogManager.getLogger(InterferenceDatabase.class.getName());

  public static List<InterferenceEntry> loadFromCSV() {
    List<InterferenceEntry> entries = new ArrayList<>();

    String filePath = "/interferences.csv";

    try {
      InputStream is = InterferenceDatabase.class.getResourceAsStream(filePath);

      // CSV reader: does not throw exceptions
      CSVParser csvParser = new CSVParserBuilder().withSeparator('\t').build();
      // CSV reader: throw exceptions
      BufferedInputStream bufferedInputStream = new BufferedInputStream(is);
      InputStreamReader inputStreamReader = new InputStreamReader(bufferedInputStream,
          StandardCharsets.UTF_8);

      // CSV reader: does not throw exceptions
      Reader reader = new BufferedReader(inputStreamReader);
      CSVReader csvReader = new CSVReaderBuilder(reader).withSkipLines(2)
          .withCSVParser(csvParser)
          .build();

      String[] line;
      while ((line = csvReader.readNext()) != null) {
        if (line.length > 4) {
          entries.add(new InterferenceEntry(line[0], line[1], line[2], line[3], line[4]));
        }
      }
    } catch (Exception e) {
      LOGGER.error("Cannot load csv data. Message: " + ExceptionUtils.getMessage(e)
          + " Details: " + ExceptionUtils.getStackTrace(e));
    }
    return entries;
  }

  public static String SOURCE = """
      Source: https://doi.org/10.1016/j.sab.2021.106098
      by Madeleine C. Lomax-Vogt, Fang Liu, John W. Olesik.
            
      Note that the data base handles half masses from doubly charged odd masses
      as being present in both neighboring even masses, i.e.,
      '139La++ @ mass 69.5 is treated as both 69 and 70.
            
      """;

  public static class InterferenceEntry {

    public final String mz;
    public final String analyteElementIon;
    public final String elementOverlapIonContains;
    public final String overlapIon;
    public final String ionType;

    public InterferenceEntry(String mz, String analyteElementIon, String elementOverlapIonContains,
        String overlapIon, String ionType) {
      this.mz = mz;
      this.analyteElementIon = analyteElementIon;
      this.elementOverlapIonContains = elementOverlapIonContains;
      this.overlapIon = overlapIon;
      this.ionType = ionType;
    }

    @Override
    public String toString() {
      String formattedOverlapIon = splitIsotopes(overlapIon);
      return String.format(
          "m/z: %-20s Analyte: %-20s interference: %-20s ion: %-20s type: %-20s",
          mz, analyteElementIon, elementOverlapIonContains, formattedOverlapIon, ionType
      );
    }
  }

  public static String splitIsotopes(String s) {
    StringBuilder result = new StringBuilder();

    if (!s.isEmpty()) {
      int safetyCounter = 0;
      int i = 0;
      while (i < s.length() && safetyCounter < 1E6) {
        safetyCounter++;
        // Collect numeric isotope part
        StringBuilder isotope = new StringBuilder();
        while (i < s.length() && Character.isDigit(s.charAt(i))) {
          isotope.append(s.charAt(i));
          i++;
        }

        // Collect element symbol (1 or 2 letters)
        StringBuilder element = new StringBuilder();
        if (i < s.length() && Character.isLetter(s.charAt(i))) {
          element.append(s.charAt(i));
          i++;
          if (i < s.length() && Character.isLowerCase(s.charAt(i))) {
            element.append(s.charAt(i));
            i++;
          }
        }

        // Append the group (isotope + element)
        if (isotope.length() > 0 || element.length() > 0) {
          result.append(isotope).append(element).append(" ");
        }
      }

    }

    // Remove the trailing space and return
    return result.toString().trim();
  }
}
