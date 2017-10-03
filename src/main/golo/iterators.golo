# ............................................................................................... #
#
# Copyright (c) 2012-2017 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# ............................................................................................... #

----
Utility function to work with iterator-like objects.

Iterator-like objects are object heaving the two following functions:

- `hasNext()`: returning `true` if an other element exists
- `next()`: returning the next element and changing the internal state (consume
  the element)

This corresponds to the `java.util.Iterator` object. However, for the functions
in this module, the object does not need to be an `Iterator` instance as long as
it provides the methods above.

This module contains several kinds of functions working on iterators:

- *reducing* functions that consume the iterator (aka fold or catamorphism), optionally
  to produce a single value (may also produce side-effects);
- *generating* functions that produce an iterator-like object (aka generator,
  unfold or anamorphism);
- *adapting* functions that produce a new iterator, wrapping the original, but
  adding behavior (e.g. `map` or `filter`).

The produced iterators (generating or adapting functions) are lazy, and can be
used to create or transform infinite iterators.

Most of these function are provided in curried form, returning a function
working on iterators. For instance:

    let mapper = gololang.Iterators.map(|x| -> x * 2)
    let i1 = mapper(list[1, 2, 3, 4]: iterator())
    i1: next() # 2
    i1: next() # 4

    let i2 = mapper(range(100, 666): iterator())
    i2: next() # 200
    i2: next() # 202

**WARNING** Since reducing functions consume the iterator, they *must not* be
used on infinite generator, since they can results in infinite loops.

Many of these functions are available through named augmentations which can be
applied on any compatible object, to provide better polymorphism.

See [`gololang.IterationAugmentations`](../IterationAugmentations.html)
----
module gololang.Iterators

import gololang.Functions

## Generating functions =======================================================

----
Function returning a generator.

A generator is a lazy iterator, using the provided functions to generate the
iterated values.

- *param* `unspool`: a function taking the last computed seed
   and returning a tuple containing the value to yield and the next seed
- *param* `finished`: a function taking the last computed seed
  and returning a boolean telling if the generation is finished
  (e.g. using ``|v| -> false`` creates an infinite iterator)
- `seed` initial value to start the generation
----
function generator = |unspool, finished, seed| ->
  gololang.Generator.generator(unspool, finished, seed)

----
The empty generator.
----
function empty = -> gololang.Generator.emptyGenerator!()

----
Returns a generator containing a single value.
----
function singleton = |x| -> generator(|s| -> match {
  when s is null then null
  otherwise [s, null]
}, `is(null), x)

----
Wraps a parameter-less function returning an iterator into an `Iterable` object.

Uses [asInterfaceInstance](../Predefined.html#asInterfaceInstance_2).
----
function asIterable = |f| -> asInterfaceInstance(java.lang.Iterable.class, f)

----
Function returning an `Iterable` that can be used in a `foreach` construct.
A new generator is created for each iteration.

*See also* [`generator`](#generator_3), [`asIterable`](#asIterable_1)
----
function iterable = |unspool, finished, seed| -> asIterable(->
  gololang.Generator.generator(unspool, finished, seed))


----
Chain several iterators.
----
function chain = |iters...| {
  let iterators = iters: iterator()
  if not iterators: hasNext() {
    return empty()
  }
  return itererator(
    unspool=|seed| {
      if seed: hasNext() {
        return [seed: next(), seed]
      }
      while iterators: hasNext() {
        let g = iterators: next()
        if g: hasNext() {
          return [g: next(), g]
        }
      }
      return null
    },
    finished=|seed| -> not (seed: hasNext() or iterators: hasNext()),
    seed=iterators: next()
  )
}

----
Prepends a value to an iterator.
----
function cons = |value, iter| -> chain(singleton(value), iter)

----
Produces a infinite generator of values. If the argument is a closure, it must have
no parameters, and it's used to produce the values (called for each `next`
call).

For instance, `repeat(5)` will return an infinite generator of `5`s, and
`repeat(^f)` will return a infinite generator yielding calls to `f`
(f(), f(), f(), ...)

- *param* `value`: a value, a closure, or a `java.util.function.Supplier`

TODO: Only values, closures are dealt with `generate`
----
function repeat = |value| -> match {
  when isClosure(value) then generator(|seed| -> [value(), null], ^False, null)
  when value oftype java.util.function.Supplier.class then generator(
    |seed| -> [value: get(), null], ^False, null)
  otherwise generator(|seed| -> [value, null], ^False, null)
}

----
Returns an infinite generator yielding iterative application of a function
to an initial element.
`iterate(z, f)` thus yields `z, f(z), f(f(z)), ...`

For instance, one can create a infinite integer generator using:

    iterate(0, |x| -> x + 1)


- *param* `zero`: the initial element of the list
- *param* `func`: the function to apply
----
function iterate = |zero, func| -> generator(|seed| -> [seed, func(seed)], ^False, zero)

function counter = |start| -> iterate(start, ^succ)

function zip = |i1, i2| -> generator(
  |s| -> [[s: get(0): next(), s: get(1): next()], s],
  |s| -> s: get(0): hasNext() and s: get(1): hasNext(),
  [iterator(i1), iterator(i2)]
)

----
Returns an iterator counting its elements.

The elements of the iterator are tuple containing the index and the value

For instance

    let it = enumerate(range('a', 'f'): iterator())
    it: next() # [0, 'a']
    it: next() # [1, 'b']
    it: next() # [2, 'c']

----
function enumerate = |iterator| -> generator(
  seed=[0, iterator],
  finished=|s| -> not s: get(1): hasNext(),
  unspool=|s| -> [
    [idx, it: next()],
    [idx + 1, it]
  ] with {
    idx, it = s
  })




## Reducing functions =========================================================

----
Checks whether all the elements satisfy the predicate.

This is a short-circuiting reducing function.

- *param* `pred`: a predicate function, taking an element and returning a boolean
- *returns* `true` if the predicate holds for all elements
----
function all = |predicate| -> |iter| {
  while iter: hasNext() {
    if not predicate(iter: next()) {
      return false
    }
  }
  return true
}

----
Checks whether the given element is present.

This is a short-circuiting reducing function.

- *param* `elt`: the searched element
- *returns* `true` if the iterator contains the given element
----
function contains = |element| -> |iter| {
  while iter: hasNext() {
    if iter: next() == element {
      return true
    }
  }
  return false
}

----
Counts the number of elements that satisfy a predicate:

This is a reducing function.

- *param* `pred`: a predicate function, taking an element and returning a boolean
- *returns* the number of elements matching the predicate
----
function count = |pred| -> reduce(0, |acc, elt| -> match {
    when pred(elt) then acc + 1
    otherwise acc
  })

----
Applies a function over each element

This is a reducing function.

- *param* `func`: the function to apply, taking the current element as a
  parameter. No value is returned, `func` *must* work by side effect.
----
function each = |func| -> |iter| {
  while iter: hasNext() {
    func(iter: next())
  }
}

----
Checks whether any element satisfied a predicate:

This is a short-circuiting reducing function.

- *param* `pred`: a predicate function, taking an element and returning a boolean.
----
function exists = |pred| -> |iter| {
  while iter: hasNext() {
    if pred(iter: next()) {
      return true
    }
  }
  return false
}

----
Finds the first element matching a predicate.

This is a short-circuiting reducing function.

- *param* `pred`: a predicate function, taking an element and returning a boolean
- *returns* `null` if no element satisfies `pred`, the first one otherwise
----
function find = |pred| -> |iter| {
  while iter: hasNext() {
    let val = iter: next()
    if pred(val) {
      return val
    }
  }
  return null
}

----
Returns the number of elements in this iterator.

This is a reducing function.
----
function size = |iter| {
  var i = 0
  while iter: hasNext() {
    i = i + 1
    iter: next()
  }
  return i
}

----
Returns the last element of the iterator.

This is a reducing function.

- *returns* null if the iterator is empty
----
function last = |iter| {
  var elt = null
  while iter: hasNext() {
    elt = elt: next()
  }
  return elt
}

----
Returns the last element of the iterator as an option.

This is a reducing function.

This is not the same behavior than `Optional.ofNullable(last(iter))`, since in
this latter case, a non-empty iterator whose last element is `null` will result
in an empty option, whereas this function will return a non-empty option
containing a null value.

- *returns* an `Optional` of the last element.
----
function lastOpt = |iter| {
  if not iter: hasNext() {
    return java.util.Optional.empty()
  }
  return java.util.Optional.of(last(iter))
}

----
Advance to the `i`th element, starting from 0.

This is a reducing function. The iterator is consumed up to the given index.

For instance:

```golo
next(list['a', 'b', 'c', 'd', 'e']: iterator(), 3) == 'd'
```

`next(0)` is the same as `next()`.

- *param* `i` the index to advance to
- *throws* `java.lang.IndexOutOfBoundsException` if `i` is negative or more than
  the size of the iterator.
----
function next = |iter, i| {
  if i < 0 {
    throw IndexOutOfBoundsException("Index: 0")
  }
  var n = 0
  while n < i - 1 and iter: hasNext() {
    iter: next()
    n = n + 1
  }
  if not iter: hasNext() {
    throw IndexOutOfBoundsException("Index: " + n)
  }
  return iter: next()
}

----
Optionally return the next element.

This is not the same behavior than `Optional.ofNullable(iter: next())`, since in
this latter case, a non-empty iterator whose next element is `null` will result
in an empty option, whereas this function will return a non-empty option
containing a null value.

- *returns* an `Optional` of the next element
----
function get = |iter| {
  if not iter: hasNext() {
    return java.util.Optional.empty()
  }
  try {
    return java.util.Optional.of(iter: next())
  } catch (e) {
    if e oftype java.util.NoSuchElementException.class {
      return java.util.Optional.empty()
    }
    throw e
  }
}

----
Partition an iterator given the given predicate.

Returns a couple whose first element is a collection containing the iterator
elements for which the predicate was `true`, and the second element is a
collection containing the iterator elements for which the predicate was `false`.

The kind of collection is defined by the provided factory function.

This is a simplified version of reducing/collecting with a partitioning
collector, and thus is a reducing function.

# Usage example

```golo
let iter = list[1, 2, 3, 4, 5, 6]: iterator()
let even, odd = partition(iter, |x| -> x % 2 == 0, -> vector[])
```

`even` is a vector containing even values, and `odd` is a vector containing odd
values.
----
function partition = |iter, predicate, collectionFactory| {
  let t = collectionFactory()
  let f = collectionFactory()
  while iter: hasNext() {
    let v = iter: next()
    if predicate(v) {
      t: add(v)
    } else {
      f: add(v)
    }
  }
  return [t, f]
}

----
Partitioning reduction.

Returns a tuple whose first element is the reduction using `reduceTrue` of the
iterator elements for which `predicate` is `true`, and the second element is the
reduction using `reduceFalse` of the iterator elements for which `predicate` is
`false`
----
function partition = |iter, predicate, reducerTrue, reducerFalse| {
  var t = reducerTrue: init()
  var f = reducerFalse: init()
  while iter: hasNext() {
    let v = iter: next()
    if predicate(v) {
      t = reducerTrue: accumulate(t, v)
    } else {
      f = reducerFalse: accumulate(f, v)
    }
  }
  return [reducerTrue: finish(t), reducerTrue: finish(f)]
}
