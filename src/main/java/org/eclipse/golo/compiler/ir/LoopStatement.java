/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler.ir;

import java.util.Objects;
import java.util.*;

import static org.eclipse.golo.compiler.ir.Builders.*;

public final class LoopStatement extends GoloStatement<LoopStatement> implements BlockContainer, ReferencesHolder {

  private AssignmentStatement initStatement = null;
  private ExpressionStatement<?> conditionStatement;
  private GoloStatement<?> postStatement = null;
  private Block block = Block.emptyBlock();;

  LoopStatement() {
    super();
    this.setConditionStatement(constant(false));
  }

  protected LoopStatement self() { return this; }

  public LoopStatement init(Object assignment) {
    if (assignment instanceof AssignmentStatement) {
      setInitStatement((AssignmentStatement) assignment);
      return this;
    }
    throw cantConvert("assignment", assignment);
  }

  public LoopStatement condition(Object expression) {
    if (expression instanceof ExpressionStatement) {
      setConditionStatement((ExpressionStatement) expression);
      return this;
    }
    throw cantConvert("expression", expression);
  }

  public LoopStatement post(Object statement) {
    if (statement instanceof GoloStatement) {
      setPostStatement((GoloStatement) statement);
      return this;
    }
    throw cantConvert("statement", statement);
  }

  public LoopStatement block(Block innerBlock) {
    setBlock(innerBlock);
    return this;
  }

  public LoopStatement block(Object... statements) {
    return this.block(Builders.block(statements));
  }

  public boolean hasInitStatement() {
    return initStatement != null;
  }

  public AssignmentStatement getInitStatement() {
    return initStatement;
  }

  public void setInitStatement(AssignmentStatement init) {
    this.initStatement = makeParentOf(init);
  }

  public ExpressionStatement<?> getConditionStatement() {
    return conditionStatement;
  }

  public void setConditionStatement(ExpressionStatement<?> cond) {
    this.conditionStatement = (cond == null ? constant(false) : cond);
    makeParentOf(this.conditionStatement);
  }

  public Block getBlock() {
    return block;
  }

  public void setBlock(Block block) {
    this.block = (block == null ? Block.emptyBlock() : block);
    makeParentOf(this.block);
  }

  public GoloStatement<?> getPostStatement() {
    return postStatement;
  }

  public void setPostStatement(GoloStatement<?> stat) {
    this.postStatement = makeParentOf(stat);
  }

  public boolean hasPostStatement() {
    return postStatement != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public LocalReference[] getReferences() {
    if (hasInitStatement()) {
      return new LocalReference[]{getInitStatement().getLocalReference()};
    }
    return new LocalReference[0];
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getReferencesCount() {
    return hasInitStatement() ? 1 : 0;
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitLoopStatement(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    if (initStatement != null) {
      initStatement.accept(visitor);
    }
    conditionStatement.accept(visitor);
    if (postStatement != null) {
      postStatement.accept(visitor);
    }
    block.accept(visitor);
  }

  @Override
  protected void replaceElement(GoloElement<?> original, GoloElement<?> newElement) {
    if (Objects.equals(initStatement, original)) {
      init(newElement);
    } else if (Objects.equals(conditionStatement, original)) {
      condition(newElement);
    } else if (Objects.equals(postStatement, original)) {
      post(newElement);
    } else if (Objects.equals(block, original)) {
      block(Block.of(newElement));
    } else {
      throw cantReplace(original, newElement);
    }
  }
}
