package syntaxtree.expr

import utility.Leb128

class ConstExprNode(private val value: Int) : ExprNode() {
    override fun generateCode(): ByteArray {
        // i32.const
        return byteArrayOf(0x41).plus(Leb128.toSignedLeb128(value))
    }
}