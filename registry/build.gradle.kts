plugins {
    id("hybridmc.kotlin-library")
    // Build-time data pipeline that turns public datasets into registries.
    id("hybridmc.kotlin-serialization")
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.serialization.json)
}
