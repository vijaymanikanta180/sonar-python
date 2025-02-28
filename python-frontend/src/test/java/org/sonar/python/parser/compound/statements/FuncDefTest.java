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
package org.sonar.python.parser.compound.statements;

import org.junit.Before;
import org.junit.Test;
import org.sonar.python.api.PythonGrammar;
import org.sonar.python.PythonTestUtils;
import org.sonar.python.parser.RuleTest;

import static org.sonar.python.parser.PythonParserAssert.assertThat;

public class FuncDefTest extends RuleTest {

  @Before
  public void init() {
    setRootRule(PythonGrammar.FUNCDEF);
  }

  @Test
  public void realLife() {
    assertThat(p).matches(PythonTestUtils.appendNewLine("def func(): pass"));
  }

  @Test
  public void trueAsParameter() {
    assertThat(p).matches(PythonTestUtils.appendNewLine("def func(True): pass"));
  }

  @Test
  public void trailingComa() {
    assertThat(p).matches(PythonTestUtils.appendNewLine("def func(self, arg1, arg2, arg3, arg4, arg5, arg6, *args, **kwargs,): pass"));
  }

}
