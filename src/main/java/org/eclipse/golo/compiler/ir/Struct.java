/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler.ir;

import org.eclipse.golo.compiler.PackageAndClass;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import org.eclipse.golo.compiler.parser.GoloASTNode;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.Collectors.toList;

import static org.eclipse.golo.compiler.ir.Builders.*;

public final class Struct extends GoloElement {

  public static final String IMMUTABLE_FACTORY_METHOD = "$_immutable";

  private PackageAndClass moduleName;
  private final String name;
  private final Set<Member> members = new LinkedHashSet<>();

  @Override
  public Struct ofAST(GoloASTNode node) {
    super.ofAST(node);
    return this;
  }

  Struct(String name) {
    super();
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public Struct members(String... members) {
    return this.members(asList(members));
  }

  public Struct members(Collection<String> members) {
    members.forEach(this::addMember);
    return this;
  }

  public void addMember(Member member) {
    this.members.add(member);
  }

  public void addMember(String name) {
    addMember(new Member(name));
  }

  public PackageAndClass getPackageAndClass() {
    return moduleName.createSubPackage("types").createSubPackage(name);
  }

  public void setModuleName(PackageAndClass module) {
    this.moduleName = module;
  }

  private List<String> getMemberNames() {
    return members.stream()
      .map(Member::getName)
      .collect(toList());
  }

  public Set<Member> getMembers() {
    return Collections.unmodifiableSet(members);
  }

  public List<Member> getPublicMembers() {
    return members.stream()
      .filter(Member::isPublic)
      .collect(toList());
  }

  public Set<GoloFunction> createFactories() {
    String fullName = getPackageAndClass().toString();
    Object[] args = members.stream()
              .map(Member::getName)
              .map(ReferenceLookup::new)
              .toArray();

    return new LinkedHashSet<GoloFunction>(asList(
        functionDeclaration(name).synthetic()
        .returns(call(fullName)),

        functionDeclaration(name).synthetic()
        .withParameters(getMemberNames())
        .returns(call(fullName)
            .withArgs(args)),

        functionDeclaration("Immutable" + name).synthetic()
        .withParameters(getMemberNames())
        .returns(call(fullName + "." + IMMUTABLE_FACTORY_METHOD)
            .withArgs(members.stream()
              .map(Member::getName)
              .map(ReferenceLookup::new)
              .toArray()))));
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitStruct(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    // nothing to do, not a composite
  }

  @Override
  protected void replaceElement(GoloElement original, GoloElement newElement) {
    throw cantReplace();
  }
}
