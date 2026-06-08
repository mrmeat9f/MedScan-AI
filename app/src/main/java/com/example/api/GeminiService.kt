package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

// --- Moshi Data Classes for Gemini Request/Response ---
@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val parts: List<GeminiPart>,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null,
    val error: GeminiErrorDetails? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiErrorDetails(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)
// --- End of Request/Response Classes ---

// --- Moshi Data Classes for Grok Request/Response ---
@JsonClass(generateAdapter = true)
data class GrokPart(
    val type: String,
    val text: String? = null,
    val image_url: GrokImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class GrokImageUrl(
    val url: String
)

@JsonClass(generateAdapter = true)
data class GrokMessage(
    val role: String,
    val content: List<GrokPart>
)

@JsonClass(generateAdapter = true)
data class GrokResponseFormat(
    val type: String
)

@JsonClass(generateAdapter = true)
data class GrokRequest(
    val model: String,
    val messages: List<GrokMessage>,
    val temperature: Float? = null,
    val response_format: GrokResponseFormat? = null
)

@JsonClass(generateAdapter = true)
data class GrokResponse(
    val choices: List<GrokChoice>? = null,
    val error: GrokErrorDetails? = null
)

@JsonClass(generateAdapter = true)
data class GrokChoice(
    val message: GrokMessageDetails? = null
)

@JsonClass(generateAdapter = true)
data class GrokMessageDetails(
    val content: String? = null
)

@JsonClass(generateAdapter = true)
data class GrokErrorDetails(
    val message: String? = null,
    val type: String? = null,
    val code: String? = null
)
// --- End of Grok Classes ---

// --- Parsed Data Classes ---

@JsonClass(generateAdapter = true)
data class ParsedZnakResult(
    val name: String,
    val gtin: String,
    val serial: String,
    val expiration_date: String, // YYYY-MM-DD
    val batch: String,
    val success: Boolean,
    val package_count: Int? = 30,
    val tags: List<String>? = emptyList()
)

@JsonClass(generateAdapter = true)
data class AnalogRecommendResult(
    val origin_name: String,
    val description: String,
    val active_substance: String,
    val analogs: List<MedicineAnalog>
)

@JsonClass(generateAdapter = true)
data class MedicineAnalog(
    val name: String,
    val manufacturer: String,
    val price_category: String, // "Доступная", "Средняя", "Высокая"
    val notes: String
)

@JsonClass(generateAdapter = true)
data class DrugInstruction(
    val name: String,
    val description: String,
    val active_substance: String,
    val composition: String = "",        // Состав препарата
    val indications: String,             // Показания к применению
    val dosage: String,                  // Способ применения и дозы
    val contraindications: String,       // Противопоказания
    val side_effects: String,            // Побочные эффекты
    val special_instructions: String,     // Особые указания
    val storage_conditions: String = "",  // Условия хранения
    val interaction: String = ""         // Взаимодействие с другими препаратами
)

object GeminiService {
    private const val TAG = "GeminiService"

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private var lastFallbackIndex = 0
    private val fallbackMedicines = listOf(
        "Энтерофурил",
        "Нурофен",
        "Но-Шпа",
        "Ксарелто",
        "Парацетамол",
        "Аспирин"
    )

    private suspend fun <T> retryOnTransientErrors(
        maxAttempts: Int = 3,
        initialDelayMs: Long = 1000,
        block: suspend () -> T
    ): T {
        var currentDelayMs = initialDelayMs
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                val msg = e.localizedMessage ?: ""
                if (msg.contains("403") || msg.contains("400") || msg.contains("API key not valid") || msg.contains("Forbidden") || msg.contains("INVALID_ARGUMENT")) {
                    throw e
                }
                if (attempt < maxAttempts) {
                    val waitTime = currentDelayMs * attempt
                    Log.w(TAG, "Transient error detected on attempt $attempt/$maxAttempts. Retrying in ${waitTime}ms...", e)
                    kotlinx.coroutines.delay(waitTime)
                    continue
                }
                throw e
            }
        }
        throw lastException ?: Exception("Unknown transient error calling Gemini API")
    }

    private fun getNextFallbackName(): String {
        synchronized(this) {
            val name = fallbackMedicines[lastFallbackIndex]
            lastFallbackIndex = (lastFallbackIndex + 1) % fallbackMedicines.size
            return name
        }
    }

    var selectedAiProvider: String = "gemini"
    var appApiKey: String = ""
    var appBaseUrl: String = ""

    var appGrokApiKey: String = ""
    var appGrokBaseUrl: String = "https://api.x.ai/v1"
    var appGrokModel: String = "grok-2-latest"

    val activeBaseUrl: String
        get() = appBaseUrl.ifEmpty { "https://generativelanguage.googleapis.com" }.removeSuffix("/")

    val activeApiKey: String
        get() = appApiKey.ifEmpty { BuildConfig.GEMINI_API_KEY }

    val activeGrokApiKey: String
        get() = appGrokApiKey.ifEmpty {
            try {
                BuildConfig.GROK_API_KEY
            } catch (e: Exception) {
                ""
            }
        }

    val isApiKeyAvailable: Boolean
        get() = try {
            activeApiKey.isNotEmpty() && activeApiKey != "MY_GEMINI_API_KEY"
        } catch (e: Exception) {
            false
        }

    val isGrokApiKeyAvailable: Boolean
        get() = activeGrokApiKey.isNotEmpty() && activeGrokApiKey != "MY_GROK_API_KEY"

    fun getApiKeyStatusDescription(): String {
        val providerStr = if (selectedAiProvider == "gemini") "Gemini API" else "Grok API"
        val geminiKey = activeApiKey
        val geminiStatus = if (geminiKey.isEmpty()) {
            "Ключ Gemini отсутствует"
        } else if (geminiKey == "MY_GEMINI_API_KEY") {
            "Встроенный плейсхолдер Gemini ('MY_GEMINI_API_KEY'). Запросы могут блокироваться Google из РФ/РБ."
        } else {
            val isCustom = appApiKey.isNotEmpty()
            val typeStr = if (isCustom) "Пользовательский (вручную)" else "Встроенный (AI Studio Secrets)"
            val masked = if (geminiKey.length > 8) "${geminiKey.take(6)}...${geminiKey.takeLast(4)}" else "***"
            "$typeStr (маска: $masked)"
        }

        val grokKey = activeGrokApiKey
        val grokStatus = if (grokKey.isEmpty()) {
            "Ключ Grok отсутствует"
        } else if (grokKey == "MY_GROK_API_KEY") {
            "Встроенный плейсхолдер Grok ('MY_GROK_API_KEY')"
        } else {
            val isCustom = appGrokApiKey.isNotEmpty()
            val typeStr = if (isCustom) "Пользовательский (вручную)" else "Встроенный (Семейство ключей)"
            val masked = if (grokKey.length > 8) "${grokKey.take(6)}...${grokKey.takeLast(4)}" else "***"
            "$typeStr (маска: $masked)"
        }

        return "Выбранный ИИ по умолчанию: $providerStr\n\n• Gemini: $geminiStatus\n• Grok: $grokStatus"
    }

    private suspend fun makeGeminiApiCall(
        modelName: String,
        prompt: String,
        base64Image: String? = null,
        responseJson: Boolean = false,
        temperature: Float = 0.2f
    ): String = withContext(Dispatchers.IO) {
        val key = activeApiKey
        if (key.isEmpty()) {
            throw Exception("Ключ API не настроен. Пожалуйста, укажите рабочий ключ в настройках.")
        }

        val parts = mutableListOf<GeminiPart>()
        if (!base64Image.isNullOrEmpty()) {
            val cleanBase64 = base64Image.substringAfter("base64,")
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = cleanBase64)))
        }
        parts.add(GeminiPart(text = prompt))

        val geminiRequest = GeminiRequest(
            contents = listOf(GeminiContent(parts = parts)),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = if (responseJson) "application/json" else null,
                temperature = temperature
            )
        )

        val requestJson = moshi.adapter(GeminiRequest::class.java).toJson(geminiRequest)

        val cleanBaseUrl = activeBaseUrl.removeSuffix("/")
        val finalUrl = if (cleanBaseUrl.contains("models/")) {
            "$cleanBaseUrl:generateContent?key=$key"
        } else if (cleanBaseUrl.contains("/v1") || cleanBaseUrl.contains("/v1beta")) {
            "$cleanBaseUrl/models/$modelName:generateContent?key=$key"
        } else {
            "$cleanBaseUrl/v1beta/models/$modelName:generateContent?key=$key"
        }

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

         val requestBody = requestJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
 
         val httpRequest = okhttp3.Request.Builder()
             .url(finalUrl)
             .post(requestBody)
             .header("Content-Type", "application/json")
             .build()
 
         client.newCall(httpRequest).execute().use { response ->
             val code = response.code
             val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val geminiResponse = try {
                    moshi.adapter(GeminiResponse::class.java).fromJson(bodyString)
                } catch (e: Exception) {
                    throw Exception("Не удалось распарсить ответ сервера: ${e.localizedMessage}")
                }
                val textResult = geminiResponse?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (textResult != null) {
                    return@withContext textResult
                } else {
                    throw Exception("В ответе сервера отсутствуют текстовые кандидаты.")
                }
            } else {
                var errMsg = try {
                    val errorContainer = moshi.adapter(GeminiResponse::class.java).fromJson(bodyString)
                    errorContainer?.error?.message ?: "Код ошибки: $code"
                } catch (e: Exception) {
                    "Код ошибки: $code. Ответ сервера: $bodyString"
                }

                if (errMsg.contains("leaked", ignoreCase = true)) {
                    errMsg = "Ваш API-ключ Gemini был заблокирован Google из-за утечки в сеть (API key reported as leaked).\n\n" +
                            "Как исправить:\n" +
                            "1. Перейдите во вкладку «Настройки» (иконка шестеренки справа внизу) в раздел «Настройка ИИ».\n" +
                            "2. Получите новый личный API-ключ в Google AI Studio и вставьте его в поле «Личный API-ключ Gemini».\n" +
                            "3. Также вы можете настроить прокси/зеркало ниже этой строки, если у вас заблокирован прямой доступ."
                }
                throw Exception(errMsg)
            }
        }
    }

    private suspend fun makeGrokApiCall(
        modelName: String,
        prompt: String,
        base64Image: String? = null,
        responseJson: Boolean = false,
        temperature: Float = 0.2f
    ): String = withContext(Dispatchers.IO) {
        val key = activeGrokApiKey
        if (key.isEmpty()) {
            throw Exception("Ключ API Grok не настроен. Пожалуйста, укажите рабочий ключ в настройках.")
        }

        val parts = mutableListOf<GrokPart>()
        parts.add(GrokPart(type = "text", text = prompt))
        if (!base64Image.isNullOrEmpty()) {
            val cleanBase64 = base64Image.substringAfter("base64,")
            parts.add(GrokPart(type = "image_url", image_url = GrokImageUrl(url = "data:image/jpeg;base64,$cleanBase64")))
        }

        val grokRequest = GrokRequest(
            model = modelName,
            messages = listOf(GrokMessage(role = "user", content = parts)),
            temperature = temperature,
            response_format = if (responseJson) GrokResponseFormat(type = "json_object") else null
        )

        val requestJson = moshi.adapter(GrokRequest::class.java).toJson(grokRequest)

        val cleanBaseUrl = appGrokBaseUrl.removeSuffix("/")
        val finalUrl = if (cleanBaseUrl.contains("chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val client = okhttp3.OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()

        val requestBody = requestJson.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val httpRequest = okhttp3.Request.Builder()
            .url(finalUrl)
            .post(requestBody)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val code = response.code
            val bodyString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val grokResponse = try {
                    moshi.adapter(GrokResponse::class.java).fromJson(bodyString)
                } catch (e: Exception) {
                    throw Exception("Не удалось распарсить ответ Grok: ${e.localizedMessage}")
                }
                val textResult = grokResponse?.choices?.firstOrNull()?.message?.content
                if (textResult != null) {
                    return@withContext textResult
                } else {
                    throw Exception("В ответе Grok отсутствуют текстовые кандидаты.")
                }
            } else {
                val errMsg = try {
                    val errorContainer = moshi.adapter(GrokResponse::class.java).fromJson(bodyString)
                    errorContainer?.error?.message ?: "Код ошибки Grok: $code"
                } catch (e: Exception) {
                    "Код ошибки Grok: $code. Ответ сервера: $bodyString"
                }
                throw Exception(errMsg)
            }
        }
    }

    private suspend fun makeUnifiedApiCall(
        geminiModelName: String,
        grokModelName: String,
        prompt: String,
        base64Image: String? = null,
        responseJson: Boolean = false,
        temperature: Float = 0.2f
    ): String = withContext(Dispatchers.IO) {
        val primaryProvider = selectedAiProvider
        val fallbackProvider = if (primaryProvider == "gemini") "grok" else "gemini"

        var lastError: Exception? = null

        // Try primary provider
        try {
            Log.d(TAG, "Trying primary AI provider: $primaryProvider")
            if (primaryProvider == "grok") {
                return@withContext makeGrokApiCall(grokModelName, prompt, base64Image, responseJson, temperature)
            } else {
                return@withContext makeGeminiApiCall(geminiModelName, prompt, base64Image, responseJson, temperature)
            }
        } catch (e: Exception) {
            lastError = e
            Log.e(TAG, "Primary AI provider ($primaryProvider) failed: ${e.localizedMessage}. Attempting failover to $fallbackProvider...", e)
        }

        // Try fallback provider
        try {
            if (fallbackProvider == "grok") {
                if (activeGrokApiKey.isNotEmpty()) {
                    Log.d(TAG, "Failover: calling Grok API...")
                    return@withContext makeGrokApiCall(grokModelName, prompt, base64Image, responseJson, temperature)
                } else {
                    Log.w(TAG, "Failover to Grok requested but no Grok API Key is available.")
                }
            } else {
                if (isApiKeyAvailable) {
                    Log.d(TAG, "Failover: calling Gemini API...")
                    return@withContext makeGeminiApiCall(geminiModelName, prompt, base64Image, responseJson, temperature)
                } else {
                    Log.w(TAG, "Failover to Gemini requested but no Gemini API Key is available.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback AI provider ($fallbackProvider) also failed: ${e.localizedMessage}")
            throw Exception("Ни один ИИ не ответил успешно.\n\n" +
                    "Главный ИИ ($primaryProvider) вернул её ошибку:\n${lastError?.localizedMessage}\n\n" +
                    "Резервный ИИ ($fallbackProvider) вернул:\n${e.localizedMessage}")
        }

        throw lastError ?: Exception("Вызов ИИ завершился ошибкой.")
    }

    /**
     * Tests the connection to the Gemini API using the currently configured key and endpoint.
     * Returns a friendly result string.
     */
    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        if (selectedAiProvider == "grok" && !isGrokApiKeyAvailable) {
            return@withContext "Ключ API Grok не настроен. Пожалуйста, укажите рабочий ключ в настройках."
        }
        if (selectedAiProvider == "gemini" && !isApiKeyAvailable) {
            return@withContext "Ключ API Gemini не настроен. Пожалуйста, укажите рабочий ключ в настройках."
        }
        try {
            val response = retryOnTransientErrors {
                makeUnifiedApiCall(
                    geminiModelName = "gemini-3.5-flash",
                    grokModelName = appGrokModel.ifEmpty { "grok-2-latest" },
                    prompt = "Respond only with the word OK.",
                    temperature = 0.1f
                )
            }
            if (response.isNotEmpty()) {
                "Успешное подключение! ИИ ответил: ${response.trim()}"
            } else {
                "Получен пустой ответ от ИИ."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Test connection failed", e)
            val msg = e.localizedMessage ?: "Неизвестная ошибка"
            if (msg.contains("403") || msg.contains("Forbidden")) {
                "Ошибка 403 Forbidden: Доступ заблокирован. Скорее всего, ваш IP-адрес находится в заблокированном регионе (например, РФ/РБ). Пожалуйста, включите качественный VPN или настройте рабочее зеркало/прокси."
            } else if (msg.contains("400") || msg.contains("API key not valid") || msg.contains("INVALID_ARGUMENT")) {
                "Ошибка 400 Bad Request: Неверный запрос или модель. Возможно, ваш API-ключ недействителен."
            } else if (msg.contains("429")) {
                "Ошибка 429 Too Many Requests: Лимит запросов превышен. Пожалуйста, подождите."
            } else {
                msg
            }
        }
    }

    /**
     * Parses medicine упаковка (photo) using multimodal Gemini/Grok API or offline fallback.
     */
    suspend fun parsePackagePhoto(base64Image: String): ParsedZnakResult = withContext(Dispatchers.IO) {
        // Real image scanning intent
        if (base64Image.isEmpty()) {
            throw Exception("Не удалось захватить снимок с камеры. Пожалуйста, сделайте ещё одно фото.")
        }

        if (selectedAiProvider == "grok" && !isGrokApiKeyAvailable) {
            throw Exception("Ключ API Grok не настроен. Пожалуйста, укажите рабочий ключ в настройках.")
        }
        if (selectedAiProvider == "gemini" && !isApiKeyAvailable) {
            throw Exception("Ключ API Gemini не настроен. Пожалуйста, укажите рабочий ключ в настройках.")
        }

        val prompt = """
            Вы — эксперт по распознаванию медицинских упаковок (лекарств) на русском языке.
            Перед вами фотография упаковки лекарства. Решите следующие задачи:
            1. Распознайте точное коммерческое наименование препарата на русском языке (например: "Энтерофурил капсулы 200мг", "Нурофен Форте таблетки 400мг", "Но-Шпа").
            2. Внимательно найдите СРОК ГОДНОСТИ на пачке (может быть вдавлен, напечатан сбоку, с торца или на обороте. Ищите форматы: 07 2027, 31.07.2027, EXP 07/2027, Годен до: 31.07.2027). Если нашли срок годности, верните его в формате YYYY-MM-DD. Если указан только месяц и год (например, 07 2027), преобразуйте в последний день этого месяца (2027-07-31).
            3. Если срок годности на фото размыт, отсутствует или не виден, верните реалистичный будущий срок годности, например "2027-12-31".
            4. Найдите серию (серия/Batch), если она видна. Если нет, оставьте пустой.
            5. Попробуйте распознать количество таблеток, капсул или ампул в упаковке (например, 16, 20, 28, 50, 100) и верните целое число в ключе "package_count". Если не определено, верните 0.
            6. Тщательно проанализируйте назначение этого лекарства и определите подходящие терапевтические категории / теги на русском языке (например: "Обезболивающее", "Жаропонижающее", "Спазмолитик", "Антибиотик", "Противовирусное", "Кишечное", "Витамины", "Сердечное", "Антикоагулянт", "Глазное", "Мазь/Крем", "Простуда" и т.п.). Верните список этих категорий в массиве "tags". Обычно достаточно 1-3 самых точных тегов.
            
            Верните результат СТРОГО в виде компактного JSON объекта со следующими ключами:
            {
              "name": "Название лекарства на русском",
              "gtin": "14-значный штрихкод, если виден, иначе пустая строка",
              "serial": "Сгенерированный случайный серийный номер из 13 символов",
              "expiration_date": "YYYY-MM-DD",
              "batch": "Номер серии лекарства или пустая строка",
              "success": true,
              "package_count": 30,
              "tags": ["Обезболивающее", "Жаропонижающее"]
            }
        """.trimIndent()

        try {
            val responseText = retryOnTransientErrors {
                makeUnifiedApiCall(
                    geminiModelName = "gemini-3.5-flash",
                    grokModelName = appGrokModel.ifEmpty { "grok-2-latest" },
                    prompt = prompt,
                    base64Image = base64Image,
                    responseJson = true,
                    temperature = 0.1f
                )
            }
            if (responseText.isNotEmpty()) {
                val cleanJson = responseText
                    .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*```$", RegexOption.IGNORE_CASE), "")
                    .trim()

                val adapter = moshi.adapter(ParsedZnakResult::class.java)
                adapter.fromJson(cleanJson) ?: throw Exception("Не удалось расшифровать структуру ответа нейросети.")
            } else {
                throw Exception("Нейросеть вернула пустой ответ.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error photo parsing via Gemini/Grok", e)
            val friendlyMsg = when {
                e.localizedMessage?.contains("429") == true ->
                    "Превышен лимит запросов к ИИ (Ошибка 429). Пожалуйста, повторите попытку через 15-30 секунд или воспользуйтесь ручным вводом."
                e.localizedMessage?.contains("503") == true ->
                    "Сервисы ИИ временно недоступны (Ошибка 503). Пожалуйста, попробуйте сфотографировать ещё раз через пару секунд."
                e.localizedMessage?.contains("403") == true ->
                    "Ошибка доступа (Код 403 Forbidden). Скорее всего, ваш API-ключ недействителен, либо доступ заблокирован по региону (пожалуйста, используйте качественный VPN/прокси)."
                else -> e.localizedMessage ?: "ошибка сети или неверный API-ключ."
            }
            throw Exception(friendlyMsg)
        }
    }

    /**
     * Looks up therapeutic substitutes.
     */
    suspend fun searchAnalogs(medicineName: String): AnalogRecommendResult = withContext(Dispatchers.IO) {
        val isProviderAvailable = if (selectedAiProvider == "grok") isGrokApiKeyAvailable else isApiKeyAvailable
        if (!isProviderAvailable) {
            val providerName = if (selectedAiProvider == "grok") "Grok" else "Gemini"
            throw Exception("Ошибка: Ключ API для $providerName не настроен. Пожалуйста, укажите рабочий ключ API в настройках профиля.")
        }

        val prompt = """
            You are a Russian medical systems expert. The user is searching for therapeutic drug analogs for: "$medicineName".
            Provide:
            1. Brief description of the drug in Russian.
            2. Active medicinal substance (Действующее вещество) name in Russian.
            3. A list of 3 Russian analogs (дженерики / аналоги) with name, manufacturer, price tier ("Доступная", "Средняя", "Высокая"), and professional clinical comparison notes in Russian.
            
            Return raw, strict JSON only.
        """.trimIndent()

        try {
            val responseText = retryOnTransientErrors {
                makeUnifiedApiCall(
                    geminiModelName = "gemini-3.5-flash",
                    grokModelName = appGrokModel.ifEmpty { "grok-2-latest" },
                    prompt = prompt,
                    responseJson = true,
                    temperature = 0.2f
                )
            }
            if (responseText.isNotEmpty()) {
                val cleanJson = responseText
                    .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*```$", RegexOption.IGNORE_CASE), "")
                    .trim()

                val adapter = moshi.adapter(AnalogRecommendResult::class.java)
                adapter.fromJson(cleanJson) ?: throw Exception("Не удалось разобрать данные аналогов от ИИ.")
            } else {
                throw Exception("Получен пустой ответ от ИИ.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error looking up analogs", e)
            throw Exception("Не удалось получить информацию об аналогах: ${e.localizedMessage ?: e.message}")
        }
    }

    // --- High-Fidelity Offline Fallback Core ---

    fun parseOfflinePhoto(queryName: String): ParsedZnakResult {
        val normalized = queryName.lowercase().trim()

        var name = "Энтерофурил капсулы 200мг"
        var gtin = "4607027768563"
        var serial = "EF" + (10000000000L..99999999999L).random().toString()
        var expDate = "2027-07-31" // 31 июля 2027
        var batch = "EF1024"
        var packageCount = 16
        var tags = listOf("Кишечное", "Противодиарейное", "Антибиотик/Антисептик")

        if (normalized.contains("нурофен")) {
            name = "Нурофен форте 400мг таб."
            gtin = "4601234567111"
            serial = "NR" + (10000000000L..99999999999L).random().toString()
            expDate = "2027-08-15"
            batch = "B5544A"
            packageCount = 12
            tags = listOf("Обезболивающее", "Жаропонижающее", "Противовоспалительное")
        } else if (normalized.contains("но-шп") || normalized.contains("ношп")) {
            name = "Но-Шпа таб. 40мг N100"
            gtin = "4601234567222"
            serial = "NS" + (10000000000L..99999999999L).random().toString()
            expDate = "2026-11-20"
            batch = "N4411C"
            packageCount = 100
            tags = listOf("Спазмолитик", "Обезболивающее")
        } else if (normalized.contains("аспирин")) {
            name = "Аспирин Кардио 100мг таб. N28"
            gtin = "4601234567333"
            serial = "AC" + (10000000000L..99999999999L).random().toString()
            expDate = "2025-05-30" // Already expired
            batch = "A1564W"
            packageCount = 28
            tags = listOf("Разжижение крови", "Сердечное", "Жаропонижающее")
        } else if (normalized.contains("ксарелт")) {
            name = "Ксарелто таб. 20мг N28"
            gtin = "4601234567444"
            serial = "XS" + (10000000000L..99999999999L).random().toString()
            expDate = "2028-02-12"
            batch = "X9900F"
            packageCount = 28
            tags = listOf("Антикоагулянт", "Разжижение крови")
        } else if (normalized.contains("парацетамол")) {
            name = "Парацетамол таб. 500мг N20"
            gtin = "4601234567555"
            serial = "PC" + (10000000000L..99999999999L).random().toString()
            expDate = "2026-06-15"
            batch = "P1020A"
            packageCount = 20
            tags = listOf("Жаропонижающее", "Обезболивающее")
        } else if (normalized.contains("энтерофурил")) {
            name = "Энтерофурил капсулы 200мг"
            gtin = "4607027768563"
            serial = "EF" + (10000000000L..99999999999L).random().toString()
            expDate = "2027-07-31" // 31 июля 2027
            batch = "EF1024"
            packageCount = 16
            tags = listOf("Кишечное", "Противодиарейное", "Антибиотик/Антисептик")
        } else if (queryName.isNotBlank() && queryName != "Новый препарат") {
            name = queryName.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
            gtin = ""
            serial = "MC" + (100000..999999).random().toString()
            expDate = "2027-12-31"
            batch = "S" + (1000..9999).random().toString()
            packageCount = 30
            tags = when {
                normalized.contains("анальгин") || normalized.contains("кето") || normalized.contains("миг") || normalized.contains("пенталгин") -> listOf("Обезболивающее")
                normalized.contains("антибиотик") || normalized.contains("амокси") -> listOf("Антибиотик")
                normalized.contains("витамин") || normalized.contains("компливит") -> listOf("Витамины")
                normalized.contains("сердце") || normalized.contains("корвал") || normalized.contains("валидол") -> listOf("Сердечное")
                normalized.contains("давлен") || normalized.contains("капотен") -> listOf("От давления")
                else -> listOf("Другое")
            }
        }

        return ParsedZnakResult(
            name = name,
            gtin = gtin,
            serial = serial,
            expiration_date = expDate,
            batch = batch,
            success = true,
            package_count = packageCount,
            tags = tags
        )
    }

    private val OFFLINE_DB = listOf(
        AnalogRecommendResult(
            origin_name = "Но-Шпа",
            description = "Спазмолитическое средство, снижающее тонус и спазм гладкой мускулатуры внутренних органов.",
            active_substance = "Дротаверин",
            analogs = listOf(
                MedicineAnalog("Дротаверин гидрохлорид", "Озон Фарм, Россия", "Доступная", "Прямой дженерик с тем же активным веществом. Стоит значительно дешевле при высокой эффективности."),
                MedicineAnalog("Спазмол", "Фармстандарт, Россия", "Доступная", "Отечественный спазмолитик на основе дротаверина высокого уровня очистки."),
                MedicineAnalog("Дюспаталин", "Abbott, Нидерланды", "Высокая", "Качественный спазмолитик на основе мебеверина, действует целенаправленно на спазмы ЖКТ.")
            )
        ),
        AnalogRecommendResult(
            origin_name = "Нурофен",
            description = "Симптоматическое противовоспалительное, жаропонижающее и анальгетическое средство группы НПВП.",
            active_substance = "Ибупрофен",
            analogs = listOf(
                MedicineAnalog("Ибупрофен форте", "Акрихин, Россия", "Доступная", "Отечественный ибупрофен высокой степени эквивалентности. Экономичное решение."),
                MedicineAnalog("МИГ 400", "Berlin-Chemie, Германия", "Средняя", "Немецкий препарат ибупрофена в дозировке 400 мг быстрого высвобождения."),
                MedicineAnalog("Парацетамол-УБФ", "Уралбиофарм, Россия", "Доступная", "Альтернативное жаропонижающее средство (парацетамол) при индивидуальной непереносимости.")
            )
        )
    )

    fun getLocalSimulatedAnalogs(medicineName: String): AnalogRecommendResult {
        val normalized = medicineName.lowercase().trim()
        val match = OFFLINE_DB.find { normalized.contains(it.origin_name.lowercase()) || it.origin_name.lowercase().contains(normalized) }
        if (match != null) {
            return match
        }

        // Generate high quality fallback equivalents
        return AnalogRecommendResult(
            origin_name = medicineName,
            description = "Лекарственный препарат для симптоматической терапии. Снижает проявление симптомов заболевания.",
            active_substance = "Терапевтический аналог",
            analogs = listOf(
                MedicineAnalog(
                    name = "$medicineName-Мекс",
                    manufacturer = "Озон Фарм, Россия",
                    price_category = "Доступная",
                    notes = "Доступный отечественный аналог с тем же действующим веществом. Прекрасное соотношение цена/качество."
                ),
                MedicineAnalog(
                    name = "${medicineName.take(5)}актив",
                    manufacturer = "Фармстандарт, Россия",
                    price_category = "Средняя",
                    notes = "Отечественный препарат, эффективная альтернатива оригиналу."
                )
            )
        )
    }

    suspend fun fetchMedicineInstruction(medicineName: String): DrugInstruction = withContext(Dispatchers.IO) {
        val isProviderAvailable = if (selectedAiProvider == "grok") isGrokApiKeyAvailable else isApiKeyAvailable
        if (!isProviderAvailable) {
            val providerName = if (selectedAiProvider == "grok") "Grok" else "Gemini"
            throw Exception("Ошибка: Ключ API для $providerName не настроен. Пожалуйста, укажите рабочий ключ API в настройках профиля.")
        }

        val prompt = """
            You are a professional Russian pharmacist and clinical medicine expert.
            The user wants the official, detailed medical instruction (инструкция по применению) for: "$medicineName".
            Provide accurate details in Russian:
            1. Real commercial name of the drug (name).
            2. High-level clinical description of what it does (description).
            3. Active pharmaceutical substance (действующее вещество) name in Russian (active_substance).
            4. Detailed composition (химический состав препарата) list (composition).
            5. Indications (показания к применению) (indications).
            6. Precise dosage instructions (способ применения и дозы) (dosage).
            7. Absolute and relative contraindications (противопоказания) (contraindications).
            8. Known side effects (побочные эффекты) (side_effects).
            9. Special instructions (особые указания), precautions, driver warnings (special_instructions).
            10. Storage conditions (условия хранения, температура, влажность) (storage_conditions).
            11. Drug interactions (взаимодействие с другими лекарственными препаратами или пищей/алкоголем) (interaction).
            
            Return raw, strict JSON only conforming to the schema of DrugInstruction.
        """.trimIndent()

        try {
            val responseText = retryOnTransientErrors {
                makeUnifiedApiCall(
                    geminiModelName = "gemini-3.5-flash",
                    grokModelName = appGrokModel.ifEmpty { "grok-2-latest" },
                    prompt = prompt,
                    responseJson = true,
                    temperature = 0.2f
                )
            }
            if (responseText.isNotEmpty()) {
                val cleanJson = responseText
                    .replace(Regex("^```json\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("^```\\s*", RegexOption.IGNORE_CASE), "")
                    .replace(Regex("\\s*```$", RegexOption.IGNORE_CASE), "")
                    .trim()

                val adapter = moshi.adapter(DrugInstruction::class.java)
                adapter.fromJson(cleanJson) ?: throw Exception("Не удалось разобрать данные инструкции от ИИ.")
            } else {
                throw Exception("Получен пустой ответ от ИИ.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching instruction", e)
            throw Exception("Не удалось загрузить инструкцию препарата от ИИ: ${e.localizedMessage ?: e.message}")
        }
    }

    fun getLocalSimulatedInstruction(medicineName: String): DrugInstruction {
        val normalized = medicineName.lowercase().trim()

        if (normalized.contains("нурофен")) {
            return DrugInstruction(
                name = "Нурофен Форте 400мг",
                description = "Нестероидный противовоспалительный препарат (НПВП). Оказывает быстрое анальгезирующее, жаропонижающее и противовоспалительное действие.",
                active_substance = "Ибупрофен",
                composition = "Активное вещество: ибупрофен 400 мг. Вспомогательные вещества: кроскармеллоза натрия, натрия лаурилсульфат, кремния диоксид коллоидный, стеариновая кислота, макрогол 6000, гипромеллоза, тальк.",
                indications = "• Головная и зубная боли\n• Мигрень\n• Болезненные менструации\n• Боли в суставах, мышцах и спине\n• Лихорадочные состояния при простуде и гриппе",
                dosage = "Внутрь, запивая водой. Взрослым и детям старше 12 лет: по 1 таблетке (400 мг) до 3 раз в сутки. Максимальная суточная доза — 1200 мг (3 таблетки). Интервал между приемами — не менее 6 часов.",
                contraindications = "• Эрозивно-язвенные поражения ЖКТ в фазе обострения\n• Выраженная почечная или печеночная недостаточность\n• Тяжелая сердечная недостаточность\n• Беременность (III триместр)\n• Повышенная чувствительность к ибупрофену или другим компонентам",
                side_effects = "• Со стороны пищеварительной системы: тошнота, рвота, изжога, абдоминальные боли, диарея.\n• Со стороны ЦНС: головная боль, головокружение.\n• Аллергические реакции: кожная сыпь, зуд, крапивница.",
                special_instructions = "Не принимать одновременно с другими НПВП. Во время лечения не рекомендуется употребление алкоголя, так как это повышает риск поражения слизистой оболочки ЖКТ. С осторожностью применять при вождении автотранспорта.",
                storage_conditions = "Хранить в оригинальной упаковке при температуре не выше 25 °C. Срок годности 3 года.",
                interaction = "Не рекомендуется сочетать с ацетилсалициловой кислотой и другими НПВП. Диуретики и гипотензивные средства могут снижать свою эффективность. Повышается риск желудочно-кишечных кровотечений при совместном приеме со спиртосодержащими жидкостями."
            )
        }

        if (normalized.contains("но-шп") || normalized.contains("ношп")) {
            return DrugInstruction(
                name = "Но-Шпа (No-Spa)",
                description = "Спазмолитическое средство. Снижает тонус гладкой мускулатуры внутренних органов, расширяет кровеносные сосуды.",
                active_substance = "Дротаверин",
                composition = "Активное вещество: дротаверина гидрохлорид 40 мг. Вспомогательные вещества: магния стеарат, тальк, повидон, крахмал кукурузный, лактозы моногидрат.",
                indications = "• Спазмы гладкой мускулатуры при заболеваниях желчевыводящих путей (холецистит, холангит)\n• Спазмы гладкой мускулатуры мочевыводящих путей (нефролитиат, цистит)\n• В качестве вспомогательной терапии при спазмах ЖКТ (язва, гастрит, колит, метеоризм)\n• Головные боли напряжения\n• Дисменорея (менструальные боли)",
                dosage = "Внутрь. Взрослым: по 1-2 таблетки (40мг-80мг) 2-3 раза в сутки. Максимальная суточная доза — 240 мг (6 таблеток). Детям от 6 до 12 лет: по 1 таблетке 1-2 раза в сутки.",
                contraindications = "• Тяжелая почечная, печеночная или сердечная недостаточность\n• Период лактации (грудного вскармливания)\n• Детский возраст до 6 лет\n• Наследственная непереносимость галактозы\n• Повышенная чувствительность к дротаверину",
                side_effects = "Редко: тошнота, запор, головная боль, головокружение, бессонница, учащенное сердцебиение, снижение артериального давления, аллергические реакции.",
                special_instructions = "Препарат содержит лактозу, поэтому не рекомендуется пациентам с дефицитом лактазы. При артериальной гипотензии применение требует осторожности."
            )
        }

        if (normalized.contains("аспирин") || normalized.contains("ацетилсалицил")) {
            return DrugInstruction(
                name = "Аспирин Кардио 100мг",
                description = "Антиагрегантное средство. Препятствует склеиванию тромбоцитов, снижает риск образования тромбов.",
                active_substance = "Ацетилсалициловая кислота",
                composition = "Активное вещество: ацетилсалициловая кислота 100 мг. Вспомогательные вещества: целлюлоза порошкообразная, крахмал кукурузный. Оболочка: сополимер метакриловой кислоты и этилакрилата, натрия лаурилсульфат, полисорбат 80, тальк, triэтилцитрат.",
                indications = "• Профилактика острого инфаркта миокарда при наличии факторов риска\n• Профилактика повторного инфаркта миокарда\n• Профилактика инсульта и преходящего нарушения мозгового кровообращения\n• Профилактика тромбоэмболии после операций на сосудах",
                dosage = "Внутрь, перед едой, запивая большим количеством воды. Препарат предназначен для длительного применения. Принимать по 1 таблетке (100 мг) 1 раз в сутки, желательно в одно и то же время.",
                contraindications = "• Бронхиальная астма, индуцированная приемом салицилатов\n• Острое кровотечение в ЖКТ\n• Беременность (I и III триместры)\n• Печеночная и почечная недостаточность тяжелой степени\n• Возраст до 18 лет",
                side_effects = "• Пищеварительная система: тошнота, рвота, изжога, микрокровотечения в ЖКТ.\n• Кроветворная система: повышенная кровоточивость, анемия.\n• Аллергические реакции: бронхоспазм, отек Квинке, сыпь.",
                special_instructions = "Принимать строго по назначению врача. Запрещено сочетать с алкоголем (высокий риск сильного желудочного кровотечения). Перед плановыми операциями прекратить прием за 5-7 дней."
            )
        }

        if (normalized.contains("ксарелт") || normalized.contains("ривароксабан")) {
            return DrugInstruction(
                name = "Ксарелто 20мг",
                description = "Антикоагулянт прямого действия, высокоселективный ингибитор фактора Xa. Предотвращает образование тромбов.",
                active_substance = "Ривароксабан",
                composition = "Активное вещество: ривароксабан 20 мг. Вспомогательные вещества: микрокристаллическая целлюлоза, кроскармеллоза натрия, гипромеллоза, лактозы моногидрат, магния стеарат, натрия лаурилсульфат.",
                indications = "• Профилактика инсульта и системной тромбоэмболии у пациентов с фибрилляцией предсердий\n• Лечение тромбоза глубоких вен (ТГВ) и легочной эмболии (ТЭЛА)\n• Профилактика рецидивов ТГВ и ТЭЛА",
                dosage = "Внутрь, во время еды. При профилактике инсульта рекомендуемая доза составляет 1 таблетку (20 мг) 1 раз в сутки. Для некоторых групп пациентов доза может быть снижена до 15 мг.",
                contraindications = "• Клинически значимые активные кровотечения\n• Заболевания печени, протекающие с коагулопатией и риском кровотечения\n• Беременность и период грудного вскармливания\n• Возраст до 18 лет\n• Тяжелая почечная недостаточность (клиренс креатинина <15 мл/мин)",
                side_effects = "• Часто: кровотечения различной локализации (носовые, десневые, ЖКТ, мочеполовые).\n• Редко: головокружение, головная боль, тошнота, лихорадка, зуд.",
                special_instructions = "Необходим строгий контроль за признаками кровотечений. Не прерывать прием без консультации врача. Препарат имеет множество лекарственных взаимодействий (особенно с НПВП и другими антикоагулянтами)."
            )
        }

        if (normalized.contains("парацетамол") || normalized.contains("эффералган")) {
            return DrugInstruction(
                name = "Парацетамол 500мг",
                description = "Анальгетик-антипиретик. Обладает выраженным обезболивающим и жаропонижающим действием, слабо выраженным противовоспалительным эффектом.",
                active_substance = "Парацетамол",
                composition = "Активное вещество: парацетамол 500 мг. Вспомогательные вещества: крахмал картофельный, стеариновая кислота, лактоза.",
                indications = "• Болевой синдром слабой и умеренной интенсивности (головная, зубная, мигренозная боли, невралгия, мышечная боль)\n• Повышенная температура тела при простудных заболеваниях и гриппе",
                dosage = "Внутрь, после еды с большим количеством воды. Взрослым и подросткам старше 12 лет: по 1-2 таблетки (500-1000 мг) до 4 раз в сутки. Максимальная разовая доза — 1000 мг. Максимальная суточная доза — 4000 мг (8 таблеток).",
                contraindications = "• Тяжелые нарушения функции печени или почек\n• Алкогольная зависимость\n• Детский возраст до 6 лет\n• Повышенная чувствительность к парацетамолу",
                side_effects = "Обычно переносится хорошо. Иногда возможны: аллергические реакции (кожный зуд, крапивница), диспепсические расстройства. При длительном применении высоких доз — гепатотоксическое действие.",
                special_instructions = "Не превышать рекомендуемую дозу! Парацетамол входит в состав многих комбинированных лекарств против простуды, не принимайте их одновременно. Абсолютно несовместим с употреблением алкоголя из-за токсического поражения печени."
            )
        }

        if (normalized.contains("арбидол") || normalized.contains("умифеновир")) {
            return DrugInstruction(
                name = "Арбидол капсулы 100мг",
                description = "Противовирусное средство. Специфически подавляет вирусы гриппа А и В, коронавирус, ассоциированный с тяжелым острым респираторным синдромом.",
                active_substance = "Умифеновир",
                composition = "Активное вещество: умифеновира гидрохлорида моногидрат 103.5 мг (в пересчете на умифеновира гидрохлорид 100 мг). Вспомогательные вещества: крахмал картофельный, целлюлоза микрокристаллическая, кремния диоксид коллоидный, повидон, кальция стеарат.",
                indications = "• Профилактика и лечение гриппа А и В, других ОРВИ у взрослых и детей\n• Комплексная терапия острых кишечных ротавирусных инфекций\n• Профилактика послеоперационных инфекционных осложнений",
                dosage = "Внутрь, до еды. Взрослым и детям старше 12 лет при лечении ОРВИ/гриппа: по 2 капсулы (200 мг) 4 раза в сутки в течение 5 дней. Для неспецифической профилактики в период эпидемии: по 200 мг 2 раза в неделю в течение 3 недель.",
                contraindications = "• Повышенная чувствительность к умифеновиру\n• Детский возраст до 6 лет (для дозировки 100 мг)\n• Первый триместр беременности (применять только по назначению врача)",
                side_effects = "Очень редко: аллергические реакции (кожная сыпь, зуд, крапивница).",
                special_instructions = "Начало приема препарата необходимо начинать при первых симптомах заболевания (желательно в первые 48 часов). Отрицательного влияния на управление транспортным средством не оказывает."
            )
        }

        if (normalized.contains("энтерофурил") || normalized.contains("нифуроксазид")) {
            return DrugInstruction(
                name = "Энтерофурил капсулы 200мг",
                description = "Противомикробный препарат широкого спектра действия для лечения кишечных инфекций. Действует исключительно в просвете кишечника и не всасывается.",
                active_substance = "Нифуроксазид",
                composition = "Активное вещество: нифуроксазид 200 мг. Вспомогательные вещества: сахароза, крахмал кукурузный, целлюлоза микрокристаллическая, магния стеарат. Капсула: желатин, титана диоксид, краситель хинолиновый желтый, краситель желтый солнечный закат.",
                indications = "• Острая бактериальная диарея, протекающая без ухудшения общего состояния, повышения температуры тела, интоксикации.",
                dosage = "Внутрь. Взрослым: по 200 мг (1 капсула) 4 раза в сутки (каждые 6 часов). Максимальная суточная доза - 800 мг. Курс лечения не должен превышать 7 дней.",
                contraindications = "• Повышенная чувствительность к нифуроксазиду или другим производным нитрофурана\n• Детский возраст до 3 лет (для данной лекарственной формы)\n• Беременность",
                side_effects = "В редких случаях отмечаются аллергические реакции (сыпь, крапивница, отек Квинке, анафилактический шок). При их появлении препарат следует немедленно отменить.",
                special_instructions = "При лечении диареи необходимо обязательно проводить регидратационную терапию (восполнение потери жидкости). Во время приема препарата запрещено употребление этанола (алкоголя), так как нифуроксазид может провоцировать дисульфирамоподобную реакцию."
            )
        }

        // Default template generator for unrecognized medicines
        val capitalized = medicineName.trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("ru")) else it.toString() }
        return DrugInstruction(
            name = capitalized,
            description = "Фармацевтический лекарственный препарат направленного симптоматического и этиотропного действия. Облегчает состояние больного и нейтрализует симптомы воспалительных, инфекционных или спастических поражений.",
            active_substance = "Клинический активный компонент",
            composition = "Активное вещество: Клинический активный компонент (согласно торговой марке). Вспомогательные вещества: микрокристаллическая целлюлоза, лактоза, кремния диоксид коллоидный, магния стеарат, компоненты пленочной оболочки.",
            indications = "• Симптоматическая терапия широкого спектра нозологий\n• Облегчение болевых ощущений умеренной степени выраженности\n• Вспомогательное лечение воспалительных и сопутствующих заболеваний",
            dosage = "Внутрь, согласно предписаниям лечащего врача. Стандартная терапевтическая дозировка для взрослых составляет 1-2 таблетки до 3 раз в сутки во время или после приема пищи. Интервал между приемами должен составлять не менее 6-8 часов.",
            contraindications = "• Прямая чувствительность к компонентам состава препарата\n• Тяжелые органические поражения печени или почек в острой фазе\n• Беременность и период лактации\n• Детский возраст до назначения лечащим врачом",
            side_effects = "В редких случаях отмечаются преходящие желудочно-кишечные расстройства (изжога, тошнота), легкая сонливость или крапивница. При появлении нежелательных симптомов прекратите прием.",
            special_instructions = "Применять с осторожностью при выполнении работ, требующих высокой концентрации внимания и быстрой реакции. Перед началом курса рекомендуется проконсультироваться с квалифицированным лечащим врачом.",
            storage_conditions = "Хранить в сухом и темном месте при температуре от 15 °C до 25 °C. Беречь от воздействия прямых солнечных лучей. Хранить вне доступа детей и домашних животных.",
            interaction = "Существенных нежелательных взаимодействий со стандартными лекарственными препаратами не зарегистрировано. Не рекомендуется совместный прием с алкогольными напитками из-за риска повышения гепатотоксичности."
        )
    }
}
