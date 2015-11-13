/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.fmt;


import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpanTest {

  private Span span;

  @BeforeMethod
  public void setUp() {
    span = Span.empty();
  }

  @Test
  public void test_empty() {
    assertThat(span.length(), is(0));
    assertThat(span.isEmpty(), is(true));
    assertThat(span.toString(), is(""));
  }

  @Test
  public void test_append() {
    span.append("a");
    span.append("bc");
    span.append("d");
    assertThat(span.length(), is(4));
    assertThat(span.toString(), is("abcd"));
    assertThat(span.isEmpty(), is(false));
  }

  @Test
  public void test_lastChar() {
    span.append("abc");
    assertThat(span.lastChar(), is('c'));
    span.append("e");
    assertThat(span.lastChar(), is('e'));
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void test_lastChar_empty() {
    span.lastChar();
  }

  @Test
  public void test_factory() {
    Span s = Span.of("a", "bc", "d");
    assertThat(s.length(), is(4));
    assertThat(s.toString(), is("abcd"));
  }

  @Test
  public void test_children() {
    assertThat(span.children(), is(empty()));
    span.append("foo");
    assertThat(span.children(), is(empty()));
  }

  @Test
  public void test_reset() {
    span.append("foo bar");
    span.reset();
    assertThat(span.length(), is(0));
    assertThat(span.isEmpty(), is(true));
    assertThat(span.toString(), is(""));
  }

  @Test
  public void test_split() {
    span.append("abc");
    span.append("de fgh");
    assertThat(span.split(0).collect(toList()), contains("abcde fgh"));
  }

}
