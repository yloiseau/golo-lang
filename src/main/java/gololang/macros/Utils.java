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

// TODO: parse command str -> IR ?? how to do with incomplete code (just 1 expression)
// TODO: eval(ir, context) ? (cf. Dynamic Code Evaluation)

package gololang.macros;

import fr.insalyon.citi.golo.compiler.ir.*;
import fr.insalyon.citi.golo.compiler.parser.GoloParser;
import fr.insalyon.citi.golo.compiler.*;
import static gololang.macros.CodeBuilder.externalRef;

public final class Utils {

  private Utils() { }

  /**
   * Dumps a representation of the given node to System.out.
   * <p>
   * This function gives the same result as the {@code golo diagnose --tool ir} command.
   * It is mainly useful for debugging.
   * @param node The IR node to dump ({@link fr.insalyon.citi.golo.compiler.ir.GoloElement} or {@link gololang.macros.CodeBuilder.IrNodeBuilder}
   */
  public static void dump(Object node) {
    try {
      toGoloElement(node).accept(new IrTreeDumper());
    } catch (IllegalArgumentException e) {
      System.out.println(node);
    }
  }

  /**
   * Prints a golo code representation of the given node to System.out.
   *  <p>
   * This function is similar to the {@code golo diagnose --tool pp} command.
   * It is mainly useful for debugging.
   * @param node The IR node to dump ({@link fr.insalyon.citi.golo.compiler.ir.GoloElement} or {@link gololang.macros.CodeBuilder.IrNodeBuilder}
   */
  public static void prettyPrint(Object node) {
    try {
      GoloPrettyPrinter printer = new GoloPrettyPrinter(false);
      toGoloElement(node).accept(printer);
      System.out.println(printer.getBuffer());
    } catch (IllegalArgumentException e) {
      System.out.println(node);
    }
  }

  /**
   * Fully expand the macros found in the given IR node
   * @param node The IR node to expand
   * @return the same node expanded
   */
  public static GoloElement expand(Object node) {
    return expand(node, true);
  }

  /**
   * Expand the macros found in the given IR node, with only one level of expansion.
   * @param node The IR node to expand
   * @return the same node expanded
   */
  public static GoloElement expandOne(Object node) {
    return expand(node, false);
  }

  public static GoloElement expand(Object node, boolean recur) {
    GoloElement element = toGoloElement(node);
    MacroExpansionIrVisitor visitor = new MacroExpansionIrVisitor(recur);
    element.accept(visitor);
    return element;
  }

  private static String cantConvert(Object value, String target) {
    return String.format("%s is not a %s nor a IrNodeBuilder, but a %s",
        value, target, value.getClass());
  }

  static ExpressionStatement toExpression(Object expression) {
    if (expression == null) { return null; }
    if (expression instanceof ExpressionStatement) {
      return (ExpressionStatement) expression;
    } 
    if (expression instanceof CodeBuilder.IrNodeBuilder) {
      return (ExpressionStatement) ((CodeBuilder.IrNodeBuilder) expression).build();
    }
    throw new IllegalArgumentException(cantConvert(expression, "ExpressionStatement"));
  }

  static GoloStatement toGoloStatement(Object statement) {
    if (statement == null) { return null; }
    if (statement instanceof GoloStatement) {
      return (GoloStatement) statement;
    } 
    if (statement instanceof CodeBuilder.IrNodeBuilder) {
      return (GoloStatement) ((CodeBuilder.IrNodeBuilder) statement).build();
    }
    throw new IllegalArgumentException(cantConvert(statement, "GoloStatement"));
  }

  public static GoloElement toGoloElement(Object element) {
    if (element == null) { return null; }
    if (element instanceof GoloElement) {
      return (GoloElement) element;
    } 
    if (element instanceof CodeBuilder.IrNodeBuilder) {
      return (GoloElement) ((CodeBuilder.IrNodeBuilder) element).build();
    }
    throw new IllegalArgumentException(cantConvert(element, "GoloElement"));
  }

  static Block toBlock(Object block) {
    if (block == null) { return null; }
    if (block instanceof Block) {
      return (Block) block;
    } 
    if (block instanceof CodeBuilder.IrNodeBuilder) {
      return (Block) ((CodeBuilder.IrNodeBuilder) block).build();
    }
    throw new IllegalArgumentException(cantConvert(block, "Block"));
  }

  public static void relinkReferenceTables(GoloElement element, ReferenceTable table) {
    if (table != null && element instanceof Scope) {
      ((Scope) element).relink(table);
    }
  }

  public static void relinkReferenceTables(GoloElement element, Block outerBlock) {
    if (outerBlock != null) {
      relinkReferenceTables(element, outerBlock.getReferenceTable());
    }
  }

  static GoloParser.ParserClassRef toClassRef(Class<?> cls) {
    return toClassRef(cls.getCanonicalName());
  }

  static GoloParser.ParserClassRef toClassRef(String clsName) {
    return new GoloParser.ParserClassRef(clsName);
  }
}
