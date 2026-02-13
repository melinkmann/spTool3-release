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

import java.io.Serial;
import java.nio.file.Path;
import org.w3c.dom.NodeList;

public class EmptyParams extends AbstractParamSet implements ParamSet {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  public static final String XML_ELEMENT_TAG = "empty";

  public EmptyParams() {
    super("Empty placeholder", XML_ELEMENT_TAG);
  }

  public EmptyParams(String label) {
    super(label, XML_ELEMENT_TAG);
  }

  @Override
  public ParamSet getNewInstance() {
    return new EmptyParams();
  }

  @Override
  public ParamSet getCopyWithNewDate() {
    return new EmptyParams(super.getLabelParameter().getValue());
  }

  @Override
  public ParamSet getCopyWithPreviousDateFileAndID() {
    ParamSet params = new EmptyParams(super.getLabelParameter().getValue());
    params.getDateParameter().setValue(getDateCreatedAsString());
    params.getIdParameter().setValue(getIdParameter().getValue());
    if (associatedPathOnDrive != null) {
      params.setAssociatedFileOnDrive(associatedPathOnDrive.toPath());
    } else {
      params.setAssociatedFileOnDrive(null);
    }

    return params;
  }

  @Override
  public void fillFromXml(NodeList nodeList, Path file) {
    // Do nothing.
  }


  @Override
  public void executeSaveAs(Path file) {
    super.setAssociatedFileOnDrive(file);
    // Do nothing
  }

  @Override
  public AvailableParameterSets getEnum() {
    return AvailableParameterSets.EMPTY;
  }

}
