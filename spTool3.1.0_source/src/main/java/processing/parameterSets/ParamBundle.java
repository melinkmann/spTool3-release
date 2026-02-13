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

import java.io.Serializable;
import java.util.List;
import org.w3c.dom.Element;
import processing.parameters.Parameter;
import processing.parameters.SpawnControlParameter;

public interface ParamBundle extends Serializable {

  public static final String BUNDLE_ID_ROI_START_STOP = "roiStartStopBundle";
  public static final String BUNDLE_ID_ROI_SIG_FACTOR = "roiSigFactorBundle";
  public static final String BUNDLE_ID_ELEMENT = "elementBundle";
  public static final String BUNDLE_ID_SIA = "siaBundle";

  public ParamBundle copy(SpawnControlParameter newParent);

  void readFromXmlElement(Element xmlElement);

  List<Parameter<?>> getSelfAndAllChildrenAllGenForXml();

  Parameter<?> getHeaderParameter();

  void requestRemoveSelf();

  String getSortingString();

  default boolean isEquivalent(ParamBundle bundle) {
    List<Parameter<?>> thisParams = getSelfAndAllChildrenAllGenForXml();
    List<Parameter<?>> thatParams = bundle.getSelfAndAllChildrenAllGenForXml();
    boolean isEquivalent = thisParams.size() == thatParams.size();
    if (isEquivalent) {
      for (int i = 0; i < thisParams.size(); i++) {
        Parameter<?> thisParam = thisParams.get(i);
        Parameter<?> thatParam = thatParams.get(i);
        if (thisParam.isEquivalent(thatParam)) {
          isEquivalent = true;
        } else {
          isEquivalent = false;
          break;
        }
      }
    }
    return isEquivalent;
  }

}
