package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.api.AnalogRecommendResult
import com.example.api.GeminiService
import com.example.api.ParsedZnakResult
import com.example.api.DrugInstruction
import com.example.data.AppDatabase
import com.example.data.Medicine
import com.example.data.MedicineRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

sealed interface AnalogsUiState {
    object Idle : AnalogsUiState
    object Loading : AnalogsUiState
    data class Success(val result: AnalogRecommendResult) : AnalogsUiState
    data class Error(val message: String) : AnalogsUiState
}

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Processing : ScanUiState
    data class Success(val result: ParsedZnakResult, val imagePath: String? = null) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface InstructionUiState {
    object Idle : InstructionUiState
    object Loading : InstructionUiState
    data class Success(val instruction: DrugInstruction) : InstructionUiState
    data class Error(val message: String) : InstructionUiState
}

sealed interface SyncUiState {
    object Idle : SyncUiState
    object Syncing : SyncUiState
    data class Success(val message: String) : SyncUiState
    data class Error(val message: String) : SyncUiState
}

enum class ActiveTab {
    CABINET,   // "Аптечка"
    SCANNER,   // "По фото"
    SEARCH,    // "Поиск аналогов"
    PROFILE,   // "Кабинет"
    SETTINGS,  // "Настройки"
    PILLBOX    // "Таблетницы"
}

enum class CabinetFilter {
    ALL,
    VALID,
    WARNING,
    EXPIRED,
    LOW_STOCK,
    OUT_OF_STOCK
}

data class ScheduleSuggestion(
    val finishedMedicineName: String,
    val sourceDosage: Double,
    val sourceFrequency: String,
    val targetMedicine: Medicine
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application
    private val database = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(database.medicineDao(), database.pillboxDao(), database.intakeLogDao())
    private val syncMutex = Mutex()

    // SharedPreferences for configuration
    private val prefs = application.getSharedPreferences("medscan_prefs", android.content.Context.MODE_PRIVATE)

    private val _appThemeMode = MutableStateFlow(prefs.getInt("theme_mode", 0)) // 0=System, 1=Light, 2=Dark
    val appThemeMode: StateFlow<Int> = _appThemeMode.asStateFlow()

    private val _expirationThresholdDays = MutableStateFlow(prefs.getInt("exp_threshold_days", 90))
    val expirationThresholdDays: StateFlow<Int> = _expirationThresholdDays.asStateFlow()

    private val _quantityThresholdCount = MutableStateFlow(prefs.getInt("quantity_threshold_count", 5))
    val quantityThresholdCount: StateFlow<Int> = _quantityThresholdCount.asStateFlow()

    // In-app alert notification
    private val _inAppAlert = MutableStateFlow<String?>(null)
    val inAppAlert: StateFlow<String?> = _inAppAlert.asStateFlow()

    // --- Shared Account Configuration parameters ---
    private val _syncAccountId = MutableStateFlow(prefs.getString("sync_account_id", "") ?: "")
    val syncAccountId: StateFlow<String> = _syncAccountId.asStateFlow()

    private val _syncPassphrase = MutableStateFlow(prefs.getString("sync_passphrase", "") ?: "")
    val syncPassphrase: StateFlow<String> = _syncPassphrase.asStateFlow()

    private val _syncLastTime = MutableStateFlow(prefs.getLong("sync_last_time", 0L))
    val syncLastTime: StateFlow<Long> = _syncLastTime.asStateFlow()

    private val _syncAutoEnabled = MutableStateFlow(prefs.getBoolean("sync_auto_enabled", true))
    val syncAutoEnabled: StateFlow<Boolean> = _syncAutoEnabled.asStateFlow()

    private val _syncUiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val syncUiState: StateFlow<SyncUiState> = _syncUiState.asStateFlow()

    fun updateSyncAutoEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sync_auto_enabled", enabled).apply()
        _syncAutoEnabled.value = enabled
    }

    fun resetSyncUiState() {
        _syncUiState.value = SyncUiState.Idle
    }

    fun getSyncLastTimeFormatted(): String {
        val lastMills = _syncLastTime.value
        if (lastMills == 0L) return "Ещё не выполнялась"
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale("ru"))
        return sdf.format(Date(lastMills))
    }

    fun updateThemeMode(mode: Int) {
        prefs.edit().putInt("theme_mode", mode).apply()
        _appThemeMode.value = mode
    }

    fun updateExpirationThresholdDays(days: Int) {
        prefs.edit().putInt("exp_threshold_days", days).apply()
        _expirationThresholdDays.value = days
    }

    fun updateQuantityThresholdCount(count: Int) {
        prefs.edit().putInt("quantity_threshold_count", count).apply()
        _quantityThresholdCount.value = count
    }

    fun showInAppAlert(message: String) {
        _inAppAlert.value = message
    }

    fun clearInAppAlert() {
        _inAppAlert.value = null
    }

    init {
        // Clean any duplicate medicines that might have been saved due to concurrent syncs
        cleanDuplicateMedicines()

        // Run daily dosage decrement simulation on launch matching May 28, 2026 current mock date
        viewModelScope.launch {
            repository.allMedicines.collect { list ->
                if (list.isNotEmpty()) {
                    runDailyDecayCheck(list)
                }
            }
        }
        
        // Trigger background initial auto-sync if enabled on startup
        if (prefs.getBoolean("sync_auto_enabled", false) && !prefs.getString("sync_account_id", "").isNullOrBlank()) {
            executeSync("merge")
        }
    }

    private fun cleanDuplicateMedicines() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val list = repository.getAllMedicinesDirect()
                val grouped = list.groupBy { it.serial }
                grouped.forEach { (serial, meds) ->
                    if (meds.size > 1) {
                        // Keep the one with highest scannedAt or id
                        val sorted = meds.sortedWith(compareByDescending<Medicine> { it.scannedAt }.thenByDescending { it.id })
                        val toKeep = sorted.first()
                        val toDelete = sorted.drop(1)
                        toDelete.forEach { duplicate ->
                            repository.delete(duplicate)
                        }
                        Log.d("MainViewModel", "Cleaned up ${toDelete.size} duplicates for serial $serial")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun runDailyDecayCheck(list: List<Medicine>) {
        val currentMills = System.currentTimeMillis()
        viewModelScope.launch {
            var updatedAny = false
            list.forEach { medicine ->
                val freqMultiplier = when (medicine.intakeFrequency) {
                    "1_daily" -> 1.0
                    "2_daily" -> 2.0
                    "3_daily" -> 3.0
                    "every_other_day" -> 0.5
                    else -> 0.0
                }
                if (freqMultiplier > 0.0 && medicine.intakeDosage > 0.0 && medicine.remainingCount > 0.0) {
                    val timeDiff = currentMills - medicine.lastIntakeDecayTimestamp
                    val elapsedDays = (timeDiff / (24 * 60 * 60 * 1000L)).toInt()
                    if (elapsedDays >= 1) {
                        val deduction = medicine.intakeDosage * freqMultiplier * elapsedDays
                        val updatedRemaining = maxOf(0.0, medicine.remainingCount - deduction)
                        val updatedMed = medicine.copy(
                            remainingCount = updatedRemaining,
                            lastIntakeDecayTimestamp = medicine.lastIntakeDecayTimestamp + elapsedDays * 24 * 60 * 60 * 1000L
                        )
                        repository.update(updatedMed)
                        updatedAny = true
                        
                        // Notify
                        if (updatedRemaining <= _quantityThresholdCount.value && updatedRemaining > 0) {
                            showLocalNotification(
                                "Заканчивается ${medicine.name}",
                                "Внимание: препарат ${medicine.name} подходит к концу (осталось ${formatDouble(updatedRemaining)} шт.)."
                            )
                        } else if (updatedRemaining == 0.0) {
                            showLocalNotification(
                                "Препарат закончился",
                                "Внимание: препарат ${medicine.name} полностью закончился!"
                            )
                            checkForScheduleSuggestion(medicine)
                        }
                    }
                }
            }
        }
    }

    fun formatDouble(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
    }

    fun showLocalNotification(title: String, message: String) {
        _inAppAlert.value = "Проводное Push-уведомление:\n$title — $message"
        
        try {
            val context = getApplication<Application>()
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    "medscan_noti_channel",
                    "Напоминания МедСкан",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Уведомления о приеме, окончании срока годности и малом запасе лекарств"
                }
                notificationManager.createNotificationChannel(channel)
            }
            
            val builder = androidx.core.app.NotificationCompat.Builder(context, "medscan_noti_channel")
                .setSmallIcon(com.example.R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText(message))
                
            notificationManager.notify((1000..9999).random(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Tabs / Screen flow
    private val _currentTab = MutableStateFlow(ActiveTab.CABINET)
    val currentTab: StateFlow<ActiveTab> = _currentTab.asStateFlow()

    // Cabinet medicines list
    val medicines: StateFlow<List<Medicine>> = repository.allMedicines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Intake history logs list
    val intakeLogs: StateFlow<List<com.example.data.IntakeLog>> = repository.allIntakeLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun clearIntakeHistory() {
        viewModelScope.launch {
            repository.deleteAllIntakeLogs()
        }
    }

    // Filtered cabinet search
    private val _cabinetSearchQuery = MutableStateFlow("")
    val cabinetSearchQuery: StateFlow<String> = _cabinetSearchQuery.asStateFlow()

    // Dashboard dynamic filter logic
    private val _cabinetFilter = MutableStateFlow(CabinetFilter.ALL)
    val cabinetFilter: StateFlow<CabinetFilter> = _cabinetFilter.asStateFlow()

    val filteredMedicines: StateFlow<List<Medicine>> = combine(
        medicines,
        _cabinetSearchQuery,
        _cabinetFilter
    ) { list, query, filter ->
        var result = list
        if (query.isNotBlank()) {
            result = result.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.gtin.contains(query) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        when (filter) {
            CabinetFilter.ALL -> {}
            CabinetFilter.VALID -> result = result.filter { getExpirationStatus(it) == ExpirationStatus.VALID }
            CabinetFilter.WARNING -> result = result.filter { getExpirationStatus(it) == ExpirationStatus.EXPIRING_SOON }
            CabinetFilter.EXPIRED -> result = result.filter { getExpirationStatus(it) == ExpirationStatus.EXPIRED }
            CabinetFilter.LOW_STOCK -> result = result.filter { it.remainingCount > 0 && it.remainingCount <= _quantityThresholdCount.value }
            CabinetFilter.OUT_OF_STOCK -> result = result.filter { it.remainingCount <= 0 }
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Scan / Parse Status logic
    private val _scanUiState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanUiState: StateFlow<ScanUiState> = _scanUiState.asStateFlow()

    // Search analogs logic
    private val _analogsUiState = MutableStateFlow<AnalogsUiState>(AnalogsUiState.Idle)
    val analogsUiState: StateFlow<AnalogsUiState> = _analogsUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Global selected medicine detail view
    private val _selectedMedicine = MutableStateFlow<Medicine?>(null)
    val selectedMedicine: StateFlow<Medicine?> = _selectedMedicine.asStateFlow()

    // Instruction loading logic
    private val _instructionUiState = MutableStateFlow<InstructionUiState>(InstructionUiState.Idle)
    val instructionUiState: StateFlow<InstructionUiState> = _instructionUiState.asStateFlow()

    // Suggest intake schedule to partner medicine packages when one runs out
    private val _scheduleSuggestion = MutableStateFlow<ScheduleSuggestion?>(null)
    val scheduleSuggestion: StateFlow<ScheduleSuggestion?> = _scheduleSuggestion.asStateFlow()

    fun dismissScheduleSuggestion() {
        _scheduleSuggestion.value = null
    }

    fun applyScheduleSuggestion() {
        val suggestion = _scheduleSuggestion.value ?: return
        viewModelScope.launch {
            val updated = if (suggestion.sourceDosage > 0.0) {
                suggestion.targetMedicine.copy(
                    intakeDosage = suggestion.sourceDosage,
                    intakeFrequency = suggestion.sourceFrequency,
                    lastIntakeDecayTimestamp = System.currentTimeMillis()
                )
            } else {
                suggestion.targetMedicine
            }
            if (suggestion.sourceDosage > 0.0) {
                repository.update(updated)
            }

            // Also, update any Pillbox entries that were referencing `finishedMedicineName`
            // so they now reference `targetMedicine.name`.
            val finishedName = suggestion.finishedMedicineName.trim()
            val targetName = suggestion.targetMedicine.name.trim()
            if (!finishedName.equals(targetName, ignoreCase = true)) {
                val pillboxEntriesList = repository.getAllPillboxEntriesDirect()
                pillboxEntriesList.forEach { entry ->
                    if (entry.medicineName.trim().equals(finishedName, ignoreCase = true)) {
                        val updatedEntry = entry.copy(medicineName = targetName)
                        repository.updatePillboxEntry(updatedEntry)
                    }
                }
            }

            _scheduleSuggestion.value = null
            if (suggestion.sourceDosage > 0.0) {
                showInAppAlert("Препарат и схема приема успешно перенесены на аналогичный: ${updated.name} (серия: ${updated.batch}).")
            } else {
                showInAppAlert("Препарат успешно заменен в таблетнице на аналогичный: ${updated.name} (серия: ${updated.batch}).")
            }
            
            // Auto sync
            triggerBackgroundAutoSync()
        }
    }

    fun checkForScheduleSuggestion(finishedMed: Medicine) {
        viewModelScope.launch {
            val allMeds = medicines.value
            val isSameMedicine: (Medicine, Medicine) -> Boolean = { med1, med2 ->
                if (med1.id == med2.id) false
                else {
                    val name1 = med1.name.trim().lowercase()
                    val name2 = med2.name.trim().lowercase()
                    if (name1 == name2) {
                        true
                    } else {
                        val brand1 = name1.split("[\\s,.-]+".toRegex()).firstOrNull { it.length > 2 } ?: ""
                        val brand2 = name2.split("[\\s,.-]+".toRegex()).firstOrNull { it.length > 2 } ?: ""
                        brand1.isNotEmpty() && brand1 == brand2
                    }
                }
            }

            val partnerCandidates = allMeds.filter { med ->
                isSameMedicine(finishedMed, med) && med.remainingCount > 0.0
            }

            if (partnerCandidates.isNotEmpty()) {
                val target = partnerCandidates.minByOrNull { it.expirationTimestamp }
                if (target != null) {
                    _scheduleSuggestion.value = ScheduleSuggestion(
                        finishedMedicineName = finishedMed.name,
                        sourceDosage = finishedMed.intakeDosage,
                        sourceFrequency = finishedMed.intakeFrequency,
                        targetMedicine = target
                    )
                }
            }
        }
    }

    fun switchTab(tab: ActiveTab) {
        _currentTab.value = tab
    }

    fun setCabinetSearchQuery(query: String) {
        _cabinetSearchQuery.value = query
    }

    fun setCabinetFilter(filter: CabinetFilter) {
        _cabinetFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun resetScanState() {
        val currentState = _scanUiState.value
        if (currentState is ScanUiState.Success) {
            currentState.imagePath?.let { path ->
                if (path.contains("temp_package_images")) {
                    try {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        _scanUiState.value = ScanUiState.Idle
    }

    fun selectMedicine(medicine: Medicine?) {
        _selectedMedicine.value = medicine
        // Reset instruction state when a new medicine is selected or closed
        _instructionUiState.value = InstructionUiState.Idle
    }

    /**
     * Loads the detailed instruction and usage guidelines for a medicine.
     */
    fun loadDrugInstruction(medicineName: String) {
        viewModelScope.launch {
            _instructionUiState.value = InstructionUiState.Loading
            try {
                val instruction = GeminiService.fetchMedicineInstruction(medicineName)
                _instructionUiState.value = InstructionUiState.Success(instruction)
            } catch (e: Exception) {
                _instructionUiState.value = InstructionUiState.Error("Не удалось загрузить медицинскую инструкцию: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Analyzes photo of a medicine package using Gemini.
     */
    fun parsePackagePhoto(base64Image: String, simulatedName: String? = null, imagePath: String? = null) {
        viewModelScope.launch {
            _scanUiState.value = ScanUiState.Processing
            try {
                val parsedResult = GeminiService.parsePackagePhoto(base64Image, simulatedName)
                if (parsedResult.serial.isNotEmpty()) {
                    val duplicate = medicines.value.find { it.serial.trim() == parsedResult.serial.trim() }
                    if (duplicate != null) {
                        _scanUiState.value = ScanUiState.Error("УПАКОВКА_ДУБЛИКАТ: Упаковка лекарства \"${parsedResult.name}\" с серийным номером ${parsedResult.serial} уже есть в вашей аптечке!")
                        return@launch
                    }
                }
                _scanUiState.value = ScanUiState.Success(parsedResult, imagePath)
            } catch (e: Exception) {
                _scanUiState.value = ScanUiState.Error("К сожалению, не удалось распознать лекарство по фото: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Searches for medicine details and returns recommended therapeutic analogs.
     */
    fun searchMedicineAnalogs(queryName: String) {
        if (queryName.isBlank()) return
        _searchQuery.value = queryName
        viewModelScope.launch {
            _analogsUiState.value = AnalogsUiState.Loading
            try {
                val result = GeminiService.searchAnalogs(queryName)
                _analogsUiState.value = AnalogsUiState.Success(result)
            } catch (e: Exception) {
                _analogsUiState.value = AnalogsUiState.Error("Не удалось получить информацию об аналогах: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Adds the identified medicine to the local Room database cabinet.
     */
    fun saveToCabinet(
        parsedResult: ParsedZnakResult,
        customNotes: String = "",
        totalPackageCount: Int = 30,
        intakeDosage: Double = 0.0,
        intakeFrequency: String = "as_needed",
        packageImagePath: String? = null
    ) {
        viewModelScope.launch {
            if (parsedResult.serial.isNotEmpty()) {
                val duplicate = medicines.value.find { it.serial.trim() == parsedResult.serial.trim() }
                if (duplicate != null) {
                    showInAppAlert("Упаковка лекарства \"${parsedResult.name}\" с серийным номером ${parsedResult.serial} уже есть в аптечке!")
                    _scanUiState.value = ScanUiState.Idle
                    return@launch
                }
            }
            
            var processedImagePath: String? = null
            if (packageImagePath != null) {
                processedImagePath = try {
                    val rawFile = java.io.File(packageImagePath)
                    if (rawFile.exists()) {
                        val destDir = java.io.File(app.filesDir, "package_images")
                        if (!destDir.exists()) destDir.mkdirs()
                        val destFile = java.io.File(destDir, "img_${System.currentTimeMillis()}.jpg")
                        val compressedBytes = com.example.api.scaleAndCompressImageFile(rawFile, maxDimension = 400, quality = 75)
                        if (compressedBytes != null) {
                            destFile.writeBytes(compressedBytes)
                        } else {
                            rawFile.copyTo(destFile, overwrite = true)
                        }
                        // Delete the temporary raw file
                        try { rawFile.delete() } catch(e: Exception) {}
                        destFile.absolutePath
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    packageImagePath
                }
            }

            val timestamp = parseDateToTimestamp(parsedResult.expiration_date)
            val medicine = Medicine(
                name = parsedResult.name,
                gtin = parsedResult.gtin,
                serial = parsedResult.serial,
                expirationDate = parsedResult.expiration_date,
                expirationTimestamp = timestamp,
                batch = parsedResult.batch,
                notes = customNotes,
                totalPackageCount = totalPackageCount,
                remainingCount = totalPackageCount.toDouble(),
                intakeDosage = intakeDosage,
                intakeFrequency = intakeFrequency,
                lastIntakeDecayTimestamp = System.currentTimeMillis(),
                packageImagePath = processedImagePath,
                tags = parsedResult.tags?.joinToString(", ") ?: ""
            )
            repository.insert(medicine)
            _scanUiState.value = ScanUiState.Idle // Close the success prompt / scan dialog
            
            // Auto background sync upload
            triggerBackgroundAutoSync()
        }
    }

    /**
     * Manually inserts a custom medicine into the local database cabinet.
     */
    fun addMedicineManually(
        name: String,
        expirationDate: String,
        notes: String = "",
        batch: String = "",
        gtin: String = "",
        totalPackageCount: Int = 30,
        intakeDosage: Double = 0.0,
        intakeFrequency: String = "as_needed",
        tags: String = ""
    ) {
        viewModelScope.launch {
            val timestamp = parseDateToTimestamp(expirationDate)
            val medicine = Medicine(
                name = name,
                gtin = gtin,
                serial = "MANUAL-" + (1000000..9999999).random().toString(),
                expirationDate = expirationDate,
                expirationTimestamp = timestamp,
                batch = batch,
                notes = notes,
                totalPackageCount = totalPackageCount,
                remainingCount = totalPackageCount.toDouble(),
                intakeDosage = intakeDosage,
                intakeFrequency = intakeFrequency,
                lastIntakeDecayTimestamp = System.currentTimeMillis(),
                tags = tags
            )
            repository.insert(medicine)
            
            // Auto background sync upload
            triggerBackgroundAutoSync()
        }
    }

    fun deleteFromCabinet(medicine: Medicine) {
        viewModelScope.launch {
            // Save deletion timestamp locally to sync with other devices
            val delPrefs = app.getSharedPreferences("aptechka_deleted_meds", android.content.Context.MODE_PRIVATE)
            delPrefs.edit().putLong(medicine.serial, System.currentTimeMillis()).apply()

            repository.delete(medicine)
            if (_selectedMedicine.value?.id == medicine.id) {
                _selectedMedicine.value = null
            }
            
            // Auto background sync upload
            triggerBackgroundAutoSync()
        }
    }

    fun updateMedicineNotes(medicine: Medicine, updatedNotes: String) {
        viewModelScope.launch {
            val updated = medicine.copy(
                notes = updatedNotes,
                lastIntakeDecayTimestamp = System.currentTimeMillis()
            )
            repository.update(updated)
            _selectedMedicine.value = _selectedMedicine.value?.let {
                if (it.id == medicine.id) updated else it
            }
            
            // Auto background sync upload
            triggerBackgroundAutoSync()
        }
    }

    fun updateMedicine(medicine: Medicine) {
        viewModelScope.launch {
            val updated = medicine.copy(lastIntakeDecayTimestamp = System.currentTimeMillis())
            repository.update(updated)
            _selectedMedicine.value = _selectedMedicine.value?.let {
                if (it.id == medicine.id) updated else it
            }
            
            // Auto background sync upload
            triggerBackgroundAutoSync()
        }
    }

    fun simulateManualIntakeDeduction(medicine: Medicine) {
        viewModelScope.launch {
            val frequencyMultiplier = when (medicine.intakeFrequency) {
                "1_daily" -> 1.0
                "2_daily" -> 2.0
                "3_daily" -> 3.0
                "every_other_day" -> 0.5
                else -> 0.0
            }
            if (frequencyMultiplier > 0.0 && medicine.intakeDosage > 0.0) {
                val deduction = medicine.intakeDosage * frequencyMultiplier
                val updatedRemaining = maxOf(0.0, medicine.remainingCount - deduction)
                val updatedMed = medicine.copy(
                    remainingCount = updatedRemaining,
                    lastIntakeDecayTimestamp = System.currentTimeMillis()
                )
                repository.update(updatedMed)
                repository.insertIntakeLog(
                    com.example.data.IntakeLog(
                        medicineName = medicine.name,
                        dosage = deduction,
                        takenTimestamp = System.currentTimeMillis(),
                        scheduledTime = "Вручную"
                    )
                )
                _selectedMedicine.value = _selectedMedicine.value?.let {
                    if (it.id == medicine.id) updatedMed else it
                }
                
                // Trigger push notifications immediately
                if (updatedRemaining <= _quantityThresholdCount.value && updatedRemaining > 0) {
                    showLocalNotification(
                        "Заканчивается препарат!",
                        "Внимание: препарат ${medicine.name} подходит к концу (осталось ${formatDouble(updatedRemaining)} шт. из ${medicine.totalPackageCount}). Пора пополнить запасы!"
                    )
                } else if (updatedRemaining == 0.0) {
                    showLocalNotification(
                        "Препарат закончился!",
                        "Внимание: препарат ${medicine.name} полностью закончился! Купите новую упаковку."
                    )
                    checkForScheduleSuggestion(medicine)
                } else {
                    showLocalNotification(
                        "Имитация приема зафиксирована",
                        "Списано ${formatDouble(deduction)} шт. Осталось: ${formatDouble(updatedRemaining)} шт."
                    )
                }
            } else {
                showInAppAlert("Для этого лекарства не настроена схема приема в настройках препарата.")
            }
        }
    }

    // Auto background upload when local state is mutated
    private fun triggerBackgroundAutoSync() {
        if (_syncAutoEnabled.value && _syncAccountId.value.isNotEmpty()) {
            executeSync("merge")
        }
    }

    fun createSharedAccount() {
        val randomId = "MED-" + (100000..999999).random().toString()
        val randomPass = java.util.UUID.randomUUID().toString().take(6).uppercase()
        
        prefs.edit()
            .putString("sync_account_id", randomId)
            .putString("sync_passphrase", randomPass)
            .putLong("sync_last_time", 0L)
            .putBoolean("sync_auto_enabled", true)
            .apply()
            
        _syncAccountId.value = randomId
        _syncPassphrase.value = randomPass
        _syncLastTime.value = 0L
        _syncAutoEnabled.value = true
        _syncUiState.value = SyncUiState.Idle
        
        // Push initial local state to new remote repository
        executeSync("upload")
    }

    fun connectSharedAccount(accountId: String, passphrase: String) {
        val cleanId = accountId.trim().uppercase()
        val cleanPass = passphrase.trim()
        
        prefs.edit()
            .putString("sync_account_id", cleanId)
            .putString("sync_passphrase", cleanPass)
            .putLong("sync_last_time", 0L)
            .putBoolean("sync_auto_enabled", true)
            .apply()
            
        _syncAccountId.value = cleanId
        _syncPassphrase.value = cleanPass
        _syncLastTime.value = 0L
        _syncAutoEnabled.value = true
        _syncUiState.value = SyncUiState.Idle
        
        // Run full merge between existing local state and retrieved remote state
        executeSync("merge")
    }

    fun disconnectAccount() {
        prefs.edit()
            .putString("sync_account_id", "")
            .putString("sync_passphrase", "")
            .putLong("sync_last_time", 0L)
            .putBoolean("sync_auto_enabled", false)
            .apply()
            
        _syncAccountId.value = ""
        _syncPassphrase.value = ""
        _syncLastTime.value = 0L
        _syncAutoEnabled.value = false
        _syncUiState.value = SyncUiState.Idle
    }

    private fun getLocalDeletedMedsMap(): Map<String, Long> {
        val delPrefs = app.getSharedPreferences("aptechka_deleted_meds", android.content.Context.MODE_PRIVATE)
        val all = delPrefs.all
        val result = mutableMapOf<String, Long>()
        for (entry in all.entries) {
            val key = entry.key
            val value = entry.value
            if (value is Long) {
                result[key] = value
            } else if (value is Number) {
                result[key] = value.toLong()
            } else if (value is String) {
                value.toLongOrNull()?.let { result[key] = it }
            }
        }
        return result
    }

    private fun saveLocalDeletedMedsMap(mergedDeletedMap: Map<String, Long>) {
        val delPrefs = app.getSharedPreferences("aptechka_deleted_meds", android.content.Context.MODE_PRIVATE)
        val editor = delPrefs.edit()
        editor.clear()
        for (entry in mergedDeletedMap.entries) {
            editor.putLong(entry.key, entry.value)
        }
        editor.apply()
    }

    fun executeSync(mode: String = "merge") {
        if (_syncAccountId.value.isBlank()) return
        
        viewModelScope.launch {
            if (syncMutex.isLocked) {
                Log.d("MainViewModel", "Sync already in progress, ignoring this concurrent call.")
                return@launch
            }
            syncMutex.withLock {
                _syncUiState.value = SyncUiState.Syncing
                val localList = repository.getAllMedicinesDirect()
                
                try {
                    val localDeletedMap = getLocalDeletedMedsMap()
                    val result = com.example.api.SyncService.performSync(
                        context = app,
                        accountId = _syncAccountId.value,
                        passphrase = _syncPassphrase.value,
                        localMedicines = localList,
                        localDeletedMap = localDeletedMap,
                        mode = mode
                    )
                    
                    when (result) {
                        is com.example.api.SyncResult.Success -> {
                            val merged = result.mergedList
                            
                            // Save the updated merged deletions locally
                            saveLocalDeletedMedsMap(result.mergedDeletedMap)
                            
                            if (mode == "merge" || mode == "download") {
                                // 1. Delete items not in synced/downloaded database
                                val mergedSerials = merged.map { it.serial }.toSet()
                                localList.forEach { local ->
                                    if (!mergedSerials.contains(local.serial)) {
                                        repository.delete(local)
                                    }
                                }
                                
                                // 2. Insert/replace keeping original ID
                                merged.forEach { syncMed ->
                                    val existingMap = localList.find { it.serial == syncMed.serial }
                                    val withId = if (existingMap != null) {
                                        syncMed.copy(id = existingMap.id)
                                    } else {
                                        syncMed.copy(id = 0)
                                    }
                                    repository.insert(withId)
                                }
                            }
                            
                            // Save last sync time
                            val now = System.currentTimeMillis()
                            prefs.edit().putLong("sync_last_time", now).apply()
                            _syncLastTime.value = now
                            
                            _syncUiState.value = SyncUiState.Success(result.actionType)
                        }
                        is com.example.api.SyncResult.Error -> {
                            _syncUiState.value = SyncUiState.Error(result.message)
                        }
                    }
                } catch (e: Exception) {
                    _syncUiState.value = SyncUiState.Error("Исключение при обмене: ${e.localizedMessage}")
                }
            }
        }
    }

    // --- USER PROFILE STATE ---
    private val _userFirstName = MutableStateFlow(prefs.getString("user_first_name", "") ?: "")
    val userFirstName: StateFlow<String> = _userFirstName.asStateFlow()

    private val _userLastName = MutableStateFlow(prefs.getString("user_last_name", "") ?: "")
    val userLastName: StateFlow<String> = _userLastName.asStateFlow()

    fun saveUserProfile(firstName: String, lastName: String) {
        prefs.edit().apply {
            putString("user_first_name", firstName)
            putString("user_last_name", lastName)
        }.apply()
        _userFirstName.value = firstName
        _userLastName.value = lastName
    }

    // --- PILLBOX STATE & METHODS ---
    val pillboxes: StateFlow<List<com.example.data.Pillbox>> = repository.allPillboxes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pillboxEntries: StateFlow<List<com.example.data.PillboxEntry>> = repository.allPillboxEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun createPillbox(name: String) {
        viewModelScope.launch {
            repository.insertPillbox(com.example.data.Pillbox(name = name))
        }
    }

    fun renamePillbox(pillbox: com.example.data.Pillbox, newName: String) {
        viewModelScope.launch {
            repository.updatePillbox(pillbox.copy(name = newName))
        }
    }

    fun deletePillbox(pillbox: com.example.data.Pillbox) {
        viewModelScope.launch {
            repository.deletePillbox(pillbox)
            // Clean up entries
            val entries = repository.getAllPillboxEntriesDirect().filter { it.pillboxId == pillbox.id }
            val context = getApplication<Application>()
            entries.forEach { entry ->
                repository.deletePillboxEntry(entry)
                com.example.receiver.ReminderReceiver.cancelPillReminder(context, entry.id)
            }
        }
    }

    fun addPillboxEntry(pillboxId: Int, medicineName: String, dosage: Double, preferredTime: String, periodicityDays: Int) {
        viewModelScope.launch {
            val entry = com.example.data.PillboxEntry(
                pillboxId = pillboxId,
                medicineName = medicineName,
                dosage = dosage,
                preferredTime = preferredTime,
                periodicityDays = periodicityDays
            )
            val id = repository.insertPillboxEntry(entry)
            val context = getApplication<Application>()
            com.example.receiver.ReminderReceiver.schedulePillReminder(
                context = context,
                entryId = id.toInt(),
                medicineName = medicineName,
                dosage = dosage,
                timeStr = preferredTime,
                periodicityDays = periodicityDays
            )
        }
    }

    fun deletePillboxEntry(entry: com.example.data.PillboxEntry) {
        viewModelScope.launch {
            repository.deletePillboxEntry(entry)
            val context = getApplication<Application>()
            com.example.receiver.ReminderReceiver.cancelPillReminder(context, entry.id)
        }
    }

    fun updatePillboxEntryTime(entry: com.example.data.PillboxEntry, newTime: String) {
        viewModelScope.launch {
            val updatedEntry = entry.copy(preferredTime = newTime)
            repository.updatePillboxEntry(updatedEntry)
            val context = getApplication<Application>()
            com.example.receiver.ReminderReceiver.cancelPillReminder(context, entry.id)
            com.example.receiver.ReminderReceiver.schedulePillReminder(
                context = context,
                entryId = entry.id,
                medicineName = entry.medicineName,
                dosage = entry.dosage,
                timeStr = newTime,
                periodicityDays = entry.periodicityDays
            )
        }
    }

    fun updatePillboxGroupTime(entries: List<com.example.data.PillboxEntry>, newTime: String) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            for (entry in entries) {
                val updatedEntry = entry.copy(preferredTime = newTime)
                repository.updatePillboxEntry(updatedEntry)
                com.example.receiver.ReminderReceiver.cancelPillReminder(context, entry.id)
                com.example.receiver.ReminderReceiver.schedulePillReminder(
                    context = context,
                    entryId = entry.id,
                    medicineName = entry.medicineName,
                    dosage = entry.dosage,
                    timeStr = newTime,
                    periodicityDays = entry.periodicityDays
                )
            }
        }
    }

    fun confirmGroupedPillboxIntake(entries: List<com.example.data.PillboxEntry>) {
        viewModelScope.launch {
            if (entries.isEmpty()) return@launch
            
            var successCount = 0
            val skippedMedicines = mutableListOf<String>()

            for (entry in entries) {
                val targetName = entry.medicineName.trim()
                val cand = medicines.value
                    .filter { it.name.trim().equals(targetName, ignoreCase = true) && it.remainingCount > 0.0 }
                    .sortedBy { it.expirationTimestamp }
                
                if (cand.isEmpty()) {
                    skippedMedicines.add(targetName)
                    continue
                }
                
                var dosageToDeduct = entry.dosage
                
                for (med in cand) {
                    if (dosageToDeduct <= 0.0) break
                    val available = med.remainingCount
                    val deduct = minOf(available, dosageToDeduct)
                    val updatedMed = med.copy(
                        remainingCount = available - deduct,
                        lastIntakeDecayTimestamp = System.currentTimeMillis()
                    )
                    repository.update(updatedMed)
                    
                    dosageToDeduct -= deduct
                    
                    // Warn low stock or finished
                    if (updatedMed.remainingCount == 0.0) {
                        showLocalNotification(
                            "Упаковка закончилась",
                            "Препарат ${med.name} (серия: ${med.batch}) полностью закончился в аптечке."
                        )
                        checkForScheduleSuggestion(updatedMed)
                    } else if (updatedMed.remainingCount <= _quantityThresholdCount.value) {
                        showLocalNotification(
                            "Заканчивается препарат",
                            "Препарат ${med.name} (серия: ${med.batch}) заканчивается! Остаток: ${formatDouble(updatedMed.remainingCount)} шт."
                        )
                    }
                }
                
                // Log taken
                val updatedEntry = entry.copy(lastTakenTimestamp = System.currentTimeMillis())
                repository.updatePillboxEntry(updatedEntry)
                repository.insertIntakeLog(
                    com.example.data.IntakeLog(
                        medicineName = entry.medicineName,
                        dosage = entry.dosage,
                        takenTimestamp = System.currentTimeMillis(),
                        scheduledTime = entry.preferredTime
                    )
                )
                successCount++
            }
            
            if (skippedMedicines.isNotEmpty()) {
                val skippedStr = skippedMedicines.joinToString(", ")
                showInAppAlert("Часть лекарств в группе отсутствует в аптечке: $skippedStr")
            }
            
            if (successCount > 0) {
                showLocalNotification(
                    "Групповой прием зафиксирован",
                    "Успешно отмечен прием для нескольких препаратов на сумму $successCount позиций."
                )
            }
            triggerBackgroundAutoSync()
        }
    }

    fun confirmPillboxIntake(entry: com.example.data.PillboxEntry) {
        viewModelScope.launch {
            val targetName = entry.medicineName.trim()
            val cand = medicines.value
                .filter { it.name.trim().equals(targetName, ignoreCase = true) && it.remainingCount > 0.0 }
                .sortedBy { it.expirationTimestamp }
            
            if (cand.isEmpty()) {
                showInAppAlert("Препарат \"$targetName\" отсутствует в вашей аптечке или его количество равно 0!")
                return@launch
            }
            
            var dosageToDeduct = entry.dosage
            
            for (med in cand) {
                if (dosageToDeduct <= 0.0) break
                val available = med.remainingCount
                val deduct = minOf(available, dosageToDeduct)
                val updatedMed = med.copy(
                    remainingCount = available - deduct,
                    lastIntakeDecayTimestamp = System.currentTimeMillis()
                )
                repository.update(updatedMed)
                
                dosageToDeduct -= deduct
                
                // Warn low stock or finished
                if (updatedMed.remainingCount == 0.0) {
                    showLocalNotification(
                        "Упаковка закончилась",
                        "Препарат ${med.name} (серия: ${med.batch}) полностью закончился в аптечке."
                    )
                    checkForScheduleSuggestion(updatedMed)
                } else if (updatedMed.remainingCount <= _quantityThresholdCount.value) {
                    showLocalNotification(
                        "Заканчивается препарат",
                        "Препарат ${med.name} (серия: ${med.batch}) заканчивается! Остаток: ${formatDouble(updatedMed.remainingCount)} шт."
                    )
                }
            }
            
            // Log taken
            val updatedEntry = entry.copy(lastTakenTimestamp = System.currentTimeMillis())
            repository.updatePillboxEntry(updatedEntry)
            repository.insertIntakeLog(
                com.example.data.IntakeLog(
                    medicineName = entry.medicineName,
                    dosage = entry.dosage,
                    takenTimestamp = System.currentTimeMillis(),
                    scheduledTime = entry.preferredTime
                )
            )
            
            showLocalNotification(
                "Прием зафиксирован",
                "Вы приняли $targetName в дозе ${formatDouble(entry.dosage)}. Списано из остатков аптечки в порядке срока годности."
            )
            triggerBackgroundAutoSync()
        }
    }

    fun deductMedicineByCount(medicine: com.example.data.Medicine, amount: Double) {
        viewModelScope.launch {
            val updatedRemaining = maxOf(0.0, medicine.remainingCount - amount)
            val updatedMed = medicine.copy(
                remainingCount = updatedRemaining,
                lastIntakeDecayTimestamp = System.currentTimeMillis()
            )
            repository.update(updatedMed)
            repository.insertIntakeLog(
                com.example.data.IntakeLog(
                    medicineName = medicine.name,
                    dosage = amount,
                    takenTimestamp = System.currentTimeMillis(),
                    scheduledTime = "Вручную"
                )
            )
            _selectedMedicine.value = _selectedMedicine.value?.let {
                if (it.id == medicine.id) updatedMed else it
            }
            
            if (updatedRemaining == 0.0) {
                showLocalNotification(
                    "Упаковка закончилась",
                    "Препарат ${medicine.name} закончился!"
                )
                checkForScheduleSuggestion(medicine)
            } else if (updatedRemaining <= _quantityThresholdCount.value) {
                showLocalNotification(
                    "Мало остатка",
                    "Препарат ${medicine.name} подходит к концу (осталось всего ${formatDouble(updatedRemaining)} шт.)."
                )
            } else {
                showLocalNotification(
                    "Прием зафиксирован",
                    "Зафиксировано списание ${formatDouble(amount)} шт. лекарства ${medicine.name}. Остаток: ${formatDouble(updatedRemaining)} шт."
                )
            }
            triggerBackgroundAutoSync()
        }
    }

    // Helper: parses date string "YYYY-MM-DD" to millisecond timestamp
    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdf.parse(dateStr)
            date?.time ?: Long.MAX_VALUE
        } catch (e: Exception) {
            // format fallback
            try {
                val sdfShort = SimpleDateFormat("yy-MM-dd", Locale.US)
                val date = sdfShort.parse(dateStr)
                date?.time ?: Long.MAX_VALUE
            } catch (ex: Exception) {
                Long.MAX_VALUE
            }
        }
    }

    /**
     * Get visual status and badge color based on medicine expiration date.
     * Checks relative to currently set mock calendar date of May 28, 2026.
     */
    fun getExpirationStatus(medicine: Medicine): ExpirationStatus {
        val currentMills = 1779981846000L // Mock current timestamp for May 28, 2026.
        val expirationMills = medicine.expirationTimestamp

        val calendarCurrent = Calendar.getInstance().apply { timeInMillis = currentMills }
        val calendarExp = Calendar.getInstance().apply { timeInMillis = expirationMills }

        if (expirationMills <= currentMills) {
            return ExpirationStatus.EXPIRED // Истёк
        }

        // Check if expiring within user threshold days (default 90)
        val thresholdDays = _expirationThresholdDays.value
        val thresholdMills = thresholdDays * 24 * 60 * 60 * 1000L
        if (expirationMills - currentMills < thresholdMills) {
            return ExpirationStatus.EXPIRING_SOON // Истекает скоро
        }

        return ExpirationStatus.VALID // Годен
    }
}

enum class ExpirationStatus(val label: String) {
    EXPIRED("Истёк срок"),
    EXPIRING_SOON("Истекает скоро"),
    VALID("Годен")
}
