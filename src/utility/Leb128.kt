package utility

import java.io.ByteArrayOutputStream

class Leb128 {
    companion object {
        fun toUnsignedLeb128(value: Int) : ByteArray {
            val byteStream = ByteArrayOutputStream()

            var value1 = value
            var remaining1 = value1 ushr 7

            while (remaining1 != 0) {
                byteStream.write(value1 and 0x7f or 0x80)
                value1 = remaining1
                remaining1 = remaining1 ushr 7
            }
            byteStream.write(value1 and 0x7f)

            return byteStream.toByteArray()
        }

        fun toSignedLeb128(value: Int) : ByteArray {
            val byteStream = ByteArrayOutputStream()

            var value1 = value
            var remaining1 = value1 shr 7
            var hasMore = true
            val end = if (value1 and Int.MIN_VALUE == 0) 0 else -1
            while (hasMore) {
                hasMore = (remaining1 != end
                        || remaining1 and 1 != value1 shr 6 and 1)
                byteStream.write(value1 and 0x7f or if (hasMore) 0x80 else 0)
                value1 = remaining1
                remaining1 = remaining1 shr 7
            }

            return byteStream.toByteArray()
        }
    }
}