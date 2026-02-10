package server

import com.twitter.chill.Base64.InputStream
import org.junit.jupiter.api.Assertions.*
import scala.Array.emptyByteArray
import java.io.ByteArrayInputStream
import kotlin.test.Test

class NewlineToCommaInputStreamTest {

    @Test
    fun assertNewlinesAreReplaced() {
        val inputStream = ByteArrayInputStream("1\n2\n3".toByteArray(Charsets.UTF_8))
        val wrapped = NewlineToCommaInputStream(inputStream)
        val wrappedResult = wrapped.readBytes().toString(Charsets.UTF_8)
        assertEquals("1,2,3", wrappedResult)
    }

}

class TrimLastByteInputStreamTest {
    @Test
    fun assertLastByteIsMissing() {
        val inputStream = ByteArrayInputStream("123".toByteArray(Charsets.UTF_8))
        val wrapped = TrimLastByteInputStream(inputStream)
        val wrappedResult = wrapped.readBytes().toString(Charsets.UTF_8)
        assertEquals("12", wrappedResult)
    }

    @Test
    fun testEmptyInput() {
        val emptyInputStream = InputStream.nullInputStream()
        val wrapped = TrimLastByteInputStream(emptyInputStream)
        val wrappedResult = wrapped.readBytes()
        assert(wrappedResult.isEmpty())
    }

}