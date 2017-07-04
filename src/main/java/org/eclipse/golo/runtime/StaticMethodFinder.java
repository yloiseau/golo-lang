/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

// TODO: we probably should reorder arguments and adapt for SAM/FI for every method (being static or not)

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
    System.err.println("\n\n====================================");
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
    return Stream.concat(Stream.of(callerClass.getName()), Extractors.getImportedNames(callerClass))
      .flatMap(resolver())
      .flatMap(this::findMemberFor);
  }

  private Function<String, Stream<FunctionInvocation>> resolver() {
    if (invocation.isQualified()) {
      return s -> Stream.of(invocation, invocation.resolve(s));
    }
    return s -> Stream.of(invocation.resolve(s));
  }

  private Stream<? extends Member> findMemberFor(FunctionInvocation invoke) {
    System.err.println("   resolved: " + invoke);
    Class<?> cls = loader.load(invoke.moduleName());
    return Stream.concat(
        Extractors.getMembers(loader.load(invoke.moduleName())),
        Extractors.getConstructors(loader.load(invoke.packageAndClass().toString())))
      .map(m -> {
        System.err.println("  -> " + m);
        return m;
      })
      .filter(invoke::match);
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
