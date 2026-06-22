plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    // Primitive collections for buffers, palettes, chunk storage.
    implementation(libs.fastutil)
}
