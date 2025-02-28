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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.CheckForNull;
import org.sonar.plugins.python.api.tree.CompoundAssignmentStatement;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.tree.TreeVisitor;

public class CompoundAssignmentStatementImpl extends SimpleStatement implements CompoundAssignmentStatement {
  private final Expression lhsExpression;
  private final Token augAssignToken;
  private final Expression rhsExpression;
  private final Separators separators;


  public CompoundAssignmentStatementImpl(Expression lhsExpression, Token augAssignToken, Expression rhsExpression, Separators separators) {
    this.lhsExpression = lhsExpression;
    this.augAssignToken = augAssignToken;
    this.rhsExpression = rhsExpression;
    this.separators = separators;
  }

  @Override
  public Expression rhsExpression() {
    return rhsExpression;
  }

  @Override
  public Token compoundAssignmentToken() {
    return augAssignToken;
  }

  @Override
  public Expression lhsExpression() {
    return lhsExpression;
  }

  @CheckForNull
  @Override
  public Token separator() {
    return separators.last();
  }

  @Override
  public void accept(TreeVisitor visitor) {
    visitor.visitCompoundAssignment(this);
  }

  @Override
  public List<Tree> computeChildren() {
    return Stream.of(Arrays.asList(lhsExpression, augAssignToken, rhsExpression), separators.elements())
      .flatMap(List::stream).filter(Objects::nonNull).collect(Collectors.toList());
  }

  @Override
  public Kind getKind() {
    return Kind.COMPOUND_ASSIGNMENT;
  }
}
