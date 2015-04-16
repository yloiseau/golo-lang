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

import java.util.HashMap;

import org.objectweb.asm.MethodVisitor;

import fr.insalyon.citi.golo.compiler.ir.*;

import static org.objectweb.asm.Opcodes.*;
import static fr.insalyon.citi.golo.compiler.JavaBytecodeUtils.*;

class QuotedGoloIrVisitor implements GoloIrVisitor {
  public static final String IR = "fr/insalyon/citi/golo/compiler/ir/";
  public static final String BUILDER = "fr/insalyon/citi/golo/compiler/IrBuilder";
  public static final String RT = "fr/insalyon/citi/golo/runtime/";
  public static final String STRING = "Ljava/lang/String;";
  public static final String OBJECT = "Ljava/lang/Object;";

  private int nextHygieneId = 0;
  private GoloIrVisitor parent;
  private MethodVisitor methodVisitor;
  private HashMap<String, String> hygienicNames = new HashMap<>();

  private String nextHygienicName(String name) {
    return "__$$_" + name + "_" + nextHygieneId++;
  }

  QuotedGoloIrVisitor(GoloIrVisitor parentVisitor, MethodVisitor visitor) {
    this.parent = parentVisitor;
    this.methodVisitor = visitor;
  }

  private String irType(String cls) {
    return String.format("L%s%s;", IR, cls);
  }

  private String rtType(String cls) {
    return String.format("L%s%s;", RT, cls);
  }

  private String builderType(String cls) {
    return String.format("L%s$%s;", BUILDER, cls);
  }

  private void asmEnum(Enum<?> e) {
    String cls = e.getClass().getName().replace(".", "/");
    methodVisitor.visitFieldInsn(GETSTATIC, cls, e.name(), "L" + cls + ";");
  }

  private void loadBoolean(boolean value) {
    loadInteger(methodVisitor, (value ? 1 : 0));
  }

  private void loadValue(Object val) {
    methodVisitor.visitLdcInsn(val);
  }

  private String signature(String returnType, String... argsTypes) {
    StringBuilder sig = new StringBuilder("(");
    for (String arg : argsTypes) {
      sig.append(arg);
    }
    sig.append(")");
    sig.append(returnType);
    return sig.toString();
  }

  private void callBuilderMethod(String cls, String meth, String sig) {
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL, BUILDER + "$" + cls, meth, sig, false);
  }

  private void build(String cls, String output) {
    callBuilderMethod(cls, "build", signature(irType(output)));
  }

  private void callBuildingMethod(String cls, String meth, String... args) {
    methodVisitor.visitMethodInsn(INVOKEVIRTUAL,
        BUILDER + "$" + cls, meth,
        signature(builderType(cls), args), false);
  }

  private void callStaticBuildMethod(String meth, String sig) {
    methodVisitor.visitMethodInsn(INVOKESTATIC, BUILDER, meth, sig, false);
  }

  private void newIrNode(String className) {
    methodVisitor.visitTypeInsn(NEW, IR + className);
    methodVisitor.visitInsn(DUP);
  }

  private void initNode(String className, String... args) {
    methodVisitor.visitMethodInsn(INVOKESPECIAL, IR + className, "<init>",
        signature("V", args), false);
  }

  @Override
  public void visitModule(GoloModule module) {
    // NOTE: should we?
    throw new IllegalArgumentException("modules can't be quoted");
  }

  @Override
  public void visitFunction(GoloFunction function) {
    /* NOTE: @yloiseau 2015-04-04
     * currently only block level statements can be quoted. Bigger changes in the parser a required
     * to quote functions
     */
    throw new IllegalArgumentException("functions can't be quoted yet, sorry");
  }

  @Override
  public void visitDecorator(Decorator decorator) {
    /* NOTE: @yloiseau 2015-04-04
     * currently only block level statements can be quoted. Bigger changes in the parser a required
     * to quote functions
     */
    throw new IllegalArgumentException("decorators can't be quoted yet, sorry");
  }

  @Override
  public void visitBlock(Block block) {
    // TODO: deal with reference table ?
    callStaticBuildMethod("block", signature(builderType("BlockBuilder")));
    for (GoloStatement statement : block.getStatements()) {
      statement.accept(this);
      callBuildingMethod("BlockBuilder", "add", OBJECT);
    }
  }

  @Override
  public void visitQuotedBlock(QuotedBlock qblock) {
    qblock.getExpression().accept(this);
    callStaticBuildMethod("quoted", signature(irType("QuotedBlock"), builderType("BlockBuilder")));
  }

  @Override
  public void visitConstantStatement(ConstantStatement constantStatement) {
    parent.visitConstantStatement(constantStatement);
    callStaticBuildMethod("constant", signature(irType("ConstantStatement"), OBJECT));
  }

  @Override
  public void visitReturnStatement(ReturnStatement returnStatement) {
    GoloStatement expr = returnStatement.getExpressionStatement();
    if (returnStatement.isReturningVoid()) {
      callStaticBuildMethod("returnsVoid", signature(irType("ReturnStatement")));
    } else {
      if (expr == null) {
        methodVisitor.visitInsn(ACONST_NULL);
      } else {
        expr.accept(this);
      }
      callStaticBuildMethod("returns",
          signature(irType("ReturnStatement"), irType("ExpressionStatement")));
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    callStaticBuildMethod("functionInvocation", signature(builderType("FunctionInvocationBuilder")));
    loadBoolean(functionInvocation.isOnReference());
    callBuildingMethod("FunctionInvocationBuilder", "onReference", "Z");
    loadBoolean(functionInvocation.isOnModuleState());
    callBuildingMethod("FunctionInvocationBuilder", "onModuleState", "Z");
    loadBoolean(functionInvocation.isConstant());
    callBuildingMethod("FunctionInvocationBuilder", "constant", "Z");
    loadValue(functionInvocation.getName());
    callBuildingMethod("FunctionInvocationBuilder", "name", STRING);
    for (ExpressionStatement arg : functionInvocation.getArguments()) {
      arg.accept(this);
      callBuildingMethod("FunctionInvocationBuilder", "arg", OBJECT);
    }
    for (FunctionInvocation inv : functionInvocation.getAnonymousFunctionInvocations()) {
      inv.accept(this);
      callBuildingMethod("FunctionInvocationBuilder", "anon", irType("FunctionInvocationBuilder"));
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    callStaticBuildMethod("assignment", signature(builderType("AssignmentStatementBuilder")));
    loadLocalReference(assignmentStatement.getLocalReference());
    callBuildingMethod("AssignmentStatementBuilder", "localRef", builderType("LocalReferenceBuilder"));
    assignmentStatement.getExpressionStatement().accept(this);
    callBuildingMethod("AssignmentStatementBuilder", "expression", irType("ExpressionStatement"));
    loadBoolean(assignmentStatement.isDeclaring());
    callBuildingMethod("AssignmentStatementBuilder", "declaring", "Z");
  }

  private void loadLocalReference(LocalReference ref) {
    // TODO: create a new hygienic local reference
    asmEnum(ref.getKind());
    methodVisitor.visitLdcInsn(ref.getName());
    callStaticBuildMethod("localRef",
        signature(builderType("LocalReferenceBuilder"), irType("LocalReference$Kind"), STRING));
    loadBoolean(ref.isSynthetic());
    callBuildingMethod("LocalReferenceBuilder", "synthetic", "Z");
    loadInteger(methodVisitor, ref.getIndex());
    callBuildingMethod("LocalReferenceBuilder", "index", "I");
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    methodVisitor.visitLdcInsn(referenceLookup.getName());
    callStaticBuildMethod("refLookup", signature(irType("ReferenceLookup"), STRING));
  }

  @Override
  public void visitConditionalBranching(ConditionalBranching conditionalBranching) {
    callStaticBuildMethod("branch", signature(builderType("ConditionalBranchingBuilder")));
    conditionalBranching.getCondition().accept(this);
    callBuildingMethod("ConditionalBranchingBuilder", "condition", irType("ExpressionStatement"));
    conditionalBranching.getTrueBlock().accept(this);
    callBuildingMethod("ConditionalBranchingBuilder", "whenTrue", builderType("BlockBuilder"));
    if (conditionalBranching.hasFalseBlock()) {
      conditionalBranching.getFalseBlock().accept(this);
      callBuildingMethod("ConditionalBranchingBuilder", "whenFalse", builderType("BlockBuilder"));
    }
    if (conditionalBranching.hasElseConditionalBranching()) {
      conditionalBranching.getElseConditionalBranching().accept(this);
      callBuildingMethod("ConditionalBranchingBuilder", "elseBranch", irType("ConditionalBranching"));
    }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    asmEnum(binaryOperation.getType());
    binaryOperation.getLeftExpression().accept(this);
    binaryOperation.getRightExpression().accept(this);
    callStaticBuildMethod("binaryOperation",
        signature(irType("BinaryOperation"),
          rtType("OperatorType"), OBJECT, OBJECT
        ));
  }

  @Override
  public void visitUnaryOperation(UnaryOperation unaryOperation) {
    asmEnum(unaryOperation.getType());
    unaryOperation.getExpressionStatement().accept(this);
    callStaticBuildMethod("unaryOperation", signature(
      irType("UnaryOperation"),
      rtType("OperatorType"), irType("ExpressionStatement")
    ));
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    callStaticBuildMethod("loop", signature(builderType("LoopBuilder")));
    loopStatement.getConditionStatement().accept(this);
    callBuildingMethod("LoopBuilder", "condition", irType("ExpressionStatement"));
    loopStatement.getBlock().accept(this);
    callBuildingMethod("LoopBuilder", "block", builderType("BlockBuilder"));
    if (loopStatement.hasInitStatement()) {
      loopStatement.getInitStatement().accept(this);
      callBuildingMethod("LoopBuilder", "init", builderType("AssignmentStatementBuilder"));
    }
    if (loopStatement.hasPostStatement()) {
      loopStatement.getPostStatement().accept(this);
      callBuildingMethod("LoopBuilder", "post", OBJECT);
    }
  }

  @Override
  public void visitMethodInvocation(MethodInvocation methodInvocation) {
    methodVisitor.visitLdcInsn(methodInvocation.getName());
    callStaticBuildMethod("methodInvocation", signature(
          builderType("MethodInvocationBuilder"), STRING));
    loadBoolean(methodInvocation.isNullSafeGuarded());
    callBuildingMethod("MethodInvocationBuilder", "nullSafe", "Z");
    for (ExpressionStatement arg : methodInvocation.getArguments()) {
      arg.accept(this);
      callBuildingMethod("MethodInvocationBuilder", "arg", irType("ExpressionStatement"));
    }
    for (FunctionInvocation inv : methodInvocation.getAnonymousFunctionInvocations()) {
      inv.accept(this);
      callBuildingMethod("MethodInvocationBuilder", "anon", irType("FunctionInvocationBuilder"));
    }
  }

  @Override
  public void visitThrowStatement(ThrowStatement throwStatement) {
    throwStatement.getExpressionStatement().accept(this);
    callStaticBuildMethod("throwException", signature(
      irType("ThrowStatement"), irType("GoloStatement")
    ));
  }

  @Override
  public void visitTryCatchFinally(TryCatchFinally tryCatchFinally) {
    callStaticBuildMethod("tryCatchFinally", signature(builderType("TryCatchBuilder")));
    methodVisitor.visitLdcInsn(tryCatchFinally.getExceptionId());
    callBuildingMethod("TryCatchBuilder", "exception", STRING);
    tryCatchFinally.getTryBlock().accept(this);
    callBuildingMethod("TryCatchBuilder", "tryBlock", builderType("BlockBuilder"));
    if (tryCatchFinally.hasCatchBlock()) {
      tryCatchFinally.getCatchBlock().accept(this);
      callBuildingMethod("TryCatchBuilder", "catchBlock", builderType("BlockBuilder"));
    }
    if (tryCatchFinally.hasFinallyBlock()) {
      tryCatchFinally.getFinallyBlock().accept(this);
      callBuildingMethod("TryCatchBuilder", "finallyBlock", builderType("BlockBuilder"));
    }
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    /* NOTE: @yloiseau -- 2015-04-04
     * closures can't be currently quoted since the corresponding static method is generated before quoting,
     * and thus outside the quoted block. For macros, this means in the defining context instead of
     * the calling context. I haven't yet investigated how to fix this.
     */
    throw new IllegalArgumentException("closures can't be quoted yet, sorry");
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    asmEnum(loopBreakFlowStatement.getType());
    callStaticBuildMethod("loopExit", signature(
      builderType("LoopBreakBuilder"),
      irType("LoopBreakFlowStatement$Type")
    ));
  }

  @Override
  public void visitCollectionLiteral(CollectionLiteral collectionLiteral) {
    asmEnum(collectionLiteral.getType());
    loadInteger(methodVisitor, collectionLiteral.getExpressions().size());
    methodVisitor.visitTypeInsn(ANEWARRAY, IR + "ExpressionStatement");
    int i = 0;
    for (ExpressionStatement statement : collectionLiteral.getExpressions()) {
      methodVisitor.visitInsn(DUP);
      loadInteger(methodVisitor, i);
      statement.accept(this);
      methodVisitor.visitInsn(AASTORE);
      i = i + 1;
    }
    callStaticBuildMethod("collection", signature(
          irType("CollectionLiteral"), 
          irType("CollectionLiteral$Type"), "[" + irType("ExpressionStatement")));
  }

}
