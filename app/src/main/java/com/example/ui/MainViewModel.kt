package com.example.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.calculator.CalculatorEngine
import com.example.calculator.CalculatorState
import com.example.data.model.PinVerificationResult
import com.example.data.model.SecurityState
import com.example.data.model.StorageStats
import com.example.data.model.VaultItem
import com.example.data.model.VaultType
import com.example.data.repository.VaultRepository
import com.example.data.security.SecurityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
  CALCULATOR,
  VAULT_DASHBOARD,
  SETTINGS
}

data class UiNotification(
  val message: String,
  val isError: Boolean = false,
  val id: Long = System.currentTimeMillis()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

  val securityManager = SecurityManager(application)
  val vaultRepository = VaultRepository(application)
  private val calculatorEngine = CalculatorEngine()

  private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val vm = application.getSystemService(VibratorManager::class.java)
    vm?.defaultVibrator
  } else {
    @Suppress("DEPRECATION")
    application.getSystemService(Application.VIBRATOR_SERVICE) as? Vibrator
  }

  private val _calculatorState = MutableStateFlow(calculatorEngine.getState())
  val calculatorState: StateFlow<CalculatorState> = _calculatorState.asStateFlow()

  private val _currentScreen = MutableStateFlow(AppScreen.CALCULATOR)
  val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

  private val _selectedTab = MutableStateFlow(VaultType.PHOTO)
  val selectedTab: StateFlow<VaultType> = _selectedTab.asStateFlow()

  private val _activeViewingItem = MutableStateFlow<VaultItem?>(null)
  val activeViewingItem: StateFlow<VaultItem?> = _activeViewingItem.asStateFlow()

  private val _pinSetupCandidate = MutableStateFlow<String?>(null)
  val pinSetupCandidate: StateFlow<String?> = _pinSetupCandidate.asStateFlow()

  private val _notification = MutableStateFlow<UiNotification?>(null)
  val notification: StateFlow<UiNotification?> = _notification.asStateFlow()

  val securityState: StateFlow<SecurityState> = securityManager.securityState

  val vaultItems: StateFlow<List<VaultItem>> = vaultRepository.itemsFlow

  val filteredItems: StateFlow<List<VaultItem>> = combine(
    vaultRepository.itemsFlow,
    _selectedTab,
    securityManager.securityState
  ) { items, tab, security ->
    if (security.isDecoyMode) {
      // In decoy mode, display empty clean vault
      emptyList()
    } else {
      items.filter { it.type == tab }
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun triggerHapticFeedback() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(18)
      }
    } catch (_: Exception) {}
  }

  fun onCalculatorDigit(digit: Char) {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onDigit(digit)
  }

  fun onCalculatorDecimal() {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onDecimal()
  }

  fun onCalculatorOperator(op: String) {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onOperator(op)
  }

  fun onCalculatorPercentage() {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onPercentage()
  }

  fun onCalculatorNegate() {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onNegate()
  }

  fun onCalculatorClear() {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onClear()
  }

  fun onCalculatorBackspace() {
    triggerHapticFeedback()
    _calculatorState.value = calculatorEngine.onBackspace()
  }

  fun onCalculatorEquals() {
    triggerHapticFeedback()
    val rawDigits = calculatorEngine.getRawDisplayDigits()
    val isConfigured = securityManager.isPinConfigured()

    if (!isConfigured) {
      if (rawDigits.length == 4) {
        // User is setting up initial PIN
        _pinSetupCandidate.value = rawDigits
        return
      }
    } else {
      if (rawDigits.length == 4) {
        val verification = securityManager.verifyPin(rawDigits)
        when (verification) {
          PinVerificationResult.MATCH_PRIMARY -> {
            calculatorEngine.onClear()
            _calculatorState.value = calculatorEngine.getState()
            _currentScreen.value = AppScreen.VAULT_DASHBOARD
            showNotification("Vault unlocked successfully.")
            return
          }
          PinVerificationResult.MATCH_DECOY -> {
            calculatorEngine.onClear()
            _calculatorState.value = calculatorEngine.getState()
            _currentScreen.value = AppScreen.VAULT_DASHBOARD
            showNotification("Decoy vault loaded.")
            return
          }
          PinVerificationResult.NO_MATCH -> {
            // Proceed normally as calculator
          }
        }
      }
    }

    // Normal arithmetic evaluation
    _calculatorState.value = calculatorEngine.onEquals()
  }

  fun confirmInitialPin(pin: String) {
    val success = securityManager.setPrimaryPin(pin)
    _pinSetupCandidate.value = null
    if (success) {
      calculatorEngine.onClear()
      _calculatorState.value = calculatorEngine.getState()
      _currentScreen.value = AppScreen.VAULT_DASHBOARD
      showNotification("Secret PIN [$pin] activated! Welcome to your Vault.")
    } else {
      showNotification("Failed to set PIN. Must be 4 numeric digits.", isError = true)
    }
  }

  fun dismissPinSetupCandidate() {
    _pinSetupCandidate.value = null
  }

  fun selectVaultTab(type: VaultType) {
    _selectedTab.value = type
  }

  fun navigateTo(screen: AppScreen) {
    _currentScreen.value = screen
  }

  fun panicLock() {
    triggerHapticFeedback()
    _activeViewingItem.value = null
    calculatorEngine.onClear()
    _calculatorState.value = calculatorEngine.getState()
    _currentScreen.value = AppScreen.CALCULATOR
    showNotification("Vault locked.")
  }

  fun openViewer(item: VaultItem) {
    _activeViewingItem.value = item
  }

  fun closeViewer() {
    _activeViewingItem.value = null
  }

  fun importMedia(uri: Uri, type: VaultType) {
    viewModelScope.launch {
      val item = vaultRepository.importFromUri(uri, type)
      if (item != null) {
        showNotification("Imported ${item.name} securely to vault.")
      } else {
        showNotification("Failed to import file.", isError = true)
      }
    }
  }

  fun exportItem(item: VaultItem, targetUri: Uri) {
    viewModelScope.launch {
      val success = vaultRepository.exportFile(item, targetUri)
      if (success) {
        showNotification("Exported ${item.name} to storage.")
      } else {
        showNotification("Failed to export file.", isError = true)
      }
    }
  }

  fun deleteItem(item: VaultItem) {
    viewModelScope.launch {
      val success = vaultRepository.deleteItem(item)
      if (success) {
        if (_activeViewingItem.value?.id == item.id) {
          _activeViewingItem.value = null
        }
        showNotification("Removed ${item.name} from vault.")
      }
    }
  }

  fun getStorageStats(): StorageStats {
    return vaultRepository.getStorageStats()
  }

  fun changePin(currentPin: String, newPin: String): Boolean {
    val success = securityManager.changePrimaryPin(currentPin, newPin)
    if (success) {
      showNotification("Secret PIN updated successfully.")
    } else {
      showNotification("Current PIN is incorrect or invalid new PIN.", isError = true)
    }
    return success
  }

  fun setDecoyPin(pin: String?): Boolean {
    val success = securityManager.setDecoyPin(pin)
    if (success) {
      showNotification(if (pin.isNullOrEmpty()) "Decoy PIN disabled." else "Decoy PIN set to $pin.")
    } else {
      showNotification("Decoy PIN must be 4 digits.", isError = true)
    }
    return success
  }

  fun showNotification(msg: String, isError: Boolean = false) {
    _notification.value = UiNotification(msg, isError)
  }

  fun dismissNotification() {
    _notification.value = null
  }
}
