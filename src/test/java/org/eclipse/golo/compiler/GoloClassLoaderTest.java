/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler;

import org.testng.annotations.Test;

import java.io.FileInputStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

public class GoloClassLoaderTest {

  private static final String SRC = "src/test/resources/for-execution/";

  @Test
  public void check_load() throws Throwable {
    GoloClassLoader classLoader = new GoloClassLoader();
    Class<?> clazz = classLoader.load("returns.golo", new FileInputStream(SRC + "returns.golo"));
    assertThat(clazz, notNullValue());
    assertThat(clazz.getName(), is("golotest.execution.FunctionsWithReturns"));
  }

  @Test
  public void loading_twice_returns_first() throws Throwable {
    GoloClassLoader classLoader = new GoloClassLoader();
    Class<?> first = classLoader.load("returns.golo", new FileInputStream(SRC + "returns.golo"));
    assertThat(first, notNullValue());
    Class<?> other = classLoader.load("returns.golo", new FileInputStream(SRC + "returns.golo"));
    assertThat(other, sameInstance(first));
  }
}
