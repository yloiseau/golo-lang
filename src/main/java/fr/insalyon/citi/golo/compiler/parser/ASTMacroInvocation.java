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

package fr.insalyon.citi.golo.compiler.parser;

public class ASTMacroInvocation extends GoloASTNode implements NamedNode {

  private String name;
  private boolean topLevel = false;
  private boolean onContext = false;

  public ASTMacroInvocation(int id) {
    super(id);
  }

  public ASTMacroInvocation(GoloParser p, int id) {
    super(p, id);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }
  
  public void setTopLevel(boolean v) {
    topLevel = v;
  }

  public boolean isTopLevel() {
    return topLevel;
  }

  public void setOnContext(boolean v) {
    onContext = v;
  }

  public boolean isOnContext() {
    return onContext;
  }
  
  @Override
  public String toString() {
    return "ASTMacroInvocation{" +
        "name='" + name + "'" +
        '}';
  }

  @Override
  public Object jjtAccept(GoloParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }

}