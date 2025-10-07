import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.gradleup.shadow") version "8.3.8"
    kotlin("plugin.serialization") version "2.2.20"
}

group = "ms.uni.imi.medic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.3.0")
    implementation("io.ktor:ktor-server-host-common:3.3.0")
    implementation("io.ktor:ktor-server-status-pages:3.3.0")
    implementation("io.ktor:ktor-server-core:3.3.0")
    testImplementation(kotlin("test"))

    implementation("au.csiro.pathling:library-runtime:8.0.2")
    implementation("au.csiro.pathling:fhirpath:8.0.2")
    implementation("au.csiro.pathling:terminology:8.0.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    implementation("io.ktor:ktor-server:3.3.0")
    implementation("io.ktor:ktor-server-netty:3.3.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}

tasks {
    jar {
        manifest {
            attributes("Main-Class" to "server.ServerKt")
        }
    }
}
tasks.withType<ShadowJar> {
    isZip64 = true
    archiveFileName.set("pathling-goes-mii.jar")
}
