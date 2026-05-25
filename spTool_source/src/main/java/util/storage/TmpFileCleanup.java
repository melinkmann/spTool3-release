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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Known bug in windows prevents an active Java instance from deleting temp file that were created
 * by itself. Hence, at the start, SpTool has to check for its own old temp files and delete them to
 * prevent pile-up.
 * <p>
 * This code comes from mzmine where most operations are tasks, hence the Runnable interface. I just
 * kept it and called the run method manually from the FxThread at startup "because it works".
 */

public class TmpFileCleanup implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(TmpFileCleanup.class.getName());

  @Override
  public void run() {

    LOGGER.debug("Checking for old temporary files...");
    try {

      // Find all temporary files with the mask spTool2_*.tmp
      File tempDir = new File(System.getProperty("java.io.tmpdir"));
      File[] remainingTmpFiles = tempDir.listFiles(new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
          if (name.matches(MemoryMapStorage.FILE_IDENTIFIER + ".*\\.tmp")) {
            return true;
          }
          return false;
        }
      });

      if (remainingTmpFiles != null) {
        for (File remainingTmpFile : remainingTmpFiles) {

          // Skip files created by someone else
          if (!remainingTmpFile.canWrite()) {
            continue;
          }

          // Try to obtain a lock on the file
          RandomAccessFile rac = new RandomAccessFile(remainingTmpFile, "rw");

          FileLock lock = rac.getChannel().tryLock();
          rac.close();

          if (lock != null) {
            // We locked the file, which means nobody is using it
            // anymore and it can be removed
            LOGGER.trace("Removing unused temporary file " + remainingTmpFile);
            boolean success = remainingTmpFile.delete();
            if (success) {
              LOGGER.trace("... successfully removed file " + remainingTmpFile + ".");
            } else {
              LOGGER.info("... unable to remove file " + remainingTmpFile + ".");
            }
          }

        }
      }
    } catch (IOException e) {
      LOGGER.warn("Error while checking for old temporary files. Stack trace: "
          + ExceptionUtils.getStackTrace(e));
    }

  }
}