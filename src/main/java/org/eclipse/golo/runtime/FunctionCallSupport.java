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

import gololang.FunctionReference;

import java.lang.invoke.*;
import java.lang.reflect.*;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.Lookup;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.methodType;
import static java.lang.reflect.Modifier.isPrivate;
import static java.lang.reflect.Modifier.isStatic;
import static org.eclipse.golo.runtime.DecoratorsHelper.getDecoratedMethodHandle;
import static org.eclipse.golo.runtime.DecoratorsHelper.isMethodDecorated;
import static org.eclipse.golo.runtime.NamedArgumentsHelper.*;
import static gololang.Messages.message;
import static org.eclipse.golo.runtime.Extractors.checkDeprecation;

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

  private static final MethodHandle OVERLOADED_GUARD_GENERIC;
  private static final MethodHandle OVERLOADED_GUARD_1;
  private static final MethodHandle OVERLOADED_GUARD_2;
  private static final MethodHandle OVERLOADED_GUARD_3;
  private static final MethodHandle OVERLOADED_GUARD_4;
  private static final MethodHandle OVERLOADED_GUARD_5;

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

      OVERLOADED_GUARD_GENERIC = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_generic",
          methodType(boolean.class, Class[].class, Object[].class));

      OVERLOADED_GUARD_1 = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_1",
          methodType(boolean.class, Class.class, Object.class));

      OVERLOADED_GUARD_2 = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_2",
          methodType(boolean.class, Class.class, Class.class, Object.class, Object.class));

      OVERLOADED_GUARD_3 = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_3",
          methodType(boolean.class, Class.class, Class.class, Class.class, Object.class, Object.class, Object.class));

      OVERLOADED_GUARD_4 = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_4",
          methodType(boolean.class, Class.class, Class.class, Class.class, Class.class, Object.class, Object.class, Object.class, Object.class));

      OVERLOADED_GUARD_5 = lookup.findStatic(
          FunctionCallSupport.class,
          "overloadedGuard_5",
          methodType(boolean.class, Class.class, Class.class, Class.class, Class.class, Class.class, Object.class, Object.class, Object.class, Object.class, Object.class));


    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new Error("Could not bootstrap the required method handles", e);
    }
  }

  public static Object samFilter(Class<?> type, Object value) {
    if (value instanceof FunctionReference) {
      return MethodHandleProxies.asInterfaceInstance(type, ((FunctionReference) value).handle());
    }
    return value;
  }

  public static Object functionalInterfaceFilter(Lookup caller, Class<?> type, Object value) throws Throwable {
    if (value instanceof FunctionReference) {
      return asFunctionalInterface(caller, type, ((FunctionReference) value).handle());
    }
    return value;
  }

  public static Object asFunctionalInterface(Lookup caller, Class<?> type, MethodHandle handle) throws Throwable {
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

  public static boolean overloadedGuard_generic(Class<?>[] types, Object[] arguments) {
    for (int i = 0; i < types.length; i++) {
      if ((arguments[i] != null) && (arguments[i].getClass() != types[i])) {
        return false;
      }
    }
    return true;
  }

  public static boolean overloadedGuard_1(Class<?> t1, Object arg1) {
    return (arg1 == null || arg1.getClass() == t1);
  }

  public static boolean overloadedGuard_2(Class<?> t1, Class<?> t2, Object arg1, Object arg2) {
    return (arg1 == null || arg1.getClass() == t1)
        && (arg2 == null || arg2.getClass() == t2);
  }

  public static boolean overloadedGuard_3(Class<?> t1, Class<?> t2, Class<?> t3, Object arg1, Object arg2, Object arg3) {
    return (arg1 == null || arg1.getClass() == t1)
        && (arg2 == null || arg2.getClass() == t2)
        && (arg3 == null || arg3.getClass() == t3);
  }

  public static boolean overloadedGuard_4(Class<?> t1, Class<?> t2, Class<?> t3, Class<?> t4, Object arg1, Object arg2, Object arg3, Object arg4) {
    return (arg1 == null || arg1.getClass() == t1)
        && (arg2 == null || arg2.getClass() == t2)
        && (arg3 == null || arg3.getClass() == t3)
        && (arg4 == null || arg4.getClass() == t4);
  }

  public static boolean overloadedGuard_5(Class<?> t1, Class<?> t2, Class<?> t3, Class<?> t4, Class<?> t5, Object arg1, Object arg2, Object arg3, Object arg4, Object arg5) {
    return (arg1 == null || arg1.getClass() == t1)
        && (arg2 == null || arg2.getClass() == t2)
        && (arg3 == null || arg3.getClass() == t3)
        && (arg4 == null || arg4.getClass() == t4)
        && (arg5 == null || arg5.getClass() == t5);
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
    MethodHandle fallbackHandle = FALLBACK
        .bindTo(callSite)
        .asCollector(Object[].class, type.parameterCount())
        .asType(type);
    callSite.setTarget(fallbackHandle);
    return callSite;
  }


  private static AccessibleObject findTarget(Class<?> callerClass, String functionName, Object[] args) {
    AccessibleObject result = findStaticMethodOrField(callerClass, callerClass, functionName, args);
    if (result == null) {
      result = findClassWithStaticMethodOrField(callerClass, functionName, args);
    }
    if (result == null) {
      result = findClassWithStaticMethodOrFieldFromImports(callerClass, functionName, args);
    }
    if (result == null) {
      result = findClassWithConstructor(callerClass, functionName, args);
    }
    if (result == null) {
      result = findClassWithConstructorFromImports(callerClass, functionName, args);
    }
    return null;
  }

  public static Object fallback(FunctionCallSite callSite, Object[] args) throws Throwable {
    String functionName = callSite.name;
    MethodType type = callSite.type();
    Lookup caller = callSite.callerLookup;
    Class<?> callerClass = caller.lookupClass();
    String[] argumentNames = callSite.argumentNames;

    MethodHandle handle = null;
    Object result = findTarget(callerClass, functionName, args);
    if (result == null) {
      throw new NoSuchMethodError(functionName + type.toMethodDescriptorString());
    }

    Class<?>[] types = null;
    if (result instanceof Method) {
      Method method = (Method) result;
      checkLocalFunctionCallFromSameModuleAugmentation(method, callerClass.getName());
      if (isMethodDecorated(method)) {
        handle = getDecoratedMethodHandle(caller, method, type.parameterCount());
      } else {
        types = method.getParameterTypes();
        handle = caller.unreflect(method);
        if (method.isAnnotationPresent(WithCaller.class)) {
          handle = handle.bindTo(callerClass);
        }
        //TODO: improve varargs support on named arguments. Matching the last param type + according argument
        if (isVarargsWithNames(method, types, args, argumentNames)) {
          handle = handle.asFixedArity().asType(type);
        } else {
          handle = handle.asType(type);
        }
      }
      handle = reorderArguments(method, handle, argumentNames);
    } else if (result instanceof Constructor) {
      Constructor<?> constructor = (Constructor<?>) result;
      types = constructor.getParameterTypes();
      if (constructor.isVarArgs() && TypeMatching.isLastArgumentAnArray(types.length, args)) {
        handle = caller.unreflectConstructor(constructor).asFixedArity().asType(type);
      } else {
        handle = caller.unreflectConstructor(constructor).asType(type);
      }
    } else {
      Field field = (Field) result;
      handle = caller.unreflectGetter(field).asType(type);
    }
    handle = insertSAMFilter(handle, callSite.callerLookup, types, 0);

    if (callSite.constant) {
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
    } else {
      callSite.setTarget(handle);
      return handle.invokeWithArguments(args);
    }
  }

  private MethodHandle guardOnOverloaded(MethodHandle handle, Object[] args) {
    Class<?>[] types = new Class<?>[args.length];
    for (int i = 0; i < types.length; i++) {
      types[i] = (args[i] == null) ? Object.class : args[i].getClass();
    }
    MethodHandle guard;
    switch (args.length) {
      case 1:
        guard = insertArguments(OVERLOADED_GUARD_1, 0, types[0]);
        break;
      case 2:
        guard = insertArguments(OVERLOADED_GUARD_2, 0, types[0], types[1]);
        break;
      case 3:
        guard = insertArguments(OVERLOADED_GUARD_3, 0, types[0], types[1], types[2]);
        break;
      case 4:
        guard = insertArguments(OVERLOADED_GUARD_4, 0, types[0], types[1], types[2], types[3]);
        break;
      case 5:
        guard = insertArguments(OVERLOADED_GUARD_5, 0, types[0], types[1], types[2], types[3], types[4]);
        break;
      default:
        guard = OVERLOADED_GUARD_GENERIC.bindTo(types).asCollector(Object[].class, types.length);
    }
    return guardWithTest(guard, target, inlineCache.resetFallback);
  }

  private static boolean isVarargsWithNames(Method method, Class<?>[] types, Object[] args, String[] argumentNames) {
    return method.isVarArgs()
      && (
          TypeMatching.isLastArgumentAnArray(types.length, args)
          || argumentNames.length > 0);
  }

  private static int[] getArgumentsOrder(Method method, List<String> parameterNames, String[] argumentNames) {
    int[] argumentsOrder = new int[parameterNames.size()];
    for (int i = 0; i < argumentNames.length; i++) {
      int actualPosition = parameterNames.indexOf(argumentNames[i]);
      checkArgumentPosition(actualPosition, argumentNames[i], method.getName() + parameterNames);
      argumentsOrder[actualPosition] = i;
    }
    return argumentsOrder;
  }

  public static MethodHandle reorderArguments(Method method, MethodHandle handle, String[] argumentNames) {
    if (argumentNames.length == 0) { return handle; }
    if (hasNamedParameters(method)) {
      return permuteArguments(handle, handle.type(), getArgumentsOrder(method, getParameterNames(method), argumentNames));
    }
    Warnings.noParameterNames(method.getName(), argumentNames);
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
    if (isPrivate(method.getModifiers()) && callerClassName.contains("$")) {
      String prefix = callerClassName.substring(0, callerClassName.indexOf("$"));
      if (method.getDeclaringClass().getName().equals(prefix)) {
        method.setAccessible(true);
      }
    }
  }

  private static AccessibleObject findClassWithConstructorFromImports(Class<?> callerClass, String classname, Object[] args) {
    String[] imports = Module.imports(callerClass);
    for (String imported : imports) {
      AccessibleObject result = findClassWithConstructor(callerClass, imported + "." + classname, args);
      if (result != null) {
        return result;
      }
      if (imported.endsWith(classname)) {
        result = findClassWithConstructor(callerClass, imported, args);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }

  private static AccessibleObject findClassWithConstructor(Class<?> callerClass, String classname, Object[] args) {
    try {
      Class<?> targetClass = Class.forName(classname, true, callerClass.getClassLoader());
      for (Constructor<?> constructor : targetClass.getConstructors()) {
        if (TypeMatching.argumentsMatch(constructor, args)) {
          return checkDeprecation(callerClass, constructor);
        }
      }
    } catch (ClassNotFoundException ignored) {
      // ignored to try the next strategy
    }
    return null;
  }

  private static AccessibleObject findClassWithStaticMethodOrFieldFromImports(Class<?> callerClass, String functionName, Object[] args) {
    String[] imports = Module.imports(callerClass);
    String[] classAndMethod = null;
    final int classAndMethodSeparator = functionName.lastIndexOf(".");
    if (classAndMethodSeparator > 0) {
      classAndMethod = new String[]{
          functionName.substring(0, classAndMethodSeparator),
          functionName.substring(classAndMethodSeparator + 1)
      };
    }
    for (String importedClassName : imports) {
      try {
        Class<?> importedClass;
        try {
          importedClass = Class.forName(importedClassName, true, callerClass.getClassLoader());
        } catch (ClassNotFoundException expected) {
          if (classAndMethod == null) {
            throw expected;
          }
          importedClass = Class.forName(importedClassName + "." + classAndMethod[0], true, callerClass.getClassLoader());
        }
        String lookup = (classAndMethod == null) ? functionName : classAndMethod[1];
        AccessibleObject result = findStaticMethodOrField(callerClass, importedClass, lookup, args);
        if (result != null) {
          return result;
        }
      } catch (ClassNotFoundException ignored) {
        // ignored to try the next strategy
        Warnings.unavailableClass(importedClassName, callerClass.getName());
      }
    }
    return null;
  }

  private static AccessibleObject findClassWithStaticMethodOrField(Class<?> callerClass, String functionName, Object[] args) {
    int methodClassSeparatorIndex = functionName.lastIndexOf(".");
    if (methodClassSeparatorIndex >= 0) {
      String className = functionName.substring(0, methodClassSeparatorIndex);
      String methodName = functionName.substring(methodClassSeparatorIndex + 1);
      try {
        Class<?> targetClass = Class.forName(className, true, callerClass.getClassLoader());
        return findStaticMethodOrField(callerClass, targetClass, methodName, args);
      } catch (ClassNotFoundException ignored) {
        // ignored to try the next strategy
        Warnings.unavailableClass(className, callerClass.getName());
      }
    }
    return null;
  }

  private static AccessibleObject findStaticMethodOrField(Class<?> caller, Class<?> klass, String name, Object[] arguments) {
    Optional<Method> meth = Extractors.getMethods(klass)
      .filter(m -> methodMatches(caller, name, arguments, m, m.isVarArgs()))
      .map(m -> checkDeprecation(caller, m))
      .findFirst();
    if (meth.isPresent()) {
      return meth.get();
    }
    if (arguments.length == 0) {
      Optional<Field> f = Extractors.getFields(klass)
        .filter(o -> fieldMatches(name, o))
        .map(o -> checkDeprecation(caller, o))
        .findFirst();
      return f.orElse(null);
    }
    return null;
  }

  private static boolean methodMatches(Class<?> caller, String name, Object[] arguments, Method method, boolean varargs) {
    return methodMatches(caller, name, arguments, method, varargs, true);
  }

  private static boolean methodMatches(Class<?> caller, String name, Object[] arguments, Method method, boolean varargs, boolean tryCaller) {
    if (!method.getName().equals(name) || !isStatic(method.getModifiers())) { return false; }
    if (isMethodDecorated(method)) { return true; }
    if (TypeMatching.argumentsMatch(method, arguments, varargs)) { return true; }
    if (method.isAnnotationPresent(WithCaller.class) && tryCaller) {
      Object[] argsWithCaller = new Object[arguments.length + 1];
      argsWithCaller[0] = caller;
      System.arraycopy(arguments, 0, argsWithCaller, 1, arguments.length);
      return methodMatches(caller, name, argsWithCaller, method, varargs, false);
    }
    return false;
  }

  private static boolean fieldMatches(String name, Field field) {
    return field.getName().equals(name) && isStatic(field.getModifiers());
  }
}
