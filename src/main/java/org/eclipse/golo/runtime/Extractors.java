/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.stream.Stream;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

import static org.eclipse.golo.runtime.TypeMatching.argumentsNumberMatches;
import static org.eclipse.golo.runtime.DecoratorsHelper.isMethodDecorated;

public final class Extractors {

  /**
   * Define the priority order between methods.
   *
   * <ol>
   * <li>defined in the most specific class
   * <li>greatest arity first
   * <li>fixed arity then varargs
   * </ol>
   */
  public static final Comparator<Method> METHOD_COMPARATOR = (m1, m2) -> {
    Class<?> m1Class = m1.getDeclaringClass();
    Class<?> m2Class = m2.getDeclaringClass();
    if (m1Class.isAssignableFrom(m2Class) && !m1Class.equals(m2Class)) {
      return 1;
    }
    if (m2Class.isAssignableFrom(m1Class) && !m1Class.equals(m2Class)) {
      return -1;
    }
    if (m1.getParameterCount() != m2.getParameterCount()) {
      return -1 * Integer.compare(m1.getParameterCount(), m2.getParameterCount());
    }
    if (m1.isVarArgs() && !m2.isVarArgs()) {
      return 1;
    }
    if (m2.isVarArgs() && !m1.isVarArgs()) {
      return -1;
    }
    return 0;
  };



  private Extractors() {
    throw new UnsupportedOperationException("don't instantiate");
  }

  public static Stream<Constructor<?>> getConstructors(Class<?> klass) {
    if (klass == null) {
      return Stream.empty();
    }
    return Stream.of(klass.getConstructors());
  }

  public static Stream<Method> getMethods(Class<?> klass) {
    if (klass == null) {
      return Stream.empty();
    }
    return Stream.concat(
        Stream.of(klass.getDeclaredMethods()),
        Stream.of(klass.getMethods()))
      .sorted(METHOD_COMPARATOR)
      .distinct();
  }

  public static Stream<Field> getFields(Class<?> klass) {
    if (klass == null) {
      return Stream.empty();
    }
    return Stream.concat(
        Stream.of(klass.getDeclaredFields()),
        Stream.of(klass.getFields()))
      .distinct();
  }

  public static Stream<String> getImportedNames(Class<?> klass) {
    if (klass == null) {
      return Stream.empty();
    }
    return Stream.of(Module.imports(klass));
  }

  public static Stream<Member> getMembers(Class<?> klass) {
    if (klass == null) {
      return Stream.empty();
    }
    return Stream.concat(getMethods(klass), getFields(klass));
  }

  public static boolean isPublic(Member m) {
    return m != null && Modifier.isPublic(m.getModifiers());
  }

  public static boolean isPrivate(Member m) {
    return m != null && Modifier.isPrivate(m.getModifiers());
  }

  public static boolean isStatic(Member m) {
    return m != null && Modifier.isStatic(m.getModifiers());
  }

  public static boolean isConcrete(Member m) {
    return m != null && !Modifier.isAbstract(m.getModifiers());
  }

  public static Predicate<Member> isNamed(String name) {
    return m -> m != null && m.getName().equals(name);
  }

  public static Predicate<Class<?>> isAssignableFrom(Class<?> receiver) {
    return target -> target != null && target.isAssignableFrom(receiver);
  }

  public static Predicate<Method> matchFunctionReference(String name, int arity, boolean varargs) {
    return m ->
      m.getName().equals(name)
      && (isMethodDecorated(m) || argumentsNumberMatches(m.getParameterCount(), arity, varargs))
      && (arity < 0 || m.isVarArgs() == varargs);
  }

}
