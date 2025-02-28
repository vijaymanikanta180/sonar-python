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
package com.sonar.python.it.plugin;

import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarScanner;
import java.io.File;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonarqube.ws.Issues;

import static com.sonar.python.it.plugin.Tests.issues;
import static org.assertj.core.api.Assertions.assertThat;

public class NoSonarTest {

  private static final String PROJECT_KEY = "nosonar";
  private static final String PROFILE_NAME = "nosonar";

  @ClassRule
  public static final Orchestrator ORCHESTRATOR = Tests.ORCHESTRATOR;

  @BeforeClass
  public static void startServer() {
    ORCHESTRATOR.getServer().provisionProject(PROJECT_KEY, PROJECT_KEY);
    ORCHESTRATOR.getServer().associateProjectToQualityProfile(PROJECT_KEY, "py", PROFILE_NAME);
    SonarScanner build = SonarScanner.create()
      .setProjectDir(new File("projects", PROJECT_KEY))
      .setProjectKey(PROJECT_KEY)
      .setProjectName(PROJECT_KEY)
      .setProjectVersion("1.0-SNAPSHOT")
      .setSourceDirs(".");
    ORCHESTRATOR.executeBuild(build);
  }

  @Test
  public void test_nosonar() {
    List<Issues.Issue> issues = issues(PROJECT_KEY);
    assertThat(issues).hasSize(2);
    assertThat(issues.get(0).getRule()).isEqualTo("python:PrintStatementUsage");
    assertThat(issues.get(0).getLine()).isEqualTo(1);
    assertThat(issues.get(1).getRule()).isEqualTo("python:NoSonar");
    assertThat(issues.get(1).getLine()).isEqualTo(2);
  }
}
