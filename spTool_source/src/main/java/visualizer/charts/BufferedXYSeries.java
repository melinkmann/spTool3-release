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

package visualizer.charts;

import dataModelNew.TISeries;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.general.SeriesChangeEvent;
import org.jfree.data.general.SeriesException;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYSeries;
import util.ArrUtils;

/**
 * Note. For every zoom action, the plot accesses this class via line 446 get() where it gets the
 * data point by point. This is super inefficient. Maybe we can optimize this later?
 */

/**
 * Represents a sequence of zero or more data items in the form (x, y).  By default, items in the
 * series will be sorted into ascending order by x-value, and duplicate x-values are permitted. Both
 * the sorting and duplicate defaults can be changed in the constructor.  Y-values can be {@code
 * null} to represent missing values.
 */
public class BufferedXYSeries extends XYSeries implements Cloneable, Serializable {

  private static final Logger LOGGER = LogManager.getLogger(BufferedXYSeries.class.getName());

  /**
   * For serialization.
   */
  static final long serialVersionUID = -5908509288197150436L;

  /**
   * Storage for the data items in the series.
   */
  private final TISeries tiSeries;
  private double[] xArr;
  private double[] yArr;
  private int counter = 0;

  /**
   * The maximum number of items for the series.
   */
  private int maximumItemCount = Integer.MAX_VALUE;

  /**
   * The lowest x-value in the series, excluding Double.NaN values.
   */
  private double minX;

  /**
   * The highest x-value in the series, excluding Double.NaN values.
   */
  private double maxX;

  /**
   * The lowest y-value in the series, excluding Double.NaN values.
   */
  private double minY;

  /**
   * The highest y-value in the series, excluding Double.NaN values.
   */
  private double maxY;

  public BufferedXYSeries(Comparable key, TISeries tiSeries) {
    super(key);
    this.tiSeries = tiSeries;
    if (tiSeries.size() > 1) {
      double[] x = tiSeries.getX();
      double[] y = tiSeries.getY();
      this.minX = x[0];
      this.maxX = x[x.length - 1];
      this.minY = y[0];
      this.maxY = y[y.length - 1];
    } else {
      this.minX = Double.NaN;
      this.maxX = Double.NaN;
      this.minY = Double.NaN;
      this.maxY = Double.NaN;
    }
    this.xArr = tiSeries.getX();
    this.yArr = tiSeries.getY();
  }

  /**
   * Returns the smallest x-value in the series, ignoring any Double.NaN values.  This method
   * returns Double.NaN if there is no smallest x-value (for example, when the series is empty).
   *
   * @return The smallest x-value.
   * @see #getMaxX()
   */
  public double getMinX() {
    return this.minX;
  }

  /**
   * Returns the largest x-value in the series, ignoring any Double.NaN values.  This method returns
   * Double.NaN if there is no largest x-value (for example, when the series is empty).
   *
   * @return The largest x-value.
   * @see #getMinX()
   */
  public double getMaxX() {
    return this.maxX;
  }

  /**
   * Returns the smallest y-value in the series, ignoring any null and Double.NaN values.  This
   * method returns Double.NaN if there is no smallest y-value (for example, when the series is
   * empty).
   *
   * @return The smallest y-value.
   * @see #getMaxY()
   */
  public double getMinY() {
    return this.minY;
  }

  /**
   * Returns the largest y-value in the series, ignoring any Double.NaN values.  This method returns
   * Double.NaN if there is no largest y-value (for example, when the series is empty).
   *
   * @return The largest y-value.
   * @see #getMinY()
   */
  public double getMaxY() {
    return this.maxY;
  }

  /**
   * Updates the cached values for the minimum and maximum data values.
   *
   * @param item the item added ({@code null} not permitted).
   */
  private void updateBoundsForAddedItem(XYDataItem item) {
    double x = item.getXValue();
    this.minX = minIgnoreNaN(this.minX, x);
    this.maxX = maxIgnoreNaN(this.maxX, x);
    if (item.getY() != null) {
      double y = item.getYValue();
      this.minY = minIgnoreNaN(this.minY, y);
      this.maxY = maxIgnoreNaN(this.maxY, y);
    }
  }

  /**
   * Updates the cached values for the minimum and maximum data values on the basis that the
   * specified item has just been removed.
   *
   * @param item the item added ({@code null} not permitted).
   */
  private void updateBoundsForRemovedItem(XYDataItem item) {
    boolean itemContributesToXBounds = false;
    boolean itemContributesToYBounds = false;
    double x = item.getXValue();
    if (!Double.isNaN(x)) {
      if (x <= this.minX || x >= this.maxX) {
        itemContributesToXBounds = true;
      }
    }
    if (item.getY() != null) {
      double y = item.getYValue();
      if (!Double.isNaN(y)) {
        if (y <= this.minY || y >= this.maxY) {
          itemContributesToYBounds = true;
        }
      }
    }
    if (itemContributesToYBounds) {
      findBoundsByIteration();
    } else if (itemContributesToXBounds) {
      if (getAutoSort()) {
        this.minX = getX(0).doubleValue();
        this.maxX = getX(getItemCount() - 1).doubleValue();
      } else {
        findBoundsByIteration();
      }
    }
  }

  /**
   * Finds the bounds of the x and y values for the series, by iterating through all the data
   * items.
   */
  private void findBoundsByIteration() {
    // bounds are set in constructor and do not change
  }

  /**
   * Returns the flag that controls whether the items in the series are automatically sorted.  There
   * is no setter for this flag, it must be defined in the series constructor.
   *
   * @return A boolean.
   */
  public boolean getAutoSort() {
    return true;
  }

  /**
   * Returns a flag that controls whether duplicate x-values are allowed. This flag can only be set
   * in the constructor.
   *
   * @return A boolean.
   */
  public boolean getAllowDuplicateXValues() {
    return false;
  }

  /**
   * Returns the number of items in the series.
   *
   * @return The item count.
   */
  @Override
  public int getItemCount() {
    return this.tiSeries.size();
  }

  /**
   * Returns the maximum number of items that will be retained in the series. The default value is
   * {@code Integer.MAX_VALUE}.
   *
   * @return The maximum item count.
   * @see #setMaximumItemCount(int)
   */
  public int getMaximumItemCount() {
    return this.maximumItemCount;
  }

  /**
   * Sets the maximum number of items that will be retained in the series. If you add a new item to
   * the series such that the number of items will exceed the maximum item count, then the first
   * element in the series is automatically removed, ensuring that the maximum item count is not
   * exceeded.
   * <p>
   * Typically this value is set before the series is populated with data, but if it is applied
   * later, it may cause some items to be removed from the series (in which case a {@link
   * SeriesChangeEvent} will be sent to all registered listeners).
   *
   * @param maximum the maximum number of items for the series.
   */
  public void setMaximumItemCount(int maximum) {
    // do nothing
  }

  /**
   * Adds a data item to the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param item the (x, y) item ({@code null} not permitted).
   */
  public void add(XYDataItem item) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds a data item to the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param x the x value.
   * @param y the y value.
   */
  public void add(double x, double y) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds a data item to the series and, if requested, sends a {@link SeriesChangeEvent} to all
   * registered listeners.
   *
   * @param x      the x value.
   * @param y      the y value.
   * @param notify a flag that controls whether or not a {@link SeriesChangeEvent} is sent to all
   *               registered listeners.
   */
  public void add(double x, double y, boolean notify) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds a data item to the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.  The unusual pairing of parameter types is to make it easier to add {@code null}
   * y-values.
   *
   * @param x the x value.
   * @param y the y value ({@code null} permitted).
   */
  public void add(double x, Number y) {
    // we do not allow this for this static type of plot!

  }

  /**
   * Adds a data item to the series and, if requested, sends a {@link SeriesChangeEvent} to all
   * registered listeners.  The unusual pairing of parameter types is to make it easier to add null
   * y-values.
   *
   * @param x      the x value.
   * @param y      the y value ({@code null} permitted).
   * @param notify a flag that controls whether or not a {@link SeriesChangeEvent} is sent to all
   *               registered listeners.
   */
  public void add(double x, Number y, boolean notify) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds a new data item to the series (in the correct position if the {@code autoSort} flag is set
   * for the series) and sends a {@link SeriesChangeEvent} to all registered listeners.
   * <p>
   * Throws an exception if the x-value is a duplicate AND the allowDuplicateXValues flag is false.
   *
   * @param x the x-value ({@code null} not permitted).
   * @param y the y-value ({@code null} permitted).
   * @throws SeriesException if the x-value is a duplicate and the {@code allowDuplicateXValues}
   *                         flag is not set for this series.
   */
  public void add(Number x, Number y) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds new data to the series and, if requested, sends a {@link SeriesChangeEvent} to all
   * registered listeners.
   * <p>
   * Throws an exception if the x-value is a duplicate AND the allowDuplicateXValues flag is false.
   *
   * @param x      the x-value ({@code null} not permitted).
   * @param y      the y-value ({@code null} permitted).
   * @param notify a flag the controls whether or not a {@link SeriesChangeEvent} is sent to all
   *               registered listeners.
   */
  public void add(Number x, Number y, boolean notify) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds a data item to the series and, if requested, sends a {@link SeriesChangeEvent} to all
   * registered listeners.
   *
   * @param item   the (x, y) item ({@code null} not permitted).
   * @param notify a flag that controls whether or not a {@link SeriesChangeEvent} is sent to all
   *               registered listeners.
   */
  public void add(XYDataItem item, boolean notify) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Deletes a range of items from the series and sends a {@link SeriesChangeEvent} to all
   * registered listeners.
   *
   * @param start the start index (zero-based).
   * @param end   the end index (zero-based).
   */
  public void delete(int start, int end) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Removes the item at the specified index and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param index the index.
   * @return The item removed.
   */
  public XYDataItem remove(int index) {
    // we do not allow this for this static type of plot!
    return null;
  }

  /**
   * Removes an item with the specified x-value and sends a {@link SeriesChangeEvent} to all
   * registered listeners.  Note that when a series permits multiple items with the same x-value,
   * this method could remove any one of the items with that x-value.
   *
   * @param x the x-value.
   * @return The item removed.
   */
  public XYDataItem remove(Number x) {
    // we do not allow this for this static type of plot!
    return null;
  }

  /**
   * Removes all data items from the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   */
  public void clear() {
    // we do not allow this for this static type of plot!
  }

  /**
   * Return the data item with the specified index.
   *
   * @param index the index.
   * @return The data item with the specified index.
   */
  public XYDataItem getDataItem(int index) {

    if (xArr == null) {
      xArr = tiSeries.getX();
    }

    if (yArr == null) {
      yArr = tiSeries.getY();
    }

    XYDataItem item = new XYDataItem(xArr[index], yArr[index]);

    // clear ram
    if (index == tiSeries.size() - 1) {
      counter++;
      // x and y were accessed
      if (counter > 0) {
        xArr = null;
        yArr = null;
        counter = 0;
        // LOGGER.trace("Cleared buffer due to final index in: " + super.getKey() + ".");
      }
    }

    return (XYDataItem) item.clone();
  }

  /**
   * Return the data item with the specified index.
   *
   * @param index the index.
   * @return The data item with the specified index.
   */
  XYDataItem getRawDataItem(int index) {
    if (xArr == null) {
      xArr = tiSeries.getX();
    }

    if (yArr == null) {
      yArr = tiSeries.getY();
    }

    XYDataItem item = new XYDataItem(xArr[index], yArr[index]);

    // clear ram
    if (index == tiSeries.size() - 1) {
      counter++;
      // x and y were accessed
      if (counter > 0) {
        xArr = null;
        yArr = null;
        counter = 0;
        // LOGGER.trace("Cleared buffer due to final index in: " + super.getKey() + ".");
      }
    }

    return (XYDataItem) item.clone();
  }

  /**
   * Returns the x-value at the specified index.
   *
   * @param index the index (zero-based).
   * @return The x-value (never {@code null}).
   */
  public Number getX(int index) {
    return getRawDataItem(index).getX();
  }

  /**
   * Returns the y-value at the specified index.
   *
   * @param index the index (zero-based).
   * @return The y-value (possibly {@code null}).
   */
  public Number getY(int index) {
    return getRawDataItem(index).getY();
  }

  /**
   * A function to find the minimum of two values, but ignoring any Double.NaN values.
   *
   * @param a the first value.
   * @param b the second value.
   * @return The minimum of the two values.
   */
  private double minIgnoreNaN(double a, double b) {
    if (Double.isNaN(a)) {
      return b;
    }
    if (Double.isNaN(b)) {
      return a;
    }
    return Math.min(a, b);
  }

  /**
   * A function to find the maximum of two values, but ignoring any Double.NaN values.
   *
   * @param a the first value.
   * @param b the second value.
   * @return The maximum of the two values.
   */
  private double maxIgnoreNaN(double a, double b) {
    if (Double.isNaN(a)) {
      return b;
    }
    if (Double.isNaN(b)) {
      return a;
    }
    return Math.max(a, b);
  }

  /**
   * Updates the value of an item in the series and sends a {@link SeriesChangeEvent} to all
   * registered listeners.
   *
   * @param index the item (zero based index).
   * @param y     the new value ({@code null} permitted).
   */
  public void updateByIndex(int index, Number y) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Updates an item in the series.
   *
   * @param x the x-value ({@code null} not permitted).
   * @param y the y-value ({@code null} permitted).
   * @throws SeriesException if there is no existing item with the specified x-value.
   */
  public void update(Number x, Number y) {
    // we do not allow this for this static type of plot!
  }

  /**
   * Adds or updates an item in the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param x the x-value.
   * @param y the y-value.
   * @return The item that was overwritten, if any.
   */
  public XYDataItem addOrUpdate(double x, double y) {
    // we do not allow this for this static type of plot!
    return null;
  }

  /**
   * Adds or updates an item in the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param x the x-value ({@code null} not permitted).
   * @param y the y-value ({@code null} permitted).
   * @return A copy of the overwritten data item, or {@code null} if no item was overwritten.
   */
  public XYDataItem addOrUpdate(Number x, Number y) {
    // we do not allow this for this static type of plot!
    return null;
  }

  /**
   * Adds or updates an item in the series and sends a {@link SeriesChangeEvent} to all registered
   * listeners.
   *
   * @param item the data item ({@code null} not permitted).
   * @return A copy of the overwritten data item, or {@code null} if no item was overwritten.
   */
  public XYDataItem addOrUpdate(XYDataItem item) {
    // we do not allow this for this static type of plot!
    return null;
  }

  /**
   * Returns the index of the item with the specified x-value, or a negative index if the series
   * does not contain an item with that x-value.  Be aware that for an unsorted series, the index is
   * found by iterating through all items in the series.
   *
   * @param x the x-value ({@code null} not permitted).
   * @return The index.
   */
  public int indexOf(Number x) {
    List<Double> xData = ArrUtils.arrToList(tiSeries.getX());
    return Collections.binarySearch(xData, x.doubleValue());
  }

  /**
   * Returns a new array containing the x and y values from this series.
   *
   * @return A new array containing the x and y values from this series.
   */
  public double[][] toArray() {
    if (xArr == null) {
      xArr = tiSeries.getX();
    }

    if (yArr == null) {
      yArr = tiSeries.getY();
    }

    double[][] result = new double[2][tiSeries.size()];

    System.arraycopy(xArr, 0, result[0], 0, xArr.length);
    System.arraycopy(yArr, 0, result[1], 0, yArr.length);

    // clear ram
    xArr = null;
    yArr = null;
    //LOGGER.trace("Cleared buffer due to final index in: " + super.getKey() + ".");
    return result;
  }

  /**
   * Returns a clone of the series.
   *
   * @return A clone of the series.
   * @throws CloneNotSupportedException if there is a cloning problem.
   */
  @Override
  public Object clone() throws CloneNotSupportedException {
    Object clone = super.clone();
    // Does this work?
    return clone;
  }

  /**
   * Creates a new series by copying a subset of the data in this time series.
   *
   * @param start the index of the first item to copy.
   * @param end   the index of the last item to copy.
   * @return A series containing a copy of this series from start until end.
   * @throws CloneNotSupportedException if there is a cloning problem.
   */
  public BufferedXYSeries createCopy(int start, int end)
      throws CloneNotSupportedException {
//    return new BufferedXYSeries(super.getKey(), tiSeries);
    return null;
  }

  /**
   * Tests this series for equality with an arbitrary object.
   *
   * @param obj the object to test against for equality ({@code null} permitted).
   * @return A boolean.
   */
  @Override
  public boolean equals(Object obj) {
    boolean eq = false;
    if (obj instanceof BufferedXYSeries) {
      if (((BufferedXYSeries) obj).tiSeries.equals(this.tiSeries)) {
        eq = true;
      }
    }
    return eq;
  }

  /**
   * Returns a hash code.
   *
   * @return A hash code.
   */
  @Override
  public int hashCode() {
    int result = super.hashCode();
    // it is too slow to look at every data item, so let's just look at
    // the first, middle and last items...
    result = 29 * tiSeries.hashCode();
    result = 29 * result + this.maximumItemCount;
    result = 29 * result + 1;
    result = 29 * result + 1;
    return result;
  }


}
