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

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.*;
import static java.lang.invoke.MethodHandles.Lookup;

/**
 * Encapsulate informations about a runtime method call.
 */
public class MethodInvocation {

  private final String name;
  private final Class<?> receiverClass;
  private final Object[] arguments;
  private final int arity;
  private final String[] argumentNames;
  private final MethodType type;
  private final Lookup lookup;

  MethodInvocation(String name, MethodType type, Object[] args, String[] argNames, Lookup lookup) {
    this.name = name;
    this.receiverClass = args[0].getClass();
    this.arguments = args;
    this.arity = args.length;
    this.type = type;
    this.argumentNames = argNames;
    this.lookup = lookup;
  }

  public String name() {
    return name;
  }

  public Class<?> receiverClass() {
    return receiverClass;
  }

  public Object[] arguments() {
    return arguments;
  }

  public int arity() {
    return arity;
  }

  public String[] argumentNames() {
    if (this.argumentNames == null) { return new String[0]; }
    return this.argumentNames;
  }

  public MethodType type() {
    return type;
  }

  public Lookup lookup() {
    return this.lookup;
  }

  private boolean isLastArgumentAnArray() {
    return arity > 0
      && arguments.length == arity
      && arguments[arity - 1] instanceof Object[];
  }

  public MethodHandle coerce(MethodHandle target) {
    if (target.isVarargsCollector() && isLastArgumentAnArray()) {
      return target.asFixedArity().asType(type);
    }
    return target.asType(type);
  }

  public boolean match(Method method) {
    return method.getName().equals(name)
      && isPublic(method.getModifiers())
      && !isAbstract(method.getModifiers())
      && TypeMatching.argumentsMatch(method, arguments);
  }

  /**
   * returns a new invocation having the given name.
   */
  MethodInvocation withName(String newName) {
    return new MethodInvocation(newName, this.type, this.arguments, this.argumentNames, this.lookup);
  }

  @Override
  public String toString() {
    return name + type + java.util.Arrays.asList(arguments);
  }


}
