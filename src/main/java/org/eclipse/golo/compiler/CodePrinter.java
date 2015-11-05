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
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;

import gololang.ir.GoloElement;
import gololang.ir.GoloIrVisitor;


import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

interface FormatingElement {
  int length();
  boolean isEmpty();
  void append(Object o);
  List<String> split(int width);
  void reset();
  char lastChar();
}

interface Spliter extends java.util.function.BiFunction<List<? extends FormatingElement>, Integer, List<String>> {

  static final Spliter SPLIT_ALL = (children, with) ->
    children.stream().map(Object::toString).collect(Collectors.toList());

}

class Span implements FormatingElement {
  StringBuilder text = new StringBuilder();

  @Override
  public int length() {
    return text.length();
  }

  @Override
  public boolean isEmpty() {
    return text.length() == 0;
  }

  @Override
  public void append(Object o) {
    text.append(o);
  }

  @Override
  public List<String> split(int width) {
    return singletonList(toString());
  }

  @Override
  public void reset() {
    text.setLength(0);
  }

  @Override
  public char lastChar() {
    return text.charAt(text.length() - 1);
  }

  @Override
  public String toString() {
    return text.toString();
  }
}

// TODO: strategies to split chunks
class Chunk implements FormatingElement {
  private LinkedList<FormatingElement> children = new LinkedList<>();
  private Spliter splitStrategy;

  Chunk() {
    this(Spliter.SPLIT_ALL);
  }

  Chunk(Spliter split) {
    splitStrategy = split;
  }

  @Override
  public int length() {
    return children.stream().mapToInt(e -> e.length() + 1).sum();
  }

  @Override
  public boolean isEmpty() {
    return children.isEmpty() || children.stream().allMatch(FormatingElement::isEmpty);
  }

  @Override
  public void append(Object o) {
    if (o instanceof FormatingElement) {
      addChildren((FormatingElement) o);
    } else if (!children.isEmpty()) {
      children.getLast().append(o);
    } else {
      throw new IllegalArgumentException("can't append " + o);
    }
  }

  @Override
  public List<String> split(int width) {
    if (width > 0 && length() > width) {
      return splitStrategy.apply(children, width);
    }
    return singletonList(toString());
  }

  @Override
  public void reset() {
    children.clear();
  }

  @Override
  public char lastChar() {
    return children.getLast().lastChar();
  }

  @Override
  public String toString() {
    return children.stream().map(Object::toString).collect(Collectors.joining(" "));
  }

  private void addChildren(FormatingElement e) {
    children.add(e);
  }
}

// TODO: replace StringBuilder with a Writer/PrintStream
// TODO: return this from methods to chain calls

public class CodePrinter {
  // TODO: configurable
  private int indent_size = 2;
  private char indent_char = ' ';
  private int maxLineLength = 120;
  private String linePrefix = "";
  private static final String NL = "\n";

  private int spacing = 0;
  private StringBuilder buffer = new StringBuilder();
  private FormatingElement currentLine = new Span();
  private Set<Character> noSpaceAfter = new HashSet<>();

  public void noSpaceAfter(Character... chars) {
    noSpaceAfter.addAll(asList(chars));
  }

  public void linePrefix(String prefix) {
    this.linePrefix = prefix;
  }

  public void reset() {
    spacing = 0;
    buffer.setLength(0);
    currentLine.reset();
  }

  public void print(Object o) {
    // TODO: add an object to the current span
    if (o != null) {
      String repr = o.toString();
      if (repr.endsWith(NL)) {
        currentLine.append(repr.trim());
        newline();
      } else {
        currentLine.append(repr);
      }
    }
  }

  private void printLines(Iterable<String> lines) {
    boolean first = true;
    for (String line : lines) {
      if (first) { first = false; }
      else { space(); }
      buffer.append(line);
      buffer.append(NL);
    }
  }

  public void newline() {
    // TODO: add a new span to the current chunk
    printLines(currentLine.split(maxLineLength));
    currentLine.reset();
  }

  public void newlineIfNeeded() {
    if (!currentLine.isEmpty()) {
      newline();
    }
  }

  public void blankLine() {
    // TODO: put the chunk in the buffer and create a new one
    newlineIfNeeded();
    if (buffer.length() >= (2 * NL.length())
        && !(NL + NL).equals(buffer.substring(buffer.length() - (2 * NL.length())))) {
      buffer.append(NL);
    }
  }

  public void space() {
    // TODO: add a space to the current span, indenting if needed
    if (currentLine.isEmpty()) {
      indent();
    }
    else if (!noSpaceAfter.contains(currentLine.lastChar())) {
      currentLine.append(' ');
    }
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
    currentLine.append(linePrefix);
    for (int i = 0; i < spacing; i++) {
      currentLine.append(indent_char);
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
    newlineIfNeeded();
    return buffer.toString();
  }
}
