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
import java.nio.charset.Charset;
import java.nio.file.*;
import java.io.*;
import java.util.List;
import java.util.LinkedList;

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
  public static Stream<Path> goloFiles(Path path) throws IOException {
    return Files.walk(path)
      .filter(p -> p.toString().endsWith(".golo"))
      .map(Path::toAbsolutePath);
  }

  /**
   * Create an {@code PrintStream} from the specified value.
   * <p>
   * If the given string is "-", {@link java.lang.System.out} is used. Otherwise, a {@link java.nio.file.Path} is created with {@link gololang.Predefined.toPath}.
   * The returned {@code PrintStream} is buffered and uses the default charset. Parent directory is created. If the file
   * exists, it is overwritten.
   *
   * @param output the file to use; "-" means standard output
   * @return a buffered {@code PrintStream} or {@link java.lang.System.out}
   * @see java.nio.charset.Charset.defaultCharset
   * @see gololang.Predefined.toPath
   */
  public static PrintStream printStreamFrom(Object output) throws IOException {
    return printStreamFrom(output, Charset.defaultCharset().name());
  }

  /**
   * Create an {@code PrintStream} from the specified value.
   * <p>
   * If the given string is "-", {@link java.lang.System.out} is used. Otherwise, a {@link java.nio.file.Path} is created with {@link gololang.Predefined.toPath}.
   * The returned {@code PrintStream} is buffered and uses the given charset. Parent directory is created. If the file
   * exists, it is overwritten.
   *
   * @param output the file to use; "-" means standard output
   * @param charset the charset to use, as a {@link java.lang.String} or a {@link java.nio.charset.Charset}
   * @return a buffered {@code PrintStream} or {@link java.lang.System.out}
   * @see java.nio.charset.Charset.defaultCharset
   * @see gololang.Predefined.toPath
   */
  public static PrintStream printStreamFrom(Object output, Object charset) throws IOException {
    if ("-".equals(output)) {
      return System.out;
    }
    if (output instanceof PrintStream) {
      return (PrintStream) output;
    }
    OutputStream out;
    if (output instanceof OutputStream) {
      out = (OutputStream) output;
    } else {
      Path outputPath = gololang.IO.toPath(output);
      if (outputPath.getParent() != null) {
        Files.createDirectories(outputPath.getParent());
      }
      out = Files.newOutputStream(outputPath);
    }
    String encoding;
    if (charset instanceof String) {
      encoding = (String) charset;
    } else {
      Predefined.require(charset instanceof Charset, "not a charset");
      encoding = ((Charset) charset).name();
    }
    return new PrintStream(
      new BufferedOutputStream(out),
      true,
      encoding);
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
