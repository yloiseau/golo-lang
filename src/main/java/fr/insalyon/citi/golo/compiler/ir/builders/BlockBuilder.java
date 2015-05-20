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

import java.util.List;
import java.util.LinkedList;

import fr.insalyon.citi.golo.compiler.ir.Block;
import fr.insalyon.citi.golo.compiler.ir.ReferenceTable;
import fr.insalyon.citi.golo.compiler.ir.GoloStatement;
import fr.insalyon.citi.golo.compiler.ir.AssignmentStatement;
import fr.insalyon.citi.golo.compiler.ir.LoopStatement;

import static gololang.macros.Utils.toGoloStatement;
import static gololang.macros.Utils.relinkReferenceTables;

public final class BlockBuilder implements IrNodeBuilder<Block> {
  private ReferenceTable ref = new ReferenceTable();
  private final List<GoloStatement> statements = new LinkedList<>();

  public BlockBuilder ref(ReferenceTable rt) {
    this.ref = rt;
    return this;
  }

  public BlockBuilder merge(Block block) {
    for (GoloStatement innerStatement : block.getStatements()) {
      this.add(innerStatement);
    }
    return this; 
  }

  private void updateRefs(GoloStatement statement) {
    if (statement instanceof AssignmentStatement) {
      AssignmentStatement assign = (AssignmentStatement) statement;
      if (assign.isDeclaring()) {
        ref.add(assign.getLocalReference());
      }
    }
    if (statement instanceof LoopStatement) {
      LoopStatement loop = (LoopStatement) statement;
      if (loop.hasInitStatement()) {
        ref.add(loop.getInitStatement().getLocalReference());
      }
    } 
  }

  public BlockBuilder add(Object statement) {
    GoloStatement stat = toGoloStatement(statement);
    updateRefs(stat);
    relinkReferenceTables(stat, ref);
    statements.add(stat);
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

