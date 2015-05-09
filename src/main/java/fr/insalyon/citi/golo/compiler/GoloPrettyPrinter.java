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
import fr.insalyon.citi.golo.compiler.parser.GoloParser;

import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import static java.util.Arrays.asList;

// TODO: prettify match statements
// TODO: deal with struct constructor functions (display them or not?)

public class GoloPrettyPrinter implements GoloIrVisitor {

  // configuration: should be customizable
  private static final int COLLECTION_WRAPPING_THRESHOLD = 5;
  private static final int INDENT = 2;

  private int spacing = 0;
  private boolean onlyReturn = false;
  private boolean inFunctionRoot = false;
  private int binaryOperatorLevel = 0;
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

  private void joinedVisit(Iterable<? extends GoloElement> elements, String separator) {
    boolean first = true;
    for (GoloElement element : elements) {
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

  public void prettyPrint(GoloModule module) {
    initBuffers();
    this.visitModule(module);
    System.out.println(this.header);
    System.out.println(this.buffer);
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
    this.buffer = this.header;
    printDocumentation(module);
    println("module " + module.getPackageAndClass());
    blankLine();

    for (ModuleImport imp : module.getImports()) {
      imp.accept(this);
    }
    this.buffer = this.body;
    for (MacroInvocation macroCall : module.getMacroInvocations()) {
      newlineIfNeeded();
      macroCall.accept(this);
    }
    for (Struct struct : module.getStructs()) {
      struct.accept(this);
    }
    for (Union union : module.getUnions()) {
      union.accept(this);
    }
    for (NamedAugmentation augmentation : module.getFullNamedAugmentations()) {
      augmentation.accept(this);
    }
    for (Augmentation augmentation : module.getFullAugmentations()) {
      augmentation.accept(this);
    }
    for (GoloFunction macro : module.getMacros()) {
      macro.accept(this);
    }
    for (GoloFunction function : module.getFunctions()) {
      function.accept(this);
    }
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
    for (Union.Value value : union.getValues()) {
      newlineIfNeeded();
      printUnionValue(value);
    }
    endBlock("}");
  }

  private void printUnionValue(Union.Value value) {
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
        function.getArity()) , ", ");
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
    if (function.getVisibility() == GoloFunction.Visibility.LOCAL) {
      print("local ");
    }
    print(function.isMacro() ? "macro " : "function ");
    print(function.getName());
    print(" = ");
    printFunctionExpression(function);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    // TODO: prettyprint a decorator
    /*
    if (!expanded) {
      print("@");
      decorator.getExpressionStatement().accept(this);
      newline();
      space();
    }
    */
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
      if (expanded || inFunctionRoot) {
        beginBlock("{");
      }
      for (GoloStatement s : block.getStatements()) {
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
    Object value = constantStatement.getValue();
    if (value instanceof String) {
      print("\"" + escape((String) value) + "\"");
    } else if (value instanceof Character) {
      print("'" + value + "'");
    } else if (value instanceof Long) {
      print(value + "_L");
    } else if (value instanceof GoloParser.ParserClassRef) {
      print(((GoloParser.ParserClassRef) value).name + ".class");
    } else if (value instanceof GoloParser.FunctionRef) {
      print("^" +
            ((((GoloParser.FunctionRef) value).module != null) 
              ? (((GoloParser.FunctionRef) value).module + "::") 
              : "") +
            ((GoloParser.FunctionRef) value).name);
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

  private boolean isReturnNull(GoloStatement statement) {
    return (statement instanceof ReturnStatement) 
      && isNull(((ReturnStatement) statement).getExpressionStatement());
  }

  private boolean willPrintStatement(GoloStatement statement) {
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
      returnStatement.getExpressionStatement().accept(this);
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    // TODO: deal with banged calls
    if (!functionInvocation.isAnonymous()) {
      print(functionInvocation.getName());
    }
    printInvocationArguments(functionInvocation.getArguments());
    for (FunctionInvocation invocation : functionInvocation.getAnonymousFunctionInvocations()) {
      invocation.accept(this);
    }
  }

  private void printInvocationArguments(List<ExpressionStatement> arguments) {
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
    assignmentStatement.getExpressionStatement().accept(this);
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    if (referenceLookup.isUnquoted()) {
      print("unquote(" + referenceLookup.getName() + ")");
    } else {
      print(referenceLookup.getName());
    }
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
    if (binaryOperatorLevel > 0) { print("("); }
    binaryOperatorLevel++;
    binaryOperation.getLeftExpression().accept(this);
    if (binaryOperation.getType() != OperatorType.METHOD_CALL
        && binaryOperation.getType() != OperatorType.ELVIS_METHOD_CALL) {
      print(" ");
    }
    print(binaryOperation.getOperator() + " ");
    binaryOperation.getRightExpression().accept(this);
    binaryOperatorLevel--;
    if (binaryOperatorLevel > 0) { print(")"); }
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
    beginBlock(" {");
    List<GoloStatement> statements = loop.getBlock().getStatements();
    for (GoloStatement st : statements.subList(1, statements.size())) {
      space();
      st.accept(this);
      newline();
    }
    endBlock("}");
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
    print(methodInvocation.getName());
    printInvocationArguments(methodInvocation.getArguments());
    for (FunctionInvocation invocation : methodInvocation.getAnonymousFunctionInvocations()) {
      invocation.accept(this);
    }
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

  @Override
  public void visitMacroInvocation(MacroInvocation macroInvocation) {
    LinkedList<ExpressionStatement> arguments = new LinkedList<>(macroInvocation.getArguments());
    Block blockArgument = null;
    if (!arguments.isEmpty() && arguments.getLast() instanceof Block) {
      blockArgument = (Block) arguments.removeLast();
    }
    print("&" + macroInvocation.getName());
    if (!arguments.isEmpty() || blockArgument == null) {
      if (blockArgument != null) { print(" "); }
      printInvocationArguments(arguments);
    }
    if (blockArgument != null) {
      print(" ");
      blockArgument.accept(this);
    }
  }

  @Override
  public void visitQuotedBlock(QuotedBlock qblock) {
    // TODO: better display builders in expanded mode
    print("quote ");
    if (!(qblock.getStatement() instanceof Block)) { beginBlock("{"); }
    qblock.getStatement().accept(this);
    if (!(qblock.getStatement() instanceof Block)) { endBlock("{"); }
  }

  @Override
  public void visitNoop(Noop noop) {
    print("# " + noop.comment());
  }
}