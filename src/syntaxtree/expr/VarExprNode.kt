package syntaxtree.expr

import syntaxtree.LetNode

class VarExprNode constructor(private val letNode: LetNode) : ExprNode() {
    override fun generateCode(): ByteArray {
        return letNode.read()
    }
}