pluginManagement {
    repositories {
        maven(url = "https://repox.jfrog.io/repox/plugins.gradle.org/")
        gradlePluginPortal()
    }

    val kotlinVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

rootProject.name = "python"

include("its:plugin")
include("its:ruling")
include("python-checks")
include("python-checks-testkit")
include("python-frontend")
include("sonar-python-plugin")
