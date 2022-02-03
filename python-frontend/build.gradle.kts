plugins {
    id "com.google.protobuf" version "0.8.18"
    id "java"
}

val kotlinVersion: String by extra
val junitVersion: String by project
val mockkVersion: String by project
val sonarVersion: String by project
val projectVersion: String by project
val projectGroupId: String by project
val protobufVersion: String by project

dependencies {
    implementation("org.sonarsource.sslr:sslr-core")
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api")
    testImplementation("org.sonarsource.sslr:sslr-testing-harness")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    implementation("com.google.guava:guava")
    implementation("com.google.protobuf:protobuf-java:$protobufVersion")
    implementation("org.sonarsource.analyzer-commons:sonar-xml-parsing")
}

tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}

val test: Test by tasks
test.dependsOn(project(":kotlin-checks-test-sources").tasks.named("build"))

tasks.jar {
    manifest {
        val displayVersion = if (project.property("buildNumber") == null) project.version else project.version.toString()
            .substring(0, project.version.toString().lastIndexOf(".")) + " (build ${project.property("buildNumber")})"
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(
            mapOf(
                "Build-Time" to buildDate,
                "Implementation-Build" to ProcessBuilder().also { it.command("git", "rev-parse", "HEAD") }
                    .start().inputStream.bufferedReader().use {
                        it.readText().trim()
                    },
                "Plugin-BuildDate" to buildDate,
                // Note that use of ChildFirstClassLoader is deprecated since SonarQube 7.9
                "Plugin-ChildFirstClassLoader" to "false",
                "Plugin-Class" to "org.sonar.plugins.python.PythonPlugin",
                "Plugin-Description" to "Code Analyzer for Python",
                "Plugin-Developers" to "SonarSource Team",
                "Plugin-Display-Version" to displayVersion,
                "Plugin-Homepage" to "http to//redirect.sonarsource.com/plugins/python.html",
                "Plugin-IssueTrackerUrl" to "https to//jira.sonarsource.com/browse/SONARPY",
                "Plugin-Key" to "python",
                "Plugin-License" to "GNU LGPL 3",
                "Plugin-Name" to "Python Code Quality and Security",
                "Plugin-Organization" to "SonarSource",
                "Plugin-OrganizationUrl" to "http to//www.sonarsource.com",
                "Plugin-SourcesUrl" to "https to//github.com/SonarSource/sonar-python",
                "Plugin-Version" to project.version,
                "Sonar-Version" to "6.7",
                "SonarLint-Supported" to "true",
                "Version" to project.version.toString(),
                "Skip-Dependencies-Packaging" to "true",
                "SonarQube-Min-Version" to sonarQubeMinVersion
            )
        )
    }
}

val shadowJar = tasks.shadowJar
val sourcesJar = tasks.sourcesJar
val javadocJar = tasks.javadocJar

tasks.shadowJar {
    minimize {}
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("META-INF/*.RSA")
    exclude("META-INF/*.SF")
    exclude("LICENSE*")
    exclude("NOTICE*")
    // Keeping all classes from python-frontend in case some class is not used (e.g. class is created for custom rules)
    exclude("org/sonarsource/python/python-frontend/**")
    doLast {
        enforceJarSizeAndCheckContent(shadowJar.get().archiveFile.get().asFile, 8_300_000L, 9_300_000L)
    }
}

artifacts {
    archives(shadowJar)
}

tasks.artifactoryPublish { skip = false }
publishing {
    publications.withType<MavenPublication> {
        artifact(shadowJar) {
            classifier = null
        }
        artifact(sourcesJar)
        artifact(javadocJar)
    }
}

fun enforceJarSizeAndCheckContent(file: File, minSize: Long, maxSize: Long) {
    val size = file.length()
    if (size < minSize) {
        throw GradleException("${file.path} size ($size) too small. Min is $minSize")
    } else if (size > maxSize) {
        throw GradleException("${file.path} size ($size) too large. Max is $maxSize")
    }
    checkJarEntriesPathUniqueness(file)
}

// A jar should not contain 2 entries with the same path, furthermore Pack200 will fail to unpack it
fun checkJarEntriesPathUniqueness(file: File) {
    val allNames = mutableSetOf<String>()
    val duplicatedNames = mutableSetOf<String>()
    file.inputStream().use { input ->
        JarInputStream(input).use { jarInput ->
            generateSequence { jarInput.nextJarEntry }.forEach { jarEntry ->
                if (!allNames.add(jarEntry.name)) {
                    duplicatedNames.add(jarEntry.name)
                }
            }
        }
    }
    if (duplicatedNames.isNotEmpty()) {
        throw GradleException("Duplicated entries in the jar: '${file.path}': ${duplicatedNames.joinToString(", ")}")
    }
}
