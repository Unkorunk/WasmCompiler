package tree.expr


class AddExprNode(private val lhs: ExprNode, private val rhs: ExprNode) : ExprNode() {

    override fun generateCode(): ByteArray {
        return lhs.generateCode().plus(rhs.generateCode()).plus(byteArrayOf(0x6a))
    }
}