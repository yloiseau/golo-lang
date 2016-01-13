/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.golo.compiler;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import org.testng.annotations.Test;

import org.eclipse.golo.compiler.ir.*;
import org.eclipse.golo.runtime.OperatorType;
import static org.eclipse.golo.compiler.ir.Builders.*;

public class OptimisationIrVisitorTest {

  private void refine(GoloElement elt) {
    elt.accept(new SugarExpansionVisitor());
    elt.accept(new OptimisationIrVisitor());
    elt.accept(new ClosureCaptureGoloIrVisitor());
    elt.accept(new LocalReferenceAssignmentAndVerificationVisitor());
  }

  @Test
  public void test_operator() {
    GoloFunction f = functionDeclaration("foo").withParameters("x").block(
      define(localRef("i")).as(binaryOperation(OperatorType.PLUS, binaryOperation(OperatorType.TIMES, constant(320), constant(42)), constant(14))),
      define(localRef("j")).as(binaryOperation(OperatorType.TIMES, constant(2), refLookup("i"))),
      define(localRef("k")).as(binaryOperation(OperatorType.TIMES, constant(0), refLookup("x"))),
      returns(binaryOperation(OperatorType.PLUS, binaryOperation(OperatorType.PLUS, refLookup("i"), refLookup("j")), refLookup("k")))
    );
    refine(f);
    List<GoloStatement<?>> statements = f.getBlock().getStatements();

    // assertThat(statements.size(), is(2))
    ReturnStatement r = (ReturnStatement) statements.get(statements.size() - 1);
    assertThat(r.getExpressionStatement(), instanceOf(BinaryOperation.class));
    BinaryOperation b = (BinaryOperation) r.getExpressionStatement();
    assertThat(b.getType(), is(OperatorType.PLUS));
    assertThat(b.getLeftExpression(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) b.getLeftExpression()).getValue(), is(40362));
    assertThat(b.getRightExpression(), instanceOf(ReferenceLookup.class));
    assertThat(((ReferenceLookup) b.getRightExpression()).getName(), is("k"));
  }

  @Test
  public void test_return_constant() {
    GoloFunction f = functionDeclaration("foo").block(
        define(localRef("a")).as(constant(21)),
        define(localRef("t")).as(constant(0)),
        returns(refLookup("a")));
    refine(f);
    List<GoloStatement<?>> statements = f.getBlock().getStatements();

    // assertThat(statements.size(), is(1));
    ReturnStatement r =  (ReturnStatement) statements.get(statements.size() - 1);
    assertThat(r.getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) r.getExpressionStatement()).getValue(), is(21));
  }

  @Test
  public void test_string_constants() {
    GoloFunction f = functionDeclaration("foo").block(
        define(localRef("a")).as(binaryOperation(OperatorType.TIMES, constant(21), constant(2))),
        define(localRef("b")).as(
          binaryOperation(OperatorType.PLUS,
            binaryOperation(OperatorType.PLUS,
              binaryOperation(OperatorType.PLUS, constant("the answer"), constant(" is ")),
              refLookup("a")),
            constant("!"))),
        returns(refLookup("b")));
    refine(f);
    List<GoloStatement<?>> statements = f.getBlock().getStatements();

    // assertThat(statements.size(), is(1));
    ReturnStatement r = (ReturnStatement) statements.get(statements.size() - 1);
    assertThat(r.getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) r.getExpressionStatement()).getValue(),
        is("the answer is 42!"));
  }

  /**
   * Dead branch elimination.
   * <p>
   * <pre><code>
   * function foo = {
   *   if false {
   *     println("err")
   *   } else {
   *     println("ok")
   *   }
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * function foo = {
   *   println("ok")
   * }
   * </code></pre>
   */
  @Test
  public void test_remove_branch_false() {
    GoloFunction f = functionDeclaration("foo").block(
      branch().condition(constant(false))
        .whenTrue(block(
          call("println").withArgs(constant("err"))))
        .whenFalse(block(
          call("println").withArgs(constant("ok")))));
    refine(f);
    List<GoloStatement<?>> statements = f.getBlock().getStatements();

    assertThat(statements.size(), is(2));
    assertThat(statements.get(0), instanceOf(FunctionInvocation.class));
    FunctionInvocation i = (FunctionInvocation) statements.get(0);
    assertThat(i.getName(), is("println"));
    assertThat(i.getArguments().size(), is(1));
    assertThat(((ConstantStatement) i.getArguments().get(0)).getValue(), is("ok"));
  }

  /**
   * Dead branch elimination.
   * <p>
   * <pre><code>
   * function foo = {
   *   if true {
   *     println("err")
   *   } else {
   *     println("ok")
   *   }
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * function foo = {
   *   println("err")
   * }
   * </code></pre>
   */
  @Test
  public void test_remove_branch_true() {
    GoloFunction f = functionDeclaration("foo").block(
      branch().condition(constant(true))
        .whenTrue(block(
          call("println").withArgs(constant("err"))))
        .whenFalse(block(
          call("println").withArgs(constant("ok")))));
    refine(f);
    List<GoloStatement<?>> statements = f.getBlock().getStatements();

    assertThat(statements.size(), is(2));
    assertThat(statements.get(0), instanceOf(FunctionInvocation.class));
    FunctionInvocation i = (FunctionInvocation) statements.get(0);
    assertThat(i.getName(), is("println"));
    assertThat(i.getArguments().size(), is(1));
    assertThat(((ConstantStatement) i.getArguments().get(0)).getValue(), is("err"));
  }

  /**
   * Module constant propagation and dead branch elimination.
   * <p>
   * <pre><code>
   * module Test
   *
   * let DEBUG = false
   *
   * function foo = {
   *   if DEBUG {
   *     println("plop")
   *   }
   *   return 42
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * module Test
   *
   * let DEBUG = false
   *
   * function foo = {
   *   return 42
   * }
   * </code></pre>
   */
  @Test
  public void test_module_state_condition_false() {
    GoloModule m = module("Test");
    m.addModuleStateInitializer(define(localRef("DEBUG").moduleLevel()).as(constant(false)));
    Block b = block(
      branch().condition(refLookup("DEBUG"))
        .whenTrue(block(
            call("println").withArgs(constant("plop")))),
      returns(constant(42)));
    m.addFunction(functionDeclaration("foo").block(b));
    refine(m);
    List<GoloStatement<?>> statements = b.getStatements();
    assertThat(statements.size(), is(2));
    assertThat(statements.get(0), instanceOf(Noop.class));
    assertThat(statements.get(1), instanceOf(ReturnStatement.class));
    assertThat(((ReturnStatement) statements.get(1)).getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) ((ReturnStatement) statements.get(1)).getExpressionStatement()).getValue(), is(42));
  }

  /**
   * Module constant propagation and dead branch elimination.
   * <p>
   * <pre><code>
   * module Test
   *
   * let DEBUG = true
   *
   * function foo = {
   *   if DEBUG {
   *     println("plop")
   *   }
   *   return 42
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * module Test
   *
   * let DEBUG = true
   *
   * function foo = {
   *   println("plop")
   *   return 42
   * }
   * </code></pre>
   */
  @Test
  public void test_module_state_condition_true() {
    GoloModule m = module("Test");
    m.addModuleStateInitializer(define(localRef("DEBUG").moduleLevel()).as(constant(true)));
    Block b = block(
      branch().condition(refLookup("DEBUG"))
        .whenTrue(block(
            call("println").withArgs(constant("plop")))),
      returns(constant(42)));
    m.addFunction(functionDeclaration("foo").block(b));
    refine(m);
    List<GoloStatement<?>> statements = b.getStatements();

    assertThat(statements.size(), is(2));
    assertThat(statements.get(1), instanceOf(ReturnStatement.class));
    assertThat(((ReturnStatement) statements.get(1)).getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) ((ReturnStatement) statements.get(1)).getExpressionStatement()).getValue(), is(42));

    assertThat(statements.get(0), instanceOf(FunctionInvocation.class));
    FunctionInvocation i = (FunctionInvocation) statements.get(0);
    assertThat(i.getName(), is("println"));
    assertThat(i.getArguments().size(), is(1));
    assertThat(((ConstantStatement) i.getArguments().get(0)).getValue(), is("plop"));
  }

  @Test
  public void test_prune_block() {
    Block b = block(
        returns(constant(42)),
        call("println").withArgs(constant("plop")));
    refine(functionDeclaration("foo").block(b));
    List<GoloStatement<?>> statements = b.getStatements();
    assertThat(statements.get(1), instanceOf(Noop.class));
  }

  @Test
  public void test_prune_block_cond() {
    Block b = block(
        branch().condition(refLookup("c"))
          .whenTrue(block(returns(constant(42))))
          .whenFalse(block(returns(constant(1337)))),
        call("println").withArgs(constant("plop")));
    refine(functionDeclaration("foo").withParameters("c").block(b));
    List<GoloStatement<?>> statements = b.getStatements();
    assertThat(statements.get(1), instanceOf(Noop.class));
  }

  @Test
  public void test_prune_block_loop() {
    Block b = block(
        foreach().var("x").on(list(constant(1))).block(
          block(returns(refLookup("x")))),
        call("println").withArgs(constant("plop")));
    refine(functionDeclaration("foo").block(b));
    List<GoloStatement<?>> statements = b.getStatements();
    assertThat(statements.get(1), instanceOf(Noop.class));
  }

  /**
   * Replace return on ref by expression.
   * <p>
   * <pre><code>
   * function foo = |x| {
   *   var r = 0
   *   if x {
   *     return r
   *   }
   *   r = 42
   *   return r
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * function foo = |x| {
   *   if x {
   *     return 0
   *   }
   *   return 42
   * }
   * </code></pre>
   */
  @Test
  public void test_replace_return_on_ref() {
    Block b = block();
    ReturnStatement fst = returns(refLookup("r"));
    ReturnStatement snd = returns(refLookup("r"));
    b.add(define(localRef("r").variable()).as(constant(0)));
    b.add(branch().condition(refLookup("x"))
        .whenTrue(block(fst)));
    b.add(assign(constant(42)).to(localRef("r")));
    b.add(snd);
    GoloFunction f = functionDeclaration("foo").withParameters("x").block(b);
    refine(f);

    // TODO: assertThat(b.getStatements().size(), is(2));
    assertThat(fst.getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(snd.getExpressionStatement(), instanceOf(ConstantStatement.class));
    assertThat(((ConstantStatement) fst.getExpressionStatement()).getValue(), is(0));
    assertThat(((ConstantStatement) snd.getExpressionStatement()).getValue(), is(42));
  }

  /**
   * Push up returns.
   * <p>
   * <pre><code>
   * function foo = |x| {
   *   var r = 42
   *   if x {
   *     r = 0
   *   }
   *   return r
   * }
   * </code></pre>
   * becomes
   * <pre><code>
   * function foo = |x| {
   *   if x {
   *     return 0
   *   }
   *   return 42
   * }
   * </code></pre>
   */
  @Test
  public void test_push_up_return() {
    GoloFunction f = functionDeclaration("foo").withParameters("x").block(
        define(localRef("r").variable()).as(constant(42)),
        branch().condition(refLookup("x"))
          .whenTrue(block(assign(constant(0)).to(localRef("r")))),
        returns(refLookup("r")));
    refine(f);

    // TODO

  }
}
