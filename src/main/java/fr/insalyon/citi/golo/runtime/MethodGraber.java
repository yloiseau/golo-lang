/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package fr.insalyon.citi.golo.runtime;

import java.util.*;
import java.lang.reflect.Method;
import java.lang.invoke.*;
import gololang.FunctionReference;
import java.lang.reflect.Parameter;

import static fr.insalyon.citi.golo.runtime.TypeMatching.*;
import static java.lang.invoke.MethodHandles.*;
import static java.lang.reflect.Modifier.*;
import static fr.insalyon.citi.golo.runtime.DecoratorsHelper.*;
import static java.lang.invoke.MethodType.genericMethodType;

public class MethodGraber {

  private final String methodName;
  private int arity = -1;
  private boolean varArgs = false;
  private boolean withPrivate = true;
  private Object[] args;
  private String[] argNames = new String[0];
  private Lookup lookup = MethodHandles.lookup();
  private MethodType type;

  public MethodGraber(String methodName) {
    this.methodName = methodName;
  }

  public MethodGraber withArity(int a) {
    this.arity = a;
    return this;
  }

  public MethodGraber varArg() {
    this.varArgs = true;
    return this;
  }

  public MethodGraber withType(MethodType type) {
    this.type = type;
    return this;
  }

  private int realArity() {
    if (this.type != null) {
      return this.type.parameterCount();
    }
    return this.arity;
  }

  public MethodGraber calledWith(Object[] args) {
    this.args = args;
    return this;
  }

  public MethodGraber calledWith(Object[] args, String[] argNames) {
    this.args = args;
    this.argNames = argNames;
    return this;
  }

  public MethodGraber onlyPublic() {
    this.withPrivate = false;
    return this;
  }

  public MethodGraber withLookup(Lookup l) {
    this.lookup = l;
    return this;
  }

  //TODO: private
  public List<Method> getCandidates(Class<?> receiver) {
    List<Method> candidates = new LinkedList<>();
    Set<Method> methods = new LinkedHashSet<>();
    Collections.addAll(methods, receiver.getDeclaredMethods());
    Collections.addAll(methods, receiver.getMethods());
    for (Method method : methods) {
      if (isCandidate(method)) {
        candidates.add(method);
      }
    }
    return candidates;
  }

  private Method getInClass(Class<?> receiver) throws NoSuchMethodException, AmbiguousFunctionReferenceException {
    List<Method> candidates = getCandidates(receiver);
    if (candidates.size() > 1) {
      throw new AmbiguousFunctionReferenceException(
          "The reference to " + methodName + " in " + receiver.getName()
          + (arity < 0 ? "" : (" with arity " + arity))
          + " is ambiguous");
    }
    if (candidates.isEmpty()) {
      throw new NoSuchMethodException(
          methodName + " in " + receiver.getName()
        + (arity < 0 ? "" : (" with arity " + arity)));

    }
    Method targetMethod = candidates.get(0);
    if (this.makeAccessible(targetMethod)) {
      targetMethod.setAccessible(true);
    }
    return targetMethod;
  }

  protected boolean makeAccessible(Method method) {
    return true;
  }

  protected boolean isCandidate(Method method) {
    return
      method.getName().equals(methodName)
      && !isAbstract(method.getModifiers())
      && matchesArity(method)
      && (withPrivate || isPublic(method.getModifiers()));
  }

  private boolean matchesArity(Method method) {
    int parameterCount = method.getParameterTypes().length;
    return (this.arity < 0)
           || (parameterCount == this.arity)
           || (method.isVarArgs() && (parameterCount <= this.arity))
           || isMethodDecorated(method);
  }

  private String[] getParameterNames(Method method) {
    return Arrays.stream(method.getParameters())
                 .map(Parameter::getName)
                 .toArray(String[]::new);
  }

  public MethodGraber addThisParameterName() {
    String[] withThis = new String[argNames.length + 1];
    withThis[0] = "this";
    System.arraycopy(argNames,0, withThis, 1, argNames.length);
    argNames = withThis;
    return this;
  }

  private MethodHandle adaptMethodHandle(MethodHandle target) throws IllegalAccessException {
    if (type == null || args == null) {
      return target;
    }
    if (target.isVarargsCollector() && isLastArgumentAnArray(this.realArity(), args)) {
      target = target.asFixedArity().asType(type);
    } else {
      target = target.asType(type);
    }
    return target;
  }

  // TODO: make protected
  public MethodHandle toMethodHandle(Method method) throws IllegalAccessException {
    MethodHandle handle;
    if (isMethodDecorated(method)) {
      handle = getDecoratedMethodHandle(lookup, method, this.realArity());
    } else {
      handle = adaptMethodHandle(lookup.unreflect(method));
    }
    if (argNames.length > 1) {
      handle = FunctionCallSupport.reorderArguments(method, handle, argNames);
    }
    return handle;
  }

  public MethodHandle getMethodHandle(Class<?> receiver) throws NoSuchMethodException, AmbiguousFunctionReferenceException, IllegalAccessException {
    return toMethodHandle(getInClass(receiver));
  }

  public FunctionReference getFunctionReference(Class<?> receiver) throws NoSuchMethodException, AmbiguousFunctionReferenceException, IllegalAccessException {
    Method targetMethod = getInClass(receiver);
    return new FunctionReference(toMethodHandle(targetMethod), getParameterNames(targetMethod));
  }

  public static MethodGraber fromCallSite(MethodInvocationSupport.InlineCache inlineCache, Object[] args) {
    return new MethodGraber(inlineCache.name)
                .withArity(inlineCache.type().parameterCount())
                .withLookup(inlineCache.callerLookup)
                .withType(inlineCache.type())
                .calledWith(args, inlineCache.argumentNames);
  }
}
