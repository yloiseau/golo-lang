
/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.types;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FunctionTypeTest {

  @Test
  public void test_generic() {
    FunctionType t = FunctionType.generic(0, false);
    assertThat(t.getParamTypes().size(), is(0));
    assertThat(t.getReturnedType(), is(Value.of(Object.class)));

    t = FunctionType.generic(3, true);
    assertThat(t.getParamTypes().size(), is(3));
    assertThat(t.getParamTypes().get(0), is(Value.of(Object.class)));
    assertThat(t.getParamTypes().get(1), is(Value.of(Object.class)));
    assertThat(t.getParamTypes().get(2), is(Value.of(Object[].class)));
    assertThat(t.getReturnedType(), is(Value.of(Object.class)));
  }

  @Test
  public void test_of() {
    FunctionType t = FunctionType.of(Value.of(Boolean.class), Value.of(Integer.class), Value.of(String.class));
    assertThat(t.getParamTypes().size(), is(2));
    assertThat(t.getParamTypes().get(0), is(Value.of(Integer.class)));
    assertThat(t.getParamTypes().get(1), is(Value.of(String.class)));
    assertThat(t.getReturnedType(), is(Value.of(Boolean.class)));
  }

  @Test
  public void test_descriptor() {
    FunctionType t = FunctionType.of(Value.of(Boolean.class), Value.of(Integer.class), Value.of(String.class));
    assertThat(t.toDescriptor(), is("(Ljava/lang/Integer;Ljava/lang/String;)Ljava/lang/Boolean;"));

    t = FunctionType.generic(1, true);
    assertThat(t.toDescriptor(), is("([Ljava/lang/Object;)Ljava/lang/Object;"));
  }

}
