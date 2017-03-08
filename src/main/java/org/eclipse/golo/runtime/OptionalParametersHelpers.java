/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.golo.runtime;

import gololang.annotations.OptionalParameters;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;
import java.util.Arrays;

public final class OptionalParametersHelpers {
  private OptionalParametersHelpers() {
    // utility class
  }

  public static boolean hasOptionalParameters(Executable method) {
    return method.isAnnotationPresent(OptionalParameters.class);
  }

  public static boolean optionalArgumentsMatch(Executable method, Object[] arguments) {
    if (method.isVarArgs()) {
      return false;
    }
    int arity = arguments.length;
    int optionalPosition = method.getAnnotation(OptionalParameters.class).value();
    if (arity < optionalPosition || method.getParameterCount() < arity) {
      return false;
    }
    return TypeMatching.argumentsMatch(method, Arrays.copyOf(arguments, method.getParameterCount()));

  }

  public static MethodHandle completeOptionalArguments(Executable method, MethodHandle handle, int arity) throws Throwable {
    int rest = method.getParameterCount() - arity;
    if (!hasOptionalParameters(method) || rest < 1) {
      return handle;
    }
    return MethodHandles.insertArguments(handle, arity, new Object[rest]);
  }
}
