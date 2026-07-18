package com.vi5hnu.calculator.core.math

/**
 * Result of evaluating an expression.
 *
 * The engine reports *why* something failed rather than collapsing every problem into a null,
 * which is what both the design mock (`throw new Error('bad')`) and the previous Dart
 * implementation did. The UI turns these into specific messages instead of a blanket
 * "Invalid Expression".
 */
sealed interface EvalOutcome {

    data class Success(val value: Double) : EvalOutcome

    data class Failure(val error: EvalError) : EvalOutcome
}

sealed interface EvalError {

    /** Human-readable text, safe to show directly in the UI. */
    val message: String

    /** Nothing to evaluate. Callers usually render this as blank rather than as an error. */
    data object EmptyExpression : EvalError {
        override val message = ""
    }

    data class UnexpectedCharacter(val character: Char, val index: Int) : EvalError {
        override val message = "Unexpected '$character'"
    }

    data class MalformedNumber(val text: String) : EvalError {
        override val message = "Invalid number '$text'"
    }

    data class UnknownIdentifier(val name: String) : EvalError {
        override val message = "Unknown name '$name'"
    }

    data object UnbalancedParentheses : EvalError {
        override val message = "Unbalanced brackets"
    }

    /** Operators without enough operands, or operands with no operator joining them. */
    data object IncompleteExpression : EvalError {
        override val message = "Incomplete expression"
    }

    /** The maths is well-formed but undefined here, e.g. `sqrt(-1)` or `0/0`. */
    data object UndefinedResult : EvalError {
        override val message = "Undefined result"
    }
}


