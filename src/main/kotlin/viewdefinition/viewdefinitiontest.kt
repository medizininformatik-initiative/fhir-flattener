import au.csiro.pathling.views.ConstantDeclaration
import au.csiro.pathling.views.FhirView
import au.csiro.pathling.views.FhirView.column
import au.csiro.pathling.views.FhirView.columns
import au.csiro.pathling.views.FhirView.forEach
import au.csiro.pathling.views.FhirView.forEachOrNull
import au.csiro.pathling.views.FhirView.select
import kotlinx.serialization.json.Json
import org.hl7.fhir.r4.model.StringType
import viewdefinition.ViewDefinition
import java.io.File

fun main() {
    File("src/main/resources/").listFiles().forEach {
        val result = Json.decodeFromString<ViewDefinition>(it.readText())

        println(result)
        println(result.toFhirView())
    }

}


fun ViewDefinition.toFhirView(): FhirView {
    var tmp = FhirView.ofResource(this.resource)


    for(constant in this.constant) {
        tmp = tmp.constant(ConstantDeclaration(constant.name, StringType(constant.valueString)))
        //TODO: Handle other data types than string
    }

    for (item in this.select) {
        val columns = item.column.map { column(it.name, it.path) }.toTypedArray()
        tmp = tmp.select(when {
            item.forEach != null -> forEach(item.forEach, *columns)
            item.forEachOrNull != null -> forEachOrNull(item.forEachOrNull, *columns)
            else -> select(*columns)
        })
        //TODO: Handle union etc.
    }

    for (item in this.where) {
        tmp = tmp.where(item.path)
    }
    return tmp.build()

}