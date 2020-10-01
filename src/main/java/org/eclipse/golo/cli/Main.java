/*
 * Copyright (c) 2012-2020 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.cli;

import com.beust.jcommander.*;

import org.eclipse.golo.cli.command.spi.CliCommand;

import java.io.*;
import java.util.*;

import static gololang.Messages.message;

public final class Main {

  private Main() {
    // utility class
  }

  @Parameters(resourceBundle = "commands")
  static class GlobalArguments {
    @Parameter(names = {"--help"}, descriptionKey = "help", help = true)
    boolean help;

    @Parameter(names = {"--usage"}, descriptionKey = "usage", validateWith = UsageFormatValidator.class)
    String usageCommand;
  }

  public static class UsageFormatValidator implements IParameterValidator {
    static Set<String> commandNames;

    @Override
    public void validate(String name, String value) throws ParameterException {
      if (!commandNames.contains(value)) {
        throw new ParameterException(message("command_error", commandNames));
      }
    }
  }

  private static CliCommand parseArguments(String[] args) throws ParameterException {
    GlobalArguments global = new GlobalArguments();
    JCommander cmd = new JCommander(global);
    cmd.setProgramName("golo");

    ServiceLoader<CliCommand> commands = ServiceLoader.load(CliCommand.class);
    for (CliCommand command : commands) {
      cmd.addCommand(command);
    }
    UsageFormatValidator.commandNames = cmd.getCommands().keySet();

    cmd.parse(args);
    CliCommand command = null;
    if (global.usageCommand != null) {
      cmd.usage(global.usageCommand);
    } else if (global.help || cmd.getParsedCommand() == null) {
      cmd.usage();
    } else {
      String parsedCommand = cmd.getParsedCommand();
      JCommander parsedJCommander = cmd.getCommands().get(parsedCommand);
      Object commandObject = parsedJCommander.getObjects().get(0);
      if (commandObject instanceof CliCommand) {
        command = (CliCommand) commandObject;
      } else {
        throw new AssertionError("WTF?");
      }
    }
    return command;
  }

  public static void main(String... args) throws Throwable {
    try {
      CliCommand cmd = parseArguments(args);
      if (cmd != null) {
        gololang.Runtime.command(cmd);
        cmd.execute();
      }
    } catch (ParameterException exception) {
      System.err.println(exception.getMessage());
      System.out.println();
    }
  }
}
