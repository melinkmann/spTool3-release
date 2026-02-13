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
import gui.dialog.SimpleFxEntry.FxFileSetEntry;
import gui.dialog.SimpleFxEntry.MethodAndDateEntry;
import gui.dialog.SimpleFxEntry.MethodDateAndFileEntry;
import gui.dialog.SimpleFxEntry.ParamSetDateEntry;
import gui.dialog.SimpleFxEntry.ParamSetEntry;
import gui.dialog.SimpleFxEntry.SampleFxEntry;
import gui.dialog.SimpleFxEntry.SampleSetFxEntry;
import gui.dialog.SimpleFxEntry.TableParFxEntry;
import gui.dialog.SimpleFxEntry.VisitedDirectoryEntry;
import io.FxFileSet;
import io.FxVisitedFile;
import io.SampleSet;
import java.util.List;
import java.util.stream.Collectors;

import io.SpCalDensity;
import processing.parameterSets.FxMethod;
import processing.parameterSets.FxParamSet;
import visualizer.ResultsTable.TablePar;


/**
 * Cannot be abstract, since the viewers use this factory whenever new items are added to build the
 * correct FxEntry instances.
 */

public interface FxEntryFactory<T> {

  public boolean isSortable();

  public FxEntry<T> create(T t);

  public List<FxEntry<T>> create(List<T> tList);

  /**
   * Represent the general stuff in an abstract class.
   */

  public abstract class AbstractFxEntryFactory<T> implements FxEntryFactory<T> {

    private final boolean sortable;

    public AbstractFxEntryFactory() {
      this.sortable = true;
    }

    public AbstractFxEntryFactory(boolean isSortable) {
      this.sortable = isSortable;
    }

    public boolean isSortable() {
      return sortable;
    }

  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class SimpleEntryFactory<T> extends AbstractFxEntryFactory<T> {

    @Override
    public FxEntry<T> create(T t) {
      return new SimpleFxEntry<>(t, isSortable());
    }

    @Override
    public List<FxEntry<T>> create(List<T> tList) {
      return tList.stream()
          .map(t -> new SimpleFxEntry<>(t, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class RecentLocationsDialogEntryFactory extends
      AbstractFxEntryFactory<FxVisitedFile> {

    @Override
    public FxEntry<FxVisitedFile> create(FxVisitedFile fxVisitedFile) {
      return new VisitedDirectoryEntry(fxVisitedFile, isSortable());
    }

    @Override
    public List<FxEntry<FxVisitedFile>> create(List<FxVisitedFile> fxVisitedFiles) {
      return fxVisitedFiles.stream()
          .map(o -> new VisitedDirectoryEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class SingleFileSetEntryFactory extends AbstractFxEntryFactory<FxFileSet> {

    @Override
    public FxEntry<FxFileSet> create(FxFileSet fxFileSet) {
      return new FxFileSetEntry(fxFileSet, isSortable());
    }

    @Override
    public List<FxEntry<FxFileSet>> create(List<FxFileSet> fxFileSets) {
      return fxFileSets.stream()
          .map(o -> new FxFileSetEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class ParamSetEntryFactory extends
      AbstractFxEntryFactory<FxParamSet> {

    @Override
    public FxEntry<FxParamSet> create(FxParamSet paramSet) {
      return new ParamSetEntry(paramSet, isSortable());
    }

    @Override
    public List<FxEntry<FxParamSet>> create(List<FxParamSet> paramSets) {
      return paramSets.stream()
          .map(o -> new ParamSetEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class ContainerFactory extends
      AbstractFxEntryFactory<SpCalDensity.Container> {

    @Override
    public FxEntry<SpCalDensity.Container> create(SpCalDensity.Container paramSet) {
      return new SimpleFxEntry.ContainerEntry(paramSet);
    }

    @Override
    public List<FxEntry<SpCalDensity.Container>> create(List<SpCalDensity.Container> paramSets) {
      return paramSets.stream()
          .map(o -> new SimpleFxEntry.ContainerEntry(o))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class ParamSetWithDateEntryFactory extends
      AbstractFxEntryFactory<FxParamSet> {

    @Override
    public FxEntry<FxParamSet> create(FxParamSet paramSet) {
      return new ParamSetDateEntry(paramSet, isSortable());
    }

    @Override
    public List<FxEntry<FxParamSet>> create(List<FxParamSet> paramSets) {
      return paramSets.stream()
          .map(o -> new ParamSetDateEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class MethodDateEntryFactory extends AbstractFxEntryFactory<FxMethod> {

    @Override
    public FxEntry<FxMethod> create(FxMethod method) {
      return new MethodAndDateEntry(method, isSortable());
    }

    @Override
    public List<FxEntry<FxMethod>> create(List<FxMethod> paramSets) {
      return paramSets.stream()
          .map(o -> new MethodAndDateEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class MethodDateFileEntryFactory extends AbstractFxEntryFactory<FxMethod> {

    @Override
    public FxEntry<FxMethod> create(FxMethod method) {
      return new MethodDateAndFileEntry(method, isSortable());
    }

    @Override
    public List<FxEntry<FxMethod>> create(List<FxMethod> paramSets) {
      return paramSets.stream()
          .map(o -> new MethodDateAndFileEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class SampleSetEntryFactory extends AbstractFxEntryFactory<SampleSet> {

    @Override
    public FxEntry<SampleSet> create(SampleSet set) {
      return new SampleSetFxEntry(set, isSortable());
    }

    @Override
    public List<FxEntry<SampleSet>> create(List<SampleSet> sets) {
      return sets.stream()
          .map(o -> new SampleSetFxEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////

  public static class SampleEntryFactory extends AbstractFxEntryFactory<FxSample> {

    @Override
    public FxEntry<FxSample> create(FxSample sample) {
      return new SampleFxEntry(sample, isSortable());
    }

    @Override
    public List<FxEntry<FxSample>> create(List<FxSample> sets) {
      return sets.stream()
          .map(o -> new SampleFxEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }
  ///////////////////////////////////////////////////////////////////////////////

  public static class TableParFactory extends AbstractFxEntryFactory<TablePar> {

    public TableParFactory(boolean isSortable) {
      super(isSortable);
    }

    @Override
    public FxEntry<TablePar> create(TablePar sample) {
      return new TableParFxEntry(sample, isSortable());
    }

    @Override
    public List<FxEntry<TablePar>> create(List<TablePar> sets) {
      return sets.stream()
          .map(o -> new TableParFxEntry(o, isSortable()))
          .collect(Collectors.toList());
    }
  }

  ///////////////////////////////////////////////////////////////////////////////


}
