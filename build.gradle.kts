plugins {
    alias(libs.plugins.kover)
    alias(libs.plugins.binary.compatibility.validator)
}

allprojects {
    group = "io.hybridmc"
    version = "0.1.0-SNAPSHOT"
}

// Aggregate coverage across every source module into a single root report.
dependencies {
    listOf(
        "core", "registry", "protocol-java", "protocol-bedrock", "network",
        "world", "worldgen", "entity", "game", "api", "server", "plugin-host", "app",
    ).forEach { kover(project(":$it")) }
}

// The plugin API (:api) is the only third-party-facing contract, so it is the only
// module whose binary compatibility we track. Everything else is internal.
apiValidation {
    ignoredProjects.addAll(subprojects.map { it.name }.filter { it != "api" })
}
