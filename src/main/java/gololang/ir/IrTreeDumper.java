/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package gololang.ir;

import org.eclipse.golo.compiler.CodePrinter;
import java.io.PrintStream;

public class IrTreeDumper implements GoloIrVisitor {

  private final PrintStream out;
  private CodePrinter printer = new CodePrinter();
  private GoloModule currentModule;

  public IrTreeDumper() {
    this(System.out);
  }

  public IrTreeDumper(PrintStream out) {
    this.out = out;
    printer.linePrefix("# ");
  }

  @Override
  public void visitModule(GoloModule module) {
    if (currentModule == module) {
      printer.incr();
      printer.space();
      printer.println("current module");
      printer.decr();
      return;
    }
    currentModule = module;
    printer.reset();
    printer.space();
    printer.println(module.getPackageAndClass());
    printer.print(" [Local References: ");
    printer.print(System.identityHashCode(module.getReferenceTable()));
    printer.println("]");
    module.walk(this);
    out.print(printer);
  }

  @Override
  public void visitModuleImport(ModuleImport moduleImport) {
    printer.incr();
    printer.space();
    printer.print(" - ");
    printer.println(moduleImport);
    moduleImport.walk(this);
    printer.decr();
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation namedAugmentation) {
    printer.incr();
    printer.space();
    printer.print("Named Augmentation ");
    printer.println(namedAugmentation.getName());
    namedAugmentation.walk(this);
    printer.decr();
  }

  @Override
  public void visitAugmentation(Augmentation augmentation) {
    printer.incr();
    printer.space();
    printer.print("Augmentation on ");
    printer.println(augmentation.getTarget());
    if (augmentation.hasNames()) {
      printer.incr();
      for (String name : augmentation.getNames()) {
        printer.space();
        printer.print("Named Augmentation ");
        printer.println(name);
      }
      printer.decr();
    }
    augmentation.walk(this);
    printer.decr();
  }

  @Override
  public void visitStruct(Struct struct) {
    printer.incr();
    printer.space();
    printer.print("Struct ");
    printer.println(struct.getPackageAndClass().className());
    printer.space();
    printer.print(" - target class = ");
    printer.println(struct.getPackageAndClass());
    printer.incr();
    printer.space();
    printer.println("Members: ");
    struct.walk(this);
    printer.decr();
    printer.decr();
  }

  @Override
  public void visitUnion(Union union) {
    printer.incr();
    printer.space();
    printer.print("Union ");
    printer.println(union.getPackageAndClass().className());
    printer.space();
    printer.print(" - target class = ");
    printer.println(union.getPackageAndClass());
    union.walk(this);
    printer.decr();
  }

  @Override
  public void visitUnionValue(UnionValue value) {
    printer.incr();
    printer.space();
    printer.print("Value ");
    printer.println(value.getPackageAndClass().className());
    printer.space();
    printer.print(" - target class = ");
    printer.println(value.getPackageAndClass());
    if (value.hasMembers()) {
      printer.incr();
      printer.space();
      printer.println("Members: ");
      value.walk(this);
      printer.decr();
    }
    printer.decr();
  }

  @Override
  public void visitFunction(GoloFunction function) {
    printer.incr();
    printer.space();
    if (function.isLocal()) {
      printer.print("Local function ");
    } else {
      printer.print("Function ");
    }
    printer.print(function.getName());
    printer.print(" = ");
    visitFunctionDefinition(function);
    printer.decr();
  }

  private void visitFunctionDefinition(GoloFunction function) {
    printer.print("|");
    boolean first = true;
    for (String param : function.getParameterNames()) {
      if (first) {
        first = false;
      } else {
        printer.print(", ");
      }
      printer.print(param);
    }
    printer.print("|");
    if (function.isVarargs()) {
      printer.print(" (varargs)");
    }
    if (function.isSynthetic()) {
      printer.print(" (synthetic, ");
      printer.print(function.getSyntheticParameterCount());
      printer.print(" synthetic parameters)");
      if (function.getSyntheticSelfName() != null) {
        printer.print(" (selfname: ");
        printer.print(function.getSyntheticSelfName());
        printer.print(")");
      }
    }
    printer.newline();
    function.walk(this);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    printer.incr();
    printer.space();
    printer.println("@Decorator");
    decorator.walk(this);
    printer.decr();
  }

  @Override
  public void visitBlock(Block block) {
    if (block.isEmpty()) { return; }
    printer.incr();
    printer.space();
    printer.println("Block");
    printer.print(" [Local References: ");
    printer.print(System.identityHashCode(block.getReferenceTable()));
    printer.print(" -> ");
    printer.print(System.identityHashCode(block.getReferenceTable().parent()));
    printer.println("]");
    block.walk(this);
    printer.decr();
  }

  @Override
  public void visitLocalReference(LocalReference ref) {
    printer.incr();
    printer.space();
    printer.print(" - ");
    printer.println(ref);
    printer.decr();
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    printer.incr();
    printer.space();
    Object v = constantStatement.value();
    printer.print("Constant = ");
    printer.print(v);
    if (v != null) {
      printer.print(" (");
      printer.print(v.getClass().getName());
      printer.print(")");
    }
    printer.newline();
    printer.decr();
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    printer.incr();
    printer.space();
    printer.println("Return");
    returnStatement.walk(this);
    printer.decr();
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    printer.incr();
    printer.space();
    printer.print("Function call: ");
    printer.print(functionInvocation.getName());
    printer.print(", on reference? -> ");
    printer.print(functionInvocation.isOnReference());
    printer.print(", on module state? -> ");
    printer.print(functionInvocation.isOnModuleState());
    printer.print(", anonymous? -> ");
    printer.print(functionInvocation.isAnonymous());
    printer.print(", constant? -> ");
    printer.print(functionInvocation.isConstant());
    printer.print(", named arguments? -> ");
    printer.println(functionInvocation.usesNamedArguments());
    functionInvocation.walk(this);
    printLocalDeclarations(functionInvocation);
    printer.decr();
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    printer.incr();
    printer.space();
    printer.print("Assignment: ");
    printer.print(assignmentStatement.getLocalReference().toString());
    printer.println(assignmentStatement.isDeclaring() ? " (declaring)" : "");
    assignmentStatement.walk(this);
    printer.decr();
  }

  @Override
  public void visitDestructuringAssignment(DestructuringAssignment assignment) {
    printer.incr();
    printer.space();
    printer.printf(
        "Destructuring assignement: {declaring=%s, varargs=%s}%n",
        assignment.isDeclaring(),
        assignment.isVarargs());
    assignment.walk(this);
    printer.decr();
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    printer.incr();
    printer.space();
    printer.print("Reference lookup: ");
    printer.println(referenceLookup.getName());
    printLocalDeclarations(referenceLookup);
    printer.decr();
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    printer.incr();
    printer.space();
    printer.println("Conditional");
    conditionalBranching.walk(this);
    printer.decr();
  }

  @Override
  public void visitCaseStatement(CaseStatement caseStatement) {
    printer.incr();
    printer.space();
    printer.println("Case");
    printer.incr();
    for (WhenClause<Block> c : caseStatement.getClauses()) {
      c.accept(this);
    }
    printer.space();
    printer.println("Otherwise");
    caseStatement.getOtherwise().accept(this);
    printer.decr();
  }

  @Override
  public void visitMatchExpression(MatchExpression matchExpression) {
    printer.incr();
    printer.space();
    printer.println("Match");
    printer.incr();
    for (WhenClause<?> c : matchExpression.getClauses()) {
      c.accept(this);
    }
    printer.space();
    printer.println("Otherwise");
    matchExpression.getOtherwise().accept(this);
    printLocalDeclarations(matchExpression);
    printer.decr();
  }

  @Override
  public void visitWhenClause(WhenClause<?> whenClause) {
    printer.space();
    printer.println("When");
    printer.incr();
    whenClause.walk(this);
    printer.decr();
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    printer.incr();
    printer.space();
    printer.print("Binary operator: ");
    printer.println(binaryOperation.getType());
    binaryOperation.walk(this);
    printLocalDeclarations(binaryOperation);
    printer.decr();
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    printer.incr();
    printer.space();
    printer.print("Unary operator: ");
    printer.println(unaryOperation.getType());
    unaryOperation.walk(this);
    printLocalDeclarations(unaryOperation);
    printer.decr();
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    printer.incr();
    printer.space();
    printer.println("Loop");
    loopStatement.walk(this);
    printer.decr();
  }

  @Override
  public void visitForEachLoopStatement(ForEachLoopStatement foreachStatement) {
    printer.incr();
    printer.space();
    printer.println("Foreach");
    printer.incr();
    for (LocalReference ref : foreachStatement.getReferences()) {
      ref.accept(this);
    }
    foreachStatement.getIterable().accept(this);
    if (foreachStatement.hasWhenClause()) {
      printer.space();
      printer.println("When:");
      foreachStatement.getWhenClause().accept(this);
    }
    foreachStatement.getBlock().accept(this);
    printer.decr();
    printer.decr();
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    printer.incr();
    printer.space();
    printer.printf("Method invocation: %s, null safe? -> %s%n",
        methodInvocation.getName(),
        methodInvocation.isNullSafeGuarded());
    methodInvocation.walk(this);
    printLocalDeclarations(methodInvocation);
    printer.decr();
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    printer.incr();
    printer.space();
    printer.println("Throw");
    throwStatement.walk(this);
    printer.decr();
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    printer.incr();
    printer.space();
    printer.println("Try");
    tryCatchFinally.getTryBlock().accept(this);
    if (tryCatchFinally.hasCatchBlock()) {
      printer.space();
      printer.print("Catch: ");
      printer.println(tryCatchFinally.getExceptionId());
      tryCatchFinally.getCatchBlock().accept(this);
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      printer.space();
      printer.println("Finally");
      tryCatchFinally.getFinallyBlock().accept(this);
    }
    printer.decr();
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    GoloFunction target = closureReference.getTarget();
    printer.incr();
    printer.space();
    if (target.isAnonymous()) {
      printer.print("Closure: ");
      printer.incr();
      visitFunctionDefinition(target);
      printer.decr();
    } else {
      printer.printf(
          "Closure reference: %s, regular arguments at index %d%n",
          target.getName(),
          target.getSyntheticParameterCount());
      printer.incr();
      for (String refName : closureReference.getCapturedReferenceNames()) {
        printer.space();
        printer.print("- capture: ");
        printer.println(refName);
      }
      printer.decr();
    }
    printer.decr();
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    printer.incr();
    printer.space();
    printer.print("Loop break flow: ");
    printer.println(loopBreakFlowStatement.getType().name());
    printer.decr();
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    printer.incr();
    printer.space();
    printer.print("Collection literal of type: ");
    printer.println(collectionLiteral.getType());
    collectionLiteral.walk(this);
    printLocalDeclarations(collectionLiteral);
    printer.decr();
  }

  @Override
  public void visitCollectionComprehension(CollectionComprehension collectionComprehension) {
    printer.incr();
    printer.space();
    printer.print("Collection comprehension of type: ");
    printer.println(collectionComprehension.getType());
    printer.incr();
    printer.space();
    printer.println("Expression: ");
    collectionComprehension.expression().accept(this);
    printer.space();
    printer.println("Comprehension: ");
    for (GoloStatement<?> b : collectionComprehension.loops()) {
      b.accept(this);
    }
    printLocalDeclarations(collectionComprehension);
    printer.decr();
    printer.decr();
  }

  @Override
  public void visitNamedArgument(NamedArgument namedArgument) {
    printer.incr();
    printer.space();
    printer.print("Named argument: ");
    printer.println(namedArgument.getName());
    namedArgument.walk(this);
    printer.decr();
  }

  @Override
  public void visitMember(Member member) {
    printer.space();
    printer.print(" - ");
    printer.print(member.getName());
    printer.newline();
  }

  private void printLocalDeclarations(ExpressionStatement<?> expr) {
    if (expr.hasLocalDeclarations()) {
      printer.incr();
      printer.space();
      printer.println("Local declaration:");
      for (GoloAssignment<?> a : expr.declarations()) {
        a.accept(this);
      }
      printer.decr();
    }
  }

  @Override
  public void visitNoop(Noop noop) {
    printer.incr();
    printer.space();
    printer.print("Noop: ");
    printer.println(noop.comment());
    printer.decr();
  }

  @Override
  public void visitToplevelElements(ToplevelElements toplevel) {
    toplevel.walk(this);
  }
}
