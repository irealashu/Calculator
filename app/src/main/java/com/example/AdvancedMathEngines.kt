package com.example

import kotlin.math.*
import java.util.Locale

// ====================================================================
// 1. CHIEF FRACTION & REPEATING DECIMAL ENGINE
// ====================================================================

data class Fraction(val num: Long, val den: Long) {
    init {
        if (den == 0L) throw ArithmeticException("Denominator cannot be zero")
    }

    fun simplify(): Fraction {
        val g = gcd(abs(num), abs(den))
        val s = if (den < 0) -1 else 1
        return Fraction((num / g) * s, (den / g) * s)
    }

    operator fun plus(o: Fraction) = Fraction(num * o.den + o.num * den, den * o.den).simplify()
    operator fun minus(o: Fraction) = Fraction(num * o.den - o.num * den, den * o.den).simplify()
    operator fun times(o: Fraction) = Fraction(num * o.num, den * o.den).simplify()
    operator fun div(o: Fraction) = Fraction(num * o.den, den * o.num).simplify()

    fun toDouble(): Double = num.toDouble() / den.toDouble()

    fun toMixedString(): String {
        val s = simplify()
        if (s.den == 1L) return s.num.toString()
        val absNum = abs(s.num)
        val whole = absNum / s.den
        val rem = absNum % s.den
        val sign = if (s.num < 0) "-" else ""
        return if (whole == 0L) {
            "${sign}${rem}/${s.den}"
        } else {
            "${sign}${whole}_${rem}/${s.den}"
        }
    }

    override fun toString(): String {
        val s = simplify()
        return if (s.den == 1L) s.num.toString() else "${s.num}/${s.den}"
    }

    companion object {
        private fun gcd(a: Long, b: Long): Long {
            var x = a
            var y = b
            while (y != 0L) {
                val temp = y
                y = x % y
                x = temp
            }
            return x
        }

        fun fromDouble(d: Double, maxDen: Long = 10000L): Fraction {
            if (d.isInfinite() || d.isNaN()) throw IllegalArgumentException("Invalid float")
            val sign = if (d < 0) -1 else 1
            val value = abs(d)
            if (value > maxDen) {
                return Fraction(value.toLong() * sign, 1L)
            }
            
            val h1 = floor(value).toLong()
            var h = h1
            var k = 1L
            
            var h2 = 1L
            var k2 = 0L
            
            var x = value - h1
            if (x < 1e-12) {
                return Fraction(h * sign, k)
            }
            
            var iterations = 0
            while (x > 1e-12 && iterations < 100) {
                iterations++
                val nextVal = 1.0 / x
                if (nextVal.isInfinite() || nextVal.isNaN()) break
                val a = floor(nextVal).toLong()
                if (a < 0L) break
                
                val prevH = h
                val prevK = k
                
                // Safe check for overflow before multiplication
                val checkH = if (a != 0L && h > Long.MAX_VALUE / a) Long.MAX_VALUE else a * h + h2
                val checkK = if (a != 0L && k > Long.MAX_VALUE / a) Long.MAX_VALUE else a * k + k2
                
                if (checkK > maxDen || checkK < 0L || checkH < 0L) {
                    break
                }
                
                h = checkH
                k = checkK
                
                h2 = prevH
                k2 = prevK
                
                val nextDiff = nextVal - a
                if (nextDiff == 0.0) break
                x = nextDiff
            }
            return Fraction(h * sign, k).simplify()
        }
    }
}

/**
 * Converts a decimal to a repeating decimal representation, e.g. 0.33333333 -> 0.3̄
 */
fun toRepeatingDecimal(d: Double): String {
    if (d.isNaN() || d.isInfinite()) return d.toString()
    val frac = Fraction.fromDouble(d)
    if (frac.den == 1L) return frac.num.toString()
    
    val whole = frac.num / frac.den
    val rem = abs(frac.num % frac.den)
    
    // Perform standard hand division tracking remainders to spot cycles
    val seenRemainders = mutableMapOf<Long, Int>()
    val digits = StringBuilder()
    var r = rem
    var cycleStart = -1
    
    var iterations = 0
    while (r != 0L && iterations < 1000) {
        iterations++
        if (seenRemainders.containsKey(r)) {
            cycleStart = seenRemainders[r]!!
            break
        }
        seenRemainders[r] = digits.length
        r *= 10
        digits.append(r / frac.den)
        r %= frac.den
    }
    
    val fracPart = if (cycleStart == -1) {
        digits.toString()
    } else {
        val nonRepeat = digits.substring(0, cycleStart)
        val repeat = digits.substring(cycleStart)
        if (repeat == "0") nonRepeat else "${nonRepeat}${repeat}̄"
    }
    val sign = if (d < 0 && whole == 0L) "-" else ""
    return if (fracPart.isEmpty()) "$whole" else "$sign$whole.$fracPart"
}

// ====================================================================
// 2. ALGEBRA & SYMBOLIC CAS ENGINE
// ====================================================================

object CAS {
    fun simplifyAlgebraicProduct(expr1: String, expr2: String): String {
        // Simple polynomial multiplication helper for polynomials like (ax + b) * (cx + d)
        // Parses (ax + b) and (cx + d) and returns exact string
        val r1 = parseLinear(expr1) ?: return "($expr1)*($expr2)"
        val r2 = parseLinear(expr2) ?: return "($expr1)*($expr2)"
        
        val a = r1.first
        val b = r1.second
        val c = r2.first
        val d = r2.second
        
        val ac = a * c
        val adbc = a * d + b * c
        val bd = b * d
        
        val sb = StringBuilder()
        if (ac != 0.0) {
            sb.append("${formatCoeff(ac)}x²")
        }
        if (adbc != 0.0) {
            val sign = if (adbc > 0 && sb.isNotEmpty()) " + " else if (adbc < 0) " - " else ""
            sb.append("$sign${formatCoeff(abs(adbc))}x")
        }
        if (bd != 0.0) {
            val sign = if (bd > 0 && sb.isNotEmpty()) " + " else if (bd < 0) " - " else ""
            sb.append("$sign${formatCoeff(abs(bd))}")
        }
        return sb.toString().ifEmpty { "0" }
    }

    private fun parseLinear(s: String): Pair<Double, Double>? {
        // Strip parenthesis
        val clean = s.replace("(", "").replace(")", "").replace(" ", "")
        // Matches forms: ax+b, x+b, -x+b, etc.
        val regex = "^(-?\\d*\\.?\\d*)x([+-]\\d*\\.?\\d*)?$".toRegex()
        val match = regex.matchEntire(clean) ?: return null
        
        val aStr = match.groups[1]?.value ?: ""
        val a = when (aStr) {
            "" -> 1.0
            "-" -> -1.0
            else -> aStr.toDoubleOrNull() ?: 1.0
        }
        
        val bStr = match.groups[2]?.value ?: ""
        val b = if (bStr.isEmpty()) 0.0 else bStr.toDoubleOrNull() ?: 0.0
        return Pair(a, b)
    }

    private fun formatCoeff(v: Double): String {
        if (v == 1.0) return ""
        if (v == -1.0) return "-"
        if (v == v.toLong().toDouble()) return v.toLong().toString()
        return v.toString()
    }

    // Quadratic Roots, Cubic & Quartic analytical solvers
    fun solveQuadratic(a: Double, b: Double, c: Double): List<String> {
        if (a == 0.0) {
            if (b == 0.0) return listOf("No Solution")
            return listOf((-c / b).toString())
        }
        val d = b * b - 4 * a * c
        return if (d >= 0.0) {
            val r1 = (-b + sqrt(d)) / (2 * a)
            val r2 = (-b - sqrt(d)) / (2 * a)
            listOf(r1.toString(), r2.toString())
        } else {
            val real = -b / (2 * a)
            val imag = sqrt(-d) / (2 * a)
            listOf("$real + ${imag}i", "$real - ${imag}i")
        }
    }

    fun solveCubic(a: Double, b: Double, c: Double, d: Double): List<String> {
        if (a == 0.0) return solveQuadratic(b, c, d)
        // Normalize terms: x³ + Ax² + Bx + C = 0
        val bigA = b / a
        val bigB = c / a
        val bigC = d / a
        
        val q = (3 * bigB - bigA * bigA) / 9.0
        val r = (9 * bigA * bigB - 27 * bigC - 2 * bigA * bigA * bigA) / 54.0
        val discriminant = q * q * q + r * r
        
        val results = mutableListOf<String>()
        if (discriminant >= 0) {
            val s = sign(r + sqrt(discriminant)) * abs(r + sqrt(discriminant)).pow(1.0/3.0)
            val t = sign(r - sqrt(discriminant)) * abs(r - sqrt(discriminant)).pow(1.0/3.0)
            
            val r1 = -bigA / 3.0 + (s + t)
            results.add(String.format(Locale.US, "%.5f", r1))
            
            val realPart = -bigA / 3.0 - (s + t) / 2.0
            val imagPart = sqrt(3.0) * (s - t) / 2.0
            if (abs(imagPart) < 1e-9) {
                results.add(String.format(Locale.US, "%.5f", realPart))
                results.add(String.format(Locale.US, "%.5f", realPart))
            } else {
                results.add(String.format(Locale.US, "%.5f + %.5fi", realPart, abs(imagPart)))
                results.add(String.format(Locale.US, "%.5f - %.5fi", realPart, abs(imagPart)))
            }
        } else {
            val theta = acos(r / sqrt(-q * q * q))
            val r1 = 2 * sqrt(-q) * cos(theta / 3.0) - bigA / 3.0
            val r2 = 2 * sqrt(-q) * cos((theta + 2 * PI) / 3.0) - bigA / 3.0
            val r3 = 2 * sqrt(-q) * cos((theta + 4 * PI) / 3.0) - bigA / 3.0
            results.add(String.format(Locale.US, "%.5f", r1))
            results.add(String.format(Locale.US, "%.5f", r2))
            results.add(String.format(Locale.US, "%.5f", r3))
        }
        return results
    }
}

// ====================================================================
// 3. ADVANCED CALCULUS & ANALYSIS ENGINE
// ====================================================================

object Calculus {
    // 1st Numerical derivative using central differences logic
    fun numericalDerivative(expr: String, xPoint: Double, angleMode: String = "DEG"): Double {
        val h = 1e-5
        val fPlus = evaluateAtX(expr, xPoint + h, angleMode)
        val fMinus = evaluateAtX(expr, xPoint - h, angleMode)
        return (fPlus - fMinus) / (2.0 * h)
    }

    // 1D Definite numerical integration using Adaptive Simpson rule
    fun numericalIntegration(expr: String, a: Double, b: Double, angleMode: String = "DEG"): Double {
        val intervals = 100
        val h = (b - a) / intervals
        var sum = evaluateAtX(expr, a, angleMode) + evaluateAtX(expr, b, angleMode)
        
        for (i in 1 until intervals) {
            val x = a + i * h
            val factor = if (i % 2 == 0) 2.0 else 4.0
            sum += factor * evaluateAtX(expr, x, angleMode)
        }
        return sum * h / 3.0
    }

    // 2D Numerical integration for double integrals
    fun doubleIntegral(expr: String, xMin: Double, xMax: Double, yMin: Double, yMax: Double, angleMode: String = "DEG"): Double {
        val steps = 30
        val hx = (xMax - xMin) / steps
        val hy = (yMax - yMin) / steps
        var sum = 0.0
        for (i in 0 until steps) {
            val x = xMin + (i + 0.5) * hx
            for (j in 0 until steps) {
                val y = yMin + (j + 0.5) * hy
                // Evaluate with both variables mapping
                val resVal = evaluateAtXY(expr, x, y, angleMode)
                sum += resVal
            }
        }
        return sum * hx * hy
    }

    // Local Limits Engine
    fun evaluateLimit(expr: String, xPoint: Double, fromLeft: Boolean, angleMode: String = "DEG"): Double {
        val h = if (fromLeft) -1e-6 else 1e-6
        return evaluateAtX(expr, xPoint + h, angleMode)
    }

    // Taylor series expansion representation for sine & cosine & exp
    fun buildTaylorExpansionString(func: String, terms: Int): String {
        val sb = StringBuilder()
        when (func.lowercase()) {
            "sin" -> {
                for (n in 0 until terms) {
                    val sign = if (n % 2 == 0) "+" else "-"
                    val fact = factorial(2 * n + 1)
                    val exp = 2 * n + 1
                    if (n == 0) sb.append("x")
                    else sb.append(" $sign x^$exp/$fact")
                }
            }
            "cos" -> {
                for (n in 0 until terms) {
                    val sign = if (n % 2 == 0) "+" else "-"
                    val fact = factorial(2 * n)
                    val exp = 2 * n
                    if (n == 0) sb.append("1")
                    else sb.append(" $sign x^$exp/$fact")
                }
            }
            "exp" -> {
                for (n in 0 until terms) {
                    val fact = factorial(n)
                    if (n == 0) sb.append("1")
                    else sb.append(" + x^$n/$fact")
                }
            }
        }
        return sb.toString()
    }

    private fun factorial(n: Int): Long {
        var r = 1L
        for (i in 2..n) r *= i
        return r
    }

    private fun evaluateAtX(expr: String, xVal: Double, angleMode: String): Double {
        return try {
            val parser = CalculatorParser(expr, angleMode, mapOf("x" to xVal))
            parser.parse().toDouble()
        } catch (e: Exception) {
            Double.NaN
        }
    }

    private fun evaluateAtXY(expr: String, xVal: Double, yVal: Double, angleMode: String): Double {
        return try {
            val parser = CalculatorParser(expr, angleMode, mapOf("x" to xVal, "y" to yVal))
            parser.parse().toDouble()
        } catch (e: Exception) {
            Double.NaN
        }
    }
}

// ====================================================================
// 4. ADVANCED LINEAR ALGEBRA ENGINE (MATRICES & VECTORS)
// ====================================================================

class Matrix(val data: Array<DoubleArray>) {
    val rows = data.size
    val cols = if (rows > 0) data[0].size else 0

    init {
        for (row in data) {
            if (row.size != cols) throw IllegalArgumentException("Unequal column lengths")
        }
    }

    fun transpose(): Matrix {
        val result = Array(cols) { DoubleArray(rows) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[j][i] = data[i][j]
            }
        }
        return Matrix(result)
    }

    operator fun plus(o: Matrix): Matrix {
        if (rows != o.rows || cols != o.cols) throw IllegalArgumentException("Matrices dim mismatch")
        val result = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = data[i][j] + o.data[i][j]
            }
        }
        return Matrix(result)
    }

    operator fun minus(o: Matrix): Matrix {
        if (rows != o.rows || cols != o.cols) throw IllegalArgumentException("Matrices dim mismatch")
        val result = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                result[i][j] = data[i][j] - o.data[i][j]
            }
        }
        return Matrix(result)
    }

    operator fun times(o: Matrix): Matrix {
        if (cols != o.rows) throw IllegalArgumentException("Dimension mismatch for multiplication")
        val result = Array(rows) { DoubleArray(o.cols) }
        for (i in 0 until rows) {
            for (j in 0 until o.cols) {
                var sum = 0.0
                for (k in 0 until cols) {
                    sum += data[i][k] * o.data[k][j]
                }
                result[i][j] = sum
            }
        }
        return Matrix(result)
    }

    fun determinant(): Double {
        if (rows != cols) throw IllegalArgumentException("Determinant expects square matrix")
        return solveDeterminant(data)
    }

    private fun solveDeterminant(mat: Array<DoubleArray>): Double {
        val n = mat.size
        if (n == 1) return mat[0][0]
        if (n == 2) return mat[0][0] * mat[1][1] - mat[0][1] * mat[1][0] // standard 2x2 formula
        
        var det = 0.0
        for (j in 0 until n) {
            val sub = Array(n - 1) { DoubleArray(n - 1) }
            for (i in 1 until n) {
                var subCol = 0
                for (k in 0 until n) {
                    if (k == j) continue
                    sub[i - 1][subCol] = mat[i][k]
                    subCol++
                }
            }
            val sign = if (j % 2 == 0) 1.0 else -1.0
            det += sign * mat[0][j] * solveDeterminant(sub)
        }
        return det
    }

    fun trace(): Double {
        val limit = min(rows, cols)
        var sum = 0.0
        for (i in 0 until limit) {
            sum += data[i][i]
        }
        return sum
    }

    fun invert(): Matrix {
        if (rows != cols) throw IllegalArgumentException("Inversion expects square matrix")
        val n = rows
        val temp = Array(n) { DoubleArray(n * 2) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                temp[i][j] = data[i][j]
            }
            temp[i][n + i] = 1.0
        }
        
        // Gauss-Jordan elimination
        for (i in 0 until n) {
            var pivotRow = i
            while (pivotRow < n && temp[pivotRow][i] == 0.0) pivotRow++
            if (pivotRow == n) throw ArithmeticException("Matrix is singular (no inverse matches)")
            
            if (pivotRow != i) {
                val rowTemp = temp[i]
                temp[i] = temp[pivotRow]
                temp[pivotRow] = rowTemp
            }
            
            val scale = temp[i][i]
            for (j in 0 until n * 2) {
                temp[i][j] /= scale
            }
            
            for (k in 0 until n) {
                if (k == i) continue
                val factor = temp[k][i]
                for (j in 0 until n * 2) {
                    temp[k][j] -= factor * temp[i][j]
                }
            }
        }
        
        val inv = Array(n) { DoubleArray(n) }
        for (i in 0 until n) {
            for (j in 0 until n) {
                inv[i][j] = temp[i][n + j]
            }
        }
        return Matrix(inv)
    }

    fun rref(): Matrix {
        val n = rows
        val m = cols
        val temp = Array(n) { DoubleArray(m) }
        for (i in 0 until n) temp[i] = data[i].clone()
        
        var lead = 0
        for (r in 0 until n) {
            if (lead >= m) break
            var i = r
            while (temp[i][lead] == 0.0) {
                i++
                if (i == n) {
                    i = r
                    lead++
                    if (lead == m) break
                }
            }
            if (lead == m) break
            
            val rTemp = temp[i]
            temp[i] = temp[r]
            temp[r] = rTemp
            
            val scale = temp[r][lead]
            if (scale != 0.0) {
                for (j in 0 until m) temp[r][j] /= scale
            }
            
            for (k in 0 until n) {
                if (k == r) continue
                val factor = temp[k][lead]
                for (j in 0 until m) {
                    temp[k][j] -= factor * temp[r][j]
                }
            }
            lead++
        }
        return Matrix(temp)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        for (i in 0 until rows) {
            sb.append(data[i].joinToString("  ") { String.format(Locale.US, "%.3f", it) })
            sb.append("\n")
        }
        return sb.toString().trimEnd()
    }
}

// 2D/3D Vector Calculus Helpers
data class VectorMath(val data: DoubleArray) {
    fun magnitude(): Double {
        var sum = 0.0
        for (v in data) sum += v * v
        return sqrt(sum)
    }

    fun dot(o: VectorMath): Double {
        var sum = 0.0
        for (i in 0 until min(data.size, o.data.size)) {
            sum += data[i] * o.data[i]
        }
        return sum
    }

    fun cross3D(o: VectorMath): VectorMath {
        if (data.size < 3 || o.data.size < 3) throw IllegalArgumentException("Cross product expects 3D vectors")
        val r = DoubleArray(3)
        r[0] = data[1] * o.data[2] - data[2] * o.data[1]
        r[1] = data[2] * o.data[0] - data[0] * o.data[2]
        r[2] = data[0] * o.data[1] - data[1] * o.data[0]
        return VectorMath(r)
    }

    fun angleWith(o: VectorMath): Double {
        val m1 = magnitude()
        val m2 = o.magnitude()
        if (m1 == 0.0 || m2 == 0.0) return 0.0
        val valCos = dot(o) / (m1 * m2)
        return acos(valCos.coerceIn(-1.0, 1.0)) * 180.0 / PI
    }
}

// ====================================================================
// 5. COMPLEX NUMBERS ENGINE
// ====================================================================

data class Complex(val real: Double, val imag: Double) {
    operator fun plus(o: Complex) = Complex(real + o.real, imag + o.imag)
    operator fun minus(o: Complex) = Complex(real - o.real, imag - o.imag)
    operator fun times(o: Complex) = Complex(real * o.real - imag * o.imag, real * o.imag + imag * o.real)
    operator fun div(o: Complex): Complex {
        val denom = o.real * o.real + o.imag * o.imag
        if (denom == 0.0) throw ArithmeticException("Divide by zero in complex plane")
        return Complex(
            (real * o.real + imag * o.imag) / denom,
            (imag * o.real - real * o.imag) / denom
        )
    }

    fun conjugate() = Complex(real, -imag)
    fun magnitude() = sqrt(real * real + imag * imag)
    fun phaseVal() = atan2(imag, real) // Angle/phase in radians
    
    // Polar conversion info
    fun formatPolar(): String {
        val r = magnitude()
        val theta = phaseVal() * 180.0 / PI // display degree phase
        return String.format(Locale.US, "%.4f ∠ %.2f°", r, theta)
    }

    fun pow(p: Double): Complex {
        val r = magnitude()
        val theta = phaseVal()
        val rP = r.pow(p)
        val thetaP = theta * p
        return Complex(rP * cos(thetaP), rP * sin(thetaP))
    }

    override fun toString(): String {
        return if (imag >= 0.0) {
            String.format(Locale.US, "%.4f + %.4fi", real, imag)
        } else {
            String.format(Locale.US, "%.4f - %.4fi", real, abs(imag))
        }
    }

    companion object {
        fun logComplex(c: Complex): Complex {
            val r = c.magnitude()
            val theta = c.phaseVal()
            return Complex(ln(r), theta)
        }

        fun sinComplex(c: Complex): Complex {
            // sin(a + bi) = sin(a)cosh(b) + i cos(a)sinh(b)
            return Complex(sin(c.real) * cosh(c.imag), cos(c.real) * sinh(c.imag))
        }
    }
}

// ====================================================================
// 6. STATISTICS, DESCRIPTIVE & PROBABILITY DISTRIBUTIONS
// ====================================================================

object Statistics {
    fun descriptiveStats(data: List<Double>): Map<String, Double> {
        if (data.isEmpty()) return emptyMap()
        val sorted = data.sorted()
        val count = sorted.size
        val sum = sorted.sum()
        val mean = sum / count
        
        val median = if (count % 2 == 0) {
            (sorted[count / 2 - 1] + sorted[count / 2]) / 2.0
        } else {
            sorted[count / 2]
        }
        
        var sqDiffSum = 0.0
        for (v in sorted) {
            sqDiffSum += (v - mean).pow(2)
        }
        val variance = if (count > 1) sqDiffSum / (count - 1) else 0.0
        val sd = sqrt(variance)
        
        // Find quartiles
        val q1 = sorted[floor(count * 0.25).toInt().coerceAtMost(count - 1)]
        val q3 = sorted[floor(count * 0.75).toInt().coerceAtMost(count - 1)]
        val iqr = q3 - q1
        
        return mapOf(
            "Mean" to mean,
            "Median" to median,
            "SD" to sd,
            "Variance" to variance,
            "Min" to sorted.first(),
            "Max" to sorted.last(),
            "Q1" to q1,
            "Q3" to q3,
            "IQR" to iqr
        )
    }

    // Regressions: linear, quadratic, exponential, power
    fun linearRegression(x: List<Double>, y: List<Double>): String {
        val n = min(x.size, y.size)
        if (n < 2) return "Insufficient points (need at least 2)"
        
        val meanX = x.sum() / n
        val meanY = y.sum() / n
        
        var num = 0.0
        var denX = 0.0
        var denY = 0.0
        for (i in 0 until n) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            num += dx * dy
            denX += dx * dx
            denY += dy * dy
        }
        val m = if (denX != 0.0) num / denX else 0.0
        val intercept = meanY - m * meanX
        
        val denom = sqrt(denX * denY)
        val r = if (denom != 0.0) num / denom else 0.0
        val r2 = r * r
        
        return String.format(
            Locale.US,
            "Equation: y = %.4f * x + %.4f\n\nSlope (m): %.5f\nIntercept (c): %.5f\nCorrelation (r): %.5f\nDet. (r²): %.5f",
            m, intercept, m, intercept, r, r2
        )
    }

    // Normal probability distributions
    fun normalCDF(x: Double, mean: Double = 0.0, sd: Double = 1.0): Double {
        val z = (x - mean) / sd
        return 0.5 * (1.0 + erfVal(z / sqrt(2.0)))
    }

    // Bounded erf approximation
    private fun erfVal(z: Double): Double {
        val t = 1.0 / (1.0 + 0.5 * abs(z))
        val ans = 1.0 - t * exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 + t * 0.17087277)))))))))
        return if (z >= 0) ans else -ans
    }
}

// ====================================================================
// 7. FINANCIAL TVM MATHEMATICS ENGINE
// ====================================================================

object TVM {
    // Computes FV based on PV, PMT, I (annual interest rate in percentage), N
    fun solveFV(pv: Double, pmt: Double, iPr: Double, n: Double): Double {
        val rate = iPr / 100.0
        if (rate == 0.0) return -(pv + pmt * n)
        val fv = -pv * (1 + rate).pow(n) - pmt * ((1 + rate).pow(n) - 1.0) / rate
        return fv
    }

    fun solvePV(fv: Double, pmt: Double, iPr: Double, n: Double): Double {
        val rate = iPr / 100.0
        if (rate == 0.0) return -(fv + pmt * n)
        val pv = (-fv - pmt * ((1 + rate).pow(n) - 1.0) / rate) / (1 + rate).pow(n)
        return pv
    }

    // Cashflow NPV Solver
    fun npv(ratePct: Double, initialInvestment: Double, flows: List<Double>): Double {
        val r = ratePct / 100.0
        var sum = -initialInvestment
        for (i in flows.indices) {
            sum += flows[i] / (1 + r).pow(i + 1)
        }
        return sum
    }
}

// ====================================================================
// 8. HIGHER SPECIAL MATHEMATICS FUNCTIONS
// ====================================================================

object SpecialMath {
    // Lanczos Gamma function approximation
    fun gamma(z: Double): Double {
        if (z < 0.5) return PI / (sin(PI * z) * gamma(1.0 - z))
        val x = z - 1.0
        val p = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var sum = p[0]
        for (i in 1 until p.size) {
            sum += p[i] / (x + i)
        }
        val t = x + p.size - 1.5
        return sqrt(2.0 * PI) * t.pow(x + 0.5) * exp(-t) * sum
    }

    // Error function erfc(x)
    fun erfc(x: Double): Double {
        val t = 1.0 / (1.0 + 0.5 * abs(x))
        // Horner's method or straight expansion
        val tau = t * exp(-x*x - 1.26551223 + t * (1.00002368 +
                t * (0.37409196 + t * (0.09678418 + t * (-0.18628806 +
                t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 +
                t * (-0.82215223 + t * 0.17087277)))))))))
        return if (x >= 0.0) tau else 2.0 - tau
    }

    // Incomplete first order Bessel Function J0(x) approximation
    fun besselJ0(x: Double): Double {
        val ax = abs(x)
        if (ax < 8.0) {
            val y = x * x
            val ans1 = 57568490574.0 + y * (-13362590354.0 + y * (651619640.7 + y * (-11214424.18 + y * (77392.33017 + y * (-184.9052456)))))
            val ans2 = 57568490574.0 + y * (102910404.0 + y * (462187.5446 + y * (975.725965 + y * 1.0)))
            return ans1 / ans2
        } else {
            val z = 8.0 / ax
            val y = z * z
            val xx = ax - 0.785398164
            val ans1 = 1.0 + y * (-0.1098628627e-2 + y * (0.2734510407e-4 + y * (-0.2073370639e-5 + y * 0.2093887211e-6)))
            val ans2 = -0.1562499995e-1 + y * (0.1430488765e-3 + y * (-0.6911147651e-5 + y * (0.7621095161e-6 - y * 0.934935152e-7)))
            return sqrt(0.636619772 / ax) * (cos(xx) * ans1 - z * sin(xx) * ans2)
        }
    }
}
