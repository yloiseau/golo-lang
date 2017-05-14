/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.golo.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloClassLoader;
import org.eclipse.golo.compiler.GoloCompilationException;
import gololang.Messages;
import gololang.FunctionReference;
import gololang.testing.Utils;
import gololang.Predefined;


@Parameters(commandNames = {"test"}, resourceBundle = "commands", commandDescriptionKey = "test")
public class TestCommand implements CliCommand {

  /**
   * Runner name used if no CLI option is given.
   */
  public static final String DEFAULT_RUNNER_OPTION = "simple";
  /**
   * Reporter name used if no CLI option is given.
   */
  public static final String DEFAULT_REPORTER_OPTION = "minimal";

  /**
   * Module to pick an extractor from if the given option is just a function name.
   */
  public static final String DEFAULT_EXTRACTOR_MODULE = "gololang.testing.suites";
  /**
   * Module to pick a runner from if the given option is just a function name.
   */
  public static final String DEFAULT_RUNNER_MODULE = "gololang.testing.runners";
  /**
   * Module to pick a reporter from if the given option is just a function name.
   */
  public static final String DEFAULT_REPORTER_MODULE = "gololang.testing.reporters";

  /**
   * Function to use as an extractor if the given option is just a module name.
   */
  public static final String DEFAULT_EXTRACTOR_FUNCTION = "extract";
  /**
   * Function to use as an extractor if the given option is just a module name.
   */
  public static final String DEFAULT_RUNNER_FUNCTION = "run";
  /**
   * Function to use as an extractor if the given option is just a module name.
   */
  public static final String DEFAULT_REPORTER_FUNCTION = "report";

  private static final class LoadingException extends RuntimeException {
    LoadingException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  @ParametersDelegate
  ClasspathOption classpath = new ClasspathOption();

  @Parameter(names = "--tests", variableArity = true, descriptionKey = "test.tests")
  Set<String> tests = Collections.singleton(".");

  @Parameter(names = "--sources", variableArity = true, descriptionKey = "source_files")
  Set<String> sources = Collections.singleton(".");

  @Parameter(names = {"-e", "--extractors"}, variableArity = true, descriptionKey = "test.extractors")
  List<String> extractors = getDefaultExtractors();

  @Parameter(names = {"-r", "--runner"}, descriptionKey = "test.runner")
  String runner = System.getProperty("golo.testing.runner", DEFAULT_RUNNER_OPTION);

  @Parameter(names = {"-p", "--reporter"}, descriptionKey = "test.reporter")
  String reporter = System.getProperty("golo.testing.reporter", DEFAULT_REPORTER_OPTION);

  @Parameter(names = {"-o", "--output"}, descriptionKey = "test.output")
  String output = "-";

  @Parameter(names = {"-v", "--verbose"}, descriptionKey = "test.verbose")
  boolean verbose = false;

  private static List<String> getDefaultExtractors() {
    String ext = System.getProperty("golo.testing.extractors", "");
    if (ext.isEmpty()) {
      return Collections.emptyList();
    }
    return Arrays.asList(ext.split(","));
  }

  @Override
  public void execute() throws Throwable {
    GoloClassLoader loader = classpath.initGoloClassLoader();
    try {
      loadSources(loader);
      if (this.extractors.isEmpty()) {
        Messages.warning(Messages.message("no_test_extractor"));
      }
      System.exit(Utils.createTest()
          .loader(loader)
          .runner(getRunner(loader))
          .reporter(getReporter(loader))
          .extractors(getExtractors(loader))
          .output(this.output)
          .execute(this.tests.toArray()));
    } catch (LoadingException e) {
      Messages.error(e.getMessage());
      // e.printStackTrace();
      System.exit(1);
    } catch (GoloCompilationException e) {
      handleCompilationException(e);
    } catch (Throwable e) {
      handleThrowable(e);
    }
  }

  private void loadSources(GoloClassLoader loader) {
    this.sources.stream().map(Paths::get).flatMap(p -> {
      try { return Files.walk(p); }
      catch (IOException e) { throw new LoadingException(Messages.message("file_not_found", p), e); }
    }).filter(p -> p.toString().endsWith(".golo"))
      .forEach(path -> loadGoloFile(loader, path));
  }


  private Object[] getExtractors(ClassLoader loader) {
    FunctionReference[] extractors = new FunctionReference[this.extractors.size()];
    for (int i = 0; i < this.extractors.size(); i++) {
      extractors[i] = getFunctionFromString("extractor",
          this.extractors.get(i),
          DEFAULT_EXTRACTOR_MODULE,
          DEFAULT_EXTRACTOR_FUNCTION,
          loader, 2);
    }
    return extractors;
  }

  private FunctionReference getRunner(ClassLoader loader) {
    return getFunctionFromString("runner", this.runner, DEFAULT_RUNNER_MODULE, DEFAULT_RUNNER_FUNCTION, loader, 1);
  }

  private FunctionReference getReporter(ClassLoader loader) {
    return getFunctionFromString("reporter", this.reporter, DEFAULT_REPORTER_MODULE, DEFAULT_REPORTER_FUNCTION, loader, 2);
  }

  private FunctionReference getFunction(Class<?> klass, String name, int arity) {
    try {
      return Predefined.fun(klass, name, arity, false);
    } catch (Throwable e) {
      throw new LoadingException(Messages.message("function_lookup_failed", name, klass.getName()), e);
    }
  }

  private FunctionReference getFunctionFromString(String kind, String desc, String defaultModule, String defaultFunction, ClassLoader loader, int arity) {
    String[] parts = desc.split("::");
    String moduleName;
    String functionName;
    if (parts.length == 1) {
      if (parts[0].contains(".")) {
        moduleName = parts[0];
        functionName = defaultFunction;
      } else {
        functionName = parts[0];
        moduleName = defaultModule;
      }
    } else if (parts.length == 2) {
      moduleName = parts[0];
      functionName = parts[1];
    } else {
      throw new IllegalArgumentException(Messages.message("invalid_function_ident"));
    }
    try {
      if (this.verbose) {
        Messages.info(Messages.message("use_test_function", kind, moduleName, functionName));
      }
      return getFunction(Class.forName(moduleName, true, loader), functionName, arity);
    } catch (ClassNotFoundException e) {
      throw new LoadingException(Messages.message("module_not_found", moduleName), e);
    }
  }

  private Class<?> loadGoloFile(GoloClassLoader loader, Path path) {
    try {
      return loader.load(path);
    } catch (IOException e) {
      throw new LoadingException(Messages.message("file_not_found", path), e);
    }
  }
}
