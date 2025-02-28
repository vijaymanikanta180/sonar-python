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
package org.sonar.python.tree;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.python.api.tree.ExpressionList;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.TreeVisitor;
import org.sonar.plugins.python.api.tree.Tree;

public class SubscriptionExpressionImpl extends PyTree implements SubscriptionExpression {

  private final Expression object;
  private final Token lBracket;
  private final ExpressionList subscripts;
  private final Token rBracket;

  public SubscriptionExpressionImpl(Expression object, Token lBracket, ExpressionList subscripts, Token rBracket) {
    this.object = object;
    this.lBracket = lBracket;
    this.subscripts = subscripts;
    this.rBracket = rBracket;
  }

  @Override
  public Expression object() {
    return object;
  }

  @Override
  public Token leftBracket() {
    return lBracket;
  }

  @Override
  public ExpressionList subscripts() {
    return subscripts;
  }

  @Override
  public Token rightBracket() {
    return rBracket;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitSubscriptionExpression(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(object, lBracket, subscripts, rBracket).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public Kind getKind() {
    return Kind.SUBSCRIPTION;
  }
}
