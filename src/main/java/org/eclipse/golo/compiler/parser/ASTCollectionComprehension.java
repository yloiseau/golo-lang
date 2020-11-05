/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.parser;

public class ASTCollectionComprehension extends ASTCollectionLiteral {

  public ASTCollectionComprehension(int id) {
    super(id);
  }

  public ASTCollectionComprehension(GoloParser p, int id) {
    super(p, id);
  }

  @Override
  public String toString() {
    return String.format("ASTCollectionComprehension{type='%s'}", getType());
  }
}
