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

import gui.dialog.FillCollection;
import gui.dialog.Fillable;
import gui.util.TextFormatterOption;
import gui.util.TextFormatterSupplier;
import io.XmlUtil;
import java.io.Serial;
import java.io.Serializable;
import javafx.scene.control.TextFormatter;
import org.apache.commons.text.StringEscapeUtils;
import org.w3c.dom.Element;

public class AutoFillParam<T extends Serializable>
    extends AbstractParameter<Fillable<T>>
    implements Serializable, Parameter<Fillable<T>> {

  @Serial
  private static final long serialVersionUID = 1_000_000L;

  private Fillable<T> value;
  private final FillCollection<T> suggestions;

  // Supplier allows to call the same method again. Otherwise, the same formatter will be used in various UI controls at the same time.
  private final TextFormatterOption textFormatterOption;

  public AutoFillParam(
      String label,
      String explanation,
      Fillable<T> value,
      FillCollection<T> suggestions,
      TextFormatterOption textFormatterOption,
      boolean isLimitedToExperts,
      String xmlLabel) {

    super(label, explanation, value, isLimitedToExperts, xmlLabel);

    this.value = value;
    this.suggestions = suggestions;
    this.textFormatterOption = textFormatterOption;
  }

  // quasi constructor
  public Parameter<Fillable<T>> copyWithoutChildren() {
    Parameter<Fillable<T>> copy = new AutoFillParam<>(
        super.getLabel(),
        super.getExplanation(),
        value,
        suggestions,
        textFormatterOption,
        super.isLimitedToExpert(),
        super.getXmlID());
    copy.setDefaultValue(getDefaultValue());
    return copy;
  }

  @Override
  public void readFromXmlElement(Element xmlElement) {
    super.readFromXmlElement(xmlElement);
    String value = xmlElement.getAttribute(XmlUtil.PAR_VALUE_ATTRIBUTE);
    String defaultVal = xmlElement.getAttribute(XmlUtil.PAR_DEFAULT_ATTRIBUTE);
    String isExpertStr = xmlElement.getAttribute(XmlUtil.PAR_EXPERT_ATTRIBUTE);

    if (value != null && !value.isEmpty()
        && defaultVal != null && !defaultVal.isEmpty()) {

      Fillable<T> valueMatch = suggestions.getMatch(StringEscapeUtils.unescapeXml(value),
          false);
      if (valueMatch != null) {
        setValue(valueMatch);
      }

      Fillable<T> defValMatch = suggestions.getMatch(StringEscapeUtils.unescapeXml(defaultVal),
          false);
      if (defValMatch != null) {
        setDefaultValue(defValMatch);
      }
    }

    if (isExpertStr != null && !isExpertStr.isEmpty() && isExpertStr.equals("true")) {
      setLimitedToExperts(true);
    }
  }

  @Override
  public Fillable<T> getValue() {
    return value;
  }

  @Override
  public String getValueAsString() {
    return value.getStringValue();
  }

  @Override
  public String getDefaultValueAsString() {
    return getDefaultValue().getStringValue();
  }

  @Override
  public void setValue(Fillable<T> val) {
    // LABELS should not be empty!
    if (val != null) {
      this.value = val;
    }
  }

  @Override
  public void trySetValue(Parameter<?> par) {
    if (par instanceof AutoFillParam) {
      try {
        Fillable<T> val = ((AutoFillParam<T>) par).getValue();
        setValue(val);
      }catch (Exception e){
        LOGGER.error("Cannot cast.");
      }
    }
  }

  @Override
  public FxParameter<Fillable<T>> getObservableInstance() {
    TextFormatter<String> formatter = TextFormatterSupplier.get(
        textFormatterOption,
        value.getStringValue());

    formatter.setValue(value.getStringValue());

    return new AutoFillFxParameter<T>(this, formatter);
  }

  @Override
  public boolean isEquivalent(Parameter<?> other) {
    boolean equivalent = other instanceof AutoFillParam;
    if (equivalent) {
      Fillable<T> thisValue = this.getValue();
      Fillable<?> thatValue = ((AutoFillParam<?>) other).getValue();
      equivalent = thisValue.isEqual(thatValue);
    }
    return equivalent;
  }

  public FillCollection<T> getSuggestions() {
    return suggestions;
  }
}