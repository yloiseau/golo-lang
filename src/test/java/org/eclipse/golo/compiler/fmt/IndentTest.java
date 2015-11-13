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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IndentTest {

  private Indent indent = new Indent(' ', 2);

  @BeforeMethod
  public void setUp() {
    indent = new Indent(' ', 2);
  }

  @Test
  public void test_empty() {
    assertThat(indent.length(), is(0));
    assertThat(indent.toString(), is(""));
  }

  @Test
  public void test_incr_decr() {
    indent.incr();
    assertThat(indent.length(), is(2));
    assertThat(indent.toString(), is("  "));
    indent.incr();
    assertThat(indent.length(), is(4));
    assertThat(indent.toString(), is("    "));
    indent.decr();
    assertThat(indent.length(), is(2));
    assertThat(indent.toString(), is("  "));
    indent.decr();
    assertThat(indent.length(), is(0));
    assertThat(indent.toString(), is(""));
  }

  @Test
  public void test_min_decr() {
    indent.decr();
    assertThat(indent.length(), is(0));
    assertThat(indent.toString(), is(""));
  }

  @Test
  public void test_empty_with_prefix() {
    indent.setLinePrefix("# ");
    assertThat(indent.length(), is(2));
    assertThat(indent.toString(), is("# "));
  }

  @Test
  public void test_incr_with_prefix() {
    indent.setLinePrefix("# ");
    indent.incr();
    assertThat(indent.length(), is(4));
    assertThat(indent.toString(), is("#   "));
  }

  @Test
  public void test_min_decr_with_prefix() {
    indent.setLinePrefix("# ");
    indent.decr();
    assertThat(indent.length(), is(2));
    assertThat(indent.toString(), is("# "));
  }

  @Test
  public void test_reset() {
    indent.incr();
    indent.incr();
    indent.incr();
    indent.reset();
    assertThat(indent.length(), is(0));
  }

  @Test
  public void test_incr_nb() {
    indent.incr(3);
    assertThat(indent.length(), is(6));
  }

  @Test
  public void test_decr_nb() {
    indent.incr(); indent.incr(); indent.incr();
    indent.decr(2);
    assertThat(indent.length(), is(2));
  }

  @Test
  public void test_decr_nb_max() {
    indent.incr(2);
    indent.decr(4);
    assertThat(indent.length(), is(0));
  }
}
