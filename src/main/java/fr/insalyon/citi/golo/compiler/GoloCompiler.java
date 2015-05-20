/*
 * Copyright 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.insalyon.citi.golo.compiler;

import fr.insalyon.citi.golo.compiler.ir.GoloModule;
import fr.insalyon.citi.golo.compiler.parser.ASTCompilationUnit;
import fr.insalyon.citi.golo.compiler.parser.GoloOffsetParser;
import fr.insalyon.citi.golo.compiler.parser.GoloParser;
import fr.insalyon.citi.golo.compiler.parser.ParseException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * The Golo compiler.
 * <p>
 * Instances of this class may be reused to compile several sources.
 * <p>
 * Several methods are made public while they do not necessarily need so for the needs of the Golo compiler.
 * Such deviations from a "good and clean" design are on-purpose, as this facilitates the implementation of
 * Golo support in IDEs.
 */
public class GoloCompiler {

  private GoloParser parser;
  private GoloCompilationException.Builder exceptionBuilder = null;


  /**
   * Initializes an ExceptionBuilder to collect errors instead of throwing immediately.
   * This method is made public for the requirements of IDEs support.
   *
   * @param builder the exception builder to add problems into.
   */
  public final void setExceptionBuilder(GoloCompilationException.Builder builder) {
    exceptionBuilder = builder;
  }

  private GoloCompilationException.Builder getOrCreateExceptionBuilder(String goloSourceFile) {
    if (exceptionBuilder == null) {
      exceptionBuilder = new GoloCompilationException.Builder(goloSourceFile);
    }
    return exceptionBuilder;
  }

  private void resetExceptionBuilder() {
    exceptionBuilder = null;
  }
  /**
   * Initializes a parser from an input stream. This method is made public for the requirements of IDEs support.
   *
   * @param sourceCodeInputStream the source code input stream.
   * @return the parser.
   */
  public final GoloParser initParser(String goloSourceFilename, InputStream sourceCodeInputStream) throws GoloCompilationException {
    try {
      return initParser(new InputStreamReader(sourceCodeInputStream, Charset.forName("UTF-8")));
    } catch (UnsupportedCharsetException e) {
      getOrCreateExceptionBuilder(goloSourceFilename).report(e).doThrow();
      return null;
    }
  }

  /**
   * Initializes a parser from a reader. This method is made public for the requirements of IDEs support.
   *
   * @param sourceReader the source code reader.
   * @return the parser.
   */
  public final GoloParser initParser(Reader sourceReader) {
    if (parser == null) {
      parser = createGoloParser(sourceReader);
    } else {
      parser.ReInit(sourceReader);
    }
    return parser;
  }

  /**
   * Build a final IR from a Golo source file.
   * <p>
   * The resulting module is expanded for macros and checked for soundness.
   * This is just a facility method that call in sequence and check for exceptions:
   * <ol>
   * <li>{@link #parse(String,GoloParser)} 
   * <li>{@link #transform(ASTCompilationUnit)}
   * <li>{@link #expand(GoloModule)}
   * <li>{@link #refine(GoloModule)}
   * </ol>
   *
   * @param goloSourceFilename    the source file name.
   * @param sourceCodeInputStream the source code input stream.
   * @return a fully built {@link fr.insalyon.citi.golo.compiler.ir.GoloModule} ready for
   * compilation
   * @throws GoloCompilationException if a problem occurs during any phase of the compilation work.
   */
  public final GoloModule buildModule(String goloSourceFilename, InputStream sourceCodeInputStream) throws GoloCompilationException {
    resetExceptionBuilder();
    ASTCompilationUnit compilationUnit = parse(goloSourceFilename, initParser(goloSourceFilename, sourceCodeInputStream));
    throwIfErrorEncountered();
    GoloModule goloModule = transform(compilationUnit);
    throwIfErrorEncountered();
    expand(goloModule);
    throwIfErrorEncountered();
    refine(goloModule);
    throwIfErrorEncountered();
    return goloModule;
  }

  /**
   * Transforms a compilation unit into a raw Golo module.
   * <p>
   * This is the 2nd compilation step, the next one is {@link #expand(GoloModule)}.
   *
   * @param compilationUnit  the compilation unit to transform, as returned by {@link #parse(String,GoloParser)}.
   * @return a raw Golo module.
   */
  public GoloModule transform(ASTCompilationUnit compilationUnit) {
    ParseTreeToGoloIrVisitor parseTreeToIR = new ParseTreeToGoloIrVisitor();
    parseTreeToIR.setExceptionBuilder(exceptionBuilder);
    return parseTreeToIR.transform(compilationUnit);
  }

  /**
   * Expand quoted block, macro calls and some IR transformations.
   * <p>
   * This is the 3rd compilation step, continue to {@link #refine(GoloModule)}.
   *
   * @param goloModule  the raw module to expand
   * @return the modified Golo module (also changed in place)
   */
  public GoloModule expand(GoloModule goloModule) {
     goloModule.accept(new QuotedIrExpander());
     goloModule.accept(new MacroExpansionIrVisitor(true));
     return goloModule;
  }

  /**
   * Finalize the IR by checking soundness.
   * <p>
   * This is the 4th compilation step.
   *
   * @param goloModule  the expanded Golo module to finalize
   * @return the modified module (also changed in place)
   */
  public GoloModule refine(GoloModule goloModule) {
    goloModule.accept(new ClosureCaptureGoloIrVisitor());
    goloModule.accept(new LocalReferenceAssignmentAndVerificationVisitor(exceptionBuilder));
    return goloModule;
  }

  /**
   * Compiles a Golo source file from an input stream, and returns a collection of results.
   *
   * @param goloSourceFilename    the source file name.
   * @param sourceCodeInputStream the source code input stream.
   * @return a list of compilation results.
   * @throws GoloCompilationException if a problem occurs during any phase of the compilation work.
   */
  public final List<CodeGenerationResult> compile(String goloSourceFilename, InputStream sourceCodeInputStream) throws GoloCompilationException {
    GoloModule goloModule = buildModule(goloSourceFilename, sourceCodeInputStream);
    JavaBytecodeGenerationGoloIrVisitor bytecodeGenerator = new JavaBytecodeGenerationGoloIrVisitor();
    List<CodeGenerationResult> result = new LinkedList<>();
    //bytecodeGenerator.setOnlyMacros(true);
    //result.addAll(bytecodeGenerator.generateBytecode(goloModule, goloSourceFilename));
    bytecodeGenerator.setOnlyMacros(false);
    result.addAll(bytecodeGenerator.generateBytecode(goloModule, goloSourceFilename));
    return result;
  }

  /**
   * Compiles Macros from a Golo source file from an input stream, and returns a collection of results.
   *
   * @param goloSourceFilename    the source file name.
   * @param sourceCodeInputStream the source code input stream.
   * @return a list of compilation results.
   * @throws GoloCompilationException if a problem occurs during any phase of the compilation work.
   */
  public final List<CodeGenerationResult> compileMacros(String goloSourceFilename, InputStream sourceCodeInputStream) throws GoloCompilationException {
    GoloModule goloModule = buildModule(goloSourceFilename, sourceCodeInputStream);
    JavaBytecodeGenerationGoloIrVisitor bytecodeGenerator = new JavaBytecodeGenerationGoloIrVisitor();
    bytecodeGenerator.setOnlyMacros(true);
    return bytecodeGenerator.generateBytecode(goloModule, goloSourceFilename);
  }


  private void throwIfErrorEncountered() {
    if (!getProblems().isEmpty()) {
      exceptionBuilder.doThrow();
    }
  }

  /**
   * Returns the list of problems encountered during the last compilation
   *
   * @return a list of compilation problems.
   */
  public List<GoloCompilationException.Problem> getProblems() {
    if (exceptionBuilder == null) {
      return Collections.emptyList();
    }
    return exceptionBuilder.getProblems();
  }

  /**
   * Compiles a Golo source file and writes the resulting JVM bytecode <code>.class</code> files in a target
   * folder. The class files are written in a directory structure that respects package names.
   *
   * @param goloSourceFilename    the source file name.
   * @param sourceCodeInputStream the source code input stream.
   * @param targetFolder          the output target folder.
   * @throws GoloCompilationException if a problem occurs during any phase of the compilation work.
   * @throws IOException              if writing the <code>.class</code> files fails for some reason.
   */
  public final void compileTo(String goloSourceFilename, InputStream sourceCodeInputStream, File targetFolder) throws GoloCompilationException, IOException {
    if (targetFolder.isFile()) {
      throw new IllegalArgumentException(targetFolder + " already exists and is a file.");
    }
    List<CodeGenerationResult> results = new LinkedList<>();
    //results.addAll(compileMacros(goloSourceFilename, sourceCodeInputStream));
    results.addAll(compile(goloSourceFilename, sourceCodeInputStream));
    for (CodeGenerationResult result : results) {
      File outputFolder = new File(targetFolder, result.getPackageAndClass().packageName().replaceAll("\\.", "/"));
      if (!outputFolder.exists() && !outputFolder.mkdirs()) {
        throw new IOException("mkdir() failed on " + outputFolder);
      }
      File outputFile = new File(outputFolder, result.getPackageAndClass().className() + ".class");
      try (FileOutputStream out = new FileOutputStream(outputFile)) {
        out.write(result.getBytecode());
      }
    }
  }

  /**
   * Produces a parse tree for a Golo source file. This is mostly useful to IDEs.
   * <p>
   * This is the first compilation step, continue with {@link #transform(ASTCompilationUnit)}.
   *
   * @param goloSourceFilename the source file name.
   * @param parser             the parser to use.
   * @return the resulting parse tree.
   * @throws GoloCompilationException if the parser encounters an error.
   */
  public final ASTCompilationUnit parse(String goloSourceFilename, GoloParser parser) throws GoloCompilationException {
    ASTCompilationUnit compilationUnit = null;
    List<ParseException> errors = new LinkedList<>();
    parser.exceptionBuilder = getOrCreateExceptionBuilder(goloSourceFilename);
    try {
      compilationUnit = parser.CompilationUnit();
    } catch (ParseException pe) {
      exceptionBuilder.report(pe, compilationUnit);
    }
    return compilationUnit;
  }

  /**
   * Checks that the source code is minimally sound by converting a parse tree to an intermediate representation, and
   * running a few classic visitors over it. This is mostly useful to IDEs.
   *
   * @param compilationUnit the source parse tree.
   * @return the intermediate representation of the source.
   * @throws GoloCompilationException if an error exists in the source represented by the input parse tree.
   */
  public final GoloModule check(ASTCompilationUnit compilationUnit) {
    return refine(expand(transform(compilationUnit)));
  }

  /**
   * Makes a Golo parser from a reader.
   *
   * @param sourceReader the reader.
   * @return the parser for <code>sourceReader</code>.
   */
  protected GoloParser createGoloParser(Reader sourceReader) {
    return new GoloOffsetParser(sourceReader);
  }
}
