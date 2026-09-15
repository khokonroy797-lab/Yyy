package com.example.ui.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.KeypadDarkNum
import com.example.ui.theme.KeypadEqualAccent
import com.example.ui.theme.KeypadFunction
import com.example.ui.theme.KeypadFunctionText
import com.example.ui.theme.KeypadOperator
import com.example.ui.theme.KeypadOperatorAccent
import com.example.ui.theme.LcdAccentCyan
import com.example.ui.theme.LcdScreenBackground
import com.example.ui.theme.LcdTextPrimary
import com.example.ui.theme.LcdTextSecondary

@Composable
fun CalculatorScreen(viewModel: MainViewModel) {
  val calcState by viewModel.calculatorState.collectAsState()
  val securityState by viewModel.securityState.collectAsState()
  val pinSetupCandidate by viewModel.pinSetupCandidate.collectAsState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
      .padding(horizontal = 16.dp, vertical = 20.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .widthIn(max = 500.dp),
      verticalArrangement = Arrangement.SpaceBetween
    ) {

      // Setup Hint Banner (Visible only when PIN is not yet set)
      AnimatedVisibility(
        visible = !securityState.isPinConfigured,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("pin_setup_banner"),
          shape = RoundedCornerShape(14.dp),
          colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceElevated
          ),
          border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
              listOf(LcdAccentCyan.copy(alpha = 0.6f), Color(0xFF7209B7).copy(alpha = 0.6f))
            ),
            width = 1.dp
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = "Security Setup",
              tint = LcdAccentCyan,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Secret Vault Setup",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
              )
              Text(
                text = "Enter a 4-digit PIN and press [ = ] to activate your vault.",
                color = LcdTextSecondary,
                fontSize = 12.sp,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // LCD-Style High-Contrast Display Panel
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .weight(0.9f)
          .padding(bottom = 16.dp)
          .testTag("lcd_display_panel"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = LcdScreenBackground),
        border = CardDefaults.outlinedCardBorder().copy(
          brush = Brush.verticalGradient(
            listOf(DarkSurfaceBorder, Color(0xFF1B2030))
          ),
          width = 1.5.dp
        )
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.SpaceBetween
        ) {
          // Status bar inside LCD
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
                  .background(LcdAccentCyan)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "RAD",
                color = LcdTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
              )
            }

            // Discreet Vault status icon
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Stealth Mode",
              tint = Color(0xFF2A3042),
              modifier = Modifier.size(14.dp)
            )
          }

          // Expression history line
          Text(
            text = calcState.expression.ifEmpty { " " },
            color = LcdTextSecondary,
            fontSize = 18.sp,
            fontFamily = FontFamily.Monospace,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("expression_history")
          )

          // Active LCD calculation / PIN entry display
          val displayText = calcState.display
          val dynamicFontSize = when {
            displayText.length > 10 -> 34.sp
            displayText.length > 7 -> 42.sp
            else -> 52.sp
          }

          Text(
            text = displayText,
            color = LcdTextPrimary,
            fontSize = dynamicFontSize,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("calculator_display")
          )
        }
      }

      // Keypad Layout (4 columns x 5 rows)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(2.0f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Row 1: C/AC, +/-, %, ÷
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CalculatorButton(
            text = if (calcState.display != "0") "C" else "AC",
            modifier = Modifier.weight(1f),
            containerColor = KeypadFunction,
            contentColor = KeypadFunctionText,
            testTag = "btn_clear",
            onClick = { viewModel.onCalculatorClear() }
          )
          CalculatorButton(
            text = "±",
            modifier = Modifier.weight(1f),
            containerColor = KeypadFunction,
            contentColor = KeypadFunctionText,
            testTag = "btn_negate",
            onClick = { viewModel.onCalculatorNegate() }
          )
          CalculatorButton(
            text = "%",
            modifier = Modifier.weight(1f),
            containerColor = KeypadFunction,
            contentColor = KeypadFunctionText,
            testTag = "btn_percent",
            onClick = { viewModel.onCalculatorPercentage() }
          )
          CalculatorButton(
            text = "÷",
            modifier = Modifier.weight(1f),
            containerColor = KeypadOperator,
            contentColor = KeypadOperatorAccent,
            testTag = "btn_divide",
            onClick = { viewModel.onCalculatorOperator("÷") }
          )
        }

        // Row 2: 7, 8, 9, ×
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CalculatorButton(
            text = "7",
            modifier = Modifier.weight(1f),
            testTag = "btn_7",
            onClick = { viewModel.onCalculatorDigit('7') }
          )
          CalculatorButton(
            text = "8",
            modifier = Modifier.weight(1f),
            testTag = "btn_8",
            onClick = { viewModel.onCalculatorDigit('8') }
          )
          CalculatorButton(
            text = "9",
            modifier = Modifier.weight(1f),
            testTag = "btn_9",
            onClick = { viewModel.onCalculatorDigit('9') }
          )
          CalculatorButton(
            text = "×",
            modifier = Modifier.weight(1f),
            containerColor = KeypadOperator,
            contentColor = KeypadOperatorAccent,
            testTag = "btn_multiply",
            onClick = { viewModel.onCalculatorOperator("×") }
          )
        }

        // Row 3: 4, 5, 6, −
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CalculatorButton(
            text = "4",
            modifier = Modifier.weight(1f),
            testTag = "btn_4",
            onClick = { viewModel.onCalculatorDigit('4') }
          )
          CalculatorButton(
            text = "5",
            modifier = Modifier.weight(1f),
            testTag = "btn_5",
            onClick = { viewModel.onCalculatorDigit('5') }
          )
          CalculatorButton(
            text = "6",
            modifier = Modifier.weight(1f),
            testTag = "btn_6",
            onClick = { viewModel.onCalculatorDigit('6') }
          )
          CalculatorButton(
            text = "−",
            modifier = Modifier.weight(1f),
            containerColor = KeypadOperator,
            contentColor = KeypadOperatorAccent,
            testTag = "btn_subtract",
            onClick = { viewModel.onCalculatorOperator("−") }
          )
        }

        // Row 4: 1, 2, 3, +
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CalculatorButton(
            text = "1",
            modifier = Modifier.weight(1f),
            testTag = "btn_1",
            onClick = { viewModel.onCalculatorDigit('1') }
          )
          CalculatorButton(
            text = "2",
            modifier = Modifier.weight(1f),
            testTag = "btn_2",
            onClick = { viewModel.onCalculatorDigit('2') }
          )
          CalculatorButton(
            text = "3",
            modifier = Modifier.weight(1f),
            testTag = "btn_3",
            onClick = { viewModel.onCalculatorDigit('3') }
          )
          CalculatorButton(
            text = "+",
            modifier = Modifier.weight(1f),
            containerColor = KeypadOperator,
            contentColor = KeypadOperatorAccent,
            testTag = "btn_add",
            onClick = { viewModel.onCalculatorOperator("+") }
          )
        }

        // Row 5: 0, ., Backspace, = (Activation Key)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          CalculatorButton(
            text = "0",
            modifier = Modifier.weight(1f),
            testTag = "btn_0",
            onClick = { viewModel.onCalculatorDigit('0') }
          )
          CalculatorButton(
            text = ".",
            modifier = Modifier.weight(1f),
            testTag = "btn_dot",
            onClick = { viewModel.onCalculatorDecimal() }
          )
          CalculatorIconButton(
            icon = Icons.AutoMirrored.Filled.Backspace,
            modifier = Modifier.weight(1f),
            containerColor = KeypadDarkNum,
            contentColor = LcdTextSecondary,
            testTag = "btn_backspace",
            onClick = { viewModel.onCalculatorBackspace() }
          )
          CalculatorButton(
            text = "=",
            modifier = Modifier.weight(1f),
            containerColor = KeypadEqualAccent,
            contentColor = Color.White,
            isEqualKey = true,
            testTag = "btn_equals",
            onClick = { viewModel.onCalculatorEquals() }
          )
        }
      }
    }
  }

  // Initial Secret PIN Setup Confirmation Dialog
  if (pinSetupCandidate != null) {
    AlertDialog(
      onDismissRequest = { viewModel.dismissPinSetupCandidate() },
      title = {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = LcdAccentCyan,
            modifier = Modifier.size(24.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text("Set Secret PIN", color = Color.White, fontWeight = FontWeight.Bold)
        }
      },
      text = {
        Column {
          Text(
            text = "Confirm passcode for your Secret Vault:",
            color = LcdTextSecondary,
            fontSize = 14.sp
          )
          Spacer(modifier = Modifier.height(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(LcdScreenBackground)
              .border(1.dp, LcdAccentCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
              .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = pinSetupCandidate ?: "",
              fontSize = 32.sp,
              fontWeight = FontWeight.Bold,
              fontFamily = FontFamily.Monospace,
              color = LcdAccentCyan,
              letterSpacing = 8.sp
            )
          }
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "From now on, typing $pinSetupCandidate and pressing [ = ] in the calculator will unlock your hidden vault.",
            color = LcdTextSecondary,
            fontSize = 12.sp,
            lineHeight = 16.sp
          )
        }
      },
      confirmButton = {
        Button(
          onClick = { viewModel.confirmInitialPin(pinSetupCandidate!!) },
          colors = ButtonDefaults.buttonColors(containerColor = KeypadEqualAccent),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.testTag("confirm_set_pin_btn")
        ) {
          Text("Confirm & Open Vault", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
      },
      dismissButton = {
        OutlinedButton(
          onClick = { viewModel.dismissPinSetupCandidate() },
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Cancel", color = LcdTextSecondary)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(18.dp)
    )
  }
}

@Composable
fun CalculatorButton(
  text: String,
  modifier: Modifier = Modifier,
  containerColor: Color = KeypadDarkNum,
  contentColor: Color = Color.White,
  isEqualKey: Boolean = false,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1.0f,
    label = "btn_press_scale"
  )

  val borderBrush = if (isEqualKey) {
    Brush.verticalGradient(
      listOf(LcdAccentCyan.copy(alpha = 0.9f), Color(0xFF0077B6))
    )
  } else {
    Brush.verticalGradient(
      listOf(DarkSurfaceBorder.copy(alpha = 0.8f), DarkSurfaceBorder.copy(alpha = 0.2f))
    )
  }

  Box(
    modifier = modifier
      .aspectRatio(1.0f)
      .scale(scale)
      .clip(RoundedCornerShape(20.dp))
      .background(containerColor)
      .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      color = contentColor,
      fontSize = if (isEqualKey) 28.sp else 24.sp,
      fontWeight = if (isEqualKey) FontWeight.Bold else FontWeight.Medium
    )
  }
}

@Composable
fun CalculatorIconButton(
  icon: ImageVector,
  modifier: Modifier = Modifier,
  containerColor: Color = KeypadDarkNum,
  contentColor: Color = Color.White,
  testTag: String,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1.0f,
    label = "btn_press_icon_scale"
  )

  Box(
    modifier = modifier
      .aspectRatio(1.0f)
      .scale(scale)
      .clip(RoundedCornerShape(20.dp))
      .background(containerColor)
      .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(20.dp))
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Icon(
      imageVector = icon,
      contentDescription = "Backspace",
      tint = contentColor,
      modifier = Modifier.size(24.dp)
    )
  }
}
