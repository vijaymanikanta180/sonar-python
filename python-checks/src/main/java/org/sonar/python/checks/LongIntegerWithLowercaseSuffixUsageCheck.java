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
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.tree.NumericLiteral;
import org.sonar.plugins.python.api.tree.Tree;

@Rule(key = LongIntegerWithLowercaseSuffixUsageCheck.CHECK_KEY)
public class LongIntegerWithLowercaseSuffixUsageCheck extends PythonSubscriptionCheck {

  public static final String CHECK_KEY = "LongIntegerWithLowercaseSuffixUsage";
  private static final String MESSAGE = "Replace suffix in long integers from lower case \"l\" to upper case \"L\".";

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.NUMERIC_LITERAL, ctx -> {
      NumericLiteral pyNumericLiteralTree = (NumericLiteral) ctx.syntaxNode();
      String value = pyNumericLiteralTree.valueAsString();
      if (value.charAt(value.length() - 1) == 'l') {
        ctx.addIssue(pyNumericLiteralTree, MESSAGE);
      }
    });
  }
}
