plugins {
    id("java-library")
    id("maven-publish")
}

val mod_id: String by project
val minecraft_version: String by project
val minecraft_version_range: String by project
val java_version: String by project
val mod_name: String by project
val mod_author: String by project
val license: String by project
val credits: String by project
val fabric_version: String by project
val fabric_loader_version: String by project
val neoforge_version: String by project
val neoforge_loader_version_range: String by project
val forge_version: String by project
val forge_loader_version_range: String by project

base {
    archivesName = "${mod_id}-${project.name}-${minecraft_version}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(java_version)
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
        capability("$group:${project.name}:$version")
        capability("$group:${base.archivesName.get()}:$version")
        capability("$group:$mod_id-${project.name}-${minecraft_version}:$version")
        capability("$group:$mod_id:$version")
    }
    publishing.publications.withType<MavenPublication>().configureEach {
        suppressPomMetadataWarningsFor(variant)
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${mod_name}" }
    }
}

tasks.jar {
    from(rootProject.file("LICENSE"))
    filesMatching(listOf("LICENSE")) {
        rename { original -> "${original}_${mod_name}" }
    }

    manifest {
        attributes(
            "Specification-Title" to mod_name,
            "Specification-Vendor" to mod_author,
            "Specification-Version" to version.toString(),
            "Implementation-Title" to project.name,
            "Implementation-Version" to version.toString(),
            "Implementation-Vendor" to mod_author,
            "Built-On-Minecraft" to minecraft_version
        )
    }
}

// Apply resource expansion for all source sets
tasks.withType<ProcessResources>().configureEach {
    val expandProps = mapOf(
        "version" to version,
        "group" to project.group, //Else we target the task's group.
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "fabric_version" to fabric_version,
        "fabric_loader_version" to fabric_loader_version,
        "mod_name" to mod_name,
        "mod_author" to mod_author,
        "mod_id" to mod_id,
        "license" to license,
        "description" to project.description,
        "neoforge_version" to neoforge_version,
        "neoforge_loader_version_range" to neoforge_loader_version_range,
        "forge_version" to forge_version,
        "forge_loader_version_range" to forge_loader_version_range,
        "credits" to credits,
        "java_version" to java_version
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
