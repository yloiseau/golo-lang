
module golo.test.AsExpressionMacros

import gololang.ir

macro answer = -> constant(42)

macro answer2 = -> call("foo")
