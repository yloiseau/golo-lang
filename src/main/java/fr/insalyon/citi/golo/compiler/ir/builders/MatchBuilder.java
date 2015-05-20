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

import fr.insalyon.citi.golo.compiler.ir.Block;

import static gololang.macros.CodeBuilder.*;
import static gololang.macros.SymbolGenerator.gensym;

public final class MatchBuilder implements IrNodeBuilder<Block> {

  private CaseBuilder caseBuilder = caseBranch();
  private LocalReferenceBuilder matchVar = localRef().variable().synthetic(true).name(gensym("match"));

  public MatchBuilder otherwiseValue(Object expression) {
    caseBuilder.otherwiseBlock(assign(expression));
    return this;
  }

  public MatchBuilder whenValue(Object condition, Object expression) {
    caseBuilder.whenCase(condition, assign(expression));
    return this;
  }

  private BlockBuilder assign(Object expression) {
    return block(assignment().localRef(matchVar).expression(expression));
  }

  @Override
  public Block build() {
    return block()
      .add(assignment(true, matchVar, constant(null)))
      .add(caseBuilder)
      .add(matchVar.lookup())
      .build();
  }
}

