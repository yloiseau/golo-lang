module golotest.execution.UnionsDefaults

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.testng.Assert

union MyUnion = {
  Type1
  Type2 = {a, b=2*a}
  Type3 = {y, z=42}
  Type4 = {v=MyUnion.Type1()}
  Type5 = {x, y=foo()}
}

let state = list[]

function foo = {
  state: add("foo")
  return 42
}

function test_constant_default_union = {
  let full = MyUnion.Type3("a", 1337)
  assertThat(full: y(), `is("a"))
  assertThat(full: z(), `is(1337))

  let def = MyUnion.Type3("a")
  assertThat(def: y(), `is("a"))
  assertThat(def: z(), `is(42))
}

function test_computed_default_union = {
  assertThat(state: size(), `is(0))

  let full = MyUnion.Type5("a", 1337)
  assertThat(state: size(), `is(0))
  assertThat(full: x(), `is("a"))
  assertThat(full: y(), `is(1337))

  let def = MyUnion.Type5("a")
  assertThat(state: size(), `is(1))
  assertThat(def: x(), `is("a"))
  assertThat(def: y(), `is(42))
}

function test_dependant_default_union = {
  let full = MyUnion.Type2(1, 42)
  assertThat(full: a(), `is(1))
  require(full: b(), `is(42))

  let def = MyUnion.Type2(21)
  require(def: a() == 21, "err")
  require(def: b() == 42, "err")
}

function test_recursive_default_union = {
  let full = MyUnion.Type4(42)
  assertThat(full: v(), `is(42))

  let def = MyUnion.Type4()
  assertTrue(def: v(): isType1())
}
