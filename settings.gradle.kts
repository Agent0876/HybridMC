pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    // Lets the JDK 21 toolchain be auto-provisioned if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(org.gradle.api.initialization.resolve.RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "HybridMC"

include(
    "core",
    "registry",
    "protocol-java",
    "protocol-bedrock",
    "network",
    "world",
    "worldgen",
    "entity",
    "game",
    "api",
    "server",
    "plugin-host",
    "app",
    "architecture-tests",
)
