module gololang.GeneratorsTest

import org.hamcrest

local function assertEquals = |value, expected| {
  MatcherAssert.assertThat(value, Matchers.equalTo(expected))
}

local function assertRaises = |f, ex| {
  try {
    f()
    raise("must fail")
  } catch (e) {
    MatcherAssert.assertThat(e, Matchers.isA(ex))
  }
}

function test_destruct = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed + 1], |seed| -> seed > 1, 0)

  let c1, r1 = g
  assertEquals(c1, 0)
  assertEquals(r1, g)

  let e = gololang.Generator.emptyGenerator()
  let f, t = e
  assertEquals(f, null)
  assertEquals(t, e)

  let c2, r2 = r1
  assertEquals(c2, 1)
  assertEquals(r2, r1)
  assertEquals(r2: hasNext(), false)
}

function test_get = {
  let g = gololang.Generator.generator(|s| -> [s, s + 1], |s| -> s > 2, 0)

  var v = g: get()
  assertEquals(v: isPresent(), true)
  assertEquals(v: get(), 0)

  v = g: get()
  assertEquals(v: isPresent(), true)
  assertEquals(v: get(), 1)

  v = g: get()
  assertEquals(v: isPresent(), true)
  assertEquals(v: get(), 2)

  v = g: get()
  assertEquals(v: isPresent(), false)
}

function test_send = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)
  assertEquals(g: isEmpty(), false)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 0)
  assertEquals(g: next(), 1)
  assertEquals(g: next(), 2)
  assertEquals(g: send(40), 40)
  assertEquals(g: next(), 41)
  assertEquals(g: next(), 42)
}

function test_iterable = {
  let g = gololang.Generator.iterable(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 5,
    0)

  let l = list[]
  foreach e in g {
    l: add(e)
  }
  let l2 = list[]
  foreach e in g {
    l2: add(e)
  }
  assertEquals(l, list[0, 1, 2, 3, 4])
  assertEquals(l2, list[0, 1, 2, 3, 4])
}

function test_as_iterable = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 5,
    0)

  let l = list[]
  foreach e in g: fork() {
    l: add(e)
  }
  let l2 = list[]
  foreach e in g {
    l2: add(e)
  }
  let l3 = list[]
  foreach e in g {
    l3: add(e)
  }
  assertEquals(l, list[0, 1, 2, 3, 4])
  assertEquals(l2, list[0, 1, 2, 3, 4])
  assertEquals(l3, list[])
}

function test_remove = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed], |s| -> false,
  "hello")
  assertRaises(-> g: remove(), UnsupportedOperationException.class)
}

function test_close = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> false,
    0)

  assertEquals(g: next(), 0)
  assertEquals(g: next(), 1)
  g: close()
  assertEquals(g: hasNext(), false)
  assertEquals(g: isEmpty(), true)
  assertRaises(-> g: next(), java.util.NoSuchElementException.class)
  g: send(2)
  assertEquals(g: hasNext(), true)
  assertEquals(g: isEmpty(), false)
  assertEquals(g: next(), 3)
}


function test_onclose = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> false,
    0)

  let l = list[]
  g: onClose(|seed| -> l: add(seed))

  assertEquals(g: next(), 0)
  assertEquals(g: next(), 1)
  g: close()
  g: close()
  assertEquals(l, list[2])
  assertEquals(g: hasNext(), false)
  assertEquals(g: isEmpty(), true)
  assertRaises(-> g: next(), java.util.NoSuchElementException.class)
  g: send(2)
  assertEquals(g: hasNext(), true)
  assertEquals(g: isEmpty(), false)
  assertEquals(g: next(), 3)
}

function test_failing_unspool = {
  let g = gololang.Generator.generator(
  unspool=|seed| {
    raise("error")
  },
  finished=|s| -> false,
  seed=null)

  try {
    g: next()
    raise("must fail")
  } catch (e) {
    assertEquals(e: message(), "error")
  }

  let h = gololang.Generator.generator(
  unspool=|seed| {
    throw java.io.IOException("error")
  },
  finished=|s| -> false,
  seed=null)

  try {
    h: next()
    raise("must fail")
  } catch (e) {
    assertEquals(e: cause(): message(), "error")
  }
}

function test_failing_finished = {
  let g = gololang.Generator.generator(
    unspool=|seed| -> [seed, seed],
    finished=|seed| {
      raise("error")
    },
    seed=null)

  assertEquals(g: hasNext(), false)
}

function test_unspool_null = {
  let g = gololang.Generator.generator(
    |seed| -> null, |s| -> false, null)

  assertRaises(-> g: next(), java.util.NoSuchElementException.class)
}

function test_fork = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)

  assertEquals(g: next(),  0)
  assertEquals(g: next(),  1)
  assertEquals(g: next(),  2)

  let h = g: fork()
  assertEquals(h: next(),  3)
  assertEquals(h: next(),  4)
  assertEquals(g: next(),  3)
  assertEquals(g: next(),  4)
}

function test_state = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)
  assertEquals(g: seed(), 0)
  assertEquals(g: current(), null)
  g: next()
  g: next()
  g: next()
  assertEquals(g: current(), 2)
  assertEquals(g: seed(), 3)
}

function test_skip = {
  let g1 = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)
  let g2 = g1: fork()
  let v1 = g1: skip(): skip(): skip(): current()

   g2: next()
   g2: next()
   let v2 = g2: next()
   assertEquals(v1, v2)
}

function test_head_tail = {
  let g = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)
  assertEquals(g: head(), 0)
  assertEquals(g: tail(): head(), 1)
  assertEquals(g: tail(): tail(): head(), 2)
  assertEquals(g: tail(): tail(): tail(): head(), 3)
  assertEquals(g: head(), 0)

  let h = gololang.Generator.generator(|seed| -> [seed, seed + 1], |s| -> false, 0)
  assertEquals(h: tail(): tail(): tail(): head(), 3)

  let f = gololang.Generator.generator(|seed| -> [seed, seed], |s| -> true, 42)
  assertEquals(f: isEmpty(), true)
  assertEquals(f: head(), null)
  assertEquals(f: tail(): isEmpty(), true)
  assertEquals(f: tail(): head(), null)
}
