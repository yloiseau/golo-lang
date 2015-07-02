# ............................................................................................... #
#
# Copyright (c) 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
#
# All rights reserved. This program and the accompanying materials
# are made available under the terms of the Eclipse Public License v1.0
# which accompanies this distribution, and is available at
# http://www.eclipse.org/legal/epl-v10.html
#
# ............................................................................................... #

----
This module defines some augmentations on collections-like object.
It mainly adds static methods from `java.util.Collections` as augmentations of
the corresponding type if applicable, with a side-effect free version.
----
module gololang.CollectionsAugmentations

----
Creates a `java.util.Comparator` from a key function

The returned comparator compare two values `v1` and `v2` doing
`func(v1): compareTo(func(v2))`

* `func`: the key function. This is a unary function, taking an element 
  and returning a `Comparable` value (at least any java/golo object having a 
  `compareTo` method)
----
function keyComparator = |func| -> asInterfaceInstance(
  java.util.Comparator.class,
  |v1, v2| -> func(v1): compareTo(func(v2)))


augment java.util.Collection {

  ----
  Returns the number of occurrences of the given object in this collection
  (see `java.util.Collections.frequency`)
  ----
  function frequency = |this, obj| -> java.util.Collections.frequency(this, obj)

  ----
  Returns the minimum element of the collection according to their
  _natural_ordering_.
  ----
  function min = |this| -> java.util.Collections.min(this)
  
  ----
  Returns the minimum element of the collection according to the given
  comparator
  ----
  function min = |this, comp| -> java.util.Collections.min(this, comp)

  ----
  Returns the minimum element of the collection according to the given key
  function.

  For instance, to find the youngest person in a collection:

      struct Person = { name, age }

      ...
      let persons = list[...]
      let youngest = persons: minByKey(|p| -> p: age())

  * `keyFun`: the key function. This is a unary function, taking an element of
    the list and returning a `Comparable` value (at least any java/golo object
    having a `compareTo` method)
  ----
  function minByKey = |this, keyFun| -> java.util.Collections.min(this, keyComparator(keyFun))

  ----
  Returns the maximum element of the collection according to their
  _natural_ordering_.
  ----
  function max = |this| -> java.util.Collections.max(this)
  
  ----
  Returns the maximum element of the collection according to the given
  comparator
  ----
  function max = |this, comp| -> java.util.Collections.max(this, comp)

  ----
  Returns the maximum element of the collection according to the given key
  function.

  For instance, to find the oldest person in a collection:

      struct Person = { name, age }

      ...
      let persons = list[...]
      let oldest = persons: maxByKey(|p| -> p: age())

  * `keyFun`: the key function. This is a unary function, taking an element of
    the list and returning a `Comparable` value (at least any java/golo object
    having a `compareTo` method)
  ----
  function maxByKey = |this, keyFun| -> java.util.Collections.max(this, keyComparator(keyFun))
}


augment java.util.List {

  ----
  Replaces all elements in this list with the given object.
  (see `java.util.Collections.fill`)

  * `obj`: the object to replace values with.
  * returns the list itself.
  ----
  function fill = |this, obj| {
    java.util.Collections.fill(this, obj)
    return this
  }

  ----
  Sort the list according to the given key function.

  For instance, to find the sort a list of persons by age:

      struct Person = { name, age }

      ...
      let persons = list[...]
      persons: orderByKey(|p| -> p: age())

  The list is sorted in-place.

  * `keyFun`: the key function. This is a unary function, taking an element of
    the list and returning a `Comparable` value (at least any java/golo object
    having a `compareTo` method)
  ----
  function orderByKey = |this, keyFun| -> this: order(keyComparator(keyFun))

  ----
  Like `orderByKey` but returns a new list (side effect free)
  ----
  function orderedByKey = |this, keyFun| -> this: ordered(keyComparator(keyFun))

  ----
  Reverses the list in place.
  ----
  function reverse = |this| {
    java.util.Collections.reverse(this)
  }

  ----
  Reverses the list. Returns a new list (side-effect free).
  ----
  function reversed = |this| {
    let reversedList = this: newWithSameType()
    reversedList: addAll(this)
    reversedList: reverse()
    return reversedList
  }

  ----
  Rotates the list by the given offset (in place).
  ----
  function rotate = |this, offset| {
    java.util.Collections.rotate(this, offset)
  }

  ----
  Rotates the list by the given offset (side-effect free).
  ----
  function rotated = |this, offset| {
    let rotatedList = this: newWithSameType()
    rotatedList: addAll(this)
    rotatedList: rotate(offset)
    return rotatedList
  }

  ----
  Randomize the list in place
  ----
  function shuffle = |this| {
    java.util.Collections.shuffle(this)
  }

  ----
  Randomize the list in place with the given random generator
  ----
  function shuffle = |this, rnd| {
    java.util.Collections.shuffle(this, rnd)
  }

  ----
  Side-effect free version of `shuffle`
  ----
  function shuffled = |this| {
    let lst = this: newWithSameType()
    lst: addAll(this)
    lst: shuffle()
    return lst
  }

  ----
  Side-effect free version of `shuffle`
  ----
  function shuffled = |this, rnd| {
    let lst = this: newWithSameType()
    lst: addAll(this)
    lst: shuffle(rnd)
    return lst
  }

  ----
  ----
  function take = |this, nb| -> this: subList(0, nb)

  #TODO: function takeWhile = ...

  function drop = |this, nb| -> match {
    if nb >= this: size() then java.util.Collections.emptyList()
    otherwise this: subList(nb, this: size())
  }

  #TODO: function dropWhile = ...

  #TODO: function zip =
  #TODO: function foldl =
  #TODO: function foldr =

}
