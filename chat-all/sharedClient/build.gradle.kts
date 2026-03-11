plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    id("com.clockworklabs.spacetimedb")
}

kotlin {
    androidLibrary {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        namespace = "com.clockworklabs.spacetimedb_chat_all.shared_client"
    }

    if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "SharedClient"
                isStatic = true
            }
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)

            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.ktor.client.okhttp)
        }

        commonMain.dependencies {
            implementation(libs.androidx.lifecycle.runtime.compose)

            implementation(libs.clockworklabs.spacetimedb.sdk)

            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)

            api(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            implementation(libs.bundles.ktor.client)

            implementation(libs.material3)
            implementation(libs.compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.clockworklabs.spacetimedb.sdk)
        }

        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.ktor.client.okhttp)
        }

        if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
            nativeMain.dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

spacetimedb {
    cli.set(file("/home/fromml/Projects/SpacetimeDB/target/release/spacetimedb-cli"))
    modulePath.set(rootProject.layout.projectDirectory.dir("spacetimedb"))
}

// Workaround: SDK plugin doesn't declare task dependency for Android compilation
tasks.configureEach {
    if (name == "compileAndroidMain") {
        dependsOn("generateSpacetimeBindings")
    }
}
