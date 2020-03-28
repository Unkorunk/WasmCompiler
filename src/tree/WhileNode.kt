package tree

import tree.expr.ExprNode
import utility.Leb128

class WhileNode(private val condition: ExprNode, private val thenNode: Node, nextNode: Node?) : Node(nextNode) {
    override fun generateCode(): ByteArray {
        return byteArrayOf(0x03).plus(Leb128.toSignedLeb128(-0x40))
            .plus(condition.generateCode()).plus(0x45).plus(0x0d).plus(Leb128.toUnsignedLeb128(1))
            .plus(thenNode.getCode())
            .plus(0x0c).plus(Leb128.toUnsignedLeb128(0))
            .plus(0x0b)
    }
}
