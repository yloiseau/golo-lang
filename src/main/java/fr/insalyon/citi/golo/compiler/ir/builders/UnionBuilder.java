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

import fr.insalyon.citi.golo.compiler.ir.Union;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import static java.util.Arrays.asList;

public final class UnionBuilder implements IrNodeBuilder<Union> {
  private Map<String, List<String>> values = new LinkedHashMap<>();
  private String name;

  public UnionBuilder name(String name) {
    this.name = name;
    return this;
  }

  public UnionBuilder value(String name, String... members) {
    values.put(name, asList(members));
    return this;
  }

  @Override
  public Union build() {
    if (name == null || values.isEmpty()) {
      throw new IllegalStateException("UnionBuilder not initialized");
    }
    Union union = new Union(name);
    for (String value : values.keySet()) {
      union.addValue(value, values.get(value));
    }
    return union;
  }
}

