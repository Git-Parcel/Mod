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
    description = "Checks version-sensitive runtime and API layering boundaries."

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
                            content.contains("import io.github.leawind.gitparcel.common.platform.") ||
                            content.contains("import io.github.leawind.gitparcel.server.minecraft.") ||
                            content.contains("import io.github.leawind.gitparcel.client.minecraft.")
                    )
                ) {
                    violations += "$relativePath makes the API layer depend on a runtime implementation"
                }

                if (relativePath.endsWith("/common/api/parcel/ParcelMeta.java") &&
                    (
                        content.contains("ParcelFormatRegistry") ||
                            content.contains("import net.minecraft.SharedConstants;") ||
                            content.contains("import net.minecraft.world.level.block.Rotation;") ||
                            content.contains(
                                "import net.minecraft.world.level.levelgen.structure.BoundingBox;",
                            )
                    )
                ) {
                    violations += "$relativePath mixes metadata with Minecraft runtime creation"
                }

                if (relativePath.endsWith("/common/api/world/Parcel.java") &&
                    content.contains("ParcelFormatRegistry")
                ) {
                    violations += "$relativePath creates models through the runtime format registry"
                }

                val minecraftVersionAdapter =
                    relativePath.endsWith(
                        "/common/minecraft/logic/version/MinecraftVersion.java",
                    )
                if (!minecraftVersionAdapter &&
                    content.contains("SharedConstants.getCurrentVersion()")
                ) {
                    violations += "$relativePath bypasses the Minecraft version adapter"
                }

                if (relativePath.contains("/common/api/permission/") &&
                    (
                        content.contains("import net.minecraft.commands.") ||
                            content.contains("import net.minecraft.server.permissions.")
                    )
                ) {
                    violations += "$relativePath exposes Minecraft's version-specific permission API"
                }

                val minecraftPermissionAdapter =
                    relativePath.endsWith(
                        "/common/minecraft/logic/permission/MinecraftPermissions.java",
                    )
                if (!minecraftPermissionAdapter &&
                    (
                        content.contains("Commands.LEVEL_") ||
                            content.contains("Commands.hasPermission(") ||
                            Regex(
                                """\b(source|player|serverPlayer)\.hasPermissions?\(""",
                            ).containsMatchIn(content) ||
                            content.contains("import net.minecraft.server.permissions.")
                    )
                ) {
                    violations += "$relativePath bypasses the Minecraft permission adapter"
                }

                val savedDataAccess =
                    relativePath.endsWith(
                        "/common/minecraft/logic/world/GitParcelSavedDataAccess.java",
                    )
                val codecSavedData =
                    relativePath.endsWith(
                        "/common/minecraft/logic/world/CodecSavedData.java",
                    )
                if (!savedDataAccess &&
                    (
                        content.contains("SavedDataType<") ||
                            content.contains("SavedData.Factory<") ||
                            Regex(
                                """\.getDataStorage\(\)\s*\.computeIfAbsent\(""",
                            ).containsMatchIn(content)
                    )
                ) {
                    violations += "$relativePath bypasses the Minecraft SavedData access adapter"
                }
                if (!codecSavedData &&
                    relativePath.contains("/common/minecraft/logic/world/") &&
                    content.contains("extends SavedData")
                ) {
                    violations += "$relativePath bypasses the codec-backed SavedData base"
                }

                val isPortableBlockSection =
                    relativePath.endsWith("/common/api/parcel/content/BlockSection.java")
                if (relativePath.contains("/common/api/parcel/") &&
                    !isPortableBlockSection &&
                    (
                        content.contains(
                            "import net.minecraft.world.level.block.state.BlockState;",
                        ) ||
                            content.contains("import org.joml.Matrix4f;")
                    )
                ) {
                    violations += "$relativePath exposes runtime-specific transform operations"
                }

                if (relativePath.endsWith("/common/api/parcel/ParcelFormat.java") &&
                    content.contains("import net.minecraft.world.level.block.Block;")
                ) {
                    violations += "$relativePath exposes version-specific block update annotations"
                }

                val isContextFormat =
                    content.contains("ParcelFormat.ContextSaver") ||
                        content.contains("ParcelFormat.ContextLoader")
                if (isContextFormat &&
                    (
                        content.contains("import net.minecraft.world.level.Level;") ||
                            content.contains(
                                "import net.minecraft.world.level.ServerLevelAccessor;",
                            ) ||
                            content.contains("import net.minecraft.world.level.block.Block;")
                    )
                ) {
                    violations += "$relativePath bypasses its format operation context"
                }

                if (relativePath.contains("/testutils/")) {
                    violations += "$relativePath places test-only code in the production source set"
                }

                if (relativePath.contains("/common/minecraft/logic/network/protocol/")) {
                    violations += "$relativePath reintroduces the retired network protocol package"
                }

                val minecraftPayloadAdapter =
                    relativePath.endsWith(
                        "/common/minecraft/logic/network/payload/MinecraftPayloads.java",
                    )
                if (!minecraftPayloadAdapter &&
                    (
                        content.contains("import net.minecraft.network.RegistryFriendlyByteBuf;") ||
                            content.contains("import net.minecraft.network.codec.") ||
                            content.contains(
                                "import net.minecraft.network.protocol.common.custom.CustomPacketPayload;",
                            )
                    )
                ) {
                    violations += "$relativePath bypasses the Minecraft payload adapter"
                }

                if (relativePath.endsWith("/common/platform/api/ServerNetworking.java") &&
                    content.contains("net.minecraft.network")
                ) {
                    violations += "$relativePath exposes Minecraft's version-specific payload API"
                }

                if (relativePath.endsWith(
                        "/client/minecraft/logic/network/ClientPayloadHandler.java",
                    ) &&
                    content.contains(".network.payload.")
                ) {
                    violations += "$relativePath handles transport payloads instead of stable messages"
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
