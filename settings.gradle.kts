import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
        id("org.jetbrains.changelog") version "2.5.0"
        id("org.jetbrains.intellij.platform") version "2.16.0"
        id("org.jetbrains.intellij.platform.settings") version "2.16.0"
        id("org.jetbrains.kotlin.jvm") version "2.3.21"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
    id("org.jetbrains.intellij.platform.settings")
}

rootProject.name = "railroads"

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()

        intellijPlatform {
            defaultRepositories()
        }
    }
}
