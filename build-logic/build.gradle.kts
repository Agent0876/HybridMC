plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.kotlin.serialization.gradle.plugin)
    implementation(libs.spotless.gradle.plugin)
    implementation(libs.kover.gradle.plugin)

    // Exposes the version catalog's generated accessors (LibrariesForLibs) to the
    // precompiled convention plugins — the standard "libs in build-logic" workaround.
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}
