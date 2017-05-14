/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package gololang.testing;

import gololang.FunctionReference;
import gololang.Predefined;
import org.eclipse.golo.compiler.GoloClassLoader;
import java.util.stream.Stream;
import java.nio.file.*;
import java.io.*;
import java.util.List;
import java.util.LinkedList;
import java.util.Objects;

import gololang.IO;
import gololang.Messages;

import static java.util.stream.Collectors.toList;

/**
 * Misc utility functions to help creating test components (extractors, runners and reporters).
 */
public final class Utils {

  private Utils() {
    // utility class
  }

  /**
   * Walk a path and select all golo files.
   * @param path the path to search for golo files
   * @return a stream of golo file paths.
   */
  public static Stream<Path> goloFiles(Object path) throws IOException {
    return Files.walk(gololang.IO.toPath(path))
      .filter(p -> p.toString().endsWith(".golo"))
      .map(Path::toAbsolutePath);
  }

  public static List<Class<?>> getModulesFrom(Object rootPath, GoloClassLoader loader) throws IOException {
    return goloFiles(rootPath)
      .map(path -> {
        try {
          return loader.load(path);
        } catch (IOException e) {
          Messages.warning(Messages.message("file_not_found", path));
          return null;
        }
      }).filter(Objects::nonNull).collect(toList());
  }

  /**
   * Builder for a complete test command.
   */
  public static Test createTest() {
    return new Test();
  }

  public static class Test {
    private FunctionReference runner;
    private FunctionReference reporter;
    private List<FunctionReference> extractors = new LinkedList<>();
    private GoloClassLoader loader;
    private String output;

    public Test runner(FunctionReference fun) {
      this.runner = fun;
      return this;
    }

    public Test reporter(FunctionReference fun) {
      this.reporter = fun;
      return this;
    }

    public Test extractors(Object... funs) {
      for (Object f : funs) {
        this.extractor(f);
      }
      return this;
    }

    public Test extractor(Object fun) {
      Predefined.require(fun instanceof FunctionReference, "The extractor must be a function reference");
      this.extractors.add((FunctionReference) fun);
      return this;
    }

    public Test loader(GoloClassLoader loader) {
      this.loader = loader;
      return this;
    }

    public Test output(String output) {
      this.output = output;
      return this;
    }

    public int execute(Object... testFiles) throws Throwable {
      List<Object> suites = new LinkedList<>();
      GoloClassLoader localLoader = this.loader;
      if (localLoader == null) {
        localLoader = gololang.Runtime.classLoader();
      }
      for (Object test : testFiles) {
        for (FunctionReference extractor : this.extractors) {
          @SuppressWarnings("unchecked")
          Iterable<Object> subSuites = (Iterable<Object>) extractor.invoke(test, localLoader);
          subSuites.forEach(suites::add);
        }
      }
      return (int) this.reporter.invoke(this.runner.invoke(suites), this.output);
    }
  }

}
