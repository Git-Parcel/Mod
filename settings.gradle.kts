pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.kikugie.dev/releases")
        maven("https://maven.kikugie.dev/snapshots")
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
}

stonecutter {
    centralScript = "build.gradle.kts"
    kotlinController = true
    shared {
        fun mc(version: String, vararg loaders: String) {
            for (loader in loaders) {
                version("$version-$loader", version)
            }
        }

        mc("26.1", "fabric", "neoforge")

        vcsVersion = "26.1-fabric"
    }
    create(rootProject)
}

rootProject.name = "gitparcel"
