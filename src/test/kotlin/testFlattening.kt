import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.content
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import server.module
import java.io.File
import kotlin.test.Test

class TestFlattening {

    @Test
    fun testFlattening() = testApplication {
        application {
            module()
        }
        client = createClient() {

        }

        val response = client.post("/fhir/ViewDefinition/\$run") {
            setBody(TestFlattening::class.java.getResourceAsStream("nested-sample.json"))
            contentType(ContentType.Application.Json)
        }
        println(response.bodyAsText())
    }
}