package tree.expr

class OperationExprNode(private val lhs: ExprNode, private val rhs: ExprNode, private val operation: Operation) : ExprNode() {

    override fun generateCode(): ByteArray {
        return lhs.generateCode().plus(rhs.generateCode()).plus(byteArrayOf(operation.bytecode))
    }
}