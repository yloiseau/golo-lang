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
This module defines some default reporters.

See also [`gololang.testing.reporters.Utils`](./reporters/Utils.html) for some utilities to help create
custom result reporters.
----
module gololang.testing.reporters

import gololang.testing.reporters.Utils

# green
let OK = colorizer("\u001B[32m")
# orange
let FAILED = colorizer("\u001B[33m")
# red
let ERROR = colorizer("\u001B[31m")

local function summaryPrinter = |printer, tests, failed, errors| {
  printer: println("")
  printer: println("─" * 10)
  printer: println(match {
    when errors > 0 then ERROR!("ERROR")
    when failed > 0 then FAILED!("FAILED")
    otherwise OK!("OK")
  })
  printer: format("%s tests, %s succeeded, %s failed, %s errors%n",
      tests,
      OK(tests - (failed + errors)),
      FAILED(failed),
      ERROR(errors))
}

# ............................................................................ #

----
Verbose result reporter.

Prints a hierarchical view of the suites and tests, with colored symbols to
represent the status of the test.

Also prints a final summary.

Best suited to output on console.
----
function verbose = |results, output| -> resultWalker!(
  header = ^noop,
  suiteDescPrinter = |printer, level, suiteDesc| {
    printer: println("")
    indent(printer, level)
    printer: print("∙ ")
    printer: println(colorizer!("\u001B[1m\u001B[34m")(suiteDesc))
  },
  reportTest = testReporter(
    testDescPrinter = |printer, level, testDesc| {
      indent(printer, level)
      printer: format("▸ %s: ", colorizer!("\u001B[34m")(testDesc))
    },
    onOk = |printer, val| {
      printer: println(OK!("✔"))
    },
    onFail = |printer, err| {
      printer: print(FAILED!("✘ "))
      printer: println(err: message())
    },
    onError = |printer, err| {
      printer: print(ERROR!("✘ "))
      printer: println(err)
      err: printStackTrace(printer)
    }),
  footer = ^summaryPrinter)(results, output)


----
Minimal result reporter.

Only prints a summary of the global success status and the total counts values.

Best suited to output on console.
----
function minimal = |results, output| -> resultWalker!(
  header = ^noop,
  suiteDescPrinter = ^noop,
  reportTest = testReporter(^noop, ^noop, ^noop, ^noop),
  footer = |printer, t, f, e| {
    printer: format("%s: %s tests, %s succeeded, %s failed, %s errors%n",
      match {
        when e > 0 then ERROR!("ERROR")
        when f > 0 then FAILED!("FAILED")
        otherwise OK!("OK")
      },
      t, OK(t - f - e), FAILED(f), ERROR(e))
  })(results, output)


----
Summary result reporter.

Prints a synthetic view of the status of each test, as well as a final summary.

Best suited to output on console.
----
function summary = |results, output| -> resultWalker!(
  header = ^noop,
  suiteDescPrinter = ^noop,
  reportTest = testReporter(
    testDescPrinter = ^noop,
    onOk = |printer, t| { printer: print(OK!(".")) },
    onFail = |printer, f| { printer: print(FAILED!("F")) },
    onError = |printer, e| { printer: print(ERROR!("E"))}),
  footer=^summaryPrinter)(results, output)
