import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("multiloader-loader")
    id("fabric-loom")
}

val props = project.properties

dependencies {
    minecraft("com.mojang:minecraft:${props["minecraft_version"]}")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${props["parchment_minecraft"]}:${props["parchment_version"]}@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:${props["fabric_loader_version"]}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${props["fabric_version"]}")
}

loom {
    val aw = project(":common").file("src/main/resources/${props["mod_id"]}.accesswidener")
    if (aw.exists()) {
        accessWidenerPath.set(aw)
    }
    mixin {
        defaultRefmapName.set("${props["mod_id"]}.refmap.json")
    }
    runs {
        named("client") {
            client()
            configName = "Fabric Client"
            ideConfigGenerated(true)
            runDir = "runs/client"
        }
        named("server") {
            server()
            configName = "Fabric Server"
            ideConfigGenerated(true)
            runDir = "runs/server"
        }
    }

    log4jConfigs.from(file("log4j-dev.xml"))
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = props["mod_id"] as String
        enableGameTests = true
        enableClientGameTests = false
        eula = true
        clearRunDirectory = true
        username = "Player0"
    }
}

// Implement mcgradleconventions loader attribute
val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
listOf(
    "apiElements",
    "runtimeElements",
    "sourcesElements",
    "javadocElements",
    "includeInternal",
    "modCompileClasspath"
).forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }
}
sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "fabric")
            }
        }
    }
}

// ✅ configureEach 使用 receiver lambda 语法
loom.remapConfigurations.configureEach {
    configurations.named(name) {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }
}

// Override shadow jar classifier to avoid output path conflict with remapJar
tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("shadow-dev")
}

// Remap the shadow jar (Mojang mapped) to Intermediary so mixin references work at runtime
tasks.remapJar {
    dependsOn("shadowJar")
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}
