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
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sonar.plugins.python.api.cfg.CfgBlock;
import org.sonar.plugins.python.api.cfg.ControlFlowGraph;
import org.sonar.plugins.python.api.tree.Tree;

/**
 * Data flow analysis operating on the CFG.
 * The analysis starts at the entry node and moves forwards in the CFG.
 *
 * See https://en.wikipedia.org/wiki/Data-flow_analysis#Forward_analysis
 * See https://lara.epfl.ch/w/_media/sav08:schwartzbach.pdf (chapter "Forwards, Backwards, May, and Must")
 */
public abstract class ForwardAnalysis {

  protected final Map<CfgBlock, ProgramStateAtBlock> programStateByBlock = new HashMap<>();

  public void compute(ControlFlowGraph cfg) {
    ProgramState initialState = initialState();
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
  }

  public abstract ProgramState initialState();

  protected class ProgramStateAtBlock {

    private final CfgBlock block;
    protected ProgramState in;
    protected ProgramState out = initialState();

    private ProgramStateAtBlock(CfgBlock block, ProgramState initialState) {
      this.block = block;
      this.in = initialState;
      this.block.elements().forEach(element -> updateProgramState(element, out));
    }

    /**
     * Propagates forward: first computes the in set from all predecessors, then the out set.
     */
    private boolean propagate() {
      block.predecessors().forEach(predecessor -> in = in.join(programStateByBlock.get(predecessor).out));
      ProgramState newOut = in.copy();
      block.elements().forEach(element -> updateProgramState(element, newOut));
      boolean outHasChanged = !newOut.equals(out);
      out = newOut;
      return outHasChanged;
    }
  }

  public abstract void updateProgramState(Tree element, ProgramState programState);
}
