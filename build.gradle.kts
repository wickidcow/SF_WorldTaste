import org.gradle.api.tasks.compile.JavaCompile

plugins {
    java
}

group = "com.haiman233"
version = "1.0.10"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val slimefunJar = providers.gradleProperty("slimefunJar")
    .orElse("libs/Slimefun-2025.11-release.jar")

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files(slimefunJar.map { file(it) }))
    compileOnly(files("libs/JustEnoughGuide.jar"))
}

// Match Slimefun Legacy: compile with the modern JDK/Paper API while emitting
// Java 21 bytecode so the addon itself remains Java 21+ compatible.
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

val contentYaml = listOf(
    "groups.yml", "recipe_types.yml", "items.yml", "foods.yml", "machines.yml",
    "recipe_machines.yml", "mb_machines.yml", "linked_recipe_machines.yml",
    "template_machines.yml", "workbenches.yml", "mob_drops.yml", "geo_resources.yml", "menus.yml"
)

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
    from(rootProject.projectDir.resolve("content")) {
        include(contentYaml)
        into("")
    }
}

tasks.jar {
    archiveFileName.set("SF_WorldTaste${project.version}.jar")
}
