/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package gololang.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @OptionalParameters} is used to define optional parameters.
 *
 * Mainly used for internal stuff, this annotation can be used to create functions with optional parameters in Java
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface OptionalParameters {
  /**
   * Represents the 0-based index of the first optional parameter.
   *
   * For instance, in the method:
   * <pre><code class="lang-java" data-lang="java">
   * @OptionalParameters(1)
   * public static Object foo(Object a, Object b, Object c) {
   *  // ...
   * }
   * </code></pre>
   * the {@code b} and {@code c} parameters will be optionals, and thus this method can be called in golo as:
   * <pre><code class="lang-golo" data-lang="golo">
   * foo("plop")
   * foo("plop", 1)
   * foo("plop", 1, list[])
   * </code><pre>
   * which will be equivalent as
   * <pre><code class="lang-golo" data-lang="golo">
   * foo("plop", null, null)
   * foo("plop", 1, null)
   * foo("plop", 1, list[])
   * </code><pre>
   */
  int value();
}
