/*
 * SonarQube Python Plugin
 * Copyright (C) 2011-2022 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.python.cfg.fixpoint;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.plugins.python.api.PythonFile;
import org.sonar.plugins.python.api.cfg.CfgBlock;
import org.sonar.plugins.python.api.cfg.ControlFlowGraph;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.symbols.Usage;
import org.sonar.plugins.python.api.tree.AssignmentStatement;
import org.sonar.plugins.python.api.tree.BaseTreeVisitor;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.python.tree.TreeUtils;

import static org.sonar.plugins.python.api.tree.Tree.Kind.ASSIGNMENT_STMT;
import static org.sonar.plugins.python.api.tree.Tree.Kind.FUNCDEF;
import static org.sonar.plugins.python.api.tree.Tree.Kind.TRY_STMT;

/**
 * https://en.wikipedia.org/wiki/Reaching_definition
 * Data flow analysis to determinate what definitions may reach a given point in the code.
 * Program state is represented by a map where:
 *  - key is symbol
 *  - value is set of possible expressions that symbol may have been assigned to
 */
public class ReachingDefinitionsAnalysis {
  private final Map<CfgBlock, ProgramStateAtBlock> programStateByBlock = new HashMap<>();
  private final Map<Name, Set<Expression>> assignedExpressionByName = new HashMap<>();
  private final PythonFile pythonFile;
  private final Map<Symbol, Set<Name>> assignedNamesBySymbol = new HashMap<>();

  public ReachingDefinitionsAnalysis(PythonFile pythonFile) {
    this.pythonFile = pythonFile;
  }

  public Set<Expression> valuesAtLocation(Name variable) {
    Symbol symbol = variable.symbol();
    if (symbol == null) {
      return Collections.emptySet();
    }
    Set<Expression> assignedExpressions = assignedExpressionByName.get(variable);
    if (assignedExpressions != null) {
      // avoid recomputing cfg and analysis
      return assignedExpressions;
    }
    FunctionDef enclosingFunction = (FunctionDef) TreeUtils.firstAncestorOfKind(variable, FUNCDEF);
    if (enclosingFunction == null || TreeUtils.hasDescendant(enclosingFunction, t -> t.is(TRY_STMT))) {
      return Collections.emptySet();
    }
    ControlFlowGraph cfg = ControlFlowGraph.build(enclosingFunction, pythonFile);
    if (cfg == null) {
      return Collections.emptySet();
    }
    compute(cfg, enclosingFunction.localVariables());
    return assignedExpressionByName.getOrDefault(variable, Collections.emptySet());
  }

  private Set<Expression> getAssignedExpressions(Name variable, ProgramStateAtElement programStateAtElement) {
    Symbol symbol = variable.symbol();
    if (symbol == null) {
      return Collections.emptySet();
    }
    boolean hasMissingBindingUsage = symbol.usages().stream()
      .filter(Usage::isBindingUsage)
      .anyMatch(u -> !assignedNamesBySymbol.getOrDefault(symbol, Collections.emptySet()).contains(u.tree()));
    if (hasMissingBindingUsage) {
      return Collections.emptySet();
    }
    return Optional.ofNullable(programStateAtElement.out.get(symbol))
      .orElse(Collections.emptySet());
  }

  private void compute(ControlFlowGraph cfg, Set<Symbol> localVariables) {
    Map<Symbol, Set<Expression>> initialState = new HashMap<>();
    for (Symbol variable : localVariables) {
      initialState.put(variable, new HashSet<>());
    }
    Set<CfgBlock> blocks = cfg.blocks();
    blocks.forEach(block -> programStateByBlock.put(block, new ProgramStateAtBlock(block, initialState)));
    Deque<CfgBlock> workList = new ArrayDeque<>(blocks);
    while (!workList.isEmpty()) {
      CfgBlock currentBlock = workList.pop();
      ProgramStateAtBlock programStateAtBlock = programStateByBlock.get(currentBlock);
      boolean outHasChanged = programStateAtBlock.propagate();
      if (outHasChanged) {
        currentBlock.successors().forEach(workList::push);
      }
    }
    updateProgramStateByElement(cfg);
  }

  private void updateProgramStateByElement(ControlFlowGraph cfg) {
    for (CfgBlock block : cfg.blocks()) {
      Map<Symbol, Set<Expression>> inputState = programStateByBlock.get(block).in;
      for (Tree element : block.elements()) {
        ProgramStateAtElement programStateAtElement = new ProgramStateAtElement(inputState, element);
        element.accept(new BaseTreeVisitor() {
          @Override
          public void visitFunctionDef(FunctionDef pyFunctionDefTree) {
            // skip inner functions
          }
          @Override
          public void visitName(Name name) {
            assignedExpressionByName.put(name, getAssignedExpressions(name, programStateAtElement));
          }
        });
        inputState = programStateAtElement.out;
      }
    }
  }

  private class ProgramStateAtBlock {

    private final CfgBlock block;
    private Map<Symbol, Set<Expression>> in;
    private Map<Symbol, Set<Expression>> out = new HashMap<>();

    private ProgramStateAtBlock(CfgBlock block, Map<Symbol, Set<Expression>> initialState) {
      this.block = block;
      this.in = initialState;
      this.block.elements().forEach(element -> updateProgramState(element, out));
    }

    /**
     * Propagates forward: first computes the in set from all predecessors, then the out set.
     */
    private boolean propagate() {
      block.predecessors().forEach(predecessor -> in = join(in, programStateByBlock.get(predecessor).out));
      Map<Symbol, Set<Expression>> newOut = new HashMap<>(in);
      block.elements().forEach(element -> updateProgramState(element, newOut));
      boolean outHasChanged = !newOut.equals(out);
      out = newOut;
      return outHasChanged;
    }
  }

  private class ProgramStateAtElement {
    private final Map<Symbol, Set<Expression>> out;

    public ProgramStateAtElement(Map<Symbol, Set<Expression>> in, Tree element) {
      out = join(in, Collections.emptyMap());
      updateProgramState(element, out);
    }
  }

  private static Map<Symbol, Set<Expression>> join(Map<Symbol, Set<Expression>> programState1, Map<Symbol, Set<Expression>> programState2) {
    Map<Symbol, Set<Expression>> result = new HashMap<>();
    Set<Symbol> allKeys = new HashSet<>(programState1.keySet());
    allKeys.addAll(programState2.keySet());
    for (Symbol key : allKeys) {
      Set<Expression> union = new HashSet<>();
      union.addAll(programState1.getOrDefault(key, Collections.emptySet()));
      union.addAll(programState2.getOrDefault(key, Collections.emptySet()));
      result.put(key, union);
    }
    return result;
  }

  private void updateProgramState(Tree element, Map<Symbol, Set<Expression>> programState) {
    if (element.is(ASSIGNMENT_STMT)) {
      AssignmentStatement assignmentStatement = (AssignmentStatement) element;
      List<Expression> lhsExpressions = assignmentStatement.lhsExpressions().stream()
        .flatMap(exprList -> exprList.expressions().stream())
        .collect(Collectors.toList());
      if (lhsExpressions.size() != 1) {
        return;
      }
      Expression lhsExpression = lhsExpressions.get(0);
      if (!lhsExpression.is(Tree.Kind.NAME)) {
        return;
      }
      TreeUtils.getSymbolFromTree(lhsExpression).ifPresent(symbol -> {
        assignedNamesBySymbol.computeIfAbsent(symbol, s -> new HashSet<>()).add(((Name) lhsExpression));
        Set<Expression> assignedValues = new HashSet<>();
        assignedValues.add(assignmentStatement.assignedValue());
        // performing a strong update
        programState.put(symbol, assignedValues);
      });
    }
  }
}
