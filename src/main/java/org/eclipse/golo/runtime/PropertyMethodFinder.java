/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.runtime;

import gololang.Union;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.Objects;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;

public class PropertyMethodFinder extends MethodFinder {

  private static final MethodHandle FLUENT_SETTER;

  static {
    try {
      FLUENT_SETTER = lookup().findStatic(
          PropertyMethodFinder.class,
          "fluentSetter",
          methodType(Object.class, Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("Could not bootstrap the required fluent method handles", e);
    }
  }

  private static Object fluentSetter(Object o, Object notUsedSetterReturnValue) {
    return o;
  }

  private String propertyName;

  public PropertyMethodFinder(MethodInvocation invocation) {
    super(invocation);
    this.propertyName = capitalize(invocation.name());
  }

  private MethodHandle findMethodForGetter() {
    if (Union.class.isAssignableFrom(invocation.receiverClass())) {
      return null;
    }
    MethodHandle target = new RegularMethodFinder(invocation.withName("get" + propertyName)).find();

    if (target != null) {
      return target;
    }
    return new RegularMethodFinder(invocation.withName("is" + propertyName)).find();
  }

  private MethodHandle fluentMethodHandle(Method candidate) {
    Objects.requireNonNull(candidate);
    MethodHandle target = toMethodHandle(candidate).orElse(null);
    if (target != null) {
      if (!TypeMatching.returnsValue(candidate)) {
        Object receiver = invocation.arguments()[0];
        MethodHandle fluent = FLUENT_SETTER.bindTo(receiver);
        target = filterReturnValue(target, fluent);
      }
    }
    return target;
  }

  private MethodHandle findMethodForSetter() {
    return new RegularMethodFinder(invocation.withName("set" + propertyName))
        .findInMethods()
        .filter(method -> !Union.class.isAssignableFrom(method.getDeclaringClass()))
        .map(this::fluentMethodHandle)
        .findFirst()
        .orElse(null);
  }

  @Override
  public MethodHandle find() {
    if (invocation.arity() == 1) {
      return findMethodForGetter();
    }
    return findMethodForSetter();
  }

  private static String capitalize(String word) {
    return Character.toUpperCase(word.charAt(0)) + word.substring(1);
  }
}
