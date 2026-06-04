package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowCircleRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.api.GeminiService
import com.example.api.MedicineAnalog
import com.example.api.ParsedZnakResult
import com.example.data.Medicine
import com.example.ui.ActiveTab
import com.example.ui.AnalogsUiState
import com.example.ui.CabinetFilter
import com.example.ui.ExpirationStatus
import com.example.ui.InstructionUiState
import com.example.ui.MainViewModel
import com.example.ui.ScanUiState
import com.example.ui.ScheduleSuggestion
import com.example.ui.theme.ColorExpired
import com.example.ui.theme.ColorExpiring
import com.example.ui.theme.ColorValid
import com.example.ui.theme.ColorNeutral
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.LightBackground
import com.example.ui.theme.LightSurface
import com.example.ui.theme.LavenderActive
import com.example.ui.theme.LavenderText
import com.example.ui.theme.MintPrimary
import com.example.ui.theme.MintTeal
import com.example.ui.theme.MintDarkText
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private var notificationIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationIntentState = intent
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            val darkTheme = when (themeMode) {
                0 -> isSystemInDarkTheme()
                1 -> false
                else -> true
            }

            // Route to target tab when launched/resumed via notification with extra
            androidx.compose.runtime.LaunchedEffect(notificationIntentState) {
                val currentIntent = notificationIntentState
                if (currentIntent != null && currentIntent.hasExtra("navigate_to_tab")) {
                    val tabName = currentIntent.getStringExtra("navigate_to_tab")
                    if (tabName == "PILLBOX") {
                        viewModel.switchTab(ActiveTab.PILLBOX)
                    }
                    currentIntent.removeExtra("navigate_to_tab")
                    notificationIntentState = null
                }
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                MainAppScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationIntentState = intent
    }
}

// Helper expansion function to convert File directly to base64
fun File.toBase64(): String {
    val bytes = this.readBytes()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainAppScreen(viewModel: MainViewModel = viewModel()) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val scanUiState by viewModel.scanUiState.collectAsStateWithLifecycle()
    val selectedMedicine by viewModel.selectedMedicine.collectAsStateWithLifecycle()
    val instructionUiState by viewModel.instructionUiState.collectAsStateWithLifecycle()
    val inAppAlert by viewModel.inAppAlert.collectAsStateWithLifecycle()
    val pillboxes by viewModel.pillboxes.collectAsStateWithLifecycle()

    // Dynamic request for POST_NOTIFICATIONS on Android 13+ (API 33+)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(android.Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.executeSync("merge")
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showManualAddDialog by remember { mutableStateOf(false) }
    var showAddChoiceDialog by remember { mutableStateOf(false) }

    if (inAppAlert != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearInAppAlert() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Внимание МедСкан", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = { Text(inAppAlert ?: "") },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearInAppAlert() },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                ) {
                    Text("ОК")
                }
            }
        )
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == ActiveTab.CABINET,
                    onClick = { viewModel.switchTab(ActiveTab.CABINET) },
                    icon = { Icon(Icons.Default.MedicalServices, contentDescription = "Аптечка") },
                    label = { Text("Аптечка", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = LavenderText,
                        indicatorColor = LavenderActive,
                        unselectedIconColor = Color(0xFF556664),
                        unselectedTextColor = Color(0xFF556664)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ActiveTab.SCANNER,
                    onClick = { viewModel.switchTab(ActiveTab.SCANNER) },
                    icon = { Icon(Icons.Default.PhotoCamera, contentDescription = "По фото") },
                    label = { Text("По фото", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = LavenderText,
                        indicatorColor = LavenderActive,
                        unselectedIconColor = Color(0xFF556664),
                        unselectedTextColor = Color(0xFF556664)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ActiveTab.SEARCH,
                    onClick = { viewModel.switchTab(ActiveTab.SEARCH) },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Аналоги") },
                    label = { Text("Аналоги", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = LavenderText,
                        indicatorColor = LavenderActive,
                        unselectedIconColor = Color(0xFF556664),
                        unselectedTextColor = Color(0xFF556664)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ActiveTab.PILLBOX,
                    onClick = { viewModel.switchTab(ActiveTab.PILLBOX) },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Таблетницы") },
                    label = { Text("Таблетки", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = LavenderText,
                        indicatorColor = LavenderActive,
                        unselectedIconColor = Color(0xFF556664),
                        unselectedTextColor = Color(0xFF556664)
                    )
                )
                NavigationBarItem(
                    selected = currentTab == ActiveTab.PROFILE,
                    onClick = { viewModel.switchTab(ActiveTab.PROFILE) },
                    icon = { Icon(Icons.Default.HealthAndSafety, contentDescription = "Кабинет") },
                    label = { Text("Кабинет", fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, softWrap = false) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MintPrimary,
                        selectedTextColor = LavenderText,
                        indicatorColor = LavenderActive,
                        unselectedIconColor = Color(0xFF556664),
                        unselectedTextColor = Color(0xFF556664)
                    )
                )
            }
        },
        floatingActionButton = {
            if (currentTab == ActiveTab.CABINET) {
                FloatingActionButton(
                    onClick = { showAddChoiceDialog = true },
                    containerColor = MintPrimary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_manual")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить лекарство")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                ActiveTab.CABINET -> CabinetScreen(viewModel = viewModel)
                ActiveTab.SCANNER -> PhotoRecognitionScreen(viewModel = viewModel)
                ActiveTab.SEARCH -> SearchAnalogsScreen(viewModel = viewModel)
                ActiveTab.PILLBOX -> PillboxScreen(viewModel = viewModel)
                ActiveTab.PROFILE -> ProfileCabinetScreen(viewModel = viewModel)
                ActiveTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
            }

            if (selectedMedicine != null) {
                val medicine = selectedMedicine!!
                val status = viewModel.getExpirationStatus(medicine)
                MedicineDetailsDialog(
                    medicine = medicine,
                    status = status,
                    instructionUiState = instructionUiState,
                    onSimulateIntake = { viewModel.simulateManualIntakeDeduction(medicine) },
                    onLoadInstruction = { viewModel.loadDrugInstruction(medicine.name) },
                    onDismiss = { viewModel.selectMedicine(null) },
                    onDelete = { viewModel.deleteFromCabinet(medicine) },
                    onSaveNotes = { notes -> viewModel.updateMedicineNotes(medicine, notes) },
                    onUpdateMedicine = { updated -> viewModel.updateMedicine(updated) },
                    onManualTake = { amount -> viewModel.deductMedicineByCount(medicine, amount) },
                    pillboxes = pillboxes,
                    onAddToPillbox = { pillboxId, dosage, preferredTime, periodicityDays ->
                        viewModel.addPillboxEntry(pillboxId, medicine.name, dosage, preferredTime, periodicityDays)
                    }
                )
            }

            val scheduleSuggestion by viewModel.scheduleSuggestion.collectAsStateWithLifecycle()
            if (scheduleSuggestion != null) {
                val sug = scheduleSuggestion!!
                val status = viewModel.getExpirationStatus(sug.targetMedicine)
                val statusColor = when (status) {
                    ExpirationStatus.EXPIRED -> ColorExpired
                    ExpirationStatus.EXPIRING_SOON -> ColorExpiring
                    ExpirationStatus.VALID -> ColorValid
                }
                AlertDialog(
                    onDismissRequest = { viewModel.dismissScheduleSuggestion() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = ColorExpiring,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Перенос схемы приема", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Препарат \"${sug.finishedMedicineName}\" полностью закончился.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Но в вашей аптечке обнаружена аналогичная упаковка этого лекарства:",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(sug.targetMedicine.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodyLarge, color = MintDarkText)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (sug.targetMedicine.batch.isNotEmpty()) {
                                        Text("Серия: ${sug.targetMedicine.batch}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Text("Годен до: ${formatRussianDate(sug.targetMedicine.expirationDate)} (${status.label})", color = statusColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("Остаток: ${sug.targetMedicine.remainingCount} шт.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                text = if (sug.sourceDosage > 0.0) {
                                    "Хотите заменить закончившееся лекарство на аналогичное с самым коротким сроком годности и перенести схему приёма?"
                                } else {
                                    "Хотите заменить закончившееся лекарство на аналогичное с самым коротким сроком годности в таблетнице и профиле?"
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.applyScheduleSuggestion() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                        ) {
                            Text(if (sug.sourceDosage > 0.0) "Перенести схему" else "Заменить лекарство")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.dismissScheduleSuggestion() }) {
                            Text("Отмена", color = ColorExpired)
                        }
                    }
                )
            }

            // Global scan success/saving dialog
            if (scanUiState is ScanUiState.Success) {
                val successState = scanUiState as ScanUiState.Success
                val data = successState.result
                val imgPath = successState.imagePath
                ScanResultDialog(
                    result = data,
                    onDismiss = { viewModel.resetScanState() },
                    onSave = { updatedData, notes, totalQty, dosage, freq ->
                        viewModel.saveToCabinet(updatedData, notes, totalQty, dosage, freq, imgPath)
                    }
                )
            } else if (scanUiState is ScanUiState.Processing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MintPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "ИИ анализирует упаковку по фото...",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Распознаем наименование, срок годности и серию на коробке...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (scanUiState is ScanUiState.Error) {
                val errMsg = (scanUiState as ScanUiState.Error).message
                val isDuplicate = errMsg.contains("УПАКО") || errMsg.contains("уже есть в")
                AlertDialog(
                    onDismissRequest = { viewModel.resetScanState() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDuplicate) Icons.Default.Info else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isDuplicate) ColorExpiring else ColorExpired,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isDuplicate) "Дубликат упаковки" else "Ошибка распознавания",
                                fontWeight = FontWeight.Bold,
                                color = if (isDuplicate) ColorExpiring else ColorExpired
                            )
                        }
                    },
                    text = { Text(errMsg) },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.resetScanState() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                        ) {
                            Text("ОК")
                        }
                    },
                    dismissButton = {
                        if (!isDuplicate) {
                            TextButton(
                                onClick = {
                                    viewModel.resetScanState()
                                    showManualAddDialog = true
                                }
                            ) {
                                Text("Ввести вручную", color = MintPrimary)
                            }
                        }
                    }
                )
            }

            // Floating manual addition dialog
            if (showManualAddDialog) {
                AddMedicineManualDialog(
                    onDismiss = { showManualAddDialog = false },
                    onSave = { name, expDate, notes, batch, gtin, totalQty, dosage, freq, tags ->
                        viewModel.addMedicineManually(name, expDate, notes, batch, gtin, totalQty, dosage, freq, tags)
                        showManualAddDialog = false
                    }
                )
            }

            // Choice dialog: manual or AI scanner
            if (showAddChoiceDialog) {
                MedicineAddChoiceDialog(
                    onDismiss = { showAddChoiceDialog = false },
                    onAddManual = {
                        showAddChoiceDialog = false
                        showManualAddDialog = true
                    },
                    onAddScanner = {
                        showAddChoiceDialog = false
                        viewModel.switchTab(ActiveTab.SCANNER)
                    }
                )
            }
        }
    }
}

// ================= MEDICINE CABINET TAB SCREEN =================

@Composable
fun CabinetScreen(viewModel: MainViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val medicines by viewModel.filteredMedicines.collectAsStateWithLifecycle()
    val allMedicinesList by viewModel.medicines.collectAsStateWithLifecycle()
    val searchQuery by viewModel.cabinetSearchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.cabinetFilter.collectAsStateWithLifecycle()
    val quantityThresholdCount by viewModel.quantityThresholdCount.collectAsStateWithLifecycle()

    val pagerState = rememberPagerState(
        initialPage = selectedFilter.ordinal,
        pageCount = { CabinetFilter.values().size }
    )

    // Synchronize pager state -> viewModel when user swipes
    LaunchedEffect(pagerState.currentPage) {
        val targetFilter = CabinetFilter.values()[pagerState.currentPage]
        if (selectedFilter != targetFilter) {
            viewModel.setCabinetFilter(targetFilter)
        }
    }

    // Synchronize viewModel -> pager state when viewModel state changes
    LaunchedEffect(selectedFilter) {
        if (pagerState.currentPage != selectedFilter.ordinal) {
            pagerState.animateScrollToPage(selectedFilter.ordinal)
        }
    }

    var selectedSerials by remember { mutableStateOf(emptySet<String>()) }
    val isMultiSelectMode = selectedSerials.isNotEmpty()

    // Pass up-to-date items only
    val activeSerials = medicines.map { it.serial }.toSet()
    if (selectedSerials.any { !activeSerials.contains(it) }) {
        selectedSerials = selectedSerials.filter { activeSerials.contains(it) }.toSet()
    }

    // Calculate dynamic stats relative to mock day May 28, 2026
    val totalCount = allMedicinesList.size
    val expiredCount = allMedicinesList.count { viewModel.getExpirationStatus(it) == ExpirationStatus.EXPIRED }
    val expiringCount = allMedicinesList.count { viewModel.getExpirationStatus(it) == ExpirationStatus.EXPIRING_SOON }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // App header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val firstName by viewModel.userFirstName.collectAsStateWithLifecycle()
                val appHeading = if (firstName.isNotBlank()) "Привет, $firstName!" else "МедСкан"
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MintPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appHeading,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintDarkText
                    )
                }
                Text(
                    text = "ИИ контроль аптечки и автосрок",
                    style = MaterialTheme.typography.bodySmall,
                    color = MintPrimary,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { viewModel.switchTab(ActiveTab.SETTINGS) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MintPrimary.copy(alpha = 0.12f),
                    contentColor = MintDarkText
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp).testTag("header_settings_nav_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MintPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Настройки",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Three stats cards exactly as shown in screenshot style
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatsMetricCard(
                label = "Всего",
                valStr = "$totalCount",
                bulletColor = ColorNeutral,
                isSelected = selectedFilter == CabinetFilter.ALL,
                selectedLineColor = MintPrimary,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setCabinetFilter(CabinetFilter.ALL) }
            )
            StatsMetricCard(
                label = "Истекли",
                valStr = "$expiredCount",
                bulletColor = ColorExpired,
                isSelected = selectedFilter == CabinetFilter.EXPIRED,
                selectedLineColor = ColorExpired,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setCabinetFilter(CabinetFilter.EXPIRED) }
            )
            StatsMetricCard(
                label = "Внимание",
                valStr = "$expiringCount",
                bulletColor = ColorExpiring,
                isSelected = selectedFilter == CabinetFilter.WARNING,
                selectedLineColor = ColorExpiring,
                modifier = Modifier
                    .weight(1f)
                    .clickable { viewModel.setCabinetFilter(CabinetFilter.WARNING) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setCabinetSearchQuery(it) },
            placeholder = { Text("Поиск лекарств по названию...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MintPrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setCabinetSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("cabinet_search_field"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Multi-filters horizontal chips precisely styled like the attachment
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            item {
                CabinetFilterChip(
                    label = "Все",
                    active = selectedFilter == CabinetFilter.ALL,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.ALL) }
                )
            }
            item {
                CabinetFilterChip(
                    label = "Годные",
                    active = selectedFilter == CabinetFilter.VALID,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.VALID) }
                )
            }
            item {
                CabinetFilterChip(
                    label = "Внимание",
                    active = selectedFilter == CabinetFilter.WARNING,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.WARNING) }
                )
            }
            item {
                CabinetFilterChip(
                    label = "Истекли",
                    active = selectedFilter == CabinetFilter.EXPIRED,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.EXPIRED) }
                )
            }
            item {
                CabinetFilterChip(
                    label = "Мало остатка",
                    active = selectedFilter == CabinetFilter.LOW_STOCK,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.LOW_STOCK) }
                )
            }
            item {
                CabinetFilterChip(
                    label = "Закончились",
                    active = selectedFilter == CabinetFilter.OUT_OF_STOCK,
                    onClick = { viewModel.setCabinetFilter(CabinetFilter.OUT_OF_STOCK) }
                )
            }
        }

        // State for tag filtration
        val allTags = remember(allMedicinesList) {
            allMedicinesList
                .flatMap { it.tags.split(",").map { t -> t.trim() }.filter { t -> t.isNotEmpty() } }
                .distinct()
                .sorted()
        }
        var selectedTagFilter by remember { mutableStateOf<String?>(null) }
        var showTagsFilterRow by remember { mutableStateOf(false) }
        var tagSearchQuery by remember { mutableStateOf("") }

        val filteredTags = remember(allTags, tagSearchQuery) {
            if (tagSearchQuery.isBlank()) {
                allTags
            } else {
                allTags.filter { it.contains(tagSearchQuery, ignoreCase = true) }
            }
        }

        if (allTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (showTagsFilterRow || selectedTagFilter != null) LavenderActive else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { showTagsFilterRow = !showTagsFilterRow }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (selectedTagFilter != null) Icons.Default.LocalOffer else Icons.Default.FilterList,
                            contentDescription = null,
                            tint = if (showTagsFilterRow || selectedTagFilter != null) LavenderText else Color(0xFF334A47),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (selectedTagFilter == null) "Поиск по тегам" else "Тег: $selectedTagFilter",
                            color = if (showTagsFilterRow || selectedTagFilter != null) LavenderText else Color(0xFF334A47),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (selectedTagFilter != null) {
                            Box(
                                modifier = Modifier
                                    .clickable { selectedTagFilter = null }
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Сбросить тег",
                                    tint = LavenderText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (showTagsFilterRow) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (showTagsFilterRow) LavenderText else Color(0xFF334A47),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (showTagsFilterRow) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Tag Search Box
                        androidx.compose.material3.OutlinedTextField(
                            value = tagSearchQuery,
                            onValueChange = { tagSearchQuery = it },
                            placeholder = { Text("Введите название тега...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MintPrimary, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (tagSearchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { tagSearchQuery = "" },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Очистить", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 40.dp)
                                .testTag("tag_filter_search_input"),
                            shape = RoundedCornerShape(8.dp),
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MintPrimary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Найдено тегов: ${filteredTags.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (selectedTagFilter != null) {
                                TextButton(
                                    onClick = { selectedTagFilter = null },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.height(24.dp)
                                ) {
                                    Text("Сбросить", style = MaterialTheme.typography.bodySmall, color = ColorExpired, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(scrollState)
                            ) {
                                if (filteredTags.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Теги не найдены",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    filteredTags.forEach { tag ->
                                        val isSelected = selectedTagFilter == tag
                                        val count = allMedicinesList.count { med ->
                                            med.tags.split(",").map { t -> t.trim().lowercase() }.contains(tag.lowercase())
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isSelected) LavenderActive.copy(alpha = 0.6f) else Color.Transparent
                                                )
                                                .clickable {
                                                    selectedTagFilter = if (isSelected) null else tag
                                                    showTagsFilterRow = false // Close upon selection/deselection as requested
                                                }
                                                .padding(horizontal = 8.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalOffer,
                                                    contentDescription = null,
                                                    tint = if (isSelected) LavenderText else Color(0xFF6B807B),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = tag,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) LavenderText else MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (isSelected) LavenderText.copy(alpha = 0.15f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                                                            CircleShape
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "$count шт.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isSelected) LavenderText else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Выбрано",
                                                        tint = LavenderText,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Ваши лекарства",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MintDarkText
            )

            if (isMultiSelectMode) {
                TextButton(
                    onClick = { selectedSerials = emptySet() }
                ) {
                    Text("Сбросить (${selectedSerials.size})", color = ColorExpired, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (isMultiSelectMode) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MintPrimary.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { selectedSerials = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена", tint = MintPrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Выбрано: ${selectedSerials.size}",
                            fontWeight = FontWeight.Bold,
                            color = MintDarkText,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                selectedSerials = medicines.map { it.serial }.toSet()
                            }
                        ) {
                            Text("Все", color = MintPrimary, fontWeight = FontWeight.Bold)
                        }
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Button(
                            onClick = {
                                val toDelete = medicines.filter { selectedSerials.contains(it.serial) }
                                toDelete.forEach { med ->
                                    viewModel.deleteFromCabinet(med)
                                }
                                selectedSerials = emptySet()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorExpired),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = "Удалить", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Удалить", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            val pageFilter = CabinetFilter.values()[page]
            val pageMedicines = remember(allMedicinesList, searchQuery, pageFilter, quantityThresholdCount, selectedTagFilter) {
                var result = allMedicinesList
                if (searchQuery.isNotBlank()) {
                    result = result.filter { 
                        it.name.contains(searchQuery, ignoreCase = true) || 
                        it.gtin.contains(searchQuery) ||
                        it.tags.contains(searchQuery, ignoreCase = true)
                    }
                }
                if (selectedTagFilter != null) {
                    val target = selectedTagFilter!!
                    result = result.filter { med ->
                        med.tags.split(",").map { t -> t.trim().lowercase() }.contains(target.lowercase())
                    }
                }
                when (pageFilter) {
                    CabinetFilter.ALL -> result
                    CabinetFilter.VALID -> result.filter { viewModel.getExpirationStatus(it) == ExpirationStatus.VALID }
                    CabinetFilter.WARNING -> result.filter { viewModel.getExpirationStatus(it) == ExpirationStatus.EXPIRING_SOON }
                    CabinetFilter.EXPIRED -> result.filter { viewModel.getExpirationStatus(it) == ExpirationStatus.EXPIRED }
                    CabinetFilter.LOW_STOCK -> result.filter { it.remainingCount > 0 && it.remainingCount <= quantityThresholdCount }
                    CabinetFilter.OUT_OF_STOCK -> result.filter { it.remainingCount <= 0 }
                }
            }

            if (pageMedicines.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.HealthAndSafety,
                            contentDescription = null,
                            tint = MintPrimary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isEmpty()) "Ваша аптечка пуста" else "Ничего не найдено",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Сфотографируйте упаковку или добавьте вручную",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pageMedicines) { med ->
                        val isSelected = selectedSerials.contains(med.serial)
                        MedicineCabinetItemCard(
                            medicine = med,
                            status = viewModel.getExpirationStatus(med),
                            selected = isSelected,
                            isMultiSelectMode = isMultiSelectMode,
                            onClick = {
                                if (isMultiSelectMode) {
                                    selectedSerials = if (isSelected) {
                                        selectedSerials - med.serial
                                    } else {
                                        selectedSerials + med.serial
                                    }
                                } else {
                                    viewModel.selectMedicine(med)
                                }
                            },
                            onLongClick = {
                                if (!isMultiSelectMode) {
                                    selectedSerials = setOf(med.serial)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatsMetricCard(
    label: String,
    valStr: String,
    bulletColor: Color,
    isSelected: Boolean,
    selectedLineColor: Color,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) selectedLineColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val borderThickness = if (isSelected) 2.dp else 1.dp

    Card(
        modifier = modifier
            .height(86.dp)
            .border(borderThickness, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Canvas(modifier = Modifier.size(8.dp)) {
                    drawCircle(color = bulletColor)
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = valStr,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) selectedLineColor else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Start)
            )
        }
    }
}

@Composable
fun CabinetFilterChip(
    label: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val bg = if (active) LavenderActive else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    val textColor = if (active) LavenderText else Color(0xFF334A47)

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MedicineCabinetItemCard(
    medicine: Medicine,
    status: ExpirationStatus,
    selected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val statusColor = when (status) {
        ExpirationStatus.EXPIRED -> ColorExpired
        ExpirationStatus.EXPIRING_SOON -> ColorExpiring
        ExpirationStatus.VALID -> ColorValid
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("medicine_item_${medicine.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (selected) {
            androidx.compose.foundation.BorderStroke(2.dp, MintPrimary)
        } else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 4.dp else 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelectMode) {
                androidx.compose.material3.Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = MintPrimary,
                        uncheckedColor = MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            // Package Image Thumbnail or Shield Icon
            val imageFile = medicine.packageImagePath?.let { java.io.File(it) }
            if (imageFile != null && imageFile.exists()) {
                coil.compose.AsyncImage(
                    model = imageFile,
                    contentDescription = "Упаковка ${medicine.name}",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Shield,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = medicine.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val formattedRem = if (medicine.remainingCount % 1.0 == 0.0) medicine.remainingCount.toInt().toString() else String.format(java.util.Locale.US, "%.1f", medicine.remainingCount)
                val remainingText = if (medicine.remainingCount <= 0.0) "Закончился" else "$formattedRem шт."
                Text(
                    text = "Остаток: $remainingText / ${medicine.totalPackageCount} шт.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (medicine.remainingCount <= 5.0) ColorExpired else MintPrimary
                )
                if (medicine.gtin.isNotEmpty()) {
                    Text(
                        text = "GTIN: ${medicine.gtin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Введено вручную",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (medicine.tags.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                    androidx.compose.foundation.layout.FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        medicine.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MintPrimary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = tag,
                                    fontSize = 10.sp,
                                    color = MintPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.widthIn(max = 100.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = status.label,
                        color = statusColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "до ${formatRussianDate(medicine.expirationDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ================= COGNITIVE PHOTO SCANNING TAB SCREEN =================

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PhotoRecognitionScreen(viewModel: MainViewModel) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Определение лекарства по фото пачки",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MintDarkText
        )
        Text(
            text = "Сфотографируйте упаковку препарата. ИИ автоматически распознает коммерческое название и найдет срок годности.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black)
        ) {
            if (cameraPermissionState.status.isGranted) {
                PackagingCameraViewfinder(
                    onPhotoCaptured = { base64, path -> viewModel.parsePackagePhoto(base64, null, path) }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Требуется разрешение на камеру",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Пожалуйста, предоставите права доступа к камере для выполнения моментальных фото-сканирований.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { cameraPermissionState.launchPermissionRequest() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
                        ) {
                            Text("Разрешить доступ")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PackagingCameraViewfinder(
    onPhotoCaptured: (String, String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Set up standard modern LifecycleCameraController
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom laser scanning lines
        ScannerLineOverlay()

        // Capture trigger and guidance overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hint / advice for placing package correctly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E3230), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Разместите упаковку так, чтобы было видно лицевую сторону и срок годности, если это возможно",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Large circular camera shutter capture button
            Button(
                onClick = {
                    try {
                        val mainExecutor = ContextCompat.getMainExecutor(context)
                        val tempFile = File.createTempFile("med_cap", ".jpg", context.cacheDir)
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                        cameraController.takePicture(
                            outputOptions,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    val permanentPath = try {
                                        val destDir = java.io.File(context.filesDir, "temp_package_images")
                                        if (!destDir.exists()) destDir.mkdirs()
                                        val destFile = java.io.File(destDir, "raw_${System.currentTimeMillis()}.jpg")
                                        val optimizedBytes = com.example.api.scaleAndCompressImageFile(tempFile, maxDimension = 1024, quality = 85)
                                        if (optimizedBytes != null) {
                                            destFile.writeBytes(optimizedBytes)
                                        } else {
                                            tempFile.copyTo(destFile, overwrite = true)
                                        }
                                        destFile.absolutePath
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    }
                                    
                                    val base64 = if (permanentPath != null) {
                                        val destFile = java.io.File(permanentPath)
                                        if (destFile.exists()) {
                                            val bytes = destFile.readBytes()
                                            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                                        } else {
                                            ""
                                        }
                                    } else {
                                        tempFile.toBase64()
                                    }
                                    
                                    try { tempFile.delete() } catch(e: Exception) {}
                                    
                                    onPhotoCaptured(base64, permanentPath)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Toast.makeText(context, "Используется авто-подбор (Камера недоступна)", Toast.LENGTH_SHORT).show()
                                    onPhotoCaptured("", null)
                                }
                            }
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Используется авто-подбор (Камера недоступна)", Toast.LENGTH_SHORT).show()
                        onPhotoCaptured("", null)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                modifier = Modifier
                    .size(56.dp)
                    .testTag("shutter_button"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = "Сделать снимок",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ScannerLineOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val animYOffset by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_laser_line"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // Draw scanning laser in Mint
        drawLine(
            color = MintPrimary,
            start = Offset(0f, height * animYOffset),
            end = Offset(width, height * animYOffset),
            strokeWidth = 3.dp.toPx()
        )

        // Guide scope corners design
        val length = 32.dp.toPx()
        val thickness = 4.dp.toPx()
        val margin = 48.dp.toPx()

        val left = margin
        val right = width - margin
        val top = margin
        val bottom = height - margin

        // Top Left
        drawLine(MintPrimary, Offset(left, top), Offset(left + length, top), thickness)
        drawLine(MintPrimary, Offset(left, top), Offset(left, top + length), thickness)

        // Top Right
        drawLine(MintPrimary, Offset(right, top), Offset(right - length, top), thickness)
        drawLine(MintPrimary, Offset(right, top), Offset(right, top + length), thickness)

        // Bottom Left
        drawLine(MintPrimary, Offset(left, bottom), Offset(left + length, bottom), thickness)
        drawLine(MintPrimary, Offset(left, bottom), Offset(left, bottom - length), thickness)

        // Bottom Right
        drawLine(MintPrimary, Offset(right, bottom), Offset(right - length, bottom), thickness)
        drawLine(MintPrimary, Offset(right, bottom), Offset(right, bottom - length), thickness)
    }
}

// ================= SEARCH ANALOGS TAB SCREEN =================

@Composable
fun SearchAnalogsScreen(viewModel: MainViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val analogsUiState by viewModel.analogsUiState.collectAsStateWithLifecycle()
    var rawSearchText by remember { mutableStateOf("") }

    // Synchronize initial screen loading state
    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty() && rawSearchText.isEmpty()) {
            rawSearchText = searchQuery
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Поиск эффективных аналогов AI",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MintDarkText
        )
        Text(
            text = "Введите название лекарства, чтобы получить полное описание, действующее вещество и дженерики.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = rawSearchText,
                onValueChange = { rawSearchText = it },
                placeholder = { Text("Например: Но-Шпа, Нурофен, Ксарелто...") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    keyboardController?.hide()
                    viewModel.searchMedicineAnalogs(rawSearchText)
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("analogs_search_field"),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.searchMedicineAnalogs(rawSearchText)
                },
                modifier = Modifier
                    .height(56.dp)
                    .testTag("analogs_search_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "Искать")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UI states displaying analogs
        switchAnalogsUiState(analogsUiState = analogsUiState, viewModel = viewModel, rawSearchTextUpdated = { rawSearchText = it })
    }
}

@Composable
fun switchAnalogsUiState(
    analogsUiState: AnalogsUiState,
    viewModel: MainViewModel,
    rawSearchTextUpdated: (String) -> Unit
) {
    when (analogsUiState) {
        is AnalogsUiState.Idle -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(LavenderActive.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "ИИ-советник готов к подбору",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Аналоги подбираются на глубокой клинической базе.\nНажмите на пример ниже, чтобы протестировать:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Suggested chips for templates
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Но-Шпа", "Нурофен", "Ксарелто").forEach { demoName ->
                            SuggestionChip(
                                onClick = {
                                    rawSearchTextUpdated(demoName)
                                    viewModel.searchMedicineAnalogs(demoName)
                                },
                                label = { Text(demoName, fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }
        }

        is AnalogsUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MintPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Ищем информацию в ИИ фармацевтике...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is AnalogsUiState.Success -> {
            val data = analogsUiState.result
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = data.origin_name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MintDarkText
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            MintPrimary.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Действующее вещество:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MintPrimary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = data.active_substance,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = data.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "Рекомендованные аналоги",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(data.analogs) { analog ->
                    AnalogItemCard(analog = analog)
                }
            }
        }

        is AnalogsUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = ColorExpired,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        analogsUiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun AnalogItemCard(analog: MedicineAnalog) {
    val priceColor = when (analog.price_category) {
        "Доступная" -> ColorValid
        "Средняя" -> ColorExpiring
        "Высокая" -> ColorExpired
        else -> MintPrimary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = analog.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .background(priceColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = analog.price_category,
                        color = priceColor,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Производитель: ${analog.manufacturer}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Default.ArrowCircleRight,
                    contentDescription = null,
                    tint = ColorValid,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = analog.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ================= MODAL/DIALOG POPUPS =================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultDialog(
    result: ParsedZnakResult,
    onDismiss: () -> Unit,
    onSave: (updatedResult: ParsedZnakResult, notes: String, totalPackageCount: Int, intakeDosage: Double, intakeFrequency: String) -> Unit
) {
    var medicineName by remember { mutableStateOf(result.name) }
    var expirationDate by remember { mutableStateOf(result.expiration_date) }
    var batch by remember { mutableStateOf(result.batch) }
    var gtin by remember { mutableStateOf(result.gtin) }
    var rawNotes by remember { mutableStateOf("") }
    var tagsString by remember { mutableStateOf(result.tags?.joinToString(", ") ?: "") }
    var totalPackageCount by remember { mutableStateOf(result.package_count?.toString() ?: "30") }
    var intakeDosage by remember { mutableStateOf("0.0") }
    var intakeFrequency by remember { mutableStateOf("as_needed") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    val totalQty = totalPackageCount.toIntOrNull() ?: 30
                    val dosage = intakeDosage.toDoubleOrNull() ?: 0.0
                    val tagsList = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val updatedResult = result.copy(
                        name = medicineName.trim(),
                        expiration_date = expirationDate.trim(),
                        batch = batch.trim(),
                        gtin = gtin.trim(),
                        tags = tagsList
                    )
                    onSave(updatedResult, rawNotes, totalQty, dosage, intakeFrequency)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                modifier = Modifier.testTag("btn_save_to_cabinet")
            ) {
                Text("Сохранить в аптечку")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MintPrimary)
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TaskAlt,
                    contentDescription = null,
                    tint = ColorValid,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Упаковка Распознана ИИ")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Вы можете проверить и скорректировать данные перед добавлением:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = medicineName,
                    onValueChange = { medicineName = it },
                    label = { Text("Название препарата *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("scan_med_name_field")
                )

                OutlinedTextField(
                    value = expirationDate,
                    onValueChange = { expirationDate = it },
                    label = { Text("Срок годности (ГГГГ-ММ-ДД) *") },
                    placeholder = { Text("Пример: 2027-12-31") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("Серия лекарства") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gtin,
                    onValueChange = { gtin = it },
                    label = { Text("GTIN штрихкода") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Quantity input with AI recognition feedback
                val isQtyAutoFilled = result.package_count != null && result.package_count > 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isQtyAutoFilled) ColorValid.copy(alpha = 0.08f) else ColorNeutral.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = if (isQtyAutoFilled) {
                            "Количество в упаковке определено по фото: ${result.package_count} шт."
                        } else {
                            "Количество не определено по фото. Пожалуйста, заполните вручную:"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isQtyAutoFilled) ColorValid else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = totalPackageCount,
                    onValueChange = { totalPackageCount = it },
                    label = { Text("Количество в упаковке *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = rawNotes,
                    onValueChange = { rawNotes = it },
                    label = { Text("Заметка (необязательно)") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_save_notes_field")
                )

                OutlinedTextField(
                    value = tagsString,
                    onValueChange = { tagsString = it },
                    label = { Text("Теги ИИ (можно изменить через запятую)") },
                    placeholder = { Text("например: Обезболивающее, Жаропонижающее") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scan_save_tags_field")
                )
            }
        }
    )
}

@Composable
fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MedicineDetailsDialog(
    medicine: Medicine,
    status: ExpirationStatus,
    instructionUiState: InstructionUiState,
    onSimulateIntake: () -> Unit,
    onLoadInstruction: () -> Unit,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSaveNotes: (String) -> Unit,
    onUpdateMedicine: (Medicine) -> Unit,
    onManualTake: (Double) -> Unit,
    pillboxes: List<com.example.data.Pillbox>,
    onAddToPillbox: (pillboxId: Int, dosage: Double, preferredTime: String, periodicityDays: Int) -> Unit
) {
    var editNotesText by remember(medicine.id) { mutableStateOf(medicine.notes) }
    var isEditingNotes by remember(medicine.id) { mutableStateOf(false) }

    var showManualTakeInput by remember(medicine.id) { mutableStateOf(false) }
    var manualTakeAmount by remember(medicine.id) { mutableStateOf("1.0") }

    var showAddToPillboxForm by remember(medicine.id) { mutableStateOf(false) }
    var selectedPillboxId by remember(medicine.id, pillboxes) { mutableStateOf(pillboxes.firstOrNull()?.id ?: 0) }
    var inputPillboxDosage by remember(medicine.id) { mutableStateOf(medicine.intakeDosage.toString()) }
    var inputPillboxTime by remember(medicine.id) { mutableStateOf("08:00") }
    var inputPillboxPeriodicity by remember(medicine.id) { mutableStateOf(1) } // 1 = каждый день, 2 = через день

    var isEditingSchedule by remember(medicine.id) { mutableStateOf(false) }
    var editDosage by remember(medicine.id) { mutableStateOf(medicine.intakeDosage.toString()) }
    var editFrequency by remember(medicine.id) { mutableStateOf(medicine.intakeFrequency) }
    var editRemaining by remember(medicine.id) { mutableStateOf(medicine.remainingCount.toString()) }
    var editTotal by remember(medicine.id) { mutableStateOf(medicine.totalPackageCount.toString()) }

    val statusColor = when (status) {
        ExpirationStatus.EXPIRED -> ColorExpired
        ExpirationStatus.EXPIRING_SOON -> ColorExpiring
        ExpirationStatus.VALID -> ColorValid
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = MintPrimary)
            }
        },
        dismissButton = {
            IconButton(
                onClick = {
                    onDelete()
                    onDismiss()
                },
                modifier = Modifier.testTag("btn_delete_med")
            ) {
                Icon(Icons.Default.DeleteForever, contentDescription = "Удалить", tint = ColorExpired)
            }
        },
        title = {
            Text(
                medicine.name,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // If there's an image, display a beautiful large package image at the top
                val imageFile = medicine.packageImagePath?.let { java.io.File(it) }
                if (imageFile != null && imageFile.exists()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        coil.compose.AsyncImage(
                            model = imageFile,
                            contentDescription = "Упаковка ${medicine.name}",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Expiry panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Статус годности:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = status.label,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Препарат годен до: ${formatRussianDate(medicine.expirationDate)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }

                // Core Intake Consumption Card
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Расход и схема приема",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    if (isEditingSchedule) {
                                        val parseDosage = editDosage.toDoubleOrNull() ?: medicine.intakeDosage
                                        val parseRemaining = editRemaining.toDoubleOrNull() ?: medicine.remainingCount
                                        val parseTotal = editTotal.toIntOrNull() ?: medicine.totalPackageCount
                                        val updatedMed = medicine.copy(
                                            intakeDosage = parseDosage,
                                            intakeFrequency = editFrequency,
                                            remainingCount = parseRemaining,
                                            totalPackageCount = parseTotal
                                        )
                                        onUpdateMedicine(updatedMed)
                                    }
                                    isEditingSchedule = !isEditingSchedule
                                }
                            ) {
                                Text(
                                    text = if (isEditingSchedule) "Сохранить" else "Изменить",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MintPrimary
                                )
                            }
                        }
                        
                        if (!isEditingSchedule) {
                            val formattedRemaining = if (medicine.remainingCount % 1.0 == 0.0) medicine.remainingCount.toInt().toString() else String.format(java.util.Locale.US, "%.1f", medicine.remainingCount)
                            val remainingText = if (medicine.remainingCount <= 0.0) "ЗАКОНЧИЛСЯ" else "$formattedRemaining шт."
                            val badgeColor = if (medicine.remainingCount <= 5.0) ColorExpired else ColorValid
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Всего при добавлении: ${medicine.totalPackageCount} шт.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Остаток: $remainingText",
                                        color = badgeColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            val freqLabel = when (medicine.intakeFrequency) {
                                "1_daily" -> "1 раз в день"
                                "2_daily" -> "2 раза в день"
                                "3_daily" -> "3 раза в день"
                                "every_other_day" -> "Каждые 2 дня"
                                "as_needed" -> "По необходимости"
                                else -> "По необходимости"
                            }
                            
                            val dosageText = if (medicine.intakeDosage % 1.0 == 0.0) medicine.intakeDosage.toInt().toString() else medicine.intakeDosage.toString()
                            Text(
                                text = "Режим приема: $dosageText шт. ($freqLabel)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            if (medicine.intakeDosage > 0.0 && medicine.intakeFrequency != "as_needed" && medicine.remainingCount > 0.0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MintPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Списание препарата производится автоматически на основе схемы приема каждый день.",
                                        fontSize = 11.sp,
                                        color = MintPrimary,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = editDosage,
                                onValueChange = { editDosage = it },
                                label = { Text("Разовая доза (таб. / шт.)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editRemaining,
                                    onValueChange = { editRemaining = it },
                                    label = { Text("Текущий остаток") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editTotal,
                                    onValueChange = { editTotal = it },
                                    label = { Text("Всего в уп.") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Схема регулярного приема:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val frequencies = listOf(
                                    "as_needed" to "По необходимости",
                                    "1_daily" to "1 раз в день",
                                    "2_daily" to "2 раза в день",
                                    "3_daily" to "3 раза в день",
                                    "every_other_day" to "Каждые 2 дня"
                                )
                                frequencies.chunked(2).forEach { rowKeys ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        rowKeys.forEach { (key, label) ->
                                            val selected = editFrequency == key
                                            Button(
                                                onClick = { editFrequency = key },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (selected) MintPrimary else MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                            ) {
                                                Text(label, fontSize = 10.sp, maxLines = 1)
                                            }
                                        }
                                        if (rowKeys.size < 2) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive manual consumption panel (Requirement 10)
                Spacer(modifier = Modifier.height(12.dp))
                if (!showManualTakeInput) {
                    Button(
                        onClick = { showManualTakeInput = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                        modifier = Modifier.fillMaxWidth().testTag("medicine_dialog_took_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Я принял (списание из остатка)", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Сколько вы приняли?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = manualTakeAmount,
                                    onValueChange = { manualTakeAmount = it },
                                    label = { Text("Количество (шт/мл)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f).testTag("manual_take_amount_input")
                                )
                                Button(
                                    onClick = {
                                        val amount = manualTakeAmount.toDoubleOrNull() ?: 1.0
                                        onManualTake(amount)
                                        showManualTakeInput = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                    modifier = Modifier.testTag("manual_take_confirm_button")
                                ) {
                                    Text("ОК")
                                }
                                TextButton(onClick = { showManualTakeInput = false }) {
                                    Text("Отмена", color = ColorExpired)
                                }
                            }
                        }
                    }
                }

                // Add to Pillbox action
                Spacer(modifier = Modifier.height(12.dp))
                if (!showAddToPillboxForm) {
                    Button(
                        onClick = { showAddToPillboxForm = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintPrimary.copy(alpha = 0.12f),
                            contentColor = MintDarkText
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("add_to_pillbox_action_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MintPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить в таблетницу", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Добавить в таблетницу", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MintDarkText)
                            
                            if (pillboxes.isEmpty()) {
                                Text(
                                    "У вас пока нет созданных таблетниц. Пожалуйста, сначала создайте таблетницу на вкладке «Таблетки».",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = ColorExpired
                                )
                                TextButton(onClick = { showAddToPillboxForm = false }) {
                                    Text("Понятно", color = MintPrimary)
                                }
                            } else {
                                Text("Выберите таблетницу:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    pillboxes.forEach { pb ->
                                        val isSelected = pb.id == selectedPillboxId
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    color = if (isSelected) MintPrimary.copy(alpha = 0.15f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) MintPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { selectedPillboxId = pb.id }
                                                .padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Folder,
                                                contentDescription = null,
                                                tint = if (isSelected) MintPrimary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = pb.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) MintDarkText else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val (initH, initM) = remember(inputPillboxTime) {
                                        val p = inputPillboxTime.split(":")
                                        val h = p.getOrNull(0)?.toIntOrNull() ?: 8
                                        val m = p.getOrNull(1)?.toIntOrNull() ?: 0
                                        h to m
                                    }
                                    val context = LocalContext.current
                                    
                                    Button(
                                        onClick = {
                                            android.app.TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    inputPillboxTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                                                },
                                                initH,
                                                initM,
                                                true
                                            ).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp), tint = MintPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Время: $inputPillboxTime", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                    }
                                    
                                    OutlinedTextField(
                                        value = inputPillboxDosage,
                                        onValueChange = { inputPillboxDosage = it },
                                        label = { Text("Доза") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Периодичность:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(1 to "Каждый день", 2 to "Через день", 3 to "Каждые 3 дня").forEach { (days, label) ->
                                        val isSelected = inputPillboxPeriodicity == days
                                        Button(
                                            onClick = { inputPillboxPeriodicity = days },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isSelected) MintPrimary else MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                                        ) {
                                            Text(label, fontSize = 10.sp, maxLines = 1)
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { showAddToPillboxForm = false }) {
                                        Text("Отмена", color = ColorExpired)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val dosage = inputPillboxDosage.toDoubleOrNull() ?: 1.0
                                            onAddToPillbox(selectedPillboxId, dosage, inputPillboxTime, inputPillboxPeriodicity)
                                            showAddToPillboxForm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Добавить")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ResultRow(label = "Штрихкод (GTIN)", value = if (medicine.gtin.isEmpty()) "Введено вручную" else medicine.gtin)
                ResultRow(label = "Серия (Batch)", value = if (medicine.batch.isEmpty()) "Отсутствует" else medicine.batch)
                ResultRow(label = "Добавлено", value = SimpleDateFormat("HH:mm dd.MM.yyyy", Locale("ru")).format(Date(medicine.scannedAt)))

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                // Notes editing block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Заметки в аптечке:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = {
                        if (isEditingNotes) {
                            onSaveNotes(editNotesText)
                        }
                        isEditingNotes = !isEditingNotes
                    }) {
                        Text(if (isEditingNotes) "Сохранить" else "Изменить")
                    }
                }

                if (isEditingNotes) {
                    OutlinedTextField(
                        value = editNotesText,
                        onValueChange = { editNotesText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (medicine.notes.isNotBlank()) medicine.notes else "Нет заметок. Добавьте сюда назначение препарата или дозировку.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (medicine.notes.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))

                var tagInput by remember(medicine.id) { mutableStateOf("") }
                var tagsList by remember(medicine.tags) {
                    mutableStateOf(if (medicine.tags.isBlank()) emptyList<String>() else medicine.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
                }

                Text(
                    "Теги препарата (нажмите для удаления):",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tagsList.isEmpty()) {
                        Text(
                            "Нет тегов. Добавьте теги ниже для удобного поиска и фильтрации.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        tagsList.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .background(MintPrimary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        val newList = tagsList - tag
                                        tagsList = newList
                                        onUpdateMedicine(medicine.copy(tags = newList.joinToString(", ")))
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tag,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MintPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Удалить тег",
                                        tint = MintPrimary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Добавить свой тег...", style = MaterialTheme.typography.bodyMedium) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tag_input_field"),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            val cleanTag = tagInput.trim()
                            if (cleanTag.isNotEmpty() && !tagsList.contains(cleanTag)) {
                                val newList = tagsList + cleanTag
                                tagsList = newList
                                onUpdateMedicine(medicine.copy(tags = newList.joinToString(", ")))
                                tagInput = ""
                            }
                        })
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            val cleanTag = tagInput.trim()
                            if (cleanTag.isNotEmpty() && !tagsList.contains(cleanTag)) {
                                val newList = tagsList + cleanTag
                                tagsList = newList
                                onUpdateMedicine(medicine.copy(tags = newList.joinToString(", ")))
                                tagInput = ""
                            }
                        },
                        modifier = Modifier.testTag("btn_add_tag")
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Добавить тег", tint = MintPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // AI clinical instruction panel
                Text(
                    "Инструкция и применение:",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MintPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                switchInstructionUiState(instructionUiState = instructionUiState, onLoadInstruction = onLoadInstruction)
            }
        }
    )
}

@Composable
fun switchInstructionUiState(
    instructionUiState: InstructionUiState,
    onLoadInstruction: () -> Unit
) {
    when (instructionUiState) {
        is InstructionUiState.Idle -> {
            Button(
                onClick = onLoadInstruction,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_load_instruction"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Инструкция ИИ и способ применения", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }
        }

        is InstructionUiState.Loading -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(28.dp),
                    color = MintPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Загрузка инструкции ИИ...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        is InstructionUiState.Success -> {
            val instruction = instructionUiState.instruction
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                InstructionSection(label = "Действующее вещество", content = instruction.active_substance, isPrimary = true)
                InstructionSection(label = "Фармакоррекция", content = instruction.description)
                InstructionSection(label = "Показания к применению", content = instruction.indications)
                InstructionSection(label = "Способ применения и дозы", content = instruction.dosage, highlight = true)
                InstructionSection(label = "Противопоказания", content = instruction.contraindications)
                InstructionSection(label = "Побочные действия", content = instruction.side_effects)
                InstructionSection(label = "Особые указания ИИ", content = instruction.special_instructions)
            }
        }

        is InstructionUiState.Error -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = instructionUiState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onLoadInstruction,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Повторить", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun InstructionSection(
    label: String,
    content: String,
    isPrimary: Boolean = false,
    highlight: Boolean = false
) {
    if (content.isNotBlank()) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = label,
                style = if (isPrimary) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MintPrimary else if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
        }
    }
}

// Dialog: Manual Drug Addition
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicineManualDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, expDate: String, notes: String, batch: String, gtin: String, totalPackageCount: Int, intakeDosage: Double, intakeFrequency: String, tags: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var gtin by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var totalPackageCount by remember { mutableStateOf("30") }
    var intakeDosage by remember { mutableStateOf("0.0") }
    var intakeFrequency by remember { mutableStateOf("as_needed") }

    var showError by remember { mutableStateOf("") }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить вручную", fontWeight = FontWeight.Black, fontSize = 20.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isBlank()) {
                        showError = "Введите название лекарства"
                        return@Button
                    }
                    if (expirationDate.trim().isBlank() || !expirationDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        showError = "Введите дату в формате ГГГГ-ММ-ДД"
                        return@Button
                    }
                    val pkgCount = totalPackageCount.toIntOrNull() ?: 30
                    val dosage = intakeDosage.toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), expirationDate.trim(), notes.trim(), batch.trim(), gtin.trim(), pkgCount, dosage, intakeFrequency, tags.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = MintPrimary)
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MintPrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showError.isNotEmpty()) {
                    Text(
                        text = showError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название препарата *") },
                    placeholder = { Text("например: Энтерофурил 200мг") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expirationDate,
                        onValueChange = { expirationDate = it },
                        label = { Text("Срок годности * (ГГГГ-ММ-ДД)") },
                        placeholder = { Text("например, 2027-07-31") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            val datePickerDialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedMonth = String.format("%02d", month + 1)
                                    val formattedDay = String.format("%02d", dayOfMonth)
                                    expirationDate = "$year-$formattedMonth-$formattedDay"
                                    showError = ""
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        }
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Выбрать дату",
                            tint = MintPrimary
                        )
                    }
                }

                OutlinedTextField(
                    value = totalPackageCount,
                    onValueChange = { totalPackageCount = it },
                    label = { Text("Количество в упаковке *") },
                    placeholder = { Text("например: 30") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = batch,
                    onValueChange = { batch = it },
                    label = { Text("Серия выпуска (необязательно)") },
                    placeholder = { Text("например: AB7700") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = gtin,
                    onValueChange = { gtin = it },
                    label = { Text("Штрихкод / GTIN (необязательно)") },
                    placeholder = { Text("например: 4607027768563") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Заметка (необязательно)") },
                    placeholder = { Text("например: Для всей семьи") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Теги — через запятую (необязательно)") },
                    placeholder = { Text("например: Обезболивающее, Спазмолитик") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_manual_tags_field")
                )
            }
        }
    )
}

// Helper: formats "YYYY-MM-DD" expiration strings to gorgeous Russian calendar labels e.g. "31 декабря 2026"
fun formatRussianDate(isoDate: String): String {
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = isoFormat.parse(isoDate) ?: return isoDate
        val ruFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
        ruFormat.format(date)
    } catch (e: Exception) {
        isoDate
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val themeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val expirationThreshold by viewModel.expirationThresholdDays.collectAsStateWithLifecycle()
    val quantityThreshold by viewModel.quantityThresholdCount.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Настройки приложения",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Section 1: Visual Theme Selection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = "Тема оформления",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Оформление (Светлая / Темная)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val modes = listOf("Системная", "Светлая", "Тёмная")
                        modes.forEachIndexed { index, name ->
                            val selected = themeMode == index
                            Button(
                                onClick = { viewModel.updateThemeMode(index) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Expiration Notifications Lead Time Control
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Предупреждение о сроке годности",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Уведомление об истечении срока",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Text(
                        text = "За сколько дней до окончания срока годности отправлять предупреждение об утилизации:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val offsetDays = listOf(30, 60, 90, 180)
                        offsetDays.forEach { days ->
                            val selected = expirationThreshold == days
                            Button(
                                onClick = { viewModel.updateExpirationThresholdDays(days) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("$days дн.", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Low supplies Packaging Warning
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Предупреждение о количестве",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Окончание упаковки",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    Text(
                        text = "Уведомлять, когда количество таблеток/капсул в упаковке снижается до критического уровня:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val pillLimits = listOf(2, 5, 10, 20)
                        pillLimits.forEach { limit ->
                            val selected = quantityThreshold == limit
                            Button(
                                onClick = { viewModel.updateQuantityThresholdCount(limit) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                            ) {
                                Text("$limit шт.", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION: Multi-Device Account Sync and Settings ---
        item {
            var showConnectDialog by remember { mutableStateOf(false) }
            val syncAccountId by viewModel.syncAccountId.collectAsStateWithLifecycle()
            val syncPassphrase by viewModel.syncPassphrase.collectAsStateWithLifecycle()
            val syncAutoEnabled by viewModel.syncAutoEnabled.collectAsStateWithLifecycle()
            val syncUiState by viewModel.syncUiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

            if (showConnectDialog) {
                ConnectSharedAccountDialog(
                    onDismiss = { showConnectDialog = false },
                    onConnect = { acc, pass ->
                        viewModel.connectSharedAccount(acc, pass)
                    }
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (syncAccountId.isNotEmpty())
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (syncAccountId.isNotEmpty())
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    else
                        Color.Transparent
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (syncAccountId.isNotEmpty()) 
                                Icons.Default.HealthAndSafety 
                            else 
                                Icons.Default.Settings,
                            contentDescription = "Синхронизация",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Общая аптечка (Синхронизация)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (syncAccountId.isNotEmpty()) "Активно (Рабата на нескольких устройствах)" else "Локальный режим",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                    if (syncAccountId.isEmpty()) {
                        // Not connected layout
                        Text(
                            text = "Вы можете объединить домашние смартфоны вашей семьи под один аккаунт синхронизации. Ваши лекарства, сроки их годности и инвентарь будут автоматически обновляться везде.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { 
                                    viewModel.createSharedAccount()
                                    Toast.makeText(context, "Создан новый кабинет аптечки!", Toast.LENGTH_LONG).show()
                                },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("Создать общий", style = MaterialTheme.typography.labelLarge)
                            }

                            Button(
                                onClick = { showConnectDialog = true },
                                modifier = Modifier.weight(1.5f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 12.dp)
                            ) {
                                Text("Подключить код", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else {
                        // Connected layout
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Account details card
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Code Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Идентификатор аптечки:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = syncAccountId,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                val annotated = androidx.compose.ui.text.buildAnnotatedString { append(syncAccountId) }
                                                clipboardManager.setText(annotated)
                                                Toast.makeText(context, "Код аптечки скопирован! Отправьте его на другое устройство.", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TaskAlt,
                                                contentDescription = "Копировать код",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // Passphrase Row
                                    var showPassSecret by remember { mutableStateOf(false) }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text("Пароль доступа:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (showPassSecret) syncPassphrase else "••••••",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                        Row {
                                            IconButton(
                                                onClick = { showPassSecret = !showPassSecret }
                                            ) {
                                                Icon(
                                                    imageVector = if (showPassSecret) Icons.Default.Close else Icons.Default.Add,
                                                    contentDescription = "Показать пароль"
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    val annotated = androidx.compose.ui.text.buildAnnotatedString { append(syncPassphrase) }
                                                    clipboardManager.setText(annotated)
                                                    Toast.makeText(context, "Пароль скопирован!", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Shield,
                                                    contentDescription = "Копировать пароль",
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                                    Text(
                                        text = "Последний обмен: ${viewModel.getSyncLastTimeFormatted()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Auto sync switcher
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Авто-синхронизация", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        "Изменения автоматически отправляются в облако",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                androidx.compose.material3.Switch(
                                    checked = syncAutoEnabled,
                                    onCheckedChange = { viewModel.updateSyncAutoEnabled(it) }
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.executeSync("merge") },
                                    modifier = Modifier.weight(1.5f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Объединить данные", style = MaterialTheme.typography.bodyMedium)
                                }

                                Button(
                                    onClick = { viewModel.disconnectAccount() },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Выйти", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            // Extra Direct Options
                            var showExtraControls by remember { mutableStateOf(false) }
                            Text(
                                text = if (showExtraControls) "Скрыть расширенные опции" else "Показать расширенные опции",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable { showExtraControls = !showExtraControls }
                                    .padding(vertical = 4.dp)
                            )

                            if (showExtraControls) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.executeSync("upload") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Залить в облако", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { viewModel.executeSync("download") },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Забрать из облака", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                    }
                                }
                            }
                        }
                    }

                    // Network indicator
                    if (syncUiState is com.example.ui.SyncUiState.Syncing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text("Выполняется синхронизация...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (syncUiState is com.example.ui.SyncUiState.Success) {
                        Text(
                            text = (syncUiState as com.example.ui.SyncUiState.Success).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LaunchedEffect(syncUiState) {
                            kotlinx.coroutines.delay(4000)
                            viewModel.resetSyncUiState()
                        }
                    } else if (syncUiState is com.example.ui.SyncUiState.Error) {
                        Text(
                            text = (syncUiState as com.example.ui.SyncUiState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Системные сведения",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Расчет остатка доз и срока годности выполняется автоматически в реальном времени. Приложение самостоятельно контролирует утилизацию, малое количество и график списания ваших лекарств.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectSharedAccountDialog(
    onDismiss: () -> Unit,
    onConnect: (String, String) -> Unit
) {
    var accountId by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Подключение общей аптечки",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Введите идентификатор и пароль общей аптечки, созданной на другом вашем устройстве.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                OutlinedTextField(
                    value = accountId,
                    onValueChange = { 
                        accountId = it.trim().uppercase()
                        errorMsg = null
                    },
                    label = { Text("Идентификатор (MED-XXXXXX)") },
                    placeholder = { Text("MED-123456") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { 
                        passphrase = it.trim()
                        errorMsg = null
                    },
                    label = { Text("Пароль аптечки") },
                    placeholder = { Text("6 символов") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (accountId.isBlank() || passphrase.isBlank()) {
                        errorMsg = "Пожалуйста, заполните все поля!"
                    } else if (!accountId.startsWith("MED-") || accountId.length < 8) {
                        errorMsg = "Неверный формат идентификатора. Должен начинаться с MED- и содержать цифры."
                    } else {
                        onConnect(accountId, passphrase)
                        onDismiss()
                    }
                }
            ) {
                Text("Подключить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun MedicineAddChoiceDialog(
    onDismiss: () -> Unit,
    onAddManual: () -> Unit,
    onAddScanner: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = MintPrimary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить лекарство", fontWeight = FontWeight.Black, fontSize = 20.sp, color = MintDarkText)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Выберите способ добавления нового лекарства в вашу аптечку:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Card for manually adding
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddManual()
                        }
                        .testTag("choice_add_manual"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MintPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "Добавить вручную",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MintDarkText
                            )
                            Text(
                                "Заполнить форму с деталями лекарства самостоятельно",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Card for scanning details with AI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddScanner()
                        }
                        .testTag("choice_add_scanner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MintPrimary.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                "ИИ сканер (По фото)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MintDarkText
                            )
                            Text(
                                "Сфотографировать упаковку для автоматического распознавания с ИИ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MintPrimary, fontWeight = FontWeight.Bold)
            }
        }
    )
}

fun isSameDay(d1: java.util.Date, d2: java.util.Date): Boolean {
    val cal1 = java.util.Calendar.getInstance().apply { time = d1 }
    val cal2 = java.util.Calendar.getInstance().apply { time = d2 }
    return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
           cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
}

@Composable
fun AdherenceChart(
    selectedTab: String,
    logs: List<com.example.data.IntakeLog>
) {
    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val oneWeekMs = 7 * oneDayMs
    val oneMonthMs = 30 * oneDayMs
    val cal = java.util.Calendar.getInstance()
    
    when (selectedTab) {
        "day" -> {
            val dayLogs = logs.filter { it.takenTimestamp >= now - oneDayMs }
            val morning = dayLogs.count { 
                cal.timeInMillis = it.takenTimestamp
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 6..11
            }
            val afternoon = dayLogs.count { 
                cal.timeInMillis = it.takenTimestamp
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 12..17
            }
            val evening = dayLogs.count { 
                cal.timeInMillis = it.takenTimestamp
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 18..23
            }
            val night = dayLogs.count { 
                cal.timeInMillis = it.takenTimestamp
                cal.get(java.util.Calendar.HOUR_OF_DAY) in 0..5
            }
            
            val maxCount = maxOf(1, morning, afternoon, evening, night)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                ChartBarItem("Утро\n(06-12)", morning, morning.toFloat() / maxCount, modifier = Modifier.weight(1f))
                ChartBarItem("День\n(12-18)", afternoon, afternoon.toFloat() / maxCount, modifier = Modifier.weight(1f))
                ChartBarItem("Вечер\n(18-00)", evening, evening.toFloat() / maxCount, modifier = Modifier.weight(1f))
                ChartBarItem("Ночь\n(00-06)", night, night.toFloat() / maxCount, modifier = Modifier.weight(1f))
            }
        }
        "week" -> {
            val weekLogs = logs.filter { it.takenTimestamp >= now - oneWeekMs }
            val dayCounts = IntArray(7)
            
            for (log in weekLogs) {
                cal.timeInMillis = log.takenTimestamp
                val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
                val index = when (dow) {
                    java.util.Calendar.MONDAY -> 0
                    java.util.Calendar.TUESDAY -> 1
                    java.util.Calendar.WEDNESDAY -> 2
                    java.util.Calendar.THURSDAY -> 3
                    java.util.Calendar.FRIDAY -> 4
                    java.util.Calendar.SATURDAY -> 5
                    java.util.Calendar.SUNDAY -> 6
                    else -> 0
                }
                dayCounts[index]++
            }
            
            val maxCount = maxOf(1, dayCounts.maxOrNull() ?: 1)
            val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 0..6) {
                    ChartBarItem(labels[i], dayCounts[i], dayCounts[i].toFloat() / maxCount, modifier = Modifier.weight(1f))
                }
            }
        }
        "month" -> {
            val monthLogs = logs.filter { it.takenTimestamp >= now - oneMonthMs }
            val weekCounts = IntArray(4)
            for (log in monthLogs) {
                val diffDays = ((now - log.takenTimestamp) / oneDayMs).toInt()
                val index = when {
                    diffDays in 0..6 -> 3 // Week 4 (current)
                    diffDays in 7..13 -> 2 // Week 3
                    diffDays in 14..20 -> 1 // Week 2
                    else -> 0 // Week 1
                }
                weekCounts[index]++
            }
            
            val maxCount = maxOf(1, weekCounts.maxOrNull() ?: 1)
            val labels = listOf("1-7 д. назад", "8-14 д. назад", "15-21 д. назад", "Тек. неделя")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                for (i in 0..3) {
                    ChartBarItem(labels[i], weekCounts[i], weekCounts[i].toFloat() / maxCount, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ChartBarItem(
    label: String,
    value: Int,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (value > 0) "$value" else "",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MintPrimary,
            fontSize = 11.sp
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(progress.coerceIn(0.01f, 1.0f))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(MintPrimary, MintPrimary.copy(alpha = 0.6f))
                        ),
                        shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                    )
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 11.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCabinetScreen(viewModel: MainViewModel) {
    val userFirstName by viewModel.userFirstName.collectAsStateWithLifecycle()
    val userLastName by viewModel.userLastName.collectAsStateWithLifecycle()
    val intakeLogs by viewModel.intakeLogs.collectAsStateWithLifecycle()
    var selectedHistoryTab by remember { mutableStateOf("week") } // day, week, month

    var editingProfile by remember { mutableStateOf(userFirstName.isBlank() && userLastName.isBlank()) }
    var inputFirstName by remember(userFirstName) { mutableStateOf(userFirstName) }
    var inputLastName by remember(userLastName) { mutableStateOf(userLastName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            val displayName = if (userFirstName.isNotBlank()) userFirstName else "Пользователь"
            Text(
                text = "Личный кабинет",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Здравствуйте, $displayName!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = "Профиль",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Данные пользователя",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    if (editingProfile) {
                        OutlinedTextField(
                            value = inputFirstName,
                            onValueChange = { inputFirstName = it },
                            label = { Text("Имя") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_first_name_input")
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = inputLastName,
                            onValueChange = { inputLastName = it },
                            label = { Text("Фамилия") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("profile_last_name_input")
                        )
                        Button(
                            onClick = {
                                viewModel.saveUserProfile(inputFirstName.trim(), inputLastName.trim())
                                editingProfile = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            modifier = Modifier.align(Alignment.End).testTag("profile_save_button")
                        ) {
                            Text("Сохранить")
                        }
                    } else {
                        Column {
                            Text("Имя: ${if (userFirstName.isNotBlank()) userFirstName else "не указано"}", style = MaterialTheme.typography.bodyLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Фамилия: ${if (userLastName.isNotBlank()) userLastName else "не указано"}", style = MaterialTheme.typography.bodyLarge)
                        }
                        TextButton(
                            onClick = { editingProfile = true },
                            modifier = Modifier.align(Alignment.End).testTag("profile_edit_button")
                        ) {
                            Text("Изменить данные", color = MintPrimary)
                        }
                    }
                }
            }
        }

        // --- INTAKE HISTORY SECTION ---
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.TaskAlt,
                        contentDescription = "История",
                        tint = MintPrimary
                    )
                    Text(
                        text = "История и статистика приёма",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText
                    )
                }
                
                if (intakeLogs.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearIntakeHistory() },
                        colors = ButtonDefaults.textButtonColors(contentColor = ColorExpired)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Очистить", fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("day" to "День", "week" to "Неделя", "month" to "Месяц").forEach { (tabId, tabName) ->
                            val isSelected = selectedHistoryTab == tabId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        color = if (isSelected) MintPrimary else Color.Transparent,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { selectedHistoryTab = tabId }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabName,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (intakeLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    tint = MaterialTheme.colorScheme.outline,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "История приёма пуста",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "После зафиксированных приёмов здесь отобразятся графики за выбранный период.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        AdherenceChart(selectedTab = selectedHistoryTab, logs = intakeLogs)
                        
                        val now = System.currentTimeMillis()
                        val filteredLogs = when (selectedHistoryTab) {
                            "day" -> intakeLogs.filter { it.takenTimestamp >= now - (24 * 60 * 60 * 1000L) }
                            "week" -> intakeLogs.filter { it.takenTimestamp >= now - (7 * 24 * 60 * 60 * 1000L) }
                            else -> intakeLogs.filter { it.takenTimestamp >= now - (30 * 24 * 60 * 60 * 1000L) }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Интервал приёма:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Принято доз: ${filteredLogs.size}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MintPrimary
                            )
                        }
                    }
                }
            }
        }

        if (intakeLogs.isNotEmpty()) {
            item {
                Text(
                    text = "Лист последних приёмов (${intakeLogs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MintDarkText,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                )
            }

            val groupedByDay = intakeLogs.groupBy { log ->
                val diffDays = ((System.currentTimeMillis() - log.takenTimestamp) / (24 * 60 * 60 * 1000L)).toInt()
                val logDate = java.util.Date(log.takenTimestamp)
                val currentDate = java.util.Date()
                when {
                    diffDays == 0 && isSameDay(logDate, currentDate) -> "Сегодня"
                    diffDays <= 1 && isSameDay(logDate, java.util.Date(currentDate.time - 24 * 60 * 60 * 1000L)) -> "Вчера"
                    else -> java.text.SimpleDateFormat("dd MMMM, EEEE", java.util.Locale("ru")).format(logDate)
                }
            }

            groupedByDay.forEach { (dateGroupLabel, logsInDay) ->
                item {
                    Text(
                        text = dateGroupLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintPrimary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                items(logsInDay.size) { index ->
                    val log = logsInDay[index]
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = log.medicineName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDarkText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (log.scheduledTime.equals("Вручную", ignoreCase = true)) "Вручную" else "По расписанию (${log.scheduledTime})",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val timeSdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                Text(
                                    text = timeSdf.format(java.util.Date(log.takenTimestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Box(
                                    modifier = Modifier
                                        .background(MintPrimary.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${viewModel.formatDouble(log.dosage)} шт",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MintDarkText
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PillboxScreen(viewModel: MainViewModel) {
    val pillboxEntries by viewModel.pillboxEntries.collectAsStateWithLifecycle()
    val cabinetMedicines by viewModel.medicines.collectAsStateWithLifecycle()
    val pillboxes by viewModel.pillboxes.collectAsStateWithLifecycle()

    // Dialog state for creating a new Pillbox
    var showAddPillboxDialog by remember { mutableStateOf(false) }
    var newPillboxName by remember { mutableStateOf("") }

    // Target pillboxId when adding an entry
    var showAddPillboxEntryDialogForId by remember { mutableStateOf<Int?>(null) }

    // State for renaming a Pillbox
    var renamingPillbox by remember { mutableStateOf<com.example.data.Pillbox?>(null) }
    var renameInputName by remember { mutableStateOf("") }

    val context = LocalContext.current
    var entryEditingTime by remember { mutableStateOf<com.example.data.PillboxEntry?>(null) }
    var groupEditingTimeEntries by remember { mutableStateOf<List<com.example.data.PillboxEntry>?>(null) }

    if (entryEditingTime != null) {
        val entry = entryEditingTime!!
        val parts = entry.preferredTime.split(":")
        val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        androidx.compose.runtime.DisposableEffect(entry) {
            val picker = android.app.TimePickerDialog(
                context,
                { _, hour, minute ->
                    val newTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                    viewModel.updatePillboxEntryTime(entry, newTime)
                    entryEditingTime = null
                },
                initHour,
                initMinute,
                true
            )
            picker.setOnCancelListener {
                entryEditingTime = null
            }
            picker.show()
            
            onDispose {
                picker.dismiss()
            }
        }
    }

    if (groupEditingTimeEntries != null) {
        val entries = groupEditingTimeEntries!!
        val firstEntry = entries.firstOrNull()
        if (firstEntry != null) {
            val parts = firstEntry.preferredTime.split(":")
            val initHour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val initMinute = parts.getOrNull(1)?.toIntOrNull() ?: 0

            androidx.compose.runtime.DisposableEffect(entries) {
                val picker = android.app.TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        val newTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                        viewModel.updatePillboxGroupTime(entries, newTime)
                        groupEditingTimeEntries = null
                    },
                    initHour,
                    initMinute,
                    true
                )
                picker.setOnCancelListener {
                    groupEditingTimeEntries = null
                }
                picker.show()
                
                onDispose {
                    picker.dismiss()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Таблетницы",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Мои Таблетницы",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = {
                        newPillboxName = "Таблетница ${pillboxes.size + 1}"
                        showAddPillboxDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp).testTag("add_new_pillbox_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать таблетницу", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (pillboxes.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Пусто",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "У вас нет активных таблетниц",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Создайте свою первую таблетницу по кнопке выше, чтобы структурировать прием лекарств по категориям или членам семьи.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            pillboxes.forEachIndexed { index, pillbox ->
                val entriesForThisPillbox = pillboxEntries.filter {
                    it.pillboxId == pillbox.id || (index == 0 && it.pillboxId == 0)
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = MintPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = pillbox.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MintDarkText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = {
                                            renamingPillbox = pillbox
                                            renameInputName = pillbox.name
                                        },
                                        modifier = Modifier.size(28.dp).testTag("rename_pillbox_btn_${pillbox.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Переименовать таблетницу",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { showAddPillboxEntryDialogForId = pillbox.id },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("add_entry_to_pillbox_${pillbox.id}")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+ Прием", fontSize = 12.sp, color = MintPrimary)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { viewModel.deletePillbox(pillbox) },
                                        modifier = Modifier.size(32.dp).testTag("delete_pillbox_${pillbox.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Удалить таблетницу",
                                            tint = ColorExpired,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

                            if (entriesForThisPillbox.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "В этой таблетнице нет лекарств.\nНажмите «+ Прием», чтобы добавить.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                val groupedByTime = entriesForThisPillbox.groupBy { it.preferredTime }.toSortedMap()
                                groupedByTime.forEach { (time, entriesInSlot) ->
                                    PillboxTimeGroupCard(
                                        time = time,
                                        entries = entriesInSlot,
                                        onTakeGroup = { viewModel.confirmGroupedPillboxIntake(entriesInSlot) },
                                        onDeleteEntry = { entry -> viewModel.deletePillboxEntry(entry) },
                                        onEditEntryTime = { entry -> entryEditingTime = entry },
                                        onEditGroupTime = { groupEditingTimeEntries = entriesInSlot }
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    if (showAddPillboxDialog) {
        AlertDialog(
            onDismissRequest = { showAddPillboxDialog = false },
            title = { Text("Новая таблетница", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPillboxName,
                    onValueChange = { newPillboxName = it },
                    label = { Text("Название таблетницы") },
                    singleLine = true,
                    placeholder = { Text("например: Утренняя, Для поездки") },
                    modifier = Modifier.fillMaxWidth().testTag("new_pillbox_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPillboxName.isNotBlank()) {
                            viewModel.createPillbox(newPillboxName.trim())
                            showAddPillboxDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    modifier = Modifier.testTag("new_pillbox_save_button")
                ) {
                    Text("Создать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPillboxDialog = false }) {
                    Text("Отмена", color = MintPrimary)
                }
            }
        )
    }

    if (renamingPillbox != null) {
        val currentPillbox = renamingPillbox!!
        AlertDialog(
            onDismissRequest = { renamingPillbox = null },
            title = { Text("Переименовать таблетницу", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInputName,
                    onValueChange = { renameInputName = it },
                    label = { Text("Новое название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_pillbox_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInputName.isNotBlank()) {
                            viewModel.renamePillbox(currentPillbox, renameInputName.trim())
                            renamingPillbox = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    modifier = Modifier.testTag("rename_pillbox_save")
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingPillbox = null }) {
                    Text("Отмена", color = MintPrimary)
                }
            }
        )
    }

    if (showAddPillboxEntryDialogForId != null) {
        val targetPillboxId = showAddPillboxEntryDialogForId!!
        AddPillboxEntryDialog(
            cabinetMedicines = cabinetMedicines,
            onDismiss = { showAddPillboxEntryDialogForId = null },
            onSave = { medicineName, dosage, preferredTime, periodicityDays ->
                viewModel.addPillboxEntry(targetPillboxId, medicineName, dosage, preferredTime, periodicityDays)
                showAddPillboxEntryDialogForId = null
            }
        )
    }
}

@Composable
fun PillboxEntryCard(
    entry: com.example.data.PillboxEntry,
    onTake: () -> Unit,
    onDelete: () -> Unit
) {
    val limitText = when (entry.periodicityDays) {
        1 -> "Каждый день"
        2 -> "Через день"
        else -> "Каждые ${entry.periodicityDays} дня"
    }

    val lastTakenStr = if (entry.lastTakenTimestamp > 0L) {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("ru"))
        "Принято: " + sdf.format(Date(entry.lastTakenTimestamp))
    } else {
        "Приемов еще не было"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.medicineName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$limitText в ${entry.preferredTime}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp).testTag("delete_pillbox_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить расписание",
                        tint = ColorExpired
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Разовая доза: ${if (entry.dosage % 1.0 == 0.0) entry.dosage.toInt().toString() else entry.dosage.toString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = lastTakenStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onTake,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("took_pillbox_${entry.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Я принял",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Я принял", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun PillboxTimeGroupCard(
    time: String,
    entries: List<com.example.data.PillboxEntry>,
    onTakeGroup: () -> Unit,
    onDeleteEntry: (com.example.data.PillboxEntry) -> Unit,
    onEditEntryTime: (com.example.data.PillboxEntry) -> Unit,
    onEditGroupTime: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MintPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "🕗 Время: $time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MintDarkText
                    )
                    IconButton(
                        onClick = onEditGroupTime,
                        modifier = Modifier.size(28.dp).testTag("edit_pillbox_group_time_$time")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Изменить время группы",
                            tint = MintPrimary.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                Button(
                    onClick = onTakeGroup,
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("took_pillbox_group_$time")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Я принял всё",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Я принял", style = MaterialTheme.typography.labelLarge)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            entries.forEachIndexed { idx, entry ->
                val limitText = when (entry.periodicityDays) {
                    1 -> "Каждый день"
                    2 -> "Через день"
                    else -> "Каждые ${entry.periodicityDays} дня"
                }

                val lastTakenStr = if (entry.lastTakenTimestamp > 0L) {
                    val sdf = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale("ru"))
                    "Принято: " + sdf.format(java.util.Date(entry.lastTakenTimestamp))
                } else {
                    "Приемов еще не было"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.medicineName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Доза: ${if (entry.dosage % 1.0 == 0.0) entry.dosage.toInt().toString() else entry.dosage.toString()}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• $limitText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = lastTakenStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { onEditEntryTime(entry) },
                            modifier = Modifier.size(36.dp).testTag("edit_pillbox_entry_time_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Изменить время приема",
                                tint = MintPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { onDeleteEntry(entry) },
                            modifier = Modifier.size(36.dp).testTag("delete_pillbox_entry_${entry.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Удалить из расписания",
                                tint = ColorExpired,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                if (idx < entries.size - 1) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPillboxEntryDialog(
    cabinetMedicines: List<com.example.data.Medicine>,
    onDismiss: () -> Unit,
    onSave: (medicineName: String, dosage: Double, preferredTime: String, periodicityDays: Int) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedMedicineName by remember { mutableStateOf("") }
    
    var dosageStr by remember { mutableStateOf("1.0") }
    var preferredTime by remember { mutableStateOf("08:00") }
    var periodicityDays by remember { mutableStateOf(1) }

    val (initHour, initMinute) = remember(preferredTime) {
        val parts = preferredTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        h to m
    }

    val timePickerDialog = remember {
        android.app.TimePickerDialog(
            context,
            { _, hour, minute ->
                preferredTime = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
            },
            initHour,
            initMinute,
            true
        )
    }

    val availableSorted = remember(cabinetMedicines, searchQuery) {
        cabinetMedicines
            .filter { it.remainingCount > 0.0 }
            .filter {
                searchQuery.isBlank() ||
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(compareBy<com.example.data.Medicine> { it.name.lowercase() }.thenBy { it.expirationTimestamp })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Новый курс приема", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedMedicineName.isEmpty()) {
                    Text(
                        "Выберите лекарство из вашей аптечки:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Поиск по названию или тегам...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MintPrimary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("pillbox_search_dialog")
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (availableSorted.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Лекарства не найдены.\nДобавьте их в аптечку сначала.",
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.heightIn(max = 280.dp)
                        ) {
                            availableSorted.forEach { med ->
                                val statusLabel = if (med.expirationTimestamp <= 1779981846000L) "Истёк!" else "Годен"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedMedicineName = med.name },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(med.name, fontWeight = FontWeight.Bold, color = MintDarkText)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                "Годен до: ${med.expirationDate}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (statusLabel == "Истёк!") ColorExpired else ColorValid,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Остаток: ${med.remainingCount} шт",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (med.tags.isNotEmpty()) {
                                            Text(
                                                "Теги: ${med.tags}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MintPrimary.copy(alpha = 0.08f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MintPrimary.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Выбрано: $selectedMedicineName",
                            fontWeight = FontWeight.ExtraBold,
                            color = MintDarkText,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { selectedMedicineName = "" },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("change_medicine_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Изменить",
                                tint = ColorExpired,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Изменить",
                                color = ColorExpired,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    OutlinedTextField(
                        value = dosageStr,
                        onValueChange = { dosageStr = it },
                        label = { Text("Разовая доза (шт. / таблеток)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth().testTag("pillbox_dosage_input")
                    )

                    Text(
                        "Время приема:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .clickable { timePickerDialog.show() }
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .testTag("pillbox_time_picker_trigger"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MintPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Выбранное время",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = preferredTime,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MintDarkText
                                )
                            }
                        }
                        Button(
                            onClick = { timePickerDialog.show() },
                            colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("Выбрать", style = MaterialTheme.typography.labelMedium)
                        }
                    }

                    Text(
                        "Периодичность приема:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val cycles = listOf(
                            1 to "Каждый день",
                            2 to "Через день",
                            3 to "Раз в 3 дня"
                        )
                        cycles.forEach { (days, label) ->
                            val isSel = periodicityDays == days
                            Button(
                                onClick = { periodicityDays = days },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSel) MintPrimary else MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = if (isSel) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (selectedMedicineName.isNotEmpty()) {
                Button(
                    onClick = {
                        val dosage = dosageStr.toDoubleOrNull() ?: 1.0
                        onSave(selectedMedicineName, dosage, preferredTime.trim(), periodicityDays)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintPrimary),
                    modifier = Modifier.testTag("pillbox_save_confirm")
                ) {
                    Text("Создать курс")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = MintPrimary)
            }
        }
    )
}
