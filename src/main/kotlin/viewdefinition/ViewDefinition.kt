package viewdefinition

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter


@JvmInline
@Serializable
value class Uri(val value: String)

@JvmInline
@Serializable
value class Code(val value: String)

@Serializable
data class ViewDefinition(
    val resourceType: String,
    val url: Uri? = null,
    val identifier: JsonObject = JsonObject(emptyMap()),
    val name: String? = null,
    val title: String? = null,
    val meta: JsonObject = JsonObject(emptyMap()),
    val status: String = "draft", //draft | active | retired | unknown
    val experimental: Boolean? = null,
    val publisher: String? = null,
    val contact: JsonObject = JsonObject(emptyMap()),
    val description: String? = null,
    val useContext: JsonObject = JsonObject(emptyMap()),
    val copyright: String? = null,
    val resource: String,
    val fhirVersion: List<String> = emptyList(),
    val constant: List<Constant> = emptyList(),
    val select: List<Select> = emptyList(),
    val where: List<Where> = emptyList(),
    )

@Serializable

data class Constant(
    val name: String,
    val valueBase64Binary: ByteArray? = null,
    val valueBoolean: Boolean? = null,
    val valueCanonical: String? = null,
    val valueCode: Code? = null,
    @Serializable(with = DateSerializer::class)
    val valueDate: LocalDate? = null,
    @Serializable(with = DateTimeSerializer::class)
    val valueDateTime: LocalDateTime? = null,
    @Serializable(with = BigDecimalSerializer::class)
    val valueDecimal: BigDecimal? = null,
    val valueId: String? = null,
    @Serializable(with = InstantSerializer::class)
    val valueInstant: Instant? = null,
    val valueInteger: Int? = null,
    val valueInteger64: Long? = null,
    val valueOid: String? = null,
    val valueString: String? = null,
    val valuePositiveInt: UInt? = null,
    @Serializable(with = TimeSerializer::class)
    val valueTime: LocalTime? = null,
    val valueUnsignedInt: UInt? = null,
    val valueUri: String? = null,
    val valueUrl: String? = null,
    val valueUuid: String? = null,
)

@Serializable
data class Select(
    val column: List<Column> = emptyList(),
    val select: List<Select> = emptyList(),
    val forEach: String? = null,
    val forEachOrNull: String? = null,
    val unionAll: List<Select> = emptyList(),
)

@Serializable
data class Column(
    val path: String? = null,
    val name: String? = null,
    val description: String? = null,
    val collection: Boolean? = null,
    val type: Uri? = null,
    val tag: List<Tag>? = emptyList(),
)
@Serializable
data class Tag(
    val name: String? = null,
    val value: String? = null,
)

@Serializable
data class Where(
    val path: String? = null,
    val description: String? = null,
)

//TODO: add extension/modifierExtension


@Serializer(forClass = LocalDate::class)
object DateSerializer : KSerializer<LocalDate> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        return LocalDate.parse(decoder.decodeString(), formatter)
    }
}
@Serializer(forClass = LocalDateTime::class)
object DateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}
@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<java.time.Instant> {

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.parse(decoder.decodeString())
    }
}


@Serializer(forClass = LocalTime::class)
object TimeSerializer : KSerializer<LocalTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_TIME

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.parse(decoder.decodeString(), formatter)
    }
}

@Serializer(forClass = java.math.BigDecimal::class)
object BigDecimalSerializer : KSerializer<java.math.BigDecimal> {
    override fun serialize(encoder: Encoder, value: java.math.BigDecimal) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): java.math.BigDecimal {
        return java.math.BigDecimal(decoder.decodeString())
    }
}



