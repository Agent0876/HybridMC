plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    // The server wires every domain module together behind the tick loop.
    implementation(project(":core"))
    implementation(project(":registry"))
    implementation(project(":network"))
    implementation(project(":world"))
    implementation(project(":worldgen"))
    implementation(project(":entity"))
    implementation(project(":game"))
    implementation(project(":api"))

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlin.logging)
    implementation(libs.slf4j.api)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
