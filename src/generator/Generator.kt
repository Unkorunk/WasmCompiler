package generator

import syntaxtree.SyntaxTree
import utility.Leb128
import java.io.ByteArrayOutputStream

class Generator {
    enum class ValueType(val opcode: Int) {
        i32(-0x01),
//        i64(-0x02),
//        f32(-0x03),
//        f64(-0x04),
//        anyfunc(-0x10),
        func(-0x20),
//        empty(-0x40)
    }

    enum class SectionType(val code: Int) {
        Type(1),
        Import(2),
        Function(3),
//        Table(4),
//        Memory(5),
//        Global(6),
        Export(7),
//        Start(8),
//        Element(9),
        Code(10),
//        Data(11)
    }

    private val sectionCountEntry = mutableMapOf<SectionType, Int>()
    private val sectionEntries = mutableMapOf<SectionType, ByteArray>()

    private fun createModule() : ByteArray {
        return byteArrayOf(0x00, 0x61, 0x73, 0x6d) // magic number i.e., \0asm
            .plus(byteArrayOf(0x01, 0x00, 0x00, 0x00)) // version number, 0x1
    }

    private fun createSection(sectionType: SectionType, payloadData: ByteArray) : ByteArray {
        return Leb128.toUnsignedLeb128(sectionType.code) // section code
            .plus(Leb128.toUnsignedLeb128(payloadData.size)) // size of this section in bytes
            .plus(payloadData) // content of this section
    }

    fun addType(paramCount: Int, returnCount: Int) : Int {
        val bytesI32 = Leb128.toSignedLeb128(ValueType.i32.opcode)

        var inputSignature = byteArrayOf()
        for (i in 0 until paramCount) {
            inputSignature = inputSignature.plus(bytesI32)
        }

        var outputSignature = byteArrayOf()
        for (i in 0 until returnCount) {
            outputSignature = outputSignature.plus(bytesI32)
        }

        val typeIndex = sectionCountEntry.getOrDefault(SectionType.Type, 0)
        sectionCountEntry[SectionType.Type] = typeIndex + 1
        sectionEntries[SectionType.Type] = sectionEntries.getOrDefault(SectionType.Type, byteArrayOf())
            .plus(Leb128.toSignedLeb128(ValueType.func.opcode))
            .plus(Leb128.toUnsignedLeb128(paramCount))
            .plus(inputSignature)
            .plus(Leb128.toUnsignedLeb128(returnCount))
            .plus(outputSignature)
        return typeIndex
    }

    fun addImport(moduleStr: String, fieldStr: String, typeIndex: Int) {
        val importIndex = sectionCountEntry.getOrDefault(SectionType.Import, 0)
        sectionCountEntry[SectionType.Import] = importIndex + 1

        val moduleStrBytes = moduleStr.encodeToByteArray()
        val fieldStrBytes = fieldStr.encodeToByteArray()

        sectionEntries[SectionType.Import] = sectionEntries.getOrDefault(SectionType.Import, byteArrayOf())
            .plus(Leb128.toUnsignedLeb128(moduleStrBytes.size)) // length of module_str in bytes
            .plus(moduleStrBytes) // module name: valid UTF-8 byte sequence
            .plus(Leb128.toUnsignedLeb128(fieldStrBytes.size)) // length of field_str in bytes
            .plus(fieldStrBytes) // field name: valid UTF-8 byte sequence
            .plus(0) // the kind of definition being imported = Function
            .plus(Leb128.toUnsignedLeb128(typeIndex)) // type index of the function signature
    }

    fun addFunc(typeIndex: Int, syntaxTree: SyntaxTree) : Int {
        val funcIndex = sectionCountEntry.getOrDefault(SectionType.Function, 0)
        sectionCountEntry[SectionType.Function] = funcIndex + 1

        sectionEntries[SectionType.Function] = sectionEntries.getOrDefault(SectionType.Function, byteArrayOf())
            .plus(Leb128.toUnsignedLeb128(typeIndex)) // type index of the function signature

        val bodyFunc = ByteArrayOutputStream()
        bodyFunc.write(Leb128.toUnsignedLeb128(1)) // number of local entries = 1
        bodyFunc.write(Leb128.toUnsignedLeb128(syntaxTree.getMaxIndex())) // local variables
        bodyFunc.write(Leb128.toSignedLeb128(ValueType.i32.opcode)) // bytecode of the function
        bodyFunc.write(syntaxTree.compile())

        val codeIndex = sectionCountEntry.getOrDefault(SectionType.Code, 0)
        sectionCountEntry[SectionType.Code] = codeIndex + 1
        sectionEntries[SectionType.Code] = sectionEntries.getOrDefault(SectionType.Code, byteArrayOf())
            .plus(Leb128.toUnsignedLeb128(bodyFunc.size())) // size of function body to follow, in bytes
            .plus(bodyFunc.toByteArray())

        return funcIndex
    }

    fun addExport(fieldStr: String, functionIndex: Int) {
        val importIndex = sectionCountEntry.getOrDefault(SectionType.Function, 0)

        val fieldStrBytes = fieldStr.encodeToByteArray()

        val exportIndex = sectionCountEntry.getOrDefault(SectionType.Export, 0)
        sectionCountEntry[SectionType.Export] = exportIndex + 1

        sectionEntries[SectionType.Export] = sectionEntries.getOrDefault(SectionType.Export, byteArrayOf())
            .plus(Leb128.toUnsignedLeb128(fieldStrBytes.size))
            .plus(fieldStrBytes)
            .plus(0) // the kind of definition being exported = function
            .plus(Leb128.toUnsignedLeb128(importIndex + functionIndex))
    }

    fun compile() : ByteArray {
        val outputStream = ByteArrayOutputStream()

        outputStream.write(createModule())

        for (sectionType in SectionType.values()) {
            val countEntry = sectionCountEntry[sectionType]
            val entries = sectionEntries[sectionType]
            if (countEntry != null && entries != null) {
                outputStream.write(createSection(sectionType, Leb128.toUnsignedLeb128(countEntry).plus(entries)))
            }
        }

        return outputStream.toByteArray()
    }
}