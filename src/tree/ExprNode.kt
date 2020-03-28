package tree

import utility.Leb128

class ExprNode constructor(private val value: Int) : Node(null) {

    public override fun generateCode(): ByteArray {
        // i32.const
        return byteArrayOf(0x41).plus(Leb128.toSignedLeb128(value))
    }

}