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

import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import fr.insalyon.citi.golo.compiler.ir.FunctionInvocation;

import static gololang.macros.Utils.toExpression;

public final class FunctionInvocationBuilder implements IrNodeBuilder<FunctionInvocation> {

  private String name;
  private boolean onRef = false;
  private boolean onModule = false;
  private boolean constant = false;
  private final List<ExpressionStatement> args = new LinkedList<>();
  private final List<FunctionInvocation> anonCalls = new LinkedList<>();

  public FunctionInvocationBuilder onReference(boolean v) {
    onRef = v;
    return this;
  }

  public FunctionInvocationBuilder onModuleState(boolean v) {
    onModule = v;
    return this;
  }

  public FunctionInvocationBuilder constant(boolean v) {
    constant = v;
    return this;
  }

  public FunctionInvocationBuilder name(String n) {
    name = n;
    return this;
  }

  public FunctionInvocationBuilder arg(Object expression) {
    args.add(toExpression(expression));
    return this;
  }

  public FunctionInvocationBuilder anon(FunctionInvocationBuilder inv) {
    anonCalls.add(inv.build());
    return this;
  }

  public FunctionInvocation build() {
    FunctionInvocation func;
    if (name == null || "".equals(name) || "anonymous".equals(name)) {
      func = new FunctionInvocation();
    } else {
      func = new FunctionInvocation(name);
    }
    func.setOnReference(onRef);
    func.setOnModuleState(onModule);
    func.setConstant(constant);
    for (ExpressionStatement arg : args) {
      func.addArgument(arg);
    }
    for (FunctionInvocation inv : anonCalls) {
      func.addAnonymousFunctionInvocation(inv);
    }
    return func;
  }
}

