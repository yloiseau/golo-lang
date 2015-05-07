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

import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.Iterator;

/**
 * A container of top-level {@code GoloElement}.
 * <p>
 * This class is mainly used by top-level macros to return a collection of golo top-level elements,
 * i.e. functions, structs, augments and so on, since a macro must return a unique GoloElement to
 * inject in the Ir by replacing the macro call.
 */
public final class TopLevelElements extends GoloElement implements Iterable<GoloElement> {

  private Set<GoloElement> elements = new LinkedHashSet<>();

  public TopLevelElements() {}

  public TopLevelElements(Collection<GoloElement> elements) {
    this.elements.addAll(elements);
  }

  public void add(GoloElement element) {
    this.elements.add(element);
  }

  @Override
  public Iterator<GoloElement> iterator() {
    return elements.iterator();
  }

  @Override
  public void replaceInParent(GoloElement original, GoloElement parent) {
    for (GoloElement element : elements) {
      parent.replaceElement(original, element);
    }
  }
}
