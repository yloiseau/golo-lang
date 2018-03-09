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

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import org.eclipse.golo.cli.command.spi.CliCommand;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.GoloCompiler;
import org.eclipse.golo.compiler.ir.GoloModule;
import org.eclipse.golo.compiler.ir.IrTreeDumper;
import org.eclipse.golo.compiler.parser.ASTCompilationUnit;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import static gololang.Messages.*;

@Parameters(commandNames = {"diagnose"}, resourceBundle = "commands", commandDescriptionKey = "diagnose")
public class DiagnoseCommand implements CliCommand {

  @Parameter(names = "--tool", hidden = true, descriptionKey = "diagnose.tool", validateWith = DiagnoseModeValidator.class)
  String mode = "ir";

  @Parameter(names = "--stage", descriptionKey = "diagnose.stage", validateWith = DiagnoseStageValidator.class)
  String stage = "refined";

  @Parameter(description = "source_files")
  List<String> files = new LinkedList<>();

  @ParametersDelegate
  ClasspathOption classpath = new ClasspathOption();

  GoloCompiler compiler = new GoloCompiler();

  @Override
  public void execute() throws Throwable {
    if ("ast".equals(this.stage) && !"ast".equals(this.mode)) {
      this.mode = "ast";
    }
    if ("ast".equals(this.mode) && !"ast".equals(this.stage)) {
      this.stage = "ast";
    }

    classpath.initGoloClassLoader();
    try {
      switch (this.mode) {
        case "ast":
          dumpASTs(this.files);
          break;
        case "ir":
          dumpIRs(this.files);
          break;
        default:
          throw new AssertionError("WTF?");
      }
    } catch (GoloCompilationException e) {
      handleCompilationException(e);
    }
  }


  private void dumpASTs(List<String> files) {
    for (String file : files) {
      dumpAST(file);
    }
  }

  private void dumpAST(String goloFile) {
    File file = new File(goloFile);
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          dumpAST(directoryFile.getAbsolutePath());
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      System.out.println(">>> AST: " + goloFile);
      try {
        ASTCompilationUnit ast = compiler.parse(file.toPath());
        ast.dump("% ");
        System.out.println();
      } catch (IOException e) {
        error(message("file_not_found", goloFile));
      }
    }
  }

  private void dumpIRs(List<String> files) {
    IrTreeDumper dumper = new IrTreeDumper();
    for (String file : files) {
      dumpIR(file, dumper);
    }
  }

  private void dumpIR(String goloFile, IrTreeDumper dumper) {
    File file = new File(goloFile);
    if (file.isDirectory()) {
      File[] directoryFiles = file.listFiles();
      if (directoryFiles != null) {
        for (File directoryFile : directoryFiles) {
          dumpIR(directoryFile.getAbsolutePath(), dumper);
        }
      }
    } else if (file.getName().endsWith(".golo")) {
      System.out.println(">>> IR: " + file);
      try {
        GoloModule module = compiler.transform(compiler.parse(file.toPath()));
        if ("refined".equals(this.stage)) {
          compiler.refine(module);
        }
        module.accept(dumper);
      } catch (IOException e) {
        error(message("file_not_found", goloFile));
      }
      System.out.println();
    }
  }

  public static final class DiagnoseModeValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
      warning(message("diagnose_tool_warning"));
      switch (value) {
        case "ast":
        case "ir":
          return;
        default:
          throw new ParameterException(message("diagnose_tool_error", "{ast, ir}"));
      }
    }
  }

  public static final class DiagnoseStageValidator implements IParameterValidator {

    @Override
    public void validate(String name, String value) throws ParameterException {
      switch (value) {
        case "ast":
        case "raw":
        case "refined":
          return;
        default:
          throw new ParameterException(message("diagnose_stage_error", "{ast, raw, refined}"));
      }
    }
  }

}
