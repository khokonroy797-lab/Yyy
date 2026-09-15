package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.calculator.CalculatorEngine
import com.example.data.model.PinVerificationResult
import com.example.data.security.SecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalculatorVaultTest {

  @Test
  fun `test basic arithmetic addition`() {
    val engine = CalculatorEngine()
    engine.onDigit('1')
    engine.onDigit('2')
    engine.onOperator("+")
    engine.onDigit('8')
    val state = engine.onEquals()
    assertEquals("20", state.display)
  }

  @Test
  fun `test basic arithmetic multiplication`() {
    val engine = CalculatorEngine()
    engine.onDigit('7')
    engine.onOperator("×")
    engine.onDigit('6')
    val state = engine.onEquals()
    assertEquals("42", state.display)
  }

  @Test
  fun `test raw pin detection`() {
    val engine = CalculatorEngine()
    engine.onDigit('1')
    engine.onDigit('3')
    engine.onDigit('5')
    engine.onDigit('7')
    assertEquals("1357", engine.getRawDisplayDigits())
  }

  @Test
  fun `test security pin setup and verification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sec = SecurityManager(context)

    assertFalse(sec.isPinConfigured())
    val setOk = sec.setPrimaryPin("7788")
    assertTrue(setOk)
    assertTrue(sec.isPinConfigured())

    val correctResult = sec.verifyPin("7788")
    assertEquals(PinVerificationResult.MATCH_PRIMARY, correctResult)

    val wrongResult = sec.verifyPin("1234")
    assertEquals(PinVerificationResult.NO_MATCH, wrongResult)
  }

  @Test
  fun `test decoy pin`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sec = SecurityManager(context)

    sec.setPrimaryPin("9999")
    sec.setDecoyPin("1111")

    val decoyResult = sec.verifyPin("1111")
    assertEquals(PinVerificationResult.MATCH_DECOY, decoyResult)

    val primaryResult = sec.verifyPin("9999")
    assertEquals(PinVerificationResult.MATCH_PRIMARY, primaryResult)
  }
}
