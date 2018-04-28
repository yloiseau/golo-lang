# ............................................................................................... #
#
# Copyright (c) 2012-2018 Institut National des Sciences Appliquées de Lyon (INSA-Lyon) and others
#
# This program and the accompanying materials are made available under
# the terms of the Eclipse Public License 2.0 which is available at
# http://www.eclipse.org/legal/epl-2.0.
#
# SPDX-License-Identifier: EPL-2.0
#
# ............................................................................................... #
----
This module contains some functions to help writing macros.
----
module gololang.macros.Utils

import gololang.ir

----
Utility to extract the last argument of a varargs function.

Useful when creating a macro used as a decorator that can take multiple
arguments.

# Example

Such a macro can be used as

```golo
@myMacro(answer=42, ping="pong")
function myFunction = ...
```

It will be defined as
```golo
macro foo = |args...| {
  let arguments, element = extractLastArgument(args)
  #...
  return element
}
```

In this example, `arguments` will be an array of `NamedArgument`s, and `element`
will contain the `GoloFunction`.

- *param* `args`: an array containing the arguments
- *returns* a tuple containing an array of the arguments but the last, and the
  last argument.
----
function extractLastArgument = |args| -> [
  java.util.Arrays.copyOf(args, args: length() - 1),
  args: get(args: length() - 1)
]

----
Converts a `NamedArgument` collection into a map whose keys are the names of the
named arguments, and values the associated values.

# Example

Given a macro defined as:
```golo
macro foo = |args...| {
  let arguments = namedArgsToMap(args)
  # ...
}
```

When called as `&foo(a=42, b="hello")`, the `arguments` variable will contains
`map[["a", constant(42)], ["b", constant("hello")]]`

The elements of the collection that are not `NamedArgument` are ignored.
----
function namedArgsToMap = |args| -> map[
  [arg: name(), arg: expression()]
  foreach arg in args when arg oftype NamedArgument.class
]

----
Convert a collection of expressions and named arguments into a triple
containing a list of the expressions and a map extracted from named arguments
like by [`namedArgsToMap`](#namedArgsToMap_1).

If `extractLast` is `true`, also extract the last argument as by
[`extractLastArgument`](#extractLastArgument_1).

# Example

```golo
macro myMacro = |args...| {
  let positional, named, last = parseArguments(args, true)
  # ...
}

@myMacro(answer=42, "foo", foo="bar")
function plop = -> "daplop"
```

In the `myMacro` macro, `positional` will be `list[constant("foo")]`, `named`
will be `map[["answer", constant(42)], ["foo", constant("bar")]]` and `last` will
be the `plop` function IR node.
----
function parseArguments = |args, extractLast| -> match {
  when extractLast then parseArguments(begin, false): extend(last) with {
    begin, last = extractLastArgument(args)
  }
  otherwise [
    list[arg foreach arg in args when not (arg oftype NamedArgument.class)],
    map[
      [arg: name(), arg: expression()]
      foreach arg in args when arg oftype NamedArgument.class]
  ]
}

local function applyIfTypeMatches = |type, mac| -> |elt| -> match {
  when elt oftype type then mac(elt)
  otherwise elt
}

local function applyToAll = |fun, args| {
  let res = toplevels()
  foreach arg in args {
    if arg oftype ToplevelElements.class {
      res: add(arg: map(fun))
    } else {
      res: add(fun(arg))
    }
  }
  return res
}

----
Decorator to help define macros on top-level elements.

Macros applied on top-level elements may often return a `ToplevelElements` to
inject several top-level elements into the module, without using side effects.

When stacking such macros, for instance with the decorator notation, each macro
must be prepared to receive a `ToplevelElements` containing various kinds of
elements, instead of the decorated one.

For instance, a macro can work on a `struct` and inject several tooling
functions and augmentations, such as:

```golo
macro myMacro = |structure| -> toplevels(
  structure,
  `function("workOn" + structure: name())
      : withParameters("s"): body(...),
  `augment(structure): with(...)
)
```

Suppose that a macro `otherMacro` has a similar behavior. It is thus not possible to
stack these two macro, as in:
```golo
@otherMacro
@myMacro
struct Foo = {x}
```

since here, the `otherMacro` will not receive a `Struct`, but the
`ToplevelElements` returned by `myMacro`.

This decorator adapt the macro to deal with `ToplevelElements`, applying it to
each contained element if its type match the given one, and returning the
element unchanged otherwise.

In the previous example, we can use it on the two macros as:
```golo
@!toplevel(Struct.class)
macro myMacro = |structure| -> toplevels(
  structure,
  `function("workOn" + structure: name())
      : withParameters(s): body(...),
  `augment(structure: packageAndClass()): with(...)
)
```

The `myMacro` will be applied to its argument if its a `struct`, don't change
any other type, and if its argument is a `ToplevelElements`, it will be applied
to any contained element. The macro can therefore be stacked on the top of other
macros whatever their returned type.

To apply the macro on any type, just use `GoloElement.class` as a filter.

Moreover, the decorator makes the macro varargs. It can therefore be called on
several structures at once, like:

```golo
&myMacro {

struct Foo = {x}

struct Bar = {a, b}

}
```

This decorator should be banged.

- *param* `type`: the type of the nodes on which the macro must be applied.
----
function toplevel = |type| -> |mac| -> |args...| -> match {
  when args: size() == 1 and args: get(0) oftype GoloModule.class then mac(args: get(0))
  otherwise applyToAll(applyIfTypeMatches(type, mac), args)
}


let SYMBOLES = org.eclipse.golo.compiler.SymbolGenerator()

----
Generate a new unique name using an internal `SymbolGenerator`.

See also [SymbolGenerator](http://golo-lang.org/documentation/next/javadoc/org/eclipse/golo/compiler/SymbolGenerator.html)
----
function gensym = -> SYMBOLES: next()

----
Generate a new unique name using an internal `SymbolGenerator` with the given name as prefix.

See also [SymbolGenerator](http://golo-lang.org/documentation/next/javadoc/org/eclipse/golo/compiler/SymbolGenerator.html)
----
function gensym = |name| -> SYMBOLES: next(name)

----
Mangle the given name to ensure hygiene.

If the argument is a `LocalReference` or a `ReferenceLookup`, a new object with
the same type, but with a mangled name, is returned.

See also [SymbolGenerator](http://golo-lang.org/documentation/next/javadoc/org/eclipse/golo/compiler/SymbolGenerator.html)
----
function mangle = |name| -> match {
  when name oftype LocalReference.class then
    localRef(SYMBOLES: getFor(name: name()))
    : kind(name: kind())
    : synthetic(name: isSynthetic())
  when name oftype ReferenceLookup.class then
    refLookup(SYMBOLES: getFor(name: name()))
  otherwise SYMBOLES: getFor(name: toString())
}
