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
package org.sonar.python.checks.hotspots;

import java.util.Arrays;
import org.junit.Test;
import org.sonar.python.checks.utils.PythonCheckVerifier;

public class UnsafeHttpMethodsCheckTest {

  @Test
  public void test_django() {
    PythonCheckVerifier.verify(Arrays.asList(
      "src/test/resources/checks/hotspots/unsafeHttpMethods/django/urls.py",
      "src/test/resources/checks/hotspots/unsafeHttpMethods/django/views.py",
      "src/test/resources/checks/hotspots/unsafeHttpMethods/django/views_decorator_fqn_null.py"
      ),
      new UnsafeHttpMethodsCheck());
  }

  @Test
  public void test_flask() {
    PythonCheckVerifier.verify(Arrays.asList(
      "src/test/resources/checks/hotspots/unsafeHttpMethods/flask/views.py",
      "src/test/resources/checks/hotspots/unsafeHttpMethods/flask/otherDecorator.py"
    ), new UnsafeHttpMethodsCheck());
  }
}
