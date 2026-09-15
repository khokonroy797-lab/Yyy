package com.example.data.model

enum class VaultType {
  PHOTO,
  VIDEO,
  DOCUMENT
}

data class VaultItem(
  val id: String,
  val name: String,
  val type: VaultType,
  val filePath: String,
  val fileSizeBytes: Long,
  val timestamp: Long,
  val mimeType: String,
  val isSample: Boolean = false,
  val sampleDrawableId: Int? = null,
  val sampleContent: String? = null
)

enum class PinVerificationResult {
  MATCH_PRIMARY,
  MATCH_DECOY,
  NO_MATCH
}

data class SecurityState(
  val isPinConfigured: Boolean,
  val hasDecoyPin: Boolean,
  val panicExitEnabled: Boolean,
  val isDecoyMode: Boolean = false
)

data class StorageStats(
  val photosBytes: Long,
  val videosBytes: Long,
  val docsBytes: Long,
  val totalFiles: Int
)
