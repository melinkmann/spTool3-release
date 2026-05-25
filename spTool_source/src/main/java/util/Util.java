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

import core.SpTool3Main;
import dataModelNew.InstrumentID;
import dataModelNew.Sample;
import dataModelNew.SampleFile;
import gui.dialog.DialogUtil;
import gui.dialog.FxEntry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import gui.dialog.caseImpl.CsvLoader;
import gui.dialog.caseImpl.NuLoader;
import gui.dialog.notification.NotificationFactory;
import gui.dialog.notification.PopupFactory;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.freehep.graphicsio.pdf.PDFPathConstructor;
import org.jetbrains.annotations.Nullable;
import processing.parameterSets.AvailableParameterSets;
import processing.parameterSets.Method;
import processing.parameterSets.ParamSet;
import processing.parameterSets.impl.CsvInterpreterParams;
import processing.parameterSets.impl.NuInterpreterParams;

import static gui.dialog.notification.NotificationFactory.openYesNo;

public abstract class Util {

  private static final Logger LOGGER = LogManager.getLogger(Util.class);

  private static final double LOG_2 = Math.log(2);

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


  @Nullable
  public static NuInterpreterParams getNuParametersFromMethod(Method method) {
    NuInterpreterParams nuParams = null;
    List<ParamSet> nuSets = method.getSets().stream()
        .filter(s -> s.getEnum().equals(AvailableParameterSets.NU_READER))
        .collect(Collectors.toList());
    if (!nuSets.isEmpty() && nuSets.get(0) instanceof NuInterpreterParams) {
      nuParams = (NuInterpreterParams) nuSets.get(0);
    }
    return nuParams;
  }

  @Nullable
  public static CsvInterpreterParams getCSVParamsFromMethod(Method method) {
    CsvInterpreterParams csvParams = null;
    List<ParamSet> csvSets = method.getSets().stream()
        .filter(s -> s.getEnum().equals(AvailableParameterSets.CSV_READER))
        .collect(Collectors.toList());
    if (!csvSets.isEmpty() && csvSets.get(0) instanceof CsvInterpreterParams) {
      csvParams = (CsvInterpreterParams) csvSets.get(0);
    }
    return csvParams;
  }

  @Nullable
  public static NuInterpreterParams getNuParametersFromDialog() {
    NuInterpreterParams nuParams = null;
    NuLoader launcher = new NuLoader();
    Optional<ParamSet> nuOptional = launcher.showAndWait();

    if (nuOptional.isPresent()) {
      ParamSet params = nuOptional.get();
      if (params instanceof NuInterpreterParams) {
        nuParams = (NuInterpreterParams) params;
      }
    }
    return nuParams;
  }

  @Nullable
  public static CsvInterpreterParams getCSVParamsFromDialog(List<Path> files) {
    CsvInterpreterParams csvParams = null;
    CsvLoader launcher = new CsvLoader(files);
    Optional<ParamSet> csvReaderOptional = launcher.showAndWait();

    if (csvReaderOptional.isPresent()) {
      ParamSet params = csvReaderOptional.get();
      if (params instanceof CsvInterpreterParams) {
        csvParams = (CsvInterpreterParams) params;
      }
    }
    return csvParams;
  }


  public static boolean isNuPath(Path path) {
    // Check which type to use: Check if TOF first.
    boolean nonNull = false;
    boolean foundTOF = false;

    if (path != null) {
      nonNull = true;
      String fileName = path.getFileName().toString();
      File pathAsFile = path.toFile();

      // check: "generate" the run.info file and check if it exists
      File runInfoFile = new File(path.toString(), "run.info");
      boolean hasRunInfo = runInfoFile.exists() && runInfoFile.isFile() && runInfoFile.canRead();

      boolean isRunInfo = fileName.equals("run.info")
          && pathAsFile.exists() && pathAsFile.isFile() && pathAsFile.canRead();

      foundTOF = hasRunInfo || isRunInfo;
    }

    return nonNull && foundTOF;
  }

  public static void updateSamplePath(List<Sample> samples) {
    int levels = SpTool3Main.getRunTime().getConfParams().getDragDropFolderDepth().getValue();
    List<Sample> issues = new ArrayList<>();
    List<Sample> fixed = new ArrayList<>();
    List<Sample> unFixed = new ArrayList<>();
    for (Sample sample : samples) {
      for (Sample subSample : sample.getAllSamples()) {
        SampleFile sampleFile = subSample.getSampleFile();
        if (sampleFile.getInstrumentID().equals(InstrumentID.NU_VITESSE)) {
          // check if exists
          boolean exists = isNuPath(sampleFile.getFilePath());
          if (!exists) {
            issues.add(subSample);
          }
        }
      }
    }

    if (!issues.isEmpty()) {

      String message = """
          There are samples whose run.info files cannot be found on drive.
          Have they been moved? You can set a new parent directory here
          and spTool tries to match the expected folder name with those folders found in the directory.
          It will list subdirectories in your input directory (depth is specified in the configuration).
          Start recovery?
          
          """;

      String issueNames = "Affected samples:";
      for (Sample issue : issues) {
        issueNames = issueNames + "\n" + issue.getSampleFile().getFilePath();
      }

      NotificationFactory.openYesNo(message + issueNames,
          () -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select run.info directory");
            File dir = directoryChooser.showDialog(SpTool3Main.getMainStage());

            if (dir.isDirectory()) {
              for (Sample issue : issues) {

                SampleFile issueFile = issue.getSampleFile();
                Path missingPath = issueFile.getFilePath();
                Path matchedPath = null;

                String targetName = missingPath.getFileName().toString();
                try (Stream<Path> stream = Files.find(dir.toPath(), levels,
                    (path, attrs) -> attrs.isDirectory() && path.getFileName().toString().equals(targetName))) {
                  matchedPath = stream
                      .filter(Util::isNuPath)
                      .findFirst()
                      .orElse(null);
                } catch (IOException e) {
                  LOGGER.error(e.getMessage() + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
                }

                if (matchedPath != null && isNuPath(matchedPath)) {
                  LOGGER.info("Found match: "
                      + "\n" + matchedPath
                      + "\nfor missing path: " + missingPath);
                  issue.getSampleFile().setUriFile(matchedPath.toUri());
                  fixed.add(issue);
                } else {
                  LOGGER.error("NO MATCH for missing path: " + missingPath);
                  unFixed.add(issue);
                }
              }
            }

            String fixedNames = "\n\nSuccess: ";
            String unfixedNames = "\n\nFailed to fix: ";

            for (Sample issue : fixed) {
              fixedNames = fixedNames + "\n" + issue.getSampleFile().getFilePath();
            }
            for (Sample issue : unFixed) {
              unfixedNames = unfixedNames + "\n" + issue.getSampleFile().getFilePath();
            }

            NotificationFactory.openInfo("Report:" + fixedNames + unfixedNames);
          });

    } else {
      NotificationFactory.openInfo("There are no issues regarding sample files.");
    }


  }


  public static boolean isCsvLikeFile(Path path) {
    File pathAsFile = path.toFile();
    return pathAsFile.exists() && pathAsFile.isFile() && pathAsFile.canRead();
  }


  public static Path getCheckedNuPathDir(Path directoryOrFile) {
    Path result;
    String fileName = directoryOrFile.getFileName().toString();
    if (fileName.equals("run.info")) {
      result = directoryOrFile.getParent(); // step up one level
    } else {
      result = directoryOrFile;
    }
    return result;
  }

  public static double log2(double x) {
    return Math.log(x) / LOG_2;
  }

  public static double[] log2(double[] a) {
    double[] arr = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      arr[i] = Util.log2(a[i]);
    }
    return arr;
  }

}
