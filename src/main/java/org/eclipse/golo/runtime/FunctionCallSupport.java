/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.function.*;
import java.util.List;
import java.util.stream.*;

import org.eclipse.golo.compiler.PackageAndClass;

import gololang.FunctionReference;
import gololang.error.Result;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;

import static org.eclipse.golo.runtime.DecoratorsHelper.getDecoratedMethodHandle;
import static org.eclipse.golo.runtime.DecoratorsHelper.isMethodDecorated;
import static org.eclipse.golo.runtime.NamedArgumentsHelper.*;

import static gololang.Messages.message;


// TODO: extract methods common with method support
public final class FunctionCallSupport {

  private FunctionCallSupport() {
    throw new UnsupportedOperationException("Don't instantiate invokedynamic bootstrap class");
  }

  static class FunctionCallSite extends MutableCallSite {

    final Lookup callerLookup;
    final String name;
    final boolean constant;
    final String[] argumentNames;

    FunctionCallSite(MethodHandles.Lookup callerLookup, String name, MethodType type, boolean constant, String... argumentNames) {
      super(type);
      this.callerLookup = callerLookup;
      this.name = name;
      this.constant = constant;
      this.argumentNames = argumentNames;
    }

    public FunctionInvocation toFunctionInvocation(Object[] args) {
      return new FunctionInvocation(PackageAndClass.of(this.name), this.constant, type(), args, argumentNames);
    }
  }

  private static final MethodHandle FALLBACK;
  private static final MethodHandle SAM_FILTER;
  private static final MethodHandle FUNCTIONAL_INTERFACE_FILTER;

  static {
    try {
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      FALLBACK = lookup.findStatic(
          FunctionCallSupport.class,
          "fallback",
          methodType(Object.class, FunctionCallSite.class, Object[].class));
      SAM_FILTER = lookup.findStatic(
          FunctionCallSupport.class,
          "samFilter",
          methodType(Object.class, Class.class, Object.class));
      FUNCTIONAL_INTERFACE_FILTER = lookup.findStatic(
          FunctionCallSupport.class,
          "functionalInterfaceFilter",
          methodType(Object.class, Lookup.class, Class.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("Could not bootstrap the required method handles", e);
    }
  }

  private static Object samFilter(Class<?> type, Object value) {
    if (value instanceof FunctionReference) {
      return MethodHandleProxies.asInterfaceInstance(type, ((FunctionReference) value).handle());
    }
    return value;
  }

  private static Object functionalInterfaceFilter(Lookup caller, Class<?> type, Object value) throws Throwable {
    if (value instanceof FunctionReference) {
      return asFunctionalInterface(caller, type, ((FunctionReference) value).handle());
    }
    return value;
  }

  private static Object asFunctionalInterface(Lookup caller, Class<?> type, MethodHandle handle) throws Throwable {
    for (Method method : type.getMethods()) {
      if (!method.isDefault() && !isStatic(method.getModifiers())) {
        MethodType lambdaType = methodType(method.getReturnType(), method.getParameterTypes());
        CallSite callSite = LambdaMetafactory.metafactory(
            caller,
            method.getName(),
            methodType(type),
            lambdaType,
            handle,
            lambdaType);
        return callSite.dynamicInvoker().invoke();
      }
    }
    throw new RuntimeException(message("handle_conversion_failed", handle, type));
  }

  public static CallSite bootstrap(Lookup caller, String name, MethodType type, Object... bsmArgs) throws IllegalAccessException, ClassNotFoundException {
    boolean constant = ((int) bsmArgs[0]) == 1;
    String[] argumentNames = new String[bsmArgs.length - 1];
    for (int i = 0; i < bsmArgs.length - 1; i++) {
      argumentNames[i] = (String) bsmArgs[i + 1];
    }
    FunctionCallSite callSite = new FunctionCallSite(
        caller,
        name.replaceAll("#", "\\."),
        type,
        constant,
        argumentNames);
    callSite.setTarget(FALLBACK
        .bindTo(callSite)
        .asCollector(Object[].class, type.parameterCount())
        .asType(type));
    return callSite;
  }

  private static MethodHandle lookupFunction(FunctionInvocation call, MethodHandles.Lookup lookup) {
    return new StaticMethodFinder(call, lookup).find();
  }

  private static Object internConstantCall(FunctionCallSite callSite, MethodHandle handle, Object[] args) throws Throwable {
    MethodType type = callSite.type();
    Object constantValue = handle.invokeWithArguments(args);
    MethodHandle constant;
    if (constantValue == null) {
      constant = MethodHandles.constant(Object.class, null);
    } else {
      constant = MethodHandles.constant(constantValue.getClass(), constantValue);
    }
    constant = MethodHandles.dropArguments(constant, 0, type.parameterArray());
    callSite.setTarget(constant.asType(type));
    return constantValue;
  }

  public static Object fallback(FunctionCallSite callSite, Object[] args) throws Throwable {
    FunctionInvocation call = callSite.toFunctionInvocation(args);
    MethodHandle handle = lookupFunction(call, callSite.callerLookup);
    if (handle == null) {
      throw new NoSuchMethodError(callSite.name + callSite.type().toMethodDescriptorString());
    }
    if (callSite.constant) {
      return internConstantCall(callSite, handle, args);
    } else {
      callSite.setTarget(handle);
      return handle.invokeWithArguments(args);
    }
  }

  public static MethodHandle insertSAMFilter(MethodHandle handle, Lookup caller, Class<?>[] types, int startIndex) {
    if (types != null) {
      for (int i = 0; i < types.length; i++) {
        if (TypeMatching.isSAM(types[i])) {
          handle = MethodHandles.filterArguments(handle, startIndex + i, SAM_FILTER.bindTo(types[i]));
        } else if (TypeMatching.isFunctionalInterface(types[i])) {
          handle = MethodHandles.filterArguments(
              handle,
              startIndex + i,
              FUNCTIONAL_INTERFACE_FILTER.bindTo(caller).bindTo(types[i]));
        }
      }
    }
    return handle;
  }





}
