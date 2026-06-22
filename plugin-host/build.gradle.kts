plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    // Loads plugins written against :api and binds them into the running :server.
    implementation(project(":api"))
    implementation(project(":server"))
}
