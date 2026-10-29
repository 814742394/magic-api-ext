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

dependencies {
    intellijPlatform {
        // Compile and test against the locally installed IDE, so no multi-gigabyte
        // SDK download is needed and the bytecode always matches the running platform.
        local("/Applications/IntelliJ IDEA.app")
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
        systemProperty("idea.home.path", "/Applications/IntelliJ IDEA.app/Contents")
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = false
        }
    }

    buildSearchableOptions {
        enabled = false
    }
}
