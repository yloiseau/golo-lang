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

package fr.insalyon.citi.golo.compiler.ir;

import java.util.Objects;

public final class MacroOperator implements Operator {
  private final String symbol;

  public MacroOperator(String symbol) {
    this.symbol = symbol;
  }

  @Override
  public String toString() {
    return "&" + symbol;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null) {
      return false;
    }
    if (this.getClass() != other.getClass()) {
      return false;
    }
    MacroOperator that = (MacroOperator) other;
    return Objects.equals(this.symbol, that.symbol);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(symbol);
  }
}
