/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.golo.compiler;

import java.util.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import org.eclipse.golo.compiler.ir.*;
import org.eclipse.golo.runtime.OperatorSupport;

import static java.lang.invoke.MethodHandles.lookup;
import static java.util.stream.Collectors.toList;

// TODO: replace return block whose last statement is a lookup by a block whose last statement is the return (push down returns)
// TODO: flow analysis: if we return a lookup previously assign (without any statement in between) replace the
// assignment with the return directly (*very* useful for TCE)
// TODO: replace block with only "assignment local + lookup" by the expression
// TODO: remove unused constant assignment -> see LocalReferenceAssignment
//       only if the value is constant (i.e. side effect free!)
// [x]: replace refLookup to constant LocalRef with constant value by the value itself
//       -> stack of values mappings ?
// TODO: replace string concatenation by a StringBuilder
// TODO: remove statements directly following a return/break/continue/throw statement

/**
 * Visitor to apply some compile time optimisations.
 */
class OptimisationIrVisitor extends AbstractGoloIrVisitor {

  private static final MethodType UNOP_TYPE = MethodType.methodType(Object.class, Object.class);
  private static final MethodType BINOP_TYPE = MethodType.methodType(Object.class, Object.class, Object.class);
  private static boolean OPTIMIZE = Boolean.valueOf(System.getProperty("golo.optimize", "true"));
  private final Deque<Map<LocalReference, ReferenceUsage>> referenceUsages = new LinkedList<>();

  private final class ReferenceUsage {
    private LocalReference reference;
    private int count;
    private AssignmentStatement init;

    ReferenceUsage(AssignmentStatement assign) {
      init = assign;
      reference = assign.getLocalReference();
      count = 0;
    }

    public void add() {
      count++;
    }

    public void remove() {
      count--;
    }

    public AssignmentStatement assignment() {
      return init;
    }

    public ExpressionStatement<?> value() {
      return init.getExpressionStatement();
    }

    public boolean isConstant() {
      return reference.isConstant() && value().isConstant();
    }

    public boolean isUsed() {
      return count > 0;
    }

    public String toString() {
      return String.format("ReferenceUsage{ref=%s,init=%s,count=%s}", reference, init, count);
    }
  }

  private ReferenceUsage getReferenceUsage(LocalReference ref) {
    for (Map<LocalReference, ReferenceUsage> usages : referenceUsages) {
      ReferenceUsage ru = usages.get(ref);
      if (ru != null) {
        return ru;
      }
    }
    return null;
  }

  private void optimize(GoloElement<?> element, GoloElement<?> optimized, boolean recur) {
    if (optimized == null) {
      return;
    }
    if (OPTIMIZE) {
      element.replaceInParentBy(optimized);
      if (recur) {
        optimized.accept(this);
      }
    } else {
      System.err.format("[INFO] %s at %s could be replaced by %s%n",
          element, element.positionInSourceCode(), optimized);
    }
  }

  private static GoloElement<?> evalBinaryOperation(BinaryOperation binaryOperation) {
    if (!binaryOperation.isConstant()) {
      return null;
    }
    String name = binaryOperation.getType().name().toLowerCase();
    try {
      MethodHandle handle = OperatorSupport.bootstrap(lookup(), name, BINOP_TYPE, 2).dynamicInvoker();
      return Builders.constant(handle.invokeWithArguments(
          ((ConstantStatement) binaryOperation.getLeftExpression()).getValue(),
          ((ConstantStatement) binaryOperation.getRightExpression()).getValue()));
    } catch (Throwable ignored) {
      System.out.println("## Optimisation failed: " + ignored);
      return null;
    }
  }

  private static GoloElement<?> evalAndOperation(BinaryOperation binaryOperation) {
    ExpressionStatement<?> leftExpr = binaryOperation.getLeftExpression();
    if (leftExpr.isConstant()) {
      Boolean left = (Boolean) ((ConstantStatement) leftExpr).getValue();
      if (!left) {
        return Builders.constant(false);
      }
      return binaryOperation.getRightExpression();
    }
    return null;
  }

  private static GoloElement<?> evalOrOperation(BinaryOperation binaryOperation) {
    ExpressionStatement<?> leftExpr = binaryOperation.getLeftExpression();
    if (leftExpr.isConstant()) {
      Boolean left = (Boolean) ((ConstantStatement) leftExpr).getValue();
      if (left) {
        return Builders.constant(true);
      }
      return binaryOperation.getRightExpression();
    }
    return null;
  }

  private static GoloElement<?> evalOrIfNullOperation(BinaryOperation binaryOperation) {
    ExpressionStatement<?> leftExpr = binaryOperation.getLeftExpression();
    if (leftExpr.isConstant()) {
      Object left = ((ConstantStatement) leftExpr).getValue();
      if (left != null) {
        return leftExpr;
      }
      return binaryOperation.getRightExpression();
    }
    return null;
  }

  private void cleanUnusedConstantReferences() {
    System.out.println(referenceUsages.peek());
    for (ReferenceUsage ru : referenceUsages.peek().values()) {
      System.out.println("## " + ru);
      if (!ru.isUsed() && !ru.assignment().getLocalReference().isModuleState() && ru.value().isConstant()) {
        optimize(ru.assignment(), new Noop("Unused variable"), false);
      }
    }
  }

  private void removeUnreachableStatements(Block block) {
    boolean seenEOB = false;
    for (GoloStatement s : block.getStatements()) {
      if (seenEOB) {
        optimize(s, new Noop("Unreachable statement"), false);
      }
      seenEOB |= isEOB(s);
    }
  }

  private static boolean isEOB(GoloStatement s) {
    // TODO: does not identify a eob statement if deeply nested...
    return s instanceof EndOfBlock
      || (s instanceof ConditionalBranching && ((ConditionalBranching) s).returnsFromBothBranches())
      || (s instanceof LoopStatement && ((LoopStatement) s).getBlock().hasReturn())
      || (s instanceof Block && (
            ((Block) s).hasReturn()
            || ((Block) s).getStatements().stream().anyMatch(OptimisationIrVisitor::isEOB)));
  }

  private static Collection<GoloElement> getPreviousInFlow(GoloElement elt) {
    GoloElement prev = elt.getPreviousSibling();
    if (prev == null) {
      GoloElement parent = elt.parent();
      if (parent instanceof GoloFunction || parent == elt || parent == null) {
        return Collections.emptySet();
      }
      if (parent instanceof ConditionalBranching) {
        ConditionalBranching cond = (ConditionalBranching) parent;
        if (elt != cond.getCondition()) {
          return Collections.singleton(cond.getCondition());
        }
      }
      // TODO: deal with try/catch/finally
      return getPreviousInFlow(parent);
    }
    if (prev instanceof Noop) {
      return getPreviousInFlow(prev);
    }
    return Collections.singleton(prev);
  }

  private void pushDownReturn(ReturnStatement ret) {
    GoloStatement expr = ret.getExpressionStatement();
    if (expr instanceof Block && ((Block) expr).getLastStatement() instanceof ReferenceLookup) {
      ReferenceLookup last = (ReferenceLookup) ((Block) expr).getLastStatement();
      optimize(last, Builders.returns(Builders.refLookup(last.getName())), false);
      optimize(ret, expr, true);
    }
  }

  /**
   * Replace a return of a reference by the last assigned expression.
   * <p>
   * Only if it's not a module state.
   */
  private void pushUpReturn(ReturnStatement ret) {
    GoloStatement expr = ret.getExpressionStatement();
    if (expr instanceof ReferenceLookup) {
      ReferenceLookup ref = (ReferenceLookup) expr;
      if (!ref.resolve().map(LocalReference::isModuleState).orElse(true)) {
        for (GoloElement previous : getPreviousInFlow(ret)) {
          if (previous instanceof AssignmentStatement) {
            AssignmentStatement assignment = (AssignmentStatement) previous;
            if (assignment.getLocalReference().getName().equals(ref.getName())) {
              ref.resolve().flatMap(l -> Optional.ofNullable(getReferenceUsage(l))).ifPresent(u -> u.remove());
              optimize(ref, assignment.getExpressionStatement(), false);
            }
          }
        }
      }
    }
  }

  @Override
  public void visitModule(GoloModule module) {
    referenceUsages.push(new HashMap<>());
    GoloFunction initializer = module.getModuleInitializer();
    if (initializer != null) {
      initializer.getBlock().walk(this);
    }
    super.visitModule(module);
    referenceUsages.pop();
  }

  @Override
  public void visitBlock(Block block) {
    referenceUsages.push(new HashMap<>());
    block.walk(this);
    removeUnreachableStatements(block);
    cleanUnusedConstantReferences();
    referenceUsages.pop();
    // mergeSubBlock(block);
  }

  private void mergeSubBlock(Block block) {
    // FIXME: local references of parent block are lost
    List<GoloStatement> statements = block.getStatements().stream().filter(s -> !(s instanceof Noop)).collect(toList());
    if (statements.size() == 1 && statements.get(0) instanceof Block) {
      Block subBlock = (Block) statements.get(0);
      // subBlock.internReferenceTable();
      subBlock.getReferenceTable().relinkTopLevel(block.getReferenceTable());
      optimize(block, subBlock, false);
    }
  }

  @Override
  public void visitBinaryOperation(BinaryOperation binaryOperation) {
    super.visitBinaryOperation(binaryOperation);
    GoloElement<?> result = null;
    switch (binaryOperation.getType()) {
      case AND:
        result = evalAndOperation(binaryOperation);
        break;
      case OR:
        result = evalOrOperation(binaryOperation);
        break;
      case ORIFNULL:
        result = evalOrIfNullOperation(binaryOperation);
        break;
      default:
        result = evalBinaryOperation(binaryOperation);
    }
    optimize(binaryOperation, result, true);
  }

  /**
   * Optimize reference lookup.
   * <p>
   * If the reference is constant with a constant value, replace the lookup with the value itself.
   */
  @Override
  public void visitReferenceLookup(ReferenceLookup lookup) {
    lookup.walk(this);
    Optional<LocalReference> ref = lookup.resolve();
    if (ref.isPresent() && !referenceUsages.isEmpty()) {
      ReferenceUsage ru = getReferenceUsage(ref.get());
      if (ru == null) { return; }
      ru.add();
      if (ru.isConstant()) {
        optimize(lookup, ru.value(), false);
        ru.remove();
      }
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignment) {
    super.visitAssignmentStatement(assignment);
    if (assignment.isConstant()) {
      referenceUsages.peek().put(assignment.getLocalReference(), new ReferenceUsage(assignment));
    }
  }

  /**
   * Optimize conditional branching.
   * <p>
   * Removes the branches that are known to not be reachable.
   */
  @Override
  public void visitConditionalBranching(ConditionalBranching branch) {
    branch.walk(this);
    ExpressionStatement<?> condition = branch.getCondition();
    GoloElement<?> result = null;
    if (condition.isConstant()) {
      Boolean cond = (Boolean) ((ConstantStatement) condition).getValue();
      if (cond) {
        result = branch.getTrueBlock();
      } else if (branch.hasFalseBlock()) {
        result = branch.getFalseBlock();
      } else if (branch.hasElseConditionalBranching()) {
        result = branch.getElseConditionalBranching();
      } else {
        result = new Noop("optimized conditional");
      }
      optimize(branch, result, false);
    }
  }

  @Override
  public void visitReturnStatement(ReturnStatement ret) {
    ret.walk(this);
    pushDownReturn(ret);
    pushUpReturn(ret);
  }
}
