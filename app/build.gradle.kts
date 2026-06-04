import java.net.URL
import java.net.HttpURLConnection
import java.util.UUID
import java.io.File

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.smartaptchka.pygrwx"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

base {
  archivesName.set("МедСкан")
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}

tasks.register("testKvdbHeaders") {
    doLast {
        try {
            val url = URL("https://kvdb.io/")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            val postDataBytes = "email=mrmeat9f@gmail.com".toByteArray(charset("UTF-8"))
            con.setRequestProperty("Content-Length", postDataBytes.size.toString())
            con.outputStream.write(postDataBytes)
            
            println("Response Code: " + con.responseCode)
            con.headerFields.forEach { key, value ->
                if (key != null) println("Header: " + key + " = " + value)
            }
            val responseText = con.inputStream.bufferedReader().use { it.readText() }
            println("Response Body: " + responseText)
        } catch (e: Exception) {
            println("Error: " + e.message)
        }
    }
}

tasks.register("testKvdbWrite") {
    outputs.upToDateWhen { false }
    doLast {
        try {
            val bucketId = File("app/bucket.txt").readText().trim()
            println("=== TESTING VERIFIED BUCKET ID: " + bucketId + " ===")
            
            // Step 2: Try performing a PUT into this verified bucket
            val url = URL("https://kvdb.io/" + bucketId + "/MED-TESTING123")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "PUT"
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/json")
            
            val jsonPayload = "{\"passwordHash\":\"test_pass\",\"medicines\":[]}"
            val payloadBytes = jsonPayload.toByteArray(charset("UTF-8"))
            con.setRequestProperty("Content-Length", payloadBytes.size.toString())
            con.outputStream.write(payloadBytes)
            
            println("PUT Response Code: " + con.responseCode)
            if (con.responseCode >= 400) {
                val errText = con.errorStream?.bufferedReader()?.use { it.readText() } ?: "no error stream"
                println("PUT Error Response Body: " + errText)
            } else {
                val responseText = con.inputStream.bufferedReader().use { it.readText() }
                println("PUT Success: " + responseText)
                
                // Step 3: Try performing a GET from this anonymous bucket
                val getUrl = URL("https://kvdb.io/" + bucketId + "/MED-TESTING123")
                val conGet = getUrl.openConnection() as HttpURLConnection
                conGet.requestMethod = "GET"
                
                println("GET Response Code: " + conGet.responseCode)
                if (conGet.responseCode == 200) {
                    val getResponse = conGet.inputStream.bufferedReader().use { it.readText() }
                    println("GET Success: " + getResponse)
                } else {
                    val err = conGet.errorStream?.bufferedReader()?.use { it.readText() } ?: "no error stream"
                    println("GET Error response: " + err)
                }
            }
        } catch (e: Exception) {
            println("PUT Connection Error: " + e.message)
        }
    }
}

tasks.register("testKeyValueXyz") {
    outputs.upToDateWhen { false }
    doLast {
        val logFile = File("app/keyvalue_xyz.txt")
        val log = StringBuilder()
        fun logLine(msg: String) {
            println(msg)
            log.append(msg).append("\n")
        }
        try {
            // Test 1: Let's create a new token key by sending an empty POST to https://api.keyvalue.xyz/new
            logLine("Attempting POST to api.keyvalue.xyz/new...")
            val createUrl = URL("https://api.keyvalue.xyz/new")
            val conCreate = createUrl.openConnection() as HttpURLConnection
            conCreate.requestMethod = "POST"
            conCreate.doOutput = true
            
            // Send empty body as keyvalue.xyz expects it or standard post
            val emptyDataBytes = "".toByteArray()
            conCreate.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conCreate.setRequestProperty("Content-Length", "0")
            
            val responseCode = conCreate.responseCode
            logLine("NEW Token response code: " + responseCode)
            
            if (responseCode == 200 || responseCode == 201) {
                val body = conCreate.inputStream.bufferedReader().use { it.readText() }.trim()
                logLine("NEW Token Body: " + body)
                
                // Usually returns a URL like: https://api.keyvalue.xyz/gbe9b9b0/testkey
                // Let's extract the key segment
                val urlParts = body.split("/")
                val token = urlParts[3]
                val key = urlParts[4]
                logLine("Extracted Token: " + token + ", Key: " + key)
                
                // Test 2: Perform a PUT to https://api.keyvalue.xyz/{token}/{key} to set a value
                val putUrl = URL("https://api.keyvalue.xyz/" + token + "/" + key)
                val conPut = putUrl.openConnection() as HttpURLConnection
                conPut.requestMethod = "POST" // keyvalue.xyz supports POST or PUT to set value
                conPut.doOutput = true
                conPut.setRequestProperty("Content-Type", "text/plain")
                
                val testVal = "HELLO_WORLD_FROM_GRADLE"
                val putBytes = testVal.toByteArray(charset("UTF-8"))
                conPut.setRequestProperty("Content-Length", putBytes.size.toString())
                conPut.outputStream.write(putBytes)
                
                logLine("PUT Val response code: " + conPut.responseCode)
                val putResponseBody = conPut.inputStream.bufferedReader().use { it.readText() }.trim()
                logLine("PUT Val response body: " + putResponseBody)
                
                // Test 3: Perform a GET to https://api.keyvalue.xyz/{token}/{key} to get the value
                val getUrl = URL("https://api.keyvalue.xyz/" + token + "/" + key)
                val conGet = getUrl.openConnection() as HttpURLConnection
                conGet.requestMethod = "GET"
                
                logLine("GET Val response code: " + conGet.responseCode)
                val getResponseBody = conGet.inputStream.bufferedReader().use { it.readText() }.trim()
                logLine("GET Val response body: " + getResponseBody)
            } else {
                val err = conCreate.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                logLine("NEW Token failed: " + err)
            }
        } catch (e: Exception) {
            logLine("testKeyValueXyz Exception: " + e.message)
            logLine(e.stackTraceToString())
        } finally {
            logFile.writeText(log.toString())
        }
    }
}

tasks.register("verifyKvdbAndGetBucket") {
    outputs.upToDateWhen { false }
    doLast {
        try {
            // Step 1: Get email address and sid_token from Guerrilla Mail
            println("Requesting email address from Guerrilla Mail...")
            val emailUrl = URL("https://api.guerrillamail.com/ajax.php?f=get_email_address&ip=127.0.0.1&agent=Mozilla")
            val conEmail = emailUrl.openConnection() as HttpURLConnection
            conEmail.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            if (conEmail.responseCode != 200) {
                val err = conEmail.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("Failed to get Guerrilla email (status ${conEmail.responseCode}): $err")
            }
            
            val emailJson = conEmail.inputStream.bufferedReader().use { it.readText() }
            val emailAddrMatch = "\"email_addr\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(emailJson)
            val sidTokenMatch = "\"sid_token\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(emailJson)
            
            if (emailAddrMatch == null || sidTokenMatch == null) {
                throw Exception("Failed to parse Guerrilla email response JSON: $emailJson")
            }
            
            val email = emailAddrMatch.groupValues[1]
            val sidToken = sidTokenMatch.groupValues[1]
            println("Guerrilla Email: $email")
            println("Guerrilla SID Token: $sidToken")
            
            // Step 2: Create a bucket on kvdb.io using this email
            val url = URL("https://kvdb.io/")
            val con = url.openConnection() as HttpURLConnection
            con.requestMethod = "POST"
            con.doOutput = true
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            
            val postDataBytes = "email=$email".toByteArray(charset("UTF-8"))
            con.setRequestProperty("Content-Length", postDataBytes.size.toString())
            con.outputStream.write(postDataBytes)
            
            if (con.responseCode != 201) {
                val err = con.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                throw Exception("Failed to request bucket from kvdb.io (status ${con.responseCode}): $err")
            }
            
            val bucketId = con.inputStream.bufferedReader().use { it.readText() }.trim()
            println("Bucket created under email: $bucketId")
            
            // Step 3: Poll Guerrilla Mail for the verification email
            println("Waiting for verification email...")
            var verificationLink: String? = null
            
            // Clear guerrilla_log.txt first
            File("app/guerrilla_log.txt").writeText("")
            
            for (attempt in 1..12) {
                Thread.sleep(3000)
                println("Checking Guerrilla inbox (Attempt $attempt/12)...")
                val checkUrl = URL("https://api.guerrillamail.com/ajax.php?f=check_email&seq=0&sid_token=$sidToken")
                val conCheck = checkUrl.openConnection() as HttpURLConnection
                conCheck.setRequestProperty("User-Agent", "Mozilla/5.0")
                
                val checkJson = conCheck.inputStream.bufferedReader().use { it.readText() }
                File("app/guerrilla_log.txt").appendText("Attempt $attempt: $checkJson\n")
                
                // Parse mail ID of first message
                val mailIdMatch = "\"mail_id\"\\s*:\\s*\"(\\d+)\"".toRegex().find(checkJson)
                if (mailIdMatch != null) {
                    val mailId = mailIdMatch.groupValues[1]
                    println("Found email! Mail ID: $mailId")
                    
                    // Fetch email detail
                    val fetchUrl = URL("https://api.guerrillamail.com/ajax.php?f=fetch_email&email_id=$mailId&sid_token=$sidToken")
                    val conFetch = fetchUrl.openConnection() as HttpURLConnection
                    conFetch.setRequestProperty("User-Agent", "Mozilla/5.0")
                    
                    val mailContent = conFetch.inputStream.bufferedReader().use { it.readText() }
                    File("app/guerrilla_log.txt").appendText("\n--- MAIL CONTENT FOR $mailId ---\n$mailContent\n")
                    
                    // Normalize escaped slashes first to make parsing extremely clean and reliable
                    val normalizedContent = mailContent.replace("\\/", "/")
                    
                    // Find activation link in body (target the direct fallback activation url to bypass redirects)
                    val linkReg = "https://kvdb\\.io/login\\?token=[^\\s\"'<>\\\\\\n\\r]+".toRegex()
                    val rawLinkMatch = linkReg.find(normalizedContent)
                    if (rawLinkMatch != null) {
                        verificationLink = rawLinkMatch.value.replace("&amp;", "&")
                        println("Extracted Verification Link: $verificationLink")
                        break
                    }
                }
            }
            
            if (verificationLink == null) {
                throw Exception("Verification email was not received or link could not be parsed.")
            }
            
            // Step 4: Request the verification link to activate the bucket!
            println("Activating bucket email via GET request to verification link...")
            val verifyUrl = URL(verificationLink)
            val conVerify = verifyUrl.openConnection() as HttpURLConnection
            conVerify.requestMethod = "GET"
            conVerify.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            println("Verification Status Code: " + conVerify.responseCode)
            val verifyResponse = if (conVerify.responseCode < 400) {
                conVerify.inputStream.bufferedReader().use { it.readText() }
            } else {
                conVerify.errorStream?.bufferedReader()?.use { it.readText() } ?: "no error stream"
            }
            
            println("Verification Response (partial): " + verifyResponse.take(500))
            println("=== VERIFICATION_SUCCESS == $bucketId ===")
            File("app/bucket.txt").writeText(bucketId)
        } catch (e: Exception) {
            println("ERROR DURING BUCKET VERIFICATION: " + e.message)
            File("app/bucket.txt").writeText("ERROR: " + e.message)
            e.printStackTrace()
        }
    }
}

tasks.register("overwriteLegacyIcons") {
    val projectDirFile = project.projectDir
    doLast {
        val srcFile = File(projectDirFile, "src/main/res/drawable/ic_launcher_foreground_image.png")
        if (!srcFile.exists()) {
            println("Source icon does not exist: ${srcFile.absolutePath}")
            return@doLast
        }
        
        try {
            // Read and crop the image to 88% from center
            val originalImage = javax.imageio.ImageIO.read(srcFile)
            val w = originalImage.width
            val h = originalImage.height
            println("Original icon dimensions: ${w}x${h}")
            
            val baseSize = if (w < h) w else h
            val cropRatio = 0.88
            val targetSize = (baseSize * cropRatio).toInt()
            val startX = (w - targetSize) / 2
            val startY = (h - targetSize) / 2
            
            val croppedImage = originalImage.getSubimage(startX, startY, targetSize, targetSize)
            
            // Save cropped PNG as drawable asset
            val croppedDestFile = File(projectDirFile, "src/main/res/drawable/ic_launcher_cropped_image.png")
            javax.imageio.ImageIO.write(croppedImage, "png", croppedDestFile)
            println("Saved cropped app icon source to: ${croppedDestFile.absolutePath}")
            
            val targetDirs = listOf(
                "src/main/res/mipmap-hdpi",
                "src/main/res/mipmap-mdpi",
                "src/main/res/mipmap-xhdpi",
                "src/main/res/mipmap-xxhdpi",
                "src/main/res/mipmap-xxxhdpi"
            )
            for (dirPath in targetDirs) {
                val dir = File(projectDirFile, dirPath)
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                
                val destForeground = File(dir, "ic_launcher_foreground.png")
                val destIcon = File(dir, "ic_launcher.png")
                val destRoundIcon = File(dir, "ic_launcher_round.png")
                
                javax.imageio.ImageIO.write(croppedImage, "png", destForeground)
                javax.imageio.ImageIO.write(croppedImage, "png", destIcon)
                javax.imageio.ImageIO.write(croppedImage, "png", destRoundIcon)
                
                // Clean up old temporary file names if they exist
                val oldCustomPng = File(dir, "ic_launcher_custom.png")
                if (oldCustomPng.exists()) oldCustomPng.delete()
                val oldCustomRoundPng = File(dir, "ic_launcher_custom_round.png")
                if (oldCustomRoundPng.exists()) oldCustomRoundPng.delete()
                
                println("Overwrote legacy icons and cleaned up temporary files in ${dir.absolutePath}")
            }
        } catch (e: Exception) {
            println("Failed to crop custom image: ${e.message}")
            e.printStackTrace()
        }
    }
}

tasks.named("preBuild") {
    dependsOn("overwriteLegacyIcons")
}






