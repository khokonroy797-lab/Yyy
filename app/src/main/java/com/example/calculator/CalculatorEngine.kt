package com.example.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

data class CalculatorState(
  val display: String = "0",
  val expression: String = "",
  val lastOperation: String? = null
)

class CalculatorEngine {

  private var currentDisplay = "0"
  private var expressionHistory = ""
  private var storedValue: BigDecimal? = null
  private var pendingOp: String? = null
  private var shouldResetDisplayOnNextDigit = false

  fun getState(): CalculatorState {
    return CalculatorState(
      display = formatDisplay(currentDisplay),
      expression = expressionHistory,
      lastOperation = pendingOp
    )
  }

  fun getRawDisplayDigits(): String {
    // Returns digits only if it represents a clean integer
    val trimmed = currentDisplay.trim()
    return if (trimmed.matches(Regex("^\\d{4}$"))) trimmed else ""
  }

  fun getCurrentDisplayRaw(): String = currentDisplay

  fun onDigit(digit: Char): CalculatorState {
    if (digit !in '0'..'9') return getState()

    if (shouldResetDisplayOnNextDigit || currentDisplay == "0") {
      currentDisplay = digit.toString()
      shouldResetDisplayOnNextDigit = false
    } else {
      if (currentDisplay.length < 12) {
        currentDisplay += digit
      }
    }
    return getState()
  }

  fun onDecimal(): CalculatorState {
    if (shouldResetDisplayOnNextDigit) {
      currentDisplay = "0."
      shouldResetDisplayOnNextDigit = false
    } else if (!currentDisplay.contains(".")) {
      currentDisplay += "."
    }
    return getState()
  }

  fun onOperator(op: String): CalculatorState {
    val currentVal = toBigDecimal(currentDisplay)

    if (storedValue != null && pendingOp != null && !shouldResetDisplayOnNextDigit) {
      val result = compute(storedValue!!, currentVal, pendingOp!!)
      storedValue = result
      currentDisplay = stripTrailingZeros(result)
      expressionHistory = "${stripTrailingZeros(result)} $op"
    } else {
      storedValue = currentVal
      expressionHistory = "${stripTrailingZeros(currentVal)} $op"
    }

    pendingOp = op
    shouldResetDisplayOnNextDigit = true
    return getState()
  }

  fun onPercentage(): CalculatorState {
    val currentVal = toBigDecimal(currentDisplay)
    val result = if (storedValue != null && (pendingOp == "+" || pendingOp == "−")) {
      // Percentage of stored value
      storedValue!!.multiply(currentVal).divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    } else {
      currentVal.divide(BigDecimal(100), 8, RoundingMode.HALF_UP)
    }
    currentDisplay = stripTrailingZeros(result)
    return getState()
  }

  fun onNegate(): CalculatorState {
    if (currentDisplay == "0" || currentDisplay.isEmpty()) return getState()
    currentDisplay = if (currentDisplay.startsWith("-")) {
      currentDisplay.substring(1)
    } else {
      "-$currentDisplay"
    }
    return getState()
  }

  fun onClear(): CalculatorState {
    if (currentDisplay != "0" && !shouldResetDisplayOnNextDigit) {
      currentDisplay = "0"
    } else {
      // AC - All clear
      currentDisplay = "0"
      storedValue = null
      pendingOp = null
      expressionHistory = ""
      shouldResetDisplayOnNextDigit = false
    }
    return getState()
  }

  fun onBackspace(): CalculatorState {
    if (shouldResetDisplayOnNextDigit) {
      currentDisplay = "0"
      shouldResetDisplayOnNextDigit = false
      return getState()
    }

    if (currentDisplay.length > 1) {
      currentDisplay = currentDisplay.dropLast(1)
      if (currentDisplay == "-") currentDisplay = "0"
    } else {
      currentDisplay = "0"
    }
    return getState()
  }

  fun onEquals(): CalculatorState {
    if (pendingOp != null && storedValue != null) {
      val secondOperand = toBigDecimal(currentDisplay)
      val result = compute(storedValue!!, secondOperand, pendingOp!!)
      expressionHistory = "${stripTrailingZeros(storedValue!!)} $pendingOp ${stripTrailingZeros(secondOperand)} ="
      currentDisplay = stripTrailingZeros(result)
      storedValue = null
      pendingOp = null
      shouldResetDisplayOnNextDigit = true
    } else {
      expressionHistory = "$currentDisplay ="
      shouldResetDisplayOnNextDigit = true
    }
    return getState()
  }

  private fun compute(a: BigDecimal, b: BigDecimal, op: String): BigDecimal {
    return try {
      when (op) {
        "+", "+" -> a.add(b)
        "-", "−" -> a.subtract(b)
        "×", "*" -> a.multiply(b)
        "÷", "/" -> {
          if (b.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal.ZERO
          } else {
            a.divide(b, 8, RoundingMode.HALF_UP)
          }
        }
        else -> b
      }
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  private fun toBigDecimal(str: String): BigDecimal {
    return try {
      BigDecimal(str)
    } catch (e: Exception) {
      BigDecimal.ZERO
    }
  }

  private fun stripTrailingZeros(bd: BigDecimal): String {
    val stripped = bd.stripTrailingZeros()
    return stripped.toPlainString()
  }

  private fun formatDisplay(str: String): String {
    if (str.isEmpty()) return "0"
    if (str.endsWith(".") || str.contains("e") || str.contains("E")) return str

    val parts = str.split(".")
    return try {
      val intPart = parts[0]
      val isNegative = intPart.startsWith("-")
      val absIntPart = if (isNegative) intPart.substring(1) else intPart

      val formatter = DecimalFormat("#,###")
      val formattedInt = if (absIntPart.isNotEmpty()) {
        formatter.format(absIntPart.toLong())
      } else {
        "0"
      }
      val sign = if (isNegative) "-" else ""

      if (parts.size > 1) {
        "$sign$formattedInt.${parts[1]}"
      } else {
        "$sign$formattedInt"
      }
    } catch (e: Exception) {
      str
    }
  }
}
