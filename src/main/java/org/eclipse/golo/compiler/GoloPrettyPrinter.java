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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Set;
import java.util.function.Function;
import static java.util.Arrays.asList;

/* TODO:
 * - extract a CodePrinter class
 * - 1 line if 1 statement in block
 * - decorators
 * - keep comments
 * - line wrapping!
 */
public class GoloPrettyPrinter implements GoloIrVisitor {

  // configuration: should be customizable
  private static final int COLLECTION_WRAPPING_THRESHOLD = 5;
  private static final int INDENT = 2;

  private static final List<String> RESERVED = asList(
    "module", "import", "function", "local", "return", "if", "else", "while", "for", "foreach",
    "in", "throw", "try", "catch", "finally", "case", "when", "match", "then", "otherwise",
    "augment", "pimp", "augmentation", "with", "break", "continue", "struct", "union", "oftype",
    "is", "isnt", "and", "or", "orIfNull", "not", "var", "let", "null", "true", "false");

  private int spacing = 0;
  private Deque<Boolean> compactReturn = new LinkedList<>();
  private StringBuilder buffer = new StringBuilder();
  private boolean expanded = false;

  private GoloModule module;

  public GoloPrettyPrinter(boolean expanded) {
    this.expanded = expanded;
  }

  private void print(Object o) {
    this.buffer.append(o);
  }

  private void newline() {
    this.buffer.append('\n');
  }

  private char lastChar(int offset) {
    return this.buffer.charAt(this.buffer.length() - (offset + 1));
  }

  private boolean endsWith(int offset, char c) {
    return (this.buffer.length() > offset
            && lastChar(offset) == c);
  }

  private void newlineIfNeeded() {
    if (!endsWith(0, '\n')) {
      newline();
    }
  }

  private void blankLine() {
    newlineIfNeeded();
    if (!endsWith(1, '\n')) {
      newline();
    }
  }

  private void space() {
    char last = lastChar(0);
    switch (last) {
      case '\n':
        indent();
        break;
      case ' ':
      case '[':
      case '(':
      case '|':
        break;
      default:
        print(' ');
    }
  }

  private void println(Object s) {
    print(s);
    newline();
  }

  private void printf(String format, Object... values) {
    print(String.format(format, values));
  }

  private void indent() {
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
    indent();
    print(delim);
  }

  private void join(Iterable<? extends Object> elements, String separator) {
    boolean first = true;
    for (Object elt : elements) {
      if (first) {
        first = false;
      } else {
        print(separator);
      }
      space();
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
      }
      space();
      element.accept(this);
    }
  }

  private static String quoteReserved(String name) {
    if (RESERVED.contains(name)) {
      return '`' + name;
    }
    return name;
  }

  private void printDocumentation(GoloElement<?> element) {
    if (element.documentation() != null) {
      blankLine();
      space();
      println("----");
      for (String line : asList(element.documentation().trim().split("\r\n|\r|\n"))) {
        space();
        println(line.trim());
      }
      space();
      println("----");
    }
  }

  @Override
  public void visitModule(GoloModule module) {
    this.module = module;
    printDocumentation(module);
    println("module " + module.getPackageAndClass());
    blankLine();
    module.walk(this);
    System.out.println(this.buffer);
  }

  @Override
  public void visitAugmentation(Augmentation augment) {
    String target = augment.getTarget().toString();
    if (!expanded && (module.getPackageAndClass().toString() + ".types").equals(augment.getTarget().packageName())) {
      target = augment.getTarget().className();
    }
    blankLine();
    printDocumentation(augment);
    if (augment.hasNames()) {
      printf("augment %s with ", target);
      join(augment.getNames(), ", ");
      newline();
    }
    if (augment.hasFunctions()) {
      printf("augment %s ", target);
      beginBlock("{");
      for (GoloFunction func : augment.getFunctions()) {
        func.accept(this);
      }
      endBlock("}");
    }
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation augmentation) {
    blankLine();
    printDocumentation(augmentation);
    if (augmentation.hasFunctions()) {
      printf("augmentation %s = ", quoteReserved(augmentation.getName()));
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
    indent();
    print(quoteReserved(value.getName()));
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
    List<Decorator> decorators = new ArrayList<>(function.getDecorators());
    Collections.reverse(decorators);
    for (Decorator decorator : decorators) {
      decorator.accept(this);
    }
    indent();
    if (function.isLocal()) {
      print("local ");
    }
    print("function ");
    print(quoteReserved(function.getName()));
    print(" = ");
    printFunctionExpression(function);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    if (!expanded) {
      indent();
      print('@');
      if (decorator.isConstant()) {
        print('!');
      }
      decorator.walk(this);
      newline();
      indent();
    }
  }

  private void checkForCompactReturn(Block block) {
    this.compactReturn.push(
        block.hasOnlyReturn()
        && block.parent() instanceof GoloFunction
        && !expanded);
  }

  private boolean compactReturn() {
    return compactReturn.peek();
  }

  private boolean isLoopBlock(List<GoloStatement<?>> statements) {
    return statements.size() == 1
        && (statements.get(0) instanceof LoopStatement
            || statements.get(0) instanceof ForEachLoopStatement);
  }

  @Override
  public void visitBlock(Block block) {
    List<GoloStatement<?>> statements = block.getStatements();
    checkForCompactReturn(block);
    space();
    if (compactReturn() || isLoopBlock(statements)) {
      statements.get(0).accept(this);
    } else if (isCompact(block)) {
      print("{ ");
      if (!block.isEmpty()) {
        statements.get(0).accept(this);
      }
      space();
      print("}");
    } else {
      beginBlock("{");
      for (GoloStatement<?> s : block.getStatements()) {
        if (willPrint(s)) {
          newlineIfNeeded();
          space();
        }
        s.accept(this);
      }
      endBlock("}");
    }
    compactReturn.pop();
  }

  private boolean isCompact(Block block) {
    return block.size() < 2
      || (block.size() == 2 && !willPrint(block.getStatements().get(1)));
  }

  private boolean willPrint(GoloStatement<?> statement) {
    return expanded
      || !(statement instanceof ReturnStatement)
      || !((ReturnStatement) statement).isSynthetic();
  }

  private boolean isLongString(String value) {
    return !expanded
          && value.split("\r\n|\r|\n|\"|\t").length >= 3;
  }

  private void printString(String value) {
    if (isLongString(value)) {
      print("\"\"\"" + value + "\"\"\"");
    } else {
      print("\"" + escape(value) + "\"");
    }
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    Object value = constantStatement.value();
    if (value instanceof String) {
      printString((String) value);
    } else if (value instanceof Character) {
      print("'" + escape(value.toString()) + "'");
    } else if (value instanceof Long) {
      print(value + "_L");
    } else if (value instanceof Float) {
      print(value + "_F");
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

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    if (compactReturn()) {
      print("-> ");
      returnStatement.walk(this);
    } else {
      if (!returnStatement.isSynthetic() || expanded) {
        print("return");
        if (!returnStatement.isReturningVoid()) {
          space();
          returnStatement.walk(this);
        }
      }
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (!functionInvocation.isAnonymous()) {
      print(quoteReserved(functionInvocation.getName()));
    }
    if (functionInvocation.isConstant()) {
      print('!');
    }
    printInvocationArguments(functionInvocation.getArguments());
  }

  private void printInvocationArguments(List<GoloElement<?>> arguments) {
    if (arguments.size() < COLLECTION_WRAPPING_THRESHOLD) {
      print("(");
      joinedVisit(arguments, ",");
      print(")");
    } else {
      beginBlock("(");
      joinedVisit(arguments, ",\n");
      endBlock(")");
    }
  }

  private void printAssignementKind(LocalReference.Kind kind, boolean declaring) {
    switch (kind) {
      case CONSTANT:
      case MODULE_CONSTANT:
        print("let ");
        break;
      case VARIABLE:
      case MODULE_VARIABLE:
        if (declaring) {
          print("var ");
        }
        break;
      default:
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    LocalReference ref = assignmentStatement.getLocalReference();
    printAssignementKind(ref.getKind(), assignmentStatement.isDeclaring());
    print(quoteReserved(ref.getName()) + " = ");
    assignmentStatement.expression().accept(this);
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    print(quoteReserved(referenceLookup.getName()));
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    print("if ");
    conditionalBranching.getCondition().accept(this);
    space();
    conditionalBranching.getTrueBlock().accept(this);
    if (conditionalBranching.hasFalseBlock()) {
      print(" else ");
      conditionalBranching.getFalseBlock().accept(this);
    } else if (conditionalBranching.hasElseConditionalBranching()) {
      print(" else ");
      conditionalBranching.getElseConditionalBranching().accept(this);
    }
  }

  private boolean hasBrackets(BinaryOperation op, ExpressionStatement<?> side) {
    if (side instanceof ClosureReference) { return true; }
    if (side instanceof BinaryOperation) {
      BinaryOperation sideOp = (BinaryOperation) side;
      if (sideOp.isMethodCall()) { return false; }
      if (sideOp.getType() == op.getType()) { return false; }
      return true;
    }
    return false;
  }

  private void printOpSide(BinaryOperation op, Function<BinaryOperation, ExpressionStatement<?>> side) {
    ExpressionStatement<?> sideExpr = side.apply(op);
    boolean brackets = hasBrackets(op, sideExpr);
    if (brackets) { print("("); }
    sideExpr.accept(this);
    if (brackets) { print(")"); }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    printOpSide(binaryOperation, BinaryOperation::left);
    if (!binaryOperation.isMethodCall()) { space(); }
    print(binaryOperation.getType());
    if (!binaryOperation.isMethodCall()) { space(); }
    printOpSide(binaryOperation, BinaryOperation::right);
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    print(unaryOperation.getType());
    space();
    ExpressionStatement<?> expr = unaryOperation.expression();
    boolean brackets = expr instanceof BinaryOperation && !((BinaryOperation) expr).isMethodCall();
    if (brackets) { print("("); }
    expr.accept(this);
    if (brackets) { print(")"); }
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement loop) {
    space();
    print("foreach ");
    joinedVisit(asList(loop.getReferences()), ", ");
    if (loop.isVarargs()) {
      print("...");
    }
    print(" in ");
    loop.getIterable().accept(this);
    if (loop.hasWhenClause()) {
      space();
      print("when ");
      loop.getWhenClause().accept(this);
    }
    if (!loop.getBlock().isEmpty()) {
      loop.getBlock().accept(this);
    }
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
    print(")");
    if (!loop.getBlock().isEmpty()) {
      loop.getBlock().accept(this);
    }
  }

  private void visitWhileLoopStatement(LoopStatement loop) {
    if (loop.hasInitStatement()) {
      loop.init().accept(this);
      newline();
      indent();
    }
    print("while ");
    loop.condition().accept(this);
    print(" ");
    loop.getBlock().accept(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    if (loopStatement.hasPostStatement()) {
      visitForLoopStatement(loopStatement);
    } else {
      visitWhileLoopStatement(loopStatement);
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    space();
    print(quoteReserved(methodInvocation.getName()));
    printInvocationArguments(methodInvocation.getArguments());
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    print("throw ");
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
    println(loopBreakFlowStatement.getType().name().toLowerCase());
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
        indent();
        joinedVisit(collectionLiteral.getExpressions(), ",\n");
        endBlock("]");
      } else {
        print("[");
        joinedVisit(collectionLiteral.getExpressions(), ",");
        print("]");
      }
    }
  }

  @Override
  public void visitLocalReference(LocalReference localRef) {
    if (!localRef.isModuleState()) {
      space();
      print(quoteReserved(localRef.getName()));
    }
  }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) {
    print(namedArgument.getName() + '=');
    namedArgument.walk(this);
  }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collection) {
    print(collection.getType().toString());
    print("[");
    collection.walk(this);
    print("]");
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    space();
    print("when ");
    whenClause.condition().accept(this);
    Object action = whenClause.action();
    if (action instanceof Block) {
      Block block = (Block) action;
      if (!block.isEmpty()) {
        block.accept(this);
      }
    } else if (action instanceof ExpressionStatement) {
      ExpressionStatement<?> expr = (ExpressionStatement) action;
      space();
      print("then ");
      expr.accept(this);
    }
  }

  @Override
  public void visitMatchExpression(MatchExpression expr) {
    space();
    print("match ");
    visitAlternatives(expr);
  }

  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    space();
    print("case ");
    visitAlternatives(caseStatement);
  }

  private void visitAlternatives(Alternatives<?> alt) {
    beginBlock("{");
    joinedVisit(alt.getClauses(), "\n");
    newline();
    space();
    print("otherwise ");
    alt.getOtherwise().accept(this);
    endBlock("}");
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment destruct) {
    space();
    printAssignementKind(destruct.getReferences()[0].getKind(), destruct.isDeclaring());
    joinedVisit(asList(destruct.getReferences()), ",");
    if (destruct.isVarargs()) {
      print("...");
    }
    space();
    print("= ");
    destruct.expression().accept(this);
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
