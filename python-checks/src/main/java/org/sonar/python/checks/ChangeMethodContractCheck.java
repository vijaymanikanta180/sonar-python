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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.sonar.check.Rule;
import org.sonar.plugins.python.api.LocationInFile;
import org.sonar.plugins.python.api.PythonSubscriptionCheck;
import org.sonar.plugins.python.api.SubscriptionContext;
import org.sonar.plugins.python.api.symbols.FunctionSymbol;
import org.sonar.plugins.python.api.symbols.Symbol;
import org.sonar.plugins.python.api.tree.FunctionDef;
import org.sonar.python.semantic.SymbolUtils;

import static org.sonar.plugins.python.api.symbols.Symbol.Kind.FUNCTION;
import static org.sonar.plugins.python.api.tree.Tree.Kind.FUNCDEF;

@Rule(key = "S2638")
public class ChangeMethodContractCheck extends PythonSubscriptionCheck {
  @Override
  public void initialize(Context context) {
    context.registerSyntaxNodeConsumer(FUNCDEF, ctx -> {
      FunctionDef functionDef = (FunctionDef) ctx.syntaxNode();
      String functionName = functionDef.name().name();
      if (functionName.startsWith("__") && functionName.endsWith("__")) {
        // ignore special methods
        return;
      }
      Symbol symbol = functionDef.name().symbol();
      if (symbol == null || symbol.kind() != FUNCTION) {
        return;
      }
      FunctionSymbol functionSymbol = (FunctionSymbol) symbol;
      if (functionSymbol.hasVariadicParameter() || functionSymbol.hasDecorators()) {
        // ignore function declarations with packed params
        return;
      }
      checkMethodContract(ctx, functionSymbol);
    });
  }

  private static void checkMethodContract(SubscriptionContext ctx, FunctionSymbol method) {
    SymbolUtils.getOverriddenMethod(method).ifPresent(overriddenMethod -> {
      if (overriddenMethod.hasVariadicParameter() || overriddenMethod.hasDecorators()) {
        // ignore function declarations with packed params
        return;
      }

      int paramsDiff = method.parameters().size() - overriddenMethod.parameters().size();

      if (paramsDiff != 0 && overriddenMethod.parameters().stream().anyMatch(FunctionSymbol.Parameter::isKeywordOnly)) {
        reportIssue(ctx, "Change this method signature to accept the same arguments as the method it overrides.", method.definitionLocation(), overriddenMethod);
        return;
      }

      if (paramsDiff > 0) {
        reportOnExtraParameters(ctx, method, overriddenMethod);
      } else if (paramsDiff < 0) {
        reportOnMissingParameters(ctx, method, overriddenMethod);
      } else {
        checkDefaultValuesAndParamNames(ctx, method, overriddenMethod);
      }
    });
  }

  private static void reportOnMissingParameters(SubscriptionContext ctx, FunctionSymbol method, FunctionSymbol overriddenMethod) {
    int indexFirstMissingParam = method.parameters().size();
    List<FunctionSymbol.Parameter> overriddenParams = overriddenMethod.parameters();
    String missingParameters = overriddenParams.subList(indexFirstMissingParam, overriddenParams.size())
      .stream()
      .map(FunctionSymbol.Parameter::name)
      .collect(Collectors.joining(" "));
    if (!missingParameters.isEmpty()) {
      reportIssue(ctx, "Add missing parameters " + missingParameters.trim() + ".", method.definitionLocation(), overriddenMethod);
    }
  }

  private static void reportIssue(SubscriptionContext ctx, String message, @Nullable LocationInFile location, FunctionSymbol overriddenMethod) {
    Optional.ofNullable(location).ifPresent(issueLocation -> {
      LocationInFile secondaryLocation = overriddenMethod.definitionLocation();
      if (secondaryLocation != null) {
        PreciseIssue preciseIssue = ctx.addIssue(issueLocation, message);
        preciseIssue.secondary(secondaryLocation, "Overridden method's definition");
      } else {
        ctx.addIssue(issueLocation, message + " This method overrides " + overriddenMethod.fullyQualifiedName() + ".");
      }
    });
  }

  private static void reportOnExtraParameters(SubscriptionContext ctx, FunctionSymbol method, FunctionSymbol overriddenMethod) {
    long paramsWithoutDefaultValue = method.parameters().stream().filter(parameter -> !parameter.hasDefaultValue()).count();
    if (paramsWithoutDefaultValue == overriddenMethod.parameters().size()) {
      return;
    }
    method.parameters().stream()
      .filter(parameter -> !parameter.hasDefaultValue() && parameter.name() != null)
      .filter(parameter -> overriddenMethod.parameters().stream().noneMatch(p -> Objects.equals(parameter.name(), p.name())))
      .forEach(parameter -> reportIssue(ctx,"Remove parameter " + parameter.name() + " or provide default value.", parameter.location(), overriddenMethod));
  }


  private static void checkDefaultValuesAndParamNames(SubscriptionContext ctx, FunctionSymbol method, FunctionSymbol overriddenMethod) {
    Map<String, Integer> mismatchedOverriddenParamPosition = new HashMap<>();
    Map<String, Integer> mismatchedParamPosition = new HashMap<>();

    List<FunctionSymbol.Parameter> parameters = method.parameters();

    for (int i = 0; i < overriddenMethod.parameters().size(); i++) {
      FunctionSymbol.Parameter overriddenParam = overriddenMethod.parameters().get(i);
      FunctionSymbol.Parameter parameter = method.parameters().get(i);
      if (!Objects.equals(overriddenParam.name(), parameter.name())) {
        mismatchedOverriddenParamPosition.put(overriddenParam.name(), i);
        mismatchedParamPosition.put(parameter.name(), i);
      } else {
        checkDefaultValueAndKeywordOnly(ctx, overriddenMethod, overriddenParam, parameter);
      }
    }

    mismatchedParamPosition.forEach((name, index) -> {
      Integer overriddenParamIndex = mismatchedOverriddenParamPosition.get(name);
      FunctionSymbol.Parameter parameter = parameters.get(index);
      if (overriddenParamIndex != null && !parameter.isKeywordOnly()) {
        reportIssue(ctx, "Move parameter " + name + " to position " + overriddenParamIndex + ".", parameter.location(), overriddenMethod);
      }
    });

  }

  private static void checkDefaultValueAndKeywordOnly(SubscriptionContext ctx, FunctionSymbol overriddenMethod, FunctionSymbol.Parameter overriddenParam,
                                                      FunctionSymbol.Parameter parameter) {
    String prefix = "Make parameter " + parameter.name();
    if (overriddenParam.hasDefaultValue() && !parameter.hasDefaultValue()) {
      reportIssue(ctx, "Add a default value to parameter " + parameter.name() + ".", parameter.location(), overriddenMethod);
    }
    if ((!overriddenParam.isKeywordOnly() && !overriddenParam.isPositionalOnly()) && (parameter.isKeywordOnly() || parameter.isPositionalOnly())) {
      reportIssue(ctx, prefix + " keyword-or-positional.", parameter.location(), overriddenMethod);
    }
    if (overriddenParam.isPositionalOnly() && !parameter.isPositionalOnly()) {
      reportIssue(ctx, prefix + " positional only.", parameter.location(), overriddenMethod);
    }
    if (overriddenParam.isKeywordOnly() && !parameter.isKeywordOnly()) {
      reportIssue(ctx, prefix + " keyword only.", parameter.location(), overriddenMethod);
    }
  }
}
