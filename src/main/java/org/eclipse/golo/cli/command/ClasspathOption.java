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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.LinkedList;

import org.eclipse.golo.compiler.GoloClassLoader;

class ClasspathOption {

  @Parameter(names = "--classpath", variableArity = true, description = "Classpath elements (.jar and directories)")
  List<String> classpath = new LinkedList<>();

  private static URLClassLoader primaryClassLoader(List<String> classpath) throws MalformedURLException {
    URL[] urls = new URL[classpath.size() + 1];
    urls[0] = new File(".").toURI().toURL();
    for (int i = 0; i < classpath.size(); i++) {
      urls[i + 1] = new File(classpath.get(i)).toURI().toURL();
    }
    return new URLClassLoader(urls);
  }

  static GoloClassLoader initGoloClassLoader(List<String> classpath) throws MalformedURLException {
    URLClassLoader primaryClassLoader = primaryClassLoader(classpath);
    GoloClassLoader loader = new GoloClassLoader(primaryClassLoader);
    Thread.currentThread().setContextClassLoader(loader);
    return loader;
  }

  GoloClassLoader initGoloClassLoader() throws MalformedURLException {
    return initGoloClassLoader(this.classpath);
  }


}
