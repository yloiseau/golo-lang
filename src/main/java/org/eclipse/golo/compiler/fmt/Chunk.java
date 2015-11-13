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
import java.util.LinkedList;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Chunk implements FormatingElement {
  private LinkedList<FormatingElement> children = new LinkedList<>();
  private Spliter splitStrategy;

  public Chunk() {
    this(Spliters.splitAll());
  }

  public Chunk(Spliter spliter) {
    setSplitStrategy(spliter);
  }

  public void setSplitStrategy(Spliter spliter) {
    splitStrategy = spliter;
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
  public Chunk append(Object o) {
    if (o instanceof FormatingElement) {
      addChildren((FormatingElement) o);
    } else if (!children.isEmpty()) {
      children.getLast().append(o);
    } else {
      throw new IllegalArgumentException("can't append " + o);
    }
    return this;
  }

  @Override
  public List<FormatingElement> children() {
    return Collections.unmodifiableList(children);
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

  public static Chunk wrapping(String separator, Set<Character> exceptions, Indent indent) {
    return new Chunk(Spliters.ifLongerThan(separator, exceptions, indent));
  }

  public static Chunk verbatim() {
    return new Chunk(Spliters.joinAll(""));
  }
}
