plugins {
    id("multiloader-loader")
    id("net.neoforged.moddev")
}

val props = project.properties;

neoForge {
    version = props["neoforge_version"] as String
    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    parchment {
        minecraftVersion = props["parchment_minecraft"] as String
        mappingsVersion = props["parchment_version"] as String
    }
    runs {
        register("client") {
            type.set("client")
            systemProperty("neoforge.enabledGameTestNamespaces", props["mod_id"] as String)
            ideName.set("NeoForge Client (${project.path})")
        }
        register("server") {
            type.set("server")
            systemProperty("neoforge.enabledGameTestNamespaces", props["mod_id"] as String)
            ideName.set("NeoForge Server (${project.path})")
        }
        register("data") {
            type.set("clientData")
            systemProperty("neoforge.enabledGameTestNamespaces", props["mod_id"] as String)
            ideName.set("NeoForge Data (${project.path})")
            programArguments.addAll(
                "--mod", props["mod_id"] as String,
                "--all",
                "--output", file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath
            )
        }
    }
    mods {
        create(props["mod_id"] as String) {
            sourceSet(sourceSets["main"])
        }
    }
}

sourceSets["main"].resources {
    srcDir("src/generated/resources")
}

val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant: String ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "neoforge")
        }
    }
}
sourceSets.configureEach {
    val configNames = listOf(
        compileClasspathConfigurationName,
        runtimeClasspathConfigurationName,
        getTaskName(null, "jarJar")
    )
    configNames.forEach { variant: String ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "neoforge")
            }
        }
    }
}
