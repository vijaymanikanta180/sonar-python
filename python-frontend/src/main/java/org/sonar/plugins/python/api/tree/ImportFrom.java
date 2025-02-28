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
package org.sonar.plugins.python.api.tree;

import java.util.List;
import javax.annotation.CheckForNull;

/**
 * Import From statement
 *
 * <pre>
 *   {@link #fromKeyword()} {@link #dottedPrefixForModule()} {@link #module()} {@link #importKeyword()} {@link #importedNames()}
 * </pre>
 *
 * See https://docs.python.org/3/reference/simple_stmts.html#grammar-token-import-stmt
 */
public interface ImportFrom extends ImportStatement {
  Token fromKeyword();

  @CheckForNull
  DottedName module();

  Token importKeyword();

  /**
   * prefix '.' tokens used in relative import
   * <pre>
   *   from ..a.b import fn
   *   #    ^^
   * </pre>
   */
  List<Token> dottedPrefixForModule();

  List<AliasedName> importedNames();

  boolean isWildcardImport();

  @CheckForNull
  Token wildcard();

  boolean hasUnresolvedWildcardImport();
}
