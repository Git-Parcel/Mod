import gg.meza.stonecraft.mod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.util.internal.VersionNumber

plugins {
    id("com.gradleup.shadow") version "8.3.10"
    id("gg.meza.stonecraft")
}

modSettings {
    clientOptions {
        // https://minecraft.wiki/w/Options.txt
        fov = 90
        guiScale = 2
        narrator = false
        musicVolume = 0.0
        guiScale = 3

        additionalLines = mapOf(
            "maxFps" to "60",
            "renderDistance" to "8",
            "simulationDistance" to "5",
            "mouseSensitivity" to "0.22"
        )
    }

    val props: Map<String, Any> = project.properties
        .filterKeys { it.startsWith("mod.") }
        .mapKeys { it.key.removePrefix("mod.") }
        .mapNotNull { (key, value) -> value?.let { key to it } }
        .toMap()

    variableReplacements.putAll(props)
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val shadowBundle: Configuration by configurations.creating

fun DependencyHandlerScope.shadow(dependencyNotation: String) {
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}

configurations.all {
    exclude(group = "net.fabricmc.fabric-api", module = "fabric-gametest-api-v1")
}

if (VersionNumber.parse(mod.minecraftVersion) <= VersionNumber.parse("1.16.5")) {
    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    shadow("com.github.Leawind:inventory-java:46400162ee")
    shadow("dev.dirs:directories:26")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        exclude(group = "com.google.guava", module = "guava") // conflict with 1.20.1-forge `guava:32.1.1-jre`
    }

    compileOnly("org.jspecify:jspecify:1.0.0")
}

tasks.shadowJar {
    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    configurations = listOf(shadowBundle)

    println("remapJar: ${tasks.findByName("remapJar")}, project.name=${project.name}, project.version=${project.version}")
    if (tasks.findByName("remapJar") == null) {
        archiveClassifier.set("")
    } else {
        archiveClassifier.set("shadow")
    }

    minimize()

    relocate("io.github.leawind.inventory", "io.github.leawind.systemstoragelib.lib.inventory")

    // directories
    relocate("dev.dirs", "io.github.leawind.systemstoragelib.lib.dirs")
    exclude("META-INF/native-image/**")
}

tasks.withType<RemapJarTask>().matching { it.name == "remapJar" }.configureEach {
    dependsOn(tasks.shadowJar)
    inputFile.set(tasks.shadowJar.flatMap { it.archiveFile })
}

val collectJar by tasks.registering(Copy::class) {
    description = "Collect all jars to one directory"
    dependsOn(tasks.assemble)
    from(layout.buildDirectory.dir("libs")) {
        include("*.jar")
        exclude("*-shadow.jar")
    }
    into(rootProject.layout.buildDirectory.dir("all-libs"))
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
    finalizedBy(collectJar)
}

if (mod.isForge) {
    tasks.compileTestJava {
        dependsOn("generatePackMCMetaJson")
    }
}

tasks.test {
    useJUnitPlatform()
}

publishMods {
    modrinth {
        if (mod.isFabric) {
            requires("fabric-api")
            optional("modmenu")
        }
    }

    curseforge {
        clientRequired = false
        serverRequired = false
        if (mod.isFabric) requires("fabric-api")
    }
}
