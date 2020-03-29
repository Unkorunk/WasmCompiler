package syntaxtree

abstract class Node constructor(var nextNode : Node?) {
    constructor() : this(null)

    public fun getCode() : ByteArray {
        if (nextNode != null) {
            return this.generateCode().plus(nextNode!!.getCode())
        }
        return this.generateCode()
    }

    abstract fun generateCode() : ByteArray
}