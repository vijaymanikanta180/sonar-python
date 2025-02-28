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
package org.sonar.python.cfg;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.plugins.python.api.cfg.CfgBlock;
import org.sonar.plugins.python.api.cfg.CfgBranchingBlock;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;

public class PythonCfgBranchingBlock extends PythonCfgBlock implements CfgBranchingBlock {

  private final Tree branchingTree;
  private CfgBlock trueSuccessor;
  private CfgBlock falseSuccessor;

  public PythonCfgBranchingBlock(Tree branchingTree, @Nullable CfgBlock trueSuccessor, CfgBlock falseSuccessor) {
    this.branchingTree = branchingTree;
    this.trueSuccessor = trueSuccessor;
    this.falseSuccessor = falseSuccessor;
  }

  @Override
  public Set<CfgBlock> successors() {
    return new LinkedHashSet<>(Arrays.asList(trueSuccessor, falseSuccessor));
  }

  @CheckForNull
  @Override
  public CfgBlock syntacticSuccessor() {
    return null;
  }

  @Override
  public CfgBlock trueSuccessor() {
    return trueSuccessor;
  }

  @Override
  public CfgBlock falseSuccessor() {
    return falseSuccessor;
  }

  @Override
  public Tree branchingTree() {
    return branchingTree;
  }

  @Override
  void replaceSuccessors(Map<PythonCfgBlock, PythonCfgBlock> replacements) {
    trueSuccessor = replacements.getOrDefault(trueSuccessor, (PythonCfgBlock) trueSuccessor);
    falseSuccessor = replacements.getOrDefault(falseSuccessor, (PythonCfgBlock) falseSuccessor);
  }

  public void setTrueSuccessor(PythonCfgBlock trueSuccessor) {
    this.trueSuccessor = trueSuccessor;
  }

  @Override
  protected String toStringDisplayPosition() {
    Token token = branchingTree.firstToken();
    return token.line() + ":" + token.column() + ":";
  }

}
