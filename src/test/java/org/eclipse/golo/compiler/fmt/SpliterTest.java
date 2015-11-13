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

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import static java.util.Collections.emptySet;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.MatcherAssert.assertThat;

public class SpliterTest {

  @Test
  public void test_simple_split_all() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    assertThat(Spliters.splitAll().apply(content, 0).collect(toList()),
        contains("aaa", "bbb", "ccc"));
    assertThat(Spliters.splitAll().apply(content, 50).collect(toList()),
        contains("aaa", "bbb", "ccc"));
  }

  @Test
  public void test_simple_join_all() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    assertThat(Spliters.joinAll(" ").apply(content, 0).collect(toList()),
        contains("aaa bbb ccc"));
    assertThat(Spliters.joinAll(" ").apply(content, 50).collect(toList()),
        contains("aaa bbb ccc"));
    assertThat(Spliters.joinAll("").apply(content, 10).collect(toList()),
        contains("aaabbbccc"));
  }

  @Test
  public void test_simple_longer_nosplit() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    Spliter s = Spliters.ifLongerThan(" ",
        new HashSet<Character>(asList(' ')));

    assertThat(s.apply(content, 30).collect(toList()),
        contains("aaa bbb ccc"));
    assertThat(s.apply(content, 11).collect(toList()),
        contains("aaa bbb ccc"));
    // 2 char tolerance
    assertThat(s.apply(content, 9).collect(toList()),
        contains("aaa bbb ccc"));
    
    assertThat(s.apply(asList(Span.of("aaa "), Span.of("bbb "), Span.of("ccc")), 50).collect(toList()),
        contains("aaa bbb ccc"));

    assertThat(s.apply(content, 0).collect(toList()),
        contains("aaa bbb ccc"));
  }
    
  @Test
  public void test_simple_longer_split_all() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    Spliter s = Spliters.ifLongerThan(" ",
        new HashSet<Character>(asList(' ')));
    assertThat(s.apply(content, 1).collect(toList()),
        contains("aaa", "bbb", "ccc"));
    assertThat(s.apply(content, 3).collect(toList()),
        contains("aaa", "bbb", "ccc"));
    assertThat(s.apply(content, 4).collect(toList()),
        contains("aaa", "bbb", "ccc"));
  }

  @Test
  public void test_simple_longer_split_some() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    Spliter s = Spliters.ifLongerThan(" ",
        new HashSet<Character>(asList(' ')));

    // we allow 2 char beyond the limit
    assertThat(s.apply(content, 5).collect(toList()),
        contains("aaa bbb", "ccc"));
    assertThat(s.apply(content, 6).collect(toList()),
        contains("aaa bbb", "ccc"));
    assertThat(s.apply(content, 7).collect(toList()),
        contains("aaa bbb", "ccc"));
    assertThat(s.apply(content, 8).collect(toList()),
        contains("aaa bbb", "ccc"));
  }

  @Test
  public void test_longer_split_with_indent() {
    List<Span> content = asList(Span.of("aaa"), Span.of("bbb"), Span.of("ccc"));
    Spliter s = Spliters.ifLongerThan(" ",
        new HashSet<Character>(asList(' ')), new Indent(' ', 2));

    assertThat(s.apply(content, 11).collect(toList()),
        contains("aaa bbb ccc"));

    assertThat(s.apply(content, 4).collect(toList()),
        contains("aaa", "  bbb", "  ccc"));

    assertThat(s.apply(content, 7).collect(toList()),
        contains("aaa bbb", "  ccc"));
  }

  @Test
  public void test_complex_split() {
    Indent indent = new Indent(' ', 2);
    Set<Character> nospace = new HashSet<Character>(asList(' '));
    Chunk content = Chunk.wrapping(" ", nospace, indent)
      .append(Chunk.wrapping(" ", nospace, indent)
          .append(Span.of("aaa"))
          .append(Span.of("bbb")))
      .append(Chunk.wrapping(" ", nospace, indent)
          .append(Span.of("ccc"))
          .append(Span.of("ddd")))
      .append(Chunk.wrapping(" ", nospace, indent)
          .append(Span.of("eee"))
          .append(Span.of("fff")));

    assertThat(content.split(30).collect(toList()),
        contains(
          "aaa bbb ccc ddd eee fff"));

    assertThat(content.split(20).collect(toList()),
        contains(
          "aaa bbb ccc ddd",
          "  eee fff"));

    assertThat(content.split(10).collect(toList()),
        contains(
          "aaa bbb",
          "  ccc ddd",
          "  eee fff"));

    assertThat(content.split(5).collect(toList()),
        contains(
          "aaa",
          "  bbb",
          "ccc",
          "  ddd",
          "eee",
          "  fff"));
  }
}
