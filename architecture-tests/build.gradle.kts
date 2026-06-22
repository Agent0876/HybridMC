plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    // Konsist scans source files on disk, so no project dependencies are needed.
    testImplementation(libs.konsist)
}
