package com.vi5hnu.calculator.core.math

import kotlin.math.E
import kotlin.math.PI

/**
 * Turns raw expression text into a flat token list.
 *
 * Throws [LexException] on the first thing it cannot read; [ExpressionEngine] converts that
 * into an [EvalOutcome.Failure].
 */
internal class Lexer(private val allowUnknownNames: Boolean) {

    fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < source.length) {
            val c = source[i]
            when {
                c.isWhitespace() -> i++

                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < source.length && (source[i].isDigit() || source[i] == '.')) i++
                    val text = source.substring(start, i)
                    // Rejects "1.2.3", which the design's parseFloat would silently read as 1.2.
                    val value = text.toDoubleOrNull()
                        ?: throw LexException(EvalError.MalformedNumber(text))
                    tokens += Token.Num(value)
                }

                c.isLetter() || c == '_' -> {
                    val start = i
                    while (i < source.length && (source[i].isLetterOrDigit() || source[i] == '_')) i++
                    tokens += identifier(source.substring(start, i))
                }

                c in Operators.SYMBOLS -> {
                    tokens += Token.BinaryOp(c); i++
                }

                c == '!' -> {
                    tokens += Token.Factorial; i++
                }

                c == '(' -> {
                    tokens += Token.LParen; i++
                }

                c == ')' -> {
                    tokens += Token.RParen; i++
                }

                c == ',' -> {
                    tokens += Token.Comma; i++
                }

                else -> throw LexException(EvalError.UnexpectedCharacter(c, i))
            }
        }
        return tokens
    }

    private fun identifier(raw: String): Token {
        val name = raw.lowercase()
        return when {
            name == "pi" -> Token.Constant("pi", PI)
            name == "e" -> Token.Constant("e", E)
            name in MathFunctions.NAMES -> Token.Func(name)
            // `ans` and `x` are always bindable; worksheet mode additionally allows user names.
            name == "ans" || name == "x" || allowUnknownNames -> Token.Variable(name)
            else -> throw LexException(EvalError.UnknownIdentifier(raw))
        }
    }
}

internal class LexException(val error: EvalError) : Exception(error.message)
