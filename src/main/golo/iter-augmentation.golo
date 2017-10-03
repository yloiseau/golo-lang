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
This module defines named augmentations for internal iterations on iterator and
iterable-like objects.
----
module gololang.IterationAugmentations

import gololang.Iterators
import gololang.Functions

----
Mixin augmentation that can be applied to any type with a `iterator` method that
returns an iterator-like object, even if the type does not implement `java.lang.Iterable`.

The methods delegate on the functions in
[`gololang.Iterators`](../Iterators.html), using the iterator-like object
returned by `iterator()`. See the module for documentation on these methods.

**required methods**:

- `iterator(this)`: returning an object with `hasNext()` and `next()` method.

*See also* [`gololang.Iterators`](../Iterators.html)
----
augmentation InternalIterable = {
  # NOTE
  #
  # In this augmentation, we use the `gololang.Iterators` functions instead of
  # delegating on the produced iterator, since this augmentation can be applied
  # to *any type* having a `iterator` method returning *any type* with `hasNext`
  # and `next` methods. As this returned type is not guaranteed to have been
  # augmented with the `InternalIterator` augmentation, we can't be sure the
  # result of `iterator` will have the required methods.

  function all = |this, pred| -> gololang.Iterators.all!(pred)(this: iterator())
  function contains = |this, elt| -> gololang.Iterators.contains!(elt)(this: iterator())
  function count = |this, pred| -> gololang.Iterators.count!(pred)(this: iterator())
  function each = |this, func| {
    gololang.Iterators.each!(func)(this: iterator())
    return this
  }
  function exists = |this, pred| -> gololang.Iterators.exists!(pred)(this: iterator())
  function find = |this, pred| -> gololang.Iterators.find!(pred)(this: iterator())


  ----
  General purpose reducing operation:

      let data = list[1, 2, 3, 4, 5]
      println("sum = " + data: reduce(0, |acc, next| -> acc + next))

  * `initialValue`: the initial accumulator value for the reducing operation.
  * `func`: the function to apply over an accumulator and the next value.
  ----
  function reduce = |this, initialValue, func| -> gololang.Iterators.reduce(
      this: iterator(),
      initialValue,
      func)


  ----
  Join the elements into a string, separated by `separator`.
  If there is no elements, `""` is returned.

  * `separator`: the element separator string.
  ----
  function join = |this, separator| -> gololang.Iterators.join(this: iterator(), separator)

  ----
  Maps elements using a function:

      println(list[1, 2, 3]: map(|n| -> n * 10))

  * `func`: a transformation function.
  ----
  function map = |this, func| -> gololang.Iterators.asIterable(-> gololang.Iterators.map(this: iterator(), func))

  ----
  Filters elements based on a predicate:

      println(list[1, 2, 3, 4]: filter(|n| -> (n % 2) == 0))

  * `pred`: a predicate function taking an element and returning a boolean.
  ----
  function filter = |this, pred| -> gololang.Iterators.asIterable(-> gololang.Iterators.filter(this: iterator(), pred))

  function dropWhile = |this, pred| -> gololang.Iterators.asIterable(-> gololang.Iterators.dropWhile(this: iterator(), pred))

  function drop = |this, nb| -> gololang.Iterators.asIterable(-> gololang.Iterators.drop(this: iterator(), nb))

  function take = |this, nb| -> gololang.Iterators.asIterable(-> gololang.Iterators.take(this: iterator(), nb))

  function takeWhile = |this, pred| -> gololang.Iterators.asIterable(-> gololang.Iterators.takeWhile(this: iterator(), pred))
}

###############################################################################
augmentation InternalIterator = {

  function isEmpty = |this| -> not this: hasNext()

  function get = |this| -> match {
    when this: hasNext() then Some(this: next())
    otherwise None()
  }

  function all = |this, pred| -> gololang.Iterators.all!(pred)(this)
  function contains = |this, elt| -> gololang.Iterators.contains!(elt)(this)
  function count = |this, pred| -> gololang.Iterators.count!(pred)(this)
  function each = |this, func| {
    gololang.Iterators.each!(func)(this)
  }
  function exists = |this, pred| -> gololang.Iterators.exists!(pred)(this)
  function find = |this, pred| -> gololang.Iterators.find!(pred)(this)

  ----
  General purpose reducing operation:

      println("sum = " + iterator: reduce(0, |acc, next| -> acc + next))

  * `initialValue`: the initial accumulator value for the reducing operation.
  * `func`: the function to apply over an accumulator and the next value.
  ----
  function reduce = |this, initialValue, func| -> gololang.Iterators.reduce(
      this,
      initialValue,
      func)




  ----
  Join the elements into a string, separated by `separator`.
  If there is no elements, `""` is returned.

  * `separator`: the element separator string.
  ----
  function join = |this, separator| -> gololang.Iterators.join(this, separator)

  ----
  Maps elements using a function:

  * `func`: a transformation function.
  ----
  function map = |this, func| -> gololang.Iterators.map(this, func)

  ----
  Filters elements based on a predicate:

      println(iterator: filter(|n| -> (n % 2) == 0))

  * `pred`: a predicate function taking an element and returning a boolean.
  ----
  function filter = |this, pred| -> gololang.Iterators.filter(this, pred)

  function dropWhile = |this, pred| -> gololang.Iterators.drop(this, pred)

  function drop = |this, nb| -> gololang.Iterators.drop(this, nb)

  function take = |this, nb| -> gololang.Iterators.take(this, nb)

  function takeWhile = |this, nb| -> gololang.Iterators.takeWhile(this, nb)
}

# .............................................................. #


local function reduceIter = |iter, init, func| {
  var acc = init
  while iter: hasNext() {
    acc = func(acc, iter: next())
  }
  return acc
}



local function joinIter = |iter, sep| {
  if not iter: hasNext() {
    return ""
  }
  var buffer = StringBuilder(iter: next(): toString())
  while iter: hasNext() {
    buffer: append(sep): append(iter: next())
  }
  return buffer: toString()
}

local function mapIter = |iter, func| -> generator(
  unspool=|it| -> [func(it: next()), it],
  finished=^isFinished,
  seed=iter)

local function filterIter = |iter, pred| -> generator(
    seed=skip(iter, p),
    finished=|seed| -> seed is null,
    unspool=|seed| -> [seed: get(0), skip(seed: get(1), p)]
  ) with {
    p = `not(pred)
  }

local function skip = |iter, pred| {
  var current = null
  while iter: hasNext() {
    current = iter: next()
    if not pred(current) {
      return [current, iter]
    }
  }
  return null
}

local function dropWhileIter = |iter, pred| {
  let r = skip(iter, pred)
  if r oftype Tuple.class {
    return chain(singleton(r: get(0)), r: get(1))
  }
  return empty()
}

local function dropIter = |iter, nb| {
  var i = 0
  while i < nb and iter: hasNext() {
    iter: next()
    i = i + 1
  }
  return iter
}

local function takeIter = |iter, nb| -> match {
  when nb <= 0 then empty()
  otherwise generator(
    seed=[nb, iter],
    finished=|seed| -> n <= 0 or not it: hasNext() with {
      n, it = seed
    },
    unspool=|seed| -> [it: next(), [n - 1, it]] with {
      n, it = seed
    })
  }

local function takeWhileIter = |iter, pred| -> generator(
  unspool=|seed| -> [seed: get(0), take(seed: get(1), pred)],
  finished=|seed| -> seed is null,
  seed=take(iter, pred)
) with {
  take = |it, p| {
    if not it: hasNext() {
      return null
    }
    let v = it: next()
    if not pred(v) {
      return null
    }
    return [v, it]
  }
}

local function isFinished = |it| -> not it: hasNext()

# TODO: stream
# TODO: take/takeWhile
# TODO: collect

###############################################################################
----
Mixin augmentation providing internal iteration on any object with a `HeadTail`
like interface:

**required methods**:

- `isEmpty()` telling if the container contains values
- `head()` returning the value
- `tail()` returning a (may be empty) headtail-like object
----
augmentation InternalHeadTail = {
  # NOTE
  #
  # In this augmentation, we use recursive local functions instead of
  # delegating on the `tail` value, since this augmentation can be applied
  # to *any type* having a `tail` method returning *any type* with `tail`
  # method. As this returned type is not guaranteed to have been
  # augmented with this augmentation itself, we can't be sure the
  # result of `tail` will have the required methods.

  ----
  Checks whether the given element is present.

  **WARNING**: if the structure is infinite, may result in a stack overflow if
  the element is not present.

  - *param* `elt`: the searched element
  - *returns* `true` if the structure contains the given element
  ----
  function contains = |this, elt| -> containsHeadTail(this, elt)

    ----
  Counts the number of elements that satisfy a predicate:

      println(ht: count(|n| -> (n % 2) == 0))

  **WARNING**: This method consume the structure, *do not use* on infinite ones.

  - *param* `pred`: a predicate function, taking an element and returning a boolean
  - *returns* the number of elements matching the predicate
  ----
  function count = |this, pred| -> countHeadTail(this, pred)



}

local function containsHeadTail = |ht, elt| -> match {
  when ht: isEmpty() then false
  when ht: head() == elt then true
  otherwise containsHeadTail(ht: tail(), elt)
}
