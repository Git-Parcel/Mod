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

val checkArchitectureBoundaries by tasks.registering {
    group = "verification"
    description = "Checks loader, networking, source-set, and API layering boundaries."

    val mainJava = layout.projectDirectory.dir("src/main/java")
    val productionMixins = layout.projectDirectory.file("src/main/resources/gitparcel.mixins.json")
    inputs.dir(mainJava)
    inputs.file(productionMixins)

    doLast {
        val violations = mutableListOf<String>()
        val sourceRoot = mainJava.asFile
        val loaderImport = Regex("""import net\.(fabricmc|neoforged|minecraftforge)\.""")

        sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { source ->
                val relativePath = source.relativeTo(sourceRoot).invariantSeparatorsPath
                val content = source.readText()

                if (!relativePath.contains("/platform/") && loaderImport.containsMatchIn(content)) {
                    violations += "$relativePath imports a loader API outside a platform package"
                }

                if (relativePath.contains("/common/api/") &&
                    (
                        content.contains("import io.github.leawind.gitparcel.common.minecraft.") ||
                            content.contains("import io.github.leawind.gitparcel.server.minecraft.") ||
                            content.contains("import io.github.leawind.gitparcel.client.minecraft.")
                    )
                ) {
                    violations += "$relativePath makes the API layer depend on a runtime implementation"
                }

                if (relativePath.contains("/testutils/")) {
                    violations += "$relativePath places test-only code in the production source set"
                }

                if (relativePath.contains("/common/minecraft/logic/network/protocol/") &&
                    (
                        content.contains("import io.github.leawind.gitparcel.client.") ||
                            content.contains("import net.minecraft.client.")
                    )
                ) {
                    violations += "$relativePath couples a common payload to client-only code"
                }
            }

        val mixinConfig = productionMixins.asFile.readText()
        val retiredRuntimeHooks =
            listOf(
                "AccessGameTestHelper",
                "AccessMinecraftServer",
                "InvokeArgumentTypeInfos",
                "MixinArgumentTypeInfos",
                "MixinClientCommonPacketListenerImpl",
                "MixinGameRenderer",
                "MixinMinecraft",
                "MixinPlayerList",
            )
        retiredRuntimeHooks
            .filter(mixinConfig::contains)
            .forEach { violations += "production mixin config reintroduces retired hook $it" }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "Architecture boundary violations:\n" +
                    violations.joinToString(separator = "\n") { " - $it" },
            )
        }
    }
}

subprojects {
    tasks.matching { it.name == "check" }.configureEach {
        dependsOn(checkArchitectureBoundaries)
    }
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
