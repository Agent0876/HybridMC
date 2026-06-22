plugins {
    id("hybridmc.kotlin-application")
}

dependencies {
    implementation(project(":server"))
    implementation(project(":plugin-host"))
    implementation(project(":network"))

    // Composition root: the only place that depends on every concrete domain module
    // so it can register their subsystems into the server.
    implementation(project(":registry"))
    implementation(project(":world"))
    implementation(project(":worldgen"))
    implementation(project(":entity"))
    implementation(project(":game"))

    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("io.hybridmc.app.MainKt")
}
