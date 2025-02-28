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
package org.sonar.python.regex;


import java.util.List;
import org.junit.Test;
import org.sonar.plugins.python.api.LocationInFile;
import org.sonarsource.analyzer.commons.regex.RegexParseResult;
import org.sonarsource.analyzer.commons.regex.RegexParser;
import org.sonarsource.analyzer.commons.regex.RegexSource;
import org.sonarsource.analyzer.commons.regex.ast.CharacterTree;
import org.sonarsource.analyzer.commons.regex.ast.FlagSet;
import org.sonarsource.analyzer.commons.regex.ast.RegexTree;
import org.sonarsource.analyzer.commons.regex.ast.SequenceTree;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.python.regex.RegexParserTestUtils.assertKind;
import static org.sonar.python.regex.RegexParserTestUtils.assertSuccessfulParse;
import static org.sonar.python.regex.RegexParserTestUtils.makeSource;

public class PythonAnalyzerRegexSourceTest {

  @Test
  // TODO: Extend test with exact syntax error location check
  public void invalid_regex() {
    RegexSource source = makeSource("r'+'");
    RegexParseResult result = new RegexParser(source, new FlagSet()).parse();

    assertThat(result.getSyntaxErrors()).isNotEmpty();
  }

  @Test
  public void test_string_literal() {
    RegexTree regex = assertSuccessfulParse("r'a\\nb'");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(3);

    assertCharacter('a', items.get(0));
    assertCharacter('\n', items.get(1));
    assertCharacter('b', items.get(2));

    assertLocation(2, 11, 12, items.get(0));
    assertLocation(2, 12, 14, items.get(1));
    assertLocation(2, 14, 15, items.get(2));
  }

  @Test
  public void test_triple_quote_literal() {
    RegexTree regex = assertSuccessfulParse("r'''a\\nb'''");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    List<RegexTree> items = ((SequenceTree) regex).getItems();
    assertThat(items).hasSize(3);

    assertCharacter('a', items.get(0));
    assertCharacter('\n', items.get(1));
    assertCharacter('b', items.get(2));

    assertLocation(2, 13, 14, items.get(0));
    assertLocation(2, 14, 16, items.get(1));
    assertLocation(2, 16, 17, items.get(2));
  }

  @Test
  public void multiline_string_literal() {
    RegexTree regex = assertSuccessfulParse("r'a\nbc\r\nde'");
    assertKind(RegexTree.Kind.SEQUENCE, regex);
    List<RegexTree> items = ((SequenceTree) regex).getItems();

    assertCharacterLocation(items.get(0), 'a', 2, 11, 12);
    assertCharacterLocation(items.get(2), 'b', 3, 0, 1);
    assertCharacterLocation(items.get(3), 'c', 3, 1, 2);
    assertCharacterLocation(items.get(6), 'd', 4, 0, 1);
  }

  private static void assertCharacterLocation(RegexTree tree, char expected, int line, int startLineOffset, int endLineOffset) {
    assertKind(RegexTree.Kind.CHARACTER, tree);
    assertThat((char) ((CharacterTree) tree).codePointOrUnit()).isEqualTo(expected);
    assertLocation(line, startLineOffset, endLineOffset, tree);
  }

  private static void assertCharacter(char expected, RegexTree tree) {
    assertKind(RegexTree.Kind.CHARACTER, tree);
    assertThat(((CharacterTree) tree).codePointOrUnit()).isEqualTo(expected);
  }

  private static void assertLocation(int line, int startLineOffset, int endLineOffset, RegexTree tree) {
    LocationInFile location = ((PythonAnalyzerRegexSource) tree.getSource()).locationInFileFor(tree.getRange());
    assertLocation(line, startLineOffset, endLineOffset, location);
  }

  private static void assertLocation(int line, int startLineOffset, int endLineOffset, LocationInFile location) {
    assertThat(location.startLine()).withFailMessage(String.format("Expected start line to be '%d' but got '%d'", line, location.startLine())).isEqualTo(line);
    assertThat(location.endLine()).withFailMessage(String.format("Expected end line to be '%d' but got '%d'", line, location.endLine())).isEqualTo(line);
    assertThat(location.startLineOffset()).withFailMessage(String.format("Expected start character to be '%d' but got '%d'", startLineOffset, location.startLineOffset())).isEqualTo(startLineOffset);
    assertThat(location.endLineOffset()).withFailMessage(String.format("Expected end character to be '%d' but got '%d'", endLineOffset, location.endLineOffset())).isEqualTo(endLineOffset);
  }

}
