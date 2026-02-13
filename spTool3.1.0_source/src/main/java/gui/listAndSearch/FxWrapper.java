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

package gui.listAndSearch;

/**
 * A very general interface to represent/fill/filter/compare items in a ListAndSearchView when
 * looking at wrapped items. Note that if an object is wrapped in FxEntry or sth alike, we cannot
 * call isEquivalent() on the object as we do not know its type. Neither can we call equal() on the
 * wrapper as the wrappers are sometimes instantiated again and so different wrappers may store the
 * same object. Hence, all wrapper need this method.
 */

public interface FxWrapper {

  boolean isEqualWrappedObject(FxWrapper fxWrapper);

}
