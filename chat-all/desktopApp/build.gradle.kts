import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.sharedClient)
    implementation(compose.desktop.currentOs)
}

compose.desktop {
    application {
        mainClass = "com.clockworklabs.spacetimedb_chat_all.desktop_app.MainKt"
        val clientArg = providers.gradleProperty("clientId").orNull
        if (clientArg != null) {
            args += listOf("--client", clientArg)
        }
        jvmArgs("--enable-native-access=ALL-UNNAMED")

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.clockworklabs.spacetimedb_chat_all"
            packageVersion = "1.0.0"
        }
    }
}
