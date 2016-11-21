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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import org.eclipse.golo.compiler.parser.GoloASTNode;
import org.eclipse.golo.compiler.ClosureCaptureGoloIrVisitor;

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

  public Struct members(Object... members) {
    for (Object member : members) {
      withMember(member);
    }
    return this;
  }

  private void addMember(Member member) {
    this.members.add(member);
    makeParentOf(member);
  }

  public Struct withMember(Object name, Object defaultValue) {
    addMember(Member.withDefault(name, defaultValue));
    return this;
  }

  public Struct withMember(Object member) {
    if (member instanceof Member) {
      addMember((Member) member);
    } else {
      addMember(Member.withDefault(member, null));
    }
    return this;
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

  private List<String> getNonDefaultMemberNames() {
    return members.stream()
      .filter(m -> !m.hasDefault())
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

  public boolean hasDefaults() {
    return members.stream().anyMatch(Member::hasDefault);
  }

  private String getFullName() {
    return getPackageAndClass().toString();
  }

  private Object[] getFullArgs() {
    return members.stream()
      .map(Member::getName)
      .map(ReferenceLookup::new)
      .toArray();
  }

  private Object[] getDefaultArgs() {
    return members.stream()
      .map(Member::getDefaultOrRef)
      .toArray();
  }

  private GoloFunction createDefaultConstructor() {
    GoloFunction defaultFactory = functionDeclaration(name).synthetic()
      .returns(call(getFullName())
        .withArgs(members.stream().map(Member::getDefaultOrNull).toArray()));
    defaultFactory.accept(new ClosureCaptureGoloIrVisitor());
    if (defaultFactory.getSyntheticParameterCount() > 0) {
      // we use a dependant default value. The default factory must raise an exception
      defaultFactory = functionDeclaration(name).synthetic()
        .block(call("raise").withArgs(constant(
                "Can't call the default constructor of a structure with dependant default value.")));
    }
    defaultFactory.insertMissingReturnStatement();
    return defaultFactory;
  }

  private GoloFunction createFullArgsConstructor() {
    return functionDeclaration(name).synthetic()
      .withParameters(getMemberNames())
      .returns(call(getFullName()).withArgs(getFullArgs()));
  }

  private GoloFunction createFullArgsImmutableConstructor() {
    return functionDeclaration("Immutable" + name).synthetic()
      .withParameters(getMemberNames())
      .returns(call(getFullName() + "." + IMMUTABLE_FACTORY_METHOD).withArgs(getFullArgs()));
  }

  private GoloFunction createDefaultArgsConstructor() {
    return functionDeclaration(name).synthetic()
      .withParameters(getNonDefaultMemberNames())
      .returns(call(getFullName()).withArgs(getDefaultArgs()));
  }

  private GoloFunction createDefaultArgsImmutableConstructor() {
    return functionDeclaration("Immutable" + name).synthetic()
      .withParameters(getNonDefaultMemberNames())
      .returns(call(getFullName() + "." + IMMUTABLE_FACTORY_METHOD).withArgs(getDefaultArgs()));
  }

  public Set<GoloFunction> createFactories() {
    Set<GoloFunction> factories = new LinkedHashSet<>();
    factories.add(createDefaultConstructor());
    factories.add(createFullArgsConstructor());
    factories.add(createFullArgsImmutableConstructor());
    if (hasDefaults()) {
      factories.add(createDefaultArgsConstructor());
      factories.add(createDefaultArgsImmutableConstructor());
    }
    return factories;
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitStruct(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    for (Member m : members) {
      m.accept(visitor);
    }
  }

  @Override
  protected void replaceElement(GoloElement original, GoloElement newElement) {
    throw cantReplace();
  }
}
