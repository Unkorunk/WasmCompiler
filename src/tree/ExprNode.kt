package tree

import utility.Leb128

class ExprNode constructor(private val value: Int) : Node(null) {

    public override fun generateCode(): ByteArray {
        // i64.const
        return byteArrayOf(0x42).plus(Leb128.toSignedLeb128(value))
    }

}