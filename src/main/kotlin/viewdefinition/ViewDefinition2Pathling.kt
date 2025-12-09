import au.csiro.pathling.views.ConstantDeclaration
import au.csiro.pathling.views.FhirView
import au.csiro.pathling.views.FhirView.column
import au.csiro.pathling.views.FhirView.forEach
import au.csiro.pathling.views.FhirView.forEachOrNull
import au.csiro.pathling.views.FhirView.select
import au.csiro.pathling.views.FhirViewBuilder
import au.csiro.pathling.views.SelectClause
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.Base64BinaryType
import org.hl7.fhir.r4.model.BooleanType
import org.hl7.fhir.r4.model.CanonicalType
import org.hl7.fhir.r4.model.CodeType
import org.hl7.fhir.r4.model.DateTimeType
import org.hl7.fhir.r4.model.DateType
import org.hl7.fhir.r4.model.DecimalType
import org.hl7.fhir.r4.model.IdType
import org.hl7.fhir.r4.model.InstantType
import org.hl7.fhir.r4.model.IntegerType
import org.hl7.fhir.r4.model.OidType
import org.hl7.fhir.r4.model.PositiveIntType
import org.hl7.fhir.r4.model.StringType
import org.hl7.fhir.r4.model.TimeType
import org.hl7.fhir.r4.model.UnsignedIntType
import org.hl7.fhir.r4.model.UriType
import org.hl7.fhir.r4.model.UrlType
import org.hl7.fhir.r4.model.UuidType
import viewdefinition.Select
import viewdefinition.ViewDefinition
import java.io.File


fun ViewDefinition.toFhirView(): FhirView {
    var tmp = FhirView.ofResource(this.resource)

    for (constant in this.constant) {
        tmp = tmp.constant(
            ConstantDeclaration(
                constant.name, when {
                    constant.valueBase64Binary != null -> Base64BinaryType(constant.valueBase64Binary)
                    constant.valueBoolean != null -> BooleanType(constant.valueBoolean)
                    constant.valueCanonical != null -> CanonicalType(constant.valueCanonical)
                    constant.valueCode != null -> CodeType(constant.valueCode.toString())
                    constant.valueDate != null -> DateType(constant.valueDate.toString())
                    constant.valueDateTime != null -> DateTimeType(constant.valueDateTime.toString())
                    constant.valueDecimal != null -> DecimalType(constant.valueDecimal.toString())
                    constant.valueId != null -> IdType(constant.valueId)
                    constant.valueInstant != null -> InstantType(constant.valueInstant.toString())
                    constant.valueInteger != null -> IntegerType(constant.valueInteger)
                    constant.valueInteger64 != null -> IntegerType(constant.valueInteger64.toString())
                    constant.valueOid != null -> OidType(constant.valueOid)
                    constant.valueString != null -> StringType(constant.valueString)
                    constant.valuePositiveInt != null -> PositiveIntType(constant.valuePositiveInt.toLong())
                    constant.valueTime != null -> TimeType(constant.valueTime.toString())
                    constant.valueUnsignedInt != null -> UnsignedIntType(constant.valueUnsignedInt.toString())
                    constant.valueUri != null -> UriType(constant.valueUri)
                    constant.valueUrl != null -> UrlType(constant.valueUrl)
                    constant.valueUuid != null -> UuidType(constant.valueUuid)

                    constant.valueString != null -> StringType(constant.valueString)
                    else -> error("Value for constant '$constant' not found")
                }
            )
        )
        //TODO: Handle other data types than string
    }

    for (item in this.select) {
        tmp.select(buildSelect(item))
    }

    for (item in this.where) {
        tmp = tmp.where(item.path)
    }
    return tmp.build()

}




private fun buildSelect(item: Select): SelectClause {
    return SelectClause.builder().apply {
        column(*item.column.map { column(it.name, it.path) }.toTypedArray())
        if(item.forEach != null) forEach(item.forEach)
        if(item.forEachOrNull != null) forEachOrNull(item.forEachOrNull)
        if(item.repeat != null) repeat(item.repeat)
        if(item.unionAll.isNotEmpty()) unionAll(*item.unionAll.map { buildSelect(it) }.toTypedArray())
        if(item.select.isNotEmpty()) select(*item.select.map { buildSelect(it) }.toTypedArray())
    }.build()
}