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
package com.sonar.plugins.security.api;

import java.util.HashSet;
import java.util.Set;

public class PythonRules {

  private static final String REPO_KEY = "pythonsecurity";

  private static final Set<String> RULE_KEYS = new HashSet<>();

  public static boolean throwOnCall = false;

  private PythonRules() {
  }

  public static Set<String> getRuleKeys() {
    if (throwOnCall) {
      throw new RuntimeException("Boom!");
    }
    return RULE_KEYS;
  }

  public static String getRepositoryKey() {
    return REPO_KEY;
  }

}
