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
import org.eclipse.golo.compiler.parser.GoloASTNode;
import java.util.Set;
import java.util.LinkedHashSet;

import static java.util.Collections.unmodifiableSet;

public final class UnionValue extends GoloElement {
  private final String name;
  private final Set<Member> members = new LinkedHashSet<>();

  UnionValue(Union union, String name) {
    this.name = name;
    setParentNode(union);
  }

  public UnionValue ofAST(GoloASTNode node) {
    super.ofAST(node);
    return this;
  }

  public PackageAndClass getPackageAndClass() {
    return getUnion().getPackageAndClass().createInnerClass(name);
  }

  public Union getUnion() {
    return (Union) getParentNode().get();
  }

  public String getName() {
    return name;
  }

  public boolean hasMembers() {
    return !this.members.isEmpty();
  }

  private void addMember(Member member) {
    this.members.add(member);
    makeParentOf(member);
  }

  void addMembers(Iterable<Member> members) {
    members.forEach(this::addMember);
  }

  public Set<Member> getMembers() {
    return unmodifiableSet(members);
  }

  public UnionValue withMember(Object name, Object defaultValue) {
    addMember(Member.withDefault(name, defaultValue));
    return this;
  }

  public UnionValue withMember(Object member) {
    if (member instanceof Member) {
      addMember((Member) member);
    } else {
      addMember(Member.withDefault(member, null));
    }
    return this;
  }

  @Override
  protected void setParentNode(GoloElement parent) {
    if (!(parent instanceof Union)) {
      throw new IllegalArgumentException("UnionValue can only be defined in a Union");
    }
    super.setParentNode(parent);
  }

  @Override
  protected void replaceElement(GoloElement original, GoloElement newElement) {
    throw cantReplace();
  }

  @Override
  public void accept(GoloIrVisitor visitor) {
    visitor.visitUnionValue(this);
  }

  @Override
  public void walk(GoloIrVisitor visitor) {
    for (Member m : members) {
      m.accept(visitor);
    }
  }
}

