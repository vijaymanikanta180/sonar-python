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

import org.junit.Test;
import org.sonar.python.checks.utils.PythonCheckVerifier;

public class StringFormatMisuseCheckTest {

  @Test
  public void test_printf_style() {
    PythonCheckVerifier.verify("src/test/resources/checks/stringFormatMisusePrintf.py", new StringFormatMisuseCheck());
  }

  @Test
  public void test_str_format_style() {
    PythonCheckVerifier.verify("src/test/resources/checks/stringFormatMisuseStrFormat.py", new StringFormatMisuseCheck());
  }
}
