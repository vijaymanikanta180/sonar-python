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
package org.sonar.plugins.python.bandit;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONArray;
import org.sonarsource.analyzer.commons.internal.json.simple.JSONObject;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.JSONParser;
import org.sonarsource.analyzer.commons.internal.json.simple.parser.ParseException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class BanditJsonReportReader {

  private final JSONParser jsonParser = new JSONParser();
  private final Consumer<Issue> consumer;

  public static class Issue {
    @Nullable
    String filePath;
    @Nullable
    String ruleKey;
    @Nullable
    String message;
    @Nullable
    Integer lineNumber;
    @Nullable
    String severity;
    @Nullable
    String confidence;
  }

  private BanditJsonReportReader(Consumer<Issue> consumer) {
    this.consumer = consumer;
  }

  static void read(InputStream in, Consumer<Issue> consumer) throws IOException, ParseException {
    new BanditJsonReportReader(consumer).read(in);
  }

  private void read(InputStream in) throws IOException, ParseException {
    JSONObject rootObject = (JSONObject) jsonParser.parse(new InputStreamReader(in, UTF_8));
    JSONArray files = (JSONArray) rootObject.get("results");
    if (files != null) {
      ((Stream<JSONObject>) files.stream()).forEach(this::onResult);
    }
  }

  private void onResult(JSONObject result) {
    Issue issue = new Issue();
    issue.ruleKey = (String) result.get("test_id");
    issue.filePath = (String) result.get("filename");
    issue.message = (String) result.get("issue_text");
    issue.lineNumber = toInteger(result.get("line_number"));
    issue.severity = (String) result.get("issue_severity");
    issue.confidence = (String) result.get("issue_confidence");
    consumer.accept(issue);
  }

  private static Integer toInteger(Object value) {
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return null;
  }

}
