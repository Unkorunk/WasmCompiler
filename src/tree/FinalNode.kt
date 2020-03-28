package tree

class FinalNode : Node(null) {
    protected override fun generateCode(): ByteArray {
        return byteArrayOf(0x0b)
    }
}