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

import fr.insalyon.citi.golo.compiler.ir.LoopBreakFlowStatement;
import fr.insalyon.citi.golo.compiler.ir.LoopStatement;

public final class LoopBreakBuilder implements IrNodeBuilder<LoopBreakFlowStatement> {
  private LoopBreakFlowStatement.Type type;
  private LoopStatement enclosingLoop = null;
  private LoopBuilder enclosingLoopBuilder = null;

  public LoopBreakBuilder type(LoopBreakFlowStatement.Type t) {
    type = t;
    return this;
  }

  public LoopBreakBuilder loop(LoopStatement l) {
    enclosingLoop = l;
    return this;
  }

  public LoopBreakBuilder loop(LoopBuilder l) {
    enclosingLoopBuilder = l;
    return this;
  }

  public LoopBreakFlowStatement build() {
    LoopBreakFlowStatement st;
    switch (type) {
      case CONTINUE:
        st = LoopBreakFlowStatement.newContinue();
        break;
      case BREAK:
        st = LoopBreakFlowStatement.newBreak();
        break;
      default:
        throw new IllegalStateException("Unknown break type");
    }
    if (enclosingLoop != null) {
      st.setEnclosingLoop(enclosingLoop);
    } else if (enclosingLoopBuilder != null) {
      st.setEnclosingLoop(enclosingLoopBuilder.build());
    }
    return st;
  }
}
