import generator.Generator
import syntaxtree.*
import utility.Leb128
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception

enum class Token {
    Variable,   // let                      <=> [var]
    Assign,     // =                        <=> [assign]
    Semicolon,  // ;                        <=> [sem]
    Name,       // \a\w*[assign]            <=> [name][assign]
    Expr,       // [assign].*[sem]          <=> [assign][expr][sem]
    RBOpen,     // round bracket open (     <=> [ropen]
    RBClose,    // round bracket close )    <=> [rclose]
    CBOpen,     // curly bracket open {     <=> [copen]
    CBClose,    // curly bracket close }    <=> [cclose]
    While,      // while                    <=> [while]
    If,         // if                       <=> [if]
    Else        // else                     <=> [else]
}

val transformRawToken = mapOf(
    Pair("let", Token.Variable),
    Pair("=", Token.Assign),
    Pair(";", Token.Semicolon),
    Pair("while", Token.While),
    Pair("if", Token.If),
    Pair("else", Token.Else),
    Pair("{", Token.CBOpen),
    Pair("}", Token.CBClose),
    Pair("(", Token.RBOpen),
    Pair(")", Token.RBClose)
)

// Variable declaration
// [var][name][assign][expr][sem] -> syntaxTree.let([name], [expr])
// Assign
// [name][assign][expr][sem] -> syntaxTree.assign([name], [expr])
// If
// [if][ropen][expr][rclose][copen] -> syntaxTree.ifCondition([expr])
// Else
// [else][copen] -> syntaxTree.elseCondition()
// While
// [while][ropen][expr][rclose][copen] -> syntaxTree.whileLoop([expr])
// End data flow block <=> '}'
// [cclose] -> syntaxTree.end()

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("no input file")
        return
    }

    val sourceFilename = args.first()
    println("Input file: $sourceFilename")

    val sourceFile = File(sourceFilename)
    if (!sourceFile.exists()) {
        println("Input file doesn't exist")
        return
    }

    val outputFilename = sourceFile.nameWithoutExtension + ".wasm"
    var sourceText = sourceFile.readText()

    val operations = arrayOf("+", "-", "/", "*", "<", ">", "==", "!=")
    val addSpaceAround = arrayOf("(", ")", "{", "}", ";").plus(operations)
    for (entry in addSpaceAround) {
        sourceText = sourceText.replace(entry, " $entry ")
    }
    var tempIdx = 1
    while (tempIdx < sourceText.length - 1) {
        if (sourceText[tempIdx] == '=' && sourceText[tempIdx - 1] != '!' && sourceText[tempIdx - 1] != '=' && sourceText[tempIdx + 1] != '=') {
            sourceText = sourceText.replaceRange(tempIdx, tempIdx + 1, " = ")
            tempIdx += 3
        } else {
            tempIdx++
        }
    }

    val tokensRaw = sourceText.split(Regex("\\s+"))
    val tokens = Array(tokensRaw.size) { i ->
        if (tokensRaw[i] in transformRawToken) transformRawToken[tokensRaw[i]] else null
    }

    val stateMachineLength = 17
    val stateMachine = Array<MutableMap<Token, Int>>(stateMachineLength + 1) { mutableMapOf() }
    for (i in 0 until stateMachineLength) {
        for (token in Token.values()) {
            stateMachine[i][token] = stateMachineLength
        }
    }

    // variable
    stateMachine[0][Token.Variable] = 1; stateMachine[1][Token.Name] = 2; stateMachine[2][Token.Assign] = 3; stateMachine[3][Token.Expr] = 4; stateMachine[4][Token.Semicolon] = 0
    // if
    stateMachine[0][Token.If] = 5; stateMachine[5][Token.RBOpen] = 6; stateMachine[6][Token.Expr] = 7; stateMachine[7][Token.RBClose] = 8; stateMachine[8][Token.CBOpen] = 0
    // assign
    stateMachine[0][Token.Name] = 9; stateMachine[9][Token.Assign] = 10; stateMachine[10][Token.Expr] = 11; stateMachine[11][Token.Semicolon] = 0
    // else
    stateMachine[0][Token.Else] = 12; stateMachine[12][Token.CBOpen] = 0
    // while
    stateMachine[0][Token.While] = 13; stateMachine[13][Token.RBOpen] = 14; stateMachine[14][Token.Expr] = 15; stateMachine[15][Token.RBClose] = 16; stateMachine[16][Token.CBOpen] = 0
    // end
    stateMachine[0][Token.CBClose] = 0
    // wait end of expression
    stateMachine[4][Token.Expr] = 4
    stateMachine[7][Token.Expr] = 7
    stateMachine[11][Token.Expr] = 11
    stateMachine[15][Token.Expr] = 15

    val syntaxTree = SyntaxTree()

    var stateMachineIdx = 0
    var variableName = ""
    var parsedExpression = arrayOf<String>()
    for ((tokenIdx, tokenRaw) in tokens.withIndex()) {
        val token = tokenRaw ?:
        if (stateMachineIdx in arrayOf(0, 1)) {
            if (tokensRaw[tokenIdx].substring(0).replace(Regex("[A-Za-z]\\w*"), "") == "") {
                variableName = tokensRaw[tokenIdx]
                Token.Name
            } else {
                throw Exception("invalid variable name")
            }
        } else if (stateMachineIdx in arrayOf(3, 4, 6, 7, 10, 11, 14, 15)) {
            if (syntaxTree.searchDeclaration(tokensRaw[tokenIdx]) != null ||
                tokensRaw[tokenIdx].toIntOrNull() != null || tokensRaw[tokenIdx] in operations) {
                parsedExpression = parsedExpression.plus(tokensRaw[tokenIdx])
                Token.Expr
            } else {
                throw Exception("invalid expression")
            }
        } else {
            throw Exception("BUG in state machine")
        }

        when {
            stateMachineIdx == 4 && token == Token.Semicolon -> syntaxTree.let(variableName, parsedExpression)
            stateMachineIdx == 8 && token == Token.CBOpen -> syntaxTree.ifCondition(parsedExpression)
            stateMachineIdx == 11 && token == Token.Semicolon -> syntaxTree.assign(variableName, parsedExpression)
            stateMachineIdx == 12 && token == Token.CBOpen -> syntaxTree.elseCondition()
            stateMachineIdx == 16 && token == Token.CBOpen -> syntaxTree.whileLoop(parsedExpression)
            stateMachineIdx == 0 && token == Token.CBClose -> syntaxTree.end()
        }

        if (token == Token.Semicolon || token == Token.CBOpen) {
            variableName = ""
            parsedExpression = arrayOf()
        }

        stateMachineIdx = stateMachine[stateMachineIdx][token]!!

        if (stateMachineIdx == stateMachineLength) {
            throw Exception("BUG in state machine")
        }
    }

    for (name in syntaxTree.getDeclarationsAtLevel()) {
        syntaxTree.console(name)
    }

    val generator = Generator()
    val typeImportedFunc = generator.addType(1, 0)
    val typeExportedFunc = generator.addType(0, 0)
    generator.addImport("imports", "imported_func", typeImportedFunc)
    val exportedFunc = generator.addFunc(typeExportedFunc, syntaxTree)
    generator.addExport("exported_func", exportedFunc)

    File(outputFilename).writeBytes(generator.compile())
    println("Output file: $outputFilename")
}
