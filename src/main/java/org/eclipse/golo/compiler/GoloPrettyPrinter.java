/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler;

import static org.eclipse.golo.compiler.utils.StringUnescaping.escape;
import gololang.ir.*;

import java.util.List;
import static java.util.Arrays.asList;

/* TODO:
 * - match pretty
 * - stack for binary operators to correctly parenthesize them
 * - struct constructor functions
 * - decorators not inlined
 * - keep comments
 */
public class GoloPrettyPrinter implements GoloIrVisitor {

  // configuration: should be customizable
  private static final int COLLECTION_WRAPPING_THRESHOLD = 5;
  private static final int INDENT = 2;

  private int spacing = 0;
  private boolean onlyReturn = false;
  private boolean inFunctionRoot = false;
  private final StringBuilder header = new StringBuilder();
  private final StringBuilder body = new StringBuilder();
  private StringBuilder buffer;

  private boolean expanded = false;

  public GoloPrettyPrinter(boolean expanded) {
    super();
    this.expanded = expanded;
    this.buffer = this.body;
  }

  private void print(Object o) {
    this.buffer.append(o);
  }

  private void newline() {
    this.buffer.append('\n');
  }

  private boolean endsWithNewline(int offset) {
    return (this.buffer.length() > offset
            && this.buffer.charAt(this.buffer.length() - (offset + 1)) != '\n');
  }

  private void newlineIfNeeded() {
    if (endsWithNewline(0)) {
      newline();
    }
  }

  private void blankLine() {
    newlineIfNeeded();
    if (endsWithNewline(1)) {
      newline();
    }
  }

  private void println(Object s) {
    print(s);
    newline();
  }

  private void printf(String format, Object... values) {
    print(String.format(format, values));
  }

  private void space() {
    for (int i = 0; i < spacing; i++) {
      print(' ');
    }
  }

  private void incr() {
    spacing = spacing + INDENT;
  }

  private void decr() {
    spacing = Math.max(0, spacing - INDENT);
  }

  private void beginBlock(String delim) {
    println(delim);
    incr();
  }

  private void endBlock(String delim) {
    newlineIfNeeded();
    decr();
    space();
    print(delim);
  }

  private void join(Iterable<? extends Object> elements, String separator) {
    boolean first = true;
    for (Object elt : elements) {
      if (!first) {
        print(separator);
        if (separator.endsWith("\n")) { space(); }
      } else {
        first = false;
      }
      print(elt);
    }
  }

  private void joinedVisit(Iterable<? extends GoloElement<?>> elements, String separator) {
    boolean first = true;
    for (GoloElement<?> element : elements) {
      if (first) {
        first = false;
      } else {
        print(separator);
        if (separator.endsWith("\n")) {
          space();
        }
      }
      element.accept(this);
    }
  }

  private void printDocumentation(GoloElement<?> element) {
    if (element.documentation() != null) {
      space();
      print("----");
      for (String line : asList(element.documentation().split("\n"))) {
        space();
        println(line.trim());
      }
      space();
      println("----");
    }
  }

  private void initBuffers() {
    this.body.delete(0, this.body.length());
    this.header.delete(0, this.header.length());
  }

  public StringBuilder getBuffer() {
    return this.buffer;
  }

  @Override
  public void visitModule(GoloModule module) {
    initBuffers();
    this.buffer = this.body;
    printDocumentation(module);
    println("module " + module.getPackageAndClass());
    blankLine();
    module.walk(this);
    System.out.println(this.buffer);
  }

  @Override
  public void visitAugmentation(Augmentation augment) {
    if (augment.hasNames()) {
      blankLine();
      printf("augment %s with ", augment.getTarget());
      join(augment.getNames(), ", ");
      newline();
    }
    if (augment.hasFunctions()) {
      blankLine();
      printf("augment %s ", augment.getTarget());
      beginBlock("{");
      for (GoloFunction func : augment.getFunctions()) {
        func.accept(this);
      }
      endBlock("}");
    }
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation augmentation) {
    if (augmentation.hasFunctions()) {
      blankLine();
      printf("augmentation %s = ", augmentation.getName());
      beginBlock("{");
      for (GoloFunction func : augmentation.getFunctions()) {
        func.accept(this);
      }
      endBlock("}");
    }
  }

  @Override
  public void visitModuleImport(ModuleImport moduleImport) {
    if (expanded || !moduleImport.isImplicit()) {
      println("import " + moduleImport.getPackageAndClass().toString());
    }
  }

  @Override
  public void visitStruct(Struct struct) {
    blankLine();
    print("struct " + struct.getPackageAndClass().className() + " = ");
    if (struct.getMembers().size() > COLLECTION_WRAPPING_THRESHOLD) {
      beginBlock("{");
      join(struct.getMembers(), ",\n");
      endBlock("}");
    } else {
      print("{ ");
      join(struct.getMembers(), ", ");
      println(" }");
    }
  }

  @Override
  public void visitUnion(Union union) {
    blankLine();
    print("union " + union.getPackageAndClass().className() + " = ");
    beginBlock("{");
    union.walk(this);
    endBlock("}");
  }

  @Override
  public void visitUnionValue(UnionValue value) {
    newlineIfNeeded();
    space();
    print(value.getName());
    if (value.hasMembers()) {
      print(" = { ");
      join(value.getMembers(), ", ");
      println(" }");
    }
  }

  @Override
  public void visitFunction(GoloFunction function) {
    if (function.isModuleInit()) {
      printModuleInit(function);
    } else if (expanded || !function.isSynthetic()) {
      printFunctionDefinition(function);
    }
  }

  private void printFunctionParams(GoloFunction function) {
    int startArguments = 0;
    if (!expanded) {
      startArguments = function.getSyntheticParameterCount();
    }
    int realArity = function.getArity() - startArguments;
    if (realArity != 0) {
      print("|");
    }
    join(function.getParameterNames().subList(
        startArguments,
        function.getArity()), ", ");
    if (function.isVarargs()) {
      print("...");
    }
    if (realArity != 0) {
      print("| ");
    }
  }

  private void printFunctionExpression(GoloFunction function) {
    inFunctionRoot = true;
    printFunctionParams(function);
    function.getBlock().accept(this);
  }

  private void printModuleInit(GoloFunction function) {
    if (expanded) {
      printFunctionDefinition(function);
    } else {
      blankLine();
      joinedVisit(function.getBlock().getStatements(), "\n");
    }
  }

  private void printFunctionDefinition(GoloFunction function) {
    blankLine();
    printDocumentation(function);
    for (Decorator decorator : function.getDecorators()) {
      decorator.accept(this);
    }
    space();
    if (function.isLocal()) {
      print("local ");
    }
    print("function ");
    print(function.getName());
    print(" = ");
    printFunctionExpression(function);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    // TODO: not expanded version ?
    if (!expanded) {
      space();
      print("@");
      decorator.walk(this);
      newline();
      space();
    }
  }

  private void checkIsOnlyReturn(List<GoloStatement<?>> statements) {
    this.onlyReturn = (
        statements.size() == 1
        && statements.get(0) instanceof ReturnStatement
        && inFunctionRoot
        && !expanded);
  }

  private boolean isLoopBlock(List<GoloStatement<?>> statements) {
    return statements.size() == 1
        && (statements.get(0) instanceof LoopStatement
            || statements.get(0) instanceof ForEachLoopStatement);
  }

  @Override
  public void visitBlock(Block block) {
    List<GoloStatement<?>> statements = block.getStatements();
    checkIsOnlyReturn(statements);
    if (onlyReturn || isLoopBlock(statements)) {
      statements.get(0).accept(this);
    } else {
      if (expanded || inFunctionRoot) {
        beginBlock("{");
      }
      for (GoloStatement<?> s : block.getStatements()) {
        if (willPrintStatement(s)) {
          newlineIfNeeded();
          space();
        }
        s.accept(this);
      }
      if (expanded || inFunctionRoot) {
        endBlock("}");
      }
    }
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    Object value = constantStatement.value();
    if (value instanceof String) {
      print("\"" + escape((String) value) + "\"");
    } else if (value instanceof Character) {
      print("'" + value + "'");
    } else if (value instanceof Long) {
      print(value + "_L");
    } else if (value instanceof ClassReference) {
      print(((ClassReference) value).getName() + ".class");
    } else if (value instanceof FunctionRef) {
      print("^"
          + ((((FunctionRef) value).module() != null)
              ? (((FunctionRef) value).module() + "::")
              : "")
          + ((FunctionRef) value).name());
    } else {
      print(constantStatement.value());
    }
  }

  private boolean isNull(GoloStatement<?> statement) {
    return (
      statement instanceof ConstantStatement
      && ((ConstantStatement) statement).value() == null);
  }

  private boolean isReturnNull(GoloStatement<?> statement) {
    return (statement instanceof ReturnStatement)
      && isNull(((ReturnStatement) statement).expression());
  }

  private boolean willPrintStatement(GoloStatement<?> statement) {
    return expanded || !inFunctionRoot || !isReturnNull(statement);
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    if (willPrintStatement(returnStatement)) {
      if (onlyReturn) {
        print("-> ");
      } else {
        print("return ");
      }
      returnStatement.expression().accept(this);
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (!functionInvocation.isAnonymous()) {
      print(functionInvocation.getName());
    }
    printInvocationArguments(functionInvocation.getArguments());
  }

  private void printInvocationArguments(List<GoloElement<?>> arguments) {
    if (arguments.size() < COLLECTION_WRAPPING_THRESHOLD) {
      print("(");
      joinedVisit(arguments, ", ");
      print(")");
    } else {
      beginBlock("(");
      joinedVisit(arguments, ",\n");
      endBlock(")");
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    LocalReference ref = assignmentStatement.getLocalReference();
    switch (ref.getKind()) {
      case CONSTANT:
      case MODULE_CONSTANT:
        print("let ");
        break;
      case VARIABLE:
      case MODULE_VARIABLE:
        if (assignmentStatement.isDeclaring()) {
          print("var ");
        }
        break;
      default:
    }
    print(ref.getName() + " = ");
    assignmentStatement.expression().accept(this);
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    print(referenceLookup.getName());
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    boolean wasInFuncRoot = inFunctionRoot;
    inFunctionRoot = false;
    print("if ");
    conditionalBranching.getCondition().accept(this);
    print(" ");
    conditionalBranching.getTrueBlock().accept(this);
    if (conditionalBranching.hasFalseBlock()) {
      print(" else ");
      conditionalBranching.getFalseBlock().accept(this);
    } else if (conditionalBranching.hasElseConditionalBranching()) {
      print(" else ");
      conditionalBranching.getElseConditionalBranching().accept(this);
    }
    inFunctionRoot = wasInFuncRoot;
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    binaryOperation.left().accept(this);
    if (binaryOperation.getType() != OperatorType.METHOD_CALL
        && binaryOperation.getType() != OperatorType.ELVIS_METHOD_CALL) {
      print(" ");
    }
    print(binaryOperation.getType() + " ");
    binaryOperation.right().accept(this);
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    print(unaryOperation.getType());
    unaryOperation.expression().accept(this);
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement loop) {
    print("foreach ");
    joinedVisit(asList(loop.getReferences()), ", ");
    print(" in ");
    loop.getIterable().accept(this);
    loop.getBlock().accept(this);
  }

  private void visitForLoopStatement(LoopStatement loop) {
    print("for (");
    if (loop.hasInitStatement()) {
      loop.init().accept(this);
    }
    print(", ");
    loop.condition().accept(this);
    print(",");
    if (loop.hasPostStatement()) {
      print(" ");
      loop.post().accept(this);
    }
    print(") ");
    loop.getBlock().accept(this);
  }

  private void visitWhileLoopStatement(LoopStatement loop) {
    if (loop.hasInitStatement()) {
      loop.init().accept(this);
      newline();
      space();
    }
    print("while ");
    loop.condition().accept(this);
    print(" ");
    loop.getBlock().accept(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    boolean wasInFuncRoot = inFunctionRoot;
    inFunctionRoot = false;
    // TODO: refactor by adding a WhileLoopStatement / EachLoopStatement / ForLoopStatement
    if (loopStatement.hasPostStatement()) {
      visitForLoopStatement(loopStatement);
    } else {
      visitWhileLoopStatement(loopStatement);
    }
    inFunctionRoot = wasInFuncRoot;
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    print(methodInvocation.getName());
    printInvocationArguments(methodInvocation.getArguments());
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    println("throw ");
    throwStatement.expression().accept(this);
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    print("try ");
    tryCatchFinally.getTryBlock().accept(this);
    if (tryCatchFinally.hasCatchBlock()) {
      print(" catch (" + tryCatchFinally.getExceptionId() + ")");
      tryCatchFinally.getCatchBlock().accept(this);
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      print(" finally ");
      tryCatchFinally.getFinallyBlock().accept(this);
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    if (expanded) {
      print("^" + closureReference.getTarget().getName());
      if (closureReference.getTarget().getSyntheticParameterCount() > 0) {
        print(": insertArguments(0, ");
        join(closureReference.getTarget().getSyntheticParameterNames(), ", ");
        print(")");
      }
    } else {
      printFunctionExpression(closureReference.getTarget());
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    space();
    println(loopBreakFlowStatement.getType().name());
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    if (collectionLiteral.getType() == CollectionLiteral.Type.range) {
      print("[");
      joinedVisit(collectionLiteral.getExpressions(), "..");
      print("]");
    } else {
      if (collectionLiteral.getType() != CollectionLiteral.Type.tuple || expanded) {
        print(collectionLiteral.getType());
      }
      if (collectionLiteral.getExpressions().size() > COLLECTION_WRAPPING_THRESHOLD) {
        beginBlock("[");
        joinedVisit(collectionLiteral.getExpressions(), ",\n");
        endBlock("]");
      } else {
        print("[");
        joinedVisit(collectionLiteral.getExpressions(), ", ");
        print("]");
      }
    }
  }

  @Override
  public void visitLocalReference(LocalReference localRef) { }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) { }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collection) {
    space();
    print(collection.getType().toString());
    print("[");
    collection.walk(this);
    print("]");
    // TODO: pretty print collection comprehension
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    // TODO: pretty print when clause
  }

  @Override
  public void visitMatchExpression(MatchExpression expr) {
    // TODO: pretty print match expression
  }

  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    // TODO: pretty print case statement
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment destruct) {
    // TODO: pretty print destruct assignment
  }

  @Override
  public void visitToplevelElements(ToplevelElements elements) {
    elements.walk(this);
  }

  @Override
  public void visitNoop(Noop noop) {
    // TODO: pretty print noop
  }

  @Override
  public void visitMember(Member member) {
    // TODO: pretty print member
  }
}
