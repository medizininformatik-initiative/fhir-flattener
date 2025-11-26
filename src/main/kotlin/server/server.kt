package server

import au.csiro.pathling.library.PathlingContext
import io.ktor.http.*
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.application.install
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.post
import io.ktor.utils.io.core.Input
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.serialization.json.*
import org.apache.spark.sql.Dataset
import org.apache.spark.sql.Row
import org.apache.spark.sql.SaveMode
import java.time.LocalDateTime
import toFhirView
import viewdefinition.Parameter
import viewdefinition.Parameters
import viewdefinition.ViewDefinition
import java.io.*
import java.util.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val contentType = ContentType("application", "fhir+json")

private val capabilityStatement = buildJsonObject {
    put("resourceType", "CapabilityStatement")
    put("status", "active")
    put("date", LocalDateTime.now().toString())
    put("publisher", "Johannes Oehm")
    put("kind", "instance")
    put("fhirVersion", "4.0.1")
    put("format", JsonArray(listOf(JsonPrimitive(contentType.toString()))))
    put(
        "rest",
        JsonArray(listOf(buildJsonObject {
            put("mode", "server")
            put(
                "operation", JsonArray(
                    listOf(
                        buildJsonObject {
                            put("name", "\$export")
                            put("definition", "http://sql-on-fhir.org/OperationDefinition/\$export")
                        },
                        buildJsonObject {
                            put("name", "\$validate")
                            put("definition", "http://sql-on-fhir.org/OperationDefinition/\$validate")
                        },
                        buildJsonObject {
                            put("name", "\$run")
                            put("definition", "http://sql-on-fhir.org/OperationDefinition/\$run")
                        }
                    )))
        }))
    )
}

class FhirException(val severity: FhirSeverity, val code: String, val exception: Throwable? = null) :
    Throwable(exception) {
    fun toOperationOutcome(): JsonObject {
        return buildJsonObject {
            put("resourceType", "OperationOutcome")
            putJsonArray("issue") {
                addJsonObject {
                    put("severity", severity.toString().lowercase())
                    put("code", code)
                    put("diagnostics", exception.toString() + "\n\n" + exception?.stackTraceToString())
                }
            }
        }
    }
}

enum class FhirSeverity { FATAL, ERROR, WARNING, INFORMATION, DEBUG }

@OptIn(ExperimentalUuidApi::class)
fun main() {
    embeddedServer(Netty, port = System.getenv("PORT")?.toInt() ?: 8000) {
        monitor.subscribe(ApplicationStopping) {
            val tempDir = File("output/")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    file.deleteRecursively()
                }
            }
        }

        install(StatusPages) {
            exception<FhirException> { call, cause ->
                call.respondText(contentType, HttpStatusCode.InternalServerError) {
                    cause.toOperationOutcome().toString()
                }
            }

            exception<Throwable> { call, cause ->
                call.respondText(contentType, HttpStatusCode.InternalServerError) {
                    buildJsonObject {
                        put("resourceType", "OperationOutcome")
                        putJsonArray("issue") {
                            addJsonObject {
                                put("severity", "error")
                                put("code", "processing")
                                put("diagnostics", cause.toString() + "\n\n" + cause.stackTraceToString())
                            }
                        }
                    }.toString()
                }
            }

            status(HttpStatusCode.NotFound) {
                call.respondText(contentType, HttpStatusCode.InternalServerError) {
                    buildJsonObject {
                        put("resourceType", "OperationOutcome")
                        putJsonArray("issue") {
                            addJsonObject {
                                put("severity", "error")
                                put("code", "not-found")
                                put("diagnostics", "there is no route defined for '${call.request.uri}'!")
                            }
                        }
                    }.toString()
                }
            }
        }

        routing {
            get("/") {
                call.respondText("Hello from fhir-flattener! Please check GET /fhir/metadata or POST /fhir/ViewDefinition/\$run")
            }
            route("/fhir/") {
                get("/metadata") {
                    call.respondText(capabilityStatement.toString(), contentType, HttpStatusCode.OK)
                }
                get("/ViewDefinition/{id}/\$run") {
                    val outputFormat = getOutputFormat(call.request.queryParameters["_format"], call.request.accept())
                    call.respondText("Not Implemented!")
                }
                post("/ViewDefinition/\$run") {
                    val parameters = Json.decodeFromString<Parameters>(call.receiveText())

                    val outputFormat = getOutputFormat(parameters["_format"]?.valueCode, call.request.accept())

                    val viewDefinition = getViewDefinitionFromParameters(parameters)

                    val resource = parameters.getAsList("resources").mapIndexed { idx, it ->
                        it.resource ?: error("No resource provided at $idx!")
                    }
                    require(resource.isNotEmpty()) { "Resources must be provided directly" }

                    require(parameters["source"] == null) { "Providing source FHIR server URL directly not (yet) supported" }
                    require(parameters["patient"] == null) { "Providing patient filter not (yet) supported" }
                    require(parameters["group"] == null) { "Providing group filter not (yet) supported" }
                    require(parameters["_since"] == null) { "Providing _since filter not (yet) supported" }
                    require(parameters["_limit"] == null) { "Providing _limit filter not (yet) supported" }


                    val uuid = Uuid.random().toString()
                    val inputStream =
                        executeViewDefinition(viewDefinition, outputFormat, resource, uuid)

                    call.respond(inputStream)

                    File("output/$uuid").deleteRecursively()
                    if (File("input/$uuid").exists()) {
                        File("input/$uuid").deleteRecursively()
                    }

                }
                post("/ViewDefinition/\$export") {
                    require(call.request.headers["Prefer"] == "respond-async") { "'Prefer:' header must be 'respond-async'!" }
                    val parameters = Json.decodeFromString<Parameters>(call.receiveText())
                    val viewDefinitions = parameters.getAsList("view")


                    val outputFormat = getOutputFormat(parameters["_format"]?.valueCode, call.request.accept())
                    val clientTrackingId = parameters["clientTrackingId"]?.valueString

                    require(parameters["source"] == null) { "Providing source FHIR server URL directly not (yet) supported" }
                    require(parameters["patient"] == null) { "Providing patient filter not (yet) supported" }
                    require(parameters["group"] == null) { "Providing group filter not (yet) supported" }
                    require(parameters["_since"] == null) { "Providing _since filter not (yet) supported" }
                    require(parameters["_limit"] == null) { "Providing _limit filter not (yet) supported" }


                    val uuid = Uuid.random().toString()

                    val job = async(Job()) {
                        for ((idx, parameter) in viewDefinitions.withIndex()) {
                            val name = parameter["name"]?.valueString
                            val viewReference = parameter["viewReference"]?.valueReference
                            val viewResource: ViewDefinition? =
                                parameter["viewResource"]?.resource?.let { Json.decodeFromJsonElement(it) }
                            if (viewResource != null && viewReference != null) {
                                error("Neither viewResource nor viewReference provided for view '$name' (index=$idx) in input parameters")
                            }
                            executeViewDefinition(viewResource!!, outputFormat, TODO(), uuid)
                        }

                    }

                    val location = "__async-status/${uuid}"
                    call.response.headers.append(HttpHeaders.ContentLocation, location)
                    call.respondText(
                        Json.encodeToString(
                            Parameters(
                                resourceType = "Parameters",
                                parameter = listOf(
                                    Parameter(name = "exportId", valueString = uuid),
                                    Parameter(name = "clientTrackingId", valueString = clientTrackingId),
                                    Parameter(name = "location", valueString = location),
                                    Parameter(name = "status", valueCode = "in-progress"),
                                ),
                            )
                        ), contentType, HttpStatusCode.Accepted
                    )


                    Parameters(
                        resourceType = "Parameters",
                        parameter = listOf(
                            Parameter(
                                name = "output", part = listOf(
                                    Parameter(
                                        "name",
                                        valueString = "client_provided_name_or_resource_name_or_generated_id"
                                    ),
                                    Parameter("location", valueString = "/export/uuid/sample_name.part1.parquet"),
                                )
                            ),
                        )
                    )

                }

                delete("__async-status/{uuid}") {
                    val uuid = call.request.pathVariables["uuid"] ?: error("No uuid provided!")


                }

            }
        }
    }.start(wait = true)
}

private fun getViewDefinitionFromParameters(parameters: Parameters): ViewDefinition {
    require(parameters["viewReference"] == null) { "viewReference is not supported" }

    val viewDefJson = parameters["viewDefinition"]?.resource ?: error("No ViewDefinition resource provided!")
    val viewDefinition = Json.decodeFromJsonElement<ViewDefinition>(viewDefJson)
    return viewDefinition
}

private fun Route.post(string: String, function: () -> Unit) {}

private fun getOutputFormat(_format: String?, acceptHeader: String?): OutputFormat {
    if (_format != null) {
        return when (_format) {
            "json" -> OutputFormat.Json
            "ndjson" -> OutputFormat.Ndjson
            "csv" -> OutputFormat.Csv
            "parquet" -> OutputFormat.Parquet
            else -> error("Unsupported _format '$_format'!")
        }
    }
    if (acceptHeader != null) {
        return when (acceptHeader) {
            "application/json" -> OutputFormat.Json
            "application/x-ndjson" -> OutputFormat.Ndjson
            "text/csv" -> OutputFormat.Csv
            "application/octet-stream" -> OutputFormat.Parquet
            else -> error("Unsupported accept header '$acceptHeader'!")
        }
    }

    return OutputFormat.Csv
}

enum class OutputFormat { Json, Ndjson, Csv, Parquet }


@OptIn(ExperimentalUuidApi::class)
private fun executeViewDefinition(
    viewDefinition: ViewDefinition,
    outputFormat: OutputFormat,
    resources: List<JsonObject>,
    uuid: String
): InputStream {
    val pc = PathlingContext.create()

    writeResourcesAsNdjson(uuid, viewDefinition, resources)
    val data = pc.read().ndjson("input/$uuid")

    val result: Dataset<Row?> = data.view(viewDefinition.toFhirView()).execute()

    val outputPath = "output/$uuid"
    val resultStream = when (outputFormat) {
        OutputFormat.Json -> {
            result.write().mode(SaveMode.Overwrite).json(outputPath)
            val result: MutableList<InputStream> = File(outputPath).listFiles { _, name ->
                name.endsWith(".json", ignoreCase = true)
            }.map { file -> NewlineToCommaInputStream(file.inputStream()) }.toMutableList()
            result[result.size - 1] = TrimLastByteInputStream(result[result.size - 1])
            SequenceInputStream(listOf(char2InputStream('[')) + result + listOf(char2InputStream(']')))
        }

        OutputFormat.Ndjson -> {
            result.write().mode(SaveMode.Overwrite).json(outputPath)
            val result = File(outputPath).listFiles { _, name ->
                name.endsWith(".json", ignoreCase = true)
            }.map { file -> file.inputStream() }
            SequenceInputStream(result)
        }

        OutputFormat.Csv -> {
            result.write().mode(SaveMode.Overwrite).csv(outputPath)

            val result = File(outputPath).listFiles { _, name ->
                name.endsWith(".csv", ignoreCase = true)
            }.map { file -> file.inputStream() }

            SequenceInputStream(result)
        }

        OutputFormat.Parquet -> {
            result.repartition(1).write().mode(SaveMode.Overwrite).parquet("$outputPath/result.parquet")
            val resultFile = File(outputPath).listFiles { _, name -> name.endsWith(".parquet") }.single()
            resultFile.inputStream()
        }
    }



    return resultStream

}

private fun char2InputStream(char: Char) = ByteArrayInputStream(byteArrayOf(char.code.toByte()))

private fun SequenceInputStream(list: List<InputStream>) = SequenceInputStream(Collections.enumeration(list))

private fun writeResourcesAsNdjson(uuid: String, viewDefinition: ViewDefinition, resources: List<JsonObject>) {
    File("input/$uuid").mkdirs()

    //prevent pathwalking
    require(viewDefinition.resource matches "[A-Za-z]+".toRegex()) { "invalid ViewDefinition.resource ${viewDefinition.resource}" }

    File("input/$uuid/${viewDefinition.resource}.ndjson").writer().use { writer ->
        for (resource in resources) {
            writer.write(resource.toString())
            writer.write("\n")
        }
    }
}


