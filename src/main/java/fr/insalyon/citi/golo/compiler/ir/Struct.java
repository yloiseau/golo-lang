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

import static fr.insalyon.citi.golo.compiler.ir.GoloFunction.Visibility.PUBLIC;
import static fr.insalyon.citi.golo.compiler.ir.GoloFunction.Scope.MODULE;
import static fr.insalyon.citi.golo.compiler.ir.LocalReference.Kind.CONSTANT;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

public final class Struct extends GoloElement {

  public static final String IMMUTABLE_FACTORY_METHOD = "$_immutable";

  private PackageAndClass moduleName;
  private final String name;
  private final Set<String> members;
  private final Set<String> publicMembers;

  public Struct(String name, Set<String> members) {
    this.name = name;
    this.members = members;
    this.publicMembers = new LinkedHashSet<>();
    for (String member : members) {
      if (!member.startsWith("_")) {
        publicMembers.add(member);
      }
    }
  }

  public PackageAndClass getPackageAndClass() {
    return new PackageAndClass(moduleName.toString() + ".types", name);
  }

  public void setModuleName(PackageAndClass module) {
    this.moduleName = module;
  }

  public Set<String> getMembers() {
    return Collections.unmodifiableSet(members);
  }

  public Set<String> getPublicMembers() {
    return Collections.unmodifiableSet(publicMembers);
  }

  public Set<GoloFunction> createFactories() {
    Set<GoloFunction> factories = new LinkedHashSet<>();
    String name = getPackageAndClass().className();

    GoloFunction factory = new GoloFunction(name, PUBLIC, MODULE);
    Block block = new Block(new ReferenceTable());
    factory.setBlock(block);
    block.addStatement(new ReturnStatement(new FunctionInvocation(getPackageAndClass().toString())));
    factories.add(factory);

    factory = new GoloFunction(name, PUBLIC, MODULE);
    factory.setParameterNames(new LinkedList<>(members));
    FunctionInvocation call = new FunctionInvocation(getPackageAndClass().toString());
    ReferenceTable table = new ReferenceTable();
    block = new Block(table);
    for (String member : members) {
      call.addArgument(new ReferenceLookup(member));
      table.add(new LocalReference(CONSTANT, member));
    }
    factory.setBlock(block);
    block.addStatement(new ReturnStatement(call));
    factories.add(factory);

    factory = new GoloFunction("Immutable" + name, PUBLIC, MODULE);
    factory.setParameterNames(new LinkedList<>(members));
    call = new FunctionInvocation(getPackageAndClass().toString() + "." + IMMUTABLE_FACTORY_METHOD);
    table = new ReferenceTable();
    block = new Block(table);
    for (String member : members) {
      call.addArgument(new ReferenceLookup(member));
      table.add(new LocalReference(CONSTANT, member));
    }
    factory.setBlock(block);
    block.addStatement(new ReturnStatement(call));
    factories.add(factory);

    return factories;
  }

  @Override
  public void replaceInParent(GoloElement original, GoloElement parent) {
    if (parent instanceof GoloModule) {
      ((GoloModule) parent).addStruct(this);
    } else {
      super.replaceInParent(original, parent);
    }
  }
}
