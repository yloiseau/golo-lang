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
----
This module helps to defines a test extractor based on conventions on modules and functions.

For instance, it can scan a directory for modules whose name ends with `Test`, and collect all its functions whose name
starts with `test`, as e.g.:

```golo
module my.moduleTest

import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.hamcrest

function test_that_new_feature = {
  assertThat(hello("World"), `is("Hello World!"))
}
```

----
module gololang.testing.suites.ConventionExtractor

import java.lang.reflect.Modifier
import gololang.testing.Utils

local function getTests = |selector, descriptor, mod| {
  let tests = set[]
  let lookup = java.lang.invoke.MethodHandles.publicLookup()
  foreach m in mod: declaredMethods() {
    if m: parameterCount() != 0 { continue }
    let modifiers = m : getModifiers()
    if not (isStatic(modifiers) and isPublic(modifiers)) { continue }
    if not selector(m) { continue }
    tests: add([descriptor(m), FunctionReference(lookup: unreflect(m))])
  }
  return tests
}

----
Creates an extractor that use the given conventions.

The resulting extractor finds functions validating the `testSelector` inside modules validating `moduleSelector`.
It generate test suites described by `suiteDescriptor` containing the selected functions described by the result of
`testDescriptor`.

- *param* `moduleSelector`: a predicate on a `java.lang.Class` object indicating if a module must be selected.
- *param* `suiteDescriptor`: a function generating the name of the test suite given the `java.lang.Class` of the module.
- *param* `testSelector`: a predicate on a `java.lang.reflect.Method` object indicating if the function must be selected
- *param* `testDescriptor`: a function generating the description of a test given the `java.lang.reflect.Method` of the
  test function
- *returns* a test suite extractor
----
function extractor = |moduleSelector, suiteDescriptor, testSelector, testDescriptor| ->
  |path, loader| {
    let suites = list[]
    foreach mod in getModulesFrom(path, loader) {
      if not moduleSelector(mod) {
        continue
      }
      let tests = getTests(testSelector, testDescriptor, mod)
      if not tests: isEmpty() {
        suites: add([suiteDescriptor(mod), tests])
      }
    }
    return suites
  }

----
Default extractor.

Select all the functions in any module and use the raw name as a description.
----
function extractor = -> extractor(^any, ^name, ^any, ^name)

local function name = |o| -> o: name()
local function any = -> true

----
Helper function to create a selector based on the name.

The generated selector selects any object whose name (given by a `name()` method) ends with one of the provided suffixes.

- *param* `suffixes`: the suffixes that the name should contains
- *returns* a predicate that can be applied on methods or classes (i.e. as module selector or test selector)
----
function nameEndsWith = |suffixes...| -> |o| {
  let name = o: name()
  foreach suffix in suffixes {
    if name: endsWith(suffix) {
      return true
    }
  }
  return false
}

----
Helper function to create a selector based on the name.

The generated selector selects any object whose name (given by a `name()` method) starts with one of the provided
prefixes.

- *param* `prefixes`: the prefixes that the name should contains
- *returns* a predicate that can be applied on methods or classes (i.e. as module selector or test selector)
----
function nameStartsWith = |prefixes...| -> |o| {
  let name = o: name()
  foreach prefix in prefixes {
    if name: startsWith(prefix) {
      return true
    }
  }
  return false
}
