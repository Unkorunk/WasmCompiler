package syntaxtree

import syntaxtree.expr.ExprNode
import utility.Leb128

class LetNode(private val exprNode: ExprNode, private val variableIndex: Int) : Node() {

    public override fun generateCode(): ByteArray {
        return exprNode.generateCode().plus(byteArrayOf(0x21)).plus(Leb128.toUnsignedLeb128(variableIndex))
    }

    public fun read() : ByteArray {
        return byteArrayOf(0x20).plus(Leb128.toUnsignedLeb128(variableIndex))
    }
    public fun write() : ByteArray {
        return byteArrayOf(0x21).plus(Leb128.toUnsignedLeb128(variableIndex))
    }
}