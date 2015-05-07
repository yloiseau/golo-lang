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

import fr.insalyon.citi.golo.compiler.PackageAndClass;

import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;
import static java.util.Collections.unmodifiableSet;

public final class Union extends GoloElement {

  public static final class Value {
    private final String name;
    private final Union union;
    private final Set<String> members = new LinkedHashSet<>();

    public Value(Union union, String name) {
      this.name = name;
      this.union = union;
    }

    public PackageAndClass getPackageAndClass() {
      return union.getPackageAndClass().createInnerClass(name);
    }

    public Union getUnion() {
      return union;
    }

    public String getName() {
      return name;
    }

    public void addMembers(Collection<String> memberNames) {
      this.members.addAll(memberNames);
    }

    public boolean hasMembers() {
      return !this.members.isEmpty();
    }

    public Set<String> getMembers() {
      return unmodifiableSet(members);
    }
  }

  private  PackageAndClass moduleName;
  private final String name;
  private final Set<Value> values = new LinkedHashSet<>();

  public Union(String name) {
    this.name = name;
  }

  public PackageAndClass getPackageAndClass() {
    return new PackageAndClass(moduleName.toString() + ".types", name);
  }

  public void setModuleName(PackageAndClass module) {
    this.moduleName = module;
  }

  public void addValue(String name, Collection<String> members) {
    Value value = new Value(this, name);
    value.addMembers(members);
    values.add(value);
  }

  public Collection<Value> getValues() {
    return unmodifiableSet(this.values);
  }

  @Override
  public void replaceInParent(GoloElement original, GoloElement parent) {
    if (parent instanceof GoloModule) {
      ((GoloModule) parent).addUnion(this);
    } else {
      super.replaceInParent(original, parent);
    }
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitUnion(this);
  }
}
