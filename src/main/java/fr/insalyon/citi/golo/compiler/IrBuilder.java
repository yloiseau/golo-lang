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

import fr.insalyon.citi.golo.compiler.ir.*;
import fr.insalyon.citi.golo.runtime.OperatorType;

import java.util.List;
import java.util.LinkedList;
import java.util.Deque;

import static java.util.Arrays.asList;

// TODO: move this in gololang.metaprog

public final class IrBuilder {

  public static interface IrNodeBuilder<T> {
    T build();
  }

  public static ExpressionStatement toExpression(Object expression) {
    if (expression == null) { return null; }
    if (expression instanceof GoloStatement) {
      return (ExpressionStatement) expression;
    } 
    if (expression instanceof IrNodeBuilder) {
      return (ExpressionStatement) ((IrNodeBuilder) expression).build();
    }
    throw new IllegalArgumentException(expression + " is not a ExpressionStatement nor a IrNodeBuilder");
  }

  public static GoloStatement toGoloStatement(Object statement) {
    if (statement == null) { return null; }
    if (statement instanceof GoloStatement) {
      return (GoloStatement) statement;
    } 
    if (statement instanceof IrNodeBuilder) {
      return (GoloStatement) ((IrNodeBuilder) statement).build();
    }
    throw new IllegalArgumentException(statement + " is not a GoloStatement nor a IrNodeBuilder");
  }

  public static Block toBlock(Object block) {
    if (block == null) { return null; }
    if (block instanceof Block) {
      return (Block) block;
    } 
    if (block instanceof IrNodeBuilder) {
      return (Block) ((IrNodeBuilder) block).build();
    }
    throw new IllegalArgumentException(block + " is not a Block nor a IrNodeBuilder");
  }

  public static final class BlockBuilder implements IrNodeBuilder<Block> {
    private ReferenceTable ref = new ReferenceTable();
    private final List<GoloStatement> statements = new LinkedList<>();

    public BlockBuilder ref(ReferenceTable rt) {
      this.ref = rt;
      return this;
    }

    public BlockBuilder add(Object statement) {
      statements.add(toGoloStatement(statement));
      return this;
    }

    public Block build() {
      Block block = new Block(ref);
      for (GoloStatement s : statements) {
        block.addStatement(s);
      }
      return block;
    }

  }

  public static final class LocalReferenceBuilder implements IrNodeBuilder<LocalReference> {
    private LocalReference.Kind kind;
    private String name;
    private boolean synthetic = false;
    private int index = -1;

    public LocalReferenceBuilder kind(LocalReference.Kind k) {
      this.kind = k;
      return this;
    }

    public LocalReferenceBuilder name(String n) {
      this.name = n;
      return this;
    }

    public LocalReferenceBuilder synthetic(boolean s) {
      this.synthetic = s;
      return this;
    }

    public LocalReferenceBuilder index(int i) {
      this.index = i;
      return this;
    }

    public LocalReference build() {
      LocalReference ref = new LocalReference(kind, name, synthetic);
      ref.setIndex(index);
      return ref;
    }
  }
  public static final class MethodInvocationBuilder implements IrNodeBuilder<MethodInvocation> {

    private String name;
    private boolean nullSafe = false;
    private final List<ExpressionStatement> args = new LinkedList<>();
    private final List<FunctionInvocation> anonCalls = new LinkedList<>();

    private MethodInvocationBuilder() { }

    public MethodInvocationBuilder name(String n) {
      name = n;
      return this;
    }

    public MethodInvocationBuilder nullSafe(boolean safe) {
      nullSafe = safe;
      return this;
    }

    public MethodInvocationBuilder arg(Object expression) {
      args.add(toExpression(expression));
      return this;
    }

    public MethodInvocationBuilder anon(FunctionInvocationBuilder inv) {
      anonCalls.add(inv.build());
      return this;
    }

    public MethodInvocation build() {
      MethodInvocation meth = new MethodInvocation(name);
      meth.setNullSafeGuarded(nullSafe);
      for (ExpressionStatement arg : args) {
        meth.addArgument(arg);
      }
      for (FunctionInvocation inv : anonCalls) {
        meth.addAnonymousFunctionInvocation(inv);
      }
      return meth;
    }
  }

  public static MethodInvocationBuilder methodInvocation(String name) {
    return new MethodInvocationBuilder().name(name);
  }

  public static MethodInvocationBuilder methodInvocation(String name, boolean safe) {
    return new MethodInvocationBuilder().name(name).nullSafe(safe);
  }

  public static final class FunctionInvocationBuilder implements IrNodeBuilder<FunctionInvocation> {

    private String name;
    private boolean onRef = false;
    private boolean onModule = false;
    private boolean constant = false;
    private final List<ExpressionStatement> args = new LinkedList<>();
    private final List<FunctionInvocation> anonCalls = new LinkedList<>();

    public FunctionInvocationBuilder onReference(boolean v) {
      onRef = v;
      return this;
    }

    public FunctionInvocationBuilder onModuleState(boolean v) {
      onModule = v;
      return this;
    }

    public FunctionInvocationBuilder constant(boolean v) {
      constant = v;
      return this;
    }

    public FunctionInvocationBuilder name(String n) {
      name = n;
      return this;
    }

    public FunctionInvocationBuilder arg(Object expression) {
      args.add(toExpression(expression));
      return this;
    }

    public FunctionInvocationBuilder anon(FunctionInvocationBuilder inv) {
      anonCalls.add(inv.build());
      return this;
    }

    public FunctionInvocation build() {
      FunctionInvocation func;
      if (name == null || "".equals(name) || "anonymous".equals(name)) {
        func = new FunctionInvocation();
      } else {
        func = new FunctionInvocation(name);
      }
      func.setOnReference(onRef);
      func.setOnModuleState(onModule);
      func.setConstant(constant);
      for (ExpressionStatement arg : args) {
        func.addArgument(arg);
      }
      for (FunctionInvocation inv : anonCalls) {
        func.addAnonymousFunctionInvocation(inv);
      }
      return func;
    }
  }

  public static final class ConditionalBranchingBuilder implements IrNodeBuilder<ConditionalBranching> {

    private ExpressionStatement condition = constant(false);
    private Block trueBlock = new Block(new ReferenceTable());
    private ConditionalBranching elseConditionalBranching;
    private Block falseBlock;

    public ConditionalBranchingBuilder condition(Object cond) {
      if (cond == null) {
        condition = constant(false);
      } else {
        condition = toExpression(cond);
      }
      return this;
    }

    public ConditionalBranchingBuilder whenTrue(BlockBuilder block) {
      if (block == null) {
        trueBlock = new Block(new ReferenceTable());
      } else {
        trueBlock = block.build();
      }
      return this;
    }

    public ConditionalBranchingBuilder whenFalse(BlockBuilder block) {
      if (block == null) {
        falseBlock = null;
      } else {
        falseBlock = block.build();
      }
      return this;
    }

    public ConditionalBranchingBuilder elseBranch(ConditionalBranchingBuilder branch) {
      if (branch == null) {
        elseConditionalBranching = null;
      } else {
        elseConditionalBranching = branch.build();
      }
      return this;
    }

    public ConditionalBranching build() {
      if (elseConditionalBranching != null) {
        return new ConditionalBranching(condition, trueBlock, elseConditionalBranching);
      }
      return new ConditionalBranching(condition, trueBlock, falseBlock);
    }
  }

  public static final class LoopBuilder implements IrNodeBuilder<LoopStatement> {
    private ExpressionStatement cond;
    private Block block;
    private AssignmentStatement init;
    private GoloStatement post;

    LoopBuilder() {
      this.condition(null);
      this.block((BlockBuilder) null);
    }

    private static final Deque<LoopBuilder> currentLoop = new LinkedList<>();;

    public static LoopBuilder currentLoop() {
      return currentLoop.peekFirst();
    }
    
    public static void currentLoop(LoopBuilder l) {
      currentLoop.addFirst(l);
    }

    public LoopBuilder init(AssignmentStatementBuilder s) {
      if (s == null) { 
        init = null;
      } else {
        init = s.build();
      }
      return this;
    }

    public LoopBuilder condition(Object s) {
      if (s == null) { 
        cond = constant(false);
      } else 
        cond = toExpression(s);
      return this;
    }

    public LoopBuilder post(Object s) {
      if (s == null) {
        post = null; 
      } else {
        post = toGoloStatement(s);
      }
      return this;
    }

    public LoopBuilder block(BlockBuilder b) {
      if (b == null) {
        block = new Block(new ReferenceTable());
      } else {
        block = b.build();
      }
      return this;
    }

    public LoopBuilder block(ReferenceTable rt, Object... statements) {
      return block(IrBuilder.block(statements).ref(rt));
    }

    public LoopBuilder block(Object... statements) {
      return block(IrBuilder.block(statements));
    }

    public LoopStatement build() {
      currentLoop.pollFirst();
      return new LoopStatement(init, cond, block, post);
    }
  }

  public static LoopBuilder loop() {
    return new LoopBuilder();
  }

  public static LoopBuilder loop(AssignmentStatementBuilder init, 
                                 Object condition, 
                                 Object post, 
                                 BlockBuilder block) {
    return loop().init(init).condition(condition).post(post).block(block);
  }

  public static final class AssignmentStatementBuilder implements IrNodeBuilder<AssignmentStatement> {
    private LocalReference ref;
    private ExpressionStatement expr;
    private boolean declaring = false;

    public AssignmentStatementBuilder localRef(LocalReferenceBuilder r) {
      ref = r.build();
      return this;
    }

    public AssignmentStatementBuilder expression(Object e) {
      expr = toExpression(e);
      return this;
    }

    public AssignmentStatementBuilder declaring(boolean d) {
      declaring = d;
      return this;
    }

    public AssignmentStatement build() {
      AssignmentStatement as = new AssignmentStatement(ref, expr);
      as.setDeclaring(declaring);
      return as;
    }
  }

  public static AssignmentStatementBuilder assignment() {
    return new AssignmentStatementBuilder();
  }

  public static AssignmentStatementBuilder assignment(boolean declaring, LocalReferenceBuilder ref, Object expr) {
    return assignment().declaring(declaring).expression(expr).localRef(ref);
  }

  public static final class BinaryOperationBuilder implements IrNodeBuilder<BinaryOperation> {
    private OperatorType type;
    private ExpressionStatement left;
    private ExpressionStatement right;

    public BinaryOperationBuilder type(OperatorType type) {
      this.type = type;
      return this;
    }

    public BinaryOperationBuilder left(Object left) {
      this.left = toExpression(left);
      return this;
    }

    public ExpressionStatement left() {
      return left;
    }

    public BinaryOperationBuilder right(Object right) {
      this.right = toExpression(right);
      return this;
    }

    public ExpressionStatement right() {
      return right;
    }

    public BinaryOperation build() {
      if (type == null || left == null || right == null) {
        throw new IllegalStateException("builder not initialized");
      }
      return new BinaryOperation(type, left, right);
    }
  }

  public static BinaryOperation binaryOperation(OperatorType type, Object left, Object right) {
    return new BinaryOperationBuilder().type(type).left(left).right(right).build();
  }

  public static BinaryOperationBuilder binaryOperation(OperatorType type) {
    return new BinaryOperationBuilder().type(type);
  }

  public static BlockBuilder block() {
    return new BlockBuilder();
  }

  public static BlockBuilder block(Object... statements) {
    BlockBuilder block = new BlockBuilder();
    for (Object st : statements) {
      block.add(st);
    }
    return block;
  }

  public static QuotedBlock quoted(Object expr) {
    return new QuotedBlock(toExpression(expr));
  }

  public static ConstantStatement constant(Object value) {
    return new ConstantStatement(value);
  }

  public static ReturnStatement returns(Object expr) {
    return new ReturnStatement(toExpression(expr));
  }

  public static LocalReferenceBuilder localRef(LocalReference.Kind kind, String name) {
    return new LocalReferenceBuilder().kind(kind).name(name);
  }

  public static LocalReferenceBuilder localRef() {
    return new LocalReferenceBuilder();
  }

  public static LocalReferenceBuilder localRef(LocalReference.Kind kind, String name, int index, boolean synthetic) {
    return localRef(kind, name).index(index).synthetic(synthetic);
  }


  public static ReturnStatement returnsVoid() {
    ReturnStatement ret = new ReturnStatement(null);
    ret.returningVoid();
    return ret;
  }

  public static ReferenceLookup refLookup(String name) {
    return new ReferenceLookup(name);
  }

  public static FunctionInvocationBuilder functionInvocation() {
    return new FunctionInvocationBuilder();
  }

  public static FunctionInvocationBuilder functionInvocation(String name, boolean onRef, boolean onModule, boolean constant) {
    return functionInvocation().name(name).onReference(onRef).onModuleState(onModule).constant(constant);
  }

  public static ConditionalBranchingBuilder branch() {
    return new ConditionalBranchingBuilder();
  }

  public static ConditionalBranchingBuilder branch(Object condition, 
                                                   BlockBuilder trueBlock, BlockBuilder falseBlock,
                                                   ConditionalBranchingBuilder elseBranch) {
    return branch().condition(condition).whenTrue(trueBlock).whenFalse(falseBlock).elseBranch(elseBranch);
  }

  public static UnaryOperation unaryOperation(OperatorType type, ExpressionStatement expr) {
    return new UnaryOperation(type, expr);
  }

  public static CollectionLiteral collection(CollectionLiteral.Type type, ExpressionStatement... values) {
    return new CollectionLiteral(type, asList(values));
  }

  public static ThrowStatement throwException(Object expr) {
    return new ThrowStatement(toExpression(expr));
  }

  public static final class LoopBreakBuilder implements IrNodeBuilder<LoopBreakFlowStatement> {
    private LoopBreakFlowStatement.Type type;
    private LoopStatement enclosingLoop = null;
    private LoopBuilder enclosingLoopBuilder = null;

    public LoopBreakBuilder type(LoopBreakFlowStatement.Type t) {
      type = t;
      return this;
    }

    public LoopBreakBuilder loop(LoopStatement l) {
      enclosingLoop = l;
      return this;
    }

    public LoopBreakBuilder loop(LoopBuilder l) {
      enclosingLoopBuilder = l;
      return this;
    }

    public LoopBreakFlowStatement build() {
      LoopBreakFlowStatement st;
      switch (type) {
        case CONTINUE:
          st = LoopBreakFlowStatement.newContinue();
          break;
        case BREAK:
          st = LoopBreakFlowStatement.newBreak();
          break;
        default:
          throw new IllegalStateException("Unknown break type");
      }
      if (enclosingLoop != null) {
        st.setEnclosingLoop(enclosingLoop);
      } else if (enclosingLoopBuilder != null) {
        st.setEnclosingLoop(enclosingLoopBuilder.build());
      }
      return st;
    }
  }

  public static LoopBreakBuilder loopExit(LoopBreakFlowStatement.Type type) {
    return new LoopBreakBuilder().type(type).loop(LoopBuilder.currentLoop());
  }

  public static LoopBreakBuilder loopBreak() {
    return loopExit(LoopBreakFlowStatement.Type.BREAK);
  }

  public static LoopBreakBuilder loopContinue() {
    return loopExit(LoopBreakFlowStatement.Type.CONTINUE);
  }

  public static final class TryCatchBuilder implements IrNodeBuilder<TryCatchFinally> {
    private String exceptionId;
    private Object tryBlock;
    private Object catchBlock;
    private Object finallyBlock;

    public TryCatchBuilder exception(String id) {
      this.exceptionId = id;
      return this;
    }

    public TryCatchBuilder tryBlock(Object b) {
      this.tryBlock = b;
      return this;
    }

    public TryCatchBuilder catchBlock(Object b) {
      this.catchBlock = b;
      return this;
    }

    public TryCatchBuilder finallyBlock(Object b) {
      this.finallyBlock = b;
      return this;
    }

    public TryCatchFinally build() {
      return new TryCatchFinally(exceptionId, 
          toBlock(tryBlock), 
          toBlock(catchBlock),
          toBlock(finallyBlock));
    }
  }

  public static TryCatchBuilder tryCatchFinally() {
    return new TryCatchBuilder();
  }

  public static TryCatchBuilder tryCatchFinally(String exception, Object tryBlock, Object catchBlock, Object finallyBlock) {
    return tryCatchFinally()
      .exception(exception)
      .tryBlock(tryBlock)
      .catchBlock(catchBlock)
      .finallyBlock(finallyBlock);
  }
}
