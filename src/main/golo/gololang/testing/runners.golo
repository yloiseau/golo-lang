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
This module defines some common utilities for test runners as well as a simple
default runner.
----
module gololang.testing.runners

import gololang.LazyLists

----
Helper function to walk a test or a sub-suite
----
function runPartWith = |rec, eval| -> |element| {
  let desc, tests = element
  return [desc, match {
    when isClosure(tests) then eval(tests)
    otherwise rec(tests)
  }]
}

----
Simple runner that walk the suites an eagerly run the tests in order.
----
function simple = |suites| -> suites: map(runPartWith(^simple, ^gololang.Errors::trying))

----
Lazy (but sequential) runner
----
function lazy = |suites| -> suites: asLazyList(): map(runPartWith(^lazy, ^gololang.Errors::trying))
