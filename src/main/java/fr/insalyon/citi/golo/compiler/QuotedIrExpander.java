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
import java.util.Deque;
import java.util.LinkedList;

import fr.insalyon.citi.golo.compiler.ir.*;

import static gololang.macros.CodeBuilder.*;
import static fr.insalyon.citi.golo.runtime.OperatorType.*;

// TODO: deal with macro calls in quoted blocks

/**
 * Visitor to expand {@code quote} expressions.
 * <p>
 * This visitor walk the IR tree and replace {@code QuotedBlock}s with calls to the
 * {@code IrBuilder} methods that build the equivalent IR when evaluated.
 */
class QuotedIrExpander extends DummyIrVisitor {
  private static final String BUILDER = "gololang.macros.CodeBuilder.";
  private static final String OPERATORS = "fr.insalyon.citi.golo.runtime.OperatorType.";
  private boolean inQuotedBlock = false;
  private Deque<Object> expandedBlocks = new LinkedList<>();
  private int nestedBlockDepth = 0;

  private static FunctionInvocationBuilder enumValue(Enum<?> val) {
    return functionInvocation().name(val.getClass().getName() + "." + val.name());
  }

  private ExpressionStatement replaceExpression(ExpressionStatement toReplace) {
    if (toReplace instanceof QuotedBlock && !expandedBlocks.isEmpty()) {
      if (expandedBlocks.peek() instanceof ExpressionStatement) {
        return (ExpressionStatement) expandedBlocks.pop();
      }
      if (expandedBlocks.peek() instanceof IrNodeBuilder) {
        return (ExpressionStatement) ((IrNodeBuilder) expandedBlocks.pop()).build();
      }
      throw new IllegalStateException("unknown type on the stack: " + expandedBlocks.peek());
    }
    return toReplace;
  }

  @Override
  public void visitQuotedBlock(QuotedBlock qblock) {
    int previousDepth = nestedBlockDepth;
    nestedBlockDepth = 0;
    inQuotedBlock = true;
    qblock.getExpression().accept(this);
    expandedBlocks.push(functionInvocation()
      .name(BUILDER + "quoted")
      .arg(expandedBlocks.pop())
      .build()
    );
    inQuotedBlock = false;
    nestedBlockDepth = previousDepth;
  }

  private boolean isSingleExpressionQuote(Block block) {
    return (
        nestedBlockDepth == 1
        && !block.isUnquoted()
        && block.getStatements().size() == 1
        && block.getStatements().get(0) instanceof ExpressionStatement
        );
  }

  @Override
  public void visitBlock(Block block) {
    nestedBlockDepth++;
    if (!inQuotedBlock) {
      super.visitBlock(block);
    } else if (block.isUnquoted()) {
      expandedBlocks.push(block);
    } else if (isSingleExpressionQuote(block)) {
      // If the quoted block contains only one expression statement, we quote this statement only
      // to ease the manipulation, and do a quotedBlock.getExpression() instead of
      // quotedBlock.getExpression().getStatements().get(0)
      block.getStatements().get(0).accept(this);
    } else {
      FunctionInvocationBuilder blockBuilder = functionInvocation().name(BUILDER + "block");
      for (GoloStatement statement : block.getStatements()) {
        statement.accept(this);
        blockBuilder.arg(expandedBlocks.pop());
      }
      expandedBlocks.push(blockBuilder);
    }
    nestedBlockDepth--;
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    super.visitReturnStatement(returnStatement);
    if (!inQuotedBlock) {
      returnStatement.setExpressionStatement(replaceExpression(returnStatement.getExpressionStatement()));
    } else {
      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "returns")
          .arg(expandedBlocks.pop())
      );
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    super.visitThrowStatement(throwStatement);
    if (!inQuotedBlock) {
      throwStatement.setExpressionStatement(replaceExpression(throwStatement.getExpressionStatement()));
    } else {
      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "throwException")
          .arg(expandedBlocks.pop())
      );
    }
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    if (!inQuotedBlock) {
      super.visitConstantStatement(constantStatement);
    } else if (constantStatement.isUnquoted()) {
      expandedBlocks.push(constantStatement);
    } else {
      expandedBlocks.push(functionInvocation()
        .name(BUILDER + "constant")
        .arg(constantStatement)
      );
    }
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    if (!inQuotedBlock) {
      super.visitReferenceLookup(referenceLookup);
    } else if (referenceLookup.isUnquoted()) {
      expandedBlocks.push(referenceLookup);
    } else {
      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "refLookup")
          .arg(constant(referenceLookup.getName()))
      );
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    super.visitAssignmentStatement(assignmentStatement);
    if (inQuotedBlock) {
      LocalReference ref = assignmentStatement.getLocalReference();
      FunctionInvocationBuilder localRefBuilder = functionInvocation()
        .name(BUILDER + "localRef")
        .arg(enumValue(ref.getKind()))
        .arg(constant(ref.getName()))
        .arg(constant(ref.getIndex()))
        .arg(constant(ref.isSynthetic()));

      expandedBlocks.push(
          functionInvocation().name(BUILDER + "assignment")
          .arg(constant(assignmentStatement.isDeclaring()))
          .arg(localRefBuilder)
          .arg(expandedBlocks.pop())
          );
    } else {
      assignmentStatement.setExpressionStatement(replaceExpression(assignmentStatement.getExpressionStatement()));
    }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    super.visitBinaryOperation(binaryOperation);
    if (inQuotedBlock) {
      if (binaryOperation.isUnquoted()) {
        expandedBlocks.push(binaryOperation);
      } else {
        Object right = expandedBlocks.pop();
        Object left = expandedBlocks.pop();
        expandedBlocks.push(functionInvocation()
            .name(BUILDER + "binaryOperation")
            .arg(enumValue(binaryOperation.getType()))
            .arg(left)
            .arg(right));
      }
    } else {
      binaryOperation.setLeftExpression(replaceExpression(binaryOperation.getLeftExpression()));
      binaryOperation.setRightExpression(replaceExpression(binaryOperation.getRightExpression()));
    }
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    super.visitLoopStatement(loopStatement);
    if (inQuotedBlock) {
      Object post = constant(null);
      if (loopStatement.hasPostStatement()) {
        post = expandedBlocks.pop();
      }
      Object init = constant(null);
      if (loopStatement.hasInitStatement()) {
        init = expandedBlocks.pop();
      }
      Object condition = expandedBlocks.pop();
      Object block = expandedBlocks.pop();

      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "loop")
          .arg(init).arg(condition).arg(post).arg(block));
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    if (!inQuotedBlock) {
      super.visitLoopBreakFlowStatement(loopBreakFlowStatement);
    } else {
      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "loopExit")
          .arg(enumValue(loopBreakFlowStatement.getType())));
    }
  }

  private ExpressionStatement buildInvocation(FunctionInvocationBuilder builder, AbstractInvocation invocation) {
    BinaryOperationBuilder finalObject = binaryOperation(METHOD_CALL).left(builder);
    for (ExpressionStatement arg : invocation.getArguments()) {
      arg.accept(this);
      finalObject = binaryOperation(METHOD_CALL).left(finalObject.right(
            methodInvocation("arg").arg(expandedBlocks.pop())
            ));
    }
    for (FunctionInvocation inv : invocation.getAnonymousFunctionInvocations()) {
      inv.accept(this);
      finalObject = binaryOperation(METHOD_CALL).left(finalObject.right(
            methodInvocation("anon").arg(expandedBlocks.pop())
            ));
    }
    return finalObject.left();
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (!inQuotedBlock) {
      super.visitFunctionInvocation(functionInvocation);
    } else {
      expandedBlocks.push(buildInvocation(
        functionInvocation()
          .name(BUILDER + "functionInvocation")
          .arg(constant(functionInvocation.getName()))
          .arg(constant(functionInvocation.isOnReference()))
          .arg(constant(functionInvocation.isOnModuleState()))
          .arg(constant(functionInvocation.isConstant())),
        functionInvocation
      ));
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    if (!inQuotedBlock) {
      super.visitMethodInvocation(methodInvocation);
    } else {
      expandedBlocks.push(buildInvocation(
        functionInvocation()
          .name(BUILDER + "methodInvocation")
          .arg(constant(methodInvocation.getName()))
          .arg(constant(methodInvocation.isNullSafeGuarded())),
        methodInvocation
      ));
    }
  }

  @Override
  public void visitAbstractInvocation(AbstractInvocation invocation) {
    int i = 0;
    for (ExpressionStatement arg : invocation.getArguments()) {
      arg.accept(this);
      invocation.setArgument(i, replaceExpression(arg));
      i++;
    }
    for (FunctionInvocation inv : invocation.getAnonymousFunctionInvocations()) {
      inv.accept(this);
    }
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    super.visitConditionalBranching(conditionalBranching);
    if (inQuotedBlock) {
      Object elseBranch = constant(null);
      if (conditionalBranching.hasElseConditionalBranching()) {
        elseBranch = expandedBlocks.pop();
      }
      Object falseBlock = constant(null);
      if (conditionalBranching.hasFalseBlock()) {
        falseBlock = expandedBlocks.pop();
      }
      Object trueBlock = expandedBlocks.pop();
      Object condition = expandedBlocks.pop();

      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "branch")
          .arg(condition)
          .arg(trueBlock)
          .arg(falseBlock)
          .arg(elseBranch)
      );
    }
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    super.visitUnaryOperation(unaryOperation);
    if (inQuotedBlock) {
      if (unaryOperation.isUnquoted()) {
        expandedBlocks.push(unaryOperation);
      } else {
        expandedBlocks.push(functionInvocation()
            .name(BUILDER + "unaryOperation")
            .arg(enumValue(unaryOperation.getType()))
            .arg(expandedBlocks.pop()));
      }
    } else {
      unaryOperation.setExpressionStatement(replaceExpression(unaryOperation.getExpressionStatement()));
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    super.visitClosureReference(closureReference);
    if (inQuotedBlock) {
      throw new IllegalStateException("closures can't be quoted yet");
    }
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    super.visitDecorator(decorator);
    if (inQuotedBlock) {
      throw new IllegalStateException("decorators can't be quoted yet");
    }
  }

  @Override
  public void visitFunction(GoloFunction function) {
    super.visitFunction(function);
    if (inQuotedBlock) {
      throw new IllegalStateException("functions can't be quoted yet");
    }
  }

  @Override
  public void visitModule(GoloModule module) {
    super.visitModule(module);
    if (inQuotedBlock) {
      throw new IllegalStateException("modules can't be quoted yet");
    }
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    if (!inQuotedBlock) {
      super.visitCollectionLiteral(collectionLiteral);
    } else {
      FunctionInvocationBuilder builder = functionInvocation()
        .name(BUILDER + "collection")
        .arg(enumValue(collectionLiteral.getType()));
      for (ExpressionStatement statement : collectionLiteral.getExpressions()) {
        statement.accept(this);
        builder.arg(expandedBlocks.pop());
      }
      expandedBlocks.push(builder);
    }
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    super.visitTryCatchFinally(tryCatchFinally);
    if (inQuotedBlock) {
      Object finallyBlock = constant(null);
      if (tryCatchFinally.hasFinallyBlock()) {
        finallyBlock = expandedBlocks.pop();
      }
      Object catchBlock = constant(null);
      if (tryCatchFinally.hasCatchBlock()) {
        catchBlock = expandedBlocks.pop();
      }
      Object tryBlock = expandedBlocks.pop();
      expandedBlocks.push(functionInvocation()
          .name(BUILDER + "tryCatchFinally")
          .arg(constant(tryCatchFinally.getExceptionId()))
          .arg(tryBlock)
          .arg(catchBlock)
          .arg(finallyBlock)
      );
    }
  }

}
