
import au.csiro.pathling.library.PathlingContext
import au.csiro.pathling.library.io.source.NdjsonSource
import au.csiro.pathling.library.query.FhirViewQuery
import au.csiro.pathling.views.FhirView
import au.csiro.pathling.views.FhirView.column
import au.csiro.pathling.views.FhirView.columns
import au.csiro.pathling.views.FhirView.forEach
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode

fun main() {
    val pc = PathlingContext.create()
    val data = pc.read().ndjson("input/data")

    /*val view1 = FhirView.ofResource("Patient").select(columns(column("id", "getResourceKey()"))).build()
    data.view(view1)
        .execute().show() */



    val view = FhirView.ofResource("Patient")
        .select(
            columns(
                column("patient_id", "getResourceKey()"),
                column("gender", "gender"),
                column("birthdate", "birthDate")
            )

        )
        .build()

    val result: Dataset<Row?> = data.view(view).execute()



    println("Hello World!")
    //result.show()
    result.write().mode(SaveMode.Overwrite).json("output/output3.json")
    println("Hello World!!!")

}