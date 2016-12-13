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
import java.lang.invoke.MethodType;
import static java.util.stream.Collectors.joining;


public final class FunctionType implements GoloType {

  private final ArrayList<GoloType> parameterTypes = new ArrayList<>();
  private GoloType returnedType;

  private FunctionType(GoloType ret, Collection<GoloType> params) {
    this.parameterTypes.addAll(params);
    this.returnedType = ret;
  }

  @Override
  public String toString() {
    return "(" + parameterTypes.stream().map(Object::toString).collect(joining(", ")) + ") -> " + returnedType.toString();
  }

  @Override
  public boolean isSubtypeOf(GoloType other) {
    // TODO
    return true;
  }

  public List<GoloType> getParamTypes() {
    return Collections.unmodifiableList(parameterTypes);
  }

  public void setParamType(int i, GoloType t) {
    parameterTypes.set(i, t);
  }

  public void setReturnedType(GoloType t) {
    returnedType = t;
  }

  public GoloType getReturnedType() {
    return returnedType;
  }

  public String toDescriptor() {
    LinkedList<Class<?>> params = new LinkedList<>();
    Class<?> ret = Object.class;
    if (returnedType instanceof Value) {
      ret = ((Value) returnedType).toClass();
    }
    for (GoloType t : parameterTypes) {
      if (t instanceof Value) {
        params.add(((Value) t).toClass());
      } else {
        params.add(Object.class);
      }
    }
    return MethodType.methodType(ret, params).toMethodDescriptorString();
  }

  public static FunctionType of(GoloType ret, GoloType... params) {
    return new FunctionType(ret, Arrays.asList(params));
  }

  public static FunctionType generic(int arity, boolean varargs) {
    LinkedList<GoloType> params = new LinkedList<>();
    if (arity == 0 && varargs) {
      throw new IllegalArgumentException("varargs needs at least 1 parameter");
    }
    if (arity > 0) {
      for (int i = 0; i < arity - 1; i++) {
        params.add(Value.of(Object.class));
      }
      if (varargs) {
        params.add(Value.of(Object[].class));
      } else {
        params.add(Value.of(Object.class));
      }
    }
    return new FunctionType(Value.of(Object.class), params);
  }

}
