/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import java.lang.invoke.MethodHandle;
import java.util.stream.*;
import java.util.function.Function;

import static java.lang.invoke.MethodHandles.Lookup;

// TODO: rename to FunctionFinder
class StaticMethodFinder extends MethodFinder<FunctionInvocation> {

  private final Loader loader;

  enum Scope { ABSOLUTE, LOCAL, IMPORT }

  static class DefiningModule {
    private final Class<?> module;
    private final Scope scope;

    DefiningModule(Class<?> module, Scope scope) {
      this.module = module;
      this.scope = scope;
    }

    public static DefiningModule ofImport(Class<?> module) {
      return new DefiningModule(module, Scope.IMPORT);
    }

    public static DefiningModule ofAbs(Class<?> module) {
      return new DefiningModule(module, Scope.ABSOLUTE);
    }

    public static DefiningModule ofLocal(Class<?> module) {
      return new DefiningModule(module, Scope.LOCAL);
    }
  }

  StaticMethodFinder(FunctionInvocation invocation, Lookup lookup) {
    super(invocation, lookup);
    this.loader = Loader.forClass(callerClass);
  }

  @Override
  public MethodHandle find() {
    // TODO: find the function
    return null;
  }

  private Stream<? extends Member> findMembers() {
    return Stream.of(
        findStaticMethodOrFieldInClass(callerClass),
        findFQNStaticMethodOrField(),
        findStaticMethodOrFieldFromImports(),
        findConstructorForClass(invocation.PackageAndClass()),
        findConstructorFromImports())
      .reduce(Stream.empty(), Stream::concat);
  }

  private Stream<? extends Member> findStaticMethodOrFieldInClass(Class<?> lookupClass) {
    return Extractors.getMembers(lookupClass).filter(invocation::match);
  }

  private Stream<? extends Member> findFQNStaticMethodOrField() {
    if (name.hasPackage()) {
      Class<?> lookupClass = Loader.forClass(callerClass).load(name.packageName());
      return findStaticMethodOrFieldInClass(lookupClass);
    }
    return Stream.empty();
  }

  private Stream<? extends Member> findStaticMethodOrFieldFromImports() {
    return Extractors.getImportedNames(callerClass)
      .map(addFQN())
      .flatMap(name -> findStaticMethodOrFieldInClass(loader.load(name)));
  }

  private Function<String, String> addFQN() {
    if (invocation.isQualified()) {
      return moduleName -> moduleName + "." + invocation.moduleName();
    }
    return Function.identity();
  }

  private Stream<? extends Member> findConstructorForClass(String name) {
    return Extractors.getConstructors(loader.load(name)).filter(invocation::match);
  }

  private Stream<? extends Member> findConstructorFromImports() {
    return Extractors.getImportedNames(callerClass)
      .flatMap(this::addConstructorFQN)
      .flatMap(name -> findConstructorForClass(name));
  }

  private Stream<String> addConstructorFQN(String moduleName) {
    if (!invocation.isQualified() && moduleName.endsWith("." + invocation.className())) {
      return Stream.of(
          moduleName,
          moduleName + "." + invocation.packageAndClass().toString());
    }
    return Stream.of(moduleName + "." + invocation.packageAndClass().toString());
  }
}
