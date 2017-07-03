module golotest.execution.SamSupport

import org.eclipse.golo.compiler.testing.support

function func = ->
  SamSupportHelpers.apply(|e| -> e + e)

function ctor = ->
  SamSupportHelpers(|e| -> e + "!"): state()

function meth = ->
  SamSupportHelpers(|e| -> e + "!"): plopIt(|e| -> e, "Yeah")

function func_varargs = ->
  SamVarargsSupportHelpers.apply(|e| -> e: get(0) + e: get(1))

function ctor_varargs = ->
  SamVarargsSupportHelpers(|e| -> e: get(0) + e: get(1) + "!"): state()

function meth_varargs = ->
  SamVarargsSupportHelpers(|e| -> e: get(0) + e: get(1) + "!"): plopIt(|e| -> e: get(0) + e: get(1), "Yeah")

function main = |args| {
  require(func() == "Hey!Hey!", "err: func")
  require(ctor() == "Plop!", "err: ctor")
  require(meth() == "Yeah", "err: meth")
  require(func_varargs() == "Hey!Hey!", "err: func_varargs")
  require(ctor_varargs() == "PlopPlop!", "err: ctor_varargs")
  require(meth_varargs() == "YeahYeah", "err: meth_varargs")
  println("===== OK =====")
}
