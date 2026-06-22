plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    // The server only knows the Subsystem SPI (in :core) and infrastructure — never the
    // concrete domain modules. The composition root (:app) wires those in.
    api(project(":core"))
    implementation(project(":api"))
    implementation(project(":network"))
}
