import gg.meza.stonecraft.mod
import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("com.gradleup.shadow") version "8.3.10"
    id("gg.meza.stonecraft")
    id("me.champeau.jmh") version "0.7.2"
}

val props: Map<String, Any> = project.properties.mapNotNull { (key, value) -> value?.let { key to it } }.toMap()

modSettings {
    // https://stonecraft.meza.gg/docs/configuration

    clientOptions {
        // https://minecraft.wiki/w/Options.txt
        fov = 88
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

    //runDirectory = rootProject.layout.projectDirectory.dir("run-temp")

    val vars = props
        .filterKeys { it.startsWith("mod.") }
        .mapKeys { it.key.removePrefix("mod.") }
    variableReplacements.putAll(vars)
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")

    // required by Modern UI
    maven {
        name = "IzzelAliz Maven"
        url = uri("https://maven.izzel.io/releases/")
    }
    maven {
        url = uri("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/")
        content {
            includeGroup("fuzs.forgeconfigapiport")
        }
    }

    // required by ModMenu
    maven {
        name = "Terraformers"
        url = uri("https://maven.terraformersmc.com/")
    }
}

val shadowBundle: Configuration by configurations.creating
fun DependencyHandlerScope.shadowBundle(dependencyNotation: String) {
    implementation(dependencyNotation)
    add("shadowBundle", dependencyNotation)
}
dependencies {
    // region mods

    // TODO why can't use modImplementation? it worked in 1.21.11
    implementation("com.github.Leawind:SystemStorageLib:0.2.0-beta.1")

    // region Modern UI

    // Modern UI - core framework (>= 3.13.0 uses new coordinates)
    implementation("dev.icyllis:modernui-core:${project.property("mod.modernui_version")}")
    // Modern UI - Markflow (required)
    implementation("icyllis.modernui:ModernUI-Markflow:${project.property("mod.modernui_version")}")
    if (mod.isFabric) {
        // compatible with Minecraft 26.1~26.1.2
        implementation("icyllis.modernui:ModernUI-Fabric:26.1.2-3.13.0.4")
    } else if (mod.isNeoforge) {
        // compatible with Minecraft 26.1~26.1.2
        implementation("icyllis.modernui:ModernUI-NeoForge:26.1.2-3.13.0.4")
    }
    // endregion

    // region forge config api port
    // it's required by Modern UI
    if (mod.isFabric) {
        // https://github.com/Fuzss/ForgeConfigApiPort
        implementation("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.1.3")
    }
    // endregion

    // region mod menu
    if (mod.isFabric) {
        implementation("com.terraformersmc:modmenu:18.0.0-beta.1")
    }
    // endregion

    // endregion

    // region bundled

    shadowBundle("com.github.Leawind:inventory-java:0.2.1")
    shadowBundle("com.github.ben-manes.caffeine:caffeine:3.2.3");

    implementation("org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r") {
        // already provided by Minecraft
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    // endregion

    // JMH for performance testing
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        // conflict with 1.20.1-forge `guava:32.1.1-jre`
        exclude(group = "com.google.guava", module = "guava")
    }

    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")

    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}
if (mod.loader == "neoforge") {
    dependencies {
        // NeoForge official test framework - provides proper FMLLoader
        // initialization for unit tests that need Minecraft classes
        testImplementation("net.neoforged:testframework:${props["neoforge_version"]}")
    }
}

if (mod.isFabric) {
    fabricApi {
        configureTests {
            createSourceSet = true
            modId = project.property("mod.id") as String
            enableGameTests = true
            enableClientGameTests = false
            eula = true
            clearRunDirectory = true
            username = "Player0"
        }
    }

    dependencies {
        "gametestImplementation"("com.google.jimfs:jimfs:1.3.0") {
            exclude(group = "com.google.guava", module = "guava")
        }
    }
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)

    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    println("remapJar: ${tasks.findByName("remapJar")}, project.name=${project.name}, project.version=${project.version}")
    if (tasks.findByName("remapJar") == null) {
        archiveClassifier.set("")
    } else {
        archiveClassifier.set("shadow")
    }

    minimize()

    val dest = "${project.property("mod.group")}.lib"
    // com.github.Leawind:inventory-java
    relocate("io.github.leawind.inventory", "${dest}.inventory")

    // com.github.ben-manes.caffeine:caffeine
    dependencies {
        exclude(dependency("com.google.errorprone:.*"))
        exclude(dependency("org.jspecify:.*"))
    }
    exclude("META-INF/LICENSE")
    relocate("com.github.benmanes.caffeine", "${dest}.caffeine")

    // org.eclipse.jgit:org.eclipse.jgit
    dependencies {
        exclude(dependency("org.slf4j:.*"))
    }
    relocate("org.eclipse.jgit", "${dest}.jgit")
    relocate("org.apache.commons", "${dest}.apache.commons")
    relocate("com.googlecode.javaewah", "${dest}.javaewah")
    exclude("about.html")
    exclude("OSGI-INF/**")
    exclude("META-INF/maven/**")
    exclude("versions/**")
    exclude("LICENSE.txt")
    exclude("NOTICE.txt")
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

tasks.named<Jar>("jmhJar") {
    isZip64 = true
}
