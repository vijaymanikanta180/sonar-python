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
package org.sonar.python.index;


import org.junit.Test;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.python.tree.TreeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.python.PythonTestUtils.lastExpression;
import static org.sonar.python.index.DescriptorUtils.descriptor;

public class VariableDescriptorTest {

  @Test
  public void variableDescriptor() {
    VariableDescriptor x = lastVariableDescriptor(
      "x: int = 42",
      "x");
    // only typeshed symbols have it != null
    assertThat(x.annotatedType()).isNull();
  }

  private VariableDescriptor lastVariableDescriptor(String... code) {
    Symbol symbol = TreeUtils.getSymbolFromTree(lastExpression(code)).get();

    VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor(symbol);
    assertThat(variableDescriptor.kind()).isEqualTo(Descriptor.Kind.VARIABLE);
    assertThat(variableDescriptor.name()).isEqualTo(symbol.name());
    assertThat(variableDescriptor.fullyQualifiedName()).isEqualTo(symbol.fullyQualifiedName());
    return variableDescriptor;
  }
}
