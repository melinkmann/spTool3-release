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

import gui.dialog.FxEntry;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;

public abstract class Util {

  private static final Logger LOGGER = LogManager.getLogger(Util.class);


  public static SimpleDateFormat getStandardDateFormat() {
    // You need to set Locale to US, else you get "Nov." instead of "Nov" (note the dot ".")
    return new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
  }

  public static String getYearMonthDate() {
    // new instance as the formatter is not thread safe!
    // You need to set Locale to US, else you get "Nov." instead of "Nov" (note the dot ".")
    final SimpleDateFormat spToolDateFormat = new SimpleDateFormat("yyyy-MMM", Locale.US);
    Date date = new Date();
    String dateValue = spToolDateFormat.format(date);
    return dateValue;
  }

  public static String getYearMonthDateDayHourMinuteSecond() {
    // new instance as the formatter is not thread safe!
    // You need to set Locale to US, else you get "Nov." instead of "Nov" (note the dot ".")
    final SimpleDateFormat spToolDateFormat = new SimpleDateFormat("yyyy-MMM-dd_HH-mm-ss",
        Locale.US);
    Date date = new Date();
    String dateValue = spToolDateFormat.format(date);
    return dateValue;
  }

  public static String dateToString() {
    // new instance as the formatter is not thread safe!
    final SimpleDateFormat spToolDateFormat = getStandardDateFormat();
    Date date = new Date();
    String dateValue = spToolDateFormat.format(date);
    return dateValue;
  }

  public static Date stringToDate(String string) {
    // new instance as the formatter is not thread safe!
    final SimpleDateFormat spToolDateFormat = getStandardDateFormat();
    Date date;
    try {
      date = spToolDateFormat.parse(string);
    } catch (ParseException e) {
      date = new Date();
      LOGGER.info("Unexpected date format. Stack trace: "
          + ExceptionUtils.getStackTrace(e));
    }
    return date;
  }

  public static double restrict(double num, double lower, double upper) {
    return Math.min(Math.max(num, lower), upper);
  }

  public static <K, V> void put(Map<K, List<V>> map, K key, V value) {
    if (map != null & value != null & key != null) {
      if (map.containsKey(key)) {
        map.get(key).add(value);
      } else {
        List<V> list = new ArrayList<>();
        list.add(value);
        map.put(key, list);
      }
    }
  }

  public static <K, V> void put(Map<K, List<V>> map, K key, List<V> values) {
    if (map != null & values != null & key != null) {
      if (map.containsKey(key)) {
        map.get(key).addAll(values);
      } else {
        List<V> list = new ArrayList<>(values);
        map.put(key, list);
      }
    }
  }




  public static void windowsSortFile(List<Path> unsortedFiles) {
    if (unsortedFiles.size() > 1) {
      // sorting: found nice explorer-like WindowsSorter
      unsortedFiles.sort(new Comparator<Path>() {
        private final Comparator<String> NATURAL_SORT = new WindowsSorter.WindowsExplorerComparator();

        @Override
        public int compare(Path o1, Path o2) {
          return NATURAL_SORT.compare(o1.toString(), o2.toString());
        }
      });
    }
  }

  public static <T> void windowsSortEntry(List<FxEntry<T>> unsortedFiles) {
    if (unsortedFiles.size() > 1) {
      // sorting: found nice explorer-like WindowsSorter
      unsortedFiles.sort(new Comparator<FxEntry<T>>() {
        private final Comparator<String> NATURAL_SORT = new WindowsSorter.WindowsExplorerComparator();

        @Override
        public int compare(FxEntry<T> o1, FxEntry<T> o2) {
          return NATURAL_SORT.compare(o1.getLabel().toString(), o2.getLabel().toString());
        }
      });
    }
  }

  public static void windowsSortParamSet(List<ParamSet> unsorted) {
    if (unsorted.size() > 1) {
      // sorting: found nice explorer-like WindowsSorter
      unsorted.sort(new Comparator<ParamSet>() {
        private final Comparator<String> NATURAL_SORT = new WindowsSorter.WindowsExplorerComparator();

        @Override
        public int compare(ParamSet o1, ParamSet o2) {
          return NATURAL_SORT.compare(o1.getLabelParameter().getValue(),
              o2.getLabelParameter().getValue());
        }
      });
    }
  }

  public static void windowsSortMethod(List<Method> unsorted) {
    if (unsorted.size() > 1) {
      // sorting: found nice explorer-like WindowsSorter
      unsorted.sort(new Comparator<Method>() {
        private final Comparator<String> NATURAL_SORT = new WindowsSorter.WindowsExplorerComparator();

        @Override
        public int compare(Method o1, Method o2) {
          return NATURAL_SORT.compare(o1.getLabelParam().getValue(),
              o2.getLabelParam().getValue());
        }
      });
    }
  }

  // Oldest at top i.e., inverts the sort order
  public static <T> void dateSortEntry(List<FxEntry<T>> unsorted) {
    if (unsorted.size() > 1) {
      unsorted.sort(LIST_DATE_COMPARATOR);
    }
  }

  public static final Comparator<FxEntry<?>> LIST_DATE_COMPARATOR = new Comparator<FxEntry<?>>() {

    @Override
    public int compare(FxEntry<?> o1, FxEntry<?> o2) {
      if (o1.getDate() == null || o2.getDate() == null) {
        return 0; // Implies they are equal. Potential for bugs?
      }
      // Minus sing inverts: newest at the top
      return -o1.getDate().compareTo(o2.getDate());
    }
  };

  public static final Comparator<String> DATE_COMPARATOR = new Comparator<String>() {

    @Override
    public int compare(String o1, String o2) {
      Date d1;
      Date d2;
      try {
        d1 = Util.stringToDate(o1);
        d2 = Util.stringToDate(o2);
        // Minus sing inverts: newest at the top
        return -d1.compareTo(d2);
      } catch (Exception e) {
        LOGGER.info("Unexpected date format. Stack trace: "
            + ExceptionUtils.getStackTrace(e));
        return 0; // Implies they are equal. Potential for bugs?
      }
    }
  };


}
