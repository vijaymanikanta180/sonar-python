dependencies {
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator")
    testImplementation("junit:junit")
    testImplementation("org.assertj:assertj-core")
    testImplementation("com.google.guave:guava")
    compile("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("ruling") || project.hasProperty("its")
    }
    systemProperty("java.awt.headless", "true") // needed?
    outputs.upToDateWhen { false }
}
