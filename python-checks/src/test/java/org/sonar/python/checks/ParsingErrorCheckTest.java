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

import com.sonar.sslr.api.RecognitionException;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;
import org.sonar.plugins.python.api.PythonCheck.PreciseIssue;
import org.sonar.plugins.python.api.PythonVisitorContext;
import org.sonar.python.parser.PythonParser;

import static org.assertj.core.api.Assertions.assertThat;

public class ParsingErrorCheckTest {

  @Test
  public void test() throws Exception {
    File file = new File("src/test/resources/checks/parsingError.py");

    PythonParser parser = PythonParser.create();
    PythonVisitorContext context;
    try {
      String fileContent = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
      parser.parse(fileContent);
      throw new IllegalStateException("Expected RecognitionException");
    } catch (RecognitionException e) {
      context = new PythonVisitorContext(null, e);
    }

    ParsingErrorCheck check = new ParsingErrorCheck();
    check.scanFile(context);
    List<PreciseIssue> issues = context.getIssues();
    assertThat(issues).hasSize(1);
    assertThat(issues.get(0).primaryLocation().startLine()).isEqualTo(1);
  }

}
