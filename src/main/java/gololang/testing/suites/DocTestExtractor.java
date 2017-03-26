/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package gololang.testing.suites;

import java.nio.file.*;
import java.util.*;

import static java.util.stream.Collectors.toList;

import org.eclipse.golo.compiler.GoloCompiler;
import org.eclipse.golo.compiler.parser.ASTCompilationUnit;
import org.eclipse.golo.doc.FunctionDocumentation;
import org.eclipse.golo.doc.ModuleDocumentation;
import org.eclipse.golo.doc.AbstractProcessor;

import com.github.rjeschke.txtmark.BlockEmitter;
import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;

import gololang.EvaluationEnvironment;
import gololang.testing.Utils;
import static gololang.Predefined.require;

/**
 * Doctest Suites Extractor.
 * <p>
 * This is a test suite extractor that create a test for a function from the code samples in its documentation.
 * <p>
 * For instance, a golo function declared as:
 * <pre class="listing"><code class="lang-golo" data-lang="golo">
 * ----
 * A function to say plop to someone
 *
 * # Example
 *
 * ```golo test
 * require(plop("Zaphod") == "plop Zaphod", "error saying plop")
 * ```
 * ----
 * function plop = |name| -> ...
 * </code></pre>
 * will generate a test equivalent to:
 * <pre class="listing"><code class="lang-golo" data-lang="golo">
 * ["test for plop", { require(plop("Zaphod") == "plop Zaphod", "error saying plop") }]
 * </code></pre>
 * <p>
 * To include some code that must not appear in the documentation, use the {@code hidden} meta, as in:
 * <pre class="listing"><code class="lang-golo" data-lang="golo">
 * ----
 * Module documentation...
 *
 * ```test hidden
 * import org.hamcrest.MatcherAssert
 * import org.hamcrest.Matchers
 * ```
 * ----
 * module MyModule
 * </code></pre>
 */
public final class DocTestExtractor {

  private DocTestExtractor() {
    // utility class
  }

  /**
   * Suite extractor.
   * <p>
   * Walk the given path looking for Golo files, extract the documentation code samples, and generate a list of suites.
   * <p>
   * A suite is created for each module, and a test is created for each function.
   * <p>
   * The module itself is <em>not</em> loaded, so the source must also be specified in the {@code --sources} argument of
   * the {@code test} command, or be compiled separately and available in the classpath.
   */
  public static Object extract(Object path, Object loader) throws Throwable {
    require(path instanceof Path, "first argument must be a Path");
    TestModuleBuilder builder = new TestModuleBuilder();
    builder.process(parseGoloFiles((Path) path), null);
    return builder.getSuites();
  }

  private static Map<String, ASTCompilationUnit> parseGoloFiles(Path path) throws Throwable {
    GoloCompiler compiler = new GoloCompiler();
    System.out.println("[info] Looking for doctests in " + path);
    List<String> files = Utils.goloFiles(path)
        .map(Path::toString)
        .collect(toList());
    Map<String, ASTCompilationUnit> units = new LinkedHashMap<>();
    for (String filename : files) {
      System.out.println("[debug] parse " + filename);
      units.put(filename, compiler.parse(filename));
    }
    return units;
  }

  private static final class TestModuleBuilder extends AbstractProcessor {

    private static final String FUNCTION_NAME = "$suites";

    private Configuration config;
    private List<Object> suites = new LinkedList<>();
    private StringBuilder code;
    private boolean indent = false;

    private void createBuilder() {
      code = new StringBuilder();
      config = Configuration.builder().forceExtentedProfile().setCodeBlockEmitter(
          new BlockEmitter() {
            @Override
            public void emitBlock(StringBuilder out, List<String> lines, String meta) {
              if (!AbstractProcessor.isTest(meta)) {
                return;
              }
              for (String rawLine : lines) {
                if (indent) {
                  code.append("      ");
                }
                code.append(rawLine).append('\n');
              }
            }
          }).build();
    }

    private void extract(String documentation) {
      Processor.process(documentation, config);
    }

    List<Object> getSuites() {
      return suites;
    }

    @Override
    public void process(Map<String, ASTCompilationUnit> units, Path targetFolder) throws Throwable {
      EvaluationEnvironment evaluation = new EvaluationEnvironment();
      for (ASTCompilationUnit unit : units.values()) {
        Class<?> module = (Class<?>) evaluation.anonymousModule(render(unit));
        @SuppressWarnings("unchecked")
        List<Object> moduleSuites = (List<Object>) module.getMethod(FUNCTION_NAME).invoke(null);
        suites.addAll(moduleSuites);
      }
    }

    private void extractTest(FunctionDocumentation funDoc, boolean first) {
      int idx = code.length();
      extract(funDoc.documentation());
      if (code.length() != idx) {
        code.insert(idx, String.format("    %s[\"test for %s\", {\n", first ? "" : ",", funDoc.name()));
        code.append("    }]\n");
      }
    }

    private void extractSuite(ModuleDocumentation doc) {
      boolean first = true;
      int idx = code.length();
      for (FunctionDocumentation funDoc : doc.functions()) {
        extractTest(funDoc, first);
        first = false;
      }
      if (code.length() != idx) {
        code.insert(idx, String.format("\n  [\"Testing documentation of module %s\", list[\n", doc.moduleName()));
        code.append("  ]]\n");
      }
    }

    @Override
    public String render(ASTCompilationUnit compilationUnit) throws Throwable {
      ModuleDocumentation doc = new ModuleDocumentation(compilationUnit);
      createBuilder();
      code.append("import ").append(doc.moduleName()).append('\n');
      extract(doc.documentation());
      indent = true;
      code.append("\nfunction ").append(FUNCTION_NAME).append(" = -> list[");
      extractSuite(doc);
      code.append("]\n");
      // TODO: if in debug mode, prints the generated module.
      // System.out.println(code.toString());
      return code.toString();
    }
  }
}
