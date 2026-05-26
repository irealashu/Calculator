package com.example

import kotlin.math.*

class CalculatorParser(private val expr: String) {
    private var pos = -1
    private var ch = 0

    private fun nextChar() {
        pos++
        ch = if (pos < expr.length) expr[pos].code else -1
    }

    private fun eat(charToEat: Int): Boolean {
        while (ch == ' '.code) {
            nextChar()
        }
        if (ch == charToEat) {
            nextChar()
            return true
        }
        return false
    }

    fun parse(): BigVal {
        if (expr.isBlank()) return BigVal(0.0)
        // Clean expressions from spaces and handle empty/blank lines
        nextChar()
        val x = parseExpression()
        while (ch == ' '.code) nextChar()
        if (pos < expr.length) {
            throw IllegalArgumentException("Unexpected: '${ch.toChar()}'")
        }
        return x
    }

    // expression = term + term | term - term
    private fun parseExpression(): BigVal {
        var x = parseTerm()
        while (true) {
            when {
                eat('+'.code) -> x += parseTerm()
                eat('-'.code) -> x -= parseTerm()
                else -> return x
            }
        }
    }

    // term = factor * factor | factor / factor | factor % factor
    private fun parseTerm(): BigVal {
        var x = parseFactor()
        while (true) {
            when {
                eat('*'.code) || eat('×'.code) -> x *= parseFactor()
                eat('/'.code) || eat('÷'.code) -> {
                    val divisor = parseFactor()
                    if (divisor.mantissa == 0.0) throw ArithmeticException("Divide by zero")
                    x /= divisor
                }
                else -> return x
            }
        }
    }

    // factor = unary | parentheses | number | function | constant | exponentiation
    private fun parseFactor(): BigVal {
        if (eat('+'.code)) return parseFactor() // unary plus
        if (eat('-'.code)) return -parseFactor() // unary minus

        var x: BigVal
        val startPos = this.pos

        if (eat('('.code)) { // parentheses
            x = parseExpression()
            eat(')'.code)
        } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
            while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                nextChar()
            }
            val numStr = expr.substring(startPos, this.pos)
            val d = numStr.toDoubleOrNull() ?: 0.0
            x = BigVal(d)
        } else if (ch == 'π'.code) {
            nextChar()
            x = BigVal(PI)
        } else if (ch == 'e'.code) {
            nextChar()
            x = BigVal(E)
        } else if ((ch >= 'a'.code && ch <= 'z'.code) || ch == '√'.code) { // named function or custom symbol
            val name: String
            if (ch == '√'.code) {
                nextChar()
                name = "√"
            } else {
                while (ch >= 'a'.code && ch <= 'z'.code) {
                    nextChar()
                }
                name = expr.substring(startPos, this.pos)
            }

            if (name == "π") {
                x = BigVal(PI)
            } else if (name == "e") {
                x = BigVal(E)
            } else {
                val arg = parseFactor()
                x = when (name) {
                    "sin" -> arg.sinVal()
                    "cos" -> arg.cosVal()
                    "tan" -> arg.tanVal()
                    "log" -> arg.logVal()
                    "ln" -> arg.lnVal()
                    "sqrt", "√" -> arg.sqrtVal()
                    else -> throw IllegalArgumentException("Unknown: $name")
                }
            }
        } else {
            throw IllegalArgumentException("Invalid layout")
        }

        // Parse exponent
        if (eat('^'.code)) {
            val power = parseFactor()
            x = x.pow(power)
        }

        // Parse postfix percentage percentage divider
        while (eat('%'.code)) {
            x = x * BigVal(0.01)
        }

        return x
    }
}
