plugins {
    alias(libs.plugins.kotlinJvm)
    id("com.clockworklabs.spacetimedb")
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

spacetimedb {
    cli.set(file("/home/fromml/Projects/SpacetimeDB/target/release/spacetimedb-cli"))
}

dependencies {
    implementation("com.clockworklabs:lib:0.1.0")
    implementation(libs.kotlinx.coroutines.core)
}
