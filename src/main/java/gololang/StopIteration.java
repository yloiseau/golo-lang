/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package gololang;

import java.util.NoSuchElementException;

public final class StopIteration extends NoSuchElementException {
  private static final long serialVersionUID = 1L;

  public StopIteration() {
    super();
  }

  @Override
  public StopIteration fillInStackTrace() {
    return this;
  }
}
