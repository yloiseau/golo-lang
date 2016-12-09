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
import org.eclipse.golo.cli.command.spi.CliCommand;

@Parameters(commandNames = {"version"}, resourceBundle = "commands", commandDescriptionKey = "version")
public class VersionCommand implements CliCommand {

  @Parameter(names = "--full", description = "version.full")
  boolean full = false;

  @Override
  public void execute() throws Throwable {
    if (this.full) {
      System.out.format("Golo: %s (build %s)%n", Metadata.VERSION, Metadata.TIMESTAMP);
      System.out.format(" JVM: %s %s%n", System.getProperty("java.vendor"), System.getProperty("java.version"));
      System.out.format("  OS: %s %s %s%n",
          System.getProperty("os.name"),
          System.getProperty("os.arch"),
          System.getProperty("os.version"));
    } else {
      System.out.println(Metadata.VERSION);
    }
  }
}
