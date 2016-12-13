/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.types;

public interface GoloType {
  default boolean isConsistentWith(GoloType other) {
    return isSubtypeOf(other) || other == Value.ANY || this == Value.ANY;
  }

  boolean isSubtypeOf(GoloType other);
}
