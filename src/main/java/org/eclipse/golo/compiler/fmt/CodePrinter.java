/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

import java.util.Deque;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.stream.Stream;
import java.util.regex.Pattern;

import gololang.ir.GoloElement;
import gololang.ir.GoloIrVisitor;

// TODO: replace StringBuilder with a Writer/PrintStream
// TODO: return this from methods to chain calls

import static java.util.Arrays.asList;

public class CodePrinter {

  private static final Pattern LINE_SPLITER = Pattern.compile(" *(\r\n|\n|\r)");

  // TODO: configurable
  private Indent indent = new Indent(' ', 2);
  private int maxLineLength = 80;
  private static final String NL = "\n";
  private boolean mustIndent = true;

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
    indent.setLinePrefix(prefix);
  }

  public void reset() {
    indent.reset();
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
    innerChunks.push(Chunk.wrapping(" ", noSpaceAfter, indent));
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
    // System.err.println("---------------------------------------------------");
    // printDump(chunk, "");
    // System.err.println("---------------------------------------------------");
    if (!chunk.isEmpty()) {
      if (buffer.length() > 0) {
        buffer.append(NL);
      }
      printLines(chunk.split(maxLineLength));
    }
    resetChunk();
  }

  // XXX: debug function
  private static void printDump(FormatingElement element, String prefix) {
    if (element instanceof Span) {
      System.err.println(prefix + element.toString());
    } else {
      for (FormatingElement elt : element.children()) {
        printDump(elt, prefix + "┆  ");
      }
    }
  }

  private boolean mustPutSpace() {
    return !currentLine().isEmpty() && !noSpaceAfter.contains(currentLine().lastChar());
  }

  public void space() {
    if (mustIndent) {
      indent();
    } else if (mustPutSpace()) {
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
    print(indent);
    mustIndent = false;
  }

  public void incr() {
    indent.incr();
  }

  public void decr() {
    indent.decr();
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
    innerChunk(Chunk.wrapping(" ", noSpaceAfter, indent));
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
      addBreak();
      element.accept(visitor);
    }
  }

  public String toString() {
    blankLine();
    return buffer.toString();
  }
}
