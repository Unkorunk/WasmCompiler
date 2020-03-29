package syntaxtree.expr

enum class Operation(val bytecode: Byte, val priority: Byte, val operationSymbol: String) {
    Add(0x6a, 2, "+"),       // i32.add
    Sub(0x6b, 2, "-"),       // i32.sub
    Mul(0x6c, 3, "*"),       // i32.mul
    Div(0x6d, 3, "/"),       // i32.div_s
    Greater(0x4a, 1, ">"),   // i32.gt_s
    Less(0x48, 1, "<"),      // i32.lt_s
    Equal(0x46, 0, "=="),    // i32.eq
    UnEqual(0x47, 0, "!=")   // i32.ne
}
