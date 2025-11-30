import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import java.io.FileInputStream
import java.util.*

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
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.intelliJPlatform) // IntelliJ Platform Gradle Plugin
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    alias(libs.plugins.qodana) // Gradle Qodana Plugin
    alias(libs.plugins.kover) // Gradle Kover Plugin
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

// Set the JVM language level used to build the project.
kotlin {
    jvmToolchain(21)
}

// Configure project's dependencies
repositories {
    mavenCentral()

    // IntelliJ Platform Gradle Plugin Repositories Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
    intellijPlatform {
        defaultRepositories()
    }
}

// Dependencies are managed with Gradle version catalog - read more: https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.opentest4j)

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        // When running test tasks, if the IDE version specified in local.properties differs from the platformVersion specified in gradle.properties,
        // this prevents the JVM from crashing.
        val isTestTask = gradle.startParameter.taskNames.any {
            it.contains("test", ignoreCase = true)
        }

        if (prop != null && !isTestTask) {
            prop.getProperty("ideDir")?.let { ideDirValue ->
                    if (ideDirValue.isNotEmpty()) {
                        local(file(ideDirValue))
                    }
                }
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
            rubymine(providers.gradleProperty("platformVersion"))
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
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
            val start = "<!-- Plugin description -->"
            val end = "<!-- Plugin description end -->"

            with(it.lines()) {
                if (!containsAll(listOf(start, end))) {
                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
                }
                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
            }
        }

        val changelog = project.changelog // local variable for configuration cache compatibility
        // Get the latest available change notes from the changelog file
        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
            with(changelog) {
                renderItem(
                    (getOrNull(pluginVersion) ?: getUnreleased())
                        .withHeader(false)
                        .withEmptySections(false),
                    Changelog.OutputType.HTML,
                )
            }
        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")

            // Note: For Railroads, since no conditions are specified, pluginUntilBuild remains undefined.
            // If pluginUntilBuild is undefined, explicitly returning null allows the recommended() method to resolve the IDE
            // If no value exists, the recommended() method cannot resolve the IDE
            untilBuild = providers.gradleProperty("pluginUntilBuild").takeIf {
                !it.orNull.isNullOrBlank()
            } ?: provider { null }
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion").map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
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

// Configure Gradle Kover Plugin - read more: https://github.com/Kotlin/kotlinx-kover#configuration
kover {
    reports {
        total {
            xml {
                onCheck = true
            }
        }
    }
}

tasks {
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    test {
        useJUnitPlatform()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}

intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}
