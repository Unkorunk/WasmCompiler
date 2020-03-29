package syntaxtree

import syntaxtree.expr.ExprNode
import utility.ConvertToExprNode
import java.lang.Exception
import java.util.*

class SyntaxTree {
    data class LevelData(
        val declarationMap: MutableMap<String, LetNode> = mutableMapOf(),
        var startNode: Node? = null,
        var parentNode: Node? = null
    )

    private enum class DataFlowType {
        Condition,
        Loop
    }

    private val exprStack = Stack<Pair<DataFlowType, ExprNode>>()
    private val levelStack = Stack<LevelData>()
    private var levelData = LevelData()

    fun searchDeclaration(name: String) : LetNode? {
        if (name in levelData.declarationMap) {
            return levelData.declarationMap[name]
        }

        var letNode : LetNode? = null
        for (levelData in levelStack) {
            if (name in levelData.declarationMap) {
                letNode = levelData.declarationMap[name]
                break
            }
        }
        return letNode
    }

    private var nextIdx = 0
    private fun getNewIndex() : Int {
        return nextIdx++
    }
    public fun getMaxIndex() : Int {
        return nextIdx + 1
    }

    private fun addNode(node: Node) {
        if (levelData.parentNode == null) {
            levelData.parentNode = node
        } else {
            levelData.parentNode!!.nextNode = node
            levelData.parentNode = levelData.parentNode!!.nextNode
        }

        if (levelData.startNode == null) {
            levelData.startNode = levelData.parentNode
        }
    }

    fun let(name: String, expr: Array<String>) {
        if (searchDeclaration(name) != null) {
            throw Exception("re-declaring variable")
        }

        val letNode = LetNode(ConvertToExprNode.getExprNode(this, expr), getNewIndex())
        levelData.declarationMap[name] = letNode

        addNode(letNode)
    }

    fun assign(name: String, expr: Array<String>) {
        val declaration = searchDeclaration(name) ?: throw Exception("use of uninitialized variable")

        val assignNode = AssignNode(declaration, ConvertToExprNode.getExprNode(this, expr))

        addNode(assignNode)
    }

    fun loop(expr: Array<String>) {
        levelStack.push(levelData)
        levelData = LevelData()
        exprStack.push(Pair(DataFlowType.Loop, ConvertToExprNode.getExprNode(this, expr)))
    }

    fun condition(expr: Array<String>) {
        levelStack.push(levelData)
        levelData = LevelData()
        exprStack.push(Pair(DataFlowType.Condition, ConvertToExprNode.getExprNode(this, expr)))
    }

    fun elseCondition() {
        // TODO: implement
    }

    fun end() {
        val expr = exprStack.pop()
        if (levelData.parentNode!!.nextNode is FinalNode) {
            levelData.parentNode!!.nextNode = null
        }
        val thenNode = levelData.startNode
        levelData = levelStack.pop() // return prev LevelData

        if (thenNode == null) {
            throw Exception("invalid data flow structure")
        }

        val flowNode : Node = if (expr.first == DataFlowType.Condition) {
            IfNode(expr.second, thenNode, null)
        } else if (expr.first == DataFlowType.Loop) {
            WhileNode(expr.second, thenNode)
        } else {
            throw Exception("please, add processing that DataFlowType to SyntaxTree.end()")
        }

        addNode(flowNode)
    }

    fun console(name: String) {
        val declaration = searchDeclaration(name) ?: throw Exception("use of uninitialized variable")

        val consoleNode = ConsoleNode(declaration)

        addNode(consoleNode)
    }

    fun compile() : ByteArray {
        if (!levelStack.empty()) {
            throw Exception("not balance curly brackets sequence")
        }

        addNode(FinalNode())
        if (levelData.startNode == null) {
            throw Exception("BUG")
        }

        return levelData.startNode!!.getCode()
    }

}