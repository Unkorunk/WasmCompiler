package parser

import syntaxtree.SyntaxTree
import java.lang.Exception

class Parser {
    // body        = { "Variable", declaration | "Data", assign | "While", whileLoop | "If", ifCondition }
    // expr        = "Data", { "Data" }
    // assign      = "Assign", expr, "Semicolon"
    // declaration = "Data", "Assign", expr, "Semicolon"
    // whileLoop   = "RBOpen", expr, "RBClose", "CBOpen", body, "CBClose"
    // ifCondition = "RBOpen", expr, "RBClose", "CBOpen", body, "CBClose", ["Else", "CBOpen", body, "CBClose"]

    private var tokens = emptyArray<Tokenizer.TokenData>()
    private var tokenIdx = 0
    private var syntaxTree = SyntaxTree()

    private var tokenOld : Tokenizer.TokenData? = null
    private var tokenNow : Tokenizer.TokenData? = null
    private var tokenProcessed = false
    private fun nextToken() : Boolean {
        tokenProcessed = true
        if (tokenIdx < tokens.size) {
            tokenOld = tokenNow
            tokenNow = tokens[tokenIdx++]
            tokenProcessed = false
            return true
        }
        return false
    }

    private fun accept(token: Tokenizer.Token) : Boolean {
        if (tokenNow != null && tokenNow!!.token == token) {
            nextToken()
            return true
        }
        return false
    }

    private fun expect(token: Tokenizer.Token) : Boolean {
        if (accept(token)) {
            return true
        }
        throw Exception("unexpected symbol")
    }

    private fun assign() {
        val name = (tokenOld ?: throw Exception("BUG")).data ?: throw Exception("BUG")
        expect(Tokenizer.Token.Assign)
        val expression = expr()
        expect(Tokenizer.Token.Semicolon)
        syntaxTree.assign(name, expression)
    }

    private fun declaration() {
        expect(Tokenizer.Token.Data)
        val name = (tokenOld ?: throw Exception("BUG")).data ?: throw Exception("BUG")
        expect(Tokenizer.Token.Assign)
        val expression = expr()
        expect(Tokenizer.Token.Semicolon)
        syntaxTree.let(name, expression)
    }

    private fun whileLoop() {
        expect(Tokenizer.Token.RBOpen)
        val expression = expr()
        expect(Tokenizer.Token.RBClose)
        expect(Tokenizer.Token.CBOpen)
        syntaxTree.whileLoop(expression)
        body()
        expect(Tokenizer.Token.CBClose)
        syntaxTree.end()
    }

    private fun expr() : Array<String> {
        var arrExpr = emptyArray<String>()

        expect(Tokenizer.Token.Data)
        do {
            arrExpr = arrExpr.plus((tokenOld ?: throw Exception("BUG")).data ?: throw Exception("BUG"))
        } while (accept(Tokenizer.Token.Data))

        return arrExpr
    }

    private fun ifCondition() {
        expect(Tokenizer.Token.RBOpen)
        val expression = expr()
        expect(Tokenizer.Token.RBClose)
        expect(Tokenizer.Token.CBOpen)
        syntaxTree.ifCondition(expression)
        body()
        expect(Tokenizer.Token.CBClose)
        if (accept(Tokenizer.Token.Else)) {
            expect(Tokenizer.Token.CBOpen)
            syntaxTree.elseCondition()
            body()
            expect(Tokenizer.Token.CBClose)
        }
        syntaxTree.end()
    }

    private fun body() {
        while (true) {
            if (accept(Tokenizer.Token.If)) {
                ifCondition()
            } else if (accept(Tokenizer.Token.While)) {
                whileLoop()
            } else if (accept(Tokenizer.Token.Variable)) {
                declaration()
            } else if (accept(Tokenizer.Token.Data)) {
                assign()
            } else {
                break
            }
        }
    }

    fun parse(sourceText: String) : SyntaxTree {
        tokens = Tokenizer.tokenize(sourceText)
        tokenIdx = 0
        syntaxTree = SyntaxTree()

        nextToken()
        body()

        if (tokenIdx < tokens.size || !tokenProcessed) {
            throw Exception("unexpected symbol")
        }

        return syntaxTree
    }
}
