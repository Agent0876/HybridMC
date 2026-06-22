plugins {
    // Public, versioned, ABI-tracked, published plugin API.
    id("hybridmc.published-library")
}

dependencies {
    api(project(":core"))
}
