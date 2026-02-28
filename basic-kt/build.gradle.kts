plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("MainKt")
}

dependencies {
    implementation("com.clockworklabs:lib:0.1.0")
    implementation(libs.kotlinx.coroutines.core)
}

// -- SpacetimeDB bindings generation --

val spacetimedbCli: String = providers.gradleProperty("spacetimedb.cli")
    .orElse("spacetimedb-cli")
    .get()

val modulePath: String = layout.projectDirectory.dir(
    providers.gradleProperty("spacetimedb.modulePath").orElse("spacetimedb").get()
).asFile.absolutePath

val generatedBindingsDir: Provider<Directory> = layout.buildDirectory.dir("generated/spacetimedb")

sourceSets {
    main {
        kotlin {
            srcDir(generatedBindingsDir)
        }
    }
}

val generateBindings by tasks.registering(Exec::class) {
    description = "Generate SpacetimeDB Kotlin bindings"
    group = "spacetimedb"

    val outDir = generatedBindingsDir.get().asFile
    inputs.dir(modulePath)
    outputs.dir(outDir)

    doFirst {
        outDir.mkdirs()
    }

    commandLine(
        spacetimedbCli, "generate",
        "--lang", "kotlin",
        "--out-dir", outDir.absolutePath,
        "--module-path", modulePath
    )
}

tasks.named("compileKotlin") {
    dependsOn(generateBindings)
}
