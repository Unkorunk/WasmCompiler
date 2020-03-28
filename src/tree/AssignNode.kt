package tree

import tree.expr.ExprNode

class AssignNode(private val letNode: LetNode, private val exprNode: ExprNode, nextNode: Node?) : Node(nextNode) {
    override fun generateCode(): ByteArray {
        return exprNode.generateCode().plus(letNode.write())
    }
}
