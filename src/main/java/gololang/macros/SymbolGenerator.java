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

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Arrays.asList;

import fr.insalyon.citi.golo.compiler.ir.ReferenceLookup;
import fr.insalyon.citi.golo.compiler.ir.builders.LocalReferenceBuilder;

public class SymbolGenerator {
  private static final String FORMAT = "__$$_%s_%d";
  private static final String DEFAULT_NAME = "symbol";
  private int counter = 0;
  private final Map<String, String> symbols = new HashMap<>();
  private LinkedList<String> prefixes = new LinkedList<>();

  public SymbolGenerator reset() {
    counter = 0;
    symbols.clear();
    return this;
  }

  public static String gensym(String name, long suffix) {
    return String.format(FORMAT, name, suffix);
  }

  public static String gensym(String name) {
    return gensym(name, System.currentTimeMillis());
  }

  public static String gensym() {
    return gensym(DEFAULT_NAME);
  }

  public String name() {
    if (prefixes.isEmpty()) {
      return DEFAULT_NAME;
    }
    return String.join("_", prefixes);
  }

  public SymbolGenerator name(String n) {
    prefixes.clear();
    prefixes.addAll(asList(n.split("[_.]")));
    return this;
  }

  public SymbolGenerator pushScope(String n) {
    prefixes.add(n);
    return this;
  }

  public SymbolGenerator popScope() {
    prefixes.removeLast();
    return this;
  }

  public SymbolGenerator counter(int c) {
    counter = c;
    return this;
  }

  public int counter() {
    return counter;
  }

  public String next() {
    return next(null);
  }

  public String next(String localName) {
    return gensym(name() + (localName == null ? "" : ("_" + localName)), counter++);
  }

  public String current() {
    return gensym(name(), counter);
  }

  public String get(String localName) {
    if (localName.startsWith("$")) {
      return localName.substring(1);
    }
    symbols.putIfAbsent(localName, gensym(name() + "_" + localName));
    return symbols.get(localName);
  }

  public Map<String, String> symbols() {
    return unmodifiableMap(symbols);
  }

  public boolean hasSymbol(String name) {
    return symbols.containsKey(name);
  }

  public LocalReferenceBuilder ref(String name) {
    return CodeBuilder.localRef().name(get(name)).synthetic(true);
  }

  public ReferenceLookup look(String name) {
    return CodeBuilder.refLookup(get(name));
  }

}
