/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.doc;

import org.eclipse.golo.compiler.parser.ASTCompilationUnit;
import gololang.FunctionReference;
import gololang.Predefined;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;

import com.github.rjeschke.txtmark.BlockEmitter;
import com.github.rjeschke.txtmark.Configuration;
import com.github.rjeschke.txtmark.Processor;


public class HtmlProcessor extends AbstractProcessor {

  private String moduleName;
  private Path targetFolder;
  private Path srcFile;

  // private static final String WEBCONSOLE_URI = "http://localhost:8080/?code=";
  private static final String WEBCONSOLE_URI = "http://golo-console.appspot.com/?code=";

  @Override
  public String render(ASTCompilationUnit compilationUnit) throws Throwable {
    FunctionReference template = template("template", "html");
    ModuleDocumentation documentation = new ModuleDocumentation(compilationUnit);
    return (String) template.invoke(documentation, srcFile);
  }

  private String renderSource(String filename) throws Throwable {
    FunctionReference template = template("src", "html");
    String content = (String) Predefined.fileToText(filename, "UTF-8");
    int nbLines = 0;
    for (int i = 0; i < content.length(); i++) {
      if (content.charAt(i) == '\n') {
        nbLines++;
      }
    }
    return (String) template.invoke(moduleName, content, nbLines);
  }

  private Path createFile(String ext, String content) throws Throwable {
    Path outFile = outputFile(this.targetFolder, this.moduleName, "." + ext);
    ensureFolderExists(outFile.getParent());
    Predefined.textToFile(content, outFile);
    return outFile;
  }

  @Override
  public void process(Map<String, ASTCompilationUnit> units, Path targetFolder) throws Throwable {
    this.targetFolder = targetFolder;
    TreeMap<String, String> moduleDocFile = new TreeMap<>();
    ensureFolderExists(targetFolder);
    for (Map.Entry<String, ASTCompilationUnit> unit : units.entrySet()) {
      this.moduleName = moduleName(unit.getValue());
      srcFile = createFile("src.html", renderSource(unit.getKey()));
      Path docFile = createFile("html", render(unit.getValue()));
      moduleDocFile.put(moduleName, targetFolder.relativize(docFile).toString());
    }
    FunctionReference indexTemplate = template("index", "html");
    String index = (String) indexTemplate.invoke(moduleDocFile);
    Predefined.textToFile(index, targetFolder.resolve("index.html"));
  }

  private static String encodeLine(String line) {
    try {
      return URLEncoder.encode(line + "\n", "UTF-8").replace("+", "%20");
    } catch (UnsupportedEncodingException e) {
      return "";
    }
  }

  public static BlockEmitter blockHighlighter(String moduleName) {
    return new BlockEmitter() {
      @Override
      public void emitBlock(StringBuilder out, List<String> lines, String meta) {
        String language;
        if ("".equals(meta)) {
          language = "golo";
        } else {
          language = meta;
        }
        StringBuilder encodedCode = new StringBuilder(WEBCONSOLE_URI);
        encodedCode.append(encodeLine("module Sample\n"));
        encodedCode.append(encodeLine("import " + moduleName + "\n"));
        encodedCode.append(encodeLine("function run = |context| {"));

        out.append("<figure class=\"code-sample\"><pre class=\"listing highlight highlightjs\">");
        out.append(String.format("<code class=\"language-%s\" data-lang=\"%s\">", language, language));
        for (String rawLine : lines) {
          encodedCode.append(encodeLine("  " + rawLine.replace("println(", "context: log(")));
          String line = rawLine
            .replace("&", "&amp;")
            .replace(">", "&gt;")
            .replace("<", "&lt;");
          out.append(line);
          out.append('\n');
        }
        encodedCode.append(encodeLine("}"));
        out.append("</code></pre>");
        if ("golo".equals(language)) {
          out.append("<a target=\"_blank\" class=\"code-runner\" href=\"");
          out.append(encodedCode);
          out.append("\">Run</a>");
        }
        out.append("</figure>\n");
      }
    };
  }

  public static String process(String documentation, int rootLevel, Configuration configuration) {
    return Processor.process(AbstractProcessor.adaptSections(documentation, rootLevel), configuration);
  }

}
