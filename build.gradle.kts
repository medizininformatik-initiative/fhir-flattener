import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.2.2"
    kotlin("plugin.serialization") version "2.2.21"
}

group = "ms.uni.imi.medic"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "3.3.1"
val pathlingVersion = "8.1.0"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")

    testImplementation(kotlin("test"))

    implementation("au.csiro.pathling:library-runtime:$pathlingVersion")
    implementation("au.csiro.pathling:fhirpath:$pathlingVersion")
    implementation("au.csiro.pathling:terminology:$pathlingVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

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
