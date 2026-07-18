package com.vi5hnu.calculator.core.math

/**
 * A single lexical unit of an expression.
 *
 * The engine pipeline is Lexer -> Preprocessor -> ShuntingYard -> RpnEvaluator, and every
 * stage speaks in these tokens.
 */
internal sealed interface Token {

    data class Num(val value: Double) : Token

    /** A named literal such as `pi` or `e`, resolved at lex time. */
    data class Constant(val symbol: String, val value: Double) : Token

    /** `x`, `ans`, or a worksheet-defined name. Resolved at evaluation time. */
    data class Variable(val name: String) : Token

    data class Func(val name: String) : Token

    /** One of `+ - * / % ^`. */
    data class BinaryOp(val symbol: Char) : Token

    /** Prefix negation, distinct from the binary `-`. Produced by the Preprocessor. */
    data object UnaryMinus : Token

    /** Postfix `!`. */
    data object Factorial : Token

    data object LParen : Token
    data object RParen : Token
    data object Comma : Token
}

/**
 * True when this token can legally end a value, i.e. something may be implicitly multiplied
 * after it. `2pi`, `(1+2)(3)`, and `5!2` all hinge on this.
 */
internal val Token.endsValue: Boolean
    get() = this is Token.Num ||
        this is Token.Constant ||
        this is Token.Variable ||
        this is Token.RParen ||
        this is Token.Factorial

/** True when this token can legally begin a value. */
internal val Token.startsValue: Boolean
    get() = this is Token.Num ||
        this is Token.Constant ||
        this is Token.Variable ||
        this is Token.Func ||
        this is Token.LParen

internal object Operators {

    const val SYMBOLS = "+-*/%^"

    /**
     * Binding power. Unary minus deliberately sits *below* `^` so that `-2^2` parses as
     * `-(2^2)` = -4, which is the standard mathematical reading.
     */
    fun precedence(token: Token): Int = when (token) {
        is Token.BinaryOp -> when (token.symbol) {
            '+', '-' -> 1
            '*', '/', '%' -> 2
            '^' -> 4
            else -> error("Unhandled operator ${token.symbol}")
        }

        Token.UnaryMinus -> 3
        else -> error("Not an operator: $token")
    }

    /** `^` is right-associative: `2^3^2` is `2^(3^2)` = 512, not `(2^3)^2` = 64. */
    fun isRightAssociative(token: Token): Boolean =
        token == Token.UnaryMinus || (token is Token.BinaryOp && token.symbol == '^')

    fun isOperator(token: Token): Boolean =
        token is Token.BinaryOp || token == Token.UnaryMinus
}
