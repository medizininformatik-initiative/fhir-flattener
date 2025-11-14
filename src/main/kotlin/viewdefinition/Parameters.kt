package viewdefinition

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Parameters(
    val resourceType: String,
    val parameter: List<Parameter>,
) {
    operator fun get(name: String): Parameter? {
        return this.parameter.find { it.name == name }
    }
    fun getAsList(name: String): List<Parameter> {
        return this.parameter.filter { it.name == name }
    }
}

@Serializable
data class Parameter(
    val name: String,
    val resource: JsonObject? = null,
    val valueString: String? = null,
    val valueCode: String? = null,
    val valueUri: String? = null,
    val valueReference: JsonObject? = null,
    val part: List<Parameter>? = null
) {
    operator fun get(name: String): Parameter? {
        return this.part?.find { it.name == name }
    }
    fun getAsList(name: String): List<Parameter>? {
        return this.part?.filter { it.name == name }
    }
}


