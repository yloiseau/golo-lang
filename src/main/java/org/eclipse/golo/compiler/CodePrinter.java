/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler;

import java.util.Deque;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import gololang.ir.GoloElement;
import gololang.ir.GoloIrVisitor;


import static java.util.Arrays.asList;

interface FormatingElement {
  int length();
  boolean isEmpty();
  void append(Object o);
  Stream<String> split(int width);
  void reset();
  char lastChar();
  Object dump();
}

interface Spliter extends java.util.function.BiFunction<List<? extends FormatingElement>, Integer, Stream<String>> { }

final class Spliters {
  private Spliters() {
    // utility class
  }

  private static final Spliter SPLIT_ALL = (children, width) ->
    children.stream()
      .filter((e) -> !e.isEmpty())
      .flatMap((e) -> e.split(width));


  public static Spliter splitAll() { return SPLIT_ALL; }

  public static Spliter joinAll(String sep) {
    return (children, width) -> Stream.of(
        children.stream().map(Object::toString).collect(Collectors.joining(sep)));
  }

  public static Spliter ifLongerThan(String sep, Set<Character> except) {
    return (children, width) -> {
      List<String> spans = new LinkedList<>();
      StringBuilder current = new StringBuilder();
      for (FormatingElement child : children) {
        if (width > 0 && (current.length() + child.length()) > width) {
          spans.add(current.toString());
          current.setLength(0);
        }
        if (current.length() > 0 && !except.contains(current.charAt(current.length() - 1))) {
          current.append(sep);
        }
        current.append(child);
      }
      if (current.length() > 0) {
        spans.add(current.toString());
      }
      return spans.stream();
    };
  }
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
  public Stream<String> split(int width) {
    return Stream.of(toString());
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

  @Override
  public Object dump() {
    return '"' + toString() + '"';
  }

  public static Span empty() {
    return new Span();
  }

  public static Span of(Object... objects) {
    Span s = new Span();
    for (Object o : objects) {
      s.append(o);
    }
    return s;
  }
}

// TODO: strategies to split chunks
class Chunk implements FormatingElement {
  private LinkedList<FormatingElement> children = new LinkedList<>();
  private Spliter splitStrategy;

  Chunk() {
    this(Spliters.splitAll());
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
  public Stream<String> split(int width) {
    return splitStrategy.apply(children, width);
  }

  @Override
  public void reset() {
    children.clear();
  }

  @Override
  public char lastChar() {
    for (int i = children.size() - 1; i >= 0; i--) {
      if (!children.get(i).isEmpty()) {
        return children.get(i).lastChar();
      }
    }
    throw new IndexOutOfBoundsException("Empty chunk");
  }

  @Override
  public String toString() {
    return children.stream().map(Object::toString).collect(Collectors.joining(" "));
  }

  @Override
  public Object dump() {
    return children.stream().map(FormatingElement::dump).collect(Collectors.toList());
  }

  private void removeLastIfEmpty(FormatingElement e) {
    if (!children.isEmpty()) {
      FormatingElement last = children.getLast();
      if (last.getClass() == e.getClass() && last.isEmpty()) {
        children.removeLast();
      }
    }
  }

  private void addChildren(FormatingElement e) {
    removeLastIfEmpty(e);
    children.add(e);
  }

  public static Chunk topLevel() {
    return new Chunk(Spliters.splitAll());
  }

  public static Chunk wrapping(String separator, Set<Character> exceptions) {
    return new Chunk(Spliters.ifLongerThan(separator, exceptions));
  }

  public static Chunk verbatim() {
    return new Chunk(Spliters.joinAll(""));
  }
}

// TODO: replace StringBuilder with a Writer/PrintStream
// TODO: return this from methods to chain calls

public class CodePrinter {
  // TODO: configurable
  private int indentSize = 2;
  private char indentChar = ' ';
  private int maxLineLength = 80;
  private String linePrefix = "";
  private static final String NL = "\n";
  private boolean mustIndent = true;

  private static final Pattern LINE_SPLITER = Pattern.compile(" *(\r\n|\n|\r)");
  private int spacing = 0;
  private StringBuilder buffer = new StringBuilder();
  private FormatingElement chunk = Chunk.topLevel();
  private Deque<Chunk> innerChunks = new LinkedList<>();
  private Set<Character> noSpaceAfter = new HashSet<>();

  public CodePrinter() {
    reset();
  }

  private FormatingElement currentLine() {
    return innerChunks.peek();
  }

  private void innerChunk(Chunk inner) {
    currentLine().append(inner);
    innerChunks.push(inner);
  }

  private Chunk exitInner() {
    return innerChunks.pop();
  }

  public void noSpaceAfter(Character... chars) {
    noSpaceAfter.addAll(asList(chars));
  }

  public void linePrefix(String prefix) {
    this.linePrefix = prefix;
  }

  public void reset() {
    spacing = 0;
    buffer.setLength(0);
    resetChunk();
  }

  private void resetChunk() {
    innerChunks.clear();
    chunk.reset();
    newline();
  }

  public void print(Object o) {
    if (o != null) {
      String repr = o.toString();
      if (repr.endsWith(NL)) {
        currentLine().append(repr.trim());
        newline();
      } else {
        currentLine().append(repr);
      }
    }
  }

  private void printLines(Stream<String> lines) {
    lines.forEach((line) -> {
        buffer.append(line);
        buffer.append(NL);
      });
  }

  public void printMultiLines(String txt) {
    // FIXME: try to wrap when too long...
    List<String> lines = asList(LINE_SPLITER.split(txt.trim()));
    // innerChunk(Chunk.verbatim());
    // addBreak();
    for (String line : lines) {
      if (line.isEmpty()) {
        currentLine().append(NL);
      } else {
        space();
        println(line);
      }
    }
    // currentLine().append(NL);
    // addBreak();
    // exitInner();
  }

  public void newline() {
    if (currentLine() != null) {
      chunk.append(innerChunks.pop());
    }
    innerChunks.push(Chunk.wrapping(" ", noSpaceAfter));
    addBreak();
    mustIndent = true;
  }

  public void addBreak() {
    currentLine().append(Span.empty());
  }

  public void newlineIfNeeded() {
    if (!currentLine().isEmpty()) {
      newline();
    }
  }

  public void blankLine() {
    newlineIfNeeded();
    if (!chunk.isEmpty()) {
      if (buffer.length() > 0) {
        buffer.append(NL);
      }
      printLines(chunk.split(maxLineLength));
    }
    resetChunk();
  }

  public void space() {
    if (mustIndent) {
      indent();
    } else if (!noSpaceAfter.contains(currentLine().lastChar())) {
      print(' ');
    }
  }

  public void println(Object s) {
    print(s);
    newline();
  }

  public void printf(String format, Object... values) {
    print(String.format(format, values));
  }

  private void indent() {
    print(linePrefix);
    for (int i = 0; i < spacing; i++) {
      print(indentChar);
    }
    mustIndent = false;
  }

  public void incr() {
    spacing = spacing + indentSize;
  }

  public void decr() {
    spacing = Math.max(0, spacing - indentSize);
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

  public void beginSoftBlock(String delim) {
    print(delim);
    innerChunk(Chunk.wrapping(" ", noSpaceAfter));
    addBreak();
  }

  public void endSoftBlock(String delim) {
    print(delim);
    exitInner();
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
    blankLine();
    return buffer.toString();
  }
}
