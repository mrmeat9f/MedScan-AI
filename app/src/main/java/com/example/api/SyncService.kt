package com.example.api

import android.util.Log
import com.example.data.Medicine
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import java.security.MessageDigest

@JsonClass(generateAdapter = true)
data class SharedCabinetPayload(
    val passwordHash: String,
    val medicines: List<SyncMedicineDto>,
    val deletedMedicines: Map<String, Long>? = emptyMap()
)

fun scaleAndCompressImageFile(inputFile: java.io.File, maxDimension: Int = 300, quality: Int = 75): ByteArray? {
    if (!inputFile.exists()) return null
    return try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        android.graphics.BitmapFactory.decodeFile(inputFile.absolutePath, options)
        
        val srcWidth = options.outWidth
        val srcHeight = options.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null
        
        var sampleSize = 1
        while ((srcWidth / sampleSize) > maxDimension * 2 || (srcHeight / sampleSize) > maxDimension * 2) {
            sampleSize *= 2
        }
        
        val decodeOptions = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = android.graphics.BitmapFactory.decodeFile(inputFile.absolutePath, decodeOptions) ?: return null
        
        val width = bitmap.width
        val height = bitmap.height
        val scale = Math.min(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        val finalBitmap = if (scale < 1.0f) {
            val targetWidth = (width * scale).toInt()
            val targetHeight = (height * scale).toInt()
            android.graphics.Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }
        
        val outputStream = java.io.ByteArrayOutputStream()
        finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, outputStream)
        
        if (finalBitmap !== bitmap) {
            finalBitmap.recycle()
        }
        bitmap.recycle()
        
        outputStream.toByteArray()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@JsonClass(generateAdapter = true)
data class SyncMedicineDto(
    val name: String,
    val gtin: String,
    val serial: String,
    val expirationDate: String,
    val expirationTimestamp: Long,
    val scannedAt: Long,
    val batch: String,
    val notes: String,
    val totalPackageCount: Int,
    val remainingCount: Double,
    val intakeDosage: Double,
    val intakeFrequency: String,
    val lastIntakeDecayTimestamp: Long,
    val packageImagePath: String? = null,
    val packageImageBase64: String? = null,
    val tags: String = ""
) {
    fun toEntity(context: android.content.Context): Medicine {
        var localPath = packageImagePath
        if (!packageImageBase64.isNullOrBlank()) {
            try {
                val bytes = android.util.Base64.decode(packageImageBase64, android.util.Base64.NO_WRAP)
                val destDir = java.io.File(context.filesDir, "package_images")
                if (!destDir.exists()) destDir.mkdirs()
                
                val cleanSerial = serial.replace(Regex("[^a-zA-Z0-9]"), "_")
                val destFile = java.io.File(destDir, "img_${cleanSerial}.jpg")
                destFile.writeBytes(bytes)
                localPath = destFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return Medicine(
            name = name,
            gtin = gtin,
            serial = serial,
            expirationDate = expirationDate,
            expirationTimestamp = expirationTimestamp,
            scannedAt = scannedAt,
            batch = batch,
            notes = notes,
            totalPackageCount = totalPackageCount,
            remainingCount = remainingCount,
            intakeDosage = intakeDosage,
            intakeFrequency = intakeFrequency,
            lastIntakeDecayTimestamp = lastIntakeDecayTimestamp,
            packageImagePath = localPath,
            tags = tags
        )
    }

    companion object {
        fun fromEntity(entity: Medicine): SyncMedicineDto {
            val base64 = entity.packageImagePath?.let { path ->
                val file = java.io.File(path)
                if (file.exists()) {
                    try {
                        val bytes = scaleAndCompressImageFile(file, maxDimension = 300, quality = 70)
                        if (bytes != null) {
                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
            }
            return SyncMedicineDto(
                name = entity.name,
                gtin = entity.gtin,
                serial = entity.serial,
                expirationDate = entity.expirationDate,
                expirationTimestamp = entity.expirationTimestamp,
                scannedAt = entity.scannedAt,
                batch = entity.batch,
                notes = entity.notes,
                totalPackageCount = entity.totalPackageCount,
                remainingCount = entity.remainingCount,
                intakeDosage = entity.intakeDosage,
                intakeFrequency = entity.intakeFrequency,
                lastIntakeDecayTimestamp = entity.lastIntakeDecayTimestamp,
                packageImagePath = entity.packageImagePath,
                packageImageBase64 = base64,
                tags = entity.tags
            )
        }
    }
}

interface SyncCabinetApi {
    @GET("{accountId}")
    suspend fun getCabinet(
        @Path("accountId") accountId: String
    ): SharedCabinetPayload

    @PUT("{accountId}")
    suspend fun uploadCabinet(
        @Path("accountId") accountId: String,
        @Body payload: SharedCabinetPayload
    ): ResponseBody
}

sealed interface SyncResult {
    data class Success(val mergedList: List<Medicine>, val mergedDeletedMap: Map<String, Long>, val actionType: String) : SyncResult
    data class Error(val message: String) : SyncResult
}

object SyncService {
    private const val TAG = "SyncService"
    
    // Custom isolated random bucket generated for MedScan AI Studio Sync
    private const val BASE_URL = "https://kvdb.io/GEbsx2FZtQHHH6dAq5hXEb/"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    val apiService: SyncCabinetApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(SyncCabinetApi::class.java)
    }

    /**
     * Helper to compute SHA-256 hash of password for security
     */
    fun sha256(input: String): String {
        return try {
            val bytes = input.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            input // fallback matching if digest is unavailable
        }
    }

    /**
     * Synchronize local medications with the cloud based on mode:
     * - "merge": Join both datasets. If clashing, the newer lastIntakeDecayTimestamp or scannedAt wins.
     * - "download": Drop local database, fetch cloud database and apply it locally.
     * - "upload": Overwrite the cloud database with our current local medications.
     */
    suspend fun performSync(
        context: android.content.Context,
        accountId: String,
        passphrase: String,
        localMedicines: List<Medicine>,
        localDeletedMap: Map<String, Long> = emptyMap(),
        mode: String = "merge" // "merge", "download", or "upload"
    ): SyncResult = withContext(Dispatchers.IO) {
        val cleanAccountId = accountId.trim().uppercase()
        val passphraseHash = sha256(passphrase.trim())

        try {
            // 1. If mode is "upload", we directly send our local medicines to the cloud overriding remote!
            if (mode == "upload") {
                val uploadPayload = SharedCabinetPayload(
                    passwordHash = passphraseHash,
                    medicines = localMedicines.map { SyncMedicineDto.fromEntity(it) },
                    deletedMedicines = localDeletedMap
                )
                apiService.uploadCabinet(cleanAccountId, uploadPayload)
                return@withContext SyncResult.Success(localMedicines, localDeletedMap, "Загрузка в облако завершена!")
            }

            // 2. Fetch existing cloud payload
            var remotePayload: SharedCabinetPayload? = null
            var remoteDoesNotExist = false
            try {
                remotePayload = apiService.getCabinet(cleanAccountId)
            } catch (e: retrofit2.HttpException) {
                if (e.code() == 404) {
                    remoteDoesNotExist = true
                } else {
                    throw e
                }
            } catch (e: Exception) {
                // Return descriptive error
                Log.e(TAG, "Failed downloading sync package", e)
                return@withContext SyncResult.Error("Не удалось подключиться к серверу синхронизации. Проверьте интернет.")
            }

            // 3. Handle security check if remote package exists
            if (remotePayload != null && remotePayload.passwordHash != passphraseHash) {
                return@withContext SyncResult.Error("Ошибка: Пароль для аккаунта $cleanAccountId не совпадает с введенным!")
            }

            // 4. Resolve download mode: replace local data completely with cloud data
            if (mode == "download") {
                if (remoteDoesNotExist || remotePayload == null) {
                    return@withContext SyncResult.Success(emptyList(), emptyMap(), "Аптечка в облаке еще не была сохранена.")
                }
                val downloadedList = remotePayload.medicines.map { it.toEntity(context) }
                val downloadedDeletedMap = remotePayload.deletedMedicines ?: emptyMap()
                return@withContext SyncResult.Success(downloadedList, downloadedDeletedMap, "Скачивание из облака завершено!")
            }

            // 5. Default "merge" mode
            val remoteList = remotePayload?.medicines ?: emptyList()
            val remoteDeletedMap = remotePayload?.deletedMedicines ?: emptyMap()
            
            // Map list elements by serial for easy matching. Manual items also have generated SERIAL.
            val localMap = localMedicines.associateBy { it.serial }
            val remoteMap = remoteList.associateBy { it.serial }

            // 1) Merge deletions: take the maximum deletion timestamp for each deleted serial
            val mergedDeletedMap = (localDeletedMap.keys + remoteDeletedMap.keys).associateWith { serial ->
                maxOf(localDeletedMap[serial] ?: 0L, remoteDeletedMap[serial] ?: 0L)
            }.toMutableMap()

            val mergedResultMap = mutableMapOf<String, Medicine>()

            // A helper to get the highest update timestamp of a medication
            fun getMedUpdateTime(scannedAt: Long, lastIntakeDecay: Long): Long {
                return maxOf(scannedAt, lastIntakeDecay)
            }

            // Union of all serials to process systematically
            val allSerials = localMap.keys + remoteMap.keys

            allSerials.forEach { serial ->
                val localMed = localMap[serial]
                val remoteMed = remoteMap[serial]
                val deletedTime = mergedDeletedMap[serial]

                val localTime = localMed?.let { getMedUpdateTime(it.scannedAt, it.lastIntakeDecayTimestamp) } ?: 0L
                val remoteTime = remoteMed?.let { getMedUpdateTime(it.scannedAt, it.lastIntakeDecayTimestamp) } ?: 0L
                val newestUpdateTime = maxOf(localTime, remoteTime)

                if (deletedTime != null && newestUpdateTime <= deletedTime) {
                    // The deletion is newer or matches. It is indeed deleted, so discard it!
                } else {
                    // It is active (or has a newer update/re-addition after deletion)
                    if (deletedTime != null) {
                        // Reintroduced with a newer update, so remove it from the merged deleted map
                        mergedDeletedMap.remove(serial)
                    }

                    if (localMed != null && remoteMed != null) {
                        val keepRemote = remoteTime > localTime
                        if (keepRemote) {
                            mergedResultMap[serial] = remoteMed.toEntity(context).copy(id = localMed.id)
                        } else {
                            mergedResultMap[serial] = localMed
                        }
                    } else if (localMed != null) {
                        mergedResultMap[serial] = localMed
                    } else if (remoteMed != null) {
                        mergedResultMap[serial] = remoteMed.toEntity(context).copy(id = 0)
                    }
                }
            }

            val finalMergedList = mergedResultMap.values.toList()

            // Save the merged result back to the cloud so both stay in sync!
            val finalUploadPayload = SharedCabinetPayload(
                passwordHash = passphraseHash,
                medicines = finalMergedList.map { SyncMedicineDto.fromEntity(it) },
                deletedMedicines = mergedDeletedMap
            )
            apiService.uploadCabinet(cleanAccountId, finalUploadPayload)

            SyncResult.Success(finalMergedList, mergedDeletedMap, "Синхронизация и объединение аптечки завершены успешно!")
        } catch (e: Exception) {
            Log.e(TAG, "Sync error performing PERFORM_SYNC", e)
            SyncResult.Error("К сожалению, произошла ошибка во время синхронизации: ${e.localizedMessage}")
        }
    }
}
