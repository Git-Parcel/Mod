plugins {
    id("multiloader-common")
    id("net.neoforged.moddev")
    id("me.champeau.jmh") version "0.7.2"
}

neoForge {
    neoFormVersion = project.property("neo_form_version") as String
    // Automatically enable AccessTransformers if the file exists
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
    parchment {
        minecraftVersion = project.property("parchment_minecraft") as String
        mappingsVersion = project.property("parchment_version") as String
    }

    // So we can access vanilla classes in test
    addModdingDependenciesTo(sourceSets.test.get())
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
    // fabric and neoforge both bundle mixinextras, so it is safe to use it in common
    compileOnly("io.github.llamalad7:mixinextras-common:0.3.5")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.3.5")

    // Unit testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // JMH for performance testing
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")

    implementation("com.github.Leawind:inventory-java:${project.property("leawind_inventory_version")}")
    implementation("org.eclipse.jgit:org.eclipse.jgit:${project.property("jgit_version")}") {
        // Exclude transitive dependencies that are already provided by the Minecraft environment
        exclude(group = "commons-codec", module = "commons-codec") // conflicts with NeoForm's strictly 1.19.0
        exclude(group = "org.slf4j", module = "slf4j-api") // already provided by Minecraft
    }
    implementation("dev.dirs:directories:${project.property("directories_version")}")

    implementation("com.github.ben-manes.caffeine:caffeine:${project.property("caffeine_version")}")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:-missing", "-quiet")
}

val commonJava: Configuration? by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}
val commonResources: Configuration by configurations.creating {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    add("commonJava", sourceSets.main.get().java.sourceDirectories.singleFile)
    add("commonResources", sourceSets.main.get().resources.sourceDirectories.singleFile)
}

// Implement mcgradleconventions loader attribute
val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
listOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements").forEach { variant ->
    configurations.named(variant) {
        attributes {
            attribute(loaderAttribute, "common")
        }
    }
}
sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "common")
            }
        }
    }
}
