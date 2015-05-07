/*
 * Copyright 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.insalyon.citi.golo.compiler.ir;

import fr.insalyon.citi.golo.compiler.parser.GoloASTNode;

import java.lang.ref.WeakReference;

public abstract class GoloElement {
  private WeakReference<GoloASTNode> nodeRef;

  public void setASTNode(GoloASTNode node) {
    nodeRef = new WeakReference<>(node);
  }

  public GoloASTNode getASTNode() {
    if (nodeRef == null) { return null; }
    return nodeRef.get();
  }

  public boolean hasASTNode() {
    return (nodeRef != null) && (nodeRef.get() != null);
  }

  public String getDocumentation() {
    if (hasASTNode()) {
      return getASTNode().getDocumentation();
    }
    return null;
  }

  public PositionInSourceCode getPositionInSourceCode() {
    GoloASTNode node = getASTNode();
    if (node == null) {
      return new PositionInSourceCode(0, 0);
    }
    return new PositionInSourceCode(node.jjtGetFirstToken().beginLine, node.jjtGetFirstToken().beginColumn);
  }

  public void accept(GoloIrVisitor visitor) { };

  public void replaceElement(GoloElement original, GoloElement newElement) {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " can't replace " +
        original.getClass().getName() + " in " +
        newElement.getClass().getName());
  }

  public void replaceInParent(GoloElement original, GoloElement parent) {
    parent.replaceElement(original, this);
  }
}
