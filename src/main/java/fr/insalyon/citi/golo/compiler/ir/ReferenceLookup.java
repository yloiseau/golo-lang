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

public class ReferenceLookup extends ExpressionStatement {

  private final String name;

  public ReferenceLookup(String name) {
    super();
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public LocalReference resolveIn(ReferenceTable referenceTable) {
    return referenceTable.get(name);
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitReferenceLookup(this);
  }

  @Override
  public String toString() {
    return String.format("Ref{name=%s}", getName());
  }
}
