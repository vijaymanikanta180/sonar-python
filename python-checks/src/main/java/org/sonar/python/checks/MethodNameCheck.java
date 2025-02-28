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
package org.sonar.python.checks;

import org.sonar.check.Rule;
import org.sonar.plugins.python.api.tree.FunctionDef;

import static org.sonar.python.checks.CheckUtils.classHasInheritance;
import static org.sonar.python.checks.CheckUtils.getParentClassDef;

@Rule(key = MethodNameCheck.CHECK_KEY)
public class MethodNameCheck extends AbstractFunctionNameCheck {
  public static final String CHECK_KEY = "S100";

  @Override
  public String typeName() {
    return "method";
  }

  @Override
  public boolean shouldCheckFunctionDeclaration(FunctionDef pyFunctionDefTree) {
    return pyFunctionDefTree.isMethodDefinition() && !classHasInheritance(getParentClassDef(pyFunctionDefTree));
  }
}
