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

import java.util.*;

import static java.util.Collections.unmodifiableList;
import static org.eclipse.golo.compiler.ir.Builders.*;
import static java.util.Objects.requireNonNull;

public final class Block extends ExpressionStatement<Block> {

  private final LinkedList<GoloStatement<?>> statements = new LinkedList<>();
  private ReferenceTable referenceTable;
  private boolean hasReturn = false;

  Block(ReferenceTable referenceTable) {
    super();
    this.referenceTable = referenceTable;
  }

  protected Block self() { return this; }

  public static Block emptyBlock() {
    return new Block(new ReferenceTable());
  }

  public static Block of(Object block) {
    if (block == null) {
      return emptyBlock();
    }
    if (block instanceof Block) {
      return (Block) block;
    }
    if (block instanceof GoloStatement<?>) {
      return emptyBlock().add(block);
    }
    throw cantConvert("Block", block);
  }

  public void merge(Block other) {
    for (GoloStatement<?> innerStatement : other.getStatements()) {
      this.addStatement(innerStatement);
    }
  }

  public ReferenceTable getReferenceTable() {
    return referenceTable;
  }

  @Override
  public Optional<ReferenceTable> getLocalReferenceTable() {
    return Optional.of(referenceTable);
  }

  public Block ref(Object referenceTable) {
    if (referenceTable instanceof ReferenceTable) {
      setReferenceTable((ReferenceTable) referenceTable);
      return this;
    }
    throw new IllegalArgumentException("not a reference table");
  }

  public void setReferenceTable(ReferenceTable referenceTable) {
    this.referenceTable = requireNonNull(referenceTable);
  }

  public void internReferenceTable() {
    this.referenceTable = referenceTable.flatDeepCopy(true);
  }

  public List<GoloStatement<?>> getStatements() {
    return unmodifiableList(statements);
  }

  public GoloStatement<?> getLastStatement() {
    for (Iterator<GoloStatement<?>> it = statements.descendingIterator(); it.hasNext();) {
      GoloStatement<?> st = it.next();
      if (st instanceof Noop) {
        continue;
      }
      return st;
    }
    return null;
  }

  public Block add(Object statement) {
    this.addStatement(toGoloStatement(statement));
    return this;
  }

  private void updateStateWith(GoloStatement<?> statement) {
    referenceTable.updateFrom(statement);
    makeParentOf(statement);
    checkForReturns(statement);
  }

  public void addStatement(GoloStatement<?> statement) {
    if (isUnaryBlock(statement)) {
      addStatement(((Block) statement).statements.get(0));
    } else {
      statements.add(statement);
      updateStateWith(statement);
    }
  }

  public void prependStatement(GoloStatement<?> statement) {
    statements.add(0, statement);
    updateStateWith(statement);
  }

  private void setStatement(int idx, GoloStatement<?> statement) {
    if (isUnaryBlock(statement)) {
      setStatement(idx, ((Block) statement).statements.get(0));
    } else {
      referenceTable.removeFrom(statements.get(idx));
      statements.set(idx, statement);
      updateStateWith(statement);
    }
  }

  public static boolean isUnaryBlock(GoloStatement<?> statement) {
    return statement instanceof Block && ((Block) statement).statements.size() == 1;
  }

  private void checkForReturns(GoloStatement<?> statement) {
    if (statement instanceof ReturnStatement || statement instanceof ThrowStatement) {
      hasReturn = true;
    } else if (statement instanceof ConditionalBranching) {
      hasReturn = hasReturn || ((ConditionalBranching) statement).returnsFromBothBranches();
    }
  }

  public boolean hasReturn() {
    return hasReturn;
  }

  public int size() {
    return statements.size();
  }

  public boolean hasOnlyReturn() {
    return statements.size() == 1
           && statements.get(0) instanceof ReturnStatement
           && !((ReturnStatement) statements.get(0)).isReturningVoid();
  }

  @Override
  public String toString() {
    return "{" + statements.toString() + "}";
  }

  public boolean isEmpty() {
    return statements.isEmpty();
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitBlock(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    for (LocalReference ref : referenceTable.ownedReferences()) {
      ref.accept(visitor);
    }
    for (GoloStatement<?> statement : statements) {
      statement.accept(visitor);
    }
  }

  @Override
  protected void replaceElement(GoloElement<?> original, GoloElement<?> newElement) {
    if (statements.contains(original) && newElement instanceof GoloStatement) {
      setStatement(statements.indexOf(original), (GoloStatement) newElement);
    } else {
      throw cantReplace(original, newElement);
    }
  }

  @Override
  protected GoloElement previousSiblingOf(GoloElement current) {
    int idx = statements.indexOf(current);
    if (idx < 1) {
      return null;
    }
    return statements.get(idx - 1);
  }

  @Override
  protected GoloElement nextSiblingOf(GoloElement current) {
    int idx = statements.indexOf(current);
    if (idx == -1 || idx == statements.size() - 1) {
      return null;
    }
    return statements.get(idx + 1);
  }

}
