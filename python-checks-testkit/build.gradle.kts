import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream


val kotlinVersion: String by extra
val junitVersion: String by project
val mockkVersion: String by project
val sonarVersion: String by project
val projectVersion: String by project
val projectGroupId: String by project

dependencies {
    implementation("$projectGroupId:python-frontend:$projectVersion")
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api")
    implementation("org.sonarsource.analyzer-commons:sonar-analyzer-test-commons")
    implementation("com.google.guava:guava")
    compile("junit:junit")
    compile("org.assertj:assertj-core")
}

tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}
