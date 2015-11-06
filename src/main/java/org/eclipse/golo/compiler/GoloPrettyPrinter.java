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
import java.util.List;
import java.util.Deque;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.function.Function;
import static java.util.Arrays.asList;

/* TODO:
 * - keep comments
 * - line wrapping!
 */
public class GoloPrettyPrinter implements GoloIrVisitor {

  // configuration: should be customizable
  private static final int COLLECTION_WRAPPING_THRESHOLD = 5;

  private final CodePrinter printer = new CodePrinter();

  private static final List<String> RESERVED = asList(
    "module", "import", "function", "local", "return", "if", "else", "while",
    "for", "foreach", "in", "throw", "try", "catch", "finally", "case", "when",
    "match", "then", "otherwise", "augment", "pimp", "augmentation", "with",
    "break", "continue", "struct", "union", "oftype", "is", "isnt", "and", "or",
    "orIfNull", "not", "var", "let", "null", "true", "false");

  private Deque<Boolean> compactReturn = new LinkedList<>();
  private boolean expanded = false;

  private GoloModule module;

  public GoloPrettyPrinter(boolean expanded) {
    this.expanded = expanded;
    printer.noSpaceAfter(' ', '[', '(', '|');
  }

  private static String quoteReserved(String name) {
    if (RESERVED.contains(name)) {
      return '`' + name;
    }
    return name;
  }

  private void printDocumentation(GoloElement<?> element) {
    if (element.documentation() != null && !element.documentation().isEmpty()) {
      printer.blankLine();
      printer.space();
      printer.println("----");
      printer.printMultiLines(element.documentation());
      printer.space();
      printer.println("----");
    }
  }

  @Override
  public void visitModule(GoloModule module) {
    this.module = module;
    printer.reset();
    printDocumentation(module);
    printer.println("module " + module.getPackageAndClass());
    printer.blankLine();
    module.walk(this);
    // TODO: don't print, create a specific method
    System.out.print(printer.toString().trim());
  }

  @Override
  public void visitAugmentation(Augmentation augment) {
    String target = augment.getTarget().toString();
    if (!expanded && (module.getPackageAndClass().toString() + ".types").equals(augment.getTarget().packageName())) {
      target = augment.getTarget().className();
    }
    printer.blankLine();
    printDocumentation(augment);
    if (augment.hasNames()) {
      printer.printf("augment %s with ", target);
      printer.join(augment.getNames(), ", ");
      printer.newline();
    }
    if (augment.hasFunctions()) {
      printer.printf("augment %s ", target);
      printer.beginBlock("{");
      for (GoloFunction func : augment.getFunctions()) {
        func.accept(this);
      }
      printer.endBlock("}");
    }
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation augmentation) {
    printer.blankLine();
    printDocumentation(augmentation);
    if (augmentation.hasFunctions()) {
      printer.printf("augmentation %s = ", quoteReserved(augmentation.getName()));
      printer.beginBlock("{");
      for (GoloFunction func : augmentation.getFunctions()) {
        func.accept(this);
      }
      printer.endBlock("}");
    }
  }

  @Override
  public void visitModuleImport(ModuleImport moduleImport) {
    if (expanded || !moduleImport.isImplicit()) {
      printer.println("import " + moduleImport.getPackageAndClass().toString());
    }
  }

  @Override
  public void visitStruct(Struct struct) {
    printer.blankLine();
    printer.print("struct " + struct.getPackageAndClass().className() + " = ");
    if (struct.getMembers().size() > COLLECTION_WRAPPING_THRESHOLD) {
      printer.beginBlock("{");
      printer.join(struct.getMembers(), ",\n");
      printer.endBlock("}");
    } else {
      printer.print("{ ");
      printer.join(struct.getMembers(), ", ");
      printer.println(" }");
    }
  }

  @Override
  public void visitUnion(Union union) {
    printer.blankLine();
    printer.print("union " + union.getPackageAndClass().className() + " = ");
    printer.beginBlock("{");
    union.walk(this);
    printer.endBlock("}");
  }

  @Override
  public void visitUnionValue(UnionValue value) {
    printer.newlineIfNeeded();
    printer.space();
    printer.print(quoteReserved(value.getName()));
    if (value.hasMembers()) {
      printer.print(" = { ");
      printer.join(value.getMembers(), ", ");
      printer.println(" }");
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
      printer.print("|");
    }
    printer.join(function.getParameterNames().subList(
        startArguments,
        function.getArity()), ", ");
    if (function.isVarargs()) {
      printer.print("...");
    }
    if (realArity != 0) {
      printer.print("| ");
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
      printer.blankLine();
      printer.joinedVisit(this, function.getBlock().getStatements(), "\n");
    }
  }

  private void printFunctionDefinition(GoloFunction function) {
    printer.blankLine();
    printDocumentation(function);
    List<Decorator> decorators = new ArrayList<>(function.getDecorators());
    Collections.reverse(decorators);
    for (Decorator decorator : decorators) {
      decorator.accept(this);
    }
    printer.space();
    if (function.isLocal()) {
      printer.print("local ");
    }
    printer.printf("function %s = ", quoteReserved(function.getName()));
    printFunctionExpression(function);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    if (!expanded) {
      printer.space();
      printer.print('@');
      if (decorator.isConstant()) {
        printer.print('!');
      }
      decorator.walk(this);
      printer.newline();
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
    printer.space();
    if (compactReturn() || isLoopBlock(statements)) {
      statements.get(0).accept(this);
    // } else if (isCompact(block)) {
    //   printer.print("{ ");
    //   if (!block.isEmpty()) {
    //     statements.get(0).accept(this);
    //   }
    //   printer.space();
    //   printer.print("}");
    } else {
      printer.beginBlock("{");
      for (GoloStatement<?> s : block.getStatements()) {
        if (willPrint(s)) {
          printer.newlineIfNeeded();
          printer.space();
        }
        s.accept(this);
      }
      printer.endBlock("}");
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
      printer.print("\"\"\"" + value + "\"\"\"");
    } else {
      printer.print("\"" + escape(value) + "\"");
    }
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    Object value = constantStatement.value();
    if (value == null) {
      printer.print("null");
    } else if (value instanceof String) {
      printString((String) value);
    } else if (value instanceof Character) {
      printer.print("'" + escape(value.toString()) + "'");
    } else if (value instanceof Long) {
      printer.print(value + "_L");
    } else if (value instanceof Float) {
      printer.print(value + "_F");
    } else if (value instanceof ClassReference) {
      printer.print(((ClassReference) value).getName() + ".class");
    } else if (value instanceof FunctionRef) {
      FunctionRef ref = (FunctionRef) value;
      printer.print("^"
          + (ref.module() != null
             ? (ref.module() + "::")
             : "")
          + ref.name());
    } else {
      printer.print(value);
    }
  }


  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    if (compactReturn()) {
      printer.print("->");
      printer.addBreak();
      returnStatement.walk(this);
    } else {
      if (!returnStatement.isSynthetic() || expanded) {
        printer.print("return");
        if (!returnStatement.isReturningVoid()) {
          printer.space();
          returnStatement.walk(this);
        }
      }
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (!functionInvocation.isAnonymous()) {
      printer.print(quoteReserved(functionInvocation.getName()));
    }
    if (functionInvocation.isConstant()) {
      printer.print('!');
    }
    printInvocationArguments(functionInvocation.getArguments());
  }

  private void printInvocationArguments(List<GoloElement<?>> arguments) {
    if (arguments.size() < COLLECTION_WRAPPING_THRESHOLD) {
      printer.print("(");
      printer.joinedVisit(this, arguments, ",");
      printer.print(")");
    } else {
      printer.beginBlock("(");
      printer.joinedVisit(this, arguments, ",\n");
      printer.endBlock(")");
    }
  }

  private void printAssignementKind(LocalReference.Kind kind, boolean declaring) {
    switch (kind) {
      case CONSTANT:
      case MODULE_CONSTANT:
        printer.print("let ");
        break;
      case VARIABLE:
      case MODULE_VARIABLE:
        if (declaring) {
          printer.print("var ");
        }
        break;
      default:
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    LocalReference ref = assignmentStatement.getLocalReference();
    printAssignementKind(ref.getKind(), assignmentStatement.isDeclaring());
    printer.print(quoteReserved(ref.getName()) + " = ");
    assignmentStatement.expression().accept(this);
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    printer.print(quoteReserved(referenceLookup.getName()));
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    printer.print("if ");
    conditionalBranching.getCondition().accept(this);
    printer.space();
    conditionalBranching.getTrueBlock().accept(this);
    if (conditionalBranching.hasFalseBlock()) {
      printer.print(" else ");
      conditionalBranching.getFalseBlock().accept(this);
    } else if (conditionalBranching.hasElseConditionalBranching()) {
      printer.print(" else ");
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
    if (brackets) { printer.print("("); }
    sideExpr.accept(this);
    if (brackets) { printer.print(")"); }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    printOpSide(binaryOperation, BinaryOperation::left);
    if (!binaryOperation.isMethodCall()) { printer.space(); }
    printer.print(binaryOperation.getType());
    if (!binaryOperation.isMethodCall()) { printer.space(); }
    printOpSide(binaryOperation, BinaryOperation::right);
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    printer.print(unaryOperation.getType());
    printer.space();
    ExpressionStatement<?> expr = unaryOperation.expression();
    boolean brackets = expr instanceof BinaryOperation && !((BinaryOperation) expr).isMethodCall();
    if (brackets) { printer.print("("); }
    expr.accept(this);
    if (brackets) { printer.print(")"); }
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement loop) {
    printer.space();
    printer.print("foreach ");
    printer.joinedVisit(this, asList(loop.getReferences()), ", ");
    if (loop.isVarargs()) {
      printer.print("...");
    }
    printer.print(" in ");
    loop.getIterable().accept(this);
    if (loop.hasWhenClause()) {
      printer.space();
      printer.print("when ");
      loop.getWhenClause().accept(this);
    }
    if (!loop.getBlock().isEmpty()) {
      loop.getBlock().accept(this);
    }
  }

  private void visitForLoopStatement(LoopStatement loop) {
    printer.print("for (");
    if (loop.hasInitStatement()) {
      loop.init().accept(this);
    }
    printer.print(", ");
    loop.condition().accept(this);
    printer.print(",");
    if (loop.hasPostStatement()) {
      printer.space();
      loop.post().accept(this);
    }
    printer.print(")");
    if (!loop.getBlock().isEmpty()) {
      loop.getBlock().accept(this);
    }
  }

  private void visitWhileLoopStatement(LoopStatement loop) {
    if (loop.hasInitStatement()) {
      loop.init().accept(this);
      printer.newline();
      printer.space();
    }
    printer.print("while ");
    loop.condition().accept(this);
    printer.space();
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
    printer.space();
    printer.print(quoteReserved(methodInvocation.getName()));
    printInvocationArguments(methodInvocation.getArguments());
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    printer.print("throw ");
    throwStatement.expression().accept(this);
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    printer.print("try ");
    tryCatchFinally.getTryBlock().accept(this);
    if (tryCatchFinally.hasCatchBlock()) {
      printer.print(" catch (" + tryCatchFinally.getExceptionId() + ")");
      tryCatchFinally.getCatchBlock().accept(this);
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      printer.print(" finally ");
      tryCatchFinally.getFinallyBlock().accept(this);
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    if (expanded) {
      printer.print("^" + closureReference.getTarget().getName());
      if (closureReference.getTarget().getSyntheticParameterCount() > 0) {
        printer.print(": insertArguments(0, ");
        printer.join(closureReference.getTarget().getSyntheticParameterNames(), ", ");
        printer.print(")");
      }
    } else {
      printFunctionExpression(closureReference.getTarget());
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    printer.space();
    printer.println(loopBreakFlowStatement.getType().name().toLowerCase());
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    if (collectionLiteral.getType() == CollectionLiteral.Type.range) {
      printer.print("[");
      printer.joinedVisit(this, collectionLiteral.getExpressions(), "..");
      printer.print("]");
    } else {
      if (collectionLiteral.getType() != CollectionLiteral.Type.tuple || expanded) {
        printer.print(collectionLiteral.getType());
      }
      if (collectionLiteral.getExpressions().size() > COLLECTION_WRAPPING_THRESHOLD) {
        printer.beginBlock("[");
        printer.space();
        printer.joinedVisit(this, collectionLiteral.getExpressions(), ",\n");
        printer.endBlock("]");
      } else {
        printer.print("[");
        printer.joinedVisit(this, collectionLiteral.getExpressions(), ",");
        printer.print("]");
      }
    }
  }

  @Override
  public void visitLocalReference(LocalReference localRef) {
    if (!localRef.isModuleState()) {
      printer.space();
      printer.print(quoteReserved(localRef.getName()));
    }
  }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) {
    printer.print(namedArgument.getName() + '=');
    namedArgument.walk(this);
  }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collection) {
    printer.print(collection.getType().toString());
    printer.print("[");
    collection.walk(this);
    printer.print("]");
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    printer.space();
    printer.print("when ");
    whenClause.condition().accept(this);
    Object action = whenClause.action();
    if (action instanceof Block) {
      Block block = (Block) action;
      if (!block.isEmpty()) {
        block.accept(this);
      }
    } else if (action instanceof ExpressionStatement) {
      ExpressionStatement<?> expr = (ExpressionStatement) action;
      printer.space();
      printer.print("then ");
      expr.accept(this);
    }
  }

  @Override
  public void visitMatchExpression(MatchExpression expr) {
    //- printer.space();
    printer.print("match ");
    visitAlternatives(expr);
  }

  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    printer.space();
    printer.print("case ");
    visitAlternatives(caseStatement);
  }

  private void visitAlternatives(Alternatives<?> alt) {
    printer.beginBlock("{");
    printer.joinedVisit(this, alt.getClauses(), "\n");
    printer.newline();
    printer.space();
    printer.print("otherwise ");
    alt.getOtherwise().accept(this);
    printer.endBlock("}");
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment destruct) {
    printer.space();
    printAssignementKind(destruct.getReferences()[0].getKind(), destruct.isDeclaring());
    printer.joinedVisit(this, asList(destruct.getReferences()), ",");
    if (destruct.isVarargs()) {
      printer.print("...");
    }
    printer.space();
    printer.print("= ");
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
