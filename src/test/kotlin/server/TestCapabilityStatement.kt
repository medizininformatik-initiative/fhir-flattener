package server

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestCapabilityStatement {

    private fun getMetadata(check: (JsonObject) -> Unit) = testApplication {
        application {
            application()()
        }
        val response = client.get("/fhir/metadata")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ContentType("application", "fhir+json"), response.contentType()?.withoutParameters())
        check(Json.parseToJsonElement(response.bodyAsText()).jsonObject)
    }

    @Test
    fun metadataNamesTheFlattener() = getMetadata { statement ->
        assertEquals("CapabilityStatement", statement["resourceType"]!!.jsonPrimitive.content)
        val software = statement["software"]!!.jsonObject
        assertEquals("fhir-flattener", software["name"]!!.jsonPrimitive.content)
    }

    @Test
    fun metadataReportsTheReleaseVersionWithoutLeadingV() = getMetadata { statement ->
        val version = statement["software"]!!.jsonObject["version"]!!.jsonPrimitive.content
        assertTrue(
            Regex("""^\d+\.\d+\.\d+(-[0-9A-Za-z.]+)?$""").matches(version),
            "unexpected version: $version"
        )
    }

    @Test
    fun metadataKeepsTheRunOperation() = getMetadata { statement ->
        val operations = statement["rest"]!!.jsonArray[0].jsonObject["resource"]!!
            .jsonArray[0].jsonObject["operation"]!!.jsonArray.map { it.jsonObject }
        assertTrue(operations.any {
            it["name"]!!.jsonPrimitive.content == "\$run" &&
                it["definition"]!!.jsonPrimitive.content == "http://sql-on-fhir.org/OperationDefinition/\$run"
        })
    }
}
