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

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javafx.util.Pair;
import math.Arithmetic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.options.MathMod;
import sandbox.montecarlo.Comparators;

public abstract class ArrUtils {

  private static final Logger LOGGER = LogManager.getLogger(ArrUtils.class);

  /*
  JUST FYI

  stream with boolean :)
    return populations.entrySet().stream().
        map(Map.Entry::getKey)
        .filter(this::hasPopulation)
        .collect(Collectors.toCollection(ArrayList::new));
        }

 stream map to list
    return removedPopulations.entrySet().stream()
        .map(Map.Entry::getValue)
        .collect(Collectors.toCollection(ArrayList::new));



  Stream with Integer
     Arrays.stream(intArray).mapToDouble(Integer::valueOf).toArray();
   */


  public static final double DEFAULT_FAIL_DOUBLE = -1;
  public static final int DEFAULT_FAIL_INT = -1;


  /*
  ----------------------------------------------------------------------------------------
  MATH stuff
  ----------------------------------------------------------------------------------------
   */

  public static List<Double> getReciprocal(List<? extends Number> numbers, double valueIfZero) {
    List<Double> reciprocal = new ArrayList<>();
    for (Number n : numbers) {
      double d = n.doubleValue();
      if (!Arithmetic.isNotZero(d)) {
        d = valueIfZero;
        LOGGER.debug("Input was equal to zero and its reciprocal could not be calculated."
            + " Especially for WLS: Weight w = " + valueIfZero
            + " (w = 1 corresponds to an OLS-approach and was returned instead).");
      }
      reciprocal.add(1 / d);
    }
    return reciprocal;
  }


  public static List<Double> getReciprocal(List<? extends Number> numbers) {
    return numbers.stream()
        .filter(d -> Arithmetic.isNotZero((double) d))
        .map(d -> 1 / (double) d).collect(Collectors.toList());
  }

  public static double[] normalizeByMaximumTimesFactor(double[] data, double factor) {
    double[] result = new double[data.length];
    double max = getMax(data);
    if (max > 0) {
      for (int i = 0; i < result.length; i++) {
        result[i] = factor * data[i] / max;
      }
    }
    return result;
  }

  public static double[] normalizeBySumOverriding(double[] data, double[] previousArray) {
    double sumPrev = 0;
    for (double d : previousArray) {
      sumPrev += d;
    }
    double sumData = 0;
    for (double d : data) {
      sumData += d;
    }

    double ratio = 1;
    if (sumData > 0) {
      ratio = sumPrev / sumData;
    }

    for (int i = 0; i < data.length; i++) {
      data[i] = data[i] * ratio;
    }
    return data;
  }

  public static void normalizeBySumOverriding(double[] array) {
    double sum = 0;
    for (double d : array) {
      sum += d;
    }
    for (int i = 0; i < array.length; i++) {
      array[i] = array[i] / sum;
    }
  }

  public static double[] normalize(double[] array, double divisor) {
    double[] result = new double[array.length];
    if (divisor != 0) {
      for (int i = 0; i < array.length; i++) {
        result[i] = array[i] / divisor;
      }
    } else {
      result = array;
    }
    return result;
  }

  public static void roundOverriding(double[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = Math.round(array[i]);
    }
  }

  public static void ceilOverriding(double[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = Math.ceil(array[i]);
    }
  }

  public static void floorOverriding(double[] array) {
    for (int i = 0; i < array.length; i++) {
      array[i] = Math.floor(array[i]);
    }
  }

  public static double[] cdf(double[] data) {
    double[] result = copy(data);
    Arrays.sort(result);
    for (int i = 0; i < result.length; i++) {
      result[i] = (i + 1) / (double) result.length;
    }
    return result;
  }

  public static List<Double> cdf(List<Double> data) {
    List<Double> result = new ArrayList<>(data);
    Collections.sort(result);
    for (int i = 0; i < result.size(); i++) {
      result.set(i, (i + 1) / (double) result.size());
    }
    return result;
  }

  public static double[] barCDF(double[] histBarHeights) {
    double[] result = new double[histBarHeights.length];

    if (histBarHeights.length > 0) {
      int n = histBarHeights.length;
      double[] cdf = new double[n];

      // ensure positive bar heights
      for (int i = 0; i < histBarHeights.length; i++) {
        histBarHeights[i] = Math.max(0, histBarHeights[i]);
      }

      double total = 0.0;
      for (double h : histBarHeights) {
        total += h;
      }

      // ensure non degenerate distribution
      if (total > 0) {
        double cumulative = 0.0;
        for (int i = 0; i < n; i++) {
          cumulative += histBarHeights[i];
          cdf[i] = cumulative / total;
        }
        result = cdf;
      }
    }
    return result;
  }

  public static double[] copy(double[] data) {
    double[] copy = new double[data.length];
    System.arraycopy(data, 0, copy, 0, data.length);
    return copy;
  }

  public static int[] copy(int[] data) {
    int[] copy = new int[data.length];
    System.arraycopy(data, 0, copy, 0, data.length);
    return copy;
  }
  /*
  ----------------------------------------------------------------------------------------
  RANDOM sampling from array
  ----------------------------------------------------------------------------------------
   */

  // Randomly sample. Modified from ChatGPT4
  public static double[] sampleWithoutReplacement(double[] input, int n) {

    double[] result = new double[n];
    int l = input.length;
    Random rand = new Random();

    // Use Fisher–Yates shuffle for sampling
    int[] indices = new int[l];
    for (int i = 0; i < l; i++) {
      indices[i] = i;
    }

    for (int i = 0; i < n; i++) {
      int j = i + rand.nextInt(l - i);
      // Swap indices[i] and indices[j]
      int temp = indices[i];
      indices[i] = indices[j];
      indices[j] = temp;

      result[i] = input[indices[i]];
    }

    return result;
  }

  public static double[] sampleWithReplacement(double[] input, int n) {
    double[] result = new double[n];
    Random rand = new Random();

    for (int i = 0; i < n; i++) {
      int index = rand.nextInt(input.length); // Random index from 0 to input.length - 1
      result[i] = input[index];
    }

    return result;
  }

  /*
  ----------------------------------------------------------------------------------------
  MATLAB-like matrix operations (highly underrated and useful)
  ----------------------------------------------------------------------------------------
   */

  // --------------------------- Array vs. number of + and - ----------------------------------

  public static double[] add(double[] a, double b) {
    double[] sum = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      sum[i] = a[i] + b;
    }
    return sum;
  }

  public static int[] add(int[] a, int b) {
    int[] sum = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      sum[i] = a[i] + b;
    }
    return sum;
  }

  public static double[] add(int[] a, double b) {
    double[] sum = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      sum[i] = a[i] + b;
    }
    return sum;
  }

  public static double[] subtract(double[] a, double b) {
    double[] difference = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      difference[i] = a[i] - b;
    }
    return difference;
  }

  public static int[] subtract(int[] a, int b) {
    int[] difference = new int[a.length];
    for (int i = 0; i < a.length; i++) {
      difference[i] = a[i] - b;
    }
    return difference;
  }

  // --------------------------- Array vs. number of * and / -------------------------------------

  public static double[] multiply(double[] a, double b) {
    double[] product;
    if (a != null) {
      product = new double[a.length];
      for (int i = 0; i < a.length; i++) {
        product[i] = a[i] * b;
      }
    } else {
      product = new double[0];
    }
    return product;
  }

  // Dividing by zero will yield +-INF, only Integer division throws exception if dividing by 0.
  public static double[] divide(double[] a, double b) {
    double[] quotient = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      quotient[i] = a[i] / b;
    }
    return quotient;
  }

  // Dividing by zero will yield +-INF, only Integer division throws exception if dividing by 0.
  public static double[] divide(double b, double[] a) {
    double[] quotient = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      quotient[i] = b / a[i];
    }
    return quotient;
  }

  public static double[] divide(double b, int[] a) {
    double[] quotient = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      quotient[i] = b / (double) a[i];
    }
    return quotient;
  }

  public static double[] divide(List<Double> a, double b) {
    double[] quotient = new double[a.size()];
    for (int i = 0; i < a.size(); i++) {
      quotient[i] = a.get(i) / b;
    }
    return quotient;
  }


  // ----------------------- no return value! (saves RAM) -------------------------------------

  public static void multiplyOverriding(double[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = a[i] * b;
    }
  }

  public static void divideOverriding(double[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = a[i] / b;
    }
  }

  // --------------------------- Array vs. array of * and / ----------------------------------

  public static double[] multiply(double[] a, double[] b) {
    double[] product = new double[1];
    if (arrayMathCheck(a, b)) {
      product = new double[a.length];
      for (int i = 0; i < a.length; i++) {
        product[i] = a[i] * b[i];
      }
    }
    return product;
  }

  // Dividing by zero will yield +-INF, only Integer division throws exception if dividing by 0.
  public static double[] divide(double[] a, double[] b) {
    double[] quotient = new double[1];
    if (arrayMathCheck(a, b)) {
      quotient = new double[a.length];
      for (int i = 0; i < a.length; i++) {
        quotient[i] = a[i] / b[i];
      }
    }
    return quotient;
  }

  // --------------------------- Array vs. array of + and - ----------------------------------

  public static double[] add(double[] a, double[] b) {
    double[] sum = new double[1];
    if (arrayMathCheck(a, b)) {
      sum = new double[a.length];
      for (int i = 0; i < a.length; i++) {
        sum[i] = a[i] + b[i];
      }
    }
    return sum;
  }


  public static int[] add(int[] a, int[] b) {
    int[] sum = new int[1];
    if (arrayMathCheck(a, b)) {
      sum = new int[a.length];
      for (int i = 0; i < a.length; i++) {
        sum[i] = a[i] + b[i];
      }
    }
    return sum;
  }

  public static double[] subtract(double[] a, double[] b) {
    double[] difference = new double[1];
    if (arrayMathCheck(a, b)) {
      difference = new double[a.length];
      for (int i = 0; i < a.length; i++) {
        difference[i] = a[i] - b[i];
      }
    }
    return difference;
  }

  // ----------------------- no return value! (saves RAM) -------------------------------------

  public static void addOverriding(double[] a, double[] b) {
    if (arrayMathCheck(a, b)) {
      for (int i = 0; i < a.length; i++) {
        a[i] = a[i] + b[i];
      }
    }
  }

  public static void addOverriding(double[] a, double b) {
    for (int i = 0; i < a.length; i++) {
      a[i] = a[i] + b;
    }
  }

  // --------------------------- List vs. number of * and / -------------------------------------

  public static List<Double> multiply(List<Double> a, double b) {
    List<Double> product;
    if (a != null) {
      product = new ArrayList<>(a.size());
      for (Double v : a) {
        product.add(v * b);
      }
    } else {
      product = new ArrayList<>();
    }
    return product;
  }

  // --------------------------- Miscellaneous ------------------------------------------


  public static Pair<Integer, Integer> countTrue(boolean... booleans) {
    int count = 0;
    for (boolean b : booleans) {
      if (b) count++;
    }
    return new Pair<>(booleans.length, count);
  }


  // --------------------------- Array vs.function ------------------------------------------

  // e.g. exp(a), sine(a), ...
  public static double[] applyOperand(double[] a, Function<Double, Double> operand) {
    double[] difference = new double[a.length];
    for (int i = 0; i < a.length; i++) {
      difference[i] = operand.apply(a[i]);
    }
    return difference;
  }

  private static <T> boolean arrayMathCheck(T a, T b) {
    boolean isGood = false;

    if (a.getClass().isArray() && b.getClass().isArray()) {
      Class<?> compType1 = a.getClass().getComponentType();
      Class<?> compType2 = b.getClass().getComponentType();

      if (compType1.equals(compType2)) {

        int len1 = Array.getLength(a);
        int len2 = Array.getLength(b);

        if (len1 == len2) {
          isGood = true;
        } else {
          LOGGER.error("Cannot apply array operator arithmetic to arrays of different length.");
        }
      }
    }
    return isGood;
  }

  /*
  ----------------------------------------------------------------------------------------
  Get MIN MAX
  ----------------------------------------------------------------------------------------
   */

  public static double getMax(double[] array) {
    double max;
    if (array == null || array.length == 0) {
      max = 0;
      LOGGER.debug("Maximum of empty array returned min=" + max + ".");
    } else {
      max = -Double.MAX_VALUE; // NOT DOUBLE.MIN_VALUE because that is 4.9E-324
      for (double v : array) {
        if (v > max) {
          max = v;
        }
      }
    }
    return max;
    // Has to be slower (2x iteration): return Collections.max(ArrayListUtils.primitiveArrToList(array));
  }

  public static int getIdxAtMax(double[] array) {
    double max;
    int idx;
    if (array == null || array.length == 0) {
      max = 0;
      idx = 0;
      LOGGER.debug("Maximum of empty array returned min=" + max + ".");
    } else {
      max = -Double.MAX_VALUE; // NOT DOUBLE.MIN_VALUE because that is 4.9E-324
      idx = 0;
      for (int i = 0; i < array.length; i++) {
        if (array[i] > max) {
          max = array[i];
          idx = i;
        }
      }
    }
    return idx;
  }

  public static double getMin(double[] array) {
    double min;
    if (array == null || array.length == 0) {
      min = 0;
      LOGGER.debug("Minimum of empty array returned min=" + min + ".");
    } else {
      min = Double.MAX_VALUE;
      for (double v : array) {
        if (v < min) {
          min = v;
        }
      }
    }
    return min;
    // Alternative but more iterations: // return Collections.min(ArrayListUtils.primitiveArrToList(array));
  }

  public static int getIndexAtMin(double[] array) {
    int minIdx;
    double min;
    if (array == null || array.length == 0) {
      min = 0;
      minIdx = 0;
      LOGGER.debug("Index at minimum of empty array returned minIdx=" + minIdx + ".");
    } else {
      min = Double.MAX_VALUE;
      minIdx = 0;
      for (int i = 0; i < array.length; i++) {
        double v = array[i];
        if (v < min) {
          min = v;
          minIdx = i;
        }

      }
    }
    return minIdx;
    // Alternative but more iterations: // return Collections.min(ArrayListUtils.primitiveArrToList(array));
  }

  public static double getMaxViaStream(double[] arr) {
    double val = DEFAULT_FAIL_DOUBLE;
    if (arr != null && arr.length > 0) {
      val = Arrays.stream(arr).max().getAsDouble();
    }
    return val;
  }

  public static double getMinViaStream(double[] arr) {
    double val = DEFAULT_FAIL_DOUBLE;
    if (arr != null && arr.length > 0) {
      val = Arrays.stream(arr).min().getAsDouble();
    }
    return val;
  }

  public static int getMaxViaStream(int[] arr) {
    int val = DEFAULT_FAIL_INT;
    if (arr != null && arr.length > 0) {
      val = Arrays.stream(arr).max().getAsInt();
    }
    return val;
  }

  public static HashMap<double[], double[]> sort(double[] mainArray, List<double[]> allArrays) {

    //Later get sorted by reference to unsorted
    HashMap<double[], double[]> refMap = new HashMap<>();

    // Sort indices based on values in main arr
    int n = mainArray.length;
    Integer[] indices = new Integer[n];
    for (int i = 0; i < n; i++) {
      indices[i] = i;
    }
    Arrays.sort(indices, Comparator.comparingDouble(i -> mainArray[i]));

    allArrays.forEach(arr -> {
      double[] sortedArr = new double[n];
      refMap.put(arr, sortedArr);
    });

    for (int i = 0; i < n; i++) {
      int idx = indices[i];

      for (int j = 0; j < allArrays.size(); j++) {
        double[] arr = allArrays.get(j);
        refMap.get(arr)[i] = arr[idx];
      }
    }

    return refMap;
  }

  public static boolean isSorted(double[] arr) {
    for (int i = 1; i < arr.length; i++) {
      if (arr[i] < arr[i - 1]) {
        return false; // Found a pair out of order
      }
    }
    return true; // No issues found
  }


  /*
  ----------------------------------------------------------------------------------------
  PRIMITIVE ARRAY TO LIST
  ----------------------------------------------------------------------------------------
  */
  public static List<Double> arrToList(double[] primitiveArray) {
    List<Double> list = new ArrayList<>(primitiveArray.length);
    for (Double db : primitiveArray) {
      list.add(db);
    }
    return list;
  }

  public static List<Integer> arrToList(int[] primitiveArray) {
    List<Integer> list = new ArrayList<>(primitiveArray.length);
    for (Integer i : primitiveArray) {
      list.add(i);
    }
    return list;
  }

  public static double[] intArrToDoubleArr(int[] values) {
    double[] doubles = new double[values.length];
    for (int i = 0; i < values.length; i++) {
      doubles[i] = values[i];
    }
    return doubles;
  }


  /*
  ----------------------------------------------------------------------------------------
 PRIMITIVE ARRAY TO LIST WITH NUMBER - STRING - CONVERSION, partly also in @SnForm
 ----------------------------------------------------------------------------------------
  */

  public static List<String> toStringList(double[] arr, NF format) {
    List<String> dataList = new ArrayList<>(arr.length);
    for (Double dat : arr) {
      dataList.add(SnF.doubleToString(dat, format));
    }
    return dataList;
  }

  public static List<String> toStringList(int[] arr) {
    List<String> dataList = new ArrayList<>(arr.length);
    for (int dat : arr) {
      dataList.add(Integer.toString(dat));
    }
    return dataList;
  }

  public static List<Double> toDoubleList(String[] arr) {
    List<Double> doubles = Arrays.stream(arr)
        .map(SnF::strToDouble).collect(Collectors.toList());
    return doubles;
  }

  /*
  ----------------------------------------------------------------------------------------
  LIST TO PRIMITIVE ARRAY
  ----------------------------------------------------------------------------------------
  */

  // Note: Collections have not .get() function. Stream is probably more efficient
  // than first wrapping with a list and then for looping.
  public static double[] doubleCollToArr(Collection<Double> collection) {
    return collection.stream().mapToDouble(Double::doubleValue).toArray();
  }

  // Classic for loop should be fast than stream!
  public static double[] doubleListToArr(List<Double> list) {
    double[] array = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      array[i] = list.get(i);
    }
    return array;
  }

  public static double[] doubleListToArr(Collection<Double> coll) {
    return doubleListToArr(new ArrayList<>(coll));
  }

  public static int[] integerListToArr(List<Integer> list) {
    int[] array = new int[list.size()];
    for (int i = 0; i < list.size(); i++) {
      array[i] = list.get(i);
    }
    return array;
  }

  public static String[] stringListToArr(List<String> list) {
    return list.toArray(new String[0]);
  }

  /*
  ----------------------------------------------------------------------------------------
  SORTING OPERATIONS THAT NEED RETURN VALUE IN CONSTRUCTOR
  ----------------------------------------------------------------------------------------
   */

  /*
  ----------------------------------------------------------------------------------------
  SUMMING OPERATIONS
  ----------------------------------------------------------------------------------------
   */
  public static double doubleSum(double[] arr) {
    double result = 0;
    for (double v : arr) {
      result += v;
    }
    return result;
  }

  public static double doubleSum(List<Double> list) {
    double result = 0;
    for (double v : list) {
      result += v;
    }
    return result;
  }


  public static double intSum(int[] arr) {
    return Arrays.stream(arr).sum();
  }


  /*
  ----------------------------------------------------------------------------------------
 NONZERO-type OPERATIONS
 ----------------------------------------------------------------------------------------
  */
  public static double[] nonzero(double[] array) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (d > 0 || d < 0) {
        nzList.add(d);
      }
    }
    return doubleListToArr(nzList);
  }

  public static List<Double> nonzero(List<Double> list) {
    List<Double> nzList = new ArrayList<>();
    for (double d : list) {
      if (d > 0 || d < 0) {
        nzList.add(d);
      }
    }
    return nzList;
  }

  /**
   * @param targetValues e.g. time values
   * @param thresholds   e.g. data based on which we decide whether to include or exclude the time.
   */
  public static double[] nonzero(double[] targetValues, double[] thresholds) {
    List<Double> nzList = new ArrayList<>();

    for (int i = 0; i < thresholds.length; i++) {
      if (thresholds[i] > 0 || thresholds[i] < 0) {
        nzList.add(targetValues[i]);
      }
    }
    return doubleListToArr(nzList);
  }

  public static double[] positiveNonzero(double[] array) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (d > 0) {
        nzList.add(d);
      }
    }
    return doubleListToArr(positiveNonzeroAsList(array));
  }

  public static void replaceNegativeWithZero(double[] array) {
    for (int i = 0; i < array.length; i++) {
      if (array[i] < 0) {
        array[i] = 0;
      }
    }
  }


  public static List<Double> positiveNonzeroAsList(double[] array) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (d > 0) {
        nzList.add(d);
      }
    }
    return nzList;
  }

  /**
   *
   */
  public static boolean isZero(double[] array) {
    boolean allZero = true;
    for (double d : array) {
      if (d > 0 || d < 0) {
        allZero = false;
        break;
      }
    }
    return allZero;
  }

  public static boolean hasDuplicates(double[] arr) {
    Set<Double> seen = new HashSet<>();
    for (double value : arr) {
      // seen.add(value) -- Returns: true if this set did not already contain the specified element
      if (!seen.add(value)) {
        // found duplicate
        return true;
      }
    }
    return false;
  }

  /**
   * Note that an array that contained zeros (this can only happen for background datasets!) will
   * contain -INF after the calculation of the log10. The former solution to this was simply
   * calculating positiveNonzero to get rid of the -INF. However, this also excludes all former
   * values between 1 and 0 which would have a negative log.
   */
  public static double[] removeInf(double[] array, MathMod math) {
    double[] result;
    if (math.equals(MathMod.LOG10)) {
      result = strictlyGreaterThan(array, Double.NEGATIVE_INFINITY);
    } else {
      result = array;
    }
    return result;
  }


  public static double[] positive(double[] array) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (d >= 0) {
        nzList.add(d);
      }
    }
    return doubleListToArr(nzList);
  }

  public static List<Double> positiveNonzero(Collection<Double> list) {
    List<Double> nzList = new ArrayList<>();
    for (Double d : list) {
      if (d > 0) {
        nzList.add(d);
      }
    }
    return nzList;
  }

  public static double[] strictlyGreaterThan(double[] array, double smallest) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (d > smallest) {
        nzList.add(d);
      }
    }
    return doubleListToArr(nzList);
  }

  public static double[] filterInclusively(double[] array, double min, double max) {
    List<Double> nzList = new ArrayList<>();
    for (double d : array) {
      if (min <= d && d <= max) {
        nzList.add(d);
      }
    }
    return doubleListToArr(nzList);
  }





  /*
  ----------------------------------------------------------------------------------------
  LIST STUFF
  ----------------------------------------------------------------------------------------
   */

  /**
   * Avoids SingletonList, as these cannot grow (add() throws UnsupportedOperationException
   */
  public static <T> List<T> wrap(T t) {
    List<T> list = new ArrayList<>();
    list.add(t);
    return list;
  }


  public static <T> List<T> makeUnique(List<T> list) {
    return new ArrayList<>(new LinkedHashSet<>(list));
  }

  /*
  ----------------------------------------------------------------------------------------
  GET PART OF AN ARRAY WITH OUT OF BOUNDS ALTERNATIVE VALUE
  ----------------------------------------------------------------------------------------
  */

  public static double[] removeFirst(double[] data) {
    double[] cutData = new double[0];
    if (data.length > 1) {
      cutData = new double[data.length - 1];
      // intelliJ suggested this instead of for loop
      System.arraycopy(data, 1, cutData, 0, data.length - 1);
    }
    return cutData;
  }

  public static double[] removeLast(double[] data) {
    double[] cutData = new double[0];
    if (data.length > 1) {
      cutData = new double[data.length - 1];
      // intelliJ suggested this instead of for loop
      System.arraycopy(data, 0, cutData, 0, data.length - 1);
    }
    return cutData;
  }

  public static double[] getPortion(double[] arr, int start, int end, Logger logger) {
    if (0 <= start && start <= end && end < arr.length) {
      // note that Arrays.copy... defines "to" = "endIdx" exclusive (therefore endIdx+1)
      return Arrays.copyOfRange(arr, start, end + 1);
    } else {
      // return array with all entries -1 and notify logger
      logger.error("getSlice: requested start or end index out of bounds.");
      int dataLength = end - start + 1; //e.g. from 5-7 {5 6 7} = 7-5+1 = 3
      double[] data = new double[dataLength];
      // Arrays.fill(data, DEFAULT_FAIL_DOUBLE);
      return data;
    }
  }

  public static int[] getPortion(int[] arr, int start, int end, Logger logger) {
    if (0 <= start && start <= end && end < arr.length) {
      // note that Arrays.copy... defines "to" = "endIdx" exclusive (therefore endIdx+1)
      return Arrays.copyOfRange(arr, start, end + 1);
    } else {
      // return array with all entries -1 and notify logger
      logger.error("getSlice: requested start or end index out of bounds.");
      int dataLength = end - start + 1; //e.g. from 5-7 {5 6 7} = 7-5+1 = 3
      int[] data = new int[dataLength];
      // Arrays.fill(data, DEFAULT_FAIL_INT);
      return data;
    }
  }

  /*
  ----------------------------------------------------------------------------------------
  CONCATENATE ARRAYS OR LISTS
  ----------------------------------------------------------------------------------------
   */

  // probably fastest due to processor level workings of System.copy
  // https://www.baeldung.com/java-concatenate-arrays
  public static <T> T concatWithCopy(T array1, T array2) {
    if (!array1.getClass().isArray() || !array2.getClass().isArray()) {
      throw new IllegalArgumentException("Only arrays are accepted.");
    }

    Class<?> compType1 = array1.getClass().getComponentType();
    Class<?> compType2 = array2.getClass().getComponentType();

    if (!compType1.equals(compType2)) {
      throw new IllegalArgumentException("Two arrays have different types.");
    }

    int len1 = Array.getLength(array1);
    int len2 = Array.getLength(array2);

    @SuppressWarnings("unchecked")
    //the cast is safe due to the previous checks
    T result = (T) Array.newInstance(compType1, len1 + len2);

    System.arraycopy(array1, 0, result, 0, len1);
    System.arraycopy(array2, 0, result, len1, len2);

    return result;
  }


  /*
  Dismissed idea of creating a "make one matrix array from lists because:
  In Summary, arrays and generics have very different type rules. Arrays are covariant and reified;
  generics are invariant and erased. As a consequence, arrays provide runtime type safety
  but not compile-time type safety and vice versa for generics. Generally speaking,
  arrays and generics don’t mix well. If you find yourself mixing them and getting compile-time
  error or warnings, your first impulse should be to replace the arrays with lists.
  I think: many times a check/calculation is needed anyway. Use that to generate a primitve array
  and concatenate these.
   */

  /*
  Also read: https://www.baeldung.com/java-avoid-null-check
   */

  // Read this and build proper Array concatenating class :)
  //https://stackoverflow.com/questions/21179964/performance-of-copying-merging-arrays-in-java
  public static double[] concat(double[] firstArray, double[]... arrays) {
    for (double[] arr : arrays) {
      // addAll clones or initializes as new array --> no constructor call necessary here
      firstArray = org.apache.commons.lang3.ArrayUtils.addAll(firstArray, arr);
    }
    return firstArray;
  }

  public static List<Double> concat(List<Double> firstList, List<Double>... lists) {
    List<Double> fullList = new ArrayList<>(firstList);
    for (List<Double> list : lists) {
      fullList.addAll(list);
    }
    return fullList;
  }

  // Modified from chatGPT!
  public static double[] merge(List<double[]> list) {

    double[] mergedArray;
    if (list.isEmpty()) {
      mergedArray = new double[0];
    } else if (list.size() == 1) {
      mergedArray = list.get(0);
    } else {

      // Calculate total length of the merged array
      int totalLength = 0;
      for (double[] array : list) {
        totalLength += array.length;
      }

      // Create the merged array
      mergedArray = new double[totalLength];

      // Copy elements from each sub-array
      int currentIndex = 0;
      for (double[] array : list) {
        System.arraycopy(array, 0, mergedArray, currentIndex, array.length);
        currentIndex += array.length;
      }
    }
    return mergedArray;
  }

  public static double[] merge(double[]... arrays) {
    List<double[]> list = new ArrayList<>(Arrays.asList(arrays));
    return merge(list);
  }


  /*
  ----------------------------------------------------------------------------------------
  MATRIX CONCATENATION
  ----------------------------------------------------------------------------------------
   */

  public static double[][] doubleArrListToArr(List<double[]> list) {
    // fail safe but maybe slow?
    int maxSize = 0;
    for (double[] d : list) {
      maxSize = Math.max(maxSize, d.length);
    }

    double[][] arr = new double[list.size()][maxSize];
    for (int i = 0; i < list.size(); i++) {
      arr[i] = list.get(i);
    }
    return arr;
  }


    /*
  ----------------------------------------------------------------------------------------
  Split an Array
  ----------------------------------------------------------------------------------------
   */

  //  Modified from ChatGPT
  public static List<double[]> splitArray(double[] input, int subArrSize, int minSize) {
    List<double[]> result = new ArrayList<>();
    int inputArrLength = input.length;

    if (inputArrLength > 0) {
      int i = 0;

      // Fill a List with copies from the original data
      while (i + subArrSize <= inputArrLength) {
        result.add(Arrays.copyOfRange(input, i, i + subArrSize));
        i += subArrSize;
      }

      // Handle the remainder if the we did not end up exactly at inputArrayLength:
      if (i < inputArrLength) {
        // Get the last chunk, what remains
        double[] lastChunk = Arrays.copyOfRange(input, i, inputArrLength);

        // If this last chunk is smaller than the minimum size, merge it with the penultimate
        if (!result.isEmpty() && lastChunk.length < minSize) {
          // Merge with the previous chunk
          double[] prevChunk = result.remove(result.size() - 1);

          // merge the existing chunk and the remainder into one and append to the list
          double[] merged = new double[prevChunk.length + lastChunk.length];
          System.arraycopy(prevChunk, 0, merged, 0, prevChunk.length);
          System.arraycopy(lastChunk, 0, merged, prevChunk.length, lastChunk.length);
          result.add(merged);
        } else {
          result.add(lastChunk);
        }
      }
    }
    return result;
  }


  // modified chatGPT
  public static double[] trim(double[] data, int maxLen) {
    double[] result = new double[0];

    if (data != null) {

      // If no trimming is needed, return a copy of the original array
      if (data.length <= maxLen) {
        double[] copy = new double[data.length];
        System.arraycopy(data, 0, copy, 0, data.length);
        result = copy;
      } else {

        // Otherwise, trim to maxLen
        double[] trimmedArray = new double[maxLen];
        System.arraycopy(data, 0, trimmedArray, 0, maxLen);
        result = trimmedArray;
      }
    }
    return result;
  }

  public static List<Double> trim(List<Double> data, int maxLen) {
    List<Double> result = new ArrayList<>();

    if (data != null) {

      // If no trimming is needed, return a copy of the original list
      if (data.size() <= maxLen) {
        result = new ArrayList<>(data);
      } else {

        // Otherwise, trim to maxLen
        result = new ArrayList<>(maxLen);
        result.addAll(data.subList(0, maxLen));
      }
    }

    return result;
  }


  /*
  ----------------------------------------------------------------------------------------
  FILL ARRAYS WITH NUMBERS OR SERIES
  ----------------------------------------------------------------------------------------
   */

  /**
   * @param firstArray            first array to be concatenated
   * @param secondArray           second array to be concatenated
   * @param lengthOfFirstRefFrame Length of the firstArray's reference frame (i.e. the TISeries
   *                              length). Reason: firstArray may only contain a fraction of indices
   *                              from the TISeries. Thus, in order to keep the second series offset
   *                              right, one needs to pass the first arrays' frame's total length.
   * @return
   */
  public static int[] continueIndices(int[] firstArray, int lengthOfFirstRefFrame,
                                      int[] secondArray) {
    int[] resultArr = firstArray;
    if (secondArray.length > 0) {
      int[] adjustedSecondArray = new int[secondArray.length];
      for (int i = 0; i < secondArray.length; i++) {
        adjustedSecondArray[i] = secondArray[i] + lengthOfFirstRefFrame;
      }
      resultArr = org.apache.commons.lang3.ArrayUtils.addAll(firstArray, adjustedSecondArray);
    }
    return resultArr;
  }

  public static double[] incrementArray(int n) {
    double[] array = new double[n];
    for (int i = 0; i < n; i++) {
      array[i] = i + 1;  // Filling the array with numbers from 1 to N
    }
    return array;
  }

  public static double[] incrementArrayInclusive(double inclusiveVal, int size) {
    double[] array = new double[size];
    if (size > 0) {
      array[0] = inclusiveVal;
      for (int i = 1; i < size; i++) {
        array[i] = array[i - 1] + inclusiveVal;  // Filling the array with numbers from 1 to N
      }
    }
    return array;
  }

  public static double[] fillArrayInclusive(double start, double end, double stepSize) {
    int length = (int) Math.ceil((end - start + 1.0) / stepSize);
    double[] result = new double[length];

    for (int i = 0; i < length; i++) {
      result[i] = start + i * stepSize;
    }

    return result;
  }

  public static double[] fillArray(double val, int n) {
    double[] arr = new double[n];
    Arrays.fill(arr, val);
    return arr;
  }

  public static double[] fillArrayExclusive(double exclusiveStart, double end, double stepsize) {
    // WARNING: DON'T use "value += stepsize;" as there are floating imprecision that add up this way!
    // WARNING2: https://stackoverflow.com/questions/37335/how-to-deal-with-java-lang-outofmemoryerror-java-heap-space-error
    int required = (int) Math.ceil((end - exclusiveStart) / stepsize);

    double[] array = new double[required];
    for (int i = 0; i < required; i++) {
      array[i] = exclusiveStart + (i + 1) * stepsize;
    }
    return array;
  }


  // Python like linspace
  public static double[] linspace(double start, double endInclusive, int steps) {
    double[] result = new double[steps];
    double step = (endInclusive - start) / (steps - 1);  // ensure inclusive end
    for (int i = 0; i < steps; i++) {
      result[i] = start + i * step;
    }
    return result;
  }


  public static double[] extendForRegression(double[] values, double ratio) {
    double[] result = new double[0];
    double valueWidth = 0;
    double[] sortedValues = ArrUtils.copy(values);
    Arrays.sort(sortedValues);

    if (sortedValues.length > 1) {
      valueWidth = sortedValues[sortedValues.length - 1] - sortedValues[0];
      valueWidth = valueWidth * ratio;
      // new array with two extra spaces
      result = new double[2];
      result[0] = sortedValues[0] - valueWidth;
      result[1] = sortedValues[sortedValues.length - 1] + valueWidth;
    } else if (sortedValues.length == 1) {
      valueWidth = sortedValues[0];
      valueWidth = valueWidth * ratio;
      // new array with two extra spaces
      result = new double[2];
      result[0] = sortedValues[0] - valueWidth;
      result[1] = sortedValues[sortedValues.length - 1] + valueWidth;
    }
    return result;
  }

  public static double[] exclude(double[] excludeFromThisArray,
                                 double[] thrArray, double thr, Comparators comparator) {
    List<Double> trimmed = new ArrayList<>();
    for (int i = 0; i < thrArray.length; i++) {
      if (!comparator.is(thrArray[i], thr)) {
        trimmed.add(excludeFromThisArray[i]);
      }
    }
    return ArrUtils.doubleListToArr(trimmed);
  }


  /*
  ----------------------------------------------------------------------------------------
  Binary search
  ----------------------------------------------------------------------------------------
   */
  public static int findClosestIndex(double[] sorted, double value) {
    int lo = 0;
    int hi = sorted.length - 1;

    if (value <= sorted[lo]) {
      return lo;
    }

    if (value >= sorted[hi]) {
      return hi;
    }

    // Go binary search
    int index = Arrays.binarySearch(sorted, value);

    if (index >= 0) {
      // Exact match found
      return index;
    } else {
      // No exact match; compute insertion point
      int insertionPoint = -index - 1;

      // I think redundant but maybe keep it here to be sure...
      if (insertionPoint == 0) {
        return 0;
      }
      if (insertionPoint == sorted.length) {
        return sorted.length - 1;
      }

      /*
       1) no index check needed as before we made sure we are not at the edges
       2) Why check at insertionPoint-1 and insertionPoint???
        --> GPT: "If value were inserted into the array to keep it sorted, at which index would it go?"
            So if we look at the situation around insertionPoint, the two nearest candidates for "closest
            value" are:
            - sorted[insertionPoint - 1] (the last value smaller than value)
            - sorted[insertionPoint] (the first value greater than value)
       */

      double lowerVal = sorted[insertionPoint - 1];
      double upperVal = sorted[insertionPoint];

      double diffLower = Math.abs(lowerVal - value);
      double diffUpper = Math.abs(upperVal - value);

      if (diffLower <= diffUpper) {
        return insertionPoint - 1;
      } else {
        return insertionPoint;
      }
    }
  }

  public static Pair<Integer, Integer> findBoundingIndices(double[] sorted, double value) {
    int index = Arrays.binarySearch(sorted, value);

    if (index >= 0) {
      // Exact match: return index and itself (degenerate case)
      return new Pair<>(index, index);
    }

    int insertionPoint = -index - 1;

    if (insertionPoint <= 0) {
      // Value is smaller than the first element
      return new Pair<>(0, 1);
    } else if (insertionPoint >= sorted.length) {
      // Value is larger than the last element
      return new Pair<>(sorted.length - 2, sorted.length - 1);
    } else {
      // Value lies between insertionPoint - 1 and insertionPoint
      return new Pair<>(insertionPoint - 1, insertionPoint);
    }
  }


}
