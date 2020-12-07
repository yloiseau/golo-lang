
module golo.test.MetaUtils

import gololang.ir.DSL
import gololang.meta.Utils
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers

&use("golo.test.MetaUtilsMacros")

function test_fallback = {
  let a = `let("a", constant(111))

  a: foobar(array["answer", 42])
  assertThat(a: metadata("foobar"), `is(arrayContaining("answer", 42)))
  assertThat(a: foobar(), `is(arrayContaining("answer", 42)))
}

function test_inherit = {
  let b = block()
  let a = `let("a", constant(111))
  b: add(a)

  b: metadata("answer", 42)
  assertThat(a: inheritedMetadata("answer"), `is(42))
  assertThat(a: answer(), `is(42))
}

@makeTest
@withMetadata("themeta", 42)
function f = {

}

function test_macro = {
  assertThat(f(), `is(42))
}

function main = |args| {
  test_fallback()
  test_inherit()
  test_macro()
}
