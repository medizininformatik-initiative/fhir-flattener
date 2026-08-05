import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import server.application
import viewdefinition.Parameter
import viewdefinition.Parameters
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFlattening {

    fun testFlatteningInternalWithFile(filename: String, expected: String) {
        val bodyStr = TestFlattening::class.java.getResource(filename)!!.readText()
        testFlatteningInternalWithString(bodyStr, expected)
    }

    fun testFlatteningInternalWithString(bodyStr: String, expected: String) = testApplication {
        application {
            application()()
        }
        client = createClient {}

        println("bodyStr = '$bodyStr'")

        val response = client.post("/fhir/ViewDefinition/\$run") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.CSV)
            setBody(bodyStr)
        }
        assertEquals(expected, response.bodyAsText().trim().lines().joinToString("\n"))
    }

    @Test
    fun testFlatteningWithNestedSelect() {
        testFlatteningInternalWithFile("nested-sample.json", "10178")
    }

    @Test
    fun testFlatteningWithMultipleForeaches() {
        testFlatteningInternalWithFile(
            "multiple-foreach-sample.json",
            """Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JonDoe,Location/1
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JaneDoe,Location/1
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JonDoe,Location/2
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JaneDoe,Location/2
Encounter/example,finished,Patient/1234,Organization/UKM,,,,http://fhir.de/CodeSystem/Kontaktebene,abteilungskontakt,Practitioner/JackJohnson,
Encounter/example,finished,Patient/1234,Organization/UKM,,,,http://fhir.de/CodeSystem/Kontaktebene,abteilungskontakt,Practitioner/JohnJackson,"""
        )
    }

    @Test
    fun assertNoFormatResultsinError() = testApplication {
        application {
            application()()
        }
        client = createClient {}

        val listMime = listOf(
            ContentType.Text.CSV,
            ContentType.Application.Json,
            ContentType("application", "x-ndjson"),
            ContentType.Application.OctetStream
        )


        val bodyStr = TestFlattening::class.java.getResource("nested-sample.json")!!.readText()
        for (mimeType in listMime) {
            val response = client.post("/fhir/ViewDefinition/\$run") {
                contentType(ContentType.Application.Json)
                accept(mimeType)
                setBody(bodyStr)
            }
            assertEquals(response.status, HttpStatusCode.OK)
        }
    }


    private fun getInputResources(filename: String) = TestFlattening::class.java.getResource(filename)!!.readText()
        .trim().lines().map { Json.decodeFromString<JsonObject>(it) }

    private fun getViewDefinition(filename: String) = TestFlattening::class.java.getResource(filename)!!.readText()
        .let { Json.decodeFromString<JsonObject>(it) }

    private fun createParametersResource(viewDefinition: JsonObject, resources: List<JsonObject>) =
        Parameters(
            resourceType = "Parameters",
            parameter = listOf(
                Parameter(name = "viewDefinition", resource = viewDefinition)
            ) + resources.map { Parameter(name = "resources", resource = it) }
        )


    @Test
    fun testQuantityAndUriExtensions() {
        val conditions = getInputResources("input/Condition.ndjson")
        val condViewDef = getViewDefinition("cond-view-def.json")
        val condParams = createParametersResource(condViewDef, conditions)

        testFlatteningInternalWithString(
            Json.encodeToString(condParams), """cond-1,summary-system-A,summary-code-1
cond-1,summary-system-D,summary-code-3-1
cond-1,summary-system-E,summary-code-3-2"""
        )

        val medications = getInputResources("input/Medication.ndjson")
        val medViewDef = getViewDefinition("med-view-def.json")
        val medParams = createParametersResource(medViewDef, medications)

        testFlatteningInternalWithString(
            Json.encodeToString(medParams), ",\n,\n,#ing_1\n,#ing_2"
        )


        val observations = getInputResources("input/Observation.ndjson")
        val labViewDef = getViewDefinition("lab-view-def.json")
        val labParams = createParametersResource(labViewDef, observations)

        testFlatteningInternalWithString(Json.encodeToString(labParams), "17")


        val patients = getInputResources("input/Patient.ndjson")
        val patViewDef = getViewDefinition("pat-view-def.json")
        val patParams = Json.encodeToString(createParametersResource(patViewDef, patients))

        testFlatteningInternalWithString(
            patParams,
            "mii-exa-person-patient-full,2024-02-22,,2024-02-22,2024-02-22,10178"
        )


    }


    @Test
    fun testFlatteningWithRfc4180Escaping() {
        testFlatteningInternalWithFile(
            "hostile-text.json",
            "\"Name is \"\"real\"\"\",\"a, b\",back\\slash,,\"\"\"\""
        )
    }

}