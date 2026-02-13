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


import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.channels.FileChannel;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * *  Strategy:
 * <p> - MemoryMapStorage generates temp files and keeps a HashSet of them
 * <p> - This HashSet is the container that is generated with each MemoryMapStorage instance
 * <p>
 * This class defines a memory-mapped temporary file storage for large primitive type arrays. The
 * storeData() function stores an array into the underlying temporary file and returns a buffer. The
 * buffer is directly bound to the memory-mapped portion of the file so the data can be directly
 * accessed without loading it into another intermediate primitive type array.
 * <p>
 * The size of each temporary file is STORAGE_FILE_CAPACITY bytes. When the file is full, a new file
 * is automatically created using the createNewMappedFile() function. The size of each temporary
 * file in the filesystem may show as 1GB, but actually only a portion of that space is occupied on
 * the disk, depending on the amount of stored data (this can be examined using the 'du -hs' Linux
 * command.
 * <p>
 * There is no support for removing data from the file - we assume such operation is rare and
 * therefore the data can be left on the disk until the whole temporary file is discarded.
 * <p>
 * There is a limit on the number of open file descriptors (e.g. 1024 by default on Linux). With 1
 * GB per temporary file, this would give us about 1 TB of storage space, so perhaps it is okay.
 * There is no way to remove the memory-mapped file, it is removed automatically when the
 * MappedByteBuffer is garbage-collected.
 * <p>
 * The total amount of storage space is also limited by the amount of addressable virtual memory
 * (e.g., 128TB on Linux). For this reason, this approach requires a 64-bit system - the limit would
 * be only 2GB on a 32-bit system.
 */
public class MemoryMapStorage {

  private static final Logger LOGGER = LogManager.getLogger(MemoryMapStorage.class);

  /**
   * One temporary file can store STORAGE_FILE_CAPACITY bytes. We need to fit within 2GB limit for a
   * single MappedByteBuffer. 1 GB per file (1_000_000_000L) seems like a good start.
   */
  private static final long STORAGE_FILE_CAPACITY = 1_800_000_000L;
  public static final String FILE_IDENTIFIER = "spTool3_";

  private final Set<File> temporaryFiles = new HashSet<>();

  /**
   * The file that we are currently writing into.
   */
  private MappedByteBuffer currentMappedFile = null;

  /**
   * Creates a new temporary file, maps it into memory, and returns the corresponding
   * MappedByteBuffer. The capacity of the buffer is STORAGE_FILE_CAPACITY bytes.
   *
   * @return a MappedByteBuffer corresponding to the memory-mapped temporary file
   * @throws IOException
   */
  private MappedByteBuffer createNewMappedFile() throws IOException {
    // Create the temporary storage file
    File storageFileName = File.createTempFile(FILE_IDENTIFIER, ".tmp");
    temporaryFiles.add(storageFileName);
    LOGGER.info("Created a temporary file " + storageFileName);

    // Open the file for writing
    RandomAccessFile storageFile = new RandomAccessFile(storageFileName, "rw");

    // Map the file into memory
    MappedByteBuffer mappedFileBuffer = storageFile.getChannel().map(FileChannel.MapMode.READ_WRITE,
        0, STORAGE_FILE_CAPACITY);

    // Close the temporary file, the memory mapping will remain
    storageFile.close();

    // Unfortunately, deleteOnExit() doesn't work on Windows, see JDK
    // bug #4171239. We will try to remove the temporary files in a
    // shutdown hook registered in the main.ShutDownHook class.
    //
    // Edit: run TmpFileCleanup.run() at startup (or maybe on button click)
    // to delete remaining .tmp files
    storageFileName.deleteOnExit();

    return mappedFileBuffer;

  }

  /**
   * Store the given double[] array in a memory-mapped temporary file and return a read-only
   * DoubleBuffer that can access the data.
   *
   * @param data the double[] array with the data
   * @return a read-only DoubleBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull
  DoubleBuffer storeData(@Nonnull final double[] data)
      throws IOException {
    return storeData(data, 0, data.length);
  }

  /**
   * Store the given double[] array in a memory-mapped temporary file and return a read-only
   * DoubleBuffer that can access the data.
   *
   * @param data   the double[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only DoubleBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized @Nonnull
  DoubleBuffer storeData(@Nonnull final double[] data, int offset, int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + ((long) length * Double.BYTES)
        > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Set the limit to the end of the new array and create a buffer slice
    currentMappedFile.limit(savedPosition + length * Double.BYTES);
    final ByteBuffer slice = currentMappedFile.slice();

    // Create a double view of the memory-mapped byte buffer
    DoubleBuffer sliceDoubleView = slice.asDoubleBuffer();

    /*
     SpTool2: Copy the data to the memory mapped storage, added try-catch for spTool since
          e.g. 35 hrs @DT=1ms LA-spICP-MS or 2 min @ DT = 1 µs would exceed 1 GB!
     --> worst case would need to convert to counts per DT directly and ceil to integer?
     --> better case: do not merge files into one but store the summed time stamps in single buffers :)

     SpTool3: I think what we would need here, is some iteration to split an array,
     in case that a double[] that exceeds 1 GB or whatever is the maximum size.
     Note: This is somewhat ridiculous since a full double[]
     (~2.15E9 doubles = 2.15E9 *  8 byte/double ~ 17.2 GB) would be more than the heap could handle
     anyway. The current limit of 1.8 GB ~ 2.25E8 doubles, i.e., 3.75 min of 1 µs acquisition.
     */

    try {
      sliceDoubleView.put(data, offset, length);
    } catch (BufferOverflowException | IndexOutOfBoundsException | ReadOnlyBufferException e) {
      LOGGER.fatal(ExceptionUtils.getStackTrace(e));
    }

    // Update the position and the main buffer so we are ready to store the next array
    currentMappedFile.position(savedPosition + length * Double.BYTES);

    // Create a read-only version of the new buffer slice
    final DoubleBuffer readOnlySlice = sliceDoubleView.asReadOnlyBuffer();
    return readOnlySlice;
  }


  /**
   * Store the given int[] array in a memory-mapped temporary file and return a read-only IntBuffer
   * that can access the data. Re-Downloaded from MZMine3 -> MemoryMapStorage Feb/21/2022
   *
   * @param data the int[] array with the data
   * @return a read-only IntBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */

  @Nonnull
  public synchronized IntBuffer storeData(@Nonnull final int[] data) throws IOException {
    return storeData(data, 0, data.length);
  }


  /**
   * Store the given int[] array in a memory-mapped temporary file and return a read-only IntBuffer
   * that can access the data. Re-Downloaded from MZMine3 -> MemoryMapStorage Feb/21/2022
   *
   * @param data   the int[] array with the data
   * @param offset offset of the stored portion of the data[] array
   * @param length size of the stored portion of the data[] array
   * @return a read-only IntBuffer that is directly mapped to the stored data on the disk
   * @throws IOException
   */
  public synchronized IntBuffer storeData(@Nonnull final int[] data, int offset,
      int length) throws IOException {

    // If we have no storage file or if the current file is full, create a new one
    if ((currentMappedFile == null)
        || (currentMappedFile.position() + ((long) length * Integer.BYTES)
        > STORAGE_FILE_CAPACITY)) {
      currentMappedFile = createNewMappedFile();
    }

    // Save the current position in the storage file
    final int savedPosition = currentMappedFile.position();

    // Set the limit to the end of the new array and create a buffer slice
    currentMappedFile.limit(savedPosition + length * Integer.BYTES);
    final ByteBuffer slice = currentMappedFile.slice();

    // Create an int view of the memory-mapped byte buffer
    IntBuffer sliceIntView = slice.asIntBuffer();

    // Copy the data to the memory mapped storage
    try {
      sliceIntView.put(data, offset, length);
    } catch (BufferOverflowException | IndexOutOfBoundsException | ReadOnlyBufferException e) {
      LOGGER.fatal(ExceptionUtils.getStackTrace(e));
    }

    // Update the position and the main buffer so we are ready to store the next array
    currentMappedFile.position(savedPosition + length * Integer.BYTES);

    // Create a read-only version of the new buffer slice
    final IntBuffer readOnlySlice = sliceIntView.asReadOnlyBuffer();
    return readOnlySlice;

  }


}