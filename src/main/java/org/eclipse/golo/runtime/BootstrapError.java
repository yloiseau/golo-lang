/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.runtime;

/**
 * An error raised during invokedynamic bootstrap class initialization.
 */
public final class BootstrapError extends Error {
  private BootstrapError(String m, Throwable e) {
    super(m, e);
  }

  public static BootstrapError becauseOf(Throwable e) {
    return new BootstrapError("Could not bootstrap the required method handles", e);
  }
}
