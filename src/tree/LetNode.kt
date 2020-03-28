package tree

import utility.Leb128

class LetNode(private val exprNode: ExprNode, nextNode: Node) : Node(nextNode) {
    private val variableIndex = getNewIndex()

    protected override fun generateCode(): ByteArray {
        return exprNode.generateCode().plus(byteArrayOf(0x21)).plus(Leb128.toUnsignedLeb128(variableIndex))
    }

    public fun getIndex() : Int {
        return variableIndex
    }

    companion object {
        private var nextIdx = 0
        private fun getNewIndex() : Int {
            return nextIdx++
        }
        public fun getMaxIndex() : Int {
            return nextIdx + 1
        }
    }
}