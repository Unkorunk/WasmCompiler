import syntaxtree.*
import syntaxtree.expr.*
import utility.Leb128
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.util.*

enum class Token {
    Variable,   // let                      <=> [var]
    Assign,     // =                        <=> [assign]
    Semicolon,  // ;                        <=> [sem]
    Name,       // \a\w+                    <=> [name]
    Expr,       // expression               <=> [expr]
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
// [var][name][assign][expr][sem] --> check [name] in declaration map (in tree)
//                                         |
//           if variable already declared / \ else
//       [error] re-declaring variable <-/   \-> convert [expr] to Reverse Polish notation +
//                                                            |
//                                                    generate ExprNode +
//                                                            |
//                                                    generate LetNode
//                                                            |
//                                                add variable to declaration map

// Assign
// [name][eq][expr][sem] -> check [name] in declaration map (in tree)
//                                           |
//                                  get LetNode <=> [name]
//                                           |
//             if variable already declared / \ else
// [error] use of uninitialized variable <-/   \-> convert [expr] to Reverse Polish notation
//                                                                  |
//                                                           generate ExprNode
//                                                                  |
//                                                           generate AssignNode


fun main(args: Array<String>) {
    val sourceFilename = args.first()
    println("Input file: $sourceFilename")

    val sourceFile = File(sourceFilename)
    if (!sourceFile.exists()) {
        println("Input file doesn't exist")
        return
    }

    val outputFilename = sourceFile.nameWithoutExtension + ".wasm"
    var sourceText = sourceFile.readText()

    val addSpaceAround = arrayOf("+", "-", "/", "*", "<", ">", "==", "!=", "=", "(", ")", "{", "}", ";")
    for (entry in addSpaceAround) {
        sourceText = sourceText.replace(entry, " $entry ")
    }
    val tokensRaw = sourceText.split(Regex("\\s+"))
    val tokens = Array(tokensRaw.size) { i ->
        if (tokensRaw[i] in transformRawToken) transformRawToken[tokensRaw[i]] else null
    }

    File("debug").writeText(tokensRaw.joinToString(" "))

    // TODO: analyze source text

    val syntaxTree = SyntaxTree()
    syntaxTree.let("a", arrayOf("1"))
    syntaxTree.let("b", arrayOf("1"))
    syntaxTree.let("i", arrayOf("0"))
    syntaxTree.loop(arrayOf("i", "<", "5"))
    syntaxTree.assign("a", arrayOf("a", "+", "b"))
    syntaxTree.assign("b", arrayOf("a", "-", "b"))
    syntaxTree.assign("i", arrayOf("i", "+", "1"))
    syntaxTree.console("a")
    syntaxTree.end()

    val outputStream = File(outputFilename).outputStream()

    outputStream.write(byteArrayOf(0x00, 0x61, 0x73, 0x6d)) // magic number i.e., \0asm
    outputStream.write(byteArrayOf(0x01, 0x00, 0x00, 0x00)) // version number, 0x1

    // Type section
    val typeSection = ByteArrayOutputStream()
    typeSection.write(Leb128.toUnsignedLeb128(2)) // count of type entries to follow
    // type#entry#0 (i32) -> void
    typeSection.write(Leb128.toSignedLeb128(-0x20)) // func
    typeSection.write(Leb128.toUnsignedLeb128(1)) // param_count = 1
    typeSection.write(Leb128.toSignedLeb128(-0x01)) // param_types = { i32 }
    typeSection.write(Leb128.toUnsignedLeb128(0)) // return_count = 0
    // type#entry#1 () -> void
    typeSection.write(Leb128.toSignedLeb128(-0x20)) // func
    typeSection.write(Leb128.toUnsignedLeb128(0)) // param_count = 0
    typeSection.write(Leb128.toUnsignedLeb128(0)) // return_count = 0

    // Import section
    val importSection = ByteArrayOutputStream()
    importSection.write(Leb128.toUnsignedLeb128(1)) // count of import entries to follow
    // import#entry#0
    val moduleStr = "imports"
    val moduleStrBytes = moduleStr.encodeToByteArray()
    importSection.write(moduleStrBytes.size)
    importSection.write(moduleStrBytes)
    val fieldStr = "imported_func"
    val fieldStrBytes = fieldStr.encodeToByteArray()
    importSection.write(fieldStrBytes.size)
    importSection.write(fieldStrBytes)
    importSection.write(byteArrayOf(0x00)) // import function
    importSection.write(byteArrayOf(0)) // type index of the function signature = type#entry#0

    // Function section
    val functionSection = ByteArrayOutputStream()
    functionSection.write(Leb128.toUnsignedLeb128(1))
    functionSection.write(Leb128.toUnsignedLeb128(1)) // type#entry#1

    // Export section
    val exportSection = ByteArrayOutputStream()
    exportSection.write(Leb128.toUnsignedLeb128(1)) // count of export entries to follow
    // export#entry#0
    val fieldStr1 = "exported_func"
    val fieldStr1Bytes = fieldStr1.encodeToByteArray()
    exportSection.write(fieldStr1Bytes.size)
    exportSection.write(fieldStr1Bytes)
    exportSection.write(byteArrayOf(0x00)) // the kind of definition being exported = function
    exportSection.write(Leb128.toUnsignedLeb128(1)) // function#entry#1

    // body#0
    val body0 = ByteArrayOutputStream()
    body0.write(Leb128.toUnsignedLeb128(1)) // number of local entries = 1
    body0.write(Leb128.toUnsignedLeb128(syntaxTree.getMaxIndex()))
    body0.write(Leb128.toSignedLeb128(-0x01))
    body0.write(syntaxTree.compile())

    // Code section
    val codeSection = ByteArrayOutputStream()
    codeSection.write(Leb128.toUnsignedLeb128(1))
    // body#0
    codeSection.write(Leb128.toUnsignedLeb128(body0.size()))
    codeSection.write(body0.toByteArray())

    outputStream.write(Leb128.toUnsignedLeb128(1)) // section code = Type
    outputStream.write(Leb128.toUnsignedLeb128(typeSection.size()))
    outputStream.write(typeSection.toByteArray())

    outputStream.write(Leb128.toUnsignedLeb128(2)) // section code = Import
    outputStream.write(Leb128.toUnsignedLeb128(importSection.size()))
    outputStream.write(importSection.toByteArray())

    outputStream.write(Leb128.toUnsignedLeb128(3)) // section code = Function
    outputStream.write(Leb128.toUnsignedLeb128(functionSection.size()))
    outputStream.write(functionSection.toByteArray())

    outputStream.write(Leb128.toUnsignedLeb128(7)) // section code = Export
    outputStream.write(Leb128.toUnsignedLeb128(exportSection.size()))
    outputStream.write(exportSection.toByteArray())

    outputStream.write(Leb128.toUnsignedLeb128(10)) // section code = Code
    outputStream.write(Leb128.toUnsignedLeb128(codeSection.size()))
    outputStream.write(codeSection.toByteArray())

    outputStream.close()

    println("Output file: $outputFilename")

}
