/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package gololang.ir;

import java.util.*;

import static java.util.Collections.unmodifiableSet;

/**
 * Represents a {@code union} element.
 *
 * <p>For instance:
 * <pre class="listing"><code class="lang-golo" data-lang="golo">
 * union ConsList = {
 *   Empty
 *   Cons = {head, tail}
 * }
 * </code></pre>
 */
public final class Union extends GoloType<Union> implements ToplevelGoloElement {

  private final Set<UnionValue> values = new LinkedHashSet<>();
  private final Set<MacroInvocation> macroCalls = new LinkedHashSet<>();

  private Union(String name) {
    super(name);
  }

  /**
   * Creates a union type.
   *
   * <p>Typical usage:
   * <pre class="listing"><code class="lang-java" data-lang="java">
   * union("ConsList")
   *   .value("Empty")
   *   .value("Cons",
   *      "head",
   *      "tail")
   * </code></pre>
   * creates
   * <pre class="listing"><code class="lang-golo" data-lang="golo">
   * union ConsList = {
   *   Empty
   *   Cons = {head, tail}
   * }
   * </code></pre>
   *
   * @param name the name of the union.
   */
  public static Union union(String name) {
    return new Union(name);
  }

  protected Union self() { return this; }

  /**
   * Adds a new value to this union.
   */
  public boolean addValue(UnionValue value) {
    makeParentOf(value);
    return values.add(value);
  }

  /**
   * Adds a macro invocation to this union.
   *
   * <p>The macro is supposed to return a {@link UnionValue} when expanded.
   * This allows decorator-like syntax on union values:
   * <pre class="listing"><code class="lang-golo" data-lang="golo">
   * union Foo = {
   *   &#64;someMacro
   *   Value = {a, b}
   *
   *   &#64;otherMacro
   *   OtherValue
   * }
   * </code></pre>
   */
  public boolean addMacroInvocation(MacroInvocation macroCall) {
    return macroCalls.add(makeParentOf(macroCall));
  }

  /**
   * Adds a value or a macro invocation according to the given argument.
   *
   * @see #addValue(UnionValue)
   * @see #addMacroInvocation(MacroInvocation)
   */
  public boolean addElement(GoloElement<?> elt) {
    if (elt instanceof UnionValue) {
      return addValue((UnionValue) elt);
    }
    if (elt instanceof MacroInvocation) {
      return addMacroInvocation((MacroInvocation) elt);
    }
    throw cantConvert("UnionValue or MacroInvocation", elt);
  }

  public Collection<UnionValue> getValues() {
    return unmodifiableSet(values);
  }

  /**
   * Adds a new value to this union.
   *
   * <p>Convenient fluent method to add a new value.
   * <p>This is a builder method.
   *
   * @see #addValue(UnionValue)
   * @see UnionValue#members(Object...)
   */
  public Union value(String name, Object... members) {
    UnionValue value = new UnionValue(name);
    value.members(members);
    addValue(value);
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitUnion(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<GoloElement<?>> children() {
    List<GoloElement<?>> children = new LinkedList<>(values);
    children.addAll(macroCalls);
    return children;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void replaceElement(GoloElement<?> original, GoloElement<?> newElement) {
    if (values.contains(original)) {
      values.remove(original);
    } else if (macroCalls.contains(original)) {
      macroCalls.remove(original);
    } else {
      throw cantReplace(original, newElement);
    }
    addElement(newElement);
  }
}
