dependencies {
    implementation(project(":server-core"))
    implementation(libs.raknetty.transport)
    implementation(libs.raknetty.codec)
    implementation(libs.raknetty.handler)
    implementation(libs.raknetty.core)
    implementation(libs.kotlinx.coroutines.core)
}
