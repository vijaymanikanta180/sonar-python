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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.CallExpression;
import org.sonar.plugins.python.api.tree.Expression;
import org.sonar.plugins.python.api.tree.ExpressionStatement;
import org.sonar.plugins.python.api.tree.InExpression;
import org.sonar.plugins.python.api.tree.SubscriptionExpression;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.plugins.python.api.types.InferredType;
import org.sonar.python.tree.TreeUtils;

@Rule(key = "S2201")
public class IgnoredPureOperationsCheck extends PythonSubscriptionCheck {

  private static final String MESSAGE_FORMAT = "The return value of \"%s\" must be used.";

  private static final Set<String> PURE_FUNCTIONS = new HashSet<>(Arrays.asList(
    "set",
    "dict",
    "frozenset",
    "str",
    "repr",
    "ascii",
    "ord",
    "hex",
    "oct",
    "bin",
    "bool",
    "bytes",
    "memoryview",
    "bytearray",
    "abs",
    "round",
    "min",
    "max",
    "divmod",
    "sum",
    "pow",
    "sorted",
    "filter",
    "enumerate",
    "reversed",
    "range",
    "slice",
    "zip",
    "help",
    "dir",
    "id",
    "object",
    "staticmethod",
    "classmethod",
    "property",
    "type",
    "isinstance",
    "issubclass",
    "callable",
    "format",
    "vars",
    "locals",
    "globals",
    "super",
    "str.capitalize",
    "str.casefold",
    "str.center",
    "str.count",
    "str.endswith",
    "str.expandtabs",
    "str.find",
    "str.format",
    "str.format_map",
    "str.index",
    "str.isalnum",
    "str.isalpha",
    "str.isascii",
    "str.isdecimal",
    "str.isdigit",
    "str.isidentifier",
    "str.islower",
    "str.isnumeric",
    "str.isprintable",
    "str.isspace",
    "str.istitle",
    "str.isupper",
    "str.join",
    "str.ljust",
    "str.lower",
    "str.lstrip",
    "str.maketrans",
    "str.partition",
    "str.replace",
    "str.rfind",
    "str.rindex",
    "str.rjust",
    "str.rpartition",
    "str.rsplit",
    "str.rstrip",
    "str.split",
    "str.splitlines",
    "str.startswith",
    "str.strip",
    "str.swapcase",
    "str.title",
    "str.translate",
    "str.upper",
    "str.zfill",
    "bytes.capitalize",
    "bytes.center",
    "bytes.count",
    "bytes.decode",
    "bytes.endswith",
    "bytes.expandtabs",
    "bytes.find",
    "bytes.fromhex",
    "bytes.hex",
    "bytes.index",
    "bytes.isalnum",
    "bytes.isalpha",
    "bytes.isascii",
    "bytes.isdigit",
    "bytes.islower",
    "bytes.isspace",
    "bytes.istitle",
    "bytes.isupper",
    "bytes.join",
    "bytes.ljust",
    "bytes.lower",
    "bytes.lstrip",
    "bytes.maketrans",
    "bytes.partition",
    "bytes.replace",
    "bytes.rfind",
    "bytes.rindex",
    "bytes.rjust",
    "bytes.rpartition",
    "bytes.rsplit",
    "bytes.rstrip",
    "bytes.split",
    "bytes.splitlines",
    "bytes.startswith",
    "bytes.strip",
    "bytes.swapcase",
    "bytes.title",
    "bytes.translate",
    "bytes.upper",
    "bytes.zfill",
    "bytearray.capitalize",
    "bytearray.center",
    "bytearray.count",
    "bytearray.decode",
    "bytearray.endswith",
    "bytearray.expandtabs",
    "bytearray.find",
    "bytearray.fromhex",
    "bytearray.hex",
    "bytearray.index",
    "bytearray.isalnum",
    "bytearray.isalpha",
    "bytearray.isascii",
    "bytearray.isdigit",
    "bytearray.islower",
    "bytearray.isspace",
    "bytearray.istitle",
    "bytearray.isupper",
    "bytearray.join",
    "bytearray.ljust",
    "bytearray.lower",
    "bytearray.lstrip",
    "bytearray.maketrans",
    "bytearray.partition",
    "bytearray.replace",
    "bytearray.rfind",
    "bytearray.rindex",
    "bytearray.rjust",
    "bytearray.rpartition",
    "bytearray.rsplit",
    "bytearray.rstrip",
    "bytearray.split",
    "bytearray.splitlines",
    "bytearray.startswith",
    "bytearray.strip",
    "bytearray.swapcase",
    "bytearray.title",
    "bytearray.translate",
    "bytearray.upper",
    "bytearray.zfill",
    "memoryview.cast",
    "memoryview.hex",
    "memoryview.tobytes",
    "memoryview.tolist",
    "memoryview.toreadonly",
    "int.as_integer_ratio",
    "int.bit_length",
    "int.conjugate",
    "int.from_bytes",
    "int.to_bytes",
    "float.as_integer_ratio",
    "float.conjugate",
    "float.fromhex",
    "float.hex",
    "float.is_integer",
    "bool.as_integer_ratio",
    "bool.bit_length",
    "bool.conjugate",
    "bool.from_bytes",
    "bool.to_bytes",
    "list.copy",
    "list.count",
    "list.index",
    "tuple.count",
    "tuple.index",
    "range.count",
    "range.index",
    "set.copy",
    "set.difference",
    "set.intersection",
    "set.isdisjoint",
    "set.issubset",
    "set.issuperset",
    "set.symmetric_difference",
    "set.union",
    "frozenset.copy",
    "frozenset.difference",
    "frozenset.intersection",
    "frozenset.isdisjoint",
    "frozenset.issubset",
    "frozenset.issuperset",
    "frozenset.symmetric_difference",
    "frozenset.union",
    "dict.copy",
    "dict.dicfromkeys",
    "dict.get",
    "dict.items",
    "dict.keys",
    "dict.values"
  ));

  private static final Set<String> PURE_GETITEM_TYPES = new HashSet<>(Arrays.asList(
    "str",
    "bytes",
    "bytearray",
    "memoryview",
    "list",
    "tuple",
    "range",
    "dict"
  ));

  private static final Set<String> PURE_CONTAINS_TYPES = new HashSet<>();
  static {
    PURE_CONTAINS_TYPES.addAll(PURE_GETITEM_TYPES);
    PURE_CONTAINS_TYPES.addAll(Arrays.asList(
      "set",
      "frozenset"
    ));
  }

  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(Tree.Kind.EXPRESSION_STMT, ctx -> {
      ExpressionStatement expressionStatement = (ExpressionStatement) ctx.syntaxNode();
      if (TreeUtils.firstAncestor(expressionStatement, IgnoredPureOperationsCheck::isInTryBlock) != null) {
        return;
      }

      expressionStatement.expressions().forEach(expression -> checkExpression(ctx, expression));
    });
  }

  private static void checkExpression(SubscriptionContext ctx, Expression expression) {
    if (expression.is(Tree.Kind.CALL_EXPR)) {
      CallExpression callExpression = (CallExpression) expression;
      Symbol symbol = callExpression.calleeSymbol();
      if (symbol == null) {
        return;
      }

      String fqn = symbol.fullyQualifiedName();
      if (fqn != null && PURE_FUNCTIONS.contains(fqn)) {
        ctx.addIssue(callExpression.callee(), String.format(MESSAGE_FORMAT, fqn));
      }
    } else if (expression.is(Tree.Kind.SUBSCRIPTION)) {
      SubscriptionExpression subscriptionExpression = (SubscriptionExpression) expression;
      InferredType type = subscriptionExpression.object().type();
      if (PURE_GETITEM_TYPES.stream().anyMatch(type::canOnlyBe)) {
        ctx.addIssue(subscriptionExpression, String.format(MESSAGE_FORMAT, "__getitem__"));
      }
    } else if (expression.is(Tree.Kind.IN)) {
      InExpression inExpression = (InExpression) expression;
      InferredType type = inExpression.rightOperand().type();
      if (PURE_CONTAINS_TYPES.stream().anyMatch(type::canOnlyBe)) {
        ctx.addIssue(inExpression, String.format(MESSAGE_FORMAT, "__contains__"));
      }
    }
  }

  private static boolean isInTryBlock(Tree tree) {
    // We need a direct STATEMENT_LIST descendant of a TRY_STATEMENT, other clauses are
    // descendants of except or finally clauses.
    return tree.is(Tree.Kind.STATEMENT_LIST) && tree.parent().is(Tree.Kind.TRY_STMT);
  }

}
