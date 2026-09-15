package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.StorageStats
import com.example.data.model.VaultItem
import com.example.data.model.VaultType
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class VaultRepository(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences(PREFS_VAULT, Context.MODE_PRIVATE)

  private val _itemsFlow = MutableStateFlow<List<VaultItem>>(emptyList())
  val itemsFlow: StateFlow<List<VaultItem>> = _itemsFlow.asStateFlow()

  init {
    loadItems()
  }

  private fun getDirectory(type: VaultType): File {
    val dirName = when (type) {
      VaultType.PHOTO -> "photos"
      VaultType.VIDEO -> "videos"
      VaultType.DOCUMENT -> "documents"
    }
    val dir = File(context.filesDir, "vault/$dirName")
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  private fun loadItems() {
    val storedJson = prefs.getString(KEY_ITEMS_JSON, null)
    val list = mutableListOf<VaultItem>()

    if (storedJson.isNullOrEmpty()) {
      // Initialize with realistic starter sample files
      list.addAll(createStarterItems())
      saveItems(list)
    } else {
      try {
        val array = JSONArray(storedJson)
        for (i in 0 until array.length()) {
          val obj = array.getJSONObject(i)
          list.add(
            VaultItem(
              id = obj.getString("id"),
              name = obj.getString("name"),
              type = VaultType.valueOf(obj.getString("type")),
              filePath = obj.getString("filePath"),
              fileSizeBytes = obj.getLong("fileSizeBytes"),
              timestamp = obj.getLong("timestamp"),
              mimeType = obj.optString("mimeType", "*/*"),
              isSample = obj.optBoolean("isSample", false),
              sampleDrawableId = if (obj.has("sampleDrawableId")) obj.getInt("sampleDrawableId") else null,
              sampleContent = obj.optString("sampleContent", null)
            )
          )
        }
      } catch (e: Exception) {
        list.addAll(createStarterItems())
      }
    }
    _itemsFlow.value = list
  }

  private fun createStarterItems(): List<VaultItem> {
    val now = System.currentTimeMillis()
    return listOf(
      VaultItem(
        id = "sample_photo_1",
        name = "Secret_Destination_Alpine.jpg",
        type = VaultType.PHOTO,
        filePath = "",
        fileSizeBytes = 948200L,
        timestamp = now - 86400000L * 2,
        mimeType = "image/jpeg",
        isSample = true,
        sampleDrawableId = R.drawable.img_vault_photo_nature
      ),
      VaultItem(
        id = "sample_photo_2",
        name = "Cyber_Night_Grid.jpg",
        type = VaultType.PHOTO,
        filePath = "",
        fileSizeBytes = 1245000L,
        timestamp = now - 86400000L,
        mimeType = "image/jpeg",
        isSample = true,
        sampleDrawableId = R.drawable.img_vault_photo_city
      ),
      VaultItem(
        id = "sample_video_1",
        name = "Stealth_Demo_Walkthrough.mp4",
        type = VaultType.VIDEO,
        filePath = "",
        fileSizeBytes = 4194304L,
        timestamp = now - 3600000L * 12,
        mimeType = "video/mp4",
        isSample = true,
        sampleDrawableId = R.drawable.img_calculator_icon,
        sampleContent = "Encrypted video container: Vault security test stream (4.2 MB)."
      ),
      VaultItem(
        id = "sample_doc_1",
        name = "Confidential_Recovery_Keys.txt",
        type = VaultType.DOCUMENT,
        filePath = "",
        fileSizeBytes = 1024L,
        timestamp = now - 3600000L * 6,
        mimeType = "text/plain",
        isSample = true,
        sampleContent = """
          =========================================
          CALCULATOR VAULT - ENCRYPTED LOG
          Architect: KHOKON ROY
          Storage Mode: Scoped Internal App Storage
          Status: Operational & Secured
          =========================================

          - Secret PIN triggers vault immediately upon typing 4 digits + [=].
          - Photos, Videos, and Documents stored here are completely isolated
            from external gallery scanners.
          - Use Export to unhide any item back to device storage.
        """.trimIndent()
      )
    )
  }

  private fun saveItems(items: List<VaultItem>) {
    val array = JSONArray()
    for (item in items) {
      val obj = JSONObject().apply {
        put("id", item.id)
        put("name", item.name)
        put("type", item.type.name)
        put("filePath", item.filePath)
        put("fileSizeBytes", item.fileSizeBytes)
        put("timestamp", item.timestamp)
        put("mimeType", item.mimeType)
        put("isSample", item.isSample)
        item.sampleDrawableId?.let { put("sampleDrawableId", it) }
        item.sampleContent?.let { put("sampleContent", it) }
      }
      array.put(obj)
    }
    prefs.edit().putString(KEY_ITEMS_JSON, array.toString()).apply()
    _itemsFlow.value = items
  }

  suspend fun importFromUri(uri: Uri, type: VaultType): VaultItem? = withContext(Dispatchers.IO) {
    val contentResolver: ContentResolver = context.contentResolver
    var fileName = "imported_${System.currentTimeMillis()}"
    var fileSize = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
      val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
      if (cursor.moveToFirst()) {
        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
      }
    }

    val dir = getDirectory(type)
    val safeExtension = when (type) {
      VaultType.PHOTO -> if (fileName.contains(".")) "" else ".jpg"
      VaultType.VIDEO -> if (fileName.contains(".")) "" else ".mp4"
      VaultType.DOCUMENT -> if (fileName.contains(".")) "" else ".bin"
    }
    val safeFileName = "${UUID.randomUUID()}_$fileName$safeExtension"
    val targetFile = File(dir, safeFileName)

    try {
      contentResolver.openInputStream(uri)?.use { input: InputStream ->
        FileOutputStream(targetFile).use { output ->
          input.copyTo(output)
        }
      } ?: return@withContext null

      val actualSize = if (fileSize > 0) fileSize else targetFile.length()
      val mimeType = contentResolver.getType(uri) ?: when (type) {
        VaultType.PHOTO -> "image/*"
        VaultType.VIDEO -> "video/*"
        VaultType.DOCUMENT -> "application/*"
      }

      val newItem = VaultItem(
        id = UUID.randomUUID().toString(),
        name = fileName,
        type = type,
        filePath = targetFile.absolutePath,
        fileSizeBytes = actualSize,
        timestamp = System.currentTimeMillis(),
        mimeType = mimeType,
        isSample = false
      )

      val current = _itemsFlow.value.toMutableList()
      current.add(0, newItem)
      saveItems(current)
      return@withContext newItem
    } catch (e: Exception) {
      e.printStackTrace()
      return@withContext null
    }
  }

  suspend fun exportFile(item: VaultItem, destinationUri: Uri): Boolean = withContext(Dispatchers.IO) {
    try {
      val contentResolver = context.contentResolver
      val outputStream = contentResolver.openOutputStream(destinationUri) ?: return@withContext false

      outputStream.use { out ->
        if (item.isSample) {
          if (item.sampleContent != null) {
            out.write(item.sampleContent.toByteArray(Charsets.UTF_8))
          } else if (item.sampleDrawableId != null) {
            context.resources.openRawResource(item.sampleDrawableId).use { input ->
              input.copyTo(out)
            }
          }
        } else {
          val file = File(item.filePath)
          if (file.exists()) {
            file.inputStream().use { input ->
              input.copyTo(out)
            }
          } else {
            return@withContext false
          }
        }
      }
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  suspend fun deleteItem(item: VaultItem): Boolean = withContext(Dispatchers.IO) {
    if (!item.isSample && item.filePath.isNotEmpty()) {
      val file = File(item.filePath)
      if (file.exists()) {
        file.delete()
      }
    }
    val current = _itemsFlow.value.toMutableList()
    current.removeAll { it.id == item.id }
    saveItems(current)
    true
  }

  fun getFileUri(item: VaultItem): Uri? {
    if (item.isSample) return null
    val file = File(item.filePath)
    if (!file.exists()) return null
    return try {
      FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
      null
    }
  }

  fun getStorageStats(): StorageStats {
    var photos = 0L
    var videos = 0L
    var docs = 0L
    val items = _itemsFlow.value
    for (item in items) {
      when (item.type) {
        VaultType.PHOTO -> photos += item.fileSizeBytes
        VaultType.VIDEO -> videos += item.fileSizeBytes
        VaultType.DOCUMENT -> docs += item.fileSizeBytes
      }
    }
    return StorageStats(photos, videos, docs, items.size)
  }

  companion object {
    private const val PREFS_VAULT = "calc_vault_storage"
    private const val KEY_ITEMS_JSON = "key_vault_items_json"
  }
}
