/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler;

import org.eclipse.golo.compiler.parser.GoloASTNode;
import org.eclipse.golo.compiler.parser.ParseException;
import org.eclipse.golo.compiler.ir.PositionInSourceCode;
import org.eclipse.golo.compiler.ir.GoloElement;

import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static java.util.Collections.unmodifiableList;
import static gololang.Messages.message;

/**
 * A Golo compilation exception that may also report a cause and several identified problems.
 */
public class GoloCompilationException extends RuntimeException {

  /**
   * A problem reported either while compiling the source code or processing the intermediate representation.
   */
  public static final class Problem {

    /**
     * The possible problem types.
     */
    public enum Type {
      PARSING,
      AUGMENT_FUNCTION_NO_ARGS,
      UNDECLARED_REFERENCE,
      ASSIGN_CONSTANT,
      BREAK_OR_CONTINUE_OUTSIDE_LOOP,
      REFERENCE_ALREADY_DECLARED_IN_BLOCK,
      UNINITIALIZED_REFERENCE_ACCESS,
      INVALID_ENCODING,
      INCOMPLETE_NAMED_ARGUMENTS_USAGE,
      AMBIGUOUS_DECLARATION
    }

    private final Type type;
    private final PositionInSourceCode position;
    private final String description;
    private final Object source;

    private Problem(Type type, PositionInSourceCode position, String description, Object source) {
      this.type = type;
      this.position = position;
      this.description = description;
      this.source = source;
    }

    /**
     * @return the problem type.
     */
    public Type getType() {
      return type;
    }

    /**
     * @return the problem description.
     */
    public String getDescription() {
      return description;
    }

    /**
     * @return the position in the source code.
     */
    public PositionInSourceCode getPositionInSourceCode() {
      return position;
    }

    /**
     * @return the source of the problem.
     */
    public Object getSource() {
      return source;
    }

    @Override
    public String toString() {
      return String.format("Problem{type=%s, description='%s', position=%s}", type, description, position);
    }
  }

  /**
   * An exception builder object allows preparing an exception by progressively adding problems.
   */
  public static class Builder {

    private final GoloCompilationException exception;

    /**
     * Makes a builder to report problems in a source file.
     *
     * @param goloSourceFilename the source file name.
     */
    public Builder(Path goloSourceFilename) {
      exception = new GoloCompilationException(message("in_module", goloSourceFilename.toString()));
      exception.setSourceCode(goloSourceFilename);
    }

    /**
     * Report a problem to the exception being built.
     *
     * @param type        the problem type.
     * @param source      the problem source.
     * @param description the problem description.
     * @return the same builder object.
     */
    public Builder report(Problem.Type type, GoloASTNode source, String description) {
      exception.report(new Problem(type,
            source != null ? source.getPositionInSourceCode() : null,
            description,
            source));
      return this;
    }

    /**
     * Report a problem to the exception being built.
     *
     * @param type        the problem type.
     * @param source      the problem source.
     * @param description the problem description.
     * @return the same builder object.
     */
    public Builder report(Problem.Type type, GoloElement<?> source, String description) {
      exception.report(new Problem(type,
            source != null ? source.positionInSourceCode() : null,
            description,
            source));
      return this;
    }

    /**
     * Report a parsing error problem to the exception being built.
     *
     * @param pe     the caught {@code ParseException}.
     * @param source the node of the {@code ParseException}.
     * @return the same builder object.
     */
    public Builder report(ParseException pe, GoloASTNode source) {
      exception.report(new Problem(Problem.Type.PARSING,
            PositionInSourceCode.of(pe.currentToken.beginLine, pe.currentToken.beginColumn, pe.currentToken.endLine, pe.currentToken.endColumn),
            pe.getMessage(),
            source));
      return this;
    }

    /**
     * Report an encoding error problem to the exception being built.
     *
     * @param uce     the caught {@code UnsupportedCharsetException}.
     * @return the same builder object.
     */
    public Builder report(UnsupportedCharsetException uce) {
      exception.report(new Problem(Problem.Type.INVALID_ENCODING, null, uce.getMessage(), null));
      return this;
    }

    /**
     * Stops adding problems and throws the exception,
     *
     * @throws GoloCompilationException everytime.
     */
    public void doThrow() throws GoloCompilationException {
      throw exception;
    }

    public List<Problem> getProblems() {
      return exception.getProblems();
    }
  }

  private final List<Problem> problems = new LinkedList<>();

  private Path sourceCode;

  /**
   * @return all reported problems.
   */
  public List<Problem> getProblems() {
    return unmodifiableList(problems);
  }

  private void report(Problem problem) {
    problems.add(problem);
  }

  private GoloCompilationException() {
    super();
  }

  /**
   * Gives the problematic source code, if specified.
   *
   * @return the source code, or {@code null} if none has been specified.
   */
  public Path getSourceCode() {
    return sourceCode;
  }

  /**
   * Specifies the problematic source code.
   *
   * @param sourceCode the raw source code.
   */
  public void setSourceCode(Path sourceCode) {
    this.sourceCode = sourceCode;
  }

  /**
   * Makes a new compiler exception with a message.
   *
   * @param message the message.
   */
  public GoloCompilationException(String message) {
    super(message);
  }

  /**
   * Makes a new compiler exception from a root cause.
   *
   * @param throwable the cause.
   */
  public GoloCompilationException(Throwable throwable) {
    super(throwable);
  }

  /**
   * Makes a new exception from a message and a root cause.
   *
   * @param message the message.
   * @param cause   the cause.
   */
  public GoloCompilationException(String message, Throwable cause) {
    super(message, cause);
  }
}
