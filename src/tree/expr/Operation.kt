package tree.expr

enum class Operation(val bytecode: Byte) {
    Add(0x6a),
    Sub(0x6b),
    Mul(0x6c),
    Div(0x6d),
    Greater(0x4a), // i32.gt_s
    Less(0x4c), // i32.le_s
    Equal(0x46), // i32.eq
    UnEqual(0x47) // i32.ne
}