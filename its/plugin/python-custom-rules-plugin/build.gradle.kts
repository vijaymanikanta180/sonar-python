dependencies {
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api")
    testImplementation("org.sonarsource.sonarqube:sonar-plugin-api-impl")
    compileOnly("org.sonarsource.python:sonar-python-plugin:$projectVersion")
    testImplementation("org.sonarsource.python:python-checks-testkit:$projectVersion")
    testImplementation("junit:junit")
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("plugin") || project.hasProperty("its")
    }
    systemProperty("java.awt.headless", "true") // needed?
    outputs.upToDateWhen { false }
}

tasks.jar {
    manifest {
        attributes(
                mapOf(
                        "Plugin-Class" to "org.sonar.samples.python.CustomPythonRulesPlugin",
                        "Require-Plugins" to "python:$projectVersion"
                        "SonarQube-Min-Version" to sonarQubeMinVersion
                )
        )
    }
}

tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}