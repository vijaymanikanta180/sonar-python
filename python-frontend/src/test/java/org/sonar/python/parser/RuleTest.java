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
package org.sonar.python.parser;

import com.sonar.sslr.api.AstNode;
import com.sonar.sslr.api.GenericTokenType;
import com.sonar.sslr.api.TokenType;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.sonar.plugins.python.api.tree.BaseTreeVisitor;
import org.sonar.plugins.python.api.tree.Token;
import org.sonar.plugins.python.api.tree.Tree;
import org.sonar.python.api.PythonTokenType;
import org.sonar.python.tree.TreeUtils;
import org.sonar.sslr.grammar.GrammarRuleKey;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class RuleTest {

  protected PythonParser p = PythonParser.create();

  protected void setRootRule(GrammarRuleKey ruleKey) {
    p.setRootRule(p.getGrammar().rule(ruleKey));
  }

  protected  <T extends Tree> T parse(String code, Function<AstNode, T> func) {
    T tree = func.apply(p.parse(code));
    // ensure every visit method of base tree visitor is called without errors
    BaseTreeVisitor visitor = new BaseTreeVisitor();
    tree.accept(visitor);
    List<TokenType> ptt = Arrays.asList(PythonTokenType.NEWLINE, PythonTokenType.DEDENT, PythonTokenType.INDENT, GenericTokenType.EOF);
    List<Token> tokenList = TreeUtils.tokens(tree);

    String tokens = tokenList.stream().filter(t -> !ptt.contains(t.type())).map(token -> {
      if(token.type() == PythonTokenType.STRING) {
        return token.value().replaceAll("\n", "").replaceAll(" ", "");
      }
      return token.value();
    }).collect(Collectors.joining(""));
    String originalCode = code.replaceAll("#.*\\n", "").replaceAll("\\n", "").replaceAll(" ", "");
    assertThat(tokens).isEqualTo(originalCode);
    return tree;
  }
}
