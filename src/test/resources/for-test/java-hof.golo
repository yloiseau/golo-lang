----
Tests invocations strategies for pure golo functions, closures (with
closed parameters), varargs or not, when called from golo function of from java
functions expecting a SAM instance or a functional interface instance.
The reverse (using SAM or FI in golo functions expecting regular
FunctionReference) is also tested.

The purpose is to test all(?) higher order functions usage w.r.t. Java
interoperability

NOTE:
  It seams that all the stuff with FI is not necessary if we allow SAM to have
  default implementations (in the `isSAM` test), and thus always use the same
  proxy approach. Indeed, FI *are* just SAM with default implementations. The
  annotation is only really useful for Java to infer the lambda types.

  Actually no! I did not take into account that one can call default methods of
  FI, and thus SAM must be pure SAM (no default methods). I must rallback
  changes in FunctionCallSupport
----
module golo.test.JavaHOF

import org.hamcrest.MatcherAssert
import org.hamcrest
import java.util.`function

local function assertEquals = |value, expected| {
  assertThat(value, Matchers.equalTo(expected))
}

# ........................................................................... #

function callGoloFunction = |f| {
  return f(42)
}

function callInvokable = |f| {
  return f: invoke(42)
}

function callSAM = |f| {
  return f: call(42)
}

function callFunction = |f| {
  return f: apply(42)
}

function identity = -> |x| -> x

function varargsId = -> |a...| -> a: get(0)

function closed = |x| -> |y| -> x + y

function closedVar = |x| -> |a...| -> x + a: get(0)

# ........................................................................... #

local function plop = -> [1..5]: iterator()

function test_iterable = {
  let l = list[]
  foreach e in ^plop: to(Iterable.class) {
    l: add(e)
  }
  assertEquals(l, list[1, 2, 3, 4])
}

# TODO: test with varargs FI!
# TODO: test calling default method of FI

----
Baseline: function-like objects are used as intended, no conversion needed
(just to be sure)
----
function test_base_calls = {
  assertEquals(callGoloFunction(identity()), 42)
  assertEquals(callGoloFunction(varargsId()), 42)
  assertEquals(callGoloFunction(closed(27)), 69)
  assertEquals(callGoloFunction(closedVar(27)), 69)

  assertEquals(callInvokable(identity()), 42)
  assertEquals(callInvokable(varargsId()), 42)
  assertEquals(callInvokable(closed(27)), 69)
  assertEquals(callInvokable(closedVar(27)), 69)

  assertEquals(callInvokable(FunctionsTest.identityInvoke()), 42)
  assertEquals(callSAM(FunctionsTest.identitySAM()), 42)
  assertEquals(callFunction(FunctionsTest.identityFunction()), 42)

  assertEquals(FunctionsTest.callGoloFunction(identity()), 42)
  assertEquals(FunctionsTest.callGoloFunction(varargsId()), 42)
  assertEquals(FunctionsTest.callGoloFunction(closed(27)), 69)
  assertEquals(FunctionsTest.callGoloFunction(closedVar(27)), 69)

  assertEquals(FunctionsTest.callInvokable(FunctionsTest.identityInvoke()), 42)
  assertEquals(FunctionsTest.callSAM(FunctionsTest.identitySAM()), 42)
  assertEquals(FunctionsTest.callFunction(FunctionsTest.identityFunction()), 42)
}

----
Check that we can explicitly convert pure golo functions (FunctionReference) and
closures to SAM instance to use as argument into java (or golo) functions
accepting such instances. Tests for fixed arity and varargs.

NOTE: use `Predefined::asInterfaceInstance` that adapts for varargs collection
and spreading and delegates to `MethodHandleProxies::asInterfaceInstance`
----
function test_SAM_converted_gfun_calls = {
  assertEquals(callFunction(identity(): to(Function.class)), 42)
  assertEquals(callFunction(varargsId(): to(Function.class)), 42)
  assertEquals(callFunction(closed(27): to(Function.class)), 69)
  assertEquals(callFunction(closedVar(27): to(Function.class)), 69)

  assertEquals(callSAM(identity(): to(FunctionsTest$UnarySAM.class)), 42)
  assertEquals(callSAM(varargsId(): to(FunctionsTest$UnarySAM.class)), 42)
  assertEquals(callSAM(closed(27): to(FunctionsTest$UnarySAM.class)), 69)
  assertEquals(callSAM(closedVar(27): to(FunctionsTest$UnarySAM.class)), 69)

  assertEquals(FunctionsTest.callFunction(identity(): to(Function.class)), 42)
  assertEquals(FunctionsTest.callFunction(varargsId(): to(Function.class)), 42)
  assertEquals(FunctionsTest.callFunction(closed(27): to(Function.class)), 69)
  assertEquals(FunctionsTest.callFunction(closedVar(27): to(Function.class)), 69)

  assertEquals(FunctionsTest.callSAM(identity(): to(FunctionsTest$UnarySAM.class)), 42)
  assertEquals(FunctionsTest.callSAM(varargsId(): to(FunctionsTest$UnarySAM.class)), 42)
  assertEquals(FunctionsTest.callSAM(closed(27): to(FunctionsTest$UnarySAM.class)), 69)
  assertEquals(FunctionsTest.callSAM(closedVar(27): to(FunctionsTest$UnarySAM.class)), 69)
}

----
Check that we can explicitly convert pure golo functions (FunctionReference)
to FI instance to use as argument into java (or golo) functions accepting such
instances. Tests for fixed arity and varargs.

NOTE: use `Predefined::asFunctionalInterface` that delegates to `AdapterFabric`
----
function test_FI_converted_gfun_calls = {
  # FIXME: error explicit to FI with varargs
  #     java.lang.ClassCastException: Cannot cast java.lang.Integer to [Ljava.lang.Object;
  # assertEquals(callFunction(asFunctionalInterface(Function.class, varargsId())), 42)
  # assertEquals(FunctionsTest.callFunction(asFunctionalInterface(Function.class, varargsId())), 42)
  # assertEquals(callFunction(asFunctionalInterface(Function.class, closedVar(27))), 69)
  # assertEquals(FunctionsTest.callFunction(asFunctionalInterface(Function.class, closedVar(27))), 69)

  assertEquals(callFunction(asFunctionalInterface(Function.class, identity())), 42)
  assertEquals(FunctionsTest.callFunction(asFunctionalInterface(Function.class, identity())), 42)
  assertEquals(callFunction(asFunctionalInterface(Function.class, closed(27))), 69)
  assertEquals(FunctionsTest.callFunction(asFunctionalInterface(Function.class, closed(27))), 69)
}

----
Check that pure golo functions are implicitly converted to SAM instance
when used as argument to Java function that needs such instances.
Tests for fixed arity and varargs.

NOTE: wrapped in `FunctionCallSupport` using `Predefined::asInterfaceInstance`
----
function test_implicit_SAM_conversion = {
  assertEquals(FunctionsTest.callSAM(identity()), 42)
  assertEquals(FunctionsTest.callSAM(varargsId()), 42)
  assertEquals(FunctionsTest.callSAM(closed(27)), 69)
  assertEquals(FunctionsTest.callSAM(closedVar(27)), 69)
}

----
Check that pure golo functions are implicitly converted to FI instance
when used as argument to Java function that needs such instances.
Tests for fixed arity and varargs.

NOTE: wrapped in `FunctionCallSupport` using `LambdaMetafactory`
----
function test_implicit_FI_conversion = {
  # NOTE:
  #   Can be fixed by switching tests order in FunctionCallSupport and thus
  #   using a MethodHandleProxies instead of LambdaMetafactory, but this break
  #   other test with:
  #   bad proxy method: public default java.lang.String org.eclipse.golo.runtime.FunctionCallSupportTest$DummyFunctionalInterface.bangDaPlop()

  assertEquals(FunctionsTest.callFunction(identity()), 42)

  # FIXME: error implicit to FI with varargs
  #     LambdaConversionException: Type mismatch for lambda argument 0: class java.lang.Object is not convertible to class [Ljava.lang.Object;
  # assertEquals(FunctionsTest.callFunction(varargsId()), 42)
  
  # FIXME: error implicit closure to FI
  #     IllegalArgumentException: not a direct method handle
  # assertEquals(FunctionsTest.callFunction(closed(27)), 69)
  # assertEquals(FunctionsTest.callFunction(closedVar(27)), 69)
}

----
Check implicit and explicit conversion to SAM and FI when the target is varargs
----
function test_varargs_SAM_conversion = {
  # FIXME: fails if the class is FI
  assertEquals(FunctionsTest.callInvokable(identity()), 42)
  assertEquals(FunctionsTest.callInvokable(varargsId()), 42)
  assertEquals(FunctionsTest.callInvokable(closed(27)), 69)
  assertEquals(FunctionsTest.callInvokable(closedVar(27)), 69)

  assertEquals(FunctionsTest.callInvokable(identity(): to(FunctionsTest$Invokable.class)), 42)
  assertEquals(FunctionsTest.callInvokable(varargsId(): to(FunctionsTest$Invokable.class)), 42)
  assertEquals(FunctionsTest.callInvokable(closed(27): to(FunctionsTest$Invokable.class)), 69)
  assertEquals(FunctionsTest.callInvokable(closedVar(27): to(FunctionsTest$Invokable.class)), 69)

  # FIXME: error if the class is not FI
  #     AdapterDefinitionProblem: java.lang.ClassNotFoundException: gololang.FunctionsTest.Invokable
  # assertEquals(FunctionsTest.callInvokable(asFunctionalInterface(FunctionsTest$Invokable.class, identity())), 42)
  # assertEquals(FunctionsTest.callInvokable(asFunctionalInterface(FunctionsTest$Invokable.class, varargsId())), 42)
  # assertEquals(FunctionsTest.callInvokable(asFunctionalInterface(FunctionsTest$Invokable.class, closed(27))), 69)
  # assertEquals(FunctionsTest.callInvokable(asFunctionalInterface(FunctionsTest$Invokable.class, closedVar(27))), 69)
}

----
Check that java instances of SAM or FI can be explicitly converted to FR to be
used by golo function that expect regular golo functions.
NOTE: would be nice to allow implicit conversion, but how?
----
function test_asFunctionReference = {
  assertEquals(callGoloFunction(FunctionsTest.identityFunction(): asFunctionReference()), 42)
  assertEquals(callGoloFunction(asFunctionReference(FunctionsTest$Invokable.class, FunctionsTest.identityInvoke())), 42)
  assertEquals(callGoloFunction(asFunctionReference(FunctionsTest$UnarySAM.class, FunctionsTest.identitySAM())), 42)

  assertEquals(callInvokable(FunctionsTest.identityFunction(): asFunctionReference()), 42)
  assertEquals(callInvokable(asFunctionReference(FunctionsTest$Invokable.class, FunctionsTest.identityInvoke())), 42)
  assertEquals(callInvokable(asFunctionReference(FunctionsTest$UnarySAM.class, FunctionsTest.identitySAM())), 42)
}

