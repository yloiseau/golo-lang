/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

import java.util.Set;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptySet;

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

  private static class IfLongerSpliter implements Spliter {
    private static final int FIT_THRESHOLD = 2;

    private final Indent indenter;
    private final String separator;
    private final Set<Character> exceptions;
    private final StringBuilder current = new StringBuilder();
    private Stream.Builder<String> spans;
    private int width;
    private boolean recur = true;

    IfLongerSpliter(String sep, Set<Character> except, Indent indent) {
      separator = sep;
      exceptions = except;
      indenter = indent;
    }

    public IfLongerSpliter notRecursive() {
      recur = false;
      return this;
    }

    private void init(int width) {
      this.width = width;
      current.setLength(0);
      spans = Stream.builder();
    }

    private boolean dontFits(FormatingElement elt) {
      return width > 0
        && current.length() + nextLength(elt) > width + FIT_THRESHOLD;
    }

    private boolean separatorNeeded() {
      return current.length() > 0 && !exceptions.contains(lastChar());
    }

    private int nextLength(FormatingElement elt) {
      return elt.length()
             + indenter.length()
             + (separatorNeeded() ? separator.length() : 0);
    }

    private char lastChar() {
      return current.charAt(current.length() - 1);
    }

    private void flush() {
      if (current.length() != 0) {
        spans.add(current.toString());
        current.setLength(0);
      }
    }

    private void wrap(FormatingElement child) {
      flush();
      indenter.incr();
      if (recur) {
        child.split(width - indenter.length()).forEach((s) ->  {
          flush();
          current.append(indenter.toString() + s);
        });
      } else {
        current.append(indenter.toString() + child.toString());
      }
      indenter.decr();
    }


    private void append(FormatingElement child) {
      if (separatorNeeded()) {
        current.append(separator);
      }
      current.append(child);
    }

    @Override
    public Stream<String> apply(List<? extends FormatingElement> children, Integer width) {
      init(width);
      for (FormatingElement child : children) {
        if (dontFits(child)) {
          wrap(child);
        } else {
          append(child);
        }
      }
      flush();
      return spans.build();
    }
  }

  public static IfLongerSpliter ifLongerThan(String sep, Set<Character> except, Indent indent) {
    return new IfLongerSpliter(sep, except, indent);
  }

  public static IfLongerSpliter ifLongerThan(String sep, Set<Character> except) {
    return new IfLongerSpliter(sep, except, Indent.nullIndent());
  }

  public static IfLongerSpliter ifLongerThan(String sep) {
    return new IfLongerSpliter(sep, emptySet(), Indent.nullIndent());
  }

}

