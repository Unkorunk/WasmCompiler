package tree

class FinalNode : Node() {
    public override fun generateCode(): ByteArray {
        return byteArrayOf(0x0b)
    }
}