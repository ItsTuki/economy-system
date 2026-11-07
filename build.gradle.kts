import org.gradle.jvm.tasks.Jar

private val targetMinecraftVersion = "1.21.11"
private val runtimeServerVersion = "1.21.11"
private val paperApiVersion = "$targetMinecraftVersion-R0.1-SNAPSHOT"

plugins {
    `java`
    id("xyz.jpenilla.run-paper") version "2.3.0"
}

group = "com.mc.economy"
version = "1.0.1"

repositories {
    maven("https://artifactory.papermc.io/artifactory/universe/") {
        name = "papermc-universe"
        metadataSources {
            mavenPom()
            artifact()
        }
        content {
            includeGroup("io.papermc.paper")
            excludeModule("io.papermc.paper", "dev-bundle")
        }
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        content {
            excludeGroup("io.papermc.paper")
        }
    }
    mavenCentral()
}

dependencies {
    compileOnly(libs.paper.api)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.named<Jar>("jar") {
    archiveBaseName.set("EconomySystem")
}

tasks.runServer {
    minecraftVersion(runtimeServerVersion)
}
