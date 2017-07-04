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
import java.lang.reflect.*;
import org.eclipse.golo.compiler.PackageAndClass;

import static java.lang.reflect.Modifier.*;

/**
 * Encapsulate informations about a runtime function call.
 *
 * <p>Note that the call can also be to an accessible static field or a constructor, since the same syntax is used in
 * Golo.
 */
public class FunctionInvocation extends AbstractInvocation {
  private final PackageAndClass name;
  private final boolean constant;

  FunctionInvocation(PackageAndClass name, boolean constant, MethodType type, Object[] args, String[] argNames) {
    super(type, args, argNames);
    this.name = name;
    this.constant = constant;
  }

  public String baseName() {
    return name.className();
  }

  public String moduleName() {
    return name.packageName();
  }

  public boolean isQualified() {
    return name.hasPackage();
  }

  public PackageAndClass packageAndClass() {
    return name;
  }

  public FunctionInvocation resolve(String importedModule) {
    return new FunctionInvocation(
        name.resolve(importedModule),
        constant,
        type(),
        arguments(),
        argumentNames());
  }

  @Override
  public boolean match(Member member) {
    System.err.println("### matching " + member.getClass().getSimpleName() + ": " + member);
    if (member instanceof Constructor) {
      return TypeMatching.argumentsMatch((Constructor) member, arguments());
    }
    System.err.print("     " + member.getName() + " ~= " + name.toString() + "?");
    boolean r = member.getName().equals(name.className())
      // && (!name.hasPackage() || member.getDeclaringClass().getName().endsWith(name.packageName()))
      && isStatic(member.getModifiers())
      && ((member instanceof Field)
          || (member instanceof Method && TypeMatching.argumentsMatch((Method) member, arguments())));
    System.err.println(": " + r);
    System.err.println("  from " + member.getDeclaringClass().getName());
    return r;
  }

  @Override
  public String toString() {
    return name.toString() + super.toString();
  }
}
