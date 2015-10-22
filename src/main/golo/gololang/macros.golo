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
TODO: module documentation
----
module gololang.macros

import gololang.ir

----
Don't expand the result of a macro.

This special macro configure the expansion process to not expand the macros
contained in the result of a macro expansion.
This can be useful for debugging, to diagnose intermediary steps.
----
@special
macro dontRecurse = |visitor| {
  visitor: recurse(false)
}

----
Don't try to expand regular calls as macros.

This special macro configure the expansion process to not be tried on regular
function invocations. Only explicit macro calls (prefixed with `&`) are
expanded.
----
@special
macro dontExpandRegularCalls = |visitor| {
  visitor: expandRegularCalls(false)
}

----
Adds a module in the macro resolution scope.

Modules added with this special macro are used for macro resolution, but are not
imported. They does not appear in the compiled code, and are not used for
regular function invocation resolutions.

Moreover, if the used module has a macro named `init`, a call to this macro,
using the additional parameters of `use`, is injected into the module.

For instance:

```golo
module Test

&use("my.macros", "answer", 42)
```

configures the visitor to lookup macros in the `my.macros` module, and expands
to

```golo
module Test

&my.macros.init("answer", 42)
```

The `init` macro may be contextual, to further modify the calling module.

- *param* `visitor`: the `MacroExpansionIrVisitor` used to expand this macro (injected)
- *param* `mod`: the name of the module to use as a string, or a class literal.
- *param `args`: additional arguments that will be passed to `init`
- *returns* the call to `init` if the macro exists, `null` otherwise.
----
@special
macro use = |visitor, mod, args...| {
  require(mod oftype ConstantStatement.class, "argument to `use` must be a constant")
  let value = mod: value()
  var name = null
  case {
    when value oftype String.class {
      name = value
    }
    when value oftype ClassReference.class {
      name = value: name()
    } otherwise {
      throw IllegalArgumentException("argument to `use` must be a string or a class literal")
    }
  }
  visitor: useMacroModule(name)
  let init = MacroInvocation.call(name + ".init"): withArgs(args)
  if visitor: macroExists(init) {
    return init
  }
}

