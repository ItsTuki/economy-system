plugins {
    java
    alias(libs.plugins.run.paper)
}

group = "cl.auryx.economy"
version = "2.0.0"
description = "Monedas físicas incrafteables para economías de servidor Paper."

val minecraftVersion: String by project

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    mavenCentral()
}

dependencies {
    compileOnly(libs.paper.api)
}

java {
    // Minecraft 26.x / Paper 26.x requieren Java 25
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release = 25
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
    }

    processResources {
        val props = mapOf("version" to project.version.toString())
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveBaseName = "EconomySystem"
    }

    runServer {
        minecraftVersion(minecraftVersion)
        jvmArgs("-Dcom.mojang.eula.agree=true")
    }
}
