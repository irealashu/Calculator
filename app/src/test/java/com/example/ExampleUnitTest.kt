package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testAddOneAndTwo() {
    val expr = "1 + 2"
    val result = CalculatorParser(expr).parse()
    assertEquals("3", result.toFormattedString())
  }

  @Test
  fun testFactorialPrecision() {
    val expr = "9!"
    val result = CalculatorParser(expr).parse()
    assertEquals("362880", result.toFormattedString())
  }

  @Test
  fun testLargeExponentiation() {
    val expr3 = "9^9^9"
    val result3 = CalculatorParser(expr3).parse()
    val formatted3 = result3.toFormattedString()
    assertEquals("4.28125×10^369693099", formatted3)
  }

  @Test
  fun testSuperLargeExponentiation() {
    val expr4 = "9^9^9^9^9"
    val result4 = CalculatorParser(expr4).parse()
    val formatted4 = result4.toFormattedString()
    println("SUPER_LARGE_RESULT: $formatted4")
  }

  @Test
  fun testScientificParser() {
    val expr = "6.62607e-34 * 10"
    val result = CalculatorParser(expr).parse()
    assertEquals("6.62607×10^(-33)", result.toFormattedString())

    val expr2 = "1.60218e-19 * 1e4"
    val result2 = CalculatorParser(expr2).parse()
    assertEquals("1.60218×10^(-15)", result2.toFormattedString())
  }
}

