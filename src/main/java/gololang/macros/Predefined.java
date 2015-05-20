/*
 * Copyright 2012-2015 Institut National des Sciences Appliquées de Lyon (INSA-Lyon)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package gololang.macros;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import fr.insalyon.citi.golo.compiler.ir.*;
import fr.insalyon.citi.golo.compiler.ir.builders.*;
import static gololang.macros.CodeBuilder.*;
import fr.insalyon.citi.golo.compiler.parser.GoloParser;

public final class Predefined {
  private Predefined() { }

  /**
   * Create an assignment to an external variable.
   * <p>
   * {@code setVar(a, "foo")} is equivalent to
   * <pre>
   * let ~a = "foo"
   * </pre>
   */
  public static Object setVar(Object reference, Object value) {
    return assignment(false, externalRef((ReferenceLookup) reference), value).build();
  }

  /**
   * String interpolation.
   * <p>
   * This macro converts a string containing references into a call to
   * {@code String.format} with the convenient format string and arguments.
   * <p>
   * For instance, a call {@code &strfmt("My name is ${name} and I'm ${age|.2f} years old")}
   * converts to a IR node equivalent to 
   * {@code String.format("My name is %s and I'm %.2f years old", name, age)}.
   * @param template The string to interpolate as a {@Code ConstantStatement} node.
   * @return a function invocation IR node representing a call to {@code String.format}.
   */
  public static Object strfmt(Object template) {
    String strTemplate = (String) ((ConstantStatement) template).getValue();
    Matcher matcher = Pattern.compile("\\$\\{([^}|]+)(\\|[^}|]+)?\\}").matcher(strTemplate);


    StringBuffer pattern= new StringBuffer();
    while (matcher.find()) {
      String fmt = "%s";
      if (matcher.group(2) != null) {
        fmt = "%" + matcher.group(2).substring(1);
      }
      matcher.appendReplacement(pattern, fmt);
    }
    matcher.appendTail(pattern);

    FunctionInvocationBuilder func = functionInvocation()
      .name("String.format")
      .arg(constant(pattern.toString()));
    matcher.reset();
    while (matcher.find()) {
      func.arg(refLookup(matcher.group(1)));
    }
    return func.build();
  }

  /**
   * Top level macro to add existing functions as augmentations to a given type.
   * <p>
   * The given functions <i>must</i> already have a explicit first argument representing the
   * augmented type value. For instance, one can do
   * {@code &augmentWithFunctions(java.util.Collection.class, ^java.util.Collections::disjoint)}
   * which is roughly equivalent to:
   * <pre>
   * augment java.util.Collection {
   *   function disjoint = |this, obj| -> java.util.Collections.disjoint(this, obj)
   * }
   * </pre>
   */
  public static Object augmentWithFunctions(Object type, Object funcRef, Object... funcRefs) {
    String typeName = ((GoloParser.ParserClassRef) ((ConstantStatement) type).getValue()).name;
    AugmentationBuilder theAugment = augmentType(typeName)
      .withFunction(createAugmentationFunction((ConstantStatement) funcRef));
    for (Object f : funcRefs) {
      theAugment.withFunction(createAugmentationFunction((ConstantStatement) f));
    }
    return theAugment;
  }

  private static FunctionDeclarationBuilder createAugmentationFunction(ConstantStatement funcRef) {
    return publicFunction().name(((GoloParser.FunctionRef) funcRef.getValue()).name)
      .param("this", "args").varargs()
      .block(
        returns(methodCall(
          methodCall(funcRef, methodInvocation("bindTo").arg(refLookup("this"))),
          methodInvocation("invoke").arg(refLookup("args"))
      )));
  }


}
