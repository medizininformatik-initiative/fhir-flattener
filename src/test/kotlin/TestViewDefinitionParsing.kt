import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import viewdefinition.ViewDefinition
import kotlin.test.Test

class TestViewDefinitionParsing {

    /**
     * These are the example resources from the SQLonFHIR-Spec
     */
    @Test
    fun testParsing() {
        testParsingInternal("BloodPressuresViewDefinition.json")
        testParsingInternal("PatientAddressesViewDefinition.json")
        testParsingInternal("PatientAndContactAddressUnionViewDefinition.json")
        testParsingInternal("PatientDemographicsViewDefinition.json")
    }

    private fun testParsingInternal(filename: String) {
        //Assert no parsing error occurs
        Json.decodeFromStream<ViewDefinition>(TestViewDefinitionParsing::class.java.getResourceAsStream(filename)!!)

    }
}