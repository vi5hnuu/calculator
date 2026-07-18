package com.vi5hnu.calculator.core.math

/** Evaluates an RPN token stream against a set of variable bindings. */
internal class RpnEvaluator(
    private val angleUnit: AngleUnit,
    private val variables: Map<String, Double>,
) {

    fun evaluate(rpn: List<Token>): Double {
        val stack = ArrayDeque<Double>()

        fun pop(): Double =
            stack.removeLastOrNull() ?: throw ParseException(EvalError.IncompleteExpression)

        for (token in rpn) {
            when (token) {
                is Token.Num -> stack.addLast(token.value)
                is Token.Constant -> stack.addLast(token.value)
                // An unbound name is a typo, not a zero. The design mock defaults these to 0,
                // which turns `qtyy*3` into a confident "0" instead of an error — the worst
                // thing a calculator can do. Fail loudly instead.
                is Token.Variable -> stack.addLast(
                    variables[token.name]
                        ?: throw ParseException(EvalError.UnknownIdentifier(token.name)),
                )
                is Token.Factorial -> stack.addLast(MathFunctions.factorial(pop()))
                is Token.Func -> stack.addLast(MathFunctions.call(token.name, pop(), angleUnit))
                Token.UnaryMinus -> stack.addLast(-pop())
                is Token.BinaryOp -> {
                    val right = pop()
                    val left = pop()
                    stack.addLast(apply(token.symbol, left, right))
                }
                // Structural tokens never reach RPN.
                Token.LParen, Token.RParen, Token.Comma ->
                    throw ParseException(EvalError.IncompleteExpression)
            }
        }

        // Exactly one value must remain; anything else means operands went unjoined.
        if (stack.size != 1) throw ParseException(EvalError.IncompleteExpression)
        return stack.last()
    }

    /**
     * `%` is remainder, not "percent of" — consistent with both the design mock and the
     * previous Dart engine, so existing muscle memory carries over.
     */
    private fun apply(symbol: Char, left: Double, right: Double): Double = when (symbol) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> left / right
        '%' -> left % right
        '^' -> Math.pow(left, right)
        else -> throw ParseException(EvalError.UnexpectedCharacter(symbol, -1))
    }
}
