/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package fr.insalyon.citi.golo.internal.testing;

import fr.insalyon.citi.golo.compiler.GoloClassLoader;
import fr.insalyon.citi.golo.compiler.GoloCompilationException;
import fr.insalyon.citi.golo.compiler.parser.ParseException;
import org.testng.Reporter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class TestUtils {

  public static Iterator<Object[]> goloFilesIn(String path) {
    List<Object[]> data = new LinkedList<>();
    File[] files = new File(path).listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".golo");
      }
    });
    for (File file : files) {
      data.add(new Object[]{file});
    }
    return data.iterator();
  }

  public static Class<?> compileAndLoadGoloModule(String sourceFolder, String goloFile) throws IOException, ParseException, ClassNotFoundException {
    return compileAndLoadGoloModule(sourceFolder, goloFile, new GoloClassLoader(TestUtils.class.getClassLoader()));
  }

  public static Class<?> compileAndLoadGoloModule(String sourceFolder, String goloFile, GoloClassLoader goloClassLoader) throws IOException, ParseException, ClassNotFoundException {
    try {
      return goloClassLoader.load(goloFile, new FileInputStream(sourceFolder + goloFile));
    } catch (GoloCompilationException e) {
      for (GoloCompilationException.Problem p : e.getProblems()) {
        Reporter.log("In " + goloFile + ": " + p.getDescription(), shouldTestNgReportToConsole());
      }
      throw e;
    }
  }

  private static boolean shouldTestNgReportToConsole() {
    return Boolean.valueOf(System.getProperty("testng-report-to-console", "false"));
  }
}
