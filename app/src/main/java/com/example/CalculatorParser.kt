package com.example

import kotlin.math.*

class CalculatorParser(
    private val expr: String,
    private val angleMode: String = "DEG",
    private val variables: Map<String, Double> = emptyMap()
) {
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

    private fun eatInfix(op: String): Boolean {
        while (ch == ' '.code) {
            nextChar()
        }
        var match = true
        for (i in op.indices) {
            val expectedCh = op[i].code
            val actualPos = pos + i
            val actualCh = if (actualPos < expr.length) expr[actualPos].code else -1
            if (actualCh != expectedCh) {
                match = false
                break
            }
        }
        if (match) {
            for (i in op.indices) {
                nextChar()
            }
            return true
        }
        return false
    }

    fun parse(): BigVal {
        if (expr.isBlank()) return BigVal(0.0)
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
        var x = parseCombinatorics()
        while (true) {
            when {
                eat('+'.code) -> x += parseCombinatorics()
                eat('-'.code) -> x -= parseCombinatorics()
                else -> return x
            }
        }
    }

    // combinatorics = term nCr term | term nPr term
    private fun parseCombinatorics(): BigVal {
        var x = parseTerm()
        while (true) {
            when {
                eatInfix("nCr") -> x = x.nCr(parseTerm())
                eatInfix("nPr") -> x = x.nPr(parseTerm())
                else -> return x
            }
        }
    }

    // term = factor * factor | factor / factor
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
            if (ch == 'e'.code || ch == 'E'.code) {
                val nextPos = pos + 1
                val nextCh = if (nextPos < expr.length) expr[nextPos] else null
                val nextCh2 = if (pos + 2 < expr.length) expr[pos + 2] else null
                
                val isExp = if (nextCh != null && nextCh.isDigit()) {
                    true
                } else if (nextCh != null && (nextCh == '-' || nextCh == '+') && nextCh2 != null && nextCh2.isDigit()) {
                    true
                } else {
                    false
                }
                
                if (isExp) {
                    nextChar() // consume 'e' or 'E'
                    if (ch == '-'.code || ch == '+'.code) {
                        nextChar() // consume sign
                    }
                    while (ch >= '0'.code && ch <= '9'.code) {
                        nextChar() // consume digits
                    }
                }
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
        } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || ch == '√'.code) { // named function, variable or custom symbol
            val name: String
            if (ch == '√'.code) {
                nextChar()
                name = "√"
            } else {
                while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || (ch >= '0'.code && ch <= '9'.code && startPos != pos)) {
                    nextChar()
                }
                name = expr.substring(startPos, this.pos)
            }

            // Check if variable
            val lowerName = name.lowercase()
            if (lowerName in variables.keys) {
                x = BigVal(variables[lowerName] ?: 0.0)
            } else if (lowerName == "π") {
                x = BigVal(PI)
            } else if (lowerName == "e") {
                x = BigVal(E)
            } else {
                // Parse arguments
                val args = mutableListOf<BigVal>()
                if (eat('('.code)) {
                    if (ch != ')'.code) {
                        args.add(parseExpression())
                        while (eat(','.code)) {
                            args.add(parseExpression())
                        }
                    }
                    eat(')'.code)
                } else {
                    // Single argument without parentheses (e.g. sin 30)
                    args.add(parseFactor())
                }

                x = when (lowerName) {
                    "sin" -> evaluateTrig("sin", args)
                    "cos" -> evaluateTrig("cos", args)
                    "tan" -> evaluateTrig("tan", args)
                    "asin" -> evaluateTrig("asin", args)
                    "acos" -> evaluateTrig("acos", args)
                    "atan" -> evaluateTrig("atan", args)
                    "sinh" -> args.first().sinhVal()
                    "cosh" -> args.first().coshVal()
                    "tanh" -> args.first().tanhVal()
                    "asinh" -> args.first().asinhVal()
                    "acosh" -> args.first().acoshVal()
                    "atanh" -> args.first().atanhVal()
                    "log" -> args.first().logVal()
                    "ln" -> args.first().lnVal()
                    "sqrt", "√" -> args.first().sqrtVal()
                    "cbrt" -> args.first().cbrtVal()
                    "abs" -> args.first().absVal()
                    "round" -> {
                        if (args.size >= 2) args[0].roundVal(args[1].toDouble().roundToInt())
                        else args.first().roundVal(0)
                    }
                    "ipart" -> args.first().iPartVal()
                    "fpart" -> args.first().fPartVal()
                    "int" -> args.first().intVal()
                    "min" -> {
                        if (args.size < 2) throw IllegalArgumentException("min expects 2 arguments")
                        if (args[0].compareTo(args[1]) <= 0) args[0] else args[1]
                    }
                    "max" -> {
                        if (args.size < 2) throw IllegalArgumentException("max expects 2 arguments")
                        if (args[0].compareTo(args[1]) >= 0) args[0] else args[1]
                    }
                    "mod" -> {
                        if (args.size < 2) throw IllegalArgumentException("mod expects 2 arguments")
                        args[0].modVal(args[1])
                    }
                    "lcm" -> {
                        if (args.size < 2) throw IllegalArgumentException("lcm expects 2 arguments")
                        args[0].lcmVal(args[1])
                    }
                    "gcd" -> {
                        if (args.size < 2) throw IllegalArgumentException("gcd expects 2 arguments")
                        args[0].gcdVal(args[1])
                    }
                    "nPr" -> {
                        if (args.size < 2) throw IllegalArgumentException("nPr expects 2 arguments")
                        args[0].nPr(args[1])
                    }
                    "nCr" -> {
                        if (args.size < 2) throw IllegalArgumentException("nCr expects 2 arguments")
                        args[0].nCr(args[1])
                    }
                    else -> throw IllegalArgumentException("Unknown function: $name")
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

        // Parse postfix factorial and percentage
        while (true) {
            if (eat('!'.code)) {
                x = x.factorial()
            } else if (eat('%'.code)) {
                x = x * BigVal(0.01)
            } else {
                break
            }
        }

        return x
    }

    private fun evaluateTrig(func: String, args: List<BigVal>): BigVal {
        if (args.isEmpty()) throw IllegalArgumentException("$func expects arguments")
        val arg = args.first()
        val radVal = if (angleMode == "DEG") {
            arg.toDouble() * Math.PI / 180.0
        } else {
            arg.toDouble()
        }

        val resDouble = when (func) {
            "sin" -> sin(radVal)
            "cos" -> cos(radVal)
            "tan" -> tan(radVal)
            "asin" -> {
                val r = asin(arg.toDouble())
                if (angleMode == "DEG") r * 180.0 / Math.PI else r
            }
            "acos" -> {
                val r = acos(arg.toDouble())
                if (angleMode == "DEG") r * 180.0 / Math.PI else r
            }
            "atan" -> {
                val r = atan(arg.toDouble())
                if (angleMode == "DEG") r * 180.0 / Math.PI else r
            }
            else -> throw IllegalArgumentException("Unknown trig: $func")
        }

        return BigVal(resDouble)
    }
}
