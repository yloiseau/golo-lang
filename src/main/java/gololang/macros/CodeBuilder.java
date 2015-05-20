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

package gololang.macros;

import fr.insalyon.citi.golo.compiler.ir.builders.*;
import fr.insalyon.citi.golo.compiler.ir.*;
import fr.insalyon.citi.golo.runtime.OperatorType;
import fr.insalyon.citi.golo.compiler.parser.GoloParser;
import fr.insalyon.citi.golo.compiler.PackageAndClass;

import java.util.List;
import java.util.LinkedList;
import java.util.Deque;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static gololang.macros.Utils.*;
import static gololang.macros.SymbolGenerator.gensym;


// TODO: check for builder completeness

public final class CodeBuilder {

  public static MethodInvocationBuilder methodInvocation(String name) {
    return new MethodInvocationBuilder(name);
  }

  public static LoopBuilder loop() {
    return new LoopBuilder();
  }

  public static AssignmentStatementBuilder assignment() {
    return new AssignmentStatementBuilder();
  }

  public static AssignmentStatementBuilder assignment(boolean declaring, Object ref, Object expr) {
    return assignment().declaring(declaring).expression(expr).localRef(ref);
  }


  public static BinaryOperation binaryOperation(OperatorType type, Object left, Object right) {
    return new BinaryOperationBuilder().type(type).left(left).right(right).build();
  }

  public static BinaryOperationBuilder binaryOperation(OperatorType type) {
    return new BinaryOperationBuilder().type(type);
  }

  public static BlockBuilder block() {
    return new BlockBuilder();
  }

  public static BlockBuilder block(Object... statements) {
    BlockBuilder block = new BlockBuilder();
    for (Object st : statements) {
      block.add(st);
    }
    return block;
  }

  public static ConstantStatement constant(Object value) {
    if (value instanceof Class) {
      return classRef(value);
    } 
    if (value instanceof ConstantStatement) {
      return (ConstantStatement) value;
    }
    return new ConstantStatement(value);
  }

  public static ConstantStatement classRef(Object cls) {
    if (cls instanceof String) {
      return constant(toClassRef((String) cls));
    }
    if (cls instanceof Class) {
      return constant(toClassRef((Class) cls));
    }
    if (cls instanceof GoloParser.ParserClassRef) {
      return constant(cls);
    }
    throw new IllegalArgumentException("unknown type " + cls.getClass() + "to build a class reference");
  }

  public static ConstantStatement functionRef(Object funcName) {
    return functionRef(null, funcName);
  }

  public static ConstantStatement functionRef(Object moduleName, Object funcName) {
    return constant(new GoloParser.FunctionRef((String) moduleName, (String) funcName));
  }

  public static ReturnStatement returns(Object expr) {
    return new ReturnStatement(toExpression(expr));
  }

  public static LocalReferenceBuilder localRef() {
    return new LocalReferenceBuilder();
  }

  public static LocalReferenceBuilder localRef(LocalReference.Kind kind, String name, int index, boolean synthetic) {
    return localRef().kind(kind).name(name).index(index).synthetic(synthetic);
  }

  public static LocalReference externalRef(Object ref) {
    String refName;
    if (ref instanceof String) {
      refName = (String) ref;
    } else if (ref instanceof ReferenceLookup) {
      refName = ((ReferenceLookup) ref).getName();
    } else {
      throw new IllegalArgumentException("invalid type (" + ref.getClass() + ") for externalRef");
    }
    return new LocalReference(LocalReference.Kind.VARIABLE, refName, false);
  }

  public static ReturnStatement returnsVoid() {
    ReturnStatement ret = new ReturnStatement(constant(null));
    ret.returningVoid();
    return ret;
  }

  public static ReferenceLookup refLookup(String name) {
    return new ReferenceLookup(name);
  }

  public static FunctionInvocationBuilder functionInvocation() {
    return new FunctionInvocationBuilder();
  }

  public static FunctionInvocationBuilder functionInvocation(String name, boolean onRef, boolean onModule, boolean constant) {
    return functionInvocation().name(name).onReference(onRef).onModuleState(onModule).constant(constant);
  }

  public static MacroInvocationBuilder macroInvocation() {
    return new MacroInvocationBuilder();
  }

  public static MacroInvocationBuilder macroInvocation(String name) {
    return macroInvocation().name(name);
  }

  public static ConditionalBranchingBuilder branch() {
    return new ConditionalBranchingBuilder();
  }

  public static ConditionalBranchingBuilder branch(Object condition,
                                                   BlockBuilder trueBlock, BlockBuilder falseBlock,
                                                   ConditionalBranchingBuilder elseBranch) {
    return branch().condition(condition).whenTrue(trueBlock).whenFalse(falseBlock).elseBranch(elseBranch);
  }

  public static UnaryOperation unaryOperation(OperatorType type, ExpressionStatement expr) {
    return new UnaryOperation(type, expr);
  }

  public static CollectionLiteral collection(CollectionLiteral.Type type, ExpressionStatement... values) {
    return new CollectionLiteral(type, asList(values));
  }

  public static ThrowStatement throwException(Object expr) {
    return new ThrowStatement(toExpression(expr));
  }


  public static LoopBreakBuilder loopExit(LoopBreakFlowStatement.Type type) {
    return new LoopBreakBuilder().type(type).loop(LoopBuilder.currentLoop());
  }

  public static LoopBreakBuilder loopBreak() {
    return loopExit(LoopBreakFlowStatement.Type.BREAK);
  }

  public static LoopBreakBuilder loopContinue() {
    return loopExit(LoopBreakFlowStatement.Type.CONTINUE);
  }


  public static TryCatchBuilder tryCatchFinally() {
    return new TryCatchBuilder();
  }

  public static TryCatchBuilder tryCatchFinally(String exception, Object tryBlock, Object catchBlock, Object finallyBlock) {
    return tryCatchFinally()
      .exception(exception)
      .tryBlock(tryBlock)
      .catchBlock(catchBlock)
      .finallyBlock(finallyBlock);
  }

  public static BinaryOperation plus(Object left, Object right) {
    return binaryOperation(OperatorType.PLUS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation minus(Object left, Object right) {
    return binaryOperation(OperatorType.MINUS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation times(Object left, Object right) {
    return binaryOperation(OperatorType.TIMES, toExpression(left), toExpression(right));
  }

  public static BinaryOperation divide(Object left, Object right) {
    return binaryOperation(OperatorType.DIVIDE, toExpression(left), toExpression(right));
  }

  public static BinaryOperation modulo(Object left, Object right) {
    return binaryOperation(OperatorType.MODULO, toExpression(left), toExpression(right));
  }

  public static BinaryOperation equals(Object left, Object right) {
    return binaryOperation(OperatorType.EQUALS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation notEquals(Object left, Object right) {
    return binaryOperation(OperatorType.NOTEQUALS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation less(Object left, Object right) {
    return binaryOperation(OperatorType.LESS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation lessOrEquals(Object left, Object right) {
    return binaryOperation(OperatorType.LESSOREQUALS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation more(Object left, Object right) {
    return binaryOperation(OperatorType.MORE, toExpression(left), toExpression(right));
  }

  public static BinaryOperation moreOrEquals(Object left, Object right) {
    return binaryOperation(OperatorType.MOREOREQUALS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation logicalAnd(Object left, Object right) {
    return binaryOperation(OperatorType.AND, toExpression(left), toExpression(right));
  }

  public static BinaryOperation logicalOr(Object left, Object right) {
    return binaryOperation(OperatorType.OR, toExpression(left), toExpression(right));
  }

  public static UnaryOperation logicalNot(Object expr) {
    return unaryOperation(OperatorType.NOT, toExpression(expr));
  }

  public static BinaryOperation identity(Object left, Object right) {
    return binaryOperation(OperatorType.IS, toExpression(left), toExpression(right));
  }

  public static BinaryOperation difference(Object left, Object right) {
    return binaryOperation(OperatorType.ISNT, toExpression(left), toExpression(right));
  }

  public static BinaryOperation ofType(Object left, Object right) {
    return binaryOperation(OperatorType.OFTYPE, toExpression(left), toExpression(right));
  }

  public static BinaryOperation nullSafe(Object left, Object right) {
    return binaryOperation(OperatorType.ORIFNULL, toExpression(left), toExpression(right));
  }

  public static BinaryOperation methodCall(Object left, Object right) {
    return binaryOperation(OperatorType.METHOD_CALL, toExpression(left), toExpression(right));
  }


  public static FunctionDeclarationBuilder publicFunction() {
    return new FunctionDeclarationBuilder();
  }

  public static FunctionDeclarationBuilder localFunction() {
    return new FunctionDeclarationBuilder()
                .visibility(GoloFunction.Visibility.LOCAL);
  }

  public static FunctionDeclarationBuilder closureFunction() {
    return new FunctionDeclarationBuilder()
      .visibility(GoloFunction.Visibility.LOCAL).asClosure();
  }


  public static StructBuilder structure() {
    return new StructBuilder();
  }


  public static UnionBuilder unionType() {
    return new UnionBuilder();
  }

  public static TopLevelElements toplevel(Object... content) {
    TopLevelElements topLevel = new TopLevelElements();
    for (Object element : content) {
      topLevel.add(toGoloElement(element));
    }
    return topLevel;
  }


  public static AugmentationBuilder augmentType(String targetName) {
    return new AugmentationBuilder().target(targetName);
  }


  public static CaseBuilder caseBranch() {
    return new CaseBuilder();
  }
  

  public static MatchBuilder matching() {
    return new MatchBuilder();
  }


  public static ForEachBuilder forEachLoop(String name) {
    return new ForEachBuilder().variable(name);
  }

  public static ModuleImport moduleImport(String name) {
    return new ModuleImport(PackageAndClass.fromString(name));
  }
  
  // TODO: forLoop builder
  // TODO: whileLoop builder


}
