package com.example.ui.vault

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.VaultItem
import com.example.data.model.VaultType
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(viewModel: MainViewModel) {
  val selectedTab by viewModel.selectedTab.collectAsState()
  val filteredItems by viewModel.filteredItems.collectAsState()
  val allItems by viewModel.vaultItems.collectAsState()
  val securityState by viewModel.securityState.collectAsState()

  var showImportSheet by remember { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState()

  // System Pickers for Importing Files
  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let { viewModel.importMedia(it, VaultType.PHOTO) }
  }

  val videoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    uri?.let { viewModel.importMedia(it, VaultType.VIDEO) }
  }

  val docPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    uri?.let { viewModel.importMedia(it, VaultType.DOCUMENT) }
  }

  // File exporter launcher
  var itemToExport by remember { mutableStateOf<VaultItem?>(null) }
  val exportFileLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("*/*")
  ) { destUri: Uri? ->
    if (destUri != null && itemToExport != null) {
      viewModel.exportItem(itemToExport!!, destUri)
      itemToExport = null
    }
  }

  Scaffold(
    containerColor = DarkBackground,
    topBar = {
      VaultTopAppBar(
        isDecoy = securityState.isDecoyMode,
        onPanicLock = { viewModel.panicLock() },
        onOpenSettings = { viewModel.navigateTo(AppScreen.SETTINGS) }
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = { showImportSheet = true },
        icon = { Icon(Icons.Default.Add, contentDescription = "Import") },
        text = { Text("Import Files", fontWeight = FontWeight.SemiBold) },
        containerColor = VaultPrimary,
        contentColor = Color.White,
        modifier = Modifier.testTag("import_fab")
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {

      // Category Tabs (Photos, Videos, Documents)
      val photoCount = if (securityState.isDecoyMode) 0 else allItems.count { it.type == VaultType.PHOTO }
      val videoCount = if (securityState.isDecoyMode) 0 else allItems.count { it.type == VaultType.VIDEO }
      val docCount = if (securityState.isDecoyMode) 0 else allItems.count { it.type == VaultType.DOCUMENT }

      TabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = DarkSurface,
        contentColor = LcdAccentCyan,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
            color = LcdAccentCyan,
            height = 3.dp
          )
        },
        divider = {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(1.dp)
              .background(DarkSurfaceBorder)
          )
        }
      ) {
        VaultTabItem(
          title = "Photos",
          icon = Icons.Default.Image,
          count = photoCount,
          isSelected = selectedTab == VaultType.PHOTO,
          onClick = { viewModel.selectVaultTab(VaultType.PHOTO) },
          testTag = "tab_photos"
        )
        VaultTabItem(
          title = "Videos",
          icon = Icons.Default.Movie,
          count = videoCount,
          isSelected = selectedTab == VaultType.VIDEO,
          onClick = { viewModel.selectVaultTab(VaultType.VIDEO) },
          testTag = "tab_videos"
        )
        VaultTabItem(
          title = "Documents",
          icon = Icons.Default.Description,
          count = docCount,
          isSelected = selectedTab == VaultType.DOCUMENT,
          onClick = { viewModel.selectVaultTab(VaultType.DOCUMENT) },
          testTag = "tab_docs"
        )
      }

      // Vault Items Grid / Empty State
      if (filteredItems.isEmpty()) {
        VaultEmptyState(
          type = selectedTab,
          onImportClick = { showImportSheet = true }
        )
      } else {
        LazyVerticalGrid(
          columns = GridCells.Fixed(if (selectedTab == VaultType.DOCUMENT) 1 else 2),
          contentPadding = PaddingValues(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier
            .fillMaxSize()
            .testTag("vault_grid")
        ) {
          items(filteredItems, key = { it.id }) { item ->
            when (item.type) {
              VaultType.PHOTO -> {
                VaultPhotoCard(
                  item = item,
                  onClick = { viewModel.openViewer(item) },
                  onExport = {
                    itemToExport = item
                    exportFileLauncher.launch(item.name)
                  },
                  onDelete = { viewModel.deleteItem(item) }
                )
              }
              VaultType.VIDEO -> {
                VaultVideoCard(
                  item = item,
                  onClick = { viewModel.openViewer(item) },
                  onExport = {
                    itemToExport = item
                    exportFileLauncher.launch(item.name)
                  },
                  onDelete = { viewModel.deleteItem(item) }
                )
              }
              VaultType.DOCUMENT -> {
                VaultDocumentCard(
                  item = item,
                  onClick = { viewModel.openViewer(item) },
                  onExport = {
                    itemToExport = item
                    exportFileLauncher.launch(item.name)
                  },
                  onDelete = { viewModel.deleteItem(item) }
                )
              }
            }
          }
        }
      }
    }
  }

  // Import Action Bottom Sheet
  if (showImportSheet) {
    ModalBottomSheet(
      onDismissRequest = { showImportSheet = false },
      sheetState = sheetState,
      containerColor = DarkSurfaceElevated,
      dragHandle = {
        Box(
          modifier = Modifier
            .padding(vertical = 12.dp)
            .size(width = 38.dp, height = 4.dp)
            .clip(CircleShape)
            .background(DarkSurfaceBorder)
        )
      }
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 8.dp)
          .padding(bottom = 32.dp)
      ) {
        Text(
          text = "Import to Secret Vault",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "Selected files will be moved into app-private encrypted storage.",
          color = LcdTextSecondary,
          fontSize = 13.sp,
          modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

        ImportOptionTile(
          icon = Icons.Default.Image,
          iconTint = LcdAccentCyan,
          title = "Import Photos",
          subtitle = "Select photos from device gallery",
          testTag = "import_photos_option",
          onClick = {
            showImportSheet = false
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }
        )

        ImportOptionTile(
          icon = Icons.Default.Movie,
          iconTint = VaultCoral,
          title = "Import Videos",
          subtitle = "Select video clips to hide",
          testTag = "import_videos_option",
          onClick = {
            showImportSheet = false
            videoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
          }
        )

        ImportOptionTile(
          icon = Icons.Default.Description,
          iconTint = VaultEmerald,
          title = "Import Documents",
          subtitle = "Select PDF, TXT, DOC or secret files",
          testTag = "import_docs_option",
          onClick = {
            showImportSheet = false
            docPickerLauncher.launch(arrayOf("*/*"))
          }
        )
      }
    }
  }
}

@Composable
fun VaultTopAppBar(
  isDecoy: Boolean,
  onPanicLock: () -> Unit,
  onOpenSettings: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkSurface)
      .border(0.dp, Color.Transparent)
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(listOf(VaultPrimary, Color(0xFF0077B6)))),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = if (isDecoy) "Decoy Vault" else "Secret Vault",
              color = Color.White,
              fontSize = 18.sp,
              fontWeight = FontWeight.Bold
            )
            if (isDecoy) {
              Spacer(modifier = Modifier.width(6.dp))
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(VaultCoral.copy(alpha = 0.2f))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  text = "DECOY",
                  color = VaultCoral,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
          Text(
            text = "App-Private Internal Storage",
            color = LcdTextSecondary,
            fontSize = 11.sp
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        // Emergency Panic Lock Button
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(VaultCoral.copy(alpha = 0.15f))
            .clickable(onClick = onPanicLock)
            .padding(horizontal = 10.dp, vertical = 7.dp)
            .testTag("panic_lock_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = "Panic Lock",
              tint = VaultCoral,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Lock",
              color = VaultCoral,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Settings Button (Opens Developer Profile + PIN changes)
        IconButton(
          onClick = onOpenSettings,
          modifier = Modifier.testTag("vault_settings_btn")
        ) {
          Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = "Vault Settings",
            tint = LcdTextPrimary
          )
        }
      }
    }
  }
}

@Composable
fun VaultTabItem(
  title: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  count: Int,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  Tab(
    selected = isSelected,
    onClick = onClick,
    modifier = Modifier.testTag(testTag)
  ) {
    Row(
      modifier = Modifier.padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = if (isSelected) LcdAccentCyan else LcdTextSecondary,
        modifier = Modifier.size(18.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = title,
        color = if (isSelected) Color.White else LcdTextSecondary,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 13.sp
      )
      if (count > 0) {
        Spacer(modifier = Modifier.width(6.dp))
        Box(
          modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) LcdAccentCyan else DarkSurfaceBorder)
            .padding(horizontal = 6.dp, vertical = 1.dp)
        ) {
          Text(
            text = count.toString(),
            color = if (isSelected) Color.Black else LcdTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
fun VaultPhotoCard(
  item: VaultItem,
  onClick: () -> Unit,
  onExport: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(0.9f)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("photo_card_${item.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkSurfaceBorder, Color.Transparent)), width = 1.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (item.isSample && item.sampleDrawableId != null) {
        androidx.compose.foundation.Image(
          painter = painterResource(id = item.sampleDrawableId),
          contentDescription = item.name,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      } else {
        AsyncImage(
          model = File(item.filePath),
          contentDescription = item.name,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Bottom gradient scrim
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
            )
          )
      )

      // Title & file size
      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
      ) {
        Text(
          text = item.name,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = formatFileSize(item.fileSizeBytes),
          color = LcdTextSecondary,
          fontSize = 10.sp
        )
      }

      // Top corner action menu
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
      ) {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(DarkSurfaceElevated)
        ) {
          DropdownMenuItem(
            text = { Text("Unhide / Export", color = Color.White) },
            onClick = {
              showMenu = false
              onExport()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", color = VaultCoral) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun VaultVideoCard(
  item: VaultItem,
  onClick: () -> Unit,
  onExport: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(0.9f)
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag("video_card_${item.id}"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(DarkSurfaceBorder, Color.Transparent)), width = 1.dp)
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      if (item.isSample && item.sampleDrawableId != null) {
        androidx.compose.foundation.Image(
          painter = painterResource(id = item.sampleDrawableId),
          contentDescription = item.name,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      } else {
        AsyncImage(
          model = File(item.filePath),
          contentDescription = item.name,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop
        )
      }

      // Play button overlay
      Box(
        modifier = Modifier
          .size(44.dp)
          .align(Alignment.Center)
          .clip(CircleShape)
          .background(Color.Black.copy(alpha = 0.65f))
          .border(1.5.dp, VaultCoral, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = "Play Video",
          tint = Color.White,
          modifier = Modifier.size(26.dp)
        )
      }

      // Bottom info
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(60.dp)
          .align(Alignment.BottomCenter)
          .background(
            Brush.verticalGradient(
              listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
            )
          )
      )

      Column(
        modifier = Modifier
          .align(Alignment.BottomStart)
          .padding(8.dp)
      ) {
        Text(
          text = item.name,
          color = Color.White,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        Text(
          text = formatFileSize(item.fileSizeBytes),
          color = LcdTextSecondary,
          fontSize = 10.sp
        )
      }

      // Menu
      Box(
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(4.dp)
      ) {
        IconButton(
          onClick = { showMenu = true },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(DarkSurfaceElevated)
        ) {
          DropdownMenuItem(
            text = { Text("Unhide / Export", color = Color.White) },
            onClick = {
              showMenu = false
              onExport()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", color = VaultCoral) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun VaultDocumentCard(
  item: VaultItem,
  onClick: () -> Unit,
  onExport: () -> Unit,
  onDelete: () -> Unit
) {
  var showMenu by remember { mutableStateOf(false) }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .testTag("doc_card_${item.id}"),
    shape = RoundedCornerShape(14.dp),
    colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DarkSurfaceBorder, Color.Transparent)), width = 1.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Box(
          modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(VaultEmerald.copy(alpha = 0.15f))
            .border(1.dp, VaultEmerald.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Description,
            contentDescription = null,
            tint = VaultEmerald,
            modifier = Modifier.size(24.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Text(
            text = item.name,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(3.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = formatFileSize(item.fileSizeBytes),
              color = LcdTextSecondary,
              fontSize = 12.sp
            )
            Text(
              text = " • ",
              color = LcdTextSecondary,
              fontSize = 12.sp
            )
            Text(
              text = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(item.timestamp)),
              color = LcdTextSecondary,
              fontSize = 12.sp
            )
          }
        }
      }

      Box {
        IconButton(onClick = { showMenu = true }) {
          Icon(
            imageVector = Icons.Default.MoreVert,
            contentDescription = "Options",
            tint = LcdTextSecondary
          )
        }

        DropdownMenu(
          expanded = showMenu,
          onDismissRequest = { showMenu = false },
          modifier = Modifier.background(DarkSurfaceElevated)
        ) {
          DropdownMenuItem(
            text = { Text("Unhide / Export", color = Color.White) },
            onClick = {
              showMenu = false
              onExport()
            }
          )
          DropdownMenuItem(
            text = { Text("Delete", color = VaultCoral) },
            onClick = {
              showMenu = false
              onDelete()
            }
          )
        }
      }
    }
  }
}

@Composable
fun ImportOptionTile(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconTint: Color,
  title: String,
  subtitle: String,
  testTag: String,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 10.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box(
      modifier = Modifier
        .size(46.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(iconTint.copy(alpha = 0.15f))
        .border(1.dp, iconTint.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = iconTint,
        modifier = Modifier.size(24.dp)
      )
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column {
      Text(
        text = title,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = subtitle,
        color = LcdTextSecondary,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
fun VaultEmptyState(
  type: VaultType,
  onImportClick: () -> Unit
) {
  val (label, icon) = when (type) {
    VaultType.PHOTO -> "No hidden photos yet" to Icons.Default.Image
    VaultType.VIDEO -> "No hidden videos yet" to Icons.Default.Movie
    VaultType.DOCUMENT -> "No hidden documents yet" to Icons.Default.Description
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
  ) {
    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(CircleShape)
        .background(DarkSurfaceElevated)
        .border(1.dp, DarkSurfaceBorder, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = LcdTextSecondary,
        modifier = Modifier.size(40.dp)
      )
    }

    Spacer(modifier = Modifier.height(18.dp))

    Text(
      text = label,
      color = Color.White,
      fontSize = 17.sp,
      fontWeight = FontWeight.SemiBold
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = "Tap Import below to protect confidential files in your private vault.",
      color = LcdTextSecondary,
      fontSize = 13.sp,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center,
      lineHeight = 18.sp
    )

    Spacer(modifier = Modifier.height(20.dp))

    androidx.compose.material3.Button(
      onClick = onImportClick,
      colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = VaultPrimary),
      shape = RoundedCornerShape(12.dp)
    ) {
      Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(modifier = Modifier.width(6.dp))
      Text("Import Now", fontWeight = FontWeight.SemiBold)
    }
  }
}

fun formatFileSize(bytes: Long): String {
  if (bytes <= 0) return "0 B"
  val kb = bytes / 1024.0
  val mb = kb / 1024.0
  val gb = mb / 1024.0
  return when {
    gb >= 1.0 -> "%.2f GB".format(gb)
    mb >= 1.0 -> "%.1f MB".format(mb)
    kb >= 1.0 -> "%.1f KB".format(kb)
    else -> "$bytes B"
  }
}
