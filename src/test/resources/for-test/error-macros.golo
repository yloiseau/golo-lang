module golotest.error.Macros

import gololang.Errors

function retOk = -> Ok(42)
function retEmpty = -> Empty()
function retErr = -> Error(IllegalArgumentException("plop"))
function retVal = -> 1337
function retNull = -> null
function fail = {
  throw IllegalArgumentException("err")
}

local function assertTrue = |value| {
  require(value,
    String.format("`%s` should be true", value))
}

local function _test = |f| {
  let r = `try(f()) orIfNull 0
  return Ok(r + 1)
}

function test_ok = {
  assertTrue(_test(^retOk): isValue(43))
}

#   _test(^retEmpty))
#   _test(^retErr))
#   _test(^retVal))
#   _test(^retNull))
#   _test(^fail))
# }
#
