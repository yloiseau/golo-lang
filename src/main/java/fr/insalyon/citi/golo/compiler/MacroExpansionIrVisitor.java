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

/**
 * Visitor to expand macro calls.
 * <p>
 * This visitor replace the {@code MacroInvocation} nodes with the result of the macro
 * expansion.
 * <p>
 * Macros functions are looked up in the current module, in modules imported by the current one, in modules specified
 * via the {@code use} special macro and in modules in {@code globalMacroClasses}, in that order.
 */
public final class MacroExpansionIrVisitor extends AbstractGoloIrVisitor {

  private static final String MACROCLASS = ".Macros";
  public static final List<String> SPECIAL = java.util.Arrays.asList("use");
  private Deque<GoloElement> elements = new LinkedList<>();
  private Deque<Block> blockStack = new LinkedList<>();
  private boolean recur = true;
  private List<String> macroClasses = new LinkedList<>();
  private static List<String> globalMacroClasses = new LinkedList<>(asList(
    "gololang.macros.Predefined"
  ));


  public MacroExpansionIrVisitor(boolean recur) {
    this.recur = recur;
    initLookUp(null);
  }

  public MacroExpansionIrVisitor() {
    this(true);
  }

  private static void addMacroClass(List<String> to, int index, String name) {
    to.add(index, name);
    if (!name.endsWith(MACROCLASS)) {
      to.add(index + 1, name + MACROCLASS);
    }
  }

  public static void addGlobalMacroClass(String name) {
    addMacroClass(globalMacroClasses, 0, name);
  }

  public static void addGlobalMacroClasses(Collection<String> names) {
    int i = 0;
    for (String name : names) {
      addMacroClass(globalMacroClasses, i, name);
      i += 2;
    }
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
    initLookUp(module);
    addImportsToMacros(module);
    elements.push(module);
    super.visitModule(module);
    elements.pop();
    module.internTypesAugmentations();
  }

  private void initLookUp(GoloModule module) {
    macroClasses.clear();
    macroClasses.addAll(globalMacroClasses);
    if (module != null) {
      addImportsToMacros(module);
    }
  }

  private void addImportsToMacros(GoloModule module) {
    for (ModuleImport mod : module.getImports()) {
      addMacroClass(macroClasses, 0, mod.getPackageAndClass().toString());
    }
    addMacroClass(macroClasses, 0, module.getPackageAndClass().toString());
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
      expanded.replaceInParent(macroInvocation, elements.peek());
    } else {
      elements.peek().replaceElement(macroInvocation,
          new Noop("macro '" + macroInvocation.getName() + "' expanded without results"));
    }
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
      addMacroClass(macroClasses, 0, ((ConstantStatement) arg).getValue().toString());
    }
    return null;
  }

  private GoloElement expand(MacroInvocation invocation) {
    GoloElement result = null;
    if (isSpecial(invocation)) {
      result = expandSpecial(invocation);
    } else {
      MethodHandle macro = findMacro(invocation);
      try {
        result = toGoloElement(macro.invokeWithArguments(invocation.getArguments()));
      } catch (Throwable t) {
        throw new RuntimeException("failed to call macro: " + invocation.getName(), t);
      }
    }
    if (result != null && invocation.hasASTNode()) {
      result.setASTNode(invocation.getASTNode());
    }
    return result;
  }

  private MethodHandle findMacro(MacroInvocation invocation) {
    String methodName = invocation.getName();
    int arity = invocation.getArity();
    if (invocation.isOnContext()) {
      arity = arity + 1;
    }
    List<String> classNames = new LinkedList<>();
    int methodClassSeparatorIndex = methodName.lastIndexOf(".");
    if (methodClassSeparatorIndex >= 0) {
      addMacroClass(classNames, 0, methodName.substring(0, methodClassSeparatorIndex));
      methodName = methodName.substring(methodClassSeparatorIndex + 1);
    }
    classNames.addAll(macroClasses);
    boolean vararg;
    MethodHandle method = null;
    for (String className : classNames) {
      vararg = false;
      for (int i = arity; i >= 0; i--) {
        try {
          method = publicLookup().findStatic(
              Class.forName(className),
              methodName,
              genericMethodType(i, vararg));
          if (invocation.isOnContext()) {
            method = method.bindTo(elements.peek());
          }
          return method;
        } catch (Throwable ignored) { }
        if (!vararg) {
          vararg = true;
          i = arity + 1;
        }
      }
    }
    throw new RuntimeException("failed to load macro: " + invocation
        + " with arity " + arity + " in classes " + classNames);
  }
}
