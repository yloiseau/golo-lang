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

import fr.insalyon.citi.golo.compiler.ir.GoloFunction;
import fr.insalyon.citi.golo.compiler.ir.Block;
import fr.insalyon.citi.golo.compiler.ir.ReturnStatement;
import fr.insalyon.citi.golo.compiler.ir.ConstantStatement;
import fr.insalyon.citi.golo.compiler.ir.ReferenceTable;
import fr.insalyon.citi.golo.compiler.ir.LocalReference;

import gololang.macros.CodeBuilder;

import java.util.List;
import java.util.LinkedList;
import static java.util.Arrays.asList;


public final class FunctionDeclarationBuilder implements IrNodeBuilder<GoloFunction> {
  private String name = "anonymous";
  private GoloFunction.Visibility visibility = GoloFunction.Visibility.PUBLIC ;
  private GoloFunction.Scope scope = GoloFunction.Scope.MODULE;
  private boolean macro = false;
  private final List<String> parameters = new LinkedList<>();
  private final List<String> syntheticParameters = new LinkedList<>();
  private Block block = null;
  private boolean varargs = false;

  @Override
  public GoloFunction build() {
    GoloFunction f = new GoloFunction(name, visibility, scope, macro);
    f.setParameterNames(parameters);
    f.setVarargs(varargs);
    if (block == null) {
      block = new Block(new ReferenceTable());
      block.addStatement(new ReturnStatement(new ConstantStatement(null)));
    }
    f.setBlock(block);
    ReferenceTable referenceTable = block.getReferenceTable();
    for (String parameter : parameters) {
      referenceTable.add(new LocalReference(LocalReference.Kind.CONSTANT, parameter));
    }
    if (!block.hasReturn()) {
      ReturnStatement missingReturnStatement = new ReturnStatement(new ConstantStatement(null));
      if (f.isMain()) {
        missingReturnStatement.returningVoid();
      }
      block.addStatement(missingReturnStatement);
    }
    return f;
  }

  public FunctionDeclarationBuilder name(String n) {
    name = n;
    return this;
  }

  public FunctionDeclarationBuilder macro(boolean m) {
    macro = m;
    return this;
  }

  public FunctionDeclarationBuilder visibility(GoloFunction.Visibility v) {
    visibility = v;
    return this;
  }

  public FunctionDeclarationBuilder inAugment() {
    scope = GoloFunction.Scope.AUGMENT;
    return this;
  }

  public FunctionDeclarationBuilder asClosure() {
    scope = GoloFunction.Scope.CLOSURE;
    return this;
  }

  public FunctionDeclarationBuilder inModule() {
    scope = GoloFunction.Scope.MODULE;
    return this;
  }

  public FunctionDeclarationBuilder param(String... params) {
    parameters.addAll(asList(params));
    return this;
  }

  public FunctionDeclarationBuilder block(Object... statements) {
    return block(CodeBuilder.block(statements));
  }

  public FunctionDeclarationBuilder block(BlockBuilder blockBuilder) {
    this.block = blockBuilder.build();
    return this;
  }

  public FunctionDeclarationBuilder varargs() {
    varargs = true;
    return this;
  }

  // TODO: synthetic params
  // TODO: synthetic - selfname
  // TODO: decorators

}

