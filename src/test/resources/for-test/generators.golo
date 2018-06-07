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

function test_iter_reduce = {
    let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 5,
    0)

    let l = g: reduce(list[], |l, v| -> l: append(v))
    assertEquals(l, list[0, 1, 2, 3, 4])
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


function test_singleton = {
  let g1 = gololang.Generators.singleton(42)
  assertEquals(g1: hasNext(), true)
  assertEquals(g1: next(), 42)
  assertEquals(g1: hasNext(), false)
  assertRaises(-> g1: next(), java.util.NoSuchElementException.class)
}

function test_chain = {
  let g = gololang.Generators.chain(
    list[1, 2]: iterator(),
    gololang.Generators.singleton(42),
    gololang.Generators.empty(),
    set["a", "b"]: iterator(),
    gololang.Generator.generator(
      |seed| -> [seed, seed + 1],
      |seed| -> seed >= 105,
      100))

  assertEquals(g: next(), 1)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 2)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 42)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), "a")
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), "b")
  assertEquals(g: hasNext(), true)

  assertEquals(g: next(), 100)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 101)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 102)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 103)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 104)
  assertEquals(g: hasNext(), false)
}

function test_dropWhile = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 13,
    0): dropWhile(|e| -> e < 10)

  assertEquals(g: next(), 10)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 11)
  assertEquals(g: hasNext(), true)
  assertEquals(g: next(), 12)
  assertEquals(g: hasNext(), false)

  let h = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 10,
    0): dropWhile(|e| -> e < 20)

  assertEquals(g: hasNext(), false)

  let l = list[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12]
  let l1 = l: dropWhile(|e| -> e < 10)
  println("### " + l1: class(): name())
  assertEquals(l1 oftype java.util.List.class, true)
  assertEquals(l1: head(), 10)
  assertEquals(l: size(), 13)
  let l2 = l: dropWhile(|e| -> e < 20)
  assertEquals(l2: isEmpty(), true)
}

function test_drop = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> false,
    0): drop(4)
  assertEquals(g: next(), 4)

  let h = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> seed >= 3,
    0): drop(5)
  assertEquals(h: hasNext(), false)

  let l = list[0, 1, 2, 3, 4, 5]
  let l1 = l: drop(4)
  assertEquals(l1 oftype java.util.List.class, true)
  assertEquals(l1: head(), 4)
  assertEquals(l: size(), 6)
  let l2 = l: drop(10)
  assertEquals(l2: isEmpty(), true)
  assertEquals(l: size(), 6)
}

function test_take = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> false,
    0): take(3)

  assertEquals(g: next(), 0)
  assertEquals(g: next(), 1)
  assertEquals(g: next(), 2)
  assertEquals(g: hasNext(), false)

  let l = list[0, 1, 2, 3, 4, 5, 6]: take(3)
  assertEquals(l oftype java.util.List.class, true)
  assertEquals(l: size(), 3)
  assertEquals(l: get(0), 0)
}

function test_takewhile = {
  let g = gololang.Generator.generator(
    |seed| -> [seed, seed + 1],
    |seed| -> false,
    0): takeWhile(|v| -> v < 3)

  assertEquals(g: next(), 0)
  assertEquals(g: next(), 1)
  assertEquals(g: next(), 2)
  assertEquals(g: hasNext(), false)

  let l = list[0, 1, 2, 3, 4, 5, 6]: takeWhile(|v| -> v < 3)
  assertEquals(l oftype java.util.List.class, true)
  assertEquals(l: size(), 3)
  assertEquals(l: get(0), 0)
}

function test_fork = {
  let g = generator(|seed| -> [seed, seed + 1], -> false, 0)

  assertEquals(g: next(),  0)
  assertEquals(g: next(),  1)
  assertEquals(g: next(),  2)

  let h = g: fork()
  assertEquals(h: next(),  3)
  assertEquals(h: next(),  4)
  assertEquals(g: next(),  3)
  assertEquals(g: next(),  4)
}

# TODO: repeat
# TODO: iterate
# TODO: count
# TODO: zip
# TODO: collect
# TODO: idem on collections
# TODO: invoke

function main = |args| {
}
