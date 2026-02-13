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

package io;

public class HexStringWrapper {


  public static String str2Hex(String string) {
    StringBuffer sb = new StringBuffer();
    //Converting string to character array
    char ch[] = string.toCharArray();
    for (int i = 0; i < ch.length; i++) {
      String hexString = Integer.toHexString(ch[i]);
      sb.append(hexString);
    }
    String result = sb.toString();
    return result;
  }

  public static String hex2Str(String string) {
    String result = new String();
    char[] charArray = string.toCharArray();
    for (int i = 0; i < charArray.length; i = i + 2) {
      String st = "" + charArray[i] + "" + charArray[i + 1];
      char ch = (char) Integer.parseInt(st, 16);
      result = result + ch;
    }
    return result;
  }

}



