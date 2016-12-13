/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.types;

import java.util.Objects;
import java.util.HashMap;

public class Value implements GoloType {
  private static final HashMap<Class<?>, Value> cache = new HashMap<>();
  private Class<?> type;

  static final Value NONE = new Value() {
    @Override
    public String toString() { return "None"; }

    @Override
    public boolean isSubtypeOf(GoloType other) {
      return true;
    }

    @Override
    public Class<?> toClass() {
      return void.class;
    }
  };

  static final Value ANY = new Value(Object.class) {
    @Override
    public String toString() { return "Any"; }

    @Override
    public boolean isSubtypeOf(GoloType other) {
      return false;
    }
  };

  private Value() {
    type = null;
  }

  private Value(Class<?> t) {
    type = Objects.requireNonNull(t);
  }

  public Class<?> toClass() {
    return type;
  }

  @Override
  public String toString() {
    return type.getName();
  }

  @Override
  public boolean isSubtypeOf(GoloType other) {
    if (other instanceof Value) {
      Value v = (Value) other;
      return v.type.isAssignableFrom(type);
    }
    // TODO...
    return false;
  }

  public static Value of(Object t) {
    if (t == null) {
      return NONE;
    }
    if (t instanceof Class) {
      Class<?> cls = (Class<?>) t;
      if (!cache.containsKey(cls)) {
        cache.put(cls, new Value(cls));
      }
      return cache.get(cls);
    }
    return of(t.getClass());
  }
}
