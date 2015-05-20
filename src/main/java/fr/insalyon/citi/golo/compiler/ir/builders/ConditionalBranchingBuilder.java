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
import fr.insalyon.citi.golo.compiler.ir.ConstantStatement;
import fr.insalyon.citi.golo.compiler.ir.ReferenceTable;

import static gololang.macros.Utils.toBlock;
import static gololang.macros.Utils.toExpression;

public final class ConditionalBranchingBuilder implements IrNodeBuilder<ConditionalBranching> {

  private ExpressionStatement condition = new ConstantStatement(false);
  private Block trueBlock = new Block(new ReferenceTable());
  private ConditionalBranching elseConditionalBranching;
  private Block falseBlock;

  public ConditionalBranchingBuilder condition(Object cond) {
    if (cond == null) {
      condition = new ConstantStatement(false);
    } else {
      condition = toExpression(cond);
    }
    return this;
  }

  public ConditionalBranchingBuilder whenTrue(Object block) {
    if (block == null) {
      trueBlock = new Block(new ReferenceTable());
    } else {
      trueBlock = toBlock(block);
    }
    return this;
  }

  public ConditionalBranchingBuilder whenFalse(Object block) {
    falseBlock = toBlock(block);
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

