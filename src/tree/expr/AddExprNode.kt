package tree.expr


class AddExprNode(private val lhs: ExprNode, private val rhs: ExprNode) : ExprNode() {

    override fun generateCode(): ByteArray {
        return lhs.getCode().plus(rhs.getCode()).plus(byteArrayOf(0x6a))
    }
}