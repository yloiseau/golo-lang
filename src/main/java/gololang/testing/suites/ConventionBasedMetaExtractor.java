/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package gololang.testing.suites;

import gololang.Tuple;
import gololang.FunctionReference;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Objects;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.io.IOException;

import org.eclipse.golo.compiler.GoloClassLoader;
import gololang.testing.Utils;

import static java.lang.reflect.Modifier.isStatic;
import static java.lang.reflect.Modifier.isPublic;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.util.stream.Collectors.toList;
import static gololang.Predefined.require;


public final class ConventionBasedMetaExtractor {

  private Function<Method, String> testDescriptor = Method::getName;
  private Predicate<Method> functionSelector = Objects::nonNull;;
  private Function<Class<?>, String> suiteDescriptor = Class::getName;
  private Predicate<Class<?>> moduleSelector = m -> true;
  private Predicate<Path> fileSelector = Objects::nonNull;

  public ConventionBasedMetaExtractor describeTestWith(Function<Method, String> d) {
    testDescriptor = d;
    return this;
  }

  public ConventionBasedMetaExtractor selectFunctionsWith(Predicate<Method> s) {
    functionSelector = s;
    return this;
  }

  public ConventionBasedMetaExtractor describeSuiteWith(Function<Class<?>, String> d) {
    suiteDescriptor = d;
    return this;
  }

  public ConventionBasedMetaExtractor selectFilesWith(Predicate<Path> s) {
    fileSelector = s;
    return this;
  }

  public ConventionBasedMetaExtractor selectModuleWith(Predicate<Class<?>> s) {
    moduleSelector = s;
    return this;
  }

  public Object extract(Object path, Object loader) throws IOException {
    require(path instanceof Path, "first argument must be a Path");
    require(loader instanceof GoloClassLoader,  "second argument must be a GoloClassLoader");
    return getModules((Path) path, (GoloClassLoader) loader)
      .filter(Objects::nonNull).filter(moduleSelector)
      .map(m -> new Tuple(suiteDescriptor.apply(m), getTests(m)))
      .collect(toList());
  }

  /**
   * @return a collection of test case as [desc, function] tuples.
   */
  private Collection<Tuple> getTests(Class<?> module) {
    Set<Tuple> tests = new LinkedHashSet<>();
    for (Method m : module.getDeclaredMethods()) {
      if (m.getParameterCount() != 0) { continue; }
      if (!isStatic(m.getModifiers())) { continue; }
      if (!isPublic(m.getModifiers())) { continue; }
      if (!functionSelector.test(m)) { continue; }
      FunctionReference testFunction;
      try {
        testFunction = new FunctionReference(publicLookup().unreflect(m));
      } catch (IllegalAccessException e) {
        System.err.println("[warning] Can't access function " + m);
        continue;
      }
      String testDescription = testDescriptor.apply(m);
      tests.add(new Tuple(testDescription, testFunction));
    }
    return tests;
  }

  private Stream<Class<?>> getModules(Path rootPath, GoloClassLoader loader) throws IOException {
    return Utils.goloFiles(rootPath)
      .filter(fileSelector)
      .map(path -> {
        try {
          return loader.load(path);
        } catch (IOException e) {
          System.err.println("[warning] can't load file " + path);
          return null;
        }
      });
  }

}
