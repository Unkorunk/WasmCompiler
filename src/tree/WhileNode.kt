package tree

import tree.expr.ExprNode
import utility.Leb128

class WhileNode(private val condition: ExprNode, private val thenNode: Node) : Node() {
    override fun generateCode(): ByteArray {
        return byteArrayOf(0x02).plus(Leb128.toSignedLeb128(-0x40)) // block (void)
            .plus(0x03).plus(Leb128.toSignedLeb128(-0x40)) // loop (void)
            .plus(condition.generateCode()).plus(0x45).plus(0x0d).plus(Leb128.toUnsignedLeb128(1)) // br_if 1 condition
            .plus(thenNode.getCode()) // thenNode
            .plus(0x0c).plus(Leb128.toUnsignedLeb128(0)) // br 0
            .plus(0x0b) // end (loop)
            .plus(0x0b) // end (block)
    }
}
