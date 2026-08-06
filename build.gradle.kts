import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.4.1"
    kotlin("plugin.serialization") version "2.4.10"
}

group = "ms.uni.imi.medic"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.5.2"
val pathlingVersion = "9.8.0"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

    implementation("au.csiro.pathling:library-runtime:$pathlingVersion")
    implementation("au.csiro.pathling:fhirpath:$pathlingVersion")
    implementation("au.csiro.pathling:terminology:$pathlingVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")


}


tasks {
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes("Main-Class" to "server.ServerKt")
        }
    }
    withType<ShadowJar> {
        isZip64 = true
        archiveFileName.set("fhir-flattener.jar")
    }
}
