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
  fun testLargeExponentiation() {
    val expr3 = "9^9^9"
    val result3 = CalculatorParser(expr3).parse()
    val formatted3 = result3.toFormattedString()
    assertEquals("4.28125×10^369693099", formatted3)
  }
}

