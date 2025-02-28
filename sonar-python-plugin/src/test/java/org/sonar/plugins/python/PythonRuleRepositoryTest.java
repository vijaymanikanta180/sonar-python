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
package org.sonar.plugins.python;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;
import org.sonar.api.SonarEdition;
import org.sonar.api.SonarQubeSide;
import org.sonar.api.SonarRuntime;
import org.sonar.api.internal.SonarRuntimeImpl;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.Version;
import org.sonar.python.checks.CheckList;

import static org.assertj.core.api.Assertions.assertThat;

public class PythonRuleRepositoryTest {

  @Test
  public void createRulesTest() throws IOException {
    RulesDefinition.Repository repository = buildRepository();

    assertThat(repository).isNotNull();
    assertThat(repository.language()).isEqualTo("py");
    assertThat(repository.name()).isEqualTo("SonarQube");

    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules).isNotNull();
    assertThat(rules).hasSameSizeAs(nonAbstractCheckFiles());

    RulesDefinition.Rule s1578 = repository.rule("S1578");
    assertThat(s1578).isNotNull();
    assertThat(s1578.activatedByDefault()).isFalse();
    RulesDefinition.Rule backstickUsage = repository.rule("BackticksUsage");
    assertThat(backstickUsage).isNotNull();
    assertThat(backstickUsage.activatedByDefault()).isTrue();

    for (RulesDefinition.Rule rule : rules) {
      assertThat(rule.htmlDescription()).isNotEmpty();
      rule.params().forEach(p -> assertThat(p.description()).isNotEmpty());
    }
  }

  @Test
  public void owaspSecurityStandard() {
    RulesDefinition.Repository repository_9_3 = buildRepository(9, 3);
    RulesDefinition.Rule s4721_9_3 = repository_9_3.rule("S4721");
    assertThat(s4721_9_3).isNotNull();
    assertThat(s4721_9_3.securityStandards()).contains("owaspTop10-2021:a3");

    RulesDefinition.Repository repository_9_2 = buildRepository(9, 2);
    RulesDefinition.Rule s4721_9_2 = repository_9_2.rule("S4721");
    assertThat(s4721_9_2).isNotNull();
    assertThat(s4721_9_2.securityStandards()).doesNotContain("owaspTop10-2021:a3");
  }

  private List<String> nonAbstractCheckFiles() throws IOException {
    return Files.walk(new File("../python-checks/src/main/java/org/sonar/python/checks").toPath())
      .filter(Files::isRegularFile)
      .map(p -> p.getFileName().toString())
      .filter(f -> f.endsWith("Check.java"))
      .filter(f -> !f.startsWith("Abstract"))
      .collect(Collectors.toList());
  }

  @Test
  public void ruleTemplates() {
    RulesDefinition.Repository repository = buildRepository();
    assertThat(repository).isNotNull();

    RulesDefinition.Rule rule;

    rule = repository.rule("S100");
    assertThat(rule).isNotNull();
    assertThat(rule.template()).isFalse();

    rule = repository.rule("CommentRegularExpression");
    assertThat(rule).isNotNull();
    assertThat(rule.template()).isTrue();

    long templateCount = repository.rules().stream()
      .map(RulesDefinition.Rule::template)
      .filter(Boolean::booleanValue)
      .count();
    assertThat(repository.rules().size()).isGreaterThan(50);
    assertThat(templateCount).isEqualTo(1);
  }

  @Test
  public void hotspotRules() {
    RulesDefinition.Repository repository = buildRepository();
    RulesDefinition.Rule hardcodedIp = repository.rule("S1313");
    assertThat(hardcodedIp.type()).isEqualTo(RuleType.SECURITY_HOTSPOT);
  }

  private static RulesDefinition.Repository buildRepository() {
    return buildRepository(9, 3);
  }

  private static RulesDefinition.Repository buildRepository(int majorVersion, int minorVersion) {
    SonarRuntime sonarRuntime = SonarRuntimeImpl.forSonarQube(Version.create(majorVersion, minorVersion), SonarQubeSide.SERVER, SonarEdition.DEVELOPER);
    PythonRuleRepository ruleRepository = new PythonRuleRepository(sonarRuntime);
    RulesDefinition.Context context = new RulesDefinition.Context();
    ruleRepository.define(context);
    return context.repository(CheckList.REPOSITORY_KEY);
  }

}
