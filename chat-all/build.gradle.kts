import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.clockworklabs.spacetimedb")
}

kotlin {
    jvmToolchain(21)
}

spacetimedb {
    cli.set(file("/home/fromml/Projects/SpacetimeDB/target/release/spacetimedb-cli"))
}

dependencies {
    implementation("com.clockworklabs:lib:0.1.0")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation("com.clockworklabs:lib:0.1.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"

        val clientArg = providers.gradleProperty("clientId").orNull
        if (clientArg != null) {
            args += listOf("--client", clientArg)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Deb)
            packageName = "chat-all"
            packageVersion = "1.0.0"
        }
    }
}
