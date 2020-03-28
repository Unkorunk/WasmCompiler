package tree

import utility.Leb128

class ConsoleNode(var letNode: LetNode?, nextNode: Node?) : Node(nextNode) {
    override fun generateCode(): ByteArray {
        if (letNode != null) {
            return letNode!!.read() // read a letNode variable
                .plus(0x10).plus(Leb128.toUnsignedLeb128(0)) // call console.log
        }
        return byteArrayOf()
    }
}