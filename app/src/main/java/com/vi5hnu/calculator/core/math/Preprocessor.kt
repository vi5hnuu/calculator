package com.vi5hnu.calculator.core.math

/**
 * Normalises a raw token stream before parsing.
 *
 * Two jobs:
 *  - insert the multiplication that people omit, so `2pi`, `3(4)` and `(1+2)(3)` all work;
 *  - tell prefix sign from binary operator, so `-5`, `3--3` and `2^-1` all work.
 *
 * A leading `+` is simply dropped, which is what makes `+2+3` evaluate to 5 — one of the
 * cases the previous Dart engine had to be patched for.
 */
internal object Preprocessor {

    fun process(tokens: List<Token>): List<Token> {
        val out = mutableListOf<Token>()

        for (token in tokens) {
            val previous = out.lastOrNull()

            if (previous != null && previous.endsValue && token.startsValue) {
                out += Token.BinaryOp('*')
            }

            if (token is Token.BinaryOp && (token.symbol == '-' || token.symbol == '+')) {
                // A sign is prefix when nothing valuable precedes it.
                val isPrefix = previous == null ||
                    previous is Token.LParen ||
                    previous is Token.Comma ||
                    Operators.isOperator(previous)

                if (isPrefix) {
                    if (token.symbol == '-') out += Token.UnaryMinus
                    continue
                }
            }

            out += token
        }
        return out
    }
}
