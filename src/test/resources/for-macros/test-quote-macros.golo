module golo.test.quoteMacros

import gololang.macros.Quote

macro unquotedM = -> gololang.ir.constant(42)
