import java.io.ByteArrayOutputStream
import java.io.File

fun toUnsignedLeb128(value: Int) : ByteArray {
    val byteStream = ByteArrayOutputStream()

    var value1 = value
    var remaining1 = value1 ushr 7

    while (remaining1 != 0) {
        byteStream.write(value1 and 0x7f or 0x80)
        value1 = remaining1
        remaining1 = remaining1 ushr 7
    }
    byteStream.write(value1 and 0x7f)

    return byteStream.toByteArray()
}

fun toSignedLeb128(value: Int) : ByteArray {
    val byteStream = ByteArrayOutputStream()

    var value1 = value
    var remaining1 = value1 shr 7
    var hasMore = true
    val end = if (value1 and Int.MIN_VALUE == 0) 0 else -1
    while (hasMore) {
        hasMore = (remaining1 != end
                || remaining1 and 1 != value1 shr 6 and 1)
        byteStream.write(value1 and 0x7f or if (hasMore) 0x80 else 0)
        value1 = remaining1
        remaining1 = remaining1 shr 7
    }

    return byteStream.toByteArray()
}

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




    val outputStream = File(outputFilename).outputStream()

    outputStream.write(byteArrayOf(0x00, 0x61, 0x73, 0x6d)) // magic number i.e., \0asm
    outputStream.write(byteArrayOf(0x01, 0x00, 0x00, 0x00)) // version number, 0x1

    // Type section
    val typeSection = ByteArrayOutputStream()
    typeSection.write(toUnsignedLeb128(2)) // count of type entries to follow
    // type#entry#0 (i32) -> void
    typeSection.write(toSignedLeb128(-0x20)) // func
    typeSection.write(toUnsignedLeb128(1)) // param_count = 1
    typeSection.write(toSignedLeb128(-0x01)) // param_types = { i32 }
    typeSection.write(toUnsignedLeb128(0)) // return_count = 0
    // type#entry#1 () -> void
    typeSection.write(toSignedLeb128(-0x20)) // func
    typeSection.write(toUnsignedLeb128(0)) // param_count = 0
    typeSection.write(toUnsignedLeb128(0)) // return_count = 0

    // Import section
    val importSection = ByteArrayOutputStream()
    importSection.write(toUnsignedLeb128(1)) // count of import entries to follow
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
    functionSection.write(toUnsignedLeb128(1))
    functionSection.write(toUnsignedLeb128(1)) // type#entry#1

    // Export section
    val exportSection = ByteArrayOutputStream()
    exportSection.write(toUnsignedLeb128(1)) // count of export entries to follow
    // export#entry#0
    val fieldStr1 = "exported_func"
    val fieldStr1Bytes = fieldStr1.encodeToByteArray()
    exportSection.write(fieldStr1Bytes.size)
    exportSection.write(fieldStr1Bytes)
    exportSection.write(byteArrayOf(0x00)) // the kind of definition being exported = function
    exportSection.write(toUnsignedLeb128(1)) // function#entry#0

    // body#0
    val body0 = ByteArrayOutputStream()
    body0.write(toUnsignedLeb128(0)) // number of local entries = 0
    body0.write(byteArrayOf(0x41)) // i32.const
    body0.write(toSignedLeb128(42)) // value : varint32 = 42
    body0.write(byteArrayOf(0x10)) // call
    body0.write(toUnsignedLeb128(0)) // type_index : varuint32 = 0
    body0.write(byteArrayOf(0x0b)) // end of the body

    // Code section
    val codeSection = ByteArrayOutputStream()
    codeSection.write(toUnsignedLeb128(1))
    // body#0
    codeSection.write(toUnsignedLeb128(body0.size()))
    codeSection.write(body0.toByteArray())

    outputStream.write(toUnsignedLeb128(1)) // section code = Type
    outputStream.write(toUnsignedLeb128(typeSection.size()))
    outputStream.write(typeSection.toByteArray())

    outputStream.write(toUnsignedLeb128(2)) // section code = Import
    outputStream.write(toUnsignedLeb128(importSection.size()))
    outputStream.write(importSection.toByteArray())

    outputStream.write(toUnsignedLeb128(3)) // section code = Function
    outputStream.write(toUnsignedLeb128(functionSection.size()))
    outputStream.write(functionSection.toByteArray())

    outputStream.write(toUnsignedLeb128(7)) // section code = Export
    outputStream.write(toUnsignedLeb128(exportSection.size()))
    outputStream.write(exportSection.toByteArray())

    outputStream.write(toUnsignedLeb128(10)) // section code = Code
    outputStream.write(toUnsignedLeb128(codeSection.size()))
    outputStream.write(codeSection.toByteArray())

    outputStream.close()

    println("Output file: $outputFilename")

}
