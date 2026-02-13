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

package math.stat;

// https://stackoverflow.com/questions/11955728/how-to-calculate-the-median-of-an-array
// cf https://rosettacode.org/wiki/Quickselect_algorithm#Java

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import smile.math.MathEx;
import util.ArrUtils;

public class Median {

  private static final Logger LOGGER = LogManager.getLogger(Median.class);

  public static double mad(double[] values, double median) {
    double mad = 0;
    if (values.length > 0) {
      double[] absDistFromMedian = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        absDistFromMedian[i] = Math.abs(values[i] - median);
      }
      mad = median(absDistFromMedian);
    }
    return mad;
  }

  public static double mad(double[] values) {
    double mad = 0;
    if (values.length > 0) {
      double median = median(values);
      double[] absDistFromMedian = new double[values.length];
      for (int i = 0; i < values.length; i++) {
        absDistFromMedian[i] = Math.abs(values[i] - median);
      }
      mad = median(absDistFromMedian);
    }
    return mad;
  }


  public static double mad(Collection<Double> values, double median) {
    double mad = 0;
    if (values.size() > 0) {
      List<Double> valueList = new ArrayList<>(values);
      double[] absDistFromMedian = new double[values.size()];
      for (int i = 0; i < values.size(); i++) {
        absDistFromMedian[i] = Math.abs(valueList.get(i) - median);
      }
      mad = median(absDistFromMedian);
    }
    return mad;
  }

  public static double mad(Collection<Double> values) {
    double mad = 0;
    if (values.size() > 0) {
      List<Double> valuesList = new ArrayList<>(values);
      double median = median(valuesList);

      double[] absDistFromMedian = new double[valuesList.size()];
      for (int i = 0; i < valuesList.size(); i++) {
        absDistFromMedian[i] = Math.abs(valuesList.get(i) - median);
      }
      mad = median(absDistFromMedian);
    }
    return mad;
  }


  public static double median(double[] data) {
    double median;
    if (data.length > 10_000) {
      // The smiles library uses QuickSelect and alters the array, thus copy!
      data = ArrUtils.copy(data);
      median = MathEx.median(data);
    } else {
      DescriptiveStatistics ds = new DescriptiveStatistics(data);
      median = ds.getPercentile(50);
    }
    return median;
  }

  public static double median(List<Double> data) {
    double median;
    if (data.size() > 10_000) {
      // The smiles library uses QuickSelect and alters the array but here we create new array anyway.
      median = MathEx.median(ArrUtils.doubleListToArr(data));
    } else {
      // a bit clumsy but we would have to iterate anyway to convert to an array...
      DescriptiveStatistics stats = new DescriptiveStatistics();
      data.forEach(stats::addValue);
      median = stats.getPercentile(50);
    }
    return median;
  }

//  /****************
//   * @param coll an ArrayList of Comparable objects
//   * @return the median of coll
//   * https://stackoverflow.com/questions/11955728/how-to-calculate-the-median-of-an-array
//   *****************/
//  public static <T extends Number> double median(List<T> coll, Comparator<T> comp) {
//    double result;
//
//    if (!coll.isEmpty()) {
//      // Stack Overflow was seen for either straight lines up or down.
//
//      try {
//        int n = coll.size() / 2;
//
//        if (coll.size() % 2 == 0) {
//          // even number of items; find the middle two and average them
//          result = (nth(coll, n - 1, comp).doubleValue() + nth(coll, n, comp).doubleValue()) / 2.0;
//        } else {
//          // odd number of items; return the one in the middle
//          result = nth(coll, n, comp).doubleValue();
//        }
//      } catch (Exception stackOverflowError) {
//        LOGGER.error(ExceptionUtils.getStackTrace(stackOverflowError));
//        LOGGER
//            .error("Fast median implementation failed. Switch to Apache's DescriptiveStatistics.");
//        double[] doubleValueColl = new double[coll.size()];
//        for (int i = 0; i < coll.size(); i++) {
//          doubleValueColl[i] = (double) coll.get(i);
//        }
//        DescriptiveStatistics ds = new DescriptiveStatistics(doubleValueColl);
//        result = ds.getPercentile(50);
//      }
//    } else {
//      result = 0;
//    }
//    // median(coll)
//    return result;
//  }
//
//
//  /*****************
//   * @param coll a collection of Comparable objects
//   * @param n  the position of the desired object, using the ordering defined on the list elements
//   * @return the nth smallest object
//   *******************/
//
//  public static <T> T nth(List<T> coll, int n, Comparator<T> comp) {
//    T result, pivot;
//    List<T> underPivot = new ArrayList<>(), overPivot = new ArrayList<>(), equalPivot = new ArrayList<>();
//
//    // choosing a pivot is a whole topic in itself.
//    // this implementation uses the simple strategy of grabbing something from the middle of the ArrayList.
//
//    pivot = coll.get(n / 2);
//
//    // split coll into 3 lists based on comparison with the pivot
//
//    for (T obj : coll) {
//      int order = comp.compare(obj, pivot);
//
//      if (order < 0) {
//        // obj < pivot
//        underPivot.add(obj);
//      } else if (order > 0) {
//        // obj > pivot
//        overPivot.add(obj);
//      } else {
//        // obj = pivot
//        equalPivot.add(obj);
//      }
//    } // for each obj in coll
//
//    // recurse on the appropriate list
//
//    if (n < underPivot.size()) {
//      result = nth(underPivot, n, comp);
//    } else if (n < underPivot.size() + equalPivot.size()) {
//      // equal to pivot; just return it
//      result = pivot;
//    } else {
//      // everything in underPivot and equalPivot is too small.  Adjust n accordingly in the recursion.
//      result = nth(overPivot, n - underPivot.size() - equalPivot.size(), comp);
//    }
//    // nth(coll, n)
//    return result;
//  }
//
//  /*
//   * Sorting the array is unnecessary and inefficient. There's a variation of the QuickSort
//   * (QuickSelect) algorithm which has an average run time of O(n); if you sort first, you're down to
//   * O(n log n). It actually finds the nth smallest item in a list; for a median, you just use n =
//   * half the list length. Let's call it quickNth (list, n).
//   * <p>
//   * The concept is that to find the nth smallest, choose a 'pivot' value. (Exactly how you choose it
//   * isn't critical; if you know the data will be thoroughly random, you can take the first item on
//   * the list.)
//   * <p>
//   * Split the original list into three smaller lists:
//   * <p>
//   * One with values smaller than the pivot. One with values equal to the pivot. And one with values
//   * greater than the pivot. You then have three cases:
//   * <p>
//   * 1) The "smaller" list has >= n items. In that case, you know that the nth smallest is in that list.
//   * Return quickNth(smaller, n). 2) The smaller list has < n items, but the sum of the lengths of the
//   * smaller and equal lists have >= n items. In this case, the nth is equal to any item in the
//   * "equal" list; you're done. 3) n is greater than the sum of the lengths of the smaller and equal
//   * lists. In that case, you can essentially skip over those two, and adjust n accordingly. Return
//   * quickNth(greater, n - length(smaller) - length(equal)). Done.
//   * <p>
//   * If you're not sure that the data is thoroughly random, you need to be more sophisticated about
//   * choosing the pivot. Taking the median of the first value in the list, the last value in the list,
//   * and the one midway between the two works pretty well.
//   * <p>
//   * If you're very unlucky with your choice of pivots, and you always choose the smallest or highest
//   * value as your pivot, this takes O(n^2) time; that's bad. But, it's also very unlikely if you
//   * choose your pivot with a decent algorithm.
//   */
//
//  /**
//   * https://www.geeksforgeeks.org/median-of-an-unsorted-array-in-liner-time-on/
//   */
//
//// This implementation sometimes just returns nasty stack overflows :-(   -------------------
//  public static double findMedian(int[] arr) {
//    double ans;
//    int[] a = {-1};
//    int[] b = {-1};
//    int n = arr.length;
//
//    if (n % 2 == 1) {
//      medianUtil(arr, 0, n - 1, n / 2, a, b);
//      ans = b[0];
//    } else {
//      medianUtil(arr, 0, n - 1, n / 2, a, b);
//      ans = (a[0] + b[0]) / 2.0;
//    }
//    return ans;
//  }
//
//
//  static void swap(int[] arr, int i, int j) {
//    int temp = arr[i];
//    arr[i] = arr[j];
//    arr[j] = temp;
//  }
//
//  public static int partition(int[] arr, int l, int r) {
//    int lst = arr[r], i = l, j = l;
//    while (j < r) {
//      if (arr[j] < lst) {
//        swap(arr, i, j);
//        i++;
//      }
//      j++;
//    }
//    swap(arr, i, r);
//    return i;
//  }
//
//  public static int randomPartition(int[] arr, int l, int r) {
//    Random rand = new Random();
//    int n = r - l + 1;
//    int pivot = rand.nextInt(n);
//    swap(arr, l + pivot, r);
//    return partition(arr, l, r);
//  }
//
//  public static void medianUtil(int[] arr, int l, int r, int k, int[] a, int[] b) {
//    if (l <= r) {
//      int partitionIndex = randomPartition(arr, l, r);
//      // find the median of odd number element in arr[]
//      if (partitionIndex == k) {
//        b[0] = arr[partitionIndex];
//        if (a[0] != -1) {
//          return;
//        }
//      } else if (partitionIndex == k - 1) { // a & b as middle element of arr[]
//        a[0] = arr[partitionIndex];
//        if (b[0] != -1) {
//          return;
//        }
//      }
//      // index in first half of the arr[]
//      if (partitionIndex >= k) {
//        medianUtil(arr, l, partitionIndex - 1, k, a, b);
//      }
//      // find the index in second half of the arr[]
//      else {
//        medianUtil(arr, partitionIndex + 1, r, k, a, b);
//      }
//    }
//  }
//
//// This implementation sometimes just returns nasty stack overflows :-(   -------------------
//  public static double findMedian(double[] arr) {
//    double ans;
//    double[] a = {-1};
//    double[] b = {-1};
//    int n = arr.length;
//
//    if (n % 2 == 1) {
//      medianUtil(arr, 0, n - 1, n / 2, a, b);
//      ans = b[0];
//    } else {
//      medianUtil(arr, 0, n - 1, n / 2, a, b);
//      ans = (a[0] + b[0]) / 2.0;
//    }
//    return ans;
//  }
//
//
//  static void swap(double[] arr, int i, int j) {
//    double temp = arr[i];
//    arr[i] = arr[j];
//    arr[j] = temp;
//  }
//
//  public static int partition(double[] arr, int l, int r) {
//    double lst = arr[r];
//    int i = l;
//    int j = l;
//
//    while (j < r) {
//      if (arr[j] < lst) {
//        swap(arr, i, j);
//        i++;
//      }
//      j++;
//    }
//    swap(arr, i, r);
//    return i;
//  }
//
//  public static int randomPartition(double[] arr, int l, int r) {
//    Random rand = new Random();
//    int n = r - l + 1;
//    int pivot = rand.nextInt(n);
//    swap(arr, l + pivot, r);
//    return partition(arr, l, r);
//  }
//
//  public static void medianUtil(double[] arr, int l, int r, int k, double[] a, double[] b) {
//    if (l <= r) {
//      int partitionIndex = randomPartition(arr, l, r);
//      // find the median of odd number element in arr[]
//      if (partitionIndex == k) {
//        b[0] = arr[partitionIndex];
//        if (a[0] != -1) {
//          return;
//        }
//      } else if (partitionIndex == k - 1) { // a & b as middle element of arr[]
//        a[0] = arr[partitionIndex];
//        if (b[0] != -1) {
//          return;
//        }
//      }
//      // index in first half of the arr[]
//      if (partitionIndex >= k) {
//        medianUtil(arr, l, partitionIndex - 1, k, a, b);
//      }
//      // find the index in second half of the arr[]
//      else {
//        medianUtil(arr, partitionIndex + 1, r, k, a, b);
//      }
//    }
//  }


}

