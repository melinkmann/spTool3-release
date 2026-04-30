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

import core.SpTool3Main;
import java.nio.file.Path;
import util.Util;

public abstract class GlobalIO {

  public static final String UNSPECIFIC_EXTENSION = ".sp";
  public static final String METHOD_EXTENSION = ".spm";
  public static final String SERIALIZED_EXTENSION = ".spo";
  public static final String SERIALIZED_PROJECT_EXTENSION = ".spp";

  public static final long MIN_XML_SIZE_BYTES = 8;

  public static final int MAX_CHAR_FILENAME = 75;

  //
  public static Path makeConfFile() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "spConf" + METHOD_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }

  public static Path makeDefaultCsvReaderFile() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "defaultCsvReader" + METHOD_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }


  public static Path makeDefaultNuReaderFile() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "defaultNuReader" + METHOD_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }

  public static Path makeExporterFile() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "spExportConf" + METHOD_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }


  public static Path makeResultsTableFile() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "resTabEntries" + UNSPECIFIC_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }

  public static Path makeImportWindow() {
    Path confFile = PathUtil.getUserDir();
    confFile = PathUtil.addDir(confFile, "spTool3", "config");
    PathUtil.createDir(confFile);
    confFile = PathUtil.addFile(confFile, "import" + SERIALIZED_EXTENSION);
    PathUtil.createFile(confFile);
    return confFile;
  }

  public static Path makeExportGraphFolder() {
    Path exportFolder = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    exportFolder = PathUtil.addDir(exportFolder, "Figures");
    PathUtil.createDir(exportFolder);
    return exportFolder;
  }

  public static Path makeExportDataFolder() {
    Path exportFolder = SpTool3Main.getRunTime().getConfParams().getDefaultProjectPath();
    exportFolder = PathUtil.addDir(exportFolder, "Data");
    PathUtil.createDir(exportFolder);
    return exportFolder;
  }

  public static Path makeMethodsFolder() {
    Path dir = PathUtil.getUserDir();
    dir = PathUtil.addDir(dir, "spTool3", "user", "methods");
    PathUtil.createDir(dir);
    return dir;
  }

  public static Path makeMethodsRecycleFolder() {
    Path dir = PathUtil.getUserDir();
    String yearMonth = Util.getYearMonthDate();
    dir = PathUtil.addDir(dir, "spTool3", "user", "methods_recycled", yearMonth);
    PathUtil.createDir(dir);
    return dir;
  }


  public static Path makeSubMethodsFile() {
    Path subMethodsFile = PathUtil.getUserDir();
    subMethodsFile = PathUtil.addDir(subMethodsFile, "spTool3", "config");
    PathUtil.createDir(subMethodsFile);
    subMethodsFile = PathUtil.addFile(subMethodsFile, "subMethods" + METHOD_EXTENSION);
    PathUtil.createFile(subMethodsFile);
    return subMethodsFile;
  }


  public static Path makeGuiParameterFile() {
    Path subMethodsFile = PathUtil.getUserDir();
    subMethodsFile = PathUtil.addDir(subMethodsFile, "spTool3", "config");
    PathUtil.createDir(subMethodsFile);
    subMethodsFile = PathUtil.addFile(subMethodsFile, "guiParameter" + METHOD_EXTENSION);
    PathUtil.createFile(subMethodsFile);
    return subMethodsFile;
  }


  public static Path makeBackupFolder() {
    Path dir = PathUtil.getUserDir();
    dir = PathUtil.addDir(dir, "spTool3", "config", "backup");
    PathUtil.createDir(dir);
    return dir;
  }

  public static Path makeLogDir() {
    Path dir = PathUtil.getUserDir();
    dir = PathUtil.addDir(dir, "spTool3", "logging");
    PathUtil.createDir(dir);
    return dir;
  }

  // https://stackoverflow.com/questions/754307/regex-to-replace-characters-that-windows-doesnt-accept-in-a-filename
  public static String cleanupWindowsFileName(String fileName) {
    // remove illegal chars
    fileName = fileName.replaceAll("%", "pct");
    fileName = fileName.replaceAll("(?<=\\d)\\.(?=\\d)", "p");  // "5.1" -> "5p1"
    fileName = fileName.replaceAll("\\.", "_"); // any other "." -> "_"
    fileName = fileName.replaceAll("µ", "u");
    fileName = fileName.replaceAll("[^\\w|\\s]", "_"); // anything non word or num
    fileName = fileName.replaceAll("\\s$", ""); // final white space
    // no spaces, just underscores; but limit to 1 "_" in a row
    fileName = fileName.replaceAll(" ", "_");
    fileName = fileName.replaceAll("_+", "_").replaceAll("^_+|_+$", "");

    fileName = fileName.substring(0, Math.min(fileName.length(), MAX_CHAR_FILENAME));
    return fileName;
  }

}
