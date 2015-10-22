/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA-Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler.macro;

import gololang.ir.*;
import org.eclipse.golo.compiler.GoloCompilationException;
import org.eclipse.golo.compiler.PositionInSourceCode;

import java.util.*;
import java.lang.invoke.MethodHandle;
import java.util.function.Function;

import static org.eclipse.golo.compiler.GoloCompilationException.Problem.Type.*;
import static gololang.Messages.message;
import static gololang.Messages.info;

/**
 * Visitor to expand macro calls.
 * <p>
 * This visitor replace the {@code MacroInvocation} nodes with the result of the macro
 * expansion.
 * <p>
 * Contextual macros are called with the call itself as implicit first argument.
 */
public final class MacroExpansionIrVisitor extends AbstractGoloIrVisitor {

  private static final boolean DEBUG = Boolean.getBoolean("golo.debug.macros");

  private GoloCompilationException.Builder exceptionBuilder;
  private final MacroFinder finder;

  private boolean expandRegularCalls = true;
  private boolean recurse = true;
  private boolean defaultRecurse = true;

  public MacroExpansionIrVisitor(GoloCompilationException.Builder exceptionBuilder, ClassLoader loader, boolean defaultRecurse) {
    this.finder = new MacroFinder(loader);
    this.exceptionBuilder = exceptionBuilder;
    this.defaultRecurse = defaultRecurse;
  }

  public MacroExpansionIrVisitor(GoloCompilationException.Builder exceptionBuilder, ClassLoader loader) {
    this(exceptionBuilder, loader, true);
  }

  public MacroExpansionIrVisitor() {
    this.finder = new MacroFinder();
    this.exceptionBuilder = null;
    this.defaultRecurse = true;
  }

  private static void debug(String message, Object... args) {
    if (DEBUG || gololang.Runtime.debugMode()) {
      info("Macro expansion: " + String.format(message, args));
    }
  }

  /**
   * Reset the internal state for the given module.
   */
  private MacroExpansionIrVisitor reset(GoloModule module) {
    this.finder.init(module.getImports().stream().map(mi -> mi.getPackageAndClass().toString()));
    this.expandRegularCalls = true;
    this.recurse = defaultRecurse;
    if (this.exceptionBuilder == null) {
      String moduleName = "unknown";
      if (module != null) {
        moduleName = module.getPackageAndClass().toString();
      }
      this.exceptionBuilder = new GoloCompilationException.Builder(moduleName);
    }
    debug("reset for module %s", module);
    return this;
  }

  /**
   * Defines if the macros must be expanded recursively.
   * <p>
   * Mainly for debugging purpose.
   */
  public MacroExpansionIrVisitor recurse(boolean v) {
    this.recurse = v;
    return this;
  }

  /**
   * Defines if regular function invocations must be tried to expand.
   * <p>
   * Mainly for debugging purpose.
   */
  public MacroExpansionIrVisitor expandRegularCalls(boolean v) {
    this.expandRegularCalls = v;
    return this;
  }

  public void setExceptionBuilder(GoloCompilationException.Builder builder) {
    exceptionBuilder = builder;
  }

  private void replace(AbstractInvocation<?> invocation, GoloElement<?> original, GoloElement<?> replacement) {
    try {
      original.replaceInParentBy(replacement);
    } catch (Throwable t) {
      expansionFailed(invocation, t);
    }
  }

  @Override
  public void visitFunction(GoloFunction function) {
    GoloElement<?> converted = convertMacroDecorator(function);
    if (converted instanceof MacroInvocation) {
      replace((MacroInvocation) converted, function, converted);
      converted.accept(this);
    } else {
      function.walk(this);
    }
  }

  /**
   * Convert a function with macro decorators into nested macro calls.
   * <p>
   * The function node is <em>mutated</em> (decorator removed).
   */
  private GoloElement<?> convertMacroDecorator(GoloFunction function) {
    GoloFunction newFunction = GoloFunction.function(function);
    GoloElement<?> newElement = newFunction;
    for (Decorator decorator : new ArrayList<>(function.getDecorators())) {
      MacroInvocation invocation = decoratorToMacroInvocation(decorator, newElement);
      if (macroExists(invocation)) {
        newElement = invocation;
        newFunction.removeDecorator(decorator);
      }
    }
    return newElement;
  }

  /**
   * Convert a macro decorator into macro call on the function declaration.
   * <p>
   * For instance
   * <pre><code>
   * @myMacro
   * function foo = |x| -> x
   * </code></pre>
   *
   * is converted into something equivalent to:
   * <pre><code>
   * &myMacro {
   * function foo = |x| -> x
   * }
   * </pre></code>
   * that is a macro call on a function declaration node.
   */
  private MacroInvocation decoratorToMacroInvocation(Decorator decorator, GoloElement<?> function) {
    ExpressionStatement<?> expr = decorator.expression();
    if (expr instanceof FunctionInvocation) {
      FunctionInvocation invocation = (FunctionInvocation) expr;
      return MacroInvocation.call(invocation.getName())
        .withArgs(invocation.getArguments().toArray())
        .withArgs(function);
    } else if (expr instanceof ReferenceLookup) {
      return MacroInvocation.call(((ReferenceLookup) expr).getName())
        .withArgs(function);
    } else if (expr instanceof ClosureReference) {
      // Not (yet?) a valid macro call
      return null;
    } else if (expr instanceof BinaryOperation) {
      // Not (yet?) a valid macro call
      return null;
    } else {
      // must not happen
      throw new IllegalArgumentException("Invalid decorator type");
    }
  }

  public GoloElement<?> expand(GoloElement<?> element) {
    element.accept(this);
    return element;
  }

  @Override
  public void visitModule(GoloModule module) {
    this.reset(module);
    module.walk(this);
    module.decoratorMacro().map(this::expand);
    module.decoratorMacro(null);
  }

  @Override
  public void visitMacroInvocation(MacroInvocation macroInvocation) {
    macroInvocation.walk(this);
    GoloElement<?> expanded = expand(macroInvocation);
    replace(macroInvocation, macroInvocation, expanded);
    if (recurse) {
      expanded.accept(this);
    }
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation macroInvocation) {
    macroInvocation.walk(this);
    if (tryExpand(macroInvocation)) {
      // Let's try to expand a regular call as a macro
      GoloElement<?> expanded = expand(macroInvocation);
      if (expanded == null) {
        // The call failed. Maybe it was not a macro after all...
        return;
      }
      replace(macroInvocation, macroInvocation, expanded);
      if (recurse) {
        expanded.accept(this);
      }
    }
  }

  private boolean tryExpand(FunctionInvocation invocation) {
    return expandRegularCalls && !invocation.isAnonymous() && !invocation.isConstant();
  }

  public MacroExpansionIrVisitor useMacroModule(String name) {
    this.finder.addMacroClass(name);
    return this;
  }

  private GoloElement<?> expand(FunctionInvocation invocation) {
    debug("try to expand %s", invocation);
    Optional<MethodHandle> macro = findMacro(invocation);
    if (!macro.isPresent()) {
      debug("macro not found");
      return null;
    }
    return macro.map(invokeMacroWith(invocation)).orElse(noMacroResult(invocation.getName()));
  }

  private GoloElement<?> expand(MacroInvocation invocation) {
    debug("try to expand %s", invocation);
    return findMacro(invocation)
      .map(invokeMacroWith(invocation))
      .orElse(noMacroResult(invocation.getName()));
  }

  private Function<MethodHandle, GoloElement<?>> invokeMacroWith(AbstractInvocation<?> invocation) {
    return (macro) -> {
      try {
        GoloElement<?> result = (GoloElement<?>) macro.invokeWithArguments(invocation.getArguments());
        debug("macro expanded to %s", result);
        return result;
      } catch (Throwable t) {
        expansionFailed(invocation, t);
        debug("expansion failed");
        return null;
      }
    };
  }

  private GoloElement<?> noMacroResult(String macroName) {
    return Noop.of("macro `" + macroName + "` expanded without results");
  }

  private void loadingFailed(MacroInvocation invocation) {
    String errorMessage = message("macro_loading_failed", invocation.getName(), invocation.getArity())
        + ' ' + position(invocation) + ".";
    exceptionBuilder.report(UNKNOWN_MACRO, invocation, errorMessage);
  }

  private void expansionFailed(AbstractInvocation<?> invocation, Throwable t) {
    String errorMessage = message("macro_expansion_failed", invocation.getName())
      + ' ' + position(invocation) + ".";
    exceptionBuilder.report(MACRO_EXPANSION, invocation, errorMessage, t);
  }

  private String position(GoloElement<?> elt) {
    PositionInSourceCode position = elt.positionInSourceCode();
    if (position == null || position.isUndefined()) {
      return message("generated_code");
    }
    return message("source_position", position.getStartLine(), position.getStartColumn());
  }

  private Optional<MethodHandle> findMacro(FunctionInvocation invocation) {
    return finder.find(invocation).map(m -> m.binded(this, invocation));
  }

  private Optional<MethodHandle> findMacro(MacroInvocation invocation) {
    Optional<MethodHandle> macro = finder.find(invocation).map(m -> m.binded(this, invocation));
    if (!macro.isPresent()) {
      loadingFailed(invocation);
    }
    return macro;
  }

  public boolean macroExists(MacroInvocation invocation) {
    Optional<MacroFinderResult> r = finder.find(invocation);
    if (r.isPresent()) {
      return true;
    }
    return false;
    // return finder.find(invocation).isPresent();
  }
}
