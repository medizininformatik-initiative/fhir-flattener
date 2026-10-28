package server

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.StreamReadConstraints
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import viewdefinition.Parameter
import viewdefinition.Parameters
import java.io.Reader
import java.io.StringWriter

/** The parameters of a `$run` request without the `resources`, which went to the callback. */
data class RunParameters(val parameters: Parameters, val resourceCount: Int)

// Jackson limits strings to 20M chars by default; base64 attachments in resources may be larger.
private val jsonFactory = JsonFactory.builder()
    .streamReadConstraints(StreamReadConstraints.builder().maxStringLength(Int.MAX_VALUE).build())
    .build()

/**
 * Reads a FHIR Parameters resource one `parameter` element at a time. The resource of every
 * `resources` parameter is handed to [onResource] and dropped, so memory depends on the largest
 * single parameter element instead of the whole body.
 */
fun readRunParameters(reader: Reader, onResource: (JsonObject) -> Unit): RunParameters {
    val kept = mutableListOf<Parameter>()
    var resourceCount = 0
    var resourceTypeSeen = false
    jsonFactory.createParser(reader).use { parser ->
        parser.expect(parser.nextToken(), JsonToken.START_OBJECT, "Parameters")
        while (parser.nextToken() == JsonToken.FIELD_NAME) {
            parser.nextToken()
            if (parser.currentName() == "resourceType") {
                // usually the first key, so a wrong type fails before any resource is handed over
                val found = if (parser.currentToken() == JsonToken.VALUE_STRING) parser.text else parser.currentToken()
                check(found == "Parameters") { "Expected resourceType Parameters, found $found" }
                resourceTypeSeen = true
            } else if (parser.currentName() == "parameter") {
                parser.expect(parser.currentToken(), JsonToken.START_ARRAY, "Parameters.parameter")
                while (parser.nextToken() != JsonToken.END_ARRAY) {
                    val parameter = Json.decodeFromString<Parameter>(copyElement(parser))
                    if (parameter.name == "resources") {
                        onResource(parameter.resource ?: error("No resource provided at $resourceCount!"))
                        resourceCount++
                    } else {
                        kept += parameter
                    }
                }
            } else {
                parser.skipChildren()
            }
        }
        parser.nextToken()?.let { throw JsonParseException(parser, "Unexpected content after Parameters: $it") }
    }
    check(resourceTypeSeen) { "No resourceType provided, expected Parameters" }
    return RunParameters(Parameters("Parameters", kept), resourceCount)
}

private fun JsonParser.expect(actual: JsonToken?, expected: JsonToken, what: String) {
    if (actual != expected) {
        throw JsonParseException(this, "$what must start with $expected, found $actual")
    }
}

/** Copies the JSON value at the current token to a string, keeping number literals as written. */
private fun copyElement(parser: JsonParser): String {
    val out = StringWriter()
    jsonFactory.createGenerator(out).use { generator ->
        var depth = 0
        do {
            copyToken(parser, generator)
            when (parser.currentToken()) {
                JsonToken.START_OBJECT, JsonToken.START_ARRAY -> depth++
                JsonToken.END_OBJECT, JsonToken.END_ARRAY -> depth--
                else -> {}
            }
        } while (depth > 0 && parser.nextToken() != null)
    }
    return out.toString()
}

private fun copyToken(parser: JsonParser, generator: JsonGenerator) {
    when (parser.currentToken()) {
        JsonToken.START_OBJECT -> generator.writeStartObject()
        JsonToken.END_OBJECT -> generator.writeEndObject()
        JsonToken.START_ARRAY -> generator.writeStartArray()
        JsonToken.END_ARRAY -> generator.writeEndArray()
        JsonToken.FIELD_NAME -> generator.writeFieldName(parser.currentName())
        JsonToken.VALUE_STRING -> generator.writeString(parser.text)
        JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT -> generator.writeNumber(parser.text)
        JsonToken.VALUE_TRUE -> generator.writeBoolean(true)
        JsonToken.VALUE_FALSE -> generator.writeBoolean(false)
        JsonToken.VALUE_NULL -> generator.writeNull()
        else -> error("Unexpected JSON token ${parser.currentToken()}")
    }
}
