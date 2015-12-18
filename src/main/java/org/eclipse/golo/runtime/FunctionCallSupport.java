/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import gololang.FunctionReference;
import gololang.error.Result;
import java.lang.invoke.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Member;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.stream.*;
import java.util.function.*;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.permuteArguments;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;
import static org.eclipse.golo.compiler.utils.NamingUtils.*;
import static org.eclipse.golo.runtime.DecoratorsHelper.getDecoratedMethodHandle;
import static org.eclipse.golo.runtime.DecoratorsHelper.isMethodDecorated;

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
    throw new RuntimeException(
        "Could not convert " + handle + " to a functional interface of type " + type);
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

  private static Supplier<NoSuchMethodError> notFound(FunctionCallSite callSite) {
    return () -> new NoSuchMethodError(callSite.name + callSite.type().toMethodDescriptorString());
  }


  private static MethodHandle lookupFunction(FunctionCallSite callSite, Object[] args) throws Throwable {
    return findMembers(callSite.callerLookup.lookupClass(), callSite.name, args)
      .map(m -> toMethodHandle(m, callSite, args))
      .findFirst()
      .orElseThrow(notFound(callSite))
      .get();
  }

  private static Result<MethodHandle, Throwable> toMethodHandle(Member result, FunctionCallSite callSite, Object[] args) {
    try {
      if (result instanceof Method) {
        return Result.ok(toMethodHandle((Method) result, callSite, args));
      } else if (result instanceof Constructor) {
        return Result.ok(toMethodHandle((Constructor<?>) result, callSite, args));
      } else {
        return Result.ok(toMethodHandle((Field) result, callSite));
      }
    } catch (Throwable e) {
      return Result.error(e);
    }
  }

  private static MethodHandle toMethodHandle(Field field, FunctionCallSite callSite) throws Throwable  {
    return callSite.callerLookup.unreflectGetter(field).asType(callSite.type());
  }

  private static MethodHandle toMethodHandle(Constructor<?> constructor, FunctionCallSite callSite, Object[] args) throws Throwable {
    Lookup caller = callSite.callerLookup;
    MethodHandle handle;
    if (constructor.isVarArgs() && TypeMatching.isLastArgumentAnArray(constructor.getParameterTypes().length, args)) {
      handle = caller.unreflectConstructor(constructor).asFixedArity().asType(callSite.type());
    } else {
      handle = caller.unreflectConstructor(constructor).asType(callSite.type());
    }
    return insertSAMFilter(handle, caller, constructor.getParameterTypes(), 0);
  }

  private static MethodHandle toMethodHandle(Method method, FunctionCallSite callSite, Object[] args) throws Throwable {
    Lookup caller = callSite.callerLookup;
    String[] argumentNames = callSite.argumentNames;
    MethodHandle handle;
    checkLocalFunctionCallFromSameModuleAugmentation(method, caller.lookupClass().getName());
    if (isMethodDecorated(method)) {
      handle = getDecoratedMethodHandle(caller, method, callSite.type().parameterCount());
    } else {
      //TODO: improve varargs support on named arguments. Matching the last param type + according argument
      if (isVarargsWithNames(method, method.getParameterTypes(), args, argumentNames)) {
        handle = caller.unreflect(method).asFixedArity().asType(callSite.type());
      } else {
        handle = caller.unreflect(method).asType(callSite.type());
      }
    }
    if (argumentNames.length > 0) {
      handle = reorderArguments(method, handle, argumentNames);
    }
    return insertSAMFilter(handle, caller, method.getParameterTypes(), 0);
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
    MethodHandle handle = lookupFunction(callSite, args);

    if (callSite.constant) {
      return internConstantCall(callSite, handle, args);
    } else {
      callSite.setTarget(handle);
      return handle.invokeWithArguments(args);
    }
  }

  private static boolean isVarargsWithNames(Method method, Class<?>[] types, Object[] args, String[] argumentNames) {
    return method.isVarArgs()
      && (
          TypeMatching.isLastArgumentAnArray(types.length, args)
          || argumentNames.length > 0);
  }

  public static MethodHandle reorderArguments(Method method, MethodHandle handle, String[] argumentNames) {
    if (Arrays.stream(method.getParameters()).allMatch(Parameter::isNamePresent)) {
      String[] parameterNames = Arrays.stream(method.getParameters())
        .map(Parameter::getName)
        .toArray(String[]::new);
      int[] argumentsOrder = new int[parameterNames.length];
      for (int i = 0; i < argumentNames.length; i++) {
        int actualPosition = -1;
        for (int j = 0; j < parameterNames.length; j++) {
          if (parameterNames[j].equals(argumentNames[i])) {
            actualPosition = j;
          }
        }
        if (actualPosition == -1) {
          throw new IllegalArgumentException(
              "Argument name " + argumentNames[i]
              + " not in parameter names used in declaration: "
              + method.getName() + Arrays.toString(parameterNames));
        }
        argumentsOrder[actualPosition] = i;
      }
      return permuteArguments(handle, handle.type(), argumentsOrder);
    }
    return handle;
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

  private static void checkLocalFunctionCallFromSameModuleAugmentation(Method method, String callerClassName) {
    if (isPrivate(method.getModifiers()) && isInnerClassOf(method.getDeclaringClass().getName(), callerClassName)) {
      method.setAccessible(true);
    }
  }

  private static Stream<? extends Member> findMembers(Class<?> callerClass, String name, Object[] args) {
    return Stream.of(
        findStaticMethodOrField(callerClass, name, args),
        findClassWithStaticMethodOrField(callerClass, name, args),
        findClassWithStaticMethodOrFieldFromImports(callerClass, name, args),
        findClassWithConstructor(callerClass, name, args),
        findClassWithConstructorFromImports(callerClass, name, args))
      .reduce(Stream.empty(), Stream::concat);
  }

  private static Stream<? extends Member> findClassWithConstructorFromImports(Class<?> callerClass, String className, Object[] args) {
    return Extractors.getImportedNames(callerClass)
      .flatMap(addSuffix(className))
      .flatMap(name -> findClassWithConstructor(callerClass, name, args));
  }

  private static Function<String, Stream<String>> addSuffix(String className) {
    return moduleName -> {
      if (moduleName.endsWith(className)) {
        return Stream.of(inModule(moduleName, className), moduleName);
      }
      return Stream.of(inModule(moduleName, className));
    };
  }

  private static Stream<? extends Member> findClassWithConstructor(Class<?> callerClass, String classname, Object[] args) {
    return Extractors.getConstructors(Loader.forClass(callerClass).load(classname))
      .filter(c -> TypeMatching.argumentsMatch(c, args));
  }

  private static Function<String, Stream<String>> addPrefixOf(String functionName) {
    if (isQualified(functionName)) {
      final String prefix = extractModuleName(functionName);
      return moduleName -> Stream.of(moduleName, inModule(moduleName, prefix));
    }
    return Stream::of;
  }

  private static Stream<Member> findClassWithStaticMethodOrFieldFromImports(Class<?> callerClass, String functionName, Object[] args) {
    Loader loader = Loader.forClass(callerClass);
    return Extractors.getImportedNames(callerClass)
      .flatMap(addPrefixOf(functionName))
      .flatMap(name -> findStaticMethodOrField(loader.load(name), extractFunctionName(functionName), args));
  }

  private static Stream<Member> findClassWithStaticMethodOrField(Class<?> callerClass, String name, Object[] args) {
    int idx = name.lastIndexOf('.');
    if (idx >= 0) {
      Class<?> lookupClass = Loader.forClass(callerClass).load(name.substring(0, idx));
      String baseName = name.substring(idx + 1);
      return findStaticMethodOrField(lookupClass, baseName, args);
    }
    return Stream.empty();
  }

  private static Stream<Member> findStaticMethodOrField(Class<?> klass, String name, Object[] arguments) {
    // TODO: try to merge with findClassWithStaticMethodOrField
    Class<?> lookupClass = klass;
    String baseName = name;
    // int idx = name.lastIndexOf(".");
    // if (idx >= 0) {
    //   lookupClass = Loader.forClass(klass).load(name.substring(0, idx));
    //   baseName = name.substring(idx + 1);
    // }
    return Extractors.getMembers(lookupClass)
      .filter(memberMatches(name, arguments));
  }

  private static Predicate<Member> memberMatches(String name, Object[] arguments) {
    return member -> member.getName().equals(name)
                     && isStatic(member.getModifiers())
                     && (!(member instanceof Method) || TypeMatching.argumentsMatch((Method) member, arguments));
  }
}
