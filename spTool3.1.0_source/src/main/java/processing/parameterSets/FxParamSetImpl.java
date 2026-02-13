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

package processing.parameterSets;

import core.SpTool3Main;
import gui.ParameterView;
import gui.dialog.FxEntry;
import gui.listAndSearch.FxWrapper;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.parameters.FxParameter;
import processing.parameters.Parameter;
import util.Util;

public class FxParamSetImpl implements FxParamSet {

  private static final Logger LOGGER = LogManager.getLogger(FxParamSetImpl.class);

  private final ParamSet plainSet;
  // Keep track of the FxInstances and avoid creating new instances for each get() call
  private final HashMap<Parameter<?>, FxParameter<?>> fxParameterMap;
  // For refresh.
  protected ParameterView controller;
  private FxEntry<FxParamSet> fxEntry;


  public FxParamSetImpl(ParamSet plainSet) {
    this.plainSet = plainSet;
    this.fxParameterMap = new LinkedHashMap<>();

    // Dummy instance! Call setController() to actually provide the functionality.
    this.controller = null;

    // Fill the HashMap ONCE with the NEWLY made FxInstances
    List<Parameter<?>> allParams = plainSet.listAllParameters();
    for (Parameter<?> par : allParams) {
      FxParameter<?> fxPar = par.getObservableInstance();
      fxPar.setListeningFxSet(this); // Who to notify when change occurs.
      fxParameterMap.put(par, fxPar);
    }

    // Determine the structure: Use the parents as the root to traverse the children
    List<Parameter<?>> parentParams = plainSet.listParentParameters();
    parentParams.forEach(par -> par.determineChildLevels(0));

    // Dummy instance
    this.fxEntry = null;
  }

  @Override
  public void setController(ParameterView controller) {
    this.controller = controller;
  }

  @Override
  public void notifyItemChange() {
    // Force update of the FxInstances to match the plain instances.
    List<FxParameter<?>> fxParameters = getActiveFxParameters();
    fxParameters.forEach(FxParameter::forceUpdateExternally);

    // Notify the viewer to reload.
    if (controller != null) {
      controller.notifyItemChange();
    }
  }

  @Override
  public void notifyValueChange() {
    // Notify the viewer to adjust the "decorative" signs like "method was changed" symbols.
    if (controller != null) {
      controller.notifyValueChange();
    }
  }

  @Override
  public void notifyLabelChange() {
    // Notify the ListItem to update the Label
    if (fxEntry != null) {
      fxEntry.notifyLabelChange();
    }
  }

  public List<FxParameter<?>> getActiveFxParametersWithoutMeta() {
    List<Parameter<?>> activePlainParameter = plainSet.listActiveParametersWithoutMeta();
    // Hash Set avoids duplicates:
    Set<FxParameter<?>> fxPars = new LinkedHashSet<>();
    for (Parameter<?> parameter : activePlainParameter) {
      if (parameter != null) {
        // Since we allow programmatic creation of parameters, we require capability to add later on
        if (!fxParameterMap.containsKey(parameter)) {
          FxParameter<?> fxPar = parameter.getObservableInstance();
          fxPar.setListeningFxSet(this); // Who to notify when change occurs.
          fxParameterMap.put(parameter, fxPar);
        }
        // Go on as before
        FxParameter<?> fxParameter = fxParameterMap.get(parameter);
        if (fxParameter != null) {
          fxPars.add(fxParameter);
        }
      }
    }
    return new ArrayList<>(fxPars);
  }

  @Override
  public List<FxParameter<?>> getActiveFxParameters() {
    // Check expert mode
    boolean showAllParams = false;
    if (SpTool3Main.getRunTime().getConfParams() != null) {
      showAllParams = SpTool3Main.getRunTime().getConfParams().showAllParamsAsExpert();
    }

    // Go on
    List<Parameter<?>> activePlainParameter = plainSet.listActiveParameters();
    // Hash Set avoids duplicates:
    Set<FxParameter<?>> fxPars = new LinkedHashSet<>();
    for (Parameter<?> parameter : activePlainParameter) {
      if (parameter != null) {
        // Since we allow programmatic creation of parameters, we require capability to add later on
        if (!fxParameterMap.containsKey(parameter)) {
          FxParameter<?> fxPar = parameter.getObservableInstance();
          fxPar.setListeningFxSet(this); // Who to notify when change occurs.
          fxParameterMap.put(parameter, fxPar);
        }
        // Go on as before
        FxParameter<?> fxParameter = fxParameterMap.get(parameter);
        if (fxParameter != null) {
          // Check if the parameter should be shown according to export mode law:
          if (showAllParams) {
            fxPars.add(fxParameter); // Show all anyway
          } else {
            if (!fxParameter.getPlainParameter().isLimitedToExpert()) {
              fxPars.add(fxParameter); // Only show if NOT expert-type
            }
          }
        }
      }
    }
    return new ArrayList<>(fxPars);
  }

  public List<FxParameter<?>> getAllFxParameters() {
    List<Parameter<?>> allPlainParameters = plainSet.listAllParameters();
    // Hash Set avoids duplicates:
    Set<FxParameter<?>> fxPars = new LinkedHashSet<>();
    for (Parameter<?> parameter : allPlainParameters) {
      if (parameter != null) {
        // Since we allow programmatic creation of parameters, we require capability to add later on
        if (!fxParameterMap.containsKey(parameter)) {
          FxParameter<?> fxPar = parameter.getObservableInstance();
          fxPar.setListeningFxSet(this); // Who to notify when change occurs.
          fxParameterMap.put(parameter, fxPar);
        }
        // Go on as before
        FxParameter<?> fxParameter = fxParameterMap.get(parameter);
        if (fxParameter != null) {
          fxPars.add(fxParameter);
        }
      }
    }
    return new ArrayList<>(fxPars);
  }

  public ParamSet getPlainSet() {
    return plainSet;
  }

  @Override
  public void setUneditable() {
    fxParameterMap.values().forEach(FxParameter::setUneditable);
  }


  @Override
  public boolean isEqualPlainObject(FxParamSet fxParamSet) {
    return fxParamSet.getPlainSet().equals(plainSet);
  }

  /// /////////////////////////////////////////////////////////////////////////////////

  //  Afaik only to update labels and keep them in sync via binding.
  public void setListeningEntry(FxEntry<FxParamSet> fxEntry) {
    this.fxEntry = fxEntry;
  }


  @Override
  public String getLabel() {
    return plainSet.getLabelParameter().getValue();
  }

  @Override
  public void setLabel(String label) {
    plainSet.getLabelParameter().setValue(label);
    /*
     Note: This is not a "labelChange".
     Why? This method gets called as an external label change, e.g., when a listView that
     presents this parameter set is editable. Now, upon changing the label there, we need
     to refill the "parameter grid" (the listview with the parameters) to show the new label
     in its textfield correctly.

     Compare: The "labelChange" is fired (so far) only when a label parameter instance
     (this is an explicit parameter class!) is edited, which occurs on the parameter grid.
     */
    notifyValueChange();
  }

  @Override
  public Date getDate() {
    Date date = Util.stringToDate(plainSet.getDateParameter().getValue());
    return date;
  }

  @Override
  public boolean hasDate() {
    return true;
  }

  @Override
  public Color getColor() {
    return plainSet.getEnum().getColor();
  }


  @Override
  public Node getShape() {
    return plainSet.getEnum().getShape();
  }

  /// /////////////////////////////////////////////////////////////////////////////////
  @Override
  public boolean isEqualWrappedObject(FxWrapper that) {
    boolean isEqual = false;
    if (that instanceof FxParamSetImpl) {
      isEqual = this.getPlainSet().equals(((FxParamSetImpl) that).getPlainSet());
    }
    return isEqual;
  }
}
