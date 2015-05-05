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
import java.util.List;
import java.util.Collection;
import java.util.LinkedList;
import java.lang.invoke.MethodHandle;

import static java.util.Arrays.asList;
import static java.lang.invoke.MethodHandles.publicLookup;
import static java.lang.reflect.Modifier.isStatic;
import static java.lang.invoke.MethodType.genericMethodType;
import static gololang.macros.CodeBuilder.quoted;
import static gololang.macros.CodeBuilder.block;
import static gololang.macros.Utils.*;

// TODO: use exception builder instead of throwing exceptions if user error
// TODO: add a cache of the found macros ?
// TODO: non FQN macro finder:
// [x] current module
// [x] imported modules
// [x] special macro
// [~] compiler option


/**
 * Visitor to expand macro calls.
 * <p>
 * This visitor replace the {@code MacroInvocation} nodes with the result of the macro
 * expansion.
 * <p>
 * Macros functions are looked up in the current module, in modules imported by the current one, in modules specified
 * via the {@code use} special macro and in modules in {@code globalMacroClasses}, in that order.
 */
public class MacroExpansionIrVisitor extends DummyIrVisitor {

  private static final String MACROCLASS = ".Macros";
  public static final List<String> SPECIAL = java.util.Arrays.asList("use");
  private Deque<GoloElement> elements = new LinkedList<>();
  private Deque<Block> blockStack = new LinkedList<>();
  private boolean recur = true;
  private List<String> macroClasses = new LinkedList<>();
  private List<String> globalMacroClasses = new LinkedList<>(asList(
    "gololang.macros.Predefined"
  ));

  public void addGlobalMacroClass(String name) {
    globalMacroClasses.add(name);
  }

  public void addGlobalMacroClasses(Collection<String> names) {
    globalMacroClasses.addAll(names);
  }

  @Override
  public void visitBlock(Block block) {
    elements.push(block);
    blockStack.push(block);
    super.visitBlock(block);
    blockStack.pop();
    elements.pop();
  }

  @Override
  public void visitModule(GoloModule module) {
    addImportsToMacros(module);
    elements.push(module);
    super.visitModule(module);
    elements.pop();
    module.internStructAugmentations();
  }

  private void addImportsToMacros(GoloModule module) {
    macroClasses.clear();
    macroClasses.addAll(globalMacroClasses);
    for (ModuleImport mod : module.getImports()) {
      macroClasses.add(0, mod.getPackageAndClass().toString());
      macroClasses.add(0, mod.getPackageAndClass().toString() + MACROCLASS);
    }
    macroClasses.add(0, module.getPackageAndClass().toString()  + MACROCLASS);
    macroClasses.add(0, module.getPackageAndClass().toString());
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
      if (recur) {
        expanded.accept(this);
      }
      relinkReferenceTables(expanded, blockStack.peek());
    }
    expanded.replaceInParent(macroInvocation, elements.peek());
  }

  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    elements.push(assignmentStatement);
    super.visitAssignmentStatement(assignmentStatement);
    elements.pop();
  }

  private boolean isSpecial(MacroInvocation invocation) {
    return SPECIAL.contains(invocation.getName());
  }

  private GoloElement expandSpecial(MacroInvocation invocation) {
    switch (invocation.getName()) {
      case "use" :
        return useMacro(invocation.getArguments());
    }
    return null;
  }

  private GoloElement useMacro(List<ExpressionStatement> args) {
    for (ExpressionStatement arg : args) {
      macroClasses.add(0, ((ConstantStatement) arg).getValue().toString());
    }
    return null;
  }

  private GoloElement expand(MacroInvocation invocation) {
    GoloElement result = null;
    if (isSpecial(invocation)) {
      result = expandSpecial(invocation);
    } else {
      MethodHandle macro = findMacro(invocation.getName(), invocation.getArity());
      try {
        result = toGoloElement(macro.invokeWithArguments(invocation.getArguments()));
      } catch (Throwable t) {
        throw new RuntimeException("failed to call macro: " + invocation.getName(), t);
      }
    }
    if (result == null) {
      result = block().build();
    }
    if (invocation.hasASTNode()) {
      result.setASTNode(invocation.getASTNode());
    }
    return result;
  }

  private MethodHandle findMacro(String name, int arity) {
    String methodName = name;
    List<String> classNames = new LinkedList<>();
    int methodClassSeparatorIndex = name.lastIndexOf(".");
    if (methodClassSeparatorIndex >= 0) {
      String prefix = name.substring(0, methodClassSeparatorIndex);
      classNames.add(prefix);
      if (!prefix.endsWith(MACROCLASS)) {
        classNames.add(prefix + MACROCLASS);
      }
      methodName = name.substring(methodClassSeparatorIndex + 1);
    }
    classNames.addAll(macroClasses);
    for (String className : classNames) {
      try {
        return publicLookup().findStatic(
            Class.forName(className),
            methodName,
            genericMethodType(arity));
      } catch (Throwable ignored) {
        continue;
      }
    }
    throw new RuntimeException("failed to load macro: " + name);
  }
}
