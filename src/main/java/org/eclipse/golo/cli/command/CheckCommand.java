/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.cli.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import java.io.File;
import java.nio.file.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.GoloCompiler;

import static gololang.Messages.*;

@Parameters(commandNames = {"check"}, resourceBundle = "commands", commandDescriptionKey = "check")
public class CheckCommand implements CliCommand {

  @Parameter(names = {"--exit"}, descriptionKey = "check.exit")
  boolean exit = false;

  @Parameter(names = {"--verbose"}, descriptionKey = "check.verbose")
  boolean verbose = false;

  @Parameter(descriptionKey = "source_files")
  List<String> files = new LinkedList<>();

  @Override
  public void execute() throws Throwable {
    GoloCompiler compiler = new GoloCompiler();
    for (String file : this.files) {
      check(Paths.get(file), compiler);
    }
  }

  private void checkFile(Path file, GoloCompiler compiler) {
    try {
      if (verbose) {
        System.err.println(">>> " + message("check_info", file));
      }
      compiler.resetExceptionBuilder();
      compiler.check(compiler.parse(file));
    } catch (IOException e) {
      error(message("file_not_found", file));
    } catch (GoloCompilationException e) {
      handleCompilationException(e, exit);
    }
  }


  private void check(Path file, GoloCompiler compiler) throws IOException {
    if (Files.isDirectory(file)) {
      PathMatcher goloFiles = FileSystems.getDefault().getPathMatcher("glob:**/*.golo");
      Files.walk(file)
          .filter(path -> goloFiles.matches(path))
          .forEach(path -> checkFile(path, compiler));
    } else {
      checkFile(file, compiler);
    }
  }
}

