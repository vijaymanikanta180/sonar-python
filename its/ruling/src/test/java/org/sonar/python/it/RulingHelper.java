/*
 * SonarQube Python Plugin
 * Copyright (C) 2012-2022 SonarSource SA
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
package org.sonar.python.it;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.locator.FileLocation;
import com.sonar.orchestrator.locator.MavenLocation;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RulingHelper {

  private static final String SQ_VERSION_PROPERTY = "sonar.runtimeVersion";
  private static final String DEFAULT_SQ_VERSION = "LATEST_RELEASE";

  static Orchestrator getOrchestrator() {
    return Orchestrator.builderEnv()
      .setSonarVersion(System.getProperty(SQ_VERSION_PROPERTY, DEFAULT_SQ_VERSION))
      .addPlugin(FileLocation.byWildcardMavenFilename(new File("../../sonar-python-plugin/target"), "sonar-python-plugin-*.jar"))
      .addPlugin(MavenLocation.of("org.sonarsource.sonar-lits-plugin", "sonar-lits-plugin", "0.10.0.2181"))
      .build();
  }

  // TODO: SONARPY-984, read rules metadata instead of hardcoding this list
  static List<String> bugRuleKeys() {
    return Arrays.asList(
      "PreIncrementDecrement",
      "S1045",
      "S1143",
      "S1226",
      "S1656",
      "S1716",
      "S1751",
      "S1763",
      "S1764",
      "S1862",
      "S2159",
      "S2190",
      "S2201",
      "S2275",
      "S2711",
      "S2712",
      "S2734",
      "S2757",
      "S2823",
      "S3403",
      "S3827",
      "S3862",
      "S3923",
      "S3981",
      "S3984",
      "S4143",
      "S5549",
      "S5607",
      "S5632",
      "S5644",
      "S5707",
      "S5708",
      "S5714",
      "S5719",
      "S5722",
      "S5724",
      "S5756",
      "S5796",
      "S5807",
      "S5828",
      "S5842",
      "S5850",
      "S5855",
      "S5856",
      "S5868",
      "S5996",
      "S6002",
      "S905",
      "S930"
    );
  }
}
