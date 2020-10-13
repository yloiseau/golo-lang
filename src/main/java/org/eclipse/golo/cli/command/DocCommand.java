/*
 * Copyright (c) 2012-2020 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.cli.command;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompiler;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.doc.AbstractProcessor;
import org.eclipse.golo.doc.CtagsProcessor;
import org.eclipse.golo.doc.HtmlProcessor;
import org.eclipse.golo.doc.MarkdownProcessor;
import org.eclipse.golo.doc.ManProcessor;
import org.eclipse.golo.doc.ModuleDocumentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.function.Supplier;
import java.util.*;

import static gololang.Messages.*;

@Parameters(commandNames = {"doc"}, commandDescriptionKey = "doc", resourceBundle = "commands")
public class DocCommand implements CliCommand {

  @Parameter(names = "--format", descriptionKey = "doc.format", validateWith = DocFormatValidator.class)
  String format = "html";

  @Parameter(names = "--output", descriptionKey = "doc.output")
  String output = ".";

  @Parameter(descriptionKey = "source_files")
  List<String> sources = new LinkedList<>();

  @ParametersDelegate
  ClasspathOption classpath = new ClasspathOption();

  private static final Map<String, Supplier<? extends AbstractProcessor>> FORMATS = new HashMap<>();
  static {
    FORMATS.put("markdown", MarkdownProcessor::new);
    FORMATS.put("html", HtmlProcessor::new);
    FORMATS.put("ctags", CtagsProcessor::new);
    FORMATS.put("man", ManProcessor::new);
  }

  private GoloCompiler compiler;

  @Override
  public void execute() throws Throwable {
    compiler = classpath.initGoloClassLoader().getCompiler();
    AbstractProcessor processor = FORMATS.get(this.format).get();
    HashSet<ModuleDocumentation> modules = new HashSet<>();
    for (String source : this.sources) {
      loadGoloFile(source, modules);
    }
    try {
      processor.process(modules, Paths.get(this.output));
    } catch (Throwable throwable) {
      handleThrowable(throwable);
    }
  }

  private void loadGoloFile(String goloFile, HashSet<ModuleDocumentation> modules) {
    File file = new File(goloFile);
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          loadGoloFile(directoryFile.getAbsolutePath(), modules);
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      try {
        modules.add(ModuleDocumentation.load(goloFile, compiler));
      } catch (IOException e) {
        error(message("file_not_found", goloFile));
        return;
      } catch (GoloCompilationException e) {
        handleCompilationException(e);
      } catch (Throwable t) {
        handleThrowable(t);
      }
    }
  }

  public static class DocFormatValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
      if (!FORMATS.keySet().contains(value)) {
        throw new ParameterException(message("format_error", FORMATS.keySet()));
      }
    }
  }
}
