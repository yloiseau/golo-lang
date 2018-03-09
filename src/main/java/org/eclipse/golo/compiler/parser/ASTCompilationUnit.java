/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler.parser;

import java.nio.file.Path;

public class ASTCompilationUnit extends GoloASTNode {

  private Path filename;

  public ASTCompilationUnit(int id) {
    super(id);
  }

  public ASTCompilationUnit(GoloParser p, int id) {
    super(p, id);
  }

  public Path getFilename() {
    return filename;
  }

  public void setFilename(Path filename) {
    this.filename = filename;
  }

  @Override
  public Object jjtAccept(GoloParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}

