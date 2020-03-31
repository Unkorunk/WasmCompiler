import generator.Generator
import parser.Parser
import java.io.File

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
    val sourceText = sourceFile.readText()

    val parser = Parser()
    val syntaxTree = parser.parse(sourceText)

    for (name in syntaxTree.getDeclarationsAtLevel()) {
        syntaxTree.console(name)
    }

    val generator = Generator()
    val typeImportedFunc = generator.addType(1, 0)
    val typeExportedFunc = generator.addType(0, 0)
    generator.addImport("imports", "imported_func", typeImportedFunc)
    val exportedFunc = generator.addFunc(typeExportedFunc, syntaxTree)
    // IMPORTANT: !!! Add exports only after imports !!!
    generator.addExport("exported_func", exportedFunc)

    File(outputFilename).writeBytes(generator.compile())
    println("Output file: $outputFilename")
}
