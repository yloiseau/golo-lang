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

package fr.insalyon.citi.golo.compiler.ir.builders;

import fr.insalyon.citi.golo.compiler.ir.LocalReference;
import fr.insalyon.citi.golo.compiler.ir.ReferenceLookup;

public final class LocalReferenceBuilder implements IrNodeBuilder<LocalReference> {
  private String name;
  private boolean synthetic = false;
  private int index = -1;
  private boolean moduleLevel = false;
  private boolean variable = false;

  private LocalReference.Kind kind() {
    if (!moduleLevel && !variable) { return LocalReference.Kind.CONSTANT; }
    if (!moduleLevel && variable) { return LocalReference.Kind.VARIABLE; }
    if (moduleLevel && !variable) { return LocalReference.Kind.MODULE_CONSTANT; }
    if (moduleLevel && variable) { return LocalReference.Kind.MODULE_VARIABLE; }
    return null;
  }

  public LocalReferenceBuilder kind(LocalReference.Kind k) {
    switch (k) {
      case CONSTANT:
        moduleLevel = false;
        variable = false;
        break;
      case VARIABLE:
        moduleLevel = false;
        variable = true;
        break;
      case MODULE_VARIABLE:
        moduleLevel = true;
        variable = true;
        break;
      case MODULE_CONSTANT:
        moduleLevel = true;
        variable = false;
        break;
    }
    return this;
  }

  public LocalReferenceBuilder variable() {
    variable = true;
    return this;
  }

  public LocalReferenceBuilder moduleLevel() {
    moduleLevel = true;
    return this;
  }

  public LocalReferenceBuilder name(String n) {
    this.name = n;
    return this;
  }

  public LocalReferenceBuilder synthetic(boolean s) {
    this.synthetic = s;
    return this;
  }

  public LocalReferenceBuilder index(int i) {
    this.index = i;
    return this;
  }

  @Override
  public LocalReference build() {
    LocalReference ref = new LocalReference(kind(), name, synthetic);
    ref.setIndex(index);
    return ref;
  }

  public ReferenceLookup lookup() {
    return new ReferenceLookup(name);
  }
}
