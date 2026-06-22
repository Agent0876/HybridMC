import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
    id("hybridmc.kotlin-common")
    `java-library`
}

// Force a deliberate, reviewed public API surface in every library module.
extensions.configure<KotlinJvmProjectExtension> {
    explicitApi()
}
