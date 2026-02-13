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

package gui.dialog;

import dataModelNew.fxImpl.FxSample;
import io.FxFileSet;
import io.FxVisitedFile;
import io.SampleSet;

import java.nio.file.Files;
import java.nio.file.Path;

import io.SpCalDensity;
import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;
import visualizer.ResultsTable.TablePar;

public class SimpleFxEntry<T> extends AbstractFxEntry<T> {


  public SimpleFxEntry(T t) {
    super(t, true);
    formatCellLabel();
  }

  public SimpleFxEntry(T t, boolean sortable) {
    super(t, sortable);
    formatCellLabel();
  }

  // Implemented methods
  @Override
  public void notifyLabelChange() {
    formatCellLabel();
  }

  @Override
  public void formatCellLabel() {
    super.getCellLabelProperty().setValue(super.getLabel());
  }

  /**
   * These following implementing classes manage how to represent certain elements in a ListView.
   * Two principles: (1) keep the FX related stuff outside of otherwise slim, functional classes.
   * These classes are wrapped in FxEntries anyway. So either the FxEntry class would have to check
   * for each Object whether it implements some special interface like THIS to generate the label..
   * or we just pass some short, enum like class that manages this. (2) Just because some class
   * implements ListableDate, it does not imply that I want the date to be represented in the view.
   * Maybe I just want to sort by date but not literally read it.
   */

  ///////////////////////////////////////////////////////////////////////////////////////

  public static class FileFxEntry extends SimpleFxEntry<Path> {

    public FileFxEntry(Path file, boolean sortable) {
      super(file, sortable);

      boolean isDisqualified = !Files.isRegularFile(file);
      setDisqualified(isDisqualified);
      setDisqualificationNote("File does not exist on your system.");
    }

    @Override
    public void formatCellLabel() {
      Path file = unwrap();
      super.getCellLabelProperty().setValue(file.toString());
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class DirectoryFxEntry extends SimpleFxEntry<Path> {

    public DirectoryFxEntry(Path dir, boolean sortable) {
      super(dir, sortable);

      boolean isDisqualified = !Files.isDirectory(dir);
      setDisqualified(isDisqualified);
      setDisqualificationNote("Directory does not exist on your system.");
    }

    @Override
    public void formatCellLabel() {
      Path dir = unwrap();
      super.getCellLabelProperty().setValue(dir.toString());
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class VisitedDirectoryEntry extends SimpleFxEntry<FxVisitedFile> {

    public VisitedDirectoryEntry(FxVisitedFile visitedDir, boolean sortable) {
      super(visitedDir, sortable);

      boolean isDisqualified = !Files.isDirectory(visitedDir.getPlainVisitedFile().getPath());
      setDisqualified(isDisqualified);
      setDisqualificationNote("Directory does not exist on your system.");
    }

    @Override
    public void formatCellLabel() {
      FxVisitedFile file = unwrap();

      String label = file.getPlainVisitedFile().getPath().toString()
          + "\n"
          + "[" + file.getDate().toString() + "]";

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class FxFileSetEntry extends SimpleFxEntry<FxFileSet> {

    public FxFileSetEntry(FxFileSet fileSet, boolean sortable) {
      super(fileSet, sortable);
    }

    @Override
    public void formatCellLabel() {
      FxFileSet set = unwrap();

      String label = set.getLabel()
          + "\n"
          + "(N=" + set.getPlainFileSet().getFiles().size() + ")"
          + "\tFolder: " + set.getPlainFileSet().getExampleFolder()
          + "\tFile: " + set.getPlainFileSet().getExampleFile() + " ..."
          + "\n"
          + "[" + set.getDate().toString() + "]";

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class ParamSetEntry extends SimpleFxEntry<FxParamSet> {

    public ParamSetEntry(FxParamSet set, boolean sortable) {
      super(set, sortable);
      set.setListeningEntry(this);
    }

    @Override
    public void formatCellLabel() {
      FxParamSet set = unwrap();
      String label = set.getLabel();

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class ContainerEntry extends SimpleFxEntry<SpCalDensity.Container> {

    public ContainerEntry(SpCalDensity.Container container) {
      super(container, false);
    }

    @Override
    public void formatCellLabel() {
      SpCalDensity.Container container = unwrap();
      String label = container.getStr() + "Density: " + container.getDensity();

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class ParamSetDateEntry extends SimpleFxEntry<FxParamSet> {

    public ParamSetDateEntry(FxParamSet set, boolean sortable) {
      super(set, sortable);

      set.setListeningEntry(this);
    }

    @Override
    public void formatCellLabel() {
      FxParamSet set = unwrap();
      String label = set.getLabel()
          + "\n"
          + "[" + set.getPlainSet().getDateParameter().getValue() + "]";

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class MethodAndDateEntry extends SimpleFxEntry<FxMethod> {

    public MethodAndDateEntry(FxMethod method, boolean sortable) {
      super(method, sortable);
    }

    @Override
    public void formatCellLabel() {
      FxMethod method = unwrap();
      String label = method.getLabel()
          + "\n "
          + "[" + method.getFxDate().getPlainParameter().getValue() + "]";

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////

  public static class MethodDateAndFileEntry extends SimpleFxEntry<FxMethod> {

    public MethodDateAndFileEntry(FxMethod method, boolean sortable) {
      super(method, sortable);
    }

    @Override
    public void formatCellLabel() {
      FxMethod method = unwrap();

      String path = method.getPlainMethod().hasAssociatedFileOnDrive() ?
          method.getPlainMethod().getAssociatedFile().toString() : "No path found.";

      String label = method.getLabel()
          + "\n"
          + path
          + "\n"
          + "[" + method.getFxDate().getPlainParameter().getValue() + "]";

      super.getCellLabelProperty().setValue(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////
  /*
  For the ListView implementation, it seems to suffice to have the get/set Label functions here
  in the Entry class. We only need access to these methods but we do not require actual StringProperties-
  i.e., we do not require an FxSampleSet class.
   */

  public static class SampleSetFxEntry extends SimpleFxEntry<SampleSet> {

    public SampleSetFxEntry(SampleSet set, boolean sortable) {
      super(set, sortable);
    }

    @Override
    public void formatCellLabel() {
      SampleSet set = unwrap();

      super.getCellLabelProperty().setValue(set.getLabel());
    }

    @Override
    public String getLabel() {
      SampleSet set = unwrap();
      return set.getLabel();
    }

    @Override
    public void setLabel(String label) {
      SampleSet set = unwrap();
      set.setLabel(label);
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////
  /*
  For the TableView implementation, we need getters for properties.
  Hence, we cannot simply wrap the SampleSet, but we need an FxSampleSet.
   */

  public static class SampleFxEntry extends SimpleFxEntry<FxSample> {

    public SampleFxEntry(FxSample sample, boolean sortable) {
      super(sample, sortable);
    }

    @Override
    public void formatCellLabel() {
      FxSample set = unwrap();

      super.getCellLabelProperty().setValue(set.getLabel());
    }
  }

  /// ////////////////////////////////////////////////////////////////////////////////////
  /*
  For the TableView implementation, we need getters for properties.
  Hence, we cannot simply wrap the SampleSet, but we need an FxSampleSet.
   */

  public static class TableParFxEntry extends SimpleFxEntry<TablePar> {

    public TableParFxEntry(TablePar par, boolean sortable) {
      super(par, sortable);
    }

    @Override
    public void formatCellLabel() {
      TablePar par = unwrap();

      super.getCellLabelProperty().setValue(par.rowLabel());
    }
  }

}
