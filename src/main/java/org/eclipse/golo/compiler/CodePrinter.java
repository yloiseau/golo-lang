/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler;

import java.util.Set;
import java.util.HashSet;
import gololang.ir.GoloElement;
import gololang.ir.GoloIrVisitor;

import static java.util.Arrays.asList;

// TODO: replace StringBuilder with a Writer/PrintStream
// TODO: return this from methods to chain calls

public class CodePrinter {
  // TODO: configurable
  private int indent_size = 2;
  private char indent_char = ' ';
  private int spacing = 0;
  private String linePrefix = "";
  private StringBuilder buffer = new StringBuilder();
  private Set<Character> noSpaceAfter = new HashSet<>();

  public void noSpaceAfter(Character... chars) {
    noSpaceAfter.addAll(asList(chars));
  }

  public void linePrefix(String prefix) {
    this.linePrefix = prefix;
  }

  public void reset() {
    spacing = 0;
    buffer = new StringBuilder();
  }

  public void print(Object o) {
    String repr = o.toString();
    if (repr.endsWith("\n")) {
      this.buffer.append(repr.trim());
      newline();
    } else {
      this.buffer.append(repr);
    }
  }

  public void newline() {
    this.buffer.append('\n');
  }

  private char lastChar(int offset) {
    if (this.buffer.length() == 0) { return Character.MIN_VALUE; }
    int idx = Math.max(0, this.buffer.length() - (offset + 1));
    return this.buffer.charAt(idx);
  }

  private boolean endsWith(int offset, char c) {
    return (this.buffer.length() > offset
            && lastChar(offset) == c);
  }

  public void newlineIfNeeded() {
    if (!endsWith(0, '\n')) {
      newline();
    }
  }

  public void blankLine() {
    newlineIfNeeded();
    if (!endsWith(1, '\n')) {
      newline();
    }
  }

  public void space() {
    char last = lastChar(0);
    if (last == Character.MIN_VALUE || last == '\n') { indent(); }
    else if (!noSpaceAfter.contains(last)) { print(' '); }
  }

  public void println(Object s) {
    print(s);
    newline();
  }

  public void printf(String format, Object... values) {
    print(String.format(format, values));
  }

  public void printMultiline(String txt) {
    for (String line : asList(txt.trim().split("\r\n|\r|\n"))) {
      space();
      println(line.trim());
    }
  }

  private void indent() {
    buffer.append(linePrefix);
    for (int i = 0; i < spacing; i++) {
      buffer.append(indent_char);
    }
  }

  public void incr() {
    spacing = spacing + indent_size;
  }

  public void decr() {
    spacing = Math.max(0, spacing - indent_size);
  }

  public void beginBlock(String delim) {
    println(delim);
    incr();
  }

  public void endBlock(String delim) {
    newlineIfNeeded();
    decr();
    indent();
    print(delim);
  }

  public void join(Iterable<? extends Object> elements, String separator) {
    boolean first = true;
    for (Object elt : elements) {
      if (first) {
        first = false;
      } else {
        print(separator);
      }
      space();
      print(elt);
    }
  }

  public void joinedVisit(GoloIrVisitor visitor, Iterable<? extends GoloElement<?>> elements, String separator) {
    boolean first = true;
    for (GoloElement<?> element : elements) {
      if (first) {
        first = false;
      } else {
        print(separator);
      }
      space();
      element.accept(visitor);
    }
  }

  public String toString() {
    return buffer.toString();
  }
}
