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

public class MacroInvocation extends AbstractInvocation {

  private boolean onContext = false;

  public void setOnContext(boolean v) {
    onContext = v;
  }

  public boolean isOnContext() {
    return onContext;
  }

  public MacroInvocation(String name) {
    super(name);
  }

  @Override
  public int getArity() {
    return super.getArity() + (onContext ? 1 : 0);
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitMacroInvocation(this);
  }

  @Override
  public String toString() {
    return "MacroInvocation{" +
              "name=" + getName() +
              ", onContext=" + isOnContext() +
           "}";
  }
}
