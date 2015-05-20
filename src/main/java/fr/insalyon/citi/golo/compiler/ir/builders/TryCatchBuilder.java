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

package fr.insalyon.citi.golo.compiler.ir.builders;

import fr.insalyon.citi.golo.compiler.ir.TryCatchFinally;
import static gololang.macros.Utils.toBlock;

public final class TryCatchBuilder implements IrNodeBuilder<TryCatchFinally> {
  private String exceptionId;
  private Object tryBlock;
  private Object catchBlock;
  private Object finallyBlock;

  public TryCatchBuilder exception(String id) {
    this.exceptionId = id;
    return this;
  }

  public TryCatchBuilder tryBlock(Object b) {
    this.tryBlock = b;
    return this;
  }

  public TryCatchBuilder catchBlock(Object b) {
    this.catchBlock = b;
    return this;
  }

  public TryCatchBuilder finallyBlock(Object b) {
    this.finallyBlock = b;
    return this;
  }

  public TryCatchFinally build() {
    return new TryCatchFinally(exceptionId,
        toBlock(tryBlock),
        toBlock(catchBlock),
        toBlock(finallyBlock));
  }
}

