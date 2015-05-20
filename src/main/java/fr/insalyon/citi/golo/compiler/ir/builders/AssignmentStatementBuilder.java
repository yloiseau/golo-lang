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
import fr.insalyon.citi.golo.compiler.ir.LocalReference;
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;

import static gololang.macros.Utils.toExpression;

public final class AssignmentStatementBuilder implements IrNodeBuilder<AssignmentStatement> {
  private LocalReference ref;
  private ExpressionStatement expr;
  private boolean declaring = false;

  public AssignmentStatementBuilder localRef(Object r) {
    if (r instanceof LocalReference) {
      ref = (LocalReference) r;
    } else if (r instanceof LocalReferenceBuilder) {
      ref = ((LocalReferenceBuilder) r).build();
    } else {
      throw new IllegalArgumentException("invalid value for the local reference");
    }
    return this;
  }

  public AssignmentStatementBuilder expression(Object e) {
    expr = toExpression(e);
    return this;
  }

  public AssignmentStatementBuilder declaring(boolean d) {
    declaring = d;
    return this;
  }

  public AssignmentStatement build() {
    AssignmentStatement as = new AssignmentStatement(ref, expr);
    as.setDeclaring(declaring);
    return as;
  }
}

