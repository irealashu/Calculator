package com.example

import kotlin.math.*
import java.util.Locale

class BigVal {
    val mantissa: Double
    val exponent: BigVal?

    constructor(m: Double, e: BigVal?) {
        if (m == 0.0) {
            this.mantissa = 0.0
            this.exponent = null
        } else if (m.isInfinite() || m.isNaN() || e?.isInfinite() == true || e?.isNaN() == true) {
            this.mantissa = if (m < 0) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
            this.exponent = null
        } else {
            var tempM = m
            var tempE = e ?: BigVal(0.0)
            
            // Normalize so that 1.0 <= |mantissa| < 10.0
            if (tempM != 0.0) {
                val logVal = log10(abs(tempM))
                val shift = floor(logVal)
                if (shift.isFinite() && shift != 0.0) {
                    tempM /= 10.0.pow(shift)
                    tempE = tempE + BigVal(shift)
                }
            }
            
            // Simplify exponent if it is small and fits in Double
            if (tempE.exponent != null && tempE.toDouble().isFinite() && abs(tempE.toDouble()) < 1e11) {
                tempE = BigVal(tempE.toDouble())
            }
            
            // Extract fractional part of exponent to keep the exponent as a clean integer if flat
            if (tempE.exponent == null && tempE.toDouble().isFinite()) {
                val eVal = tempE.toDouble()
                if (eVal == 0.0) {
                    this.mantissa = tempM
                    this.exponent = null
                    return
                }
                val floorE = floor(eVal)
                val fracE = eVal - floorE
                if (fracE != 0.0 && floorE.isFinite()) {
                    tempM *= 10.0.pow(fracE)
                    tempE = BigVal(floorE)
                }
            }
            
            val tempEDouble = tempE.toDouble()
            if (tempE.exponent == null && tempEDouble == 0.0) {
                this.mantissa = tempM
                this.exponent = null
            } else {
                this.mantissa = tempM
                this.exponent = tempE
            }
        }
    }

    constructor(m: Double, e: Double) : this(m, BigVal(e))

    constructor(value: Double) : this(
        if (value.isInfinite() || value.isNaN() || value == 0.0) value else value,
        null as BigVal?
    )

    fun toDouble(): Double {
        if (this.mantissa.isInfinite() || this.mantissa.isNaN()) return this.mantissa
        val expD = exponent?.toDouble() ?: 0.0
        if (expD > 308.25) return if (this.mantissa < 0) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        if (expD < -324.0) return 0.0
        return this.mantissa * 10.0.pow(expD)
    }

    fun isInfinite(): Boolean = this.mantissa.isInfinite()
    fun isNaN(): Boolean = this.mantissa.isNaN()

    operator fun plus(other: BigVal): BigVal {
        if (this.mantissa == 0.0) return other
        if (other.mantissa == 0.0) return this
        if (this.mantissa.isInfinite() || other.mantissa.isInfinite()) {
            return BigVal(this.toDouble() + other.toDouble())
        }
        
        val comp = compareExponents(this.exponent, other.exponent)
        if (comp == 0) {
            return BigVal(this.mantissa + other.mantissa, this.exponent)
        }
        
        val diffB = (this.exponent ?: BigVal(0.0)) - (other.exponent ?: BigVal(0.0))
        val diffD = diffB.toDouble()
        
        return if (diffD.isInfinite()) {
            if (diffD > 0) this else other
        } else {
            if (diffD >= 15.0) {
                this
            } else if (diffD <= -15.0) {
                other
            } else {
                val shiftedOtherM = other.mantissa * 10.0.pow(-diffD)
                BigVal(this.mantissa + shiftedOtherM, this.exponent)
            }
        }
    }

    operator fun minus(other: BigVal): BigVal {
        if (other.mantissa == 0.0) return this
        if (this.mantissa == 0.0) return -other
        if (this.mantissa.isInfinite() || other.mantissa.isInfinite()) {
            return BigVal(this.toDouble() - other.toDouble())
        }
        
        val comp = compareExponents(this.exponent, other.exponent)
        if (comp == 0) {
            return BigVal(this.mantissa - other.mantissa, this.exponent)
        }
        
        val diffB = (this.exponent ?: BigVal(0.0)) - (other.exponent ?: BigVal(0.0))
        val diffD = diffB.toDouble()
        
        return if (diffD.isInfinite()) {
            if (diffD > 0) this else -other
        } else {
            if (diffD >= 15.0) {
                this
            } else if (diffD <= -15.0) {
                -other
            } else {
                val shiftedOtherM = other.mantissa * 10.0.pow(-diffD)
                BigVal(this.mantissa - shiftedOtherM, this.exponent)
            }
        }
    }

    operator fun times(other: BigVal): BigVal {
        if (this.mantissa == 0.0 || other.mantissa == 0.0) return BigVal(0.0)
        return BigVal(this.mantissa * other.mantissa, (this.exponent ?: BigVal(0.0)) + (other.exponent ?: BigVal(0.0)))
    }

    operator fun div(other: BigVal): BigVal {
        if (other.mantissa == 0.0) throw ArithmeticException("Divide by zero")
        return BigVal(this.mantissa / other.mantissa, (this.exponent ?: BigVal(0.0)) - (other.exponent ?: BigVal(0.0)))
    }

    operator fun unaryMinus(): BigVal {
        return BigVal(-this.mantissa, this.exponent)
    }

    fun log10Val(): BigVal {
        if (this.mantissa <= 0.0) throw IllegalArgumentException("Result is non-real")
        val logM = log10(this.mantissa)
        return BigVal(logM) + (this.exponent ?: BigVal(0.0))
    }

    fun pow(other: BigVal): BigVal {
        if (other.mantissa == 0.0) return BigVal(1.0, null as BigVal?)
        if (this.mantissa == 0.0) return BigVal(0.0, null as BigVal?)
        
        // Handle normal values if within bounds
        val thisD = this.toDouble()
        val otherD = other.toDouble()
        if (thisD.isFinite() && otherD.isFinite()) {
            val res = thisD.pow(otherD)
            if (res.isFinite()) {
                return BigVal(res)
            }
        }

        // Extremely large exponentiation
        val logThis = this.log10Val()
        val newExponent = other * logThis
        
        return BigVal(1.0, newExponent)
    }

    fun sinVal(): BigVal = BigVal(sin(toDouble()))
    fun cosVal(): BigVal = BigVal(cos(toDouble()))
    fun tanVal(): BigVal = BigVal(tan(toDouble()))
    
    fun lnVal(): BigVal {
        if (this.mantissa <= 0.0) throw IllegalArgumentException("Result is non-real")
        return logVal() * BigVal(ln(10.0))
    }
    
    fun logVal(): BigVal {
        if (this.mantissa <= 0.0) throw IllegalArgumentException("Result is non-real")
        return BigVal(log10(this.mantissa)) + (this.exponent ?: BigVal(0.0))
    }
    
    fun sqrtVal(): BigVal {
        if (this.mantissa < 0.0) throw IllegalArgumentException("Result is non-real")
        return BigVal(sqrt(this.mantissa), (this.exponent ?: BigVal(0.0)) / BigVal(2.0))
    }

    fun compareTo(other: BigVal): Int {
        if (this.mantissa == 0.0 && other.mantissa == 0.0) return 0
        if (this.mantissa == 0.0) return if (other.mantissa < 0.0) 1 else -1
        if (other.mantissa == 0.0) return if (this.mantissa < 0.0) -1 else 1

        if (this.mantissa.isInfinite() && other.mantissa.isInfinite()) {
            return this.mantissa.compareTo(other.mantissa)
        }
        if (this.mantissa.isInfinite()) return if (this.mantissa < 0.0) -1 else 1
        if (other.isInfinite()) return if (other.mantissa < 0.0) 1 else -1

        val signThis = sign(this.mantissa)
        val signOther = sign(other.mantissa)
        if (signThis != signOther) {
            return signThis.compareTo(signOther)
        }

        val expComp = compareExponents(this.exponent, other.exponent)
        if (expComp != 0) {
            return if (signThis > 0) expComp else -expComp
        }

        return if (signThis > 0) {
            this.mantissa.compareTo(other.mantissa)
        } else {
            other.mantissa.compareTo(this.mantissa)
        }
    }

    private fun compareExponents(e1: BigVal?, e2: BigVal?): Int {
        val v1 = e1 ?: BigVal(0.0)
        val v2 = e2 ?: BigVal(0.0)
        return v1.compareTo(v2)
    }

    fun toFormattedString(): String {
        if (this.mantissa.isNaN()) return "NaN"
        if (this.mantissa.isInfinite()) return if (this.mantissa < 0.0) "-Infinity" else "Infinity"
        if (this.mantissa == 0.0) return "0"
        
        val expB = this.exponent ?: BigVal(0.0)
        val expD = expB.toDouble()
        
        // If exponent is in reasonable display range, format as a regular decimal
        if (expB.exponent == null && expD.isFinite() && expD >= -4.0 && expD < 11.0) {
            val d = toDouble()
            return if (d == d.toLong().toDouble()) {
                d.toLong().toString()
            } else {
                val formatted = String.format(Locale.US, "%.10f", d)
                var end = formatted.length - 1
                while (end > 0 && formatted[end] == '0') {
                    end--
                }
                if (formatted[end] == '.') {
                    end--
                }
                formatted.substring(0, end + 1)
            }
        }
        
        // Format as scientific notation
        val formattedM = if (this.mantissa == this.mantissa.toLong().toDouble()) {
            this.mantissa.toLong().toString()
        } else {
            val formatted = String.format(Locale.US, "%.5f", this.mantissa)
            var end = formatted.length - 1
            while (end > 0 && formatted[end] == '0') {
                end--
            }
            if (formatted[end] == '.') {
                end--
            }
            formatted.substring(0, end + 1)
        }
        
        val formattedE = expB.toFormattedString()
        val eNeedsParens = formattedE.contains('^') || formattedE.contains('×') || formattedE.contains('-')
        val eStr = if (eNeedsParens) "($formattedE)" else formattedE
        
        if (this.mantissa == 1.0) {
            return "10^$eStr"
        }
        return "${formattedM}×10^$eStr"
    }
}
