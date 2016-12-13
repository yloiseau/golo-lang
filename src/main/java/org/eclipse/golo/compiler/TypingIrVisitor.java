/*
 * Copyright (c) 2012-2016 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.golo.compiler;

import org.eclipse.golo.compiler.ir.*;
import org.eclipse.golo.compiler.types.*;
import java.lang.invoke.MethodType;
import java.util.*;

/**
 * Visitor to infer static type of Golo elements as much as possible.
 */
public class TypingIrVisitor extends AbstractGoloIrVisitor {

  private final Deque<Set<GoloType>> blockStack = new LinkedList<>();

  @Override
  public void visitConstantStatement(ConstantStatement constant) {
    if (constant.getValue() != null) {
      constant.setInferedStaticType(Value.of(constant.getValue()));
    }
  }

  @Override
  public void visitFunction(GoloFunction function) {
    function.walk(this);
    FunctionType t = FunctionType.generic(function.getArity(), function.isVarargs());
    t.setReturnedType(function.getBlock().getInferedStaticType());
    function.setInferedStaticType(t);
    // TODO: infer arguments and returned type
  }

  @Override
  public void visitBinaryOperation(BinaryOperation operation) {
    operation.walk(this);
    switch (operation.getType()) {
      case EQUALS: case NOTEQUALS: case LESS: case LESSOREQUALS: case MORE: case MOREOREQUALS:
      case AND: case OR: case IS: case ISNT: case OFTYPE:
        operation.setInferedStaticType(Value.of(Boolean.class));
        break;
      case PLUS: case TIMES:
        if (operation.getLeftExpression().getInferedStaticType() == Value.of(String.class)
            || operation.getLeftExpression().getInferedStaticType() == Value.of(String.class)) {
          operation.setInferedStaticType(Value.of(String.class));
        } else {
          operation.setInferedStaticType(UnionType.of(Value.of(String.class), UnionType.Arithmetic));
        }
        break; // TODO: Number or Char or String
      case MINUS: case DIVIDE:
      case MODULO:
        operation.setInferedStaticType(UnionType.Arithmetic);
        break;
      case ANON_CALL: case METHOD_CALL: case ELVIS_METHOD_CALL:
        break; // TODO: call operator
      case ORIFNULL:
        operation.setInferedStaticType(UnionType.of(
              operation.getLeftExpression().getInferedStaticType(),
              operation.getRightExpression().getInferedStaticType()));
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + operation.getType());
    }
  }

  @Override
  public void visitUnaryOperation(UnaryOperation operation) {
    operation.walk(this);
    switch (operation.getType()) {
      case NOT:
        operation.setInferedStaticType(Value.of(Boolean.class));
        break;
      default:
        throw new IllegalArgumentException("Unknown operator: " + operation.getType());
    }
  }

  @Override
  public void visitBlock(Block block) {
    blockStack.push(new HashSet<GoloType>());
    block.walk(this);
    block.setInferedStaticType(UnionType.of(blockStack.pop()));
  }

  @Override
  public void visitReturnStatement(ReturnStatement ret) {
    ret.walk(this);
    blockStack.peek().add(((ExpressionStatement) ret.getExpressionStatement()).getInferedStaticType());
  }

  // TODO: Block, ClosureReference, MatchExpression, NamedArgument, ReferenceLookup, UnaryOperation

}
