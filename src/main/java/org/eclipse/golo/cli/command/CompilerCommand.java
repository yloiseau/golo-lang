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
import com.beust.jcommander.ParametersDelegate;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.GoloCompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static gololang.Messages.*;

@Parameters(commandNames = {"compile"}, resourceBundle = "commands", commandDescriptionKey = "compile")
public class CompilerCommand implements CliCommand {

  @Parameter(names = "--output", descriptionKey = "compile.output")
  String output = ".";

  @Parameter(descriptionKey = "source_files")
  List<String> sources = new LinkedList<>();

  @ParametersDelegate
  ClasspathOption classpath = new ClasspathOption();

  @Override
  public void execute() throws Throwable {
    // TODO: recurse into directories
    GoloCompiler compiler = classpath.initGoloClassLoader().getCompiler();
    try {
      if (this.output.endsWith(".jar")) {
        compileJar(compiler);
      } else {
        compileClasses(compiler);
      }
    } catch (GoloCompilationException e) {
      handleCompilationException(e);
    }
  }

  private void compileClasses(GoloCompiler compiler) throws Throwable {
    File outputDir = new File(this.output);
    for (String source : this.sources) {
      File file = new File(source);
      try (FileInputStream in = new FileInputStream(file)) {
        compiler.compileTo(file.getName(), in, outputDir);
      } catch (IOException e) {
        error(message("file_not_found", source));
      }
    }
  }

  private void compileJar(GoloCompiler compiler) throws Throwable {
    try (JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(new File(this.output)), manifest())) {
      for (String source : this.sources) {
        File file = new File(source);
        try (FileInputStream in = new FileInputStream(file)) {
          compiler.compileToJar(file.getName(), in, jarOutputStream);
        } catch (IOException e) {
          error(message("file_not_found", source));
        }
      }
    }
  }

  private Manifest manifest() {
    Manifest manifest = new Manifest();
    Attributes attributes = manifest.getMainAttributes();
    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    attributes.put(new Attributes.Name("Created-By"), "Eclipse Golo " + Metadata.VERSION);
    return manifest;
  }
}
