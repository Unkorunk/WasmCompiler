package parser

class Tokenizer {
    enum class Token {
        Variable,   // let                      <=> [var]
        Assign,     // =                        <=> [assign]
        Semicolon,  // ;                        <=> [sem]
        Data,       // name or expr
        RBOpen,     // round bracket open (     <=> [ropen]
        RBClose,    // round bracket close )    <=> [rclose]
        CBOpen,     // curly bracket open {     <=> [copen]
        CBClose,    // curly bracket close }    <=> [cclose]
        While,      // while                    <=> [while]
        If,         // if                       <=> [if]
        Else        // else                     <=> [else]
    }

    data class TokenData(val token: Token, val data: String?)

    companion object {
        private val transformRawToken = mapOf(
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

        private fun preparationStage(sourceText: String): String {
            var output = sourceText

            val operations = arrayOf("+", "-", "/", "*", "<", ">", "==", "!=")
            val addSpaceAround = arrayOf("(", ")", "{", "}", ";").plus(operations)
            for (entry in addSpaceAround) {
                output = output.replace(entry, " $entry ")
            }
            var tempIdx = 1
            while (tempIdx < output.length - 1) {
                if (output[tempIdx] == '=' && output[tempIdx - 1] != '!' && output[tempIdx - 1] != '=' && output[tempIdx + 1] != '=') {
                    output = output.replaceRange(tempIdx, tempIdx + 1, " = ")
                    tempIdx += 3
                } else {
                    tempIdx++
                }
            }

            return output
        }

        fun tokenize(sourceText: String): Array<TokenData> {
            val text = preparationStage(sourceText)
            val tokensRaw = text.split(Regex("\\s+"))
            return Array(tokensRaw.size) { i ->
                val token = transformRawToken.getOrDefault(tokensRaw[i], Token.Data)
                if (token == Token.Data) {
                    TokenData(token, tokensRaw[i])
                } else {
                    TokenData(token, null)
                }
            }
        }
    }
}