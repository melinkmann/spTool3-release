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

package util;


// chatGPT
public class Key3 implements MultiKey {
  public final String s1;
  public final String s2;
  public final String s3;

  public Key3(String s1, String s2, String s3) {
    this.s1 = s1;
    this.s2 = s2;
    this.s3 = s3;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Key3)) return false;
    Key3 k = (Key3) o;
    return s1.equals(k.s1)
        && s2.equals(k.s2)
        && s3.equals(k.s3);
  }

  @Override
  public int hashCode() {
    int h = s1.hashCode();
    h = 31 * h + s2.hashCode();
    h = 31 * h + s3.hashCode();
    return h;
  }
}