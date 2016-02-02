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

import java.util.function.*;


public class FunctionsTest extends GoloTest {

  public interface UnarySAM {
    Integer call(Integer v);

    default int plop() { return 42; }
  }

  public interface Invokable {
    Object invoke(Object... arguments) throws Throwable;
  }

  @FunctionalInterface
  public interface VarargsFI {
    Object call(Object... arguments) throws Throwable;
  }

  @Override
  protected String srcDir() {
    return "for-test/";
  }

  /* .............................................................. */
  public static Object callGoloFunction(FunctionReference f) throws Throwable {
    return f.invoke(42);
  }

  public static Object callInvokable(Invokable f) throws Throwable {
    return f.invoke(42);
  }

  public static Object callSAM(UnarySAM o) {
    return o.call(42);
  }

  public static Object callFunction(Function<Object, Object> f) {
    return f.apply(42);
  }

  public static Function<Object, Object> identityFunction() {
    return (x) -> x;
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
    run("java-hof");
  }
}
