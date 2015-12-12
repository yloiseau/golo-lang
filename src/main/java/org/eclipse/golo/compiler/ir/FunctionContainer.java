/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.ir;

import java.util.Set;
import java.util.Collection;
import org.eclipse.golo.compiler.FunctionSignature;
import org.eclipse.golo.runtime.AmbiguousFunctionReferenceException;

public interface FunctionContainer {
  Set<GoloFunction> getFunctions();

  void addFunction(GoloFunction func);

  default void checkShadowing(GoloFunction func) throws AmbiguousFunctionReferenceException {
    FunctionSignature newFunction = FunctionSignature.of(func);
    for (GoloFunction f : getFunctions()) {
      if (newFunction.shadows(f)) {
        throw new AmbiguousFunctionReferenceException(func + " is shadowing " + f);
      }
    }
  }

  default void addFunctions(Collection<GoloFunction> funcs) {
    for (GoloFunction f : funcs) {
      addFunction(f);
    }
  }

  boolean hasFunctions();
}
