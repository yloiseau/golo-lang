/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.runtime;

import java.lang.invoke.MethodHandle;

import static java.lang.invoke.MethodHandles.Lookup;

class FunctionFinder extends MethodFinder {

  FunctionFinder(MethodInvocation invocation, Lookup lookup) {
    super(invocation, lookup);
  }

  @Override
  public MethodHandle find() {
    // TODO: find the function
    return null;
  }

}
