package com.example.ui.vault

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.VaultItem
import com.example.data.model.VaultType
import com.example.ui.MainViewModel
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LcdAccentCyan
import com.example.ui.theme.LcdTextPrimary
import com.example.ui.theme.LcdTextSecondary
import com.example.ui.theme.VaultCoral
import com.example.ui.theme.VaultPrimary
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun MediaViewerDialog(
  item: VaultItem,
  viewModel: MainViewModel,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  var showDeleteConfirm by remember { mutableStateOf(false) }

  val exportLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("*/*")
  ) { destUri: Uri? ->
    if (destUri != null) {
      viewModel.exportItem(item, destUri)
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground)
    ) {
      when (item.type) {
        VaultType.PHOTO -> PhotoViewerContent(item = item)
        VaultType.VIDEO -> VideoViewerContent(item = item)
        VaultType.DOCUMENT -> DocumentViewerContent(item = item)
      }

      // Top Control Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.TopCenter)
          .background(Color.Black.copy(alpha = 0.65f))
          .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onDismiss) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = Color.White
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Text(
              text = item.name,
              color = Color.White,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              maxLines = 1
            )
            Text(
              text = formatFileSize(item.fileSizeBytes),
              color = LcdTextSecondary,
              fontSize = 11.sp
            )
          }
        }

        Row {
          // Export / Unhide
          IconButton(
            onClick = { exportLauncher.launch(item.name) },
            modifier = Modifier.testTag("export_media_btn")
          ) {
            Icon(
              imageVector = Icons.Default.FileDownload,
              contentDescription = "Unhide/Export",
              tint = LcdAccentCyan
            )
          }

          // Share
          IconButton(
            onClick = {
              val shareUri = viewModel.vaultRepository.getFileUri(item)
              if (shareUri != null) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                  type = item.mimeType
                  putExtra(Intent.EXTRA_STREAM, shareUri)
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Secret File"))
              } else {
                viewModel.showNotification("Sharing exported copy of file.")
                exportLauncher.launch(item.name)
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = "Share",
              tint = Color.White
            )
          }

          // Delete
          IconButton(
            onClick = { showDeleteConfirm = true },
            modifier = Modifier.testTag("delete_media_btn")
          ) {
            Icon(
              imageVector = Icons.Default.Delete,
              contentDescription = "Delete",
              tint = VaultCoral
            )
          }
        }
      }
    }
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete From Vault", color = Color.White, fontWeight = FontWeight.Bold) },
      text = {
        Text(
          "Are you sure you want to permanently delete \"${item.name}\" from your secret vault?",
          color = LcdTextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            viewModel.deleteItem(item)
          },
          colors = ButtonDefaults.buttonColors(containerColor = VaultCoral)
        ) {
          Text("Delete Permanently", color = Color.White)
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel", color = LcdTextSecondary)
        }
      },
      containerColor = DarkSurface,
      shape = RoundedCornerShape(16.dp)
    )
  }
}

@Composable
fun PhotoViewerContent(item: VaultItem) {
  var scale by remember { mutableFloatStateOf(1f) }
  var offset by remember { mutableStateOf(Offset.Zero) }

  val transformState = rememberTransformableState { zoomChange, panChange, _ ->
    scale = (scale * zoomChange).coerceIn(1f, 4f)
    offset = if (scale > 1f) offset + panChange else Offset.Zero
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .transformable(state = transformState),
    contentAlignment = Alignment.Center
  ) {
    if (item.isSample && item.sampleDrawableId != null) {
      androidx.compose.foundation.Image(
        painter = painterResource(id = item.sampleDrawableId),
        contentDescription = item.name,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
          ),
        contentScale = ContentScale.Fit
      )
    } else {
      AsyncImage(
        model = File(item.filePath),
        contentDescription = item.name,
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(
            scaleX = scale,
            scaleY = scale,
            translationX = offset.x,
            translationY = offset.y
          ),
        contentScale = ContentScale.Fit
      )
    }
  }
}

@Composable
fun VideoViewerContent(item: VaultItem) {
  var isPlaying by remember { mutableStateOf(true) }
  var progress by remember { mutableFloatStateOf(0.25f) }

  LaunchedEffect(isPlaying) {
    while (isPlaying) {
      delay(200)
      progress = if (progress >= 1f) 0f else progress + 0.01f
    }
  }

  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    // Video poster thumbnail
    if (item.isSample && item.sampleDrawableId != null) {
      androidx.compose.foundation.Image(
        painter = painterResource(id = item.sampleDrawableId),
        contentDescription = item.name,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
      )
    } else {
      AsyncImage(
        model = File(item.filePath),
        contentDescription = item.name,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
      )
    }

    // Video Player Controls Overlay
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.35f)),
      contentAlignment = Alignment.Center
    ) {
      // Big Play / Pause button in center
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.7f))
          .border(2.dp, LcdAccentCyan, CircleShape)
          .clickable { isPlaying = !isPlaying },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
          contentDescription = if (isPlaying) "Pause" else "Play",
          tint = Color.White,
          modifier = Modifier.size(36.dp)
        )
      }

      // Bottom playback progress
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.Black.copy(alpha = 0.75f))
          .padding(horizontal = 20.dp, vertical = 16.dp)
      ) {
        Slider(
          value = progress,
          onValueChange = { progress = it },
          colors = SliderDefaults.colors(
            thumbColor = LcdAccentCyan,
            activeTrackColor = LcdAccentCyan,
            inactiveTrackColor = Color.DarkGray
          ),
          modifier = Modifier.fillMaxWidth()
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val currentSec = (progress * 64).toInt()
          Text(
            text = "%02d:%02d".format(currentSec / 60, currentSec % 60),
            color = Color.White,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
          Text(
            text = "01:04",
            color = LcdTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
          )
        }
      }
    }
  }
}

@Composable
fun DocumentViewerContent(item: VaultItem) {
  val content = item.sampleContent ?: "Document file stored in encrypted sandbox.\nFile: ${item.name}\nSize: ${formatFileSize(item.fileSizeBytes)}\nPath: app-internal://vault/documents/"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(top = 70.dp, bottom = 20.dp, start = 16.dp, end = 16.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .clip(RoundedCornerShape(16.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(16.dp))
        .padding(20.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.Description,
          contentDescription = null,
          tint = LcdAccentCyan,
          modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = item.name,
          color = Color.White,
          fontSize = 16.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Spacer(modifier = Modifier.height(16.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(DarkSurfaceBorder)
      )
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = content,
        color = LcdTextPrimary,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        fontFamily = FontFamily.Monospace
      )
    }
  }
}
