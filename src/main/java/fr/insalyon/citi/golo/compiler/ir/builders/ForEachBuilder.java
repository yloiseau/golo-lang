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
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import fr.insalyon.citi.golo.compiler.ir.BinaryOperation;

import static gololang.macros.Utils.toBlock;
import static gololang.macros.Utils.toExpression;

import gololang.macros.CodeBuilder;
import static gololang.macros.CodeBuilder.*;
import static gololang.macros.SymbolGenerator.gensym;

public final class ForEachBuilder implements IrNodeBuilder<Block> {

  private LocalReferenceBuilder elementVar = localRef().variable();
  private LocalReferenceBuilder iteratorVar = localRef().variable().synthetic(true).name(gensym("iterator"));
  private Block block;
  private ExpressionStatement iterable;

  public ForEachBuilder variable(String name) {
    this.elementVar.name(name);
    return this;
  }

  public ForEachBuilder on(Object expression) {
    this.iterable = toExpression(expression);
    return this;
  }

  public ForEachBuilder block(Object block) {
    this.block = toBlock(block);
    return this;
  }

  private BinaryOperation nextCall() {
    return methodCall(this.iteratorVar.lookup(), methodInvocation("next"));
  }

  private BinaryOperation hasNextCall() {
    return methodCall(this.iteratorVar.lookup(), methodInvocation("hasNext"));
  }

  private BinaryOperation iteratorCall() {
    return methodCall(this.iterable, methodInvocation("iterator"));
  }

  @Override
  public Block build() {
    return CodeBuilder.block(loop()
        .init(assignment(true, iteratorVar, iteratorCall()))
        .condition(hasNextCall())
        .block(CodeBuilder.block()
          .add(assignment(true, elementVar, nextCall()))
          .add(this.block)
          )).build();
  }
}
