/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.parser;

public class ASTRangeLiteral extends ASTCollectionLiteral {

  public ASTRangeLiteral(int id) {
    super(id);
    setType("range");
  }

  public ASTRangeLiteral(GoloParser p, int id) {
    super(p, id);
    setType("range");
  }
}
