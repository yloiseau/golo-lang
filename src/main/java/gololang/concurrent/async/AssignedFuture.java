/*
 * Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package gololang.concurrent.async;

/**
 * Convenience implementation for pre-set futures.
 */
public final class AssignedFuture implements Future {

  private AssignedFuture(Object value) {
    this.value = value;
  }

  private final Object value;

  /**
   * Builds a new future that has been set to a value.
   *
   * @param value the future value.
   * @return a new future object.
   */
  public static AssignedFuture setFuture(Object value) {
    return new AssignedFuture(value);
  }

  /**
   * Builds a new future that has failed.
   *
   * @param throwable the failure.
   * @return a new future object.
   */
  public static AssignedFuture failedFuture(Throwable throwable) {
    return new AssignedFuture(throwable);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object get() {
    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object blockingGet() throws InterruptedException {
    return value;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isResolved() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isFailed() {
    return value instanceof Throwable;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future onSet(Observer observer) {
    if (!isFailed()) {
      observer.apply(value);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future onFail(Observer observer) {
    if (isFailed()) {
      observer.apply(value);
    }
    return this;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return String.format("AssignedFuture{value=%s}", value);
  }
}
