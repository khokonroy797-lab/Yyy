package com.example.data.security

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.PinVerificationResult
import com.example.data.model.SecurityState
import java.security.MessageDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SecurityManager(context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private val _securityState = MutableStateFlow(loadState())
  val securityState: StateFlow<SecurityState> = _securityState.asStateFlow()

  private fun loadState(): SecurityState {
    val isConfigured = prefs.getBoolean(KEY_PIN_CONFIGURED, false)
    val hasDecoy = !prefs.getString(KEY_DECOY_PIN_HASH, "").isNullOrEmpty()
    val panicExit = prefs.getBoolean(KEY_PANIC_EXIT, true)
    return SecurityState(
      isPinConfigured = isConfigured,
      hasDecoyPin = hasDecoy,
      panicExitEnabled = panicExit,
      isDecoyMode = false
    )
  }

  fun isPinConfigured(): Boolean {
    return prefs.getBoolean(KEY_PIN_CONFIGURED, false)
  }

  fun setPrimaryPin(pin: String): Boolean {
    if (pin.length != 4 || !pin.all { it.isDigit() }) return false
    val hash = hashPin(pin)
    prefs.edit()
      .putString(KEY_PRIMARY_PIN_HASH, hash)
      .putBoolean(KEY_PIN_CONFIGURED, true)
      .apply()
    refreshState()
    return true
  }

  fun verifyPin(enteredPin: String): PinVerificationResult {
    if (enteredPin.length != 4) return PinVerificationResult.NO_MATCH
    val enteredHash = hashPin(enteredPin)

    val primaryHash = prefs.getString(KEY_PRIMARY_PIN_HASH, null)
    if (primaryHash != null && primaryHash == enteredHash) {
      _securityState.value = _securityState.value.copy(isDecoyMode = false)
      return PinVerificationResult.MATCH_PRIMARY
    }

    val decoyHash = prefs.getString(KEY_DECOY_PIN_HASH, null)
    if (decoyHash != null && decoyHash == enteredHash) {
      _securityState.value = _securityState.value.copy(isDecoyMode = true)
      return PinVerificationResult.MATCH_DECOY
    }

    return PinVerificationResult.NO_MATCH
  }

  fun changePrimaryPin(currentPin: String, newPin: String): Boolean {
    val currentHash = hashPin(currentPin)
    val savedHash = prefs.getString(KEY_PRIMARY_PIN_HASH, null)
    if (savedHash == null || savedHash != currentHash) {
      return false
    }
    return setPrimaryPin(newPin)
  }

  fun setDecoyPin(pin: String?): Boolean {
    if (pin == null || pin.isEmpty()) {
      prefs.edit().remove(KEY_DECOY_PIN_HASH).apply()
      refreshState()
      return true
    }
    if (pin.length != 4 || !pin.all { it.isDigit() }) return false
    val hash = hashPin(pin)
    prefs.edit().putString(KEY_DECOY_PIN_HASH, hash).apply()
    refreshState()
    return true
  }

  fun setPanicExitEnabled(enabled: Boolean) {
    prefs.edit().putBoolean(KEY_PANIC_EXIT, enabled).apply()
    refreshState()
  }

  private fun refreshState() {
    _securityState.value = loadState()
  }

  private fun hashPin(pin: String): String {
    val salted = "$SALT_PREFIX:$pin:$SALT_SUFFIX"
    val md = MessageDigest.getInstance("SHA-256")
    val bytes = md.digest(salted.toByteArray(Charsets.UTF_8))
    return bytes.joinToString("") { "%02x".format(it) }
  }

  companion object {
    private const val PREFS_NAME = "calc_vault_security_prefs"
    private const val KEY_PIN_CONFIGURED = "key_pin_configured"
    private const val KEY_PRIMARY_PIN_HASH = "key_primary_pin_hash"
    private const val KEY_DECOY_PIN_HASH = "key_decoy_pin_hash"
    private const val KEY_PANIC_EXIT = "key_panic_exit"
    private const val SALT_PREFIX = "calc_sec_v1_salt"
    private const val SALT_SUFFIX = "vault_khokon_roy_2026"
  }
}
