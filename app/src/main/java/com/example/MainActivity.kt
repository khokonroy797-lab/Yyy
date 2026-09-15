package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.settings.VaultSettingsScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LcdAccentCyan
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VaultCoral
import com.example.ui.vault.MediaViewerDialog
import com.example.ui.vault.VaultDashboardScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme(darkTheme = true) {
        val viewModel: MainViewModel = viewModel()
        val lifecycleOwner = LocalLifecycleOwner.current

        // Panic Lock on App Minimize / Backgrounding
        DisposableEffect(lifecycleOwner) {
          val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
              if (viewModel.securityState.value.panicExitEnabled) {
                viewModel.panicLock()
              }
            }
          }
          lifecycleOwner.lifecycle.addObserver(observer)
          onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
          }
        }

        CalculatorVaultApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun CalculatorVaultApp(viewModel: MainViewModel) {
  val currentScreen by viewModel.currentScreen.collectAsState()
  val activeViewingItem by viewModel.activeViewingItem.collectAsState()
  val notification by viewModel.notification.collectAsState()

  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()

  LaunchedEffect(notification) {
    notification?.let {
      scope.launch {
        snackbarHostState.showSnackbar(it.message)
        viewModel.dismissNotification()
      }
    }
  }

  // Handle system back button gracefully
  BackHandler(enabled = currentScreen != AppScreen.CALCULATOR) {
    when (currentScreen) {
      AppScreen.SETTINGS -> viewModel.navigateTo(AppScreen.VAULT_DASHBOARD)
      AppScreen.VAULT_DASHBOARD -> viewModel.panicLock()
      AppScreen.CALCULATOR -> { /* Exit app */ }
    }
  }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = DarkBackground,
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState) { data ->
        Snackbar(
          snackbarData = data,
          containerColor = DarkSurfaceElevated,
          contentColor = Color.White,
          actionColor = LcdAccentCyan
        )
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      AnimatedContent(
        targetState = currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "screen_transition"
      ) { screen ->
        when (screen) {
          AppScreen.CALCULATOR -> {
            CalculatorScreen(viewModel = viewModel)
          }
          AppScreen.VAULT_DASHBOARD -> {
            VaultDashboardScreen(viewModel = viewModel)
          }
          AppScreen.SETTINGS -> {
            VaultSettingsScreen(viewModel = viewModel)
          }
        }
      }

      // Active media viewer modal
      activeViewingItem?.let { item ->
        MediaViewerDialog(
          item = item,
          viewModel = viewModel,
          onDismiss = { viewModel.closeViewer() }
        )
      }
    }
  }
}
