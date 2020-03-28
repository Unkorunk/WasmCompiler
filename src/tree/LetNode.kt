package tree

import tree.expr.ExprNode
import utility.Leb128

class LetNode(private val exprNode: ExprNode, nextNode: Node?) : Node(nextNode) {
    private val variableIndex = getNewIndex()

    public override fun generateCode(): ByteArray {
        return exprNode.generateCode().plus(byteArrayOf(0x21)).plus(Leb128.toUnsignedLeb128(variableIndex))
    }

    public fun read() : ByteArray {
        return byteArrayOf(0x20).plus(Leb128.toUnsignedLeb128(variableIndex))
    }
    public fun write() : ByteArray {
        return byteArrayOf(0x21).plus(Leb128.toUnsignedLeb128(variableIndex))
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