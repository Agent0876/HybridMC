import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("hybridmc.kotlin-library")
    `maven-publish`
}

// Modules carrying a third-party-facing contract (the plugin API) are published artifacts.
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
