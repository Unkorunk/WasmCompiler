import tree.ConsoleNode
import tree.FinalNode
import tree.LetNode
import tree.expr.AddExprNode
import tree.expr.ConstExprNode
import tree.expr.VarExprNode
import utility.Leb128
import java.io.ByteArrayOutputStream
import java.io.File

enum class Token {
    Variable,
    Equal,
    Semicolon,
    Name,
    Number,

}

// let\s+           --> [var]
// \s*=\s*          --> [eq]
// ;                --> [sem]

// \][A-Za-z]\w*\[  --> [name]
// \d+              --> [num]

// [var][name][eq].*[sem] --> check variable in declaration map
//                                         |
//           if variable already declared / \ else
//       [error] re-declaring variable <-/   \-> generate variable declaration
//                                                           |
//                                              add variable to declaration map
//                                                           |
//                                           convert .* to Reverse Polish notation
//                                                           |
//                                        generate code using Reverse Polish notation
//                                                           |
//                                                         [code]


// [name][eq][expr][sem] -> check variable in declaration map --> generate assignment expression
//                                                             \-> [error] variable not declared


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

    // TODO: analyze source text

    val letNodeA = LetNode(ConstExprNode(123), null)
    val letNodeB = LetNode(ConstExprNode(256), null)
    letNodeA.nextNode = letNodeB
    val letNodeC = LetNode(AddExprNode(VarExprNode(letNodeA), VarExprNode(letNodeB)), null)
    letNodeB.nextNode = letNodeC

    val consoleNode = ConsoleNode(letNodeC, null)
    letNodeC.nextNode = consoleNode

    val finalNode = FinalNode()
    consoleNode.nextNode = finalNode

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
    body0.write(Leb128.toUnsignedLeb128(LetNode.getMaxIndex()))
    body0.write(Leb128.toSignedLeb128(-0x01))
    body0.write(letNodeA.getCode())

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
