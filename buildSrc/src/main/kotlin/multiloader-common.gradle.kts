plugins {
    id("java-library")
    id("maven-publish")
}

val props = project.properties

tasks.withType<AbstractArchiveTask> {
    val loaderName = archiveBaseName.get();
    archiveBaseName = "${project.property("mod.id")}"
    archiveVersion = "${project.property("mod.version")}+${loaderName}+mc${project.property("mod.minecraft_version")}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(props["dep.java_version"].toString().toInt())
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    // https://docs.gradle.org/current/userguide/declaring_repositories.html#declaring_content_exclusively_found_in_one_repository
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven {
                name = "ParchmentMC"
                url = uri("https://maven.parchmentmc.org/")
            },
            maven {
                name = "NeoForge"
                url = uri("https://maven.neoforged.net/releases")
            }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven {
        name = "BlameJared"
        url = uri("https://maven.blamejared.com")
    }

    maven { url = uri("https://jitpack.io") }
}

// Declare capabilities on the outgoing configurations.
// Read more about capabilities here: https://docs.gradle.org/current/userguide/component_capabilities.html#sec:declaring-additional-capabilities-for-a-local-component
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.getByName(variant).outgoing {
        capability("${props["mod.group"]}:${project.name}:${props["mod.version"]}")
        capability("${props["mod.group"]}:${base.archivesName.get()}:${props["mod.version"]}")
        capability(
            "${props["mod.group"]}:${props["mod.id"]}-${project.name}-${props["mod.minecraft_version"]}:${
                props["mod.version"]
            }"
        )
        capability("${props["mod.group"]}:${props["mod.id"]}:${props["mod.version"]}")
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        suppressPomMetadataWarningsFor(variant)
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${props["mod.name"]}" }
    }
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${props["mod.name"]}" }
    }

    manifest {
        attributes(
            "Specification-Title" to props["mod.name"],
            "Specification-Vendor" to props["mod.author"],
            "Specification-Version" to props["mod.version"],
            "Implementation-Title" to project.name,
            "Implementation-Version" to props["mod.version"],
            "Implementation-Vendor" to props["mod.author"],
            "Built-On-Minecraft" to props["mod.minecraft_version"]
        )
    }
}

// Apply resource expansion for all source sets
tasks.withType<ProcessResources>().configureEach {
    val rawExpandProps: Map<String, Any?> = project.properties
        .filterKeys { it.startsWith("mod.") || it.startsWith("dep.") }

    // Convert flat dotted keys ("mod.id", "dep.java_version") into nested maps
    // so Groovy SimpleTemplateEngine can resolve "${mod.id}" etc.
    fun Map<String, Any?>.toNested(): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        for ((key, value) in this) {
            val parts = key.split(".", limit = 2)
            if (parts.size == 2 && value != null) {
                @Suppress("UNCHECKED_CAST")
                val nested =
                    result.computeIfAbsent(parts[0]) { linkedMapOf<String, Any?>() } as MutableMap<String, Any?>
                nested[parts[1]] = value
            }
        }
        return result
    }

    val jsonRawExpandProps: Map<String, Any?> = rawExpandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(rawExpandProps.toNested())
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonRawExpandProps.toNested())
    }

    inputs.properties(rawExpandProps)
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
    repositories {
        val localMavenUrl = System.getenv("local_maven_url")
        if (localMavenUrl != null) {
            maven {
                url = uri(localMavenUrl)
            }
        }
    }
}
