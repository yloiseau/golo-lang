/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler.utils;

public final class StringUnescaping {

  private StringUnescaping() {
    //utility class
  }

  public static String escape(String str) {
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      switch (ch) {
        case '\\':
          sb.append("\\\\");
          break;
        case '\b':
          sb.append("\\b");
          break;
        case '\f':
          sb.append("\\f");
          break;
        case '\n':
          sb.append("\\n");
          break;
        case '\r':
          sb.append("\\r");
          break;
        case '\t':
          sb.append("\\t");
          break;
        case '\"':
          sb.append("\\\"");
          break;
        default:
          sb.append(ch);
          break;
      }
    }
    return sb.toString();
  }

  public static String unescape(String str) {
    StringBuilder sb = new StringBuilder(str.length());
    for (int i = 0; i < str.length(); i++) {
      char ch = str.charAt(i);
      if (ch == '\\') {
        char nextChar = (i == str.length() - 1) ? '\\' : str.charAt(i + 1);
        switch (nextChar) {
          case 'u':
            ch = (char) Integer.parseInt(str.substring(i + 2, i + 6), 16);
            i += 4;
            break;
          case '\\':
            ch = '\\';
            break;
          case 'b':
            ch = '\b';
            break;
          case 'f':
            ch = '\f';
            break;
          case 'n':
            ch = '\n';
            break;
          case 'r':
            ch = '\r';
            break;
          case 't':
            ch = '\t';
            break;
          case '\"':
            ch = '\"';
            break;
          case '\'':
            ch = '\'';
            break;
          default:
            // not a special char, do nothing
            break;
        }
        i++;
      }
      sb.append(ch);
    }
    return sb.toString();
  }
}
