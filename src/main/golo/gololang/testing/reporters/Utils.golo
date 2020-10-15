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
This module defines some common utilities to help create custom result reporters.
----
module gololang.testing.reporters.Utils

import java.nio.charset.Charset
import java.nio.file
import java.io

----
Mutable structure to count the results
----
struct TestCounts = {
  ----
  Total number of tests ran
  ----
  tests,

  ----
  Number of tests that failed with an `AssertionError`
  ----
  failed,

  ----
  Number of tests that raised another kind of error
  ----
  errors
}
augment TestCounts {
  ----
  Increment the total number of tests.
  ----
  function addSuccess = |this| {
    this: tests(this: tests() + 1)
  }

  ----
  Increment the number of failed tests, as well as the total number of tests.
  ----
  function addFailed = |this| {
    this: tests(this: tests() + 1)
    this: failed(this: failed() + 1)
  }

  ----
  Increment the number of tests on error, as well as the total number of tests.
  ----
  function addError = |this| {
    this: tests(this: tests() + 1)
    this: errors(this: errors() + 1)
  }

  ----
  Merge two counters

  Creates a new counter whose values are the sum of the two given counters
  ----
  function merge = |this, other| {
    return TestCounts(
      this: tests() + other: tests(),
      this: failed() + other: failed(),
      this: errors() + other: errors())
  }
}


----
Create a `java.io.PrintStream` from the specified value.

If the given string is "-", `java.lang.System.out` is used.
Otherwise, a `java.nio.file.Path` is created with the predefined `pathFrom`.
The returned `PrintStream` is buffered and uses the default charset.
Parent directory is created. If the file exists, it is overwritten.

- *param* `output`: the file to use; "-" means standard output
- *returns* a buffered `PrintStream` or `java.lang.System.out`

See `java.nio.charset.Charset.defaultCharset`
----
function printStreamFrom = |output| -> printStreamFrom(output, defaultCharset(): name())

----
Create a `java.io.PrintStream` from the specified value.

If the given string is "-", `java.lang.System.out` is used.
Otherwise, a `java.nio.file.Path` is created with the predefined `pathFrom`.
The returned `PrintStream` is buffered and uses the given charset.
Parent directory is created. If the file exists, it is overwritten.

- *param* `output` the file to use; can be a `PrintStream`, a `OutputStream`, or
  any argument to `gololang.Predefined.pathFrom`, "-" means standard output
- *param* `charset` the charset to use, as a `String` or a `java.nio.charset.Charset`
- *returns* a buffered `PrintStream` or `java.lang.System.out`
----
function printStreamFrom = |output, charset| {
  if output == "-" {
    return System.out()
  }
  if output oftype PrintStream.class {
    return output
  }
  var out = null
  if output oftype OutputStream.class {
    out = output
  } else {
    let outputPath = pathFrom(output)
    if outputPath: parent() isnt null {
      Files.createDirectories(outputPath: parent())
    }
    out = Files.newOutputStream(outputPath)
  }
  return PrintStream(BufferedOutputStream(out), true, match {
    when charset oftype Charset.class then charset: name()
    otherwise charset: toString()
  })
}


----
Create a function colorizing text, if supported.

The color is supported on non-Windows platforms consoles.

- *param* `color`: the color to use
- *returns* a function accepting a value and returning a colorized string if
  supported, or a string representation of the value otherwise
----
function colorizer = |color| -> match {
  when System.getProperty("os.name"): contains("Windows") or System.console() is null then |s| -> s: toString()
  otherwise |s| -> String.format("%s%s\u001B[0m", color, s)
}

----
Do nothing

Can be used with the [`resultWalker`](#resultWalker_4) to ignore some part.
----
function noop = |args...| -> null

----
Create an indent by printing `level` spaces to `printer`.
----
function indent = |printer, level| {
  if level > 0 {
    printer: print(level * "  ")
  }
}

----
Higher order function to create a test reporter.

The reporter is built from a bunch of printing functions, in the style of the
“Template Method” design pattern. The first parameter of each of theses methods
is a `PrintStream` like object (often standard output), that is used to print
the report to.

Theses methods are:

- a function to print a test description. It takes
  (besides the printer) the nesting level of its suite and the test description;

- a function to print a valid test result, called with the value return by the
  test in a `Result` value as argument;

- a function to print a failed test result, called if the test failed with an
  `AssertionError` (in a `Result` error), with the exception as argument;

- a function to print a test on error, called if the test failed with any other
  exception, with the exception as argument;

The returned function can be used to report a test result, and used as argument
to [`resultWalker`](#resultWalker_4).

- *param* `testDescPrinter`: function used to print a test description
- *param* `onOk`: function used to print a valid test result
- *param* `onFail`: function used to print a failed test result
- *param* `onError`: function used to print a test in error
- *returns* a function to report a test as needed by
  [`resultWalker`](#resultWalker_4)
----
function testReporter = |testDescPrinter, onOk, onFail, onError| ->
  |printer, level, desc, result, counts| {
    testDescPrinter(printer, level, desc)
    let err, val = result
    case {
      when err is null {
        onOk(printer, val)
        counts: addSuccess()
      }
      when err oftype AssertionError.class {
        onFail(printer, err)
        counts: addFailed()
      }
      otherwise {
        onError(printer, err)
        counts: addError()
      }
    }
  }

----
Higher order function to create a reporter.

The reporter is built from a bunch of printing functions, in the style of the
“Template Method” design pattern. The first parameter of all of theses methods
is a `PrintStream` like object (often standard output), that is used to print
the report to.

Theses methods are:

- a function to print a global header, called at the very beginning of the report.
  It takes (besides the printer) the full collection of suite results;

- a function to print a suite description, called at the beginning of every
  suite. It takes (besides the printer) the nesting level of the suite (starting
  from 0) and the suite description;

- a function to print a test description, called for every test. It takes
  (besides the printer) the nesting level of its suite, the test description,
  the test result (as an instance of `gololang.error.Result`, and a `TestCount`
  instance that will be mutated.

- a function to print a global footer, called at the very end of the report.
  It takes (besides the printer) the total number of tests, the number of failed
  tests, and the number of errors.

The built reporter will create a printer from its second string argument, walk
the suite results using this printer, call each function accordingly, and finally
return the total number of failed tests.

- *param* `header`: function used to print a global header
- *param* `suiteDescPrinter`: function used to print a suite description.
- *param* `reportTest`: function to report a test, as returned by
  [`testReporter`](#testReporter_4) for instance
- *param* `footer`: function used to print a global footer
- *returns* a function that can be used as a reporter
----
function resultWalker = |header, suiteDescPrinter, reportTest, footer| {
  let reportElement = |printer, level, desc, result, counts| {
    if result oftype gololang.error.Result.class {
      reportTest(printer, level, desc, result, counts)
    } else {
      suiteDescPrinter(printer, level, desc)
      foreach subDesc, subResult in result {
        reportElement(printer, level + 1, subDesc, subResult, counts)
      }
    }
  }
  return |suiteResults, output| {
    let printer = printStreamFrom(output)
    header(printer, suiteResults)
    let count = TestCounts(0, 0, 0)
    foreach suiteDesc, suiteResult in suiteResults {
      reportElement(printer, 0, suiteDesc, suiteResult, count)
    }
    let t, f, e = count
    footer(printer, t, f, e)
    return f + e
  }
}
