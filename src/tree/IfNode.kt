package tree

import tree.expr.ExprNode
import utility.Leb128

class IfNode(private val condition: ExprNode, private val thenNode: Node,
             private val elseNode: Node?) : Node() {
    override fun generateCode(): ByteArray {
        var code = condition.generateCode().plus(0x04).plus(Leb128.toSignedLeb128(-0x40)) // if (void) condition
            .plus(thenNode.getCode())  // thenNode
        if (elseNode != null) {
            code = code.plus(0x05) // else
                .plus(elseNode.getCode()) // elseNode
        }
        return code.plus(0x0b) // end
    }
}
