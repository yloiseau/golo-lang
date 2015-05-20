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

import fr.insalyon.citi.golo.compiler.ir.ConditionalBranching;
import fr.insalyon.citi.golo.compiler.ir.Block;
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import static gololang.macros.Utils.toBlock;
import static gololang.macros.Utils.toExpression;

import java.util.Deque;
import java.util.LinkedList;

public final class CaseBuilder implements IrNodeBuilder<ConditionalBranching> {
  private final Deque<ExpressionStatement> conditions = new LinkedList<>();
  private final Deque<Block> blocks = new LinkedList<>();
  private Block otherwiseBlock;

  public CaseBuilder otherwiseBlock(Object block) {
    otherwiseBlock = toBlock(block);
    return this;
  }

  public CaseBuilder whenCase(Object condition, Object block) {
    conditions.push(toExpression(condition));
    blocks.push(toBlock(block));
    return this;
  }

  @Override
  public ConditionalBranching build() {
    if (otherwiseBlock == null) { throw new IllegalStateException("No otherwise clause"); }
    if (conditions.isEmpty()) { throw new IllegalStateException("No when clause"); }
    ConditionalBranchingBuilder caseBranch = new ConditionalBranchingBuilder().whenFalse(otherwiseBlock);
    while (conditions.size() > 1) {
      caseBranch = new ConditionalBranchingBuilder().elseBranch(caseBranch.condition(conditions.pop()).whenTrue(blocks.pop()));
    }
    return caseBranch.condition(conditions.pop()).whenTrue(blocks.pop()).build();
  }
}
