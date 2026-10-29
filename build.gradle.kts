import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    kotlin("plugin.serialization") version "2.4.21"
    jacoco
}

group = "ms.uni.imi.medic"
version = "v0.1.0-alpha.10"

kotlin {
    jvmToolchain(21)
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.6.0"
val pathlingVersion = "10.0.0"

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

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    // streaming parser for the $run body; same version as the jackson-bom that Pathling brings in
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.4")


}


tasks {
    test {
        useJUnitPlatform()
        finalizedBy(jacocoTestReport)
    }
    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
        }
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
