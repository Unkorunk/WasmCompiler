package tree

abstract class Node constructor(private val nextNode : Node?) {

    public fun getCode() : ByteArray {
        if (nextNode != null) {
            return this.generateCode().plus(nextNode.getCode())
        }
        return this.generateCode()
    }

    protected abstract fun generateCode() : ByteArray
}