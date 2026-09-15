package com.example.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KeypadEqualAccent
import com.example.ui.theme.LcdAccentCyan
import com.example.ui.theme.LcdTextPrimary
import com.example.ui.theme.LcdTextSecondary
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultEmerald
import com.example.ui.theme.VaultPrimary
import com.example.ui.vault.formatFileSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultSettingsScreen(viewModel: MainViewModel) {
  val securityState by viewModel.securityState.collectAsState()
  val stats = remember { viewModel.getStorageStats() }

  var showChangePinDialog by remember { mutableStateOf(false) }
  var showDecoyPinDialog by remember { mutableStateOf(false) }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(DarkSurface)
          .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(
          onClick = { viewModel.navigateTo(AppScreen.VAULT_DASHBOARD) },
          modifier = Modifier.testTag("settings_back_btn")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.White
          )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Vault Settings & Security",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {

      // DEVELOPER PROFILE SECTION (KHOKON ROY + UPLOADED PHOTO)
      DeveloperProfileCard()

      // PIN & SECURITY SETTINGS
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("security_settings_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.verticalGradient(
            listOf(DarkSurfaceBorder, Color.Transparent)
          ),
          width = 1.dp
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = LcdAccentCyan,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Security & Disguise Controls",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(DarkSurfaceBorder)
          )

          // Change PIN Setting
          SettingsActionRow(
            icon = Icons.Default.LockReset,
            title = "Change Secret PIN",
            subtitle = "Modify the 4-digit passcode used with [ = ]",
            testTag = "change_pin_row",
            onClick = { showChangePinDialog = true }
          )

          // Decoy Mode Setting
          SettingsActionRow(
            icon = Icons.Default.VisibilityOff,
            title = "Decoy Vault PIN",
            subtitle = if (securityState.hasDecoyPin) "Configured (Opens clean decoy vault)" else "Disabled (Tap to configure)",
            testTag = "decoy_pin_row",
            onClick = { showDecoyPinDialog = true }
          )

          // Panic Exit Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(VaultCoral.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = VaultCoral,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "Panic Exit Mode",
                  color = Color.White,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = "Instantly locks vault on minimize or lock tap",
                  color = LcdTextSecondary,
                  fontSize = 11.sp
                )
              }
            }

            Switch(
              checked = securityState.panicExitEnabled,
              onCheckedChange = { viewModel.securityManager.setPanicExitEnabled(it) },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VaultCoral,
                uncheckedThumbColor = LcdTextSecondary,
                uncheckedTrackColor = DarkSurface
              )
            )
          }
        }
      }

      // STORAGE USAGE SUMMARY
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.verticalGradient(
            listOf(DarkSurfaceBorder, Color.Transparent)
          ),
          width = 1.dp
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.PieChart,
              contentDescription = null,
              tint = VaultEmerald,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Vault Encrypted Storage",
              color = Color.White,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(DarkSurfaceBorder)
          )

          StorageStatRow(label = "Hidden Photos", size = stats.photosBytes, color = LcdAccentCyan)
          StorageStatRow(label = "Hidden Videos", size = stats.videosBytes, color = VaultCoral)
          StorageStatRow(label = "Hidden Documents", size = stats.docsBytes, color = VaultEmerald)

          val totalBytes = stats.photosBytes + stats.videosBytes + stats.docsBytes
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "Total Protected Size",
              color = Color.White,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = formatFileSize(totalBytes),
              color = LcdAccentCyan,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace
            )
          }
        }
      }

      // LOCK VAULT BUTTON
      Button(
        onClick = { viewModel.panicLock() },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("lock_vault_btn"),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Lock Vault & Exit to Calculator",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }

  // Change PIN Dialog
  if (showChangePinDialog) {
    ChangePinDialog(
      onDismiss = { showChangePinDialog = false },
      onConfirm = { currentPin, newPin ->
        val ok = viewModel.changePin(currentPin, newPin)
        if (ok) showChangePinDialog = false
      }
    )
  }

  // Decoy PIN Dialog
  if (showDecoyPinDialog) {
    DecoyPinDialog(
      hasExisting = securityState.hasDecoyPin,
      onDismiss = { showDecoyPinDialog = false },
      onSave = { decoyPin ->
        viewModel.setDecoyPin(decoyPin)
        showDecoyPinDialog = false
      }
    )
  }
}

@Composable
fun DeveloperProfileCard() {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("developer_profile_card"),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(
      brush = Brush.linearGradient(
        listOf(LcdAccentCyan.copy(alpha = 0.8f), VaultPrimary.copy(alpha = 0.4f), Color(0xFF7209B7).copy(alpha = 0.8f))
      ),
      width = 1.5.dp
    )
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Header badge
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(LcdAccentCyan.copy(alpha = 0.12f))
          .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Code,
          contentDescription = null,
          tint = LcdAccentCyan,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "LEAD DEVELOPER PROFILE",
          color = LcdAccentCyan,
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      // Developer Avatar with luxury border
      Box(
        modifier = Modifier
          .size(104.dp)
          .clip(CircleShape)
          .border(
            width = 3.dp,
            brush = Brush.sweepGradient(
              listOf(LcdAccentCyan, Color(0xFF7209B7), VaultPrimary, LcdAccentCyan)
            ),
            shape = CircleShape
          )
          .padding(4.dp)
          .clip(CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_developer_avatar),
          contentDescription = "KHOKON ROY",
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Developer Name: KHOKON ROY
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = "KHOKON ROY",
          color = Color.White,
          fontSize = 22.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Verified Creator",
          tint = LcdAccentCyan,
          modifier = Modifier.size(20.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Lead Android Architect & Security Engineer",
        color = LcdAccentCyan,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "Crafted the Calculator Vault disguise engine with Scoped Storage isolation, zero-knowledge PIN activation, and stealth AMOLED aesthetics.",
        color = LcdTextSecondary,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Skills & Credentials badges
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
      ) {
        ProfileBadge(text = "Android")
        Spacer(modifier = Modifier.width(6.dp))
        ProfileBadge(text = "Kotlin")
        Spacer(modifier = Modifier.width(6.dp))
        ProfileBadge(text = "Compose")
        Spacer(modifier = Modifier.width(6.dp))
        ProfileBadge(text = "Security Engine")
      }
    }
  }
}

@Composable
fun ProfileBadge(text: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(DarkSurfaceBorder)
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = text,
      color = LcdTextPrimary,
      fontSize = 10.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun SettingsActionRow(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 8.dp, horizontal = 4.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Row(
      modifier = Modifier.weight(1f),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(VaultPrimary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = VaultPrimary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = title,
          color = Color.White,
          fontSize = 14.sp,
          fontWeight = FontWeight.SemiBold
        )
        Text(
          text = subtitle,
          color = LcdTextSecondary,
          fontSize = 11.sp
        )
      }
    }

    Text(
      text = "Edit",
      color = LcdAccentCyan,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold
    )
  }
}

@Composable
fun StorageStatRow(label: String, size: Long, color: Color) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(color)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = label,
        color = LcdTextSecondary,
        fontSize = 13.sp
      )
    }
    Text(
      text = formatFileSize(size),
      color = Color.White,
      fontSize = 13.sp,
      fontFamily = FontFamily.Monospace
    )
  }
}

@Composable
fun ChangePinDialog(
  onDismiss: () -> Unit,
  onConfirm: (currentPin: String, newPin: String) -> Unit
) {
  var currentPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Change Secret PIN", color = Color.White, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = currentPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) currentPin = it },
          label = { Text("Current 4-Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LcdAccentCyan,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        OutlinedTextField(
          value = newPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
          label = { Text("New 4-Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LcdAccentCyan,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
          label = { Text("Confirm New PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LcdAccentCyan,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        if (errorMessage != null) {
          Text(text = errorMessage!!, color = VaultCoral, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          when {
            currentPin.length != 4 -> errorMessage = "Current PIN must be 4 digits"
            newPin.length != 4 -> errorMessage = "New PIN must be 4 digits"
            newPin != confirmPin -> errorMessage = "New PINs do not match"
            else -> onConfirm(currentPin, newPin)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = KeypadEqualAccent)
      ) {
        Text("Save New PIN", color = Color.White)
      }
    },
    dismissButton = {
      OutlinedButton(onClick = onDismiss) {
        Text("Cancel", color = LcdTextSecondary)
      }
    },
    containerColor = DarkSurface,
    shape = RoundedCornerShape(18.dp)
  )
}

@Composable
fun DecoyPinDialog(
  hasExisting: Boolean,
  onDismiss: () -> Unit,
  onSave: (decoyPin: String?) -> Unit
) {
  var decoyPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Decoy Vault PIN", color = Color.White, fontWeight = FontWeight.Bold) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "If someone forces you to open the app, enter this Decoy PIN instead of your real PIN. It will open an empty, clean vault.",
          color = LcdTextSecondary,
          fontSize = 13.sp
        )

        OutlinedTextField(
          value = decoyPin,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) decoyPin = it },
          label = { Text("Decoy 4-Digit PIN") },
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LcdAccentCyan,
            unfocusedBorderColor = DarkSurfaceBorder,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
          )
        )

        if (errorMessage != null) {
          Text(text = errorMessage!!, color = VaultCoral, fontSize = 12.sp)
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (decoyPin.length == 4) {
            onSave(decoyPin)
          } else {
            errorMessage = "Decoy PIN must be exactly 4 digits"
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = KeypadEqualAccent)
      ) {
        Text("Save Decoy PIN", color = Color.White)
      }
    },
    dismissButton = {
      Row {
        if (hasExisting) {
          OutlinedButton(
            onClick = { onSave(null) },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VaultCoral)
          ) {
            Text("Disable Decoy")
          }
          Spacer(modifier = Modifier.width(8.dp))
        }
        OutlinedButton(onClick = onDismiss) {
          Text("Cancel", color = LcdTextSecondary)
        }
      }
    },
    containerColor = DarkSurface,
    shape = RoundedCornerShape(18.dp)
  )
}
