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

import fr.insalyon.citi.golo.compiler.ir.BinaryOperation;
import fr.insalyon.citi.golo.compiler.ir.ExpressionStatement;
import static gololang.macros.Utils.toExpression;
import fr.insalyon.citi.golo.runtime.OperatorType;

public final class BinaryOperationBuilder implements IrNodeBuilder<BinaryOperation> {
  private OperatorType type;
  private ExpressionStatement left;
  private ExpressionStatement right;

  public BinaryOperationBuilder type(OperatorType type) {
    this.type = type;
    return this;
  }

  public BinaryOperationBuilder left(Object left) {
    this.left = toExpression(left);
    return this;
  }

  public ExpressionStatement left() {
    return left;
  }

  public BinaryOperationBuilder right(Object right) {
    this.right = toExpression(right);
    return this;
  }

  public ExpressionStatement right() {
    return right;
  }

  public BinaryOperation build() {
    if (type == null || left == null || right == null) {
      throw new IllegalStateException("builder not initialized");
    }
    return new BinaryOperation(type, left, right);
  }
}
