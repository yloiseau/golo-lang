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

import java.util.Collection;

import fr.insalyon.citi.golo.compiler.ir.*;

/**
 * Abstract IR Visitor.
 * <p>
 * This visitor walk the IR tree, but do nothing. It can be used to implement specific IR
 * visitors by overriding only the specific methods, like for example the ones used in the
 * compilation check and transformation step.
 */
public abstract class AbstractGoloIrVisitor implements GoloIrVisitor {

  @Override
  public void visitModule(GoloModule module) {
    // imports
    // functions
    // structs
    // module state
    // unions
    // augmentations / augmentations applications
    for (MacroInvocation macroCall : module.getMacroInvocations()) {
      macroCall.accept(this);
    }
    for (GoloFunction function : module.getFunctions()) {
      function.accept(this);
    }
    for (GoloFunction macro : module.getMacros()) {
      macro.accept(this);
    }
    for (Augmentation augmentation : module.getFullAugmentations()) {
      augmentation.accept(this);
    }
    for (NamedAugmentation augmentation : module.getFullNamedAugmentations()) {
      augmentation.accept(this);
    }
    for (Collection<GoloFunction> functions : module.getNamedAugmentations().values()) {
      for (GoloFunction function : functions) {
        function.accept(this);
      }
    }
  }

  @Override
  public void visitAugmentation(Augmentation augment) {
    for (GoloFunction func : augment.getFunctions()) {
      func.accept(this);
    }
  }

  @Override
  public void visitNamedAugmentation(NamedAugmentation augment) {
    for (GoloFunction func : augment.getFunctions()) {
      func.accept(this);
    }
  }

  @Override
  public void visitModuleImport(ModuleImport moduleImport) { }

  @Override
  public void visitStruct(Struct struct) { }

  @Override
  public void visitUnion(Union union) { }

  @Override
  public void visitFunction(GoloFunction function) {
    function.getBlock().accept(this);
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    decorator.getExpressionStatement().accept(this);
  }

  @Override
  public void visitBlock(Block block) {
    for (GoloStatement statement : block.getStatements()) {
      statement.accept(this);
    }
  }

  @Override
  public void visitQuotedBlock(QuotedBlock qblock) {
    qblock.getStatement().accept(this);
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    returnStatement.getExpressionStatement().accept(this);
  }

  public void visitAbstractInvocation(AbstractInvocation invocation) {
    for (ExpressionStatement arg : invocation.getArguments()) {
      arg.accept(this);
    }
    for (FunctionInvocation inv : invocation.getAnonymousFunctionInvocations()) {
      inv.accept(this);
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    visitAbstractInvocation(functionInvocation);
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    visitAbstractInvocation(methodInvocation);
  }

  @Override
  public void visitMacroInvocation(MacroInvocation macroInvocation) {
    visitAbstractInvocation(macroInvocation);
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    assignmentStatement.getExpressionStatement().accept(this);
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    conditionalBranching.getCondition().accept(this);
    conditionalBranching.getTrueBlock().accept(this);
    if (conditionalBranching.hasFalseBlock()) {
      conditionalBranching.getFalseBlock().accept(this);
    }
    if (conditionalBranching.hasElseConditionalBranching()) {
      conditionalBranching.getElseConditionalBranching().accept(this);
    }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    binaryOperation.getLeftExpression().accept(this);
    binaryOperation.getRightExpression().accept(this);
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    unaryOperation.getExpressionStatement().accept(this);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    if (loopStatement.hasInitStatement()) {
      loopStatement.getInitStatement().accept(this);
    }
    loopStatement.getConditionStatement().accept(this);
    loopStatement.getBlock().accept(this);
    if (loopStatement.hasPostStatement()) {
      loopStatement.getPostStatement().accept(this);
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    throwStatement.getExpressionStatement().accept(this);
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    tryCatchFinally.getTryBlock().accept(this);
    if (tryCatchFinally.hasCatchBlock()) {
      tryCatchFinally.getCatchBlock().accept(this);
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      tryCatchFinally.getFinallyBlock().accept(this);
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    // TODO: visit closure references in AbstractGoloIrVisitor
    //closureReference.getTarget().accept(this);
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    for (ExpressionStatement statement : collectionLiteral.getExpressions()) {
      statement.accept(this);
    }
  }

  @Override
  public void visitNoop(Noop noop) { }

}
