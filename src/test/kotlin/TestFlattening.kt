import io.ktor.client.request.accept
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import server.application
import kotlin.test.Test
import kotlin.test.assertEquals

class TestFlattening {

    fun testFlatteningInternal(filename: String, expected: String) = testApplication {
        application {
            application()()
        }
        client = createClient {}

        val bodyStr = TestFlattening::class.java.getResource(filename)!!.readText()
        val response = client.post("/fhir/ViewDefinition/\$run") {
            contentType(ContentType.Application.Json)
            accept(ContentType.Text.CSV)
            setBody(bodyStr)
        }
        assertEquals(expected, response.bodyAsText().trim().lines().joinToString("\n"))
    }

    @Test
    fun testFlatteningWithNestedSelect() {
        testFlatteningInternal("nested-sample.json", "10178")
    }

    @Test
    fun testFlatteningWithMultipleForeaches() {
        testFlatteningInternal(
            "multiple-foreach-sample.json",
            """Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JonDoe,Location/1
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JaneDoe,Location/1
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JonDoe,Location/2
Encounter/example,in-progress,Patient/example,Organization/UKM,2015-02-07T13:28:17-05:00,2017-01-01T00:00:00.000Z,EpisodeOfCare/example,http://fhir.de/CodeSystem/Kontaktebene,einrichtungskontakt,Practitioner/JaneDoe,Location/2
Encounter/example,finished,Patient/1234,Organization/UKM,,,,http://fhir.de/CodeSystem/Kontaktebene,abteilungskontakt,Practitioner/JackJohnson,
Encounter/example,finished,Patient/1234,Organization/UKM,,,,http://fhir.de/CodeSystem/Kontaktebene,abteilungskontakt,Practitioner/JohnJackson,"""
        )
    }
}