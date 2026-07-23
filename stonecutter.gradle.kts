plugins {
    id("dev.kikugie.stonecutter")

    val modstitchVersion = "0.8.4"
    id("dev.isxander.modstitch.base") version modstitchVersion apply false
    id("net.fabricmc.fabric-loom") version "1.15-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "8.3.10" apply false

    id("me.modmuss50.mod-publish-plugin") version "0.8.4" apply false
}

stonecutter active "26.1-fabric"

val buildAndCollect by tasks.registering(Sync::class) {
    group = "build"
    description = "Builds and collects all distributable jars."
    into(layout.buildDirectory.dir("libs"))
}

allprojects {
    repositories {
        mavenCentral()

        exclusiveContent {
            forRepository { maven("https://jitpack.io") }
            filter { includeGroup("com.github.Leawind") }
        }

        // The Terraformers Maven occasionally responds with 502.
        exclusiveContent {
            forRepository {
                maven("https://maven.gnomecraft.net/releases") {
                    name = "GnomeCraft (Terraformers Mirror)"
                }
            }
            filter { includeGroup("com.terraformersmc") }
        }

        // Modern UI
        exclusiveContent {
            forRepository {
                maven("https://maven.izzel.io/releases/") {
                    name = "IzzelAliz Maven"
                }
            }
            filter {
                includeGroup("icyllis.modernui")
            }
        }
        maven {
            url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
            content {
                includeGroup("fuzs.forgeconfigapiport")
            }
        }

        maven("https://maven.neoforged.net/releases/") {
            content {
                includeGroupByRegex("net\\.neoforged(\\..*)?")
            }
        }
    }
}
