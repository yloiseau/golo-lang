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

public class ChunkTest {

  private Chunk chunk;

  @BeforeMethod
  public void setUp() {
    chunk = new Chunk();
  }

  @Test
  public void test_empty() {
    assertThat(chunk.isEmpty(), is(true));
    chunk.append(Span.empty());
    chunk.append(Chunk.topLevel());
    Chunk tmp = Chunk.topLevel();
    tmp.append(Span.empty());
    tmp.append(Chunk.topLevel());
    chunk.append(tmp);
    chunk.append(Span.empty());
    assertThat(chunk.isEmpty(), is(true));
  }

  @Test
  public void test_append_spans_and_text() {
    chunk.append(Span.empty());
    chunk.append(4);
    chunk.append(2);
    chunk.append(Span.of("is the"));
    chunk.append(new Chunk().append(Span.of("answer")));
    assertThat(chunk.toString(), is("42 is the answer"));
  }

  @Test
  public void test_append_empty() {
    chunk.append(Span.empty());
    chunk.append(Span.of(42));
    assertThat(chunk.children().size(), is(1));

    chunk.reset();
    chunk.append(new Chunk());
    chunk.append(new Chunk().append(Span.of("42")));
    assertThat(chunk.children().size(), is(1));

    chunk.reset();
    chunk.append(Span.empty());
    chunk.append(new Chunk());
    assertThat(chunk.children().size(), is(2));
    
    chunk.reset();
    chunk.append(new Chunk());
    chunk.append(Span.empty());
    assertThat(chunk.children().size(), is(2));
  }

  @Test(expectedExceptions = IllegalArgumentException.class)
  public void test_append_error() {
    chunk.append("foo");
  }

  @Test
  public void test_lastChar() {
    chunk.append(Span.of("42"));
    assertThat(chunk.lastChar(), is('2'));
    chunk.append(Span.of("1337"));
    assertThat(chunk.lastChar(), is('7'));
    Chunk tmp = new Chunk();
    tmp.append(Span.of("foo"));
    chunk.append(tmp);
    assertThat(chunk.lastChar(), is('o'));
  }

  @Test(expectedExceptions = IndexOutOfBoundsException.class)
  public void test_lastChar_empty() {
    chunk.lastChar();
  }

  @Test
  public void test_reset() {
    chunk
      .append(Span.of("42"))
      .append(Span.of("is the"))
      .append(new Chunk().append(Span.of("answer")));
    assertThat(chunk.isEmpty(), is(false));
    chunk.reset();
    assertThat(chunk.isEmpty(), is(true));
  }
}
