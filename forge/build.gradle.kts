plugins {
    id("multiloader-loader")
    id("net.minecraftforge.gradle") version "[6.0.46,6.2)"
    id("org.spongepowered.mixin") version "0.7-SNAPSHOT"
    id("idea")
}

val props = project.properties

base {
    archivesName.set("${props["mod.id"]}-forge-${props["mod.minecraft_version"]}")
}

mixin {
    config("${props["mod.id"]}.mixins.json")
    config("${props["mod.id"]}.forge.mixins.json")
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "MixinConfigs" to "${props["mod.id"]}.mixins.json,${props["mod.id"]}.forge.mixins.json"
            )
        )
    }
}

minecraft {
    mappings("official", props["mod.minecraft_version"] as String)

    copyIdeResources = true
    reobf = false

    // AccessTransformer: 使用函数调用而非赋值
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformer(at)
    }

    runs {
        // Client run
        create("client") {
            workingDirectory(file("runs/client"))
            ideaModule("${rootProject.name}.${project.name}.main")
            taskName("Client")
            mods {
                create("modClientRun") {
                    source(sourceSets["main"])
                }
            }
        }

        // Server run
        create("server") {
            workingDirectory(file("runs/server"))
            ideaModule("${rootProject.name}.${project.name}.main")
            taskName("Server")
            mods {
                create("modServerRun") {
                    source(sourceSets["main"])
                }
            }
        }

        // Data run
        create("data") {
            workingDirectory(file("runs/data"))
            ideaModule("${rootProject.name}.${project.name}.main")
            args(
                "--mod", props["mod.id"] as String,
                "--all",
                "--output", file("src/generated/resources/"),
                "--existing", file("src/main/resources/")
            )
            taskName("Data")
            mods {
                create("modDataRun") {
                    source(sourceSets["main"])
                }
            }
        }
    }
}

sourceSets["main"].resources.srcDir(layout.projectDirectory.dir("src/generated/resources"))

dependencies {
    minecraft("net.minecraftforge:forge:${props["mod.minecraft_version"]}-${props["dep.forge_version"]}")
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT:processor")
}

sourceSets.configureEach {
    val dir = layout.buildDirectory.dir("sourcesSets/$name")
    output.resourcesDir = dir.get().asFile
    java.destinationDirectory = dir.get().asFile
}

// Implement mcgradleconventions loader attribute
val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "forge")
        }
    }
}
sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "forge")
            }
        }
    }
}
