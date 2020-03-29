package syntaxtree

import utility.Leb128

class ConsoleNode(var letNode: LetNode) : Node() {
    override fun generateCode(): ByteArray {
        return letNode.read() // read a letNode variable
            .plus(0x10).plus(Leb128.toUnsignedLeb128(0)) // call imported console.log
    }
}
