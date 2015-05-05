/*
 * Copyright 2012-2014 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
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

import static fr.insalyon.citi.golo.compiler.utils.StringUnescaping.escape;
import fr.insalyon.citi.golo.compiler.ir.*;
import fr.insalyon.citi.golo.runtime.OperatorType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static java.util.Arrays.asList;

/* TODO:
 * [ ] match pretty
 * [ ] stack for binary operators to correctly parenthesize them
 * [ ] struct constructor functions
 * [ ] decorators not inlined
 */
public class GoloPrettyPrinter implements GoloIrVisitor {

  private int spacing = 0;
  private boolean onlyReturn = false;
  private boolean inFunctionRoot = false;
  private StringBuilder buffer;

  // configuration: should be customizable
  private int collectionWrappingThreshold = 5;
  private boolean expanded = false;

  public GoloPrettyPrinter(boolean expanded) {
    super();
    this.expanded = expanded;
  }

  private void print(Object o) {
    this.buffer.append(o);
  }

  private void newline() {
    buffer.append('\n');
  }

  private void blankLine() {
    if (buffer.charAt(buffer.length() - 1) != '\n') { buffer.append('\n'); }
    if (buffer.charAt(buffer.length() - 2) != '\n') { buffer.append('\n'); }
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
      buffer.append(' ');
    }
  }

  private void incr() {
    spacing = spacing + 2;
  }

  private void decr() {
    spacing = spacing - 2;
  }

  private void beginBlock(String delim) {
    println(delim);
    incr();
    space();
  }

  private void endBlock(String delim) {
    newline();
    decr();
    space();
    println(delim);
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

  private void joinedVisit(Iterable<? extends GoloElement> elements, String separator) {
    boolean first = true;
    for (GoloElement element : elements) {
      if (!first) {
        print(separator);
        if (separator.endsWith("\n")) { space(); }
      } else {
        first = false;
      }
      element.accept(this);
    }
  }

  private void printDocumentation(GoloElement element) {
    if (element.getDocumentation() != null) {
      space();
      print("----");
      for (String line : asList(element.getDocumentation().split("\n"))) {
        space();
        println(line.trim());
      }
      space();
      println("----");
    }
  }

  @Override
  public void visitModule(GoloModule module) {
    GoloFunction main = null;
    this.buffer = new StringBuilder();
    printDocumentation(module);
    println("module " + module.getPackageAndClass());
    newline();

    for (ModuleImport imp : module.getImports()) {
      imp.accept(this);
    }

    for (GoloFunction function : module.getFunctions()) {
      if (function.isMain()) { 
        // skip the main function to print it at the bottom of the file
        main = function;
        continue;
      }
      function.accept(this);
    }

    for (Struct struct : module.getStructs()) {
      struct.accept(this);
    }

    for (String augmentation : module.getAugmentations().keySet()) {
      println("\n");
      print("augment " + augmentation);
      print(" ");
      beginBlock("{");
      joinedVisit(module.getAugmentations().get(augmentation), "\n\n");
      endBlock("}");
    }

    if (main != null) {
      main.accept(this);
    }
    System.out.println(this.buffer.toString());
  }

  @Override
  public void visitAugmentation(Augmentation augment) {
    // TODO
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
    if (struct.getMembers().size() > collectionWrappingThreshold) {
      beginBlock("{");
      join(struct.getMembers(), ",\n");
      endBlock("}");
    } else {
      print("{");
      join(struct.getMembers(), ", ");
      println("}");
    } 
  }

  @Override
  public void visitFunction(GoloFunction function) {
    if (function.isModuleInit()) {
      visitModuleInit(function);
    } else if (expanded || !function.isSynthetic()) {
      visitFunctionDefinition(function);
    }
  }

  private void visitClosureExpression(GoloFunction function) {
    inFunctionRoot = true;
    if (function.getSyntheticParameterCount() == 0) {
      visitFunctionExpression(function);
    } else {
      int realArity = function.getArity() - function.getSyntheticParameterCount();
      if (realArity != 0) { print("|"); }
      join(function.getParameterNames().subList(
          function.getSyntheticParameterCount(),
          function.getArity()) , ", ");
      if (function.isVarargs()) { print("..."); }
      if (realArity != 0) { print("| "); }
      function.getBlock().accept(this);
    }
  }

  private void visitFunctionExpression(GoloFunction function) {
    inFunctionRoot = true;
    if (function.getArity() != 0) { print("|"); }
    join(function.getParameterNames(), ", ");
    if (function.isVarargs()) {
      print("...");
    }
    if (function.getArity() != 0) { print("| "); }
    function.getBlock().accept(this);
  }

  private void visitModuleInit(GoloFunction function) {
    if (expanded) {
      visitFunctionDefinition(function);
    } else {
      joinedVisit(function.getBlock().getStatements(), "\n");
    }
  }

  private void visitFunctionDefinition(GoloFunction function) {
    blankLine();
    printDocumentation(function);
    for (Decorator decorator : function.getDecorators()) {
      decorator.accept(this);
    }
    if (function.getVisibility() == GoloFunction.Visibility.LOCAL) {
      print("local ");
    }
    print("function " + function.getName());
    print(" = ");
    visitFunctionExpression(function);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    // TODO: not expanded version ?
    /*if (!expanded) {
      print("@");
      decorator.getExpressionStatement().accept(this);
      newline();
      space();
    }*/
  }

  private void checkIsOnlyReturn(List<GoloStatement> statements) {
    this.onlyReturn = (
        statements.size() == 1 
        && statements.get(0) instanceof ReturnStatement
        && inFunctionRoot
        && !expanded
    );
  }

  private boolean isLoopBlock(List<GoloStatement> statements) {
    return (statements.size() == 1 && statements.get(0) instanceof LoopStatement);
  }

  @Override
  public void visitBlock(Block block) {
    List<GoloStatement> statements = block.getStatements();
    checkIsOnlyReturn(statements);
    if (onlyReturn || isLoopBlock(statements)) {
      statements.get(0).accept(this);
    } else {
      beginBlock("{");
      joinedVisit(block.getStatements(), "\n");
      endBlock("}");
    }
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    Object value = constantStatement.getValue();
    if (value instanceof String) {
      print("\"" + escape((String) value) + "\"");
    } else if (value instanceof Character) {
      print("'" + value + "'");
    } else if (value instanceof Long) {
      print(value + "_L");
    } else if (value instanceof Class<?>) {
      // FIXME
      print(((Class<?>) value).getName() + ".class");
    } else {
      print(constantStatement.getValue());
    }
  }

  private boolean isNull(GoloStatement statement) {
    return (
      statement instanceof ConstantStatement
      && ((ConstantStatement) statement).getValue() == null
    );
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    if (! (inFunctionRoot && isNull(returnStatement.getExpressionStatement()))) {
      if (onlyReturn) {
        print("-> ");
      } else {
        print("return ");
      }
      returnStatement.getExpressionStatement().accept(this);
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (functionInvocation.isAnonymous()) {
      print(")(");
    } else {
      print(functionInvocation.getName() + "(");
    }
    joinedVisit(functionInvocation.getArguments(), ", ");
    for (FunctionInvocation invocation : functionInvocation.getAnonymousFunctionInvocations()) {
      invocation.accept(this);
    }
    if (!functionInvocation.isAnonymous()) { print(")"); }
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
    assignmentStatement.getExpressionStatement().accept(this);
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
    binaryOperation.getLeftExpression().accept(this);
    if (binaryOperation.getType() != OperatorType.METHOD_CALL
        && binaryOperation.getType() != OperatorType.ELVIS_METHOD_CALL) {
      print(" ");
    }
    print(binaryOperation.getType() + " ");
    binaryOperation.getRightExpression().accept(this);
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    print(unaryOperation.getType());
    unaryOperation.getExpressionStatement().accept(this);
  }

  private boolean isForEach(LoopStatement loop) {
    if (loop.hasPostStatement()) { return false; }
    if (!(loop.getConditionStatement() instanceof BinaryOperation)) { return false; }
    BinaryOperation cond = (BinaryOperation) loop.getConditionStatement();
    if (!(cond.getRightExpression() instanceof MethodInvocation)) { return false; }
    MethodInvocation meth = (MethodInvocation) cond.getRightExpression();
    return "hasNext".equals(meth.getName());
  }

  private void visitForEachLoopStatement(LoopStatement loop) {
    print("foreach ");
    visitForEachParam(loop);
    print(" in ");
    visitForEarchCollection(loop);
    println(" {");
    incr();
    List<GoloStatement> statements = loop.getBlock().getStatements();
    for (GoloStatement st : statements.subList(1, statements.size())) {
      space();
      st.accept(this);
      newline();
    }
    decr();
    space();
    print("}");
  }

  private void visitForEarchCollection(LoopStatement loop) {
    AssignmentStatement init = loop.getInitStatement();
    BinaryOperation iter = (BinaryOperation) init.getExpressionStatement();
    iter.getLeftExpression().accept(this);
  }

  private void visitForEachParam(LoopStatement loop) {
    AssignmentStatement var = (AssignmentStatement) loop.getBlock().getStatements().get(0);
    print(var.getLocalReference().getName());
  }

  private void visitForLoopStatement(LoopStatement loop) {
    print("for (");
    if (loop.hasInitStatement()) {
      loop.getInitStatement().accept(this);
    }
    print(", ");
    loop.getConditionStatement().accept(this);
    print(",");
    if (loop.hasPostStatement()) {
      print(" ");
      loop.getPostStatement().accept(this);
    }
    print(") ");
    loop.getBlock().accept(this);
  }

  private void visitWhileLoopStatement(LoopStatement loop) {
    if (loop.hasInitStatement()) {
      loop.getInitStatement().accept(this);
      newline();
      space();
    }
    print("while ");
    loop.getConditionStatement().accept(this);
    print(" ");
    loop.getBlock().accept(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    boolean wasInFuncRoot = inFunctionRoot;
    inFunctionRoot= false;
    // TODO: refactor by adding a WhileLoopStatement / EachLoopStatement / ForLoopStatement
    if (loopStatement.hasPostStatement()) { 
      visitForLoopStatement(loopStatement);
    } else if (isForEach(loopStatement) && !expanded) {
      visitForEachLoopStatement(loopStatement);
    } else {
      visitWhileLoopStatement(loopStatement);
    }
    inFunctionRoot = wasInFuncRoot;
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    print(methodInvocation.getName() + "(");
    joinedVisit(methodInvocation.getArguments(), ", ");
    for (FunctionInvocation invocation : methodInvocation.getAnonymousFunctionInvocations()) {
      invocation.accept(this);
    }
    print(")");
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    println("throw ");
    throwStatement.getExpressionStatement().accept(this);
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
      // TODO: bind the captured parameters ?
      print("^" + closureReference.getTarget().getName());
    } else {
      visitClosureExpression(closureReference.getTarget());
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    // TODO
    incr();
    space();
    println("Loop break flow: " + loopBreakFlowStatement.getType().name());
    decr();
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    if (collectionLiteral.getType() != CollectionLiteral.Type.tuple || expanded) {
      print(collectionLiteral.getType());
    }
    if (collectionLiteral.getExpressions().size() > collectionWrappingThreshold) {
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
