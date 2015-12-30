
package gololang;

import java.util.stream.Collector;
import java.util.List;
import java.util.ArrayList;

import static gololang.Predefined.fun;

public class Reducer {

  public final FunctionReference initFunction;
  public final FunctionReference accumFunction;
  public final FunctionReference finishFunction;

  public Reducer(FunctionReference init, FunctionReference accum, FunctionReference finish) {
    this.initFunction = init;
    this.accumFunction = accum;
    this.finishFunction = finish;
  }

  public Reducer withAccumulator(FunctionReference acc) {
    return new Reducer(initFunction, acc, finishFunction);
  }

  public Object init(Object reducible) throws Throwable {
    return initFunction.invoke(reducible);
  }

  public Object accumulate(Object acc, Object val) throws Throwable {
    return accumFunction.invoke(acc, val);
  }

  public Object finish(Object acc) throws Throwable {
    return finishFunction.invoke(acc);
  }

  public static Object constInit(Object val, Object red) {
    return new Tuple(false, val);
  }

  public static Object identityFinish(Tuple acc) {
    return acc.get(1);
  }

  public static Object redWrapper(FunctionReference func, Tuple a, Object v) throws Throwable {
    return new Tuple(a.get(0), func.invoke(a.get(1), v));
  }

  public static Reducer simple(Object zero, FunctionReference fun) throws Throwable {

    // FunctionReference init = Reducer.class.getMethod("constInit", )
    // return new FunctionReference(MethodHandles.publicLookup().unreflect(targetMethod), parameterNames);

    return new Reducer(
      (FunctionReference) fun("constInit", Reducer.class, 2),
      (FunctionReference) fun("redWrapper", Reducer.class, 3),
      (FunctionReference) fun("identityFinish", Reducer.class, 1)
    );
  }

  public static Collector toTuple() {
    return Collector.of(
        ArrayList::new,
        List::add,
        (left, right) -> { left.addAll(right); return left; },
        (col) -> Tuple.fromArray(col.toArray()));
  }


}
