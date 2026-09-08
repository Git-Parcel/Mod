import net.fabricmc.loom.task.RemapJarTask

plugins {
    id("dev.kikugie.stonecutter")
    id("dev.isxander.modstitch.base")
    id("com.gradleup.shadow")
    `maven-publish`
    id("me.modmuss50.mod-publish-plugin")
}

// region Versions & Project Info
val mcVersion = requiredProp("mcVersion")
val modGroupValue = requiredProp("mod.group")
val modIdValue = requiredProp("mod.id")
val modVersionString = requiredProp("mod.version")
val modNameValue = requiredProp("mod.name")
val modDescriptionValue = requiredProp("mod.description")
val modAuthorValue = requiredProp("mod.author")
val modLicenseValue = requiredProp("mod.license")
val modLogoFile = requiredProp("mod.logo_file")
val modHomeUrl = requiredProp("mod.home_url")
val modSourceUrl = requiredProp("mod.source_url")
val modIssuesUrl = requiredProp("mod.issues_url")
val modEmail = requiredProp("mod.email")

val systemStorageLibVersion = requiredProp("deps.systemStorageLib")
val modernUiVersion = requiredProp("deps.modernUi")

val isFabric = modstitch.isLoom
val isNeoforge = modstitch.isModDevGradleRegular
val isForge = modstitch.isModDevGradleLegacy
val loader = when {
    isFabric -> "fabric"
    isNeoforge -> "neoforge"
    isForge -> "forge"
    else -> error("Unknown loader")
}

// Unit testing: only Fabric is supported for now.
// Fabric: unitTesting() adds fabric-loader-junit which causes ServiceLoader classloader
// isolation issues, so we add JUnit dependencies manually instead.
// NeoForge: unitTesting() requires a valid mod JAR but classes dir isn't recognized.
val supportsUnitTesting = isFabric
// endregion

// region ModStitch Setup
modstitch {
    minecraftVersion = mcVersion

    loom {
        if (isFabric) {
            fabricLoaderVersion = requiredProp("deps.fabricLoader")
            configureLoom {
                runs.named("client") {
                    runDir = project.relativePath(rootProject.file("run"))
                }
            }
        }
    }

    if (!isFabric) {
        runs {
            register("client") {
                client()
                gameDirectory.set(rootProject.layout.projectDirectory.dir("run"))
            }
        }
    }

    moddevgradle {
        if (isNeoforge) {
            neoForgeVersion = requiredProp("deps.neoforge")
        }
        if (isForge) {
            forgeVersion = requiredProp("deps.forge")
        }
    }

    metadata {
        modId = modIdValue
        modName = modNameValue
        modVersion = "$modVersionString+$loader-$mcVersion"
        modGroup = modGroupValue
        modDescription = modDescriptionValue
        modLicense = modLicenseValue
        modAuthor = modAuthorValue

        replacementProperties.put("logo_file", modLogoFile)
        replacementProperties.put("home_url", modHomeUrl)
        replacementProperties.put("source_url", modSourceUrl)
        replacementProperties.put("issues_url", modIssuesUrl)
        replacementProperties.put("email", modEmail)
        replacementProperties.put("mc", requiredProp("meta.mcDep"))
        replacementProperties.put("system_storage_lib_version", systemStorageLibVersion)
        replacementProperties.put("modernui_version", modernUiVersion)
        if (isNeoforge || isForge) {
            replacementProperties.put("loaderVersion", requiredProp("meta.loaderDep"))
        }
    }

    mixin {
        addMixinsToModManifest = true
        configs.register("gitparcel")
        if (isFabric) configs.register("gitparcel.fabric")
        if (isForge) configs.register("gitparcel.forge")
        if (isNeoforge) configs.register("gitparcel.neoforge")
    }

    // Unit testing setup:
    // We don't use unitTesting() because fabric-loader-junit wraps tests in a Knot
    // classloader that causes ServiceLoader classloader isolation. Instead, we rely on
    // Fabric Loom's default test classpath (which includes Minecraft classes) and
    // add JUnit dependencies manually below.
}
// endregion

// region Stonecutter
stonecutter {
    constants {
        put("fabric", isFabric)
        put("neoforge", isNeoforge)
        put("forge", isForge)
    }

    // ResourceLocation -> Identifier
    replacements.string(current.parsed >= "1.21.11") {
        replace("net.minecraft.resources.ResourceLocation", "net.minecraft.resources.Identifier")
        replace("ResourceLocation", "Identifier")
    }
    // Input -> ClientInput
    replacements.string(current.parsed > "1.21") {
        replace(
            "net.minecraft.client.player.Input",
            "net.minecraft.client.player.ClientInput"
        )
    }
}
// endregion

// region Dependencies
val shadowBundle: Configuration by configurations.creating
configurations.named("implementation") {
    extendsFrom(shadowBundle)
}

// Force specific log4j version to avoid dynamic version resolution issues in offline mode
configurations.configureEach {
    resolutionStrategy {
        force("org.apache.logging.log4j:log4j-api:2.24.3")
        force("org.apache.logging.log4j:log4j-core:2.24.3")
    }
}

dependencies {
    // region mods
    if (isFabric) {
        modstitchModImplementation(
            "net.fabricmc.fabric-api:fabric-api:${requiredProp("deps.fabricApi")}",
        )
        modstitchModImplementation(
            "com.terraformersmc:modmenu:${requiredProp("deps.modMenu")}",
        )
    }

    modstitchModImplementation("com.github.Leawind:SystemStorageLib:$systemStorageLibVersion")

    // Modern UI
    modstitchModImplementation("dev.icyllis:modernui-core:$modernUiVersion")
    modstitchModImplementation("icyllis.modernui:ModernUI-Markflow:$modernUiVersion")
    if (isFabric) {
        modstitchModImplementation(
            "icyllis.modernui:ModernUI-Fabric:${requiredProp("deps.modernUiPlatform")}",
        )
    } else if (isNeoforge) {
        modstitchModImplementation(
            "icyllis.modernui:ModernUI-NeoForge:${requiredProp("deps.modernUiPlatform")}",
        )
    }

    // Forge Config API Port (required by Modern UI)
    if (isFabric) {
        modstitchModImplementation(
            "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:${requiredProp("deps.forgeConfigApiPort")}",
        )
    }
    // endregion

    // region bundled
    add(shadowBundle.name, "com.github.Leawind:inventory-java:0.4.0")
    add(shadowBundle.name, "com.github.ben-manes.caffeine:caffeine:3.2.3")
    add(shadowBundle.name, "org.eclipse.jgit:org.eclipse.jgit:7.6.0.202603022253-r") {
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "commons-codec", module = "commons-codec")
    }
    // endregion

    // JMH for performance testing
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    // Test - add JUnit for all platforms
    testCompileOnly("org.jspecify:jspecify:1.0.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.3")
    testImplementation("com.google.jimfs:jimfs:1.3.0") {
        exclude(group = "com.google.guava", module = "guava")
    }

    // Compile only
    compileOnly("org.jspecify:jspecify:1.0.0")
    compileOnly("org.jetbrains:annotations:24.0.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
}
// endregion

// region Fabric Gametest
if (isFabric) {
    the<net.fabricmc.loom.api.fabricapi.FabricApiExtension>().configureTests {
        createSourceSet.set(true)
        modId.set(project.property("mod.id") as String)
        enableGameTests.set(true)
        enableClientGameTests.set(false)
        eula.set(true)
        clearRunDirectory.set(true)
        username.set("Player0")
    }

    dependencies {
        "gametestImplementation"("com.google.jimfs:jimfs:1.3.0") {
            exclude(group = "com.google.guava", module = "guava")
        }
    }

    // Configure stonecutter variable replacement for gametest resources
    val gametestProps = mapOf(
        "id" to modIdValue,
        "version" to modVersionString,
        "name" to modNameValue,
        "logo_file" to modLogoFile,
    )
    tasks.named<ProcessResources>("processGametestResources") {
        filesMatching("fabric.mod.json") {
            expand(gametestProps)
        }
    }
}
// endregion

// region Shadow Jar
val shadowDest = "$modGroupValue.lib"

tasks.shadowJar {
    configurations = listOf(shadowBundle)

    dependsOn(tasks.processResources)
    tasks.findByName("generatePackMCMetaJson")?.let { dependsOn(it) }

    if (tasks.findByName("remapJar") == null) {
        archiveClassifier.set("")
    } else {
        archiveClassifier.set("shadow")
    }

    minimize()

    // com.github.Leawind:inventory-java
    relocate("io.github.leawind.inventory", "${shadowDest}.inventory")

    // com.github.ben-manes.caffeine:caffeine
    dependencies {
        exclude(dependency("com.google.errorprone:.*"))
        exclude(dependency("org.jspecify:.*"))
    }
    exclude("META-INF/LICENSE")
    relocate("com.github.benmanes.caffeine", "${shadowDest}.caffeine")

    // org.eclipse.jgit:org.eclipse.jgit
    dependencies {
        exclude(dependency("org.slf4j:.*"))
    }
    relocate("org.eclipse.jgit", "${shadowDest}.jgit")
    relocate("org.apache.commons", "${shadowDest}.apache.commons")
    relocate("com.googlecode.javaewah", "${shadowDest}.javaewah")
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

tasks.assemble {
    dependsOn(tasks.shadowJar)
}
// endregion

// region Tasks
tasks.withType<JavaExec>().configureEach {
    if (name == "runClient") {
        workingDir(rootProject.layout.projectDirectory.dir("run"))
    }
}

tasks.test {
    useJUnitPlatform()
    if (!supportsUnitTesting) {
        enabled = false
    }
}

// Skip test compilation for unsupported platforms
if (!supportsUnitTesting) {
    tasks.compileTestJava {
        enabled = false
    }
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

// Replace ${id} and other variables in mixin config files
val resourceProps = mapOf(
    "id" to modIdValue,
    "version" to modVersionString,
    "name" to modNameValue,
    "group" to modGroupValue,
)
tasks.named<ProcessResources>("processResources") {
    filesMatching("**/*.mixins.json") {
        expand(resourceProps)
    }
}

java {
    withSourcesJar()
}

// Exclude default refmap for Forge
if (isForge) {
    tasks.named<ProcessResources>("processResources") {
        exclude("gitparcel.refmap.json")
    }
}
// endregion

// region Publishing
val sourcesJar = tasks.named<Jar>("sourcesJar")
rootProject.tasks.named<Sync>("buildAndCollect") {
    dependsOn(modstitch.finalJarTask, tasks.shadowJar, sourcesJar)
    from(modstitch.finalJarTask.flatMap { it.archiveFile })
    from(sourcesJar.flatMap { it.archiveFile })
}

// read changelog
val changelogFile = rootProject.file("CHANGELOG.md")
val changelogText =
    (if (changelogFile.exists()) changelogFile.readText() else "").ifBlank {
      "Unreleased development build"
    }

if (supportsUnitTesting) {
    tasks.register<JavaExec>("benchmarkRle") {
        group = "verification"
        description = "Runs the VolumetricRLE JMH benchmark on the test runtime classpath."
        classpath = sourceSets["test"].runtimeClasspath
        mainClass.set("org.openjdk.jmh.Main")
        args(
            "io.github.leawind.gitparcel.common.utils.algorithms.VolumetricRLEBenchmark",
            "-foe", "true")
    }
}

afterEvaluate {
    publishMods {
        dryRun.set(System.getenv("DRY_RUN") != "false")
        displayName.set("$modVersionString for $mcVersion $loader")
        file = modstitch.finalJarTask.flatMap { it.archiveFile }
        additionalFiles.from(tasks.named("sourcesJar"))
        changelog.set(changelogText)

        type = if (modVersionString.contains("beta", true)) {
            BETA
        } else if (modVersionString.contains("alpha", true)) {
            ALPHA
        } else {
            STABLE
        }

        modLoaders.add(loader)
        modrinth {
            accessToken = System.getenv("MODRINTH_TOKEN")
            projectId = System.getenv("MODRINTH_ID")
            minecraftVersions.add(mcVersion)
            if (isFabric) {
                optional { slug.set("modmenu") }
            }
        }
        curseforge {
            accessToken = System.getenv("CURSEFORGE_TOKEN")
            projectId = System.getenv("CURSEFORGE_ID")
            minecraftVersions.add(mcVersion)
            clientRequired = true
            serverRequired = true
            if (isFabric) {
                optional { slug.set("modmenu") }
            }
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = modIdValue
            version = "$modVersionString+$loader-$mcVersion"
            from(components["java"])
            pom {
                name.set(modNameValue)
                description.set(modDescriptionValue)
            }
        }
    }

    repositories {
        mavenLocal()
    }
}
// endregion

// region Helpers
fun requiredProp(property: String): String =
    findProperty(property)?.toString()?.takeIf { it.isNotBlank() }
        ?: error("Required Gradle property '$property' is missing or blank")
// endregion
