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
import java.lang.reflect.*;
import java.util.Optional;

import org.eclipse.golo.compiler.PackageAndClass;

import static java.lang.invoke.MethodHandles.Lookup;

class StaticMethodFinder extends MethodFinder<FunctionInvocation> {

  private final Loader loader;

  StaticMethodFinder(FunctionInvocation invocation, Lookup lookup) {
    super(invocation, lookup);
    this.loader = Loader.forClass(callerClass);
  }

  @Override
  public MethodHandle find() {
    System.err.println("# Looking for " + invocation);
    return findMembers()
      .map(m -> {
        System.err.println("## Found " + m.getClass().getSimpleName() + ": " + m);
        return m;
      })
      .map(this::toMethodHandle)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .findFirst()
      .orElse(null);
  }

  private Stream<? extends Member> findMembers() {
    return Stream.of(
        findStaticMethodOrFieldInClass(callerClass),
        findFQNStaticMethodOrField(),
        findStaticMethodOrFieldFromImports(),
        findConstructorForClass(invocation.packageAndClass().toString()),
        findConstructorFromImports()
      ).reduce(Stream.empty(), Stream::concat);
  }

  private Stream<? extends Member> findStaticMethodOrFieldInClass(Class<?> lookupClass) {
    return Extractors.getMembers(lookupClass).filter(invocation::match);
  }

  private Stream<? extends Member> findFQNStaticMethodOrField() {
    if (invocation.isQualified()) {
      return findStaticMethodOrFieldInClass(loader.load(invocation.moduleName()));
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
    if (!invocation.isQualified() && moduleName.endsWith("." + invocation.baseName())) {
      return Stream.of(
          moduleName,
          moduleName + "." + invocation.packageAndClass().toString());
    }
    return Stream.of(moduleName + "." + invocation.packageAndClass().toString());
  }

  private Optional<MethodHandle> toMethodHandle(Constructor<?> constructor) {
    MethodHandle handle;
    try {
      handle = lookup.unreflectConstructor(constructor);
    } catch (IllegalAccessException e) {
      return Optional.empty();
    }
    // TODO: reorder named arguments ?
    handle = FunctionCallSupport.insertSAMFilter(invocation.coerce(handle), lookup, constructor.getParameterTypes(), 0);
    return Optional.of(handle);
  }

  @Override
  protected Optional<MethodHandle> toMethodHandle(Method method) {
    // TODO: move insertSAMFilter to invocation.coerce or super.toMethodHandle ?
    if (Extractors.isPrivate(method) && PackageAndClass.of(callerClass.getName()).isInnerClassOf(PackageAndClass.of(method))) {
      method.setAccessible(true);
    }
    return super.toMethodHandle(method).map(h -> FunctionCallSupport.insertSAMFilter(h, lookup, method.getParameterTypes(), 0));
  }

  private Optional<MethodHandle> toMethodHandle(Member result) {
    if (result instanceof Method) {
      return toMethodHandle((Method) result);
    } else if (result instanceof Constructor) {
      return toMethodHandle((Constructor<?>) result);
    } else {
      return toMethodHandle((Field) result);
    }
  }
}
