dependencies {
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator")
    testImplementation("org.sonarsource.sonarqube:sonar-ws")
    testImplementation("junit:junit")
    testImplementation("org.sonarsource.sonarlint.core:sonarlint-core")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.google.guave:guava")
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("plugin") || project.hasProperty("its")
    }
    filter {
        includeTestsMatching("org.sonar.python.it.plugin.Tests")
        includeTestsMatching("org.sonar.python.it.plugin.SonarLintTest")
    }
    systemProperty("java.awt.headless", "true") // needed?
    outputs.upToDateWhen { false }
}
