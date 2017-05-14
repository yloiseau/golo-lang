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
This module defines an extractor based on a predefined function.

This extractor finds modules containing a predefined function, and run it to
obtain a list of test suites.

For instance, one can define a suite as:
```golo
function $suites = -> list[
  ["Suite 1", list[
    ["Test 1.1", {
      # do some tests
    }],
    ["Test 1.2", {
      # ...
    }]
  ]],
  ["Suite 2", list[
    ["Test 2.1", {
      # ...
    }],
    ["Test 2.2", {
      # ...
    }]
  ]]
]
```

----
module gololang.testing.suites.FactoryExtractor

import gololang.testing.Utils

----
Extractor factory

Returns the extractor function for the given name.
----
function extractor = |suitesFactoryName| -> |path, loader| {
  let suites = list[]
  foreach mod in getModulesFrom(path, loader) {
    # Messages.info("Looking for test factory in " + mod: name())
    try {
      suites: addAll(fun(suitesFactoryName, mod, 0, false)())
    } catch (e) {
      if not (e oftype NoSuchMethodException.class) {
        Messages.warning(e)
        e: printStackTrace()
      }
      continue
    }
  }
  return suites
}
