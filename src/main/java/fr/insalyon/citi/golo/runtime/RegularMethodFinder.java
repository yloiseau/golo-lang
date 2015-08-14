/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package fr.insalyon.citi.golo.runtime;

import java.lang.invoke.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import gololang.GoloStruct;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.reflect.Modifier.*;
import static java.util.Arrays.copyOfRange;
import static fr.insalyon.citi.golo.runtime.DecoratorsHelper.*;
import static fr.insalyon.citi.golo.runtime.TypeMatching.*;

class RegularMethodFinder implements MethodFinder {

  private final Object[] args;
  private final MethodType type;
  private final Class<?> receiverClass;
  private final String methodName;
  private final Lookup lookup;
  private final boolean makeAccessible;
  private final MethodGraber methodGraber;

  public RegularMethodFinder(MethodInvocationSupport.InlineCache inlineCache, Class<?> receiverClass, Object[] args) {
    this.args = args;
    this.type = inlineCache.type();
    this.receiverClass = receiverClass;
    this.methodName = inlineCache.name;
    this.lookup = inlineCache.callerLookup;
    this.makeAccessible = !isPublic(receiverClass.getModifiers());

    this.methodGraber = new MethodGraber(methodName) {
      @Override
      protected boolean isCandidate(Method method) {
        return super.isCandidate(method)
               || isValidPrivateStructAccess(method);
      }

      @Override
      public MethodHandle toMethodHandle(Method method) throws IllegalAccessException {
        if (makeAccessible || isValidPrivateStructAccess(method)) {
          method.setAccessible(true);
        }
        MethodHandle target = super.toMethodHandle(method);
        return FunctionCallSupport.insertSAMFilter(target, lookup, method.getParameterTypes(), 1);
      }
    };
    this.methodGraber
      .withLookup(inlineCache.callerLookup)
      .withType(inlineCache.type())
      .calledWith(args, inlineCache.argumentNames)
      .addThisParameterName();
  }

  @Override
  public MethodHandle find() {
    try {
      MethodHandle target = findInMethods();
      if (target != null) { return target; }

      return findInFields();
    } catch (IllegalAccessException ignored) {
    /* We need to give augmentations a chance, as IllegalAccessException can be noise in our resolution.
     * Example: augmenting HashSet with a map function.
     *  java.lang.IllegalAccessException: member is private: java.util.HashSet.map/java.util.HashMap/putField
     */
      return null;
    }
  }

  private MethodHandle toMethodHandle(Field field) throws IllegalAccessException {
    MethodHandle target = null;
    if (makeAccessible) {
      field.setAccessible(true);
    }
    if (args.length == 1) {
      target = lookup.unreflectGetter(field).asType(type);
    } else {
      target = lookup.unreflectSetter(field);
      target = filterReturnValue(target, constant(receiverClass, args[0])).asType(type);
    }
    return target;
  }

  private boolean isValidPrivateStructAccess(Method method) {
    Object receiver = args[0];
    if (!(receiver instanceof GoloStruct)) {
      return false;
    }
    String receiverClassName = receiver.getClass().getName();
    String callerClassName = lookup.lookupClass().getName();
    return method.getName().equals(methodName) &&
        isPrivate(methodModifiers()) &&
        (receiverClassName.startsWith(callerClassName) ||
            callerClassName.equals(reverseStructAugmentation(receiverClassName)));
  }

  private static String reverseStructAugmentation(String receiverClassName) {
    return receiverClassName.substring(0, receiverClassName.indexOf(".types")) + "$" + receiverClassName.replace('.', '$');
  }

  private MethodHandle findInMethods() throws IllegalAccessException {
    List<Method> candidates = methodGraber.getCandidates(receiverClass);
    if (candidates.isEmpty()) { return null; }
    if (candidates.size() == 1) { return methodGraber.toMethodHandle(candidates.get(0)); }

    System.err.println("## ambiguous: " + candidates);
    for (Method method : candidates) {
      if (isMethodDecorated(method) || canAssignVarArgs(method)) {
        return methodGraber.toMethodHandle(method);
      }
    }
    return null;
  }

  private boolean canAssignVarArgs(Method method) {
    Class<?>[] parameterTypes = method.getParameterTypes();
    Object[] argsWithoutReceiver = copyOfRange(args, 1, args.length);
    return
      (haveSameNumberOfArguments(argsWithoutReceiver, parameterTypes)
       || haveEnoughArgumentsForVarargs(argsWithoutReceiver, method, parameterTypes))
      && canAssign(parameterTypes, argsWithoutReceiver, method.isVarArgs());
  }

  private MethodHandle findInFields() throws IllegalAccessException {
    if (type.parameterCount() > 3) { return null; }

    for (Field field : receiverClass.getDeclaredFields()) {
      if (isMatchingField(field)) {
        return toMethodHandle(field);
      }
    }
    for (Field field : receiverClass.getFields()) {
      if (isMatchingField(field)) {
        return toMethodHandle(field);
      }
    }
    return null;
  }

  private boolean isMatchingField(Field field) {
    return field.getName().equals(methodName) && !isStatic(field.getModifiers());
  }
}
