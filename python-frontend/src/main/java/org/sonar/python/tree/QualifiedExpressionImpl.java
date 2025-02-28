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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Name;
import org.sonar.plugins.python.api.tree.QualifiedExpression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class QualifiedExpressionImpl extends PyTree implements QualifiedExpression {
  private final Name name;
  private final Expression qualifier;
  private final Token dotToken;

  public QualifiedExpressionImpl(Name name, Expression qualifier, Token dotToken) {
    this.name = name;
    this.qualifier = qualifier;
    this.dotToken = dotToken;
  }

  @Override
  public Expression qualifier() {
    return qualifier;
  }

  @Override
  public Token dotToken() {
    return dotToken;
  }

  @Override
  public Name name() {
    return name;
  }

  @Override
  public Kind getKind() {
    return Kind.QUALIFIED_EXPR;
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitQualifiedExpression(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(qualifier, dotToken, name).collect(Collectors.toList());
  }
}
