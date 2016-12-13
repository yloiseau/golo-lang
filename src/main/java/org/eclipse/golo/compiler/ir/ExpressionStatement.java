/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.ir;

import org.eclipse.golo.compiler.types.*;

public abstract class ExpressionStatement extends GoloStatement {

  private GoloType inferedStaticType = Value.of(Object.class);

  public GoloType getInferedStaticType() {
    return inferedStaticType;
  }

  public void setInferedStaticType(GoloType type) {
    inferedStaticType = type;
  }

  public void setTypeIfMoreSpecific(GoloType type) {
    if (type.isSubtypeOf(inferedStaticType)) {
      inferedStaticType = type;
    }
  }

  public static ExpressionStatement of(Object expr) {
    if (expr instanceof ExpressionStatement) {
      return (ExpressionStatement) expr;
    }
    throw cantConvert("ExpressionStatement", expr);
  }
}
