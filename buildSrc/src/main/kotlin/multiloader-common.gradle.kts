plugins {
    id("java-library")
    id("maven-publish")
}

val props = project.properties

base {
    archivesName = "${props["mod_id"]}-${project.name}-${props["minecraft_version"]}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(props["java_version"].toString().toInt())
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
        capability("${props["group"]}:${project.name}:${props["version"]}")
        capability("${props["group"]}:${base.archivesName.get()}:${props["version"]}")
        capability(
            "${props["group"]}:${props["mod_id"]}-${project.name}-${props["minecraft_version"]}:${
                project.property(
                    "version"
                )
            }"
        )
        capability("${props["group"]}:${props["mod_id"]}:${props["version"]}")
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        suppressPomMetadataWarningsFor(variant)
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${props["mod_name"]}" }
    }
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${props["mod_name"]}" }
    }

    manifest {
        attributes(
            "Specification-Title" to props["mod_name"],
            "Specification-Vendor" to props["mod_author"],
            "Specification-Version" to props["version"],
            "Implementation-Title" to project.name,
            "Implementation-Version" to props["version"],
            "Implementation-Vendor" to props["mod_author"],
            "Built-On-Minecraft" to props["minecraft_version"]
        )
    }
}

// Apply resource expansion for all source sets
tasks.withType<ProcessResources>().configureEach {
    val expandProps = mapOf(
        "version" to props["version"],
        "group" to props["group"], //Else we target the task's group.
        "minecraft_version" to props["minecraft_version"],
        "minecraft_version_range" to props["minecraft_version_range"],
        "fabric_version" to props["fabric_version"],
        "fabric_loader_version" to props["fabric_loader_version"],
        "mod_name" to props["mod_name"],
        "mod_author" to props["mod_author"],
        "mod_id" to props["mod_id"],
        "license" to props["license"],
        "description" to props["description"],
        "neoforge_version" to props["neoforge_version"],
        "neoforge_loader_version_range" to props["neoforge_loader_version_range"],
        "forge_version" to props["forge_version"],
        "forge_loader_version_range" to props["forge_loader_version_range"],
        "credits" to props["credits"],
        "java_version" to props["java_version"]
    )

    val jsonExpandProps = expandProps.mapValues { (_, value) ->
        if (value is String) value.replace("\n", "\\\\n") else value
    }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json", "*.mixins.json")) {
        expand(jsonExpandProps)
    }

    inputs.properties(expandProps)
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
