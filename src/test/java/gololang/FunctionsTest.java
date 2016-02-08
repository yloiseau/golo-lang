/*
 * Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package gololang;

import org.testng.annotations.Test;
import org.eclipse.golo.internal.testing.GoloTest;
import org.eclipse.golo.internal.testing.TestUtils;
import org.eclipse.golo.compiler.GoloClassLoader;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class FunctionsTest extends GoloTest {

  @Override
  protected String srcDir() {
    return "for-test/";
  }

  /* .............................................................. */
  public interface UnarySAM {
    Integer call(Integer v);
  }

  public interface Invokable {
    Object invoke(Object... arguments) throws Throwable;
  }

  @FunctionalInterface
  public interface VarargsFI {
    Integer call(Integer... arguments) throws Throwable;

    default int jumpstreet() { return 21; }
  }

  @FunctionalInterface
  public interface UnaryFI {
    Integer apply(Integer v);

    default int jumpstreet() { return 21; }
  }

  /* .............................................................. */
  public static Object callGoloFunction(FunctionReference f) throws Throwable {
    return f.invoke(42);
  }

  public static Object callInvokable(Invokable f) throws Throwable {
    return f.invoke(42);
  }

  public static Object callInvokableVar(Invokable f) throws Throwable {
    return f.invoke(42, 0);
  }

  public static Object callVarargsFI(VarargsFI f) throws Throwable {
    return f.call(21, 0) + f.jumpstreet();
  }

  public static Object callSAM(UnarySAM o) {
    return o.call(42);
  }

  public static Object callFI(UnaryFI f) {
    return f.apply(21) + f.jumpstreet();
  }

  /* .............................................................. */
  public static UnaryFI identityFI() {
    return (x) -> x;
  }

  public static VarargsFI identityVarFI() {
    return (Integer... xs) -> xs[0];
  }

  public static UnarySAM identitySAM() {
    return new UnarySAM() {
      @Override
      public Integer call(Integer v) {
        return v;
      }
    };
  }

  public static Invokable identityInvoke() {
    return new Invokable() {
      @Override
      public Object invoke(Object... args) {
        return args[0];
      }
    };
  }

  /* .............................................................. */

  @Test
  public void testJavaHOF() throws Throwable {
    // Thread.currentThread().getContextClassLoader();
    GoloClassLoader l = new GoloClassLoader(Thread.currentThread().getContextClassLoader());
    TestUtils.runTests("src/test/resources/for-test/", "java-hof.golo", l);
    // run("java-hof");

  }
}
