/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.cli.command.spi;

import org.eclipse.golo.compiler.GoloCompilationException;
import gololang.Messages;
import gololang.Runtime;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.invoke.MethodType.methodType;

public interface CliCommand {

  class NoMainMethodException extends NoSuchMethodException {
  }

  void execute() throws Throwable;

  default void callRun(Class<?> klass, String[] arguments) throws Throwable {
    MethodHandle main;
    try {
      main = publicLookup().findStatic(klass, "main", methodType(void.class, String[].class));
    } catch (NoSuchMethodException e) {
      throw new NoMainMethodException().initCause(e);
    }
    main.invoke(arguments);
  }

  default void handleCompilationException(GoloCompilationException e) {
    handleCompilationException(e, true, Runtime.debugMode());
  }

  default void handleCompilationException(GoloCompilationException e, boolean exit) {
    handleCompilationException(e, exit, Runtime.debugMode());
  }

  default void handleCompilationException(GoloCompilationException e, boolean exit, boolean withStack) {
    handleThrowable(e, false);
    Messages.error(e.getMessage());
    for (GoloCompilationException.Problem problem : e.getProblems()) {
      Messages.error(problem.getDescription());
    }
    if (exit) {
      System.exit(1);
    }
  }

  default void handleThrowable(Throwable e) {
    handleThrowable(e, true);
  }

  default void handleThrowable(Throwable e, boolean exit) {
    handleThrowable(e, exit, gololang.Runtime.debugMode() || gololang.Runtime.showStackTrace());
  }

  default void handleThrowable(Throwable e, boolean exit, boolean withStack) {
    Messages.error(e);
    if (e.getCause() != null) {
      Messages.error(e.getCause().getMessage());
    }
    if (withStack) {
      Messages.printStackTrace(e);
    } else {
      Messages.error(Messages.message("use_debug"));
    }
    if (withStack) {
      e.printStackTrace();
    }
    if (exit) {
      System.exit(1);
    }
  }
}
