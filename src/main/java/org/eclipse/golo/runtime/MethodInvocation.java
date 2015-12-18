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
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static java.lang.reflect.Modifier.*;

/**
 * Encapsulate informations about a runtime method call.
 */
public class MethodInvocation extends AbstractInvocation {

  private final String name;
  private final Class<?> receiverClass;

  MethodInvocation(String name, MethodType type, Object[] args, String[] argNames) {
    super(type, args, argNames);
    this.name = name;
    this.receiverClass = args[0].getClass();
  }

  public String name() {
    return name;
  }

  public Class<?> receiverClass() {
    return receiverClass;
  }

  @Override
  public boolean match(Member member) {
    if (!(member instanceof Method)) {
      return false;
    }
    Method method = (Method) member;

    return method.getName().equals(name)
      && isPublic(method.getModifiers())
      && !isAbstract(method.getModifiers())
      && TypeMatching.argumentsMatch(method, arguments());
  }

  /**
   * returns a new invocation having the given name.
   */
  MethodInvocation withName(String newName) {
    return new MethodInvocation(newName, type(), arguments(), argumentNames());
  }

  @Override
  public String toString() {
    return name + super.toString();
  }
}
