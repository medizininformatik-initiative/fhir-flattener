package server

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class TestRunParameters {

    private fun read(body: String): Pair<RunParameters, List<JsonObject>> {
        val resources = mutableListOf<JsonObject>()
        val result = readRunParameters(StringReader(body)) { resources += it }
        return result to resources
    }

    @Test
    fun resourcesAreHandedOverAndOtherParametersKept() {
        val (result, resources) = read(
            """
            {"resourceType": "Parameters", "parameter": [
              {"name": "viewDefinition", "resource": {"resourceType": "ViewDefinition", "resource": "Patient"}},
              {"name": "resources", "resource": {"resourceType": "Patient", "id": "p1"}},
              {"name": "_format", "valueCode": "csv"},
              {"name": "resources", "resource": {"resourceType": "Patient", "id": "p2"}}
            ]}
            """
        )

        assertEquals(
            listOf("""{"resourceType":"Patient","id":"p1"}""", """{"resourceType":"Patient","id":"p2"}"""),
            resources.map { it.toString() })
        assertEquals(2, result.resourceCount)
        assertEquals(listOf("viewDefinition", "_format"), result.parameters.parameter.map { it.name })
        assertEquals("csv", result.parameters["_format"]?.valueCode)
    }

    @Test
    fun nameAfterResourceAndLiteralsAreKeptAsWritten() {
        val resource = """{"resourceType":"Observation","valueQuantity":{"value":1.10,"unit":"Ω"},""" +
                """"n":12345678901234567890,"e":1e-7,"s":"a\"b\\c\n","b":[true,false,null]}"""
        val (_, resources) = read(
            """{"parameter": [{"resource": $resource, "name": "resources"}], "resourceType": "Parameters"}"""
        )

        assertEquals(listOf(Json.decodeFromString<JsonObject>(resource).toString()), resources.map { it.toString() })
        assertEquals(""""value":1.10""", Regex(""""value":[0-9.]+""").find(resources.single().toString())?.value)
    }

    @Test
    fun resourcesParameterWithoutResourceIsAnError() {
        val error = assertFailsWith<IllegalStateException> {
            read(
                """
                {"resourceType": "Parameters", "parameter": [
                  {"name": "resources", "resource": {"resourceType": "Patient", "id": "p1"}},
                  {"name": "viewDefinition", "resource": {"resourceType": "ViewDefinition"}},
                  {"name": "resources", "valueString": "p2"}
                ]}
                """
            )
        }
        assertEquals("No resource provided at 1!", error.message)
    }

    @Test
    fun otherResourceTypeIsAnErrorBeforeAnyResourceIsHandedOver() {
        val resources = mutableListOf<JsonObject>()
        val error = assertFailsWith<IllegalStateException> {
            readRunParameters(
                StringReader(
                    """
                    {"resourceType": "Bundle", "parameter": [
                      {"name": "resources", "resource": {"resourceType": "Patient", "id": "p1"}}
                    ]}
                    """
                )
            ) { resources += it }
        }
        assertEquals("Expected resourceType Parameters, found Bundle", error.message)
        assertEquals(emptyList(), resources)
    }

    @Test
    fun resourceTypeAfterParameterIsChecked() {
        val error = assertFailsWith<IllegalStateException> {
            read("""{"parameter": [], "resourceType": "Bundle"}""")
        }
        assertEquals("Expected resourceType Parameters, found Bundle", error.message)
        assertEquals(0, read("""{"parameter": [], "resourceType": "Parameters"}""").first.resourceCount)
    }

    @Test
    fun resourceTypeThatIsNotAStringIsAnError() {
        val error = assertFailsWith<IllegalStateException> {
            read("""{"resourceType": {"value": "Parameters"}, "parameter": []}""")
        }
        assertEquals("Expected resourceType Parameters, found START_OBJECT", error.message)
    }

    @Test
    fun missingResourceTypeIsAnError() {
        val error = assertFailsWith<IllegalStateException> { read("""{"parameter": []}""") }
        assertEquals("No resourceType provided, expected Parameters", error.message)
    }

    @Test
    fun bodyThatIsNotAnObjectIsAnError() {
        assertFails { read("""[{"name": "_format", "valueCode": "csv"}]""") }
    }

    @Test
    fun trailingContentAfterParametersIsAnError() {
        assertFails { read("""{"resourceType": "Parameters", "parameter": []} {}""") }
    }
}
