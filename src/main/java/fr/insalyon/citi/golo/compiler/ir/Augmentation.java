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
 * "classical" augmentation.
 */
public class Augmentation extends GoloElement {
  private final String target;
  private final Set<GoloFunction> functions;
  private final Set<String> names;

  public Augmentation(String target) {
    this(target, new LinkedHashSet<>(), new LinkedHashSet<>());
  }

  public Augmentation(String target, Set<GoloFunction> functions, Set<String> names) {
    this.target = target;
    this.functions = functions;  
    this.names = names;
  }

  public String getTarget() {
    return target;
  }

  public Set<GoloFunction> getFunctions() {
    return unmodifiableSet(functions);
  }

  public void addFunction(GoloFunction func) {
    functions.add(func);
  }

  public Set<String> getNames() {
    return unmodifiableSet(names);
  }

  public void addName(String name) {
    names.add(name);
  }

  public void addNames(Collection<String> names) {
    this.names.addAll(names);
  }

  @Override
  public void replaceInParent(GoloElement original, GoloElement parent) {
    if (parent instanceof GoloModule) {
      ((GoloModule) parent).addAugmentation(this);
    } else {
      super.replaceInParent(original, parent);
    }
  }
}
