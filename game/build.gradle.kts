plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":world"))
    implementation(project(":entity"))
    implementation(project(":registry"))
}
