package com.vi5hnu.calculator.core.math

/**
 * Dijkstra's shunting-yard, converting infix tokens to Reverse Polish Notation.
 *
 * One deliberate divergence from the design's JavaScript: a prefix operator is pushed
 * *without* first draining higher-precedence operators from the stack. A prefix operator
 * appears exactly where an operand is expected, so there is never anything to its left ready
 * to reduce. The mock pops eagerly here, which corrupts the stack and makes `2^-3` evaluate
 * to NaN. Pushing directly yields the correct 0.125 while leaving `-2^2 = -4` intact.
 */
internal object ShuntingYard {

    fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when {
                token is Token.Num || token is Token.Constant || token is Token.Variable ->
                    output += token

                // Postfix: its operand is already emitted, so it can go straight out.
                token is Token.Factorial -> output += token

                token is Token.Func -> stack.addLast(token)

                token is Token.Comma -> {
                    while (stack.lastOrNull().let { it != null && it !is Token.LParen }) {
                        output += stack.removeLast()
                    }
                }

                token == Token.UnaryMinus -> stack.addLast(token)

                token is Token.BinaryOp -> {
                    while (true) {
                        val top = stack.lastOrNull()
                        if (top == null || !Operators.isOperator(top)) break

                        val shouldPop = if (Operators.isRightAssociative(token)) {
                            Operators.precedence(token) < Operators.precedence(top)
                        } else {
                            Operators.precedence(token) <= Operators.precedence(top)
                        }
                        if (!shouldPop) break
                        output += stack.removeLast()
                    }
                    stack.addLast(token)
                }

                token is Token.LParen -> stack.addLast(token)

                token is Token.RParen -> {
                    while (stack.lastOrNull().let { it != null && it !is Token.LParen }) {
                        output += stack.removeLast()
                    }
                    if (stack.isEmpty()) throw ParseException(EvalError.UnbalancedParentheses)
                    stack.removeLast() // discard the '('
                    // A function call closes with its bracket.
                    if (stack.lastOrNull() is Token.Func) output += stack.removeLast()
                }
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LParen) throw ParseException(EvalError.UnbalancedParentheses)
            output += top
        }
        return output
    }
}

internal class ParseException(val error: EvalError) : Exception(error.message)
