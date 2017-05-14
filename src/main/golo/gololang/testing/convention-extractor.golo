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
This module defines an extractor based on conventions on modules and functions.
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

function nameEndsWith = |suffixes...| -> |o| {
  let name = o: name()
  foreach suffix in suffixes {
    if name: endsWith(suffix) {
      return true
    }
  }
  return false
}

function nameStartsWith = |prefixes...| -> |o| {
  let name = o: name()
  foreach prefix in prefixes {
    if name: startsWith(prefix) {
      return true
    }
  }
  return false
}
