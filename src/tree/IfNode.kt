package tree

import tree.expr.ExprNode
import utility.Leb128

class IfNode(private val condition: ExprNode, private val thenNode: Node,
             private val elseNode: Node?, nextNode: Node?) : Node(nextNode) {
    override fun generateCode(): ByteArray {
        var code = condition.generateCode().plus(0x04).plus(Leb128.toSignedLeb128(-0x40)).plus(thenNode.getCode())
        if (elseNode != null) {
            code = code.plus(0x05).plus(elseNode.getCode())
        }
        return code.plus(0x0b)
    }
}