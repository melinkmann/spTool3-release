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

package io;

import gui.dialog.notification.NotificationFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class PathUtil {

  private static final Logger LOGGER = LogManager.getLogger(PathUtil.class);

  // C:\Users\Matthias
  public static Path getUserDir() {
    Path homeFolder = Path.of("C:\\");
    try {
      homeFolder = Path.of(SystemUtils.getUserHome().getAbsolutePath());
    } catch (Exception exception) {
      NotificationFactory.openError(exception);
    }
    return homeFolder;
  }

  /*
  This will print the absolute path of the current directory from where your application was initialized.
  i.e. C:\SPB_Data\git\SpTool3_light
   */
  public static Path getCwd() {
    Path homeFolder = Path.of("C:\\");
    try {
      homeFolder = Path.of(System.getProperty("user.dir"));
    } catch (Exception exception) {
      NotificationFactory.openError(exception);
    }
    return homeFolder;
  }


  // List files recursively
  public static List<Path> listFiles(Path path, boolean recursive) {
    //https://stackoverflow.com/questions/2056221/recursively-list-files-in-java
    List<Path> recursivePaths = new ArrayList<>();

    int levels;
    if (recursive) {
      levels = Integer.MAX_VALUE;
    } else {
      levels = 1;
    }

    try {
      Files.find(path, levels,
          (filePath, fileAttr) -> fileAttr.isRegularFile())
          .forEach(recursivePaths::add);
    } catch (IOException ioException) {
      LOGGER.error(ExceptionUtils.getStackTrace(ioException));
      NotificationFactory.openError(ioException);
    }

    return recursivePaths;
  }

  // Filter files
//  public static List<Path> removeType(List<Path> paths, String typeWithoutDot) {
//    List<Path> filteredPaths = new ArrayList<>();
//    for (Path path : paths) {
//      String stringValue = path.getFileName().toString();
//      String type = stringValue.substring(stringValue.lastIndexOf(".") + 1);
//      if (!type.equals(typeWithoutDot)) {
//        filteredPaths.add(path);
//      }
//    }
//    return filteredPaths;
//  }

//  public static List<Path> retainType(List<Path> paths, String typeWithoutDot) {
//    List<Path> filteredPaths = new ArrayList<>();
//    for (Path path : paths) {
//      String stringValue = path.getFileName().toString();
//      String type = stringValue.substring(stringValue.lastIndexOf(".") + 1);
//      if (type.equals(typeWithoutDot)) {
//        filteredPaths.add(path);
//      }
//    }
//    return filteredPaths;
//  }


  public static List<Path> removeType(List<Path> paths, String typeWithDot) {
    List<Path> filteredPaths = new ArrayList<>();
    for (Path path : paths) {
      String stringValue = path.getFileName().toString();
      String type = stringValue.substring(stringValue.lastIndexOf("."));
      if (!type.equals(typeWithDot)) {
        filteredPaths.add(path);
      }
    }
    return filteredPaths;
  }

  public static List<Path> retainType(List<Path> paths, String typeWithDot) {
    List<Path> filteredPaths = new ArrayList<>();
    for (Path path : paths) {
      String stringValue = path.getFileName().toString();
      String type = stringValue.substring(stringValue.lastIndexOf("."));
      if (type.equals(typeWithDot)) {
        filteredPaths.add(path);
      }
    }
    return filteredPaths;
  }

  public static String getExtensionWithoutDot(Path path) {
    String extension = "";
    if (path != null && path.getFileName() != null) {

      String fileName = path.getFileName().toString();
      int lastDotIndex = fileName.lastIndexOf('.');

      if (lastDotIndex != -1 && lastDotIndex < fileName.length() - 1) {
        extension =  fileName.substring(lastDotIndex + 1); // excludes the dot
      }

    }
    return extension;
  }

  public static String getExtensionWithDot(Path path) {
    String extension = "";
    if (path != null && path.getFileName() != null) {

      String fileName = path.getFileName().toString();
      int lastDotIndex = fileName.lastIndexOf('.');

      if (lastDotIndex != -1 && lastDotIndex != fileName.length() - 1) {
        extension = fileName.substring(lastDotIndex); // includes the dot
      }

    }
    return extension;
  }

  public static String getParentFolderNameFromFile(Path path) {
    String folderName = "";

    if (path != null) {
      Path parent = path.getParent();
      if (parent != null) {
        Path folder = parent.getFileName();
        if (folder != null) {
          folderName = folder.toString();
        }
      }
    }

    return folderName;
  }

  public static String getFileNameWithoutExtension(Path path) {
    String fileNameWithoutExt = "";

    if (path != null) {
      Path fileName = path.getFileName();
      if (fileName != null) {
        String name = fileName.toString();
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex > 0) {  // dot exists and is not the first character
          fileNameWithoutExt = name.substring(0, dotIndex);
        } else {
          fileNameWithoutExt = name;  // no extension found
        }
      }
    }

    return fileNameWithoutExt;
  }

  public static String getFileNameWithExtension(Path path) {
    String fileNameWithExtension = "";

    if (path != null) {
      Path fileName = path.getFileName();
      if (fileName != null) {
        fileNameWithExtension = fileName.toString();
      }
    }

    return fileNameWithExtension;
  }

  // Helper methods
  public static Path addDir(Path path, String... folders) {
    for (String folder : folders) {
      path = path.resolve(folder);
    }
    return path;
  }

  public static boolean createDir(Path directory) {
    boolean good = false;
    if (!Files.isDirectory(directory)) {
      try {
        // PLURAL as in directorIES in order to create non-existing parents!
        Files.createDirectories(directory);
        good = true;
      } catch (IOException ioException) {
        LOGGER.error(ExceptionUtils.getStackTrace(ioException));
        NotificationFactory.openError(ioException);
      }
    }
    return good;
  }

  public static Path addFile(Path path, String nameWithType) {
    Path file = path;
    if (Files.isDirectory(path)) {
      file = path.resolve(nameWithType);
    }
    return file;
  }

  public static boolean createFile(Path fileInPath) {
    boolean good = false;
    if (!Files.isReadable(fileInPath)) {
      try {
        Files.createFile(fileInPath);
        good = true;
      } catch (IOException ioException) {
        LOGGER.error(ExceptionUtils.getStackTrace(ioException));
        NotificationFactory.openError(ioException);
      }
    }
    return good;
  }

  public static String removeExtension(String fileName) {
    String reduced = fileName;
    int dotIndex = fileName.lastIndexOf('.');
    if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
      reduced = fileName.substring(0, dotIndex);
    }
    return reduced; // No extension found
  }

}
