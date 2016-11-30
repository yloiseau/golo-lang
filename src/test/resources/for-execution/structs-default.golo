module golotest.execution.StructsDefault

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.testng.Assert


struct MyStruct = {a, b=42, c="plop"}

struct ComputedStruct = {a, b=foo()}

let state = list[]

function foo = {
  state: add("foo")
  return 42
}

struct DependantStruct = {x, y=2*x}

function test_constant_default_struct = {
  let full = MyStruct("me", 21, "zoo")
  assertThat(full: a(), `is("me"))
  assertThat(full: b(), `is(21))
  assertThat(full: c(), `is("zoo"))

  let def = MyStruct("me")
  assertThat(def: a(), `is("me"))
  assertThat(def: b(), `is(42))
  assertThat(def: c(), `is("plop"))

  let empty = MyStruct()
  assertThat(empty: a(), nullValue())
  assertThat(empty: b(), `is(42))
  assertThat(empty: c(), `is("plop"))
}

function test_function_default_struct = {
  assertThat(state: size(), `is(0))

  let full = ComputedStruct(1, 2)
  assertThat(state: size(), `is(0))
  assertThat(full: a(), `is(1))
  assertThat(full: b(), `is(2))

  let def = ComputedStruct(1)
  assertThat(state: size(), `is(1))
  assertThat(def: a(), `is(1))
  assertThat(def: b(), `is(42))

  let empty = ComputedStruct()
  assertThat(state: size(), `is(2))
  assertThat(empty: a(), nullValue())
  assertThat(empty: b(), `is(42))
}

function test_dependant_default_struct = {
  let full = DependantStruct(0, 42)
  assertThat(full: x(), `is(0))
  assertThat(full: y(), `is(42))

  let def = DependantStruct(21)
  assertThat(def: x(), `is(21))
  assertThat(def: y(), `is(42))

  try {
    let empty = DependantStruct()
    fail("should have failed")
  } catch(e) {
    assertThat(e: getMessage(), containsString("Can't call the default constructor"))
  }

}

