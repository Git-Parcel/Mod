plugins {
    id("multiloader-common")
    id("com.gradleup.shadow")
}
val props = project.properties

configurations {
    register("commonJava") {
        isCanBeResolved = true
    }
    register("commonResources") {
        isCanBeResolved = true
    }
}

dependencies {
    compileOnly(project(":common")) {
        capabilities {
            requireCapability("${props["group"]}:${props["mod_id"]}")
        }
        val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
        attributes {
            attribute(loaderAttribute, "common")
        }
    }

    add("commonJava", project(":common", configuration = "commonJava"))
    add("commonResources", project(":common", configuration = "commonResources"))

    compileOnly("org.jspecify:jspecify:1.0.0")

    shadow("com.github.Leawind:inventory-java:${props["leawind_inventory_version"]}")
    shadow("org.eclipse.jgit:org.eclipse.jgit:${props["jgit_version"]}") {
        // conflicts with NeoForm's strictly 1.19.0
        exclude(group = "commons-codec", module = "commons-codec")
        // already provided by Minecraft
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
    shadow("dev.dirs:directories:${props["directories_version"]}")
    shadow("com.github.ben-manes.caffeine:caffeine:${props["caffeine_version"]}") {
        // annotations
        exclude(group = "com.google.errorprone")
        exclude(group = "org.jspecify", module = "jspecify")
    }
}

repositories {
    maven { url = uri("https://jitpack.io") }
}

// Wire shadow configuration to compile and runtime classpaths
configurations.compileClasspath.get().extendsFrom(configurations.getByName("shadow"))
configurations.runtimeClasspath.get().extendsFrom(configurations.getByName("shadow"))

tasks.named<JavaCompile>("compileJava") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(configurations.getByName("commonJava"))
    source(configurations.getByName("commonJava"))

    (options as? org.gradle.external.javadoc.StandardJavadocDocletOptions)?.addStringOption(
        "Xdoclint:-missing",
        "-quiet"
    )
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(configurations.getByName("commonJava"))
    from(configurations.getByName("commonJava"))
    dependsOn(configurations.getByName("commonResources"))
    from(configurations.getByName("commonResources"))
}

tasks.named("shadowJar", com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar::class.java) {
    archiveClassifier = ""
    configurations = listOf(project.configurations.getByName("shadow"))
    minimize()

    // com.github.Leawind:inventory-java
    relocate("io.github.leawind.inventory", "io.github.leawind.gitparcel.lib.inventory")

    // org.eclipse.jgit:org.eclipse.jgit
    relocate("org.eclipse.jgit", "io.github.leawind.gitparcel.lib.jgit")
    relocate("com.googlecode.javaewah", "io.github.leawind.gitparcel.lib.javaewah")
    exclude("about.html")
    exclude("OSGI-INF/**")
    exclude("META-INF/maven/org.eclipse.jgit/**")
    exclude("META-INF/maven/com.googlecode.javaewah/**")

    // dev.dirs:directories
    relocate("dev.dirs", "io.github.leawind.gitparcel.lib.dirs")
    exclude("META-INF/native-image/**")

    // com.github.ben-manes.caffeine:caffeine
    relocate("com.github.benmanes.caffeine", "io.github.leawind.gitparcel.lib.caffeine")
}

tasks.named("assemble") {
    dependsOn(tasks.named("shadowJar"))
}
