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

package gui.table;

import gui.listAndSearch.FxWrapper;

/**
 * Sole purpose of this class: Assume that there is a Class that does not have UI fields such as
 * StringProperties. We wrap this class in this interface's instances. These instances have the
 * respective properties and also know how to SET the new value in the original class. Note, in
 * contrast to the list view, we do not need as many special cases such as "isSelected" and so on.
 * Why? For the table, we need a very specific implementation for each wrapped class anyway. Hence,
 * we can organize these cases in a custom factory for each wrapped class.
 */

public interface FxTableEntry<T> extends FxWrapper {

  T unwrap();

  boolean isEqualWrappedObject(FxWrapper fxWrapper);

}
