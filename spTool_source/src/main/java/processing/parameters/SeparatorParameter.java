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

package processing.parameters;

import java.io.Serial;
import java.io.Serializable;
import org.w3c.dom.Element;

public class SeparatorParameter extends AbstractParameter<String> implements Serializable,
    Parameter<String> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  public SeparatorParameter() {
    super("", "", "", false, "sepPar");
  }

  // quasi constructor
  public Parameter<String> copyWithoutChildren() {
return new SeparatorParameter();
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
  }

  @Override
  public String getValue() {
    return "";
  }


  @Override
  public String getValueAsString() {
    return "";
  }

  @Override
  public String getDefaultValueAsString() {
    return "";
  }

  @Override
  public void setValue(String s) {
    // Do nothing
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    //
  }

  @Override
  public FxParameter<String> getObservableInstance() {
    return new SeparatorFxParameter(this);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof SeparatorParameter;
    return equivalent;
  }
}

