/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Member;

import static java.lang.reflect.Modifier.*;

/**
 * Encapsulate informations about a runtime call.
 */
public abstract class AbstractInvocation {

  private final Object[] arguments;
  private final int arity;
  private final String[] argumentNames;
  private final MethodType type;

  AbstractInvocation(MethodType type, Object[] args, String[] argNames) {
    this.arguments = args;
    this.arity = args.length;
    this.type = type;
    this.argumentNames = argNames;
  }

  public Object[] arguments() {
    return arguments;
  }

  public int arity() {
    return arity;
  }

  public String[] argumentNames() {
    return argumentNames;
  }

  public MethodType type() {
    return type;
  }

  private boolean isLastArgumentAnArray() {
    return TypeMatching.isLastArgumentAnArray(arity, arguments);
  }

  public MethodHandle coerce(MethodHandle target) {
    if (isVarargsCall(target)) {
      return target.asFixedArity().asType(type);
    }
    return target.asType(type);
  }

  private boolean isVarargsCall(MethodHandle target) {
    return target.isVarargsCollector() && (isLastArgumentAnArray() || argumentNames.length > 0);
  }

  public abstract boolean match(Member member);

  @Override
  public String toString() {
    return type.toString() + java.util.Arrays.asList(arguments);
  }

}
