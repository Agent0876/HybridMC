plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    api(project(":core"))
    api(project(":registry"))
    implementation(libs.fastutil)
}
