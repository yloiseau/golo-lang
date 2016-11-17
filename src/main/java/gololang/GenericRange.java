
/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package gololang;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Represents a generic range.
 * <p>
 * A generic range is defined using an increment function.
 */
final class GenericRange<T extends Comparable<T>> extends AbstractRange<T> {

  public GenericRange(T from, T to) {
    super(from, to);
  }

  @Override
  public Range<T> reversed() {
    // TODO: reversed
    return null;
  }

  @Override
  public Range<T> tail() {
    if (isEmpty()) {
      return this;
    }
    // TODO: tail
    return null;
  }

  @Override
  public Iterator<T> iterator() {
    // TODO: iterator
    return null;
  }
}
