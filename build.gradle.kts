import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import java.io.FileInputStream
import java.util.Properties

// load local.properties
val localPropertiesFileExists = File(rootProject.rootDir, "local.properties").exists()
val prop = if (localPropertiesFileExists) Properties().apply {
    load(FileInputStream(File(rootProject.rootDir, "local.properties")))
} else null

fun IntelliJPlatformDependenciesExtension.addPlugin(vararg pluginIds: String) {
    // For RubyMine, since the Ruby plugin is bundled with the IDE, use bundledPlugins().
    bundledPlugins(provider {
        pluginIds.filter {
            intellijPlatform.productInfo.productCode == IntelliJPlatformType.RubyMine.code
        }
    })

    // For IntelliJ IDEA, since the Ruby plugin is not bundled with the IDE,
    // use compatiblePlugin() to add a Ruby plugin with compatibility considerations.
    compatiblePlugins(provider {
        pluginIds.filter {
            intellijPlatform.productInfo.productCode == IntelliJPlatformType.IntellijIdeaUltimate.code
        }
    })
}

plugins {
    id("java") // Java support
    id("org.jetbrains.kotlin.jvm") // Kotlin support
    id("org.jetbrains.intellij.platform") // IntelliJ Platform Gradle Plugin
    id("org.jetbrains.changelog") // Gradle Changelog Plugin
}

group = providers.gradleProperty("group").get()
version = providers.gradleProperty("version").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.opentest4j:opentest4j:1.3.0")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        val shouldUseLocalIde = prop != null && gradle.startParameter.taskNames.any {
            it.contains("runIde", ignoreCase = true)
        }
        val localIdePath = prop?.getProperty("ideDir")?.takeIf { it.isNotBlank() }

        if (shouldUseLocalIde && localIdePath != null) {
            local(file(localIdePath))
        } else {
            // Originally, Target Platform was set to IDEA Ultimate.
            // However, starting from IDE 2025.2, verifyPlugin would fail
            // unless org.jetbrains.plugins.ruby was specified in the plugin.xml's depends section.
            // While org.jetbrains.plugins.ruby is the name of the Ruby plugin,
            // the official documentation doesn't explicitly state that it should be included in the depends list.
            // https://plugins.jetbrains.com/docs/intellij/rubymine.html
            //
            // Some open-source plugins using the Ruby plugin include this specification,
            // but there was no clear evidence indicating whether this was truly appropriate.
            // Therefore, we reverted to using com.intellij.modules.ruby for depends while changing the Target Platform to RubyMine instead.
            //
            // After verifying functionality using IDEA Ultimate with the Ruby plugin,
            // no issues were found, so we will proceed with this configuration for the time being.
            rubymine(providers.gradleProperty("railroadsBuildTargetRubyMineVersion"))
        }

        addPlugin("org.jetbrains.plugins.ruby")

        testFramework(TestFrameworkType.Platform)
    }

    // railroads plugin dependencies
    val junitVersion = "5.10.3"
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-params:${junitVersion}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine:${junitVersion}")
}

// Configure IntelliJ Platform Gradle Plugin - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        version = providers.gradleProperty("version")

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("version").map { releaseVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(releaseVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("railroadsMinimumIdeBuild")

            // Note: For Railroads, since no conditions are specified, pluginUntilBuild remains undefined.
            untilBuild = provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The version is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("version").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}

// Configure Gradle Changelog Plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.empty()
    repositoryUrl = providers.gradleProperty("pluginRepositoryUrl")
}

tasks {
    publishPlugin {
        dependsOn(patchChangelog)
    }
}
