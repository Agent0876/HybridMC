plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    api(project(":core"))
    implementation(project(":world"))
    implementation(project(":registry"))
}
