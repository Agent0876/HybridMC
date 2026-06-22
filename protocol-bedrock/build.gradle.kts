plugins {
    id("hybridmc.kotlin-library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":registry"))
    // Bedrock edition transport: RakNet implemented over Netty UDP.
    implementation(libs.bundles.netty)
}
