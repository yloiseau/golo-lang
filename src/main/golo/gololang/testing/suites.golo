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
This module defines some common utilities for test suite extractors as well as
some predefined extractors.
----
module gololang.testing.suites

function Test = |description| -> ^gololang.Functions::id

function doctest = |path, loader| -> gololang.testing.suites.DocTestExtractor.extract(path, loader)

function factory = |path, loader| -> gololang.testing.suites.FactoryExtractor.extractor!("$suites")(path, loader)

----
Build a test suite for each module whose name ends with "Test", using all public
functions whose name starts with "test_" or "check_"
----
function names = |path, loader| -> gololang.testing.suites.ConventionExtractor.extractor!(
  moduleSelector = gololang.testing.suites.ConventionExtractor.nameEndsWith("Test"),
  suiteDescriptor = |m| {
    let n = m: getSimpleName()
    return "Testing module %s with names extractor": format(n: substring(0, n: length() - 4))
  },
  testSelector = gololang.testing.suites.ConventionExtractor.nameStartsWith("test_", "check_"),
  testDescriptor = |f| -> f: name(): replace("_", " "))(path, loader)
