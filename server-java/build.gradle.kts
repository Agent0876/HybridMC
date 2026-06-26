dependencies {
    implementation(project(":server-core"))
    implementation(libs.netty.transport)
    implementation(libs.netty.handler)
    implementation(libs.netty.codec)
    implementation(libs.netty.buffer)
    implementation(libs.kotlinx.coroutines.core)
}
