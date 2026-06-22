plugins {
    // Lets the JDK 21 toolchain be auto-provisioned if it isn't installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
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
)
