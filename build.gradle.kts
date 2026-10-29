import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

/**
 * Compile and test against an IDE distribution.
 *
 * Resolution order:
 *   1. `-PideaPath=/path/to/IntelliJ IDEA.app` (or set `ideaPath` in gradle.properties)
 *   2. the IDE path recorded by the IDE itself, so cloning from the IDE "just works"
 *   3. a downloaded IntelliJ IDEA Community build (requires network, several hundred MB)
 *
 * Using a local installation avoids the SDK download and guarantees the bytecode
 * matches the platform actually running the plugin.
 */
val localIdeaPath: String? = providers.gradleProperty("ideaPath").orNull
    ?: System.getProperty("idea.home.path")
    ?: System.getenv("IDEA_PATH")

dependencies {
    intellijPlatform {
        if (localIdeaPath != null) {
            local(localIdeaPath)
        } else {
            create("IC", "2026.1")
        }
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

intellijPlatform {
    buildSearchableOptions.set(false)

    pluginConfiguration {
        id = "org.ssssssss.magicapi.idea"
        name = providers.gradleProperty("pluginName")
        version = providers.gradleProperty("pluginVersion")

        ideaVersion {
            sinceBuild = "261"
            untilBuild = provider { null }
        }
    }
}

tasks {
    test {
        if (localIdeaPath != null) {
            systemProperty("idea.home.path", localIdeaPath)
        }
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }

    buildSearchableOptions {
        enabled = false
    }
}
