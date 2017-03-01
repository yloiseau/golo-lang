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
import org.eclipse.golo.cli.command.spi.CliCommand;

import java.util.LinkedList;
import java.util.List;


@Parameters(commandNames = {"run"}, commandDescription = "Runs compiled Golo code")
public class RunCommand implements CliCommand {

  @Parameter(names = "--module", description = "The Golo module with a main function", required = true)
  String module;

  @Parameter(description = "Program arguments")
  List<String> arguments = new LinkedList<>();

  @ParametersDelegate
  ClasspathOption classpath = new ClasspathOption();

  @Override
  public void execute() throws Throwable {

    try {
      Class<?> module = Class.forName(this.module, true, classpath.initGoloClassLoader());
      callRun(module, this.arguments.toArray(new String[this.arguments.size()]));
    } catch (ClassNotFoundException e) {
      System.out.println("The module " + this.module + " could not be loaded.");
    } catch (NoSuchMethodException e) {
      System.out.println("The module " + this.module + " does not have a main method with an argument.");
    }
  }
}
