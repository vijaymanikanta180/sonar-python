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
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.ParameterList;
import org.sonar.plugins.python.api.tree.Parameter;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;

@Rule(key = "S2733")
public class ExitHasBadArgumentsCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE_ADD = "Add the missing argument.";
  private static final String MESSAGE_REMOVE = "Remove the unnecessary argument.";

  private static final int EXIT_ARGUMENTS_NUMBER = 4;

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.FUNCDEF, ctx -> {
      FunctionDef funcDef = (FunctionDef) ctx.syntaxNode();
      if (!funcDef.name().name().equals("__exit__")) {
        return;
      }
      ParameterList parameters = funcDef.parameters();
      int arity = 0;
      if (parameters != null) {
        if (parameters.nonTuple().stream().anyMatch(ExitHasBadArgumentsCheck::isStarredParam)) {
          return;
        }
        arity = parameters.all().size();
      }
      raiseIssue(ctx, funcDef, arity);
    });
  }

  private static boolean isStarredParam(Parameter param) {
    return param.starToken() != null;
  }

  private static void raiseIssue(SubscriptionContext ctx, FunctionDef tree, int argumentsNumber) {
    if (argumentsNumber != EXIT_ARGUMENTS_NUMBER) {
      String message = MESSAGE_ADD;
      if (argumentsNumber > EXIT_ARGUMENTS_NUMBER) {
        message = MESSAGE_REMOVE;
      }
      Tree funcName = tree.name();
      Token rightParenthesis = tree.rightPar();
      ctx.addIssue(funcName.firstToken(), rightParenthesis, message);
    }
  }
}

