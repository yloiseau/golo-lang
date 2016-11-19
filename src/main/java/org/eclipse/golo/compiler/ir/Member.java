/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.ir;

import static java.util.Objects.requireNonNull;

public final class Member {
  private final String name;
  private final ExpressionStatement defaultValue;

  Member(String name, ExpressionStatement defaultValue) {
    this.name = requireNonNull(name);
    this.defaultValue = defaultValue;
  }

  Member(String name) {
    this(name, null);
  }

  public String getName() {
    return name;
  }

  public ExpressionStatement getDefaultValue() {
    return defaultValue;
  }

  public boolean hasDefault() {
    return defaultValue != null;
  }

  public boolean isPublic() {
    return !name.startsWith("_");
  }

  @Override
  public String toString() {
    return defaultValue == null ? name : name + "=" + defaultValue;
  }
}
