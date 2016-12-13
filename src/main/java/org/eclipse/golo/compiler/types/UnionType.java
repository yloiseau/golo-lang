/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.types;

import java.util.*;
import static java.util.stream.Collectors.joining;


public final class UnionType implements GoloType, Iterable<GoloType> {

  public static final UnionType Arithmetic = new UnionType(Arrays.asList(Value.of(Number.class), Value.of(Character.class)));

  private final Set<GoloType> types = new HashSet<>();

  private UnionType(Collection<GoloType> types) {
    this.types.addAll(types);
  }

  @Override
  public Iterator<GoloType> iterator() {
    return types.iterator();
  }

  @Override
  public String toString() {
    return types.stream().map(Object::toString).collect(joining("|"));
  }

  @Override
  public boolean isSubtypeOf(GoloType other) {
    // TODO
    if (other instanceof UnionType) {
      for (GoloType t1 : types) {
        boolean r = false;
        for (GoloType t2 : (UnionType) other) {
          r = r || t1.isConsistentWith(t2);
        }
        if (!r) { return false; }
      }
      return true;
    }
    return false;
  }

  public static GoloType of(GoloType type, GoloType... types) {
    List<GoloType> l = new ArrayList<>(Arrays.asList(types));
    l.add(0, type);
    return of(l);
  }

  public static GoloType of(Collection<GoloType> types) {
    if (types.isEmpty()) {
      return Value.NONE;
    }
    HashSet<GoloType> ts = new HashSet<>();
    for (GoloType t : types) {
      if (t instanceof UnionType) {
        ((UnionType) t).forEach(ts::add);
      } else {
        ts.add(t);
      }
    }
    // TODO: filter to keep only the more specific types
    if (ts.size() == 1) { return ts.iterator().next(); }
    return new UnionType(ts);

  }

}
