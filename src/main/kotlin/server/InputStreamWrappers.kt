package server

import java.io.InputStream

class NewlineToCommaInputStream(private val inputStream: InputStream) : InputStream() {

    override fun read(cbuf: ByteArray, off: Int, len: Int): Int {
        val numRead = inputStream.read(cbuf, off, len)
        if (numRead == -1) return -1

        for (i in off..<off + numRead) {
            if (cbuf[i] == '\n'.code.toByte()) {
                cbuf[i] = ','.code.toByte()
            }
        }
        return numRead
    }

    override fun read() = when (val read = inputStream.read()) {
        '\n'.code -> ','.code
        else -> read
    }

    override fun close() {
        inputStream.close()
    }

}


class TrimLastByteInputStream(private val inputStream: InputStream) : InputStream() {
    private var nextByte: Int

    init {
        nextByte = inputStream.read()
    }

    override fun read(): Int {
        if (nextByte == -1) return -1 //special case: InputStream is empty

        val current = nextByte
        nextByte = inputStream.read()

        if (nextByte == -1) return -1

        return current
    }

    override fun close() {
        inputStream.close()
    }
}


