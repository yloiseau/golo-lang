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

import fr.insalyon.citi.golo.compiler.ir.MethodInvocation;
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import fr.insalyon.citi.golo.compiler.ir.FunctionInvocation;

import static gololang.macros.Utils.toExpression;

public final class MethodInvocationBuilder implements IrNodeBuilder<MethodInvocation> {

  private String name;
  private boolean nullSafe = false;
  private final List<ExpressionStatement> args = new LinkedList<>();
  private final List<FunctionInvocation> anonCalls = new LinkedList<>();

  public MethodInvocationBuilder(String name) {
    this.name = name;
  }

  public MethodInvocationBuilder nullSafe(boolean safe) {
    nullSafe = safe;
    return this;
  }

  public MethodInvocationBuilder arg(Object expression) {
    args.add(toExpression(expression));
    return this;
  }

  public MethodInvocationBuilder anon(FunctionInvocationBuilder inv) {
    anonCalls.add(inv.build());
    return this;
  }

  public MethodInvocation build() {
    MethodInvocation meth = new MethodInvocation(name);
    meth.setNullSafeGuarded(nullSafe);
    for (ExpressionStatement arg : args) {
      meth.addArgument(arg);
    }
    for (FunctionInvocation inv : anonCalls) {
      meth.addAnonymousFunctionInvocation(inv);
    }
    return meth;
  }
}
