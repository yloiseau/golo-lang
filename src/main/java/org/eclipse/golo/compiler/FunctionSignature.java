/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler;

import org.eclipse.golo.compiler.ir.GoloElement;
import org.eclipse.golo.compiler.ir.GoloFunction;
import org.eclipse.golo.compiler.ir.GoloModule;
import org.eclipse.golo.compiler.ir.NamedAugmentation;
import java.util.Objects;

// GoloFunction: module, augmentation, named augmentation
// FunctionRef (no arity)
// AbstractInvocation

/**
 * Represent the signature of a golo function (compile time).
 */
public final class FunctionSignature {
  private final PackageAndClass packageAndClass;
  private final String name;
  private final int arity;
  private final boolean varargs;

  public FunctionSignature(PackageAndClass packageAndClass, String name, int arity, boolean varargs) {
    this.packageAndClass = Objects.requireNonNull(packageAndClass);
    this.name = Objects.requireNonNull(name);
    this.arity = arity;
    this.varargs = varargs;
  }

  /**
   * Create a new signature from a {@code GoloFunction}.
   */
  public static FunctionSignature of(GoloFunction function) {
    GoloElement parent = function.getParentNode().get();
    PackageAndClass pac;
    if (parent instanceof GoloModule) {
      pac = ((GoloModule) parent).getPackageAndClass();
    } else if (parent instanceof NamedAugmentation) {
      pac = ((NamedAugmentation) parent).getPackageAndClass();
    } else {
      throw new IllegalStateException("Unknown parent for this function");
    }
    return new FunctionSignature(pac, function.getName(), function.getArity(), function.isVarargs());
  }

  /**
   * Change the package of this signature.
   *
   * @return a new signature.
   */
  public FunctionSignature within(PackageAndClass packageAndClass) {
    return new FunctionSignature(packageAndClass, this.name, this.arity, this.varargs);
  }

  /**
   * Check is this signature shadows another one.
   *
   * A function shadows another one when at runtime, the call informations are not enough to 
   * determine the exact function to call. This can append for instance when declaring in the same
   * module a vararg function having less parameters than another one having the same name.
   * <p>
   * For instance, given:
   * <code><pre>
   * function f = |x, y| -> ...
   * function f = |x...| -> ...
   *
   * f(1, 2)
   * </pre><code>
   * The runtime can't determine which one should be called.
   */
  public boolean shadows(FunctionSignature other) {
    return this.equals(other)
          || (this.packageAndClass.equals(other.packageAndClass)
              && this.name.equals(other.name)
              && this.varargs
              && this.arity <= other.arity);
  }

  public boolean shadows(GoloFunction other) {
    return this.shadows(of(other));
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) { return false; }
    if (this == o) { return true; }
    if (!(o instanceof FunctionSignature)) { return false; }
    FunctionSignature that = (FunctionSignature) o;
    return this.name.equals(that.name)
      && this.packageAndClass.equals(that.packageAndClass)
      && this.arity == that.arity
      && this.varargs == that.varargs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(packageAndClass, name, arity, varargs);
  }

  @Override
  public String toString() {
    return packageAndClass.toString() + "::" + name + "\\" + arity + (varargs ? "..." : "");
  }
}
