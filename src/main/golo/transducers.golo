
module gololang.Transducers

import java.util.stream
import java.util.`function
import java.util.stream.Collectors

# TODO: collector to create a lazy generator (iterator based)
# TODO: refactor standard augmentations
# TODO: refactor LazyList and Tuple

function SimpleReducer = |zero, func| -> Reducer(
  ^gololang.Reducer::constInit: bindTo(zero),
  ^gololang.Reducer::redWrapper: bindTo(func),
  ^gololang.Reducer::identityFinish)

function toReducer = |o| -> match {
  when o oftype java.util.stream.Collector.class then Reducer(
    |red| -> [false, o: supplier(): get()],
    ^gololang.Reducer::redWrapper: bindTo(|acc, val| {
      o: accumulator(): accept(acc, val)
      return acc
    }),
    |acc| -> o: finisher(): apply(acc: get(1)))
  otherwise o
}

# ........................................................................... #

function reduceIterableWith = |iter, reducer| {
  return reduceIteratorWith(iter: iterator(), reducer)
}

function reduceHeadTailWith = |headtail, r| {
  let reducer = toReducer(r)
  var acc = reducer: init(headtail)
  var ht = headtail
  while not (acc: get(0) or ht: isEmpty()) {
    acc = reducer: accumulate(acc, ht: head())
    ht = ht: tail()
  }
  return reducer: finish(acc)
}

function reduceIteratorWith = |iter, r| {
  let reducer = toReducer(r)
  var acc = reducer: init(iter)
  while iter: hasNext() and not acc: get(0) {
    acc = reducer: accumulate(acc, iter: next())
  }
  return reducer: finish(acc)
}

function reduceSpliteratorWith = |split, r| {
  let reducer = toReducer(r)
  var acc = box(reducer: init(split))
  var finished = acc: get(): get(0)
  let consumer = asInterfaceInstance(Consumer.class, |val| {
    acc: set(reducer: accumulate(acc: get(), val))
  })
  while not finished {
    let exhausted = not split: tryAdvance(consumer)
    finished = exhausted or acc: get(): get(0)
  }
  return reducer: finish(acc: get())
}


augment java.lang.Iterable {
  function reduceWith = |this, reducer| -> reduceIteratorWith(this: iterator(), reducer)
}

augment gololang.LazyList {
  function reduceWith = |this, reducer| {
    return reduceHeadTailWith(this, reducer)
  }
}

augment java.util.stream.Stream {
  function reduceWith = |this, reducer| -> match {
    when reducer oftype Collector.class then this: collect(reducer)
    otherwise reduceSpliteratorWith(this: spliterator(), reducer)
  }
}

# ........................................................................... #

local function _map_ = |r, f| ->
  r: reduceWith(mapping(f): invoke(r: defaultCollector()))

local function _filter_ = |r, p| ->
  r: reduceWith(filtering(p): invoke(r: defaultCollector()))
  
local function _reduce_ = |r, z, f| -> r:reduceWith(SimpleReducer(z, f))

local function _take_ = |r, n| ->
  r: reduceWith(take(n): invoke(r: defaultCollector()))

----
Augmentation that can be applied to any type having:
- a `reduceWith(reducer)` method
- a `defaultCollector(): collector` method
to provide several convenient methods.
----
augmentation Reducible = {
  function map = |this, f| -> _map_(this, f)
  function filter = |this, pred| -> _filter_(this, pred)
  function reduce = |this, init, aggreg| -> _reduce_(this, init, aggreg)
  function take = |this, n| -> _take_(this, n)
}

augment java.util.List with Reducible
augment java.util.List {
  function defaultCollector = |this| -> toList()
}

# ........................................................................... #

function mapping = |f| -> |reducer| {
  let r = toReducer(reducer)
  return r: withAccumulator(
    |acc, val| -> match {
      when acc: get(0) then acc
      otherwise r: accumulate(acc, f: invoke(val))
    })
}

function filtering = |pred| -> |reducer| {
  let r = toReducer(reducer)
  return r: withAccumulator(
    |acc, val| -> match {
      when pred: invoke(val) then r: accumulate(acc, val)
      otherwise acc
    })
}

function take = |n| -> |reducer| {
  let count = java.util.concurrent.atomic.AtomicInteger(n)
  let r = toReducer(reducer)
  return r: withAccumulator(
    |acc, val| {
      let res = match {
        when count: getAndDecrement() > 0 then r: accumulate(acc, val)
        otherwise acc
      }
      return match {
        when (count: get() > 0) then [false, res: get(1)]
        otherwise [true, res: get(1)]
      }
    })
}

# ........................................................................... #

function pipe = |t, ts...| {
  var r = t
  foreach trans in ts {
    r = trans: andThen(r)
  }
  return r
}
