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

package fr.insalyon.citi.golo.doc;

import fr.insalyon.citi.golo.compiler.parser.*;
import fr.insalyon.citi.golo.compiler.utils.Register;
import fr.insalyon.citi.golo.compiler.utils.AbstractRegister;

import java.util.*;

class FunctionDocumentationsRegister extends AbstractRegister<String, FunctionDocumentation> {
  private static final long serialVersionUID = 1L;

  @Override
  protected Set<FunctionDocumentation> emptyValue() {
    return new TreeSet<>();
  }

  @Override
  protected Map<String, Set<FunctionDocumentation>> initMap() {
    return new TreeMap<>();
  }
}

class ModuleDocumentation implements DocumentationElement {

  private String moduleName;
  private int moduleDefLine;
  private String moduleDocumentation;

  private final Map<String, Integer> imports = new TreeMap<>();
  private final Map<String, Integer> moduleStates = new TreeMap<>();
  private final SortedSet<FunctionDocumentation> functions = new TreeSet<>();
  private final Map<String, AugmentationDocumentation> augmentations = new TreeMap<>();
  private final SortedSet<StructDocumentation> structs = new TreeSet<>();
  private final SortedSet<UnionDocumentation> unions = new TreeSet<>();
  private final Set<NamedAugmentationDocumentation> namedAugmentations = new TreeSet<>();

  ModuleDocumentation(ASTCompilationUnit compilationUnit) {
    new ModuleVisitor().visit(compilationUnit, null);
  }

  public SortedSet<StructDocumentation> structs() {
    return structs;
  }

  public SortedSet<UnionDocumentation> unions() {
    return unions;
  }

  public SortedSet<FunctionDocumentation> functions() {
    return functions(false);
  }

  public SortedSet<FunctionDocumentation> functions(boolean withLocal) {
    if (withLocal) { return functions; }
    TreeSet<FunctionDocumentation> pubFunctions = new TreeSet<>();
    for (FunctionDocumentation f : functions) {
      if (!f.local()) { pubFunctions.add(f); }
    }
    return pubFunctions;
  }

  public String moduleName() {
    return moduleName;
  }

  public String name() {
    return moduleName;
  }

  public int moduleDefLine() {
    return moduleDefLine;
  }

  public int line() {
    return moduleDefLine;
  }

  public String moduleDocumentation() {
    return (moduleDocumentation != null) ? moduleDocumentation : "\n";
  }

  public String documentation() {
    return moduleDocumentation();
  }

  public Map<String, Integer> moduleStates() {
    return moduleStates;
  }

  public Collection<AugmentationDocumentation> augmentations() {
    return augmentations.values();
  }

  public Collection<NamedAugmentationDocumentation> namedAugmentations() {
    return namedAugmentations;
  }

  public Map<String, Integer> imports() {
    return imports;
  }

  private class ModuleVisitor implements GoloParserVisitor {

    private Deque<Set<FunctionDocumentation>> functionContext = new LinkedList<>();
    private FunctionDocumentation currentFunction = null;
    private UnionDocumentation currentUnion;

    @Override
    public Object visit(ASTCompilationUnit node, Object data) {
      functionContext.push(functions);
      node.childrenAccept(this, data);
      return data;
    }

    @Override
    public Object visit(ASTModuleDeclaration node, Object data) {
      moduleName = node.getName();
      moduleDefLine = node.getLineInSourceCode();
      moduleDocumentation = node.getDocumentation();
      return data;
    }

    @Override
    public Object visit(ASTImportDeclaration node, Object data) {
      imports.put(node.getName(), node.getLineInSourceCode());
      return data;
    }

    @Override
    public Object visit(ASTToplevelDeclaration node, Object data) {
      node.childrenAccept(this, data);
      return data;
    }

    @Override
    public Object visit(ASTStructDeclaration node, Object data) {
      structs.add(new StructDocumentation()
        .name(node.getName())
        .documentation(node.getDocumentation())
        .line(node.getLineInSourceCode())
        .members(node.getMembers())
      );
      return data;
    }

    @Override
    public Object visit(ASTUnionDeclaration node, Object data) {
      this.currentUnion = new UnionDocumentation()
        .name(node.getName())
        .documentation(node.getDocumentation())
        .line(node.getLineInSourceCode());
      unions.add(this.currentUnion);
      return node.childrenAccept(this, data);
    }

    @Override
    public Object visit(ASTUnionValue node, Object data) {
      this.currentUnion.addValue(node.getName())
        .documentation(node.getDocumentation())
        .line(node.getLineInSourceCode())
        .members(node.getMembers());
      return data;
    }

    @Override
    public Object visit(ASTAugmentDeclaration node, Object data) {
      /* NOTE:
       * if multiple augmentations are defined for the same target
       * only the line and (non empty) documentation of the first one are kept.
       *
       * Maybe we should concatenate documentations since the golodoc merges
       * the functions documentations, but we could then generate not meaningful
       * content...
       */
      String target = node.getName();
      if (!augmentations.containsKey(target)) {
        augmentations.put(target, new AugmentationDocumentation()
            .target(target)
            .augmentationNames(node.getAugmentationNames())
            .line(node.getLineInSourceCode())
        );
      }
      functionContext.push(augmentations.get(target).documentation(node.getDocumentation()));
      node.childrenAccept(this, data);
      functionContext.pop();
      return data;
    }

    @Override
    public Object visit(ASTNamedAugmentationDeclaration node, Object data) {
      NamedAugmentationDocumentation augment = new NamedAugmentationDocumentation()
          .name(node.getName())
          .documentation(node.getDocumentation())
          .line(node.getLineInSourceCode());
      namedAugmentations.add(augment);
      functionContext.push(augment);
      node.childrenAccept(this, data);
      functionContext.pop();
      return data;
    }

    @Override
    public Object visit(ASTFunctionDeclaration node, Object data) {
      currentFunction = new FunctionDocumentation()
        .name(node.getName())
        .documentation(node.getDocumentation())
        .augmentation(node.isAugmentation())
        .line(node.getLineInSourceCode())
        .local(node.isLocal());
      functionContext.peek().add(currentFunction);
      node.childrenAccept(this, data);
      currentFunction = null;
      return data;
    }

    @Override
    public Object visit(ASTFunction node, Object data) {
      if (currentFunction != null) {
        currentFunction
          .arguments(node.getArguments())
          .varargs(node.isVarargs());
      }
      return data;
    }

    @Override
    public Object visit(ASTLetOrVar node, Object data) {
      if (node.isModuleState()) {
        moduleStates.put(node.getName(), node.getLineInSourceCode());
      }
      return data;
    }

    @Override
    public Object visit(ASTContinue node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTBreak node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTThrow node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTWhileLoop node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTForLoop node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTForEachLoop node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTTryCatchFinally node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTUnaryExpression node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTCommutativeExpression node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTAssociativeExpression node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTMethodInvocation node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTLiteral node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTCollectionLiteral node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTReference node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTAssignment node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTReturn node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTAnonymousFunctionInvocation node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTFunctionInvocation node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTConditionalBranching node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTCase node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTMatch node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTDecoratorDeclaration node, Object data) {
      return data;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTerror node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTQuotedBlock node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTUnquotedExpression node, Object data) {
      return data;
    }

    @Override
    public Object visit(ASTMacroInvocation node, Object data) {
      return data;
    }
  }
}
