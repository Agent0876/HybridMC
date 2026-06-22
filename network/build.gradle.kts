plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    api(project(":core"))
    // Unifies both edition frontends behind a single PlayerConnection abstraction.
    implementation(project(":protocol-java"))
    implementation(project(":protocol-bedrock"))
}
