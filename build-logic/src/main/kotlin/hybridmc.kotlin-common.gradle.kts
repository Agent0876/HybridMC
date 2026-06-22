import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.tasks.testing.Test

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("com.diffplug.spotless")
    id("org.jetbrains.kotlinx.kover")
}

val libs = the<LibrariesForLibs>()

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
