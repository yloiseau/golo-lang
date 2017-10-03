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
module gololang.reducers


struct Reducer = {supplier, accumulator, finisher}

augment Reducer {
  function init = |this| -> this: supplier()()
  function accumulate = |this, acc, val| -> this: accumulator()(acc, val)
  function finish = |this, acc| -> this: finisher()(acc)
}

local function checkInit = |init| {
  requireNotNull(init)
  if isClosure(init) {
    require(init: acceptArity(0),
            "The init must be a function without parameter")
    return init
  }
  return -> init
}

local function checkAccumulator = |acc| {
  requireNotNull(acc)
  require(isClosure(acc) and acc: acceptArity(1),
          "The accumulator must be a binary function")
  return acc
}

local function checkFinisher = |f| {
  if f is null {
    return ^gololang.Functions::id
  }
  require(isClosure(f) and f: acceptArity(1),
          "The finished must be an unary function")
  return f
}

function Reducer = |init, acc| -> Reducer(init, acc, null)

function Reducer = |red| -> match {
  when red: acceptArity(1) then Reducer(red, red, red)
  otherwise Reducer(red, red, null)
}

function Reducer = |init, acc, fin| -> gololang.reducers.types.Reducer(
  checkInit(init),
  checkAccumulator(acc),
  checkFinisher(fin))

function reducingFunction = |init, acc, fin| {
  let i = checkInit(init)
  let a = checkAccumulator(acc)
  let f = checkFinisher(fin)
  return |args...| -> match {
    when args: size() == 0 then i()
    when args: size() == 1 then f(args: get(0))
    when args: size() == 2 then a(args: get(0), args: get(1))
    otherwise raise("Wrong number of arguments")
  }
}

struct Reduced = { get }

function isReduced = |v| -> v oftype gololang.reducers.types.Reduced.class

function reduced = |v| -> match {
  when v oftype gololang.reducers.types.Reduced.class then v
  otherwise Reduced(v)
}

