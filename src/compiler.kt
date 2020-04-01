import generator.Generator
import parser.Parser
import java.io.File
import java.lang.Exception

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("no input file")
        return
    }

    val sourceFilename = args.first()
    println("input file: $sourceFilename")

    val sourceFile = File(sourceFilename)
    if (!sourceFile.exists()) {
        println("input file doesn't exist")
        return
    }

    val outputFilename = sourceFile.nameWithoutExtension + ".wasm"
    val sourceText = sourceFile.readText()

    val parser = Parser()
    val syntaxTree = try {
        parser.parse(sourceText)
    } catch (ex: Exception) {
        println("[error] ${ex.message}")
        return
    }
    // TODO: delete that. it's for debug
    for (name in syntaxTree.getDeclarationsAtLevel()) {
        syntaxTree.console(name)
    }

    val generator = Generator()
    val typeImportedFunc = generator.addType(1, 0) // TODO: delete that. it's for debug
    val typeExportedFunc = generator.addType(0, 0)
    generator.addImport("imports", "imported_func", typeImportedFunc) // TODO: delete that. it's for debug
    val exportedFunc = generator.addFunc(typeExportedFunc, syntaxTree)
    // IMPORTANT: !!! Add exports only after imports !!!
    generator.addExport("exported_func", exportedFunc)

    File(outputFilename).writeBytes(generator.compile())
    println("output file: $outputFilename")
}
