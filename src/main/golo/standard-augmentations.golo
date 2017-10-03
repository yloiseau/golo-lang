# ............................................................................................... #
#
# Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA Lyon) and others
#
# This program and the accompanying materials are made available under the
# terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# SPDX-License-Identifier: EPL-2.0
#
# ............................................................................................... #
----
This module defines the set of standard augmentations.
----
module gololang.StandardAugmentations


local function _newWithSameType = |this| {
  try {
    return this: getClass(): newInstance()
  } catch (e) {
    if not(e oftype java.lang.InstantiationException.class) {
      throw e
    }
    let fallback = match {
      when this oftype java.util.RandomAccess.class then java.util.ArrayList()
      when this oftype java.util.List.class then java.util.LinkedList()
      when this oftype java.util.Set.class then java.util.HashSet()
      when this oftype java.util.Map.class then java.util.HashMap()
      otherwise null
    }
    if fallback is null {
      raise("Cannot create a new collection from " + this: getClass())
    }
    return fallback
  }
}

local function _closureWithIndexArgument = |target| -> match {
  when target: arity() == 0
    then FunctionReference(java.lang.invoke.MethodHandles.dropArguments(target: handle(), 0, java.lang.Object.class))
  otherwise
    target
}

# ............................................................................................... #

----
Number augmentations.
----
augment java.lang.Number {

  ----
  Repeats a function many times, as in:

      3: times(-> println("Hey!")
      4: times(|i| -> println(i))

  * `count`: how many times the `func` function must be repeated.
  * `func`: a function to execute.

  The `func` function may take 0 or 1 argument. In the later case the argument is the iteration
  count index.
  ----
  function times = |count, func| {
    let target = _closureWithIndexArgument(func)
    for (var i = 0, i < count, i = i + 1) {
      target(i)
    }
  }

  ----
  Repeats a function over a discrete interval:

      1: upTo(3, -> println("Hello"))
      1: upTo(3, |i| ->println(i))

  * `low`: the start value (inclusive).
  * `high`: the end value (inclusive).
  * `func`: the function to execute.

  As in the case of [`times`](#times_2), `func` may take an optional index parameter.
  See also [`downTo`](#downTo_3).
  ----
  function upTo = |low, high, func| {
    let target = _closureWithIndexArgument(func)
    for (var i = low, i <= high, i = i + 1) {
      target(i)
    }
  }

  ----
  Similar to [`upTo`](#upTo_3), except that the interval iteration is made from `high` down to `low`.
  ----
  function downTo = |high, low, func| {
    let target = _closureWithIndexArgument(func)
    for (var i = high, i >= low, i = i - 1) {
      target(i)
    }
  }
}

# ............................................................................................... #

----
Useful string augmentations.
----
augment java.lang.String {

  ----
  Convenience wrapper over `java.lang.String.format(...)`.
  ----
  function format = |this, args...| {
    if args: length() == 1 {
      return java.lang.String.format(this, args: get(0))
    } else {
      return java.lang.String.format(this, args)
    }
  }

  ----
  Wrapper over `java.lang.Integer.parseInt`.
  ----
  function toInt = |this| ->
    java.lang.Integer.parseInt(this)

   ----
  Wrapper over `java.lang.Integer.parseInt`.
  ----
  function toInteger = |this| ->
    java.lang.Integer.parseInt(this)

  ----
  Wrapper over `java.lang.Integer.parseDouble`.
  ----
  function toDouble = |this| ->
    java.lang.Double.parseDouble(this)

  ----
  Wrapper over `java.lang.Integer.parseFloat`.
  ----
  function toFloat = |this| ->
    java.lang.Float.parseFloat(this)

  ----
  Wrapper over `java.lang.Integer.parseLong`.
  ----
  function toLong = |this| ->
    java.lang.Long.parseLong(this)
}

# ............................................................................................... #
----
Augmentations over `CharSequence` to view it as a "real" `char` collection.
----
augment java.lang.CharSequence {
  # TODO: all iterable/head-tail augmentations on CharSequence

  ----
  Returns the first `char` of the sequence, of `null` if empty.
  ----
  function head = |this| -> match {
    when this: isEmpty() then null
    otherwise this: charAt(0)
  }

  ----
  Returns the remaining subsequence as a String (i.e. an immutable sequence)
  ----
  function tail = |this| -> match {
    when this: isEmpty() then this: getClass(): newInstance()
    otherwise this: subSequence(1, this: length()): toString()
  }

  ----
  Checks if the sequence is empty.
  ----
  function isEmpty = |this| -> this: length() == 0
}


# ............................................................................................... #

----
Augmentations over iterable.
----
augment java.lang.Iterable with gololang.IterationAugmentations.InternalIterable

augment java.util.Iterator with gololang.IterationAugmentations.InternalIterator


# ............................................................................................... #

----
Java collections augmentations.
----
augment java.util.Collection {

  ----
  Returns an empty collection of the same type as `this`.
  ----
  function newWithSameType = |this| -> _newWithSameType(this)

  ----
  Destructuration helper.

  * return a tuple of the values
  ----
  function destruct = |this| -> Tuple.fromArray(this: toArray())

  ----
  Filters elements based on a predicate:

      println(list[1, 2, 3, 4]: filter(|n| -> (n % 2) == 0))

  * `this`: a collection.
  * `pred`: a predicate function taking an element and returning a boolean.

  `filter` returns a new collection of the same type as the original one, hence
  the original collection is kept intact.
  ----
  function filter = |this, pred| -> collectCollection(
    this: iterator(): filter(pred),
    this)

  ----
  Maps elements of a collection using a function:

      println(list[1, 2, 3]: map(|n| -> n * 10))

  * `this`: a collection.
  * `func`: a transformation function.

  `map` returns a new collection with the same type, keeping the original one intact.
  ----
  function map = |this, func| -> collectCollection(
    this: iterator() : map(func),
    this)

  function drop = |this, nb| -> collectCollection(
    this: iterator(): drop(nb),
    this)

  function dropWhile = |this, pred| -> collectCollection(
    this: iterator(): dropWhile(pred),
    this)

  function take = |this, nb| -> collectCollection(
    this: iterator(): take(nb),
    this)

  function takeWhile = |this, pred| -> collectCollection(
    this: iterator(): takeWhile(pred),
    this)
}

local function collectCollection = |iter, collect| -> iter: reduce(
  collect: newWithSameType(),
  |c, v| {
    c: add(v)
    return c
  })

# ............................................................................................... #

----
Java lists augmentations.
----
augment java.util.List {

  ----
  Appends an element to a list.
  ----
  function append = |this, element| {
    this: add(element)
    return this
  }

  ----
  Prepends an element to a list.
  ----
  function prepend = |this, element| {
    this: add(0, element)
    return this
  }

  ----
  Inserts an element at some index.
  ----
  function insert = |this, index, element| {
    this: add(index, element)
    return this
  }

  ----
  Appends a variable number of arguments to a list.

  * `head`: an element to append.
  * `tail`: a variable number of elements to append.
  ----
  function append = |this, head, tail...| {
    this: append(head)
    foreach (element in tail) {
      this: append(element)
    }
    return this
  }

  ----
  Prepends a variable number of arguments to a list.
  ----
  function prepend = |this, head, tail...| {
    for (var i = tail: length() - 1, i >= 0, i = i - 1) {
      this: prepend(tail: get(i))
    }
    return this: prepend(head)
  }

  ----
  Returns a list first element, of `null` if empty.
  ----
  function head = |this| -> match {
    when this: isEmpty() then null
    otherwise this: get(0)
  }

  ----
  Returns a list last element.
  ----
  function last = |this| -> this: get(this: size() - 1)

  ----
  Returns the rest of a list after its head, as an unmodifiable list.
  ----
  function tail = |this| -> match {
    when this: size() <= 1 then java.util.Collections.EMPTY_LIST()
    otherwise java.util.Collections.unmodifiableList(this: subList(1, this: size()))
  }

  ----
  Convenience wrapper over `java.util.Collections.unmodifiableList`.
  ----
  function unmodifiableView = |this| -> java.util.Collections.unmodifiableList(this)

  ----
  Reverse the elements of the list and returns the list.

  See also [`reversed`](#java.util.List.reversed_1).
  ----
  function reverse = |this| {
    java.util.Collections.reverse(this)
    return this
  }

  ----
  Same as [`reverse`](#java.util.List.reverse_1), but the returned list is a new one, leaving the original list order intact.
  ----
  function reversed = |this| {
    let reversedList = this: newWithSameType()
    reversedList: addAll(this)
    return reversedList: reverse()
  }

  ----
  Sorts the list elements and returns the list.
  ----
  function order = |this| {
    java.util.Collections.sort(this)
    return this
  }

  ----
  Returns a new list where the elements have been sorted.
  ----
  function ordered = |this| {
    let sortedList = this: newWithSameType()
    sortedList: addAll(this)
    return sortedList: order()
  }

  ----
  Sorts the element using a comparator, see `java.util.Collections.sort(...)`.
  ----
  function order = |this, comparator| {
    java.util.Collections.sort(this, comparator)
    return this
  }

  ----
  Returns a new list where the elements have been sorted using a comparator.
  See `java.util.Collections.sort`.
  ----
  function ordered = |this, comparator| {
    let sortedList = this: newWithSameType()
    sortedList: addAll(this)
    return sortedList: order(comparator)
  }

  ----
  Removes the element at the specified position.

  This method has the same behaviour as `java.util.List.remove(int)`, but is
  needed since for Golo everything is an `Object` and `remove` is overloaded.
  ----
  function removeAt = |this, idx| -> removeByIndex(this, idx)
}

# ............................................................................................... #

----
Augmentations over set collections.
----
augment java.util.Set {

  ----
  Alias for `add` that returns the set.
  ----
  function include = |this, element| {
    this: add(element)
    return this
  }

  ----
  Alias for `remove` that returns the set.
  ----
  function exclude = |this, element| {
    this: remove(element)
    return this
  }

  ----
  Includes a variable number of elements, and returns the set.
  ----
  function include = |this, first, rest...| {
    this: add(first)
    foreach (element in rest) {
      this: add(element)
    }
    return this
  }

  ----
  Excludes a variable number of elements, and returns the set.
  ----
  function exclude = |this, first, rest...| {
    this: remove(first)
    foreach (element in rest) {
      this: remove(element)
    }
    return this
  }

  ----
  Alias for `contains`.
  ----
  function has = |this, element| -> this: contains(element)

  ----
  Alias for `contains` over a variable number of elements.
  ----
  function has = |this, first, rest...| {
    if not(this: contains(first)) {
      return false
    } else {
      foreach (element in rest) {
        if not(this: contains(element)) {
          return false
        }
      }
    }
    return true
  }

  ----
  Convenience wrapper for `java.util.Collections.unmodifiableSet(...)`.
  ----
  function unmodifiableView = |this| -> java.util.Collections.unmodifiableSet(this)

}

# ............................................................................................... #

----
Augmentations over maps.
----
augment java.util.Map {

  ----
  Alias for `put` that returns the map.
  ----
  function add = |this, key, value| {
    this: put(key, value)
    return this
  }

  ----
  Adds a tuple `[key, value]` or a map entry and returns the map.
  ----
  function add = |this, kv| {
    case {
      when kv oftype Tuple.class and kv: size() == 2 {
        this: put(kv: get(0), kv: get(1))
      }
      when kv oftype java.util.Map$Entry.class {
        this: put(kv: getKey(), kv: getValue())
      }
      otherwise {
        throw IllegalArgumentException(
          "expected a 2-tuple or a Map.Entry, got a " + kv: getClass())
      }
    }
    return this
  }

  ----
  Alias for `remove` that returns the map.
  ----
  function delete = |this, key| {
    this: remove(key)
    return this
  }

  ----
  Adds an element to the map only if there is no entry for that key.

  * `this`: a map.
  * `key`: the element key.
  * `value`: the element value or a function to evaluate to get a value.

  The fact that `value` can be a function allows for delayed evaluation which can be useful for
  performance reasons. So instead of:

      map: addIfAbsent(key, expensiveOperation())

  one may delay the evaluation as follows:

      map: addIfAbsent(key, -> expensiveOperation())

  `addIfAbsent` returns the map.
  ----
  function addIfAbsent = |this, key, value| {
    if not(this: containsKey(key)) {
      if isClosure(value) {
        this: put(key, value())
      } else {
        this: put(key, value)
      }
    }
    return this
  }

  ----
  Returns a value from a key or a default value if the entry is not defined.

  * `this`: a map.
  * `key`: the key to look for.
  * `replacement`: the default value, or a function giving the default value.

  As it is the case for [`addIfAbsent`](#java.util.Map.addifAbsent_3),
  one can take advantage of delayed evaluation:

      println(map: getOrElse(key, "n/a"))
      println(map: getOrElse(key, -> expensiveOperation())

  Note that `replacement` yields the return value also when there is an entry for `key` but the
  value is `null`.
  ----
  function getOrElse = |this, key, replacement| {
    let value = this: get(key)
    if value isnt null {
      return value
    }
    if isClosure(replacement) {
      return replacement()
    } else {
      return replacement
    }
  }

  ----
  Wrapper for `java.util.Collections.unmodifiableMap(...)`.
  ----
  function unmodifiableView = |this| -> java.util.Collections.unmodifiableMap(this)

  ----
  Returns a new empty map of the same type.
  ----
  function newWithSameType = |this| -> _newWithSameType(this)

  ----
  Returns the first element that satisfies a predicate, or `null` if none matches.

  `pred` takes 2 arguments: a key and a value, and returns a boolean.
  ----
  function find = |this, pred| -> this: entrySet(): find(onEntry(pred))

  ----
  Filters elements using a predicate, and returns a new map.

  `pred` takes 2 arguments: a key and a value, and returns a boolean.
  ----
  function filter = |this, pred| -> collectCollection(
    this: entrySet(): iterator(): filter(onEntry(pred)),
    this)

  ----
  Maps entries of the map using a function.

  `func` takes 2 arguments: a key and a value. The returned value must have `getKey()` and
  `getValue()` to represent a map entry. We suggest using the predefined `mapEntry(key, value)`
  function as it returns such object.
  ----
  function map = |this, func| -> collectCollection(
    this: entrySet(): iterator(): map(onEntry(func)),
    this)

  ----
  Reduces the entries of a map.

  `func` takes 3 arguments:

  * an accumulator whose initial value is `initialValue`,
  * a key for the next entry,
  * a value for the next entry.
  ----
  function reduce = |this, initialValue, func| -> this: entrySet()
    : reduce(initialValue,
            |acc, entry| -> func(acc, entry: getKey(), entry: getValue()))

  ----
  Iterates over each entry of a map.

  `func` takes 2 arguments: the entry key and its value.
  ----
  function each = |this, func| -> this: entrySet(): each(onEntry(func))

  ----
  Counts the number of elements satisfying a predicate.
  ----
  function count = |this, pred| -> this: entrySet(): count(onEntry(pred))

  ----
  Returns `true` if there is any value satisfying `pred`, `false` otherwise.
  ----
  function exists = |this, pred| -> this: entrySet(): exists(onEntry(pred))
}

local function onEntry = |f| -> |e| -> f(e: getKey(), e: getValue())

augment java.util.Map$Entry {
  ----
  Destructurate a map entry in key and value
  ----
  function destruct = |this| -> [ this: getKey(), this: getValue() ]

  ----
  Convert then entry into an array containing the key and the value.
  ----
  function toArray = |this| -> array[this: getKey(), this: getValue()]
}
# ............................................................................................... #

----
Augmentations for Golo tuples.
----
augment gololang.Tuple {

  ----
  Filters elements using a predicate, returning a new tuple.
  ----
  function filter = |this, func| {
    let matching = list[]
    foreach element in this {
      if func(element) {
        matching: add(element)
      }
    }
    return gololang.Tuple.fromArray(matching: toArray())
  }

  ----
  Maps the elements of a tuple, and returns a tuple with the transformed values.
  ----
  function map = |this, func| {
    let values = list[]
    foreach element in this {
      values: add(func(element))
    }
    return gololang.Tuple.fromArray(values: toArray())
  }
}

# ............................................................................................... #

----
Augment functions to make them behave more like objects from java.util.function
----
augment gololang.FunctionReference {
  ----
  Call this function with no arguments.
  ----
  function get = |this| {
    if not this: acceptArity(0) {
      throw UnsupportedOperationException(
        "`get` must be called on function accepting 0 parameter")
    }
    return this()
  }

  ----
  Alias for `FunctionReference::invoke`
  ----
  function apply = |this, args...| -> this: invoke(args)

  ----
  Alias for `FunctionReference::invoke`
  ----
  function accept = |this, args...| -> this: invoke(args)
}


----
Augment java stream to provide an interface more compatible with
collections/iterators.
----
augment java.util.stream.Stream {

  ----
  TODO: doc
  ----
  function all = |this, pred| -> this
    : allMatch(asInterfaceInstance(java.util.function.Predicate.class, pred))

  ----
  TODO: doc
  ----
  function exists = |this, pred| -> this
    : anyMatch(asInterfaceInstance(java.util.function.Predicate.class, pred))
}





