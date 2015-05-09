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

package fr.insalyon.citi.golo.compiler.ir;

import fr.insalyon.citi.golo.runtime.OperatorType;

public class UnaryOperation extends ExpressionStatement {

  private final Operator operator;
  private ExpressionStatement expressionStatement;

  public UnaryOperation(Operator operator, ExpressionStatement expressionStatement) {
    super();
    this.operator = operator;
    this.expressionStatement = expressionStatement;
  }

  public ExpressionStatement getExpressionStatement() {
    return expressionStatement;
  }

  public void setExpressionStatement(ExpressionStatement expr) {
    expressionStatement = expr;
  }

  public OperatorType getType() {
    return (operator instanceof OperatorType ? (OperatorType) operator : null);
  }

  public Operator getOperator() {
    return operator;
  }

  public MacroOperator getMacro() {
    return (operator instanceof MacroOperator ? (MacroOperator) operator : null);
  }

  public boolean isMacroOperation() {
    return operator instanceof MacroOperator;
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitUnaryOperation(this);
  }

  @Override
  public void replaceElement(GoloElement original, GoloElement replacement) {
    if (expressionStatement == original) {
      expressionStatement = (ExpressionStatement) replacement;
    }
  }
}
