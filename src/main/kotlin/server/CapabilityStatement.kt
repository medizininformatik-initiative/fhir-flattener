package server

import io.ktor.http.ContentType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.time.LocalDateTime
import java.util.Properties

internal const val SOFTWARE_NAME = "fhir-flattener"

internal val softwareVersion: String = loadSoftwareVersion()

private fun loadSoftwareVersion(): String {
    val resource = "/version.properties"
    val stream = object {}.javaClass.getResourceAsStream(resource)
        ?: throw IllegalStateException("classpath resource $resource is missing; the build must generate it")
    val version = stream.use { Properties().apply { load(it) } }.getProperty("version")
    return version ?: throw IllegalStateException("key 'version' is missing in classpath resource $resource")
}

internal val contentType = ContentType("application", "fhir+json")

internal val capabilityStatement = buildJsonObject {
    put("resourceType", "CapabilityStatement")
    put("status", "active")
    put("date", LocalDateTime.now().toString())
    put("publisher", "Johannes Oehm")
    put("kind", "instance")
    putJsonObject("software") {
        put("name", SOFTWARE_NAME)
        put("version", softwareVersion)
    }
    put("fhirVersion", "4.0.1")
    put("format", JsonArray(listOf(JsonPrimitive(contentType.toString()))))
    put(
        "rest",
        JsonArray(listOf(buildJsonObject {
            put("mode", "server")
            putJsonArray("resource") {
                addJsonObject {
                    put("type", "ViewDefinition")
                    put(
                        "operation", JsonArray(
                            listOf(
                                buildJsonObject {
                                    put("name", "\$run")
                                    put("definition", "http://sql-on-fhir.org/OperationDefinition/\$run")
                                }
                            )))
                }
            }

        }))
    )
}
