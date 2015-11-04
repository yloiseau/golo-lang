module golotest.execution.Operators

function plus_one = |a| {
  return a + 1
}

function plus = |a, b| {
  return a + b
}

function minus_one = |a| {
  return a - 1
}

function minus = |a, b| -> a - b

function half = |a| {
  return a / 2
}

function divide = |a, b| -> a / b

function twice = |a| {
  return a * 2
}

function multiply = |a, b| -> a * b

function modulo = |a, b| -> a % b

function compute_92 = {
  return ((1 + 2 + 3) * (5) * 6 + (10 / 2) - 1) / 2
}

function eq = |a, b| {
  return a == b
}

function at_least_5 = |a| {
  if a < 5 {
    return 5
  } else {
    return a
  }
}

function strictly_between_1_and_10 = |a| {
  return 1 < a and a < 10
}

function between_1_and_10_or_20_and_30 = |a| {
  return (a >=1 and a <= 10) or ((a >= 20) and (a <= 30))
}

function neq = |a, b| {
  return not (a == b)
}

function same_ref = |a, b| {
  return a is b
}

function different_ref = |a, b| {
  return a isnt b
}

function special_concat = |a, b, c, d| {
  return "[" + a + ":" + b + ":" + c + ":" + d + "]"
}

function oftype_string = |a| {
  return a oftype java.lang.String.class
}

function average = | zero, items... | {
  let count = items: size()
  var sum = zero
  for (var i = 0, i < count, i = i + 1) {
    sum = sum + items: get(i)
  }
  return sum / count
}

function is_even = |value| {
  return (value % 2) == 0
}

function null_guarded = {
  let map = map[]
  return map: get("bogus") orIfNull "n/a"
}

local function sideeffect = |l, v| {
  l: add(v)
  return v
}

function lazy_ifnull = {
  let l = list[]
  var a = "plop" orIfNull sideeffect(l, 42)
  a = "foo" orIfNull sideeffect(l, "foo")
  a = null orIfNull sideeffect(l, 42)
  return [a, l] == [42, list[42]]
}

function polymorphic_number_comparison = {
  let left = list[1, 1_L, 1.0, 1.0_F, 1.0_B, 1_B]
  let right = list[2, 2_L, 1.1, 1.1_F, 1.1_B, 2_B]

  foreach a in left {
    foreach b in right {
      require((not (a == b)), "equals failed")
      require((not (b == a)), "equals failed")
      require((b != a), "not equals failed")
      require((a != b), "not equals failed")

      require((a <= b), "less or equals failed")
      require((not (a >= b)), "more or equals failed")
      require((b >= a), "more or equals failed")
      require((not (b <= a)), "less or equals failed")

      require((a < b), "less failed")
      require((not (a > b)), "more failed")
      require((b > a), "more failed")
      require((not (b < a)), "less failed")
    }
  }

  foreach a in left {
    foreach b in left {
      require((a == b), "equals failed")
      require((b == a), "equals failed")
      require((not (a != b)), "not equals failed")
      require((not (b != a)), "not equals failed")

      require((a <= b), "less or equals failed")
      require((a >= b), "more or equals failed")
      require((b >= a), "more or equals failed")
      require((b <= a), "less or equals failed")

      require((not (a < b)), "less failed")
      require((not (a > b)), "more failed")
      require((not (b > a)), "more failed")
      require((not (b < a)), "less failed")
    }
  }

  return true
}

function main = |args| {
    require(plus_one(1) == 2, "err")
    require(plus_one("x = ") == "x = 1", "err")
    require(plus_one(10_L) == 11_L, "err")

    require(minus_one(5) == 4, "err")

    require(half(12) == 6, "err")
    require(half('T') == 42, "err")

    require(twice(6) == 12, "err")
    require(twice('*') == 84, "err")
    require(twice("Plop") == "PlopPlop", "err")

    require(compute_92() == 92, "err")

    require(eq('a', 'a') == true, "err")
    require(eq('*', 42) == true, "err")
    require(eq('a', 'b') == false, "err")
    require(eq(66.6, 66.6) == true, "err")
    require(eq(666, 666) == true, "err")
    require(eq(666, 666.0) == true, "err")
    require(eq(999, 666) == false, "err")

    require(at_least_5(10) == 10, "err")
    require(at_least_5(-10) == 5, "err")

    require(strictly_between_1_and_10(5) == true, "err")
    require(strictly_between_1_and_10(-5) == false, "err")
    require(strictly_between_1_and_10(15) == false, "err")

    require(between_1_and_10_or_20_and_30(5) == true, "err")
    require(between_1_and_10_or_20_and_30(25) == true, "err")
    require(between_1_and_10_or_20_and_30(15) == false, "err")
    require(between_1_and_10_or_20_and_30(50) == false, "err")

    require(neq("foo", "bar") == true, "err")

    require(same_ref("foo", "foo") == true, "err")
    require(same_ref("foo", 1) == false, "err")

    require(different_ref("foo", "foo") == false, "err")
    require(different_ref("foo", 1) == true, "err")

    require(special_concat(1, "a", 2, "b") == "[1:a:2:b]", "err")

    require(oftype_string("Hello") == true, "err")
    require(oftype_string(666) == false, "err")

    require(average(1, array[1, 2, 3]) == 2, "err")
    require(average(1, array[1, 2_L, 3]) == 2_L, "err")
    require(Math.abs(average(1, array[1, 2_L, 3.0]) - 2.0) < 0.5, "err")

    require(is_even(2) == true, "err")
    require(is_even(3) == false, "err")

    require(null_guarded() == "n/a", "err")

    require(polymorphic_number_comparison() == true, "err")

    println("ok")
}
