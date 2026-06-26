plugins {
    application
}

dependencies {
    implementation(project(":server-core"))
    implementation(project(":server-java"))
    implementation(project(":server-bedrock"))
}

application {
    mainClass.set("io.github.agent0876.hybridmc.app.MainKt")
}
