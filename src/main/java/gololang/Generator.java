/*
 * Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package gololang;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.Optional;
import static gololang.Predefined.require;
import static gololang.Predefined.requireNotNull;

/**
 * A generator object.
 *
 * <p>A generator is an iterator that compute its next value, maintaining an internal state.
 *
 * <p>The {@code unspool} function use the internal state (the seed) to generate the next value
 * of the iterator as well as the new seed that will be stored into the internal state.
 *
 * <p>The iterator generate values until the predicate function {@code finished} holds for the <em>seed</em> (not the
 * returned value). Using {@code gololang.Functions::False} creates an infinite iterator.
 *
 * <p>Generators are lazy, values are only computed when required by the {@link #next()} method.
 * When a generator is traversed, values are consumed, and a new generator
 * must be created to iterate again. Since a generator is usually traversed only once, the generated values are not
 * cached, contrary to a {@link LazyList}.
 *
 * <p>A generator can be {@link #close()}ed. A closed generator has no next element ({@link #hasNext()} always returns
 * {@code false} and {@link #next()} throws a {@code java.util.NoSuchElementException}. Moreover, if a function was
 * registered with {@link #onClose(FunctionReference)}, it is called with the current seed before closing the generator.
 * This can be used for instance when creating a generator using a resource that must be closed after use (e.g. a file).
 *
 * <p>The internal state of a generator can be reseted using its {@link #send(Object)} method. This can be used to emulate
 * coroutines for instance.
 *
 * TODO: update the since wrt the merged branch
 * @since 3.3
 * @see LazyList
 */
public class Generator implements Iterator<Object>, AutoCloseable, Supplier<Optional<Object>> {
  private final FunctionReference unspool;
  private final FunctionReference finished;
  private FunctionReference onClose;
  private Object currentSeed;
  private Object currentValue;
  private boolean isClosed = false;

  private Generator(FunctionReference unspool, FunctionReference finished, Object seed, Object value, boolean closed) {
    this.currentSeed = seed;
    this.currentValue = value;
    this.unspool = unspool;
    this.finished = finished;
    this.isClosed = closed;
  }

  private static final Generator EMPTY = new Generator(null, null, null, null, true) {
    @Override
    public Generator onClose(FunctionReference handler) {
      throw new UnsupportedOperationException("empty generator can't define a close behavior");
    }

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public Object next() {
      throw new NoSuchElementException();
    }

    @Override
    public Object send(Object newSeed) {
      throw new UnsupportedOperationException("empty generators can't be sent seeds");
    }
  };

  /**
   * Creates a new generator.
   *
   * @param unspool function used to generate values, of the type {@code |currentSeed| -> [nextValue, nextSeed]}.
   * @param finished predicate function used to test if the generator is exhausted or not, of the type {@code |currentSeed| -> isFinised}
   * @param seed the initial seed to use.
   */
  public static Generator generator(FunctionReference unspool, FunctionReference finished, Object seed) {
    requireNotNull(unspool);
    requireNotNull(finished);
    require(unspool.acceptArity(1), "`unspool` must take the seed as parameter");
    require(finished.acceptArity(1), "`finished` must take the seed as parameter");
    return new Generator(unspool, finished, seed, null, false);
  }

  /**
   * Creates an empty closed generator.
   */
  public static Generator emptyGenerator() {
    return EMPTY;
  }

  /**
   * Fork this generator.
   *
   * <p>Creates a new generator in the same state such that they raise the same values but can now be advanced independently.
   * For instance:
   * <pre class="listing"><code class="lang-golo" data-lang="golo">
   * let g = generator(|seed| -> [seed, seed + 1], -> false, 0)
   *
   * require(g: next() == 0, "err")
   * require(g: next() == 1, "err")
   * require(g: next() == 2, "err")
   *
   * let h = g: fork()
   * require(h: next() == 3, "err")
   * require(h: next() == 4, "err")
   * require(g: next() == 3, "err")
   * require(g: next() == 4, "err")
   * </code></pre>
   *
   * @return an independent generator in the same state as this one.
   */
  public Generator fork() {
    return new Generator(this.unspool, this.finished, this.currentSeed, this.currentValue, this.isClosed);
  }

  /**
   * Defines the function to call when closing the generator.
   * <p>
   * @param handler the function to call on the current seed when the generator is {@link #close()}ed
   */
  public Generator onClose(FunctionReference handler) {
    this.onClose = handler;
    return this;
  }

  private Tuple unspool() {
    if (this.isClosed) {
      return null;
    }
    try {
      return (Tuple) (unspool.invoke(this.currentSeed));
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @inheritDoc
   *
   * <p>This method calls the {@code finished} function on the current seed.
   * <p>If {@code finished} raises an exception, this method returns
   * {@code false}.
   */
  @Override
  public boolean hasNext() {
    if (this.isClosed) {
      return false;
    }
    try {
      return !((Boolean) this.finished.invoke(this.currentSeed));
    } catch (Throwable e) {
      return false;
    }
  }

  public boolean isEmpty() {
    return !hasNext();
  }

  /**
   * @inheritDoc
   *
   * <p>This method calls the {@code unspool} function on the current seed, store the second element of the returned
   * tuple, and return the first one.
   * <p>If {@code unspool} raises an exception, it is reraised (wrapped in a runtime exception if needed).
   *
   * @return the next element of the generator
   * @throws NoSuchElementException if the {@code unspool} function returns {@code null}
   */
  @Override
  public Object next() {
    Tuple res = unspool();
    if (res == null) {
      throw new NoSuchElementException();
    }
    this.currentValue = res.get(0);
    this.currentSeed = res.get(1);
    return currentValue;
  }

  /**
   * @inheritDoc
   */
  @Override
  public Optional<Object> get() {
    if (!hasNext()) {
      return Optional.empty();
    }
    try {
      return Optional.of(next());
    } catch (NoSuchElementException e) {
      return Optional.empty();
    }
  }

  /**
   * @inheritDoc
   *
   * The function defined by {@link #onClose(FunctionReference)} is called with the current seed.
   */
  @Override
  public void close() {
    if (!isClosed && onClose != null) {
      try {
        onClose.invoke(this.currentSeed);
      } catch (Throwable ignored) {
        // ignore
      }
    }
    this.isClosed = true;
    this.currentSeed = null;
    this.currentValue = null;
  }

  /**
   * Defines a new seed and return the corresponding next value.
   *
   * <p>If the generator was closed, re-opens it.
   *
   * <p>For instance:
   * <pre class="listing"><code class="lang-golo" data-lang="golo">
   * let g = generator(|seed| -> [seed, seed + 1], -> false, 0)
   *
   * require(g: next() == 0, "err")
   * require(g: next() == 1, "err")
   * require(g: next() == 2, "err")
   * require(g: send(40) == 40, "err")
   * require(g: next() == 41, "err")
   * require(g: next() == 42, "err")
   * </code></pre>
   *
   * @param newSeed the object defining the new internal state
   */
  public Object send(Object newSeed) {
    this.currentSeed = newSeed;
    this.isClosed = false;
    return this.next();
  }

  /**
   * Returns the current seed.
   */
  public Object seed() {
    return this.currentSeed;
  }

  /**
   * Returns the current value.
   */
  public Object current() {
    return this.currentValue;
  }

  public Object invoke(Object... args) {
    if (args.length == 0) {
      if (hasNext()) {
        return this.next();
      }
      throw new StopIteration();
    }
    if (args.length == 1) {
      return this.send(args[0]);
    }
    throw new IllegalArgumentException("Generators can only be invoked with 0 or 1 argument");
  }

  /**
   * Destructuring helper.
   *
   * @return a tuple of the next element and the generator itself
   */
  public Tuple destruct() {
    if (!hasNext()) {
      return new Tuple(null, this);
    }
    return new Tuple(next(), this);
  }
}
