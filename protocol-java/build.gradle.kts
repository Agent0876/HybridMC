plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":registry"))
    // Java edition transport: Netty TCP pipeline.
    implementation(libs.bundles.netty)
}
