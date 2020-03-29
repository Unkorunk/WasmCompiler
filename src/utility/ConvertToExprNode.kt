package utility

import syntaxtree.SyntaxTree
import syntaxtree.expr.*
import java.lang.Exception
import java.util.*

class ConvertToExprNode {
    companion object {
        private fun toReversePolishNotation(exprToken: List<Pair<Operation?, ExprNode?>>): ExprNode {
            val exprStack = Stack<ExprNode>()
            val operationStack = Stack<Operation>()

            for (token in exprToken) {
                if (token.first == null && token.second != null) {
                    exprStack.push(token.second!!)
                } else if (token.first != null && token.second == null) {
                    while (!operationStack.empty() && operationStack.peek().priority >= token.first!!.priority) {
                        val secondExpr = exprStack.pop()
                        val firstExpr = exprStack.pop()
                        exprStack.push(OperationExprNode(firstExpr, secondExpr, operationStack.pop()))
                    }
                    operationStack.push(token.first)
                } else {
                    throw Exception("operation and ExprNode either equal null or not null")
                }
            }

            while (!operationStack.empty()) {
                val secondExpr = exprStack.pop()
                val firstExpr = exprStack.pop()
                exprStack.push(OperationExprNode(firstExpr, secondExpr, operationStack.pop()))
            }

            if (exprStack.size != 1) {
                throw Exception("Error")
            }

            return exprStack.pop()
        }

        private fun toExprList(tree: SyntaxTree, expr: Array<String>): List<Pair<Operation?, ExprNode?>> {
            val transformStrToOp = mutableMapOf<String, Operation>()
            for (operation in Operation.values()) {
                transformStrToOp[operation.operationSymbol] = operation
            }

            val exprTokens = mutableListOf<Pair<Operation?, ExprNode?>>()

            for (tokenStr in expr) {
                if (tokenStr in transformStrToOp) {
                    exprTokens.add(Pair(transformStrToOp[tokenStr], null))
                } else {
                    val declaration = tree.searchDeclaration(tokenStr)
                    if (declaration != null) {
                        exprTokens.add(Pair(null, VarExprNode(declaration)))
                    } else {
                        val intVal = tokenStr.toIntOrNull()
                        if (intVal != null) {
                            exprTokens.add(Pair(null, ConstExprNode(intVal)))
                        } else {
                            throw Exception("use of uninitialized variable")
                        }
                    }
                }
            }


            return exprTokens
        }

        public fun getExprNode(tree: SyntaxTree, expr: Array<String>): ExprNode {
            return toReversePolishNotation(toExprList(tree, expr))
        }
    }
}