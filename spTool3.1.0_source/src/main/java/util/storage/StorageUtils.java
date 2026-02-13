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

package util.storage;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Inspired by https://github.com/mzmine/mzmine3/blob/master/src/main/java/io/github/mzmine/datamodel/featuredata/impl/StorageUtils.java
 * Util methods to assist use of the {@link MemoryMapStorage} class
 */

public abstract class StorageUtils {

  private static final Logger LOGGER = LogManager.getLogger(StorageUtils.class);

  /**
   * Stores the given array into a double buffer.
   *
   * @param storage The storage to be used. If null, the values will be wrapped using {@link
   *                DoubleBuffer#wrap(double[])}.
   * @param values  The values to be stored.
   * @return The double buffer the values were stored in.
   */
  @Nonnull
  public static DoubleBuffer storeToDoubleBuffer(@Nullable final MemoryMapStorage storage,
      @Nonnull final double[] values) {

    DoubleBuffer buffer;
    if (storage != null) {
      try {
        buffer = storage.storeData(values);
      } catch (IOException e) {
        LOGGER.warn("Could not write to HDD buffer. Data were wrapped to RAM. Error message: "
            + e.getMessage()
            + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
        buffer = DoubleBuffer.wrap(values);
      }
    } else {
      buffer = DoubleBuffer.wrap(values);
    }
    return buffer;
  }

  @Nonnull
  public static IntBuffer storeToIntBuffer(@Nullable final MemoryMapStorage storage,
      @Nonnull final int[] values) {

    IntBuffer buffer;
    if (storage != null) {
      try {
        buffer = storage.storeData(values);
      } catch (IOException e) {
        LOGGER.warn("Could not write to HDD buffer. Data were wrapped to RAM. Error message: "
            + e.getMessage()
            + ". Stack trace: " + ExceptionUtils.getStackTrace(e));
        buffer = IntBuffer.wrap(values);
      }
    } else {
      buffer = IntBuffer.wrap(values);
    }
    return buffer;
  }


  /*
  GETTERS FOR BUFFER POSSIBLY WITH (POSSIBLY) WRAPPED ARRAYS
   */

  // DoubleBuffer.array() directly accesses the public final heap buffer field --> fast!
  public static double[] getArray(DoubleBuffer values) {
    if (values.hasArray()) {
      return values.array();
    } else {
      return getBufferAsArray(values);
    }
  }

  public static int[] getArray(IntBuffer values) {
    if (values.hasArray()) {
      return values.array();
    } else {
      return getBufferAsArray(values);
    }
  }


  /*
GETTERS FOR BUFFER WITHOUT WRAPPED ARRAYS

     --> ALWAYS AVOID RELATIVE GETTERS BECAUSE SPTOOL USES MULTITHREADING
     --> DO NOT SYNCHRONIZE THESE STATIC METHODS BECAUSE THAT CAUSES CLASS LEVEL LOCK
         AND WOULD GUARANTEE DEADLOCK

        abstract double d =	get();
          Relative get method.

        DoubleBuffer buffer = get(double[] dst);
          Relative bulk get method.

        get(double[] dst, int offset, int length);
          Relative bulk get method.

        As far as I read the oracle documentation the only absolute getter:
        abstract double	get(int index)
          Absolute get method.
 */

  public static double[] getBufferAsArray(DoubleBuffer values) {
    // Use .capacity() or .limit() since MemoryMapStorage creates BufferSlices of exact length!
    int buffLength = values.capacity();
    double[] data = new double[buffLength];
    for (int i = 0; i < values.capacity(); i++) {
      data[i] = values.get(i);
    }
    return data;
    /*
    Only Allow Absolute Method (c.f. GIT history to compare with older method).
    Try-catch idea was nice but it does not guarantee that the CORRECT values are read
    because an underflow might not occur in case of a full 64 byte bit shift and so on!

    BIG NO NO:

    <<<<<<<<<<<<<<<<<<<<
    / fills the array efficiently: first set buffer to zero position, then relative getter returns entire buffer
    try {
      values.position(0);
      values.get(data, 0, buffLength);
      return data;
    } catch (BufferUnderflowException | IndexOutOfBoundsException e) {
      LOGGER.log(Level.WARNING, LoggerUtils.getMessage(e));
      // presumably less efficient but useful if something goes wrong with relative bulk getter above
      // either due to position-reset or capacity-definition.
      for (int i = 0; i < values.capacity(); i++) {
        data[i] = values.get(i);
      }
      return data;
    }
    >>>>>>>>>>>>>>>>>>
     */
  }

  public static int[] getBufferAsArray(IntBuffer values) {
    // Use .capacity() or .limit() since MemoryMapStorage creates BufferSlices of exact length!
    int buffLength = values.capacity();
    int[] data = new int[buffLength];
    for (int i = 0; i < values.capacity(); i++) {
      data[i] = values.get(i);
    }
    return data;
  }



  /*
  ### CONCERNING THREAD SAFETY:

  What happens when you use relative getters in multi-threading context?
  In SpTool2 v48C:
  Exception is provoked by importing samples,
  then click "Import as Copy"
  directly followed by the "Dev->QuickImport"
  ---> This causes a situation in which apparently the refresh of the Parameter Table from .runlater()
       kills the Buffer. A part of that is, .position(0) resets the Buffer. If it was reading
       at any point and gets interrupted, chances are that at the end there are not enough bits
       left to read, thus underflow.

  [17:06:20|WARNING|datamodel.storage.StorageUtils]:
		java.nio.BufferUnderflowException
	at java.base/java.nio.DirectDoubleBufferS.get(DirectDoubleBufferS.java:312)
	at datamodel.storage.StorageUtils.getBufferAsArray(StorageUtils.java:235)
	at datamodel.impl.tISeries.TISeriesHDD.getIntensity(TISeriesHDD.java:82)
	at datamodel.impl.tISeries.GenTISeries.getStatValues(GenTISeries.java:248)
	at gui.table.templatedTwoCol.TableTemplateFactory.lambda$createRawStatisticSection$8(TableTemplateFactory.java:127)
	at gui.table.templatedTwoCol.TabTemplate.addDouble(TabTemplate.java:46)
	at gui.table.templatedTwoCol.TableTemplateFactory.createRawStatisticSection(TableTemplateFactory.java:126)
	at gui.table.templatedTwoCol.TableTemplateFactory.create(TableTemplateFactory.java:38)
	at gui.MainWindowController.createParamTable(MainWindowController.java:3071)
	at gui.MainWindowController.tabulate(MainWindowController.java:2911)
	at gui.MainWindowController.tracePopulationChange(MainWindowController.java:2904)
	at datamodel.Project.lambda$refreshMainWindow$0(Project.java:95)
	at com.sun.javafx.application.PlatformImpl.lambda$runLater$10(PlatformImpl.java:428)
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:391)
	at com.sun.javafx.application.PlatformImpl.lambda$runLater$11(PlatformImpl.java:427)
	at com.sun.glass.ui.InvokeLaterDispatcher$Future.run(InvokeLaterDispatcher.java:96)
	at com.sun.glass.ui.win.WinApplication._runLoop(Native Method)
	at com.sun.glass.ui.win.WinApplication.lambda$runLoop$3(WinApplication.java:174)
	at java.base/java.lang.Thread.run(Thread.java:832)
   */

  /*
  What happens if relative Getters are use? See this Test: It also implies,
  that the absolute methods are more or less safe (at least for what we do here, namely access
  from many threads to a single index).

     double[] doubles = new double[5_000_000];
    for (int i = 0; i < 5_000_000; i++) {
      doubles[i] = i;
    }
    MemoryMapStorage storage = new MemoryMapStorage();
    DoubleBuffer buffer = storage.storeData(doubles);

    List<RunThread> list = new ArrayList<>();

    for (int n = 1; n < 2600; n++) {
      list.add(new RunThread(buffer, 1500, n));
    }

    for (Runnable r : list) {
      Thread t = new Thread(r);
      t.start();
    }


  }


  private class RunThread implements Runnable {

    private DoubleBuffer buffer;
    private int idx;
    private long sleep;


    public RunThread(DoubleBuffer buffer, int idx, long sleep) {
      this.buffer = buffer;
      this.idx = idx;
      this.sleep = sleep;
    }

    @Override
    public void run() {
      while (true) {

        buffer.position(1500);

        try {
          Thread.sleep(sleep);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }

        for (int i = 0; i < 5_000_000; i++) {
          double a = 9 / (buffer.get(i));
        }

        double d = buffer.get(idx);
        if (!DoubleMath.fuzzyEquals(d, 1500.0, 0.0001)) {
          System.out.println(toString() + "  --- > " + d);
        }
        if (sleep == ((2600 - 1) * 1)) {
          System.out.println(".");
        }
        System.out.println(buffer.get());

        Summary:
        With absolute method this works ok at least for 15 minutes constant.
        When however position() and get() are used, it differs by up to +3 indices forward.
         1500.0
        1500.0
        1500.0
        1501.0
        1500.0
        1501.0
        1502.0
        1501.0
        1501.0
        1500.0
        1500.0
        1502.0
        1500.0
        1502.0
        1501.0
        1500.0
        1501.0
        1500.0
        1502.0
        1501.0
        1500.0
        1500.0
        1501.0
        1502.0
        1500.0
        1500.0
        1500.0
        1500.0
        1500.0
        1500.0
        1500.0
        1501.0
        1502.0
        1500.0
        1500.0
        1503.0
        1501.0
        1500.0
        1502.0
        1501.0
        1500.0
        1501.0
        1503.0
        1500.0
        1500.0
        1500.0


        }
            }
   */
}

