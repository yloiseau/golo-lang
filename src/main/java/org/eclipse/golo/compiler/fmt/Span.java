/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;

import java.util.List;
import java.util.LinkedList;
import java.util.Collections;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * A Span is a formating element that can't be splitted.
 */
final public class Span implements FormatingElement {
  private LinkedList<Element> elements = new LinkedList<>();

  private Span() { }

  @Override
  public int length() {
    return elements.stream().mapToInt(Element::length).sum() + elements.size() - 1;
  }

  @Override
  public boolean isEmpty() {
    return elements.isEmpty();
  }

  @Override
  public Span append(Object o) {
    if (o instanceof Element) {
      elements.add((Element) o);
      return this;
    }
    throw new IllegalArgumentException("can't add " + o);
  }

  @Override
  public List<FormatingElement> children() {
    return Collections.emptyList();
  }

  @Override
  public Stream<String> split(int width) {
    return Stream.of(toString());
  }

  @Override
  public void reset() {
    elements.clear();
  }

  @Override
  public char lastChar() throws IndexOutOfBoundsException {
    String v = elements.getLast().toString();
    return v.charAt(v.length() - 1);
  }

  @Override
  public String toString() {
    return elements.stream().map(Element::toString).collect(Collectors.joining(" "));
  }

  public static Span empty() {
    return new Span();
  }

  public static Span of(Element... objects) {
    Span s = new Span();
    for (Element e : objects) {
      s.append(e);
    }
    return s;
  }
}


