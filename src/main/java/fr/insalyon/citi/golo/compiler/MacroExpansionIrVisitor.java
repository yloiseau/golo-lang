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

package fr.insalyon.citi.golo.compiler;

import fr.insalyon.citi.golo.compiler.ir.*;

import java.util.Deque;
import java.util.LinkedList;
import java.lang.invoke.MethodHandle;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.reflect.Modifier.isStatic;
import static java.lang.invoke.MethodType.genericMethodType;
import static gololang.macros.CodeBuilder.quoted;
import static gololang.macros.CodeBuilder.block;
import static gololang.macros.CodeBuilder.toGoloElement;

/**
 * Visitor to expand macro calls.
 * <p>
 * This visitor replace the {@code MacroInvocation} nodes with the result of the macro
 * expansion.
 */
public class MacroExpansionIrVisitor extends DummyIrVisitor {

  private Deque<GoloElement> elements = new LinkedList<>();

  @Override
  public void visitBlock(Block block) {
    elements.push(block);
    super.visitBlock(block);
    elements.pop();
  }

  @Override
  public void visitModule(GoloModule module) {
    elements.push(module);
    super.visitModule(module);
    elements.pop();
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    elements.push(functionInvocation);
    super.visitFunctionInvocation(functionInvocation);
    elements.pop();  
  }

  @Override
  public void visitMacroInvocation(MacroInvocation macroInvocation) {
    super.visitMacroInvocation(macroInvocation);
    GoloElement expanded = expand(macroInvocation);
    if (expanded != null) {
      expanded.accept(this);
      elements.peek().replaceElement(macroInvocation, expanded);
    }
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    elements.push(assignmentStatement);
    super.visitAssignmentStatement(assignmentStatement);
    elements.pop();
  }

  public static GoloElement expand(MacroInvocation invocation) {
    GoloElement result = block().build();
    String macroName = invocation.getName();
    int methodClassSeparatorIndex = macroName.lastIndexOf(".");
    if (methodClassSeparatorIndex >= 0) {
      String className = macroName.substring(0, methodClassSeparatorIndex);
      String methodName = macroName.substring(methodClassSeparatorIndex + 1);
      try {
        MethodHandle macro = publicLookup().findStatic(
            Class.forName(className),
            methodName,
            genericMethodType(invocation.getArity()));
        result = toGoloElement(macro.invokeWithArguments(invocation.getArguments()));
      } catch (Throwable t) {
        throw new RuntimeException("failed to call macro: " + invocation.getName(), t);
      }
    }
    if (result == null) { return block().build(); }
    if (invocation.hasASTNode()) {
      result.setASTNode(invocation.getASTNode());
    }
    return result;
  }
}
