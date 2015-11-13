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
import java.util.Collections;
import java.util.stream.Stream;

final class Span implements FormatingElement {
  StringBuilder text = new StringBuilder();

  private Span() { }

  @Override
  public int length() {
    return text.length();
  }

  @Override
  public boolean isEmpty() {
    return text.length() == 0;
  }

  @Override
  public Span append(Object o) {
    text.append(o);
    return this;
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
    text.setLength(0);
  }

  @Override
  public char lastChar() throws IndexOutOfBoundsException {
    return text.charAt(text.length() - 1);
  }

  @Override
  public String toString() {
    return text.toString();
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


