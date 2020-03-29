package tree

import tree.expr.ExprNode

class AssignNode(private val letNode: LetNode, private val exprNode: ExprNode) : Node() {
    override fun generateCode(): ByteArray {
        return exprNode.generateCode().plus(letNode.write())
    }
}
