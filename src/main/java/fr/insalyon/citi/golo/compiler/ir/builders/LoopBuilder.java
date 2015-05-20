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

package fr.insalyon.citi.golo.compiler.ir.builders;

import fr.insalyon.citi.golo.compiler.ir.AssignmentStatement;
import fr.insalyon.citi.golo.compiler.ir.Block;
import fr.insalyon.citi.golo.compiler.ir.ConstantStatement;
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import fr.insalyon.citi.golo.compiler.ir.GoloStatement;
import fr.insalyon.citi.golo.compiler.ir.LoopStatement;
import fr.insalyon.citi.golo.compiler.ir.ReferenceTable;

import static gololang.macros.Utils.toExpression;
import static gololang.macros.Utils.toGoloStatement;
import static gololang.macros.Utils.toBlock;

import gololang.macros.CodeBuilder;

import java.util.Deque;
import java.util.LinkedList;

public final class LoopBuilder implements IrNodeBuilder<LoopStatement> {
  private ExpressionStatement cond;
  private Block block;
  private AssignmentStatement init;
  private GoloStatement post;

  public LoopBuilder() {
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
      cond = new ConstantStatement(false);
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

  public LoopBuilder block(Object b) {
    if (b == null) {
      block = new Block(new ReferenceTable());
    } else {
      block = toBlock(b);
    }
    return this;
  }

  public LoopBuilder block(Object... statements) {
    return block(CodeBuilder.block(statements));
  }

  public LoopStatement build() {
    currentLoop.pollFirst();
    return new LoopStatement(init, cond, block, post);
  }
}
