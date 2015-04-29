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

package fr.insalyon.citi.golo.compiler;

import fr.insalyon.citi.golo.compiler.ir.*;

import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Collection;

import static fr.insalyon.citi.golo.compiler.GoloCompilationException.Problem.Type.*;

class LocalReferenceAssignmentAndVerificationVisitor extends DummyIrVisitor {

  private GoloModule module = null;
  private AssignmentCounter assignmentCounter = new AssignmentCounter();
  private Deque<GoloFunction> functionStack = new LinkedList<>();
  private Deque<ReferenceTable> tableStack = new LinkedList<>();
  private Deque<Set<LocalReference>> assignmentStack = new LinkedList<>();
  private Deque<LoopStatement> loopStack = new LinkedList<>();
  private GoloCompilationException.Builder exceptionBuilder;
  private final HashSet<LocalReference> uninitializedReferences = new HashSet<>();


  private static class AssignmentCounter {

    private int counter = 0;

    public int next() {
      return counter++;
    }

    public void reset() {
      counter = 0;
    }
  }

  public LocalReferenceAssignmentAndVerificationVisitor() { }

  public LocalReferenceAssignmentAndVerificationVisitor(GoloCompilationException.Builder builder) {
    this();
    setExceptionBuilder(builder);
  }

  public void setExceptionBuilder(GoloCompilationException.Builder builder) {
    exceptionBuilder = builder;
  }

  private GoloCompilationException.Builder getExceptionBuilder() {
    if (exceptionBuilder == null) {
      exceptionBuilder = new GoloCompilationException.Builder(module.getPackageAndClass().toString());
    }
    return exceptionBuilder;
  }

  @Override
  public void visitModule(GoloModule module) {
    this.module = module;
    super.visitModule(module);
  }

  @Override
  public void visitFunction(GoloFunction function) {
    assignmentCounter.reset();
    functionStack.push(function);
    ReferenceTable table = function.getBlock().getReferenceTable();
    for (String parameterName : function.getParameterNames()) {
      LocalReference reference = table.get(parameterName);
      uninitializedReferences.remove(reference);
      if (reference == null) {
        if (!function.isSynthetic()) {
          throw new IllegalStateException("[please report this bug] " + parameterName + " is not declared in the references of function " + function.getName());
        }
      } else {
        reference.setIndex(assignmentCounter.next());
      }
    }
    function.getBlock().accept(this);
    String selfName = function.getSyntheticSelfName();
    if (function.isSynthetic() && selfName != null) {
      LocalReference self = function.getBlock().getReferenceTable().get(selfName);
      ClosureReference closureReference = new ClosureReference(function);
      for (String syntheticRef : function.getSyntheticParameterNames()) {
        closureReference.addCapturedReferenceName(syntheticRef);
      }
      AssignmentStatement assign = new AssignmentStatement(self, closureReference);
      function.getBlock().prependStatement(assign);
    }
    functionStack.pop();
  }

  @Override
  public void visitBlock(Block block) {
    ReferenceTable table = block.getReferenceTable();
    extractUninitializedReferences(table);
    tableStack.push(table);
    assignmentStack.push(extractAssignedReferences(table));
    super.visitBlock(block);
    assignmentStack.pop();
    tableStack.pop();
  }

  private void extractUninitializedReferences(ReferenceTable table) {
    for (LocalReference reference : table.ownedReferences()) {
      if (reference.getIndex() < 0 && !reference.isModuleState()) {
        reference.setIndex(assignmentCounter.next());
        uninitializedReferences.add(reference);
      }
    }
  }

  private Set<LocalReference> extractAssignedReferences(ReferenceTable table) {
    HashSet<LocalReference> assigned = new HashSet<>();
    if (table == functionStack.peek().getBlock().getReferenceTable()) {
      for (String param : functionStack.peek().getParameterNames()) {
        assigned.add(table.get(param));
      }
    }
    if (!assignmentStack.isEmpty()) {
      assigned.addAll(assignmentStack.peek());
    }
    return assigned;
  }

  @Override
  public void visitFunctionInvocation(FunctionInvocation functionInvocation) {
    if (tableStack.peek().hasReferenceFor(functionInvocation.getName())) {
      if (tableStack.peek().get(functionInvocation.getName()).isModuleState()) {
        functionInvocation.setOnModuleState(true);
      } else {
        functionInvocation.setOnReference(true);
      }
    }
    super.visitFunctionInvocation(functionInvocation);
  }


  @Override
  public void visitAssignmentStatement(AssignmentStatement assignmentStatement) {
    LocalReference reference = assignmentStatement.getLocalReference();
    Set<LocalReference> assignedReferences = assignmentStack.peek();
    if (assigningConstant(reference, assignedReferences)) {
      getExceptionBuilder().report(ASSIGN_CONSTANT, assignmentStatement.getASTNode(),
          "Assigning `" + reference.getName() +
              "` at " + assignmentStatement.getPositionInSourceCode() +
              " but it is a constant reference"
      );
    } else if (redeclaringReferenceInBlock(assignmentStatement, reference, assignedReferences)) {
      getExceptionBuilder().report(REFERENCE_ALREADY_DECLARED_IN_BLOCK, assignmentStatement.getASTNode(),
          "Declaring a duplicate reference `" + reference.getName() +
              "` at " + assignmentStatement.getPositionInSourceCode()
      );
    }
    bindReference(reference);
    assignedReferences.add(reference);
    assignmentStatement.getExpressionStatement().accept(this);
    if (assignmentStatement.isDeclaring() && !reference.isSynthetic()) {
      uninitializedReferences.remove(reference);
    }
  }

  private void bindReference(LocalReference reference) {
    ReferenceTable table = tableStack.peek();
    if (reference.getIndex() < 0) {
      if (table.hasReferenceFor(reference.getName())) {
        reference.setIndex(table.get(reference.getName()).getIndex());
      } else if (reference.isSynthetic()) {
        reference.setIndex(assignmentCounter.next());
        table.add(reference);
      }
    }
  }
  
  private boolean redeclaringReferenceInBlock(AssignmentStatement assignmentStatement, LocalReference reference, Set<LocalReference> assignedReferences) {
    return !reference.isSynthetic() && assignmentStatement.isDeclaring() && referenceNameExists(reference, assignedReferences);
  }

  private boolean assigningConstant(LocalReference reference, Set<LocalReference> assignedReferences) {
    return reference.isConstant() && (
        assignedReferences.contains(reference) ||
        ( reference.isModuleState() && !functionStack.peek().isModuleInit()));
  }

  private boolean referenceNameExists(LocalReference reference, Set<LocalReference> referencesInBlock) {
    for (LocalReference ref : referencesInBlock) {
      if ((ref != null) && ref.getName().equals(reference.getName())) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void visitReferenceLookup(ReferenceLookup referenceLookup) {
    ReferenceTable table = tableStack.peek();
    if (!table.hasReferenceFor(referenceLookup.getName())) {
      getExceptionBuilder().report(UNDECLARED_REFERENCE, referenceLookup.getASTNode(),
          "Undeclared reference `" + referenceLookup.getName() + "` at " + referenceLookup.getPositionInSourceCode());
    }
    LocalReference ref = referenceLookup.resolveIn(table);
    if (isUninitialized(ref)) {
      getExceptionBuilder().report(UNINITIALIZED_REFERENCE_ACCESS, referenceLookup.getASTNode(),
          "Uninitialized reference `" + ref.getName() + "` at " + referenceLookup.getPositionInSourceCode());
    }
  }

  private boolean isUninitialized(LocalReference ref) {
    return ref != null && !ref.isSynthetic() && !ref.isModuleState() && uninitializedReferences.contains(ref);
  }

  @Override
  public void visitLoopStatement(LoopStatement loopStatement) {
    loopStack.push(loopStatement);
    super.visitLoopStatement(loopStatement);
    loopStack.pop();
  }

  @Override
  public void visitClosureReference(ClosureReference closureReference) {
    GoloFunction target = closureReference.getTarget();
    for (String name : target.getSyntheticParameterNames()) {
      closureReference.addCapturedReferenceName(name);
    }
  }

  @Override
  public void visitLoopBreakFlowStatement(LoopBreakFlowStatement loopBreakFlowStatement) {
    if (loopStack.isEmpty()) {
      getExceptionBuilder().report(BREAK_OR_CONTINUE_OUTSIDE_LOOP,
          loopBreakFlowStatement.getASTNode(),
          "continue or break statement outside a loop at " + loopBreakFlowStatement.getPositionInSourceCode());
    } else {
      loopBreakFlowStatement.setEnclosingLoop(loopStack.peek());
    }
  }

}
