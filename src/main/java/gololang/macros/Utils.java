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

package gololang.macros;

import fr.insalyon.citi.golo.compiler.ir.*;

public final class Utils {

  private Utils() { }

  public static void prettyPrint(Object node) {
    try {
      toGoloElement(node).accept(new IrTreeDumper());
    } catch (IllegalArgumentException e) {
      System.out.println(node);
    }
  }

  public static ExpressionStatement toExpression(Object expression) {
    if (expression == null) { return null; }
    if (expression instanceof ExpressionStatement) {
      return (ExpressionStatement) expression;
    } 
    if (expression instanceof CodeBuilder.IrNodeBuilder) {
      return (ExpressionStatement) ((CodeBuilder.IrNodeBuilder) expression).build();
    }
    throw new IllegalArgumentException(expression + " is not a ExpressionStatement nor a IrNodeBuilder");
  }

  public static GoloStatement toGoloStatement(Object statement) {
    if (statement == null) { return null; }
    if (statement instanceof GoloStatement) {
      return (GoloStatement) statement;
    } 
    if (statement instanceof CodeBuilder.IrNodeBuilder) {
      return (GoloStatement) ((CodeBuilder.IrNodeBuilder) statement).build();
    }
    throw new IllegalArgumentException(statement + " is not a GoloStatement nor a IrNodeBuilder");
  }

  public static GoloElement toGoloElement(Object element) {
    if (element == null) { return null; }
    if (element instanceof GoloElement) {
      return (GoloElement) element;
    } 
    if (element instanceof CodeBuilder.IrNodeBuilder) {
      return (GoloElement) ((CodeBuilder.IrNodeBuilder) element).build();
    }
    throw new IllegalArgumentException(element + " is not a GoloElement nor a IrNodeBuilder");

  }

  public static Block toBlock(Object block) {
    if (block == null) { return null; }
    if (block instanceof Block) {
      return (Block) block;
    } 
    if (block instanceof CodeBuilder.IrNodeBuilder) {
      return (Block) ((CodeBuilder.IrNodeBuilder) block).build();
    }
    throw new IllegalArgumentException(block + " is not a Block nor a IrNodeBuilder");
  }


}
