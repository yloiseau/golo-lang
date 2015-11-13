/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

final class Indent {

  private static final Indent NULL = new Indent(' ', 0);

  private final char indentChar;
  private final int indentSize;
  private String linePrefix;
  private int level = 0;

  public Indent(char indentChar, int indentSize, String linePrefix) {
    this.indentChar = indentChar;
    this.indentSize = indentSize;
    setLinePrefix(linePrefix);
  }

  public Indent(char indentChar, int indentSize) {
    this(indentChar, indentSize, "");
  }

  public void setLinePrefix(String prefix) {
    linePrefix = prefix;
  }

  public Indent incr() {
    return incr(1);
  }

  public Indent incr(int nb) {
    level += indentSize * nb;
    return this;
  }

  public Indent decr() {
    return decr(1);
  }

  public Indent decr(int nb) {
    level = Math.max(0, level - (indentSize * nb));
    return this;
  }

  public int length() {
    return linePrefix.length() + level;
  }

  public Indent reset() {
    level = 0;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder indent = new StringBuilder(linePrefix);
    for (int i = 0; i < level; i++) {
      indent.append(indentChar);
    }
    return indent.toString();
  }

  public static Indent of(Indent other) {
    Indent i = new Indent(other.indentChar, other.indentChar, other.linePrefix);
    i.level = other.level;
    return i;
  }

  public static Indent nullIndent() {
    return NULL;
  }
}
