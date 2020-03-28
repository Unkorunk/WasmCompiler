package tree.expr

import tree.LetNode

class VarExprNode constructor(private val letNode: LetNode) : ExprNode() {
    override fun generateCode(): ByteArray {
        return letNode.read()
    }
}