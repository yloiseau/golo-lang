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

import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;
import static java.util.Collections.unmodifiableSet;

/**
 * Named augmentation definition
 */
public class NamedAugmentation extends GoloElement {
  private final String name;
  private final Set<GoloFunction> functions;

  public NamedAugmentation(String name) {
    this(name, new LinkedHashSet<>());
  }

  public NamedAugmentation(String name, Set<GoloFunction> functions) {
    this.name = name;
    this.functions = functions;
  }

  public String getName() {
    return this.name;
  }

  public Set<GoloFunction> getFunctions() {
    return unmodifiableSet(functions);
  }
    
  public void addFunction(GoloFunction func) {
    functions.add(func);
  }

  public void addFunctions(Collection<GoloFunction> funcs) {
    functions.addAll(funcs);
  }

  public boolean hasFunctions() {
    return !functions.isEmpty();
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitNamedAugmentation(this);
  }

  @Override
  public void replaceInParent(GoloElement original, GoloElement parent) {
    if (parent instanceof GoloModule) {
      ((GoloModule) parent).addNamedAugmentation(this);
    } else {
      super.replaceInParent(original, parent);
    }
  }

}
