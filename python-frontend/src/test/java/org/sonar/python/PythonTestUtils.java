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
package org.sonar.python;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.CheckForNull;
import org.mockito.Mockito;
import org.sonar.plugins.python.api.PythonFile;
import org.sonar.plugins.python.api.symbols.ClassSymbol;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.ClassDef;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ExpressionStatement;
import org.sonar.plugins.python.api.tree.FileInput;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.plugins.python.api.tree.Statement;
import org.sonar.plugins.python.api.tree.StatementList;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.python.parser.PythonParser;
import org.sonar.python.semantic.SymbolTableBuilder;
import org.sonar.python.tree.PythonTreeMaker;
import org.sonar.python.tree.TreeUtils;

import static org.assertj.core.api.Assertions.assertThat;

public final class PythonTestUtils {

  private static final PythonParser p = PythonParser.create();
  private static final PythonTreeMaker pythonTreeMaker = new PythonTreeMaker();

  private PythonTestUtils() {
  }

  public static String appendNewLine(String s) {
    return s + "\n";
  }

  public static FileInput parse(String... lines) {
    return parse(new SymbolTableBuilder(pythonFile("")), lines);
  }

  public static FileInput parse(SymbolTableBuilder symbolTableBuilder, String... lines) {
    String code = String.join(System.getProperty("line.separator"), lines);
    FileInput tree = pythonTreeMaker.fileInput(p.parse(code));
    symbolTableBuilder.visitFileInput(tree);
    return tree;
  }

  public static FileInput parseWithoutSymbols(String... lines) {
    String code = String.join(System.getProperty("line.separator"), lines);
    return pythonTreeMaker.fileInput(p.parse(code));
  }


  @CheckForNull
  public static <T extends Tree> T getFirstChild(Tree tree, Predicate<Tree> predicate) {
    for (Tree child : tree.children()) {
      if (predicate.test(child)) {
        return (T) child;
      }
      Tree firstChild = getFirstChild(child, predicate);
      if (firstChild != null) {
        return (T) firstChild;
      }
    }
    return null;
  }

  public static <T extends Tree> List<T> getAllDescendant(Tree tree, Predicate<Tree> predicate) {
    List<T> res = new ArrayList<>();
    for (Tree child : tree.children()) {
      if (predicate.test(child)) {
        res.add((T) child);
      }
      res.addAll(getAllDescendant(child, predicate));
    }
    return res;
  }

  public static <T extends Tree> T getFirstDescendant(Tree tree, Predicate<Tree> predicate) {
    List<T> descendants = getAllDescendant(tree, predicate);
    assertThat(descendants).isNotEmpty();
    return descendants.get(0);
  }

  public static <T extends Tree> T getLastDescendant(Tree tree, Predicate<Tree> predicate) {
    List<T> descendants = getAllDescendant(tree, predicate);
    assertThat(descendants).isNotEmpty();
    return descendants.get(descendants.size() - 1);
  }

  public static PythonFile pythonFile(String fileName) {
    PythonFile pythonFile = Mockito.mock(PythonFile.class);
    Mockito.when(pythonFile.fileName()).thenReturn(fileName);
    try {
      Mockito.when(pythonFile.uri()).thenReturn(Files.createTempFile(fileName, "py").toUri());
    } catch (IOException e) {
      throw new IllegalStateException("Cannot create temporary file");
    }
    return pythonFile;
  }

  public static Expression lastExpression(String... lines) {
    String code = String.join("\n", lines);
    FileInput fileInput = PythonTestUtils.parse(new SymbolTableBuilder("", pythonFile("mod1.py")), code);
    Statement statement = lastStatement(fileInput.statements());
    if (!(statement instanceof ExpressionStatement)) {
      assertThat(statement).isInstanceOf(FunctionDef.class);
      FunctionDef fnDef = (FunctionDef) statement;
      statement = lastStatement(fnDef.body());
    }
    assertThat(statement).isInstanceOf(ExpressionStatement.class);
    List<Expression> expressions = ((ExpressionStatement) statement).expressions();
    return expressions.get(expressions.size() - 1);
  }

  private static Statement lastStatement(StatementList statementList) {
    List<Statement> statements = statementList.statements();
    return statements.get(statements.size() - 1);
  }

  public static Expression lastExpressionInFunction(String... lines) {
    String code = "def f():\n  " + String.join("\n  ", lines);
    return lastExpression(code);
  }

  public static FunctionSymbol functionSymbol(PythonFile pythonFile, String... code) {
    FileInput fileInput = parse(new SymbolTableBuilder(pythonFile), code);
    FunctionDef functionDef = (FunctionDef) PythonTestUtils.getAllDescendant(fileInput, t -> t.is(Tree.Kind.FUNCDEF)).get(0);;
    return TreeUtils.getFunctionSymbolFromDef(functionDef);
  }

  public static FunctionSymbol functionSymbol(String... code) {
    return functionSymbol(PythonTestUtils.pythonFile("foo"), code);
  }

  public static FunctionSymbol lastFunctionSymbol(String... code) {
    FileInput fileInput = parse(new SymbolTableBuilder("package", pythonFile("mod")), code);
    FunctionDef functionDef = PythonTestUtils.getLastDescendant(fileInput, t -> t.is(Tree.Kind.FUNCDEF));;
    return TreeUtils.getFunctionSymbolFromDef(functionDef);
  }

  public static ClassSymbol lastClassSymbol(String... code) {
    FileInput fileInput = parse(new SymbolTableBuilder("package", PythonTestUtils.pythonFile("mod")), code);
    ClassDef classDef = PythonTestUtils.getLastDescendant(fileInput, t -> t.is(Tree.Kind.CLASSDEF));
    return TreeUtils.getClassSymbolFromDef(classDef);
  }

  public static Symbol lastSymbolFromDef(String... code) {
    FileInput fileInput = parse(new SymbolTableBuilder("package", pythonFile("mod")), code);
    Tree tree = PythonTestUtils.getLastDescendant(fileInput, t -> t.is(Tree.Kind.FUNCDEF, Tree.Kind.CLASSDEF));
    if (tree.is(Tree.Kind.CLASSDEF)) {
      return ((ClassDef) tree).name().symbol();
    }
    return ((FunctionDef) tree).name().symbol();
  }
}
