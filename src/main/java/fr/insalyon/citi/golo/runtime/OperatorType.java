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

package fr.insalyon.citi.golo.runtime;

public enum OperatorType {

  PLUS("+"),
  MINUS("-"),
  TIMES("*"),
  DIVIDE("/"),
  MODULO("%"),

  EQUALS("=="),
  NOTEQUALS("!="),
  LESS("<"),
  LESSOREQUALS("<="),
  MORE(">"),
  MOREOREQUALS(">="),

  AND("and"),
  OR("or"),
  NOT("not"),

  IS("is"),
  ISNT("isnt"),

  OFTYPE("oftype"),

  ORIFNULL("orIfNull"),

  ANON_CALL(""),
  METHOD_CALL(":"),
  ELVIS_METHOD_CALL("?:");

  private final String symbol;

  OperatorType(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    return symbol;
  }

  public static OperatorType fromString(String symbol) {
    switch (symbol) {
      case "+":
        return PLUS;
      case "-":
        return MINUS;
      case "*":
        return TIMES;
      case "/":
        return DIVIDE;
      case "%":
        return MODULO;
      case "<":
        return LESS;
      case "<=":
        return LESSOREQUALS;
      case "==":
        return EQUALS;
      case "!=":
        return NOTEQUALS;
      case ">":
        return MORE;
      case ">=":
        return MOREOREQUALS;
      case "and":
        return AND;
      case "or":
        return OR;
      case "not":
        return NOT;
      case "is":
        return IS;
      case "isnt":
        return ISNT;
      case "oftype":
        return OFTYPE;
      case ":":
        return METHOD_CALL;
      case "orIfNull":
        return ORIFNULL;
      case "?:":
        return ELVIS_METHOD_CALL;
      default:
        throw new IllegalArgumentException(symbol);
    }
  }
}
