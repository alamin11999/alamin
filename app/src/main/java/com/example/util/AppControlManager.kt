package com.example.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.example.data.db.AppAliasEntity
import com.example.data.repository.JarvisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppControlManager(
    private val context: Context,
    private val repository: JarvisRepository
) {

    private val packageManager: PackageManager = context.packageManager

    /**
     * Scans installed applications with launcher intents, synchronizes them with our database,
     * and returns the synchronized list.
     */
    suspend fun syncInstalledApps(): List<AppAliasEntity> = withContext(Dispatchers.IO) {
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        
        val resolveInfos = packageManager.queryIntentActivities(intent, 0)
        val installedApps = resolveInfos.mapNotNull { info ->
            val packageName = info.activityInfo.packageName
            val appLabel = info.loadLabel(packageManager).toString()
            if (packageName.isNotEmpty()) {
                packageName to appLabel
            } else {
                null
            }
        }.distinctBy { it.first }

        Log.d("AppControlManager", "Scanned ${installedApps.size} apps from device.")

        // Load existing database aliases to preserve edits
        val existingAliases = mutableMapOf<String, AppAliasEntity>()
        repository.getEnabledAliases() // just a test or load
        repository.allAppAliases.collect { list ->
            list.forEach { existingAliases[it.packageName] = it }
        }

        val synchedList = installedApps.map { (packageName, appName) ->
            val existing = existingAliases[packageName]
            if (existing != null) {
                existing
            } else {
                // Pre-populate with default matching rules for JARVIS
                val defaultAlias = when (appName.lowercase()) {
                    "whatsapp" -> "whatsapp"
                    "facebook" -> "facebook"
                    "messenger" -> "messenger"
                    "youtube" -> "youtube"
                    "chrome" -> "chrome"
                    "camera" -> "camera"
                    "gallery" -> "gallery"
                    "photos" -> "photos"
                    "calculator" -> "calculator"
                    "settings" -> "settings"
                    else -> appName
                }
                AppAliasEntity(
                    packageName = packageName,
                    appName = appName,
                    alias = defaultAlias,
                    isFavorite = false,
                    isEnabled = true
                )
            }
        }

        // Insert new apps into the database
        repository.addAliases(synchedList)
        synchedList
    }

    /**
     * Searches database for an enabled app matching the speech phrase, and launches it if found.
     * Also supports dynamic system app lookup and direct system intents (e.g., Settings, Camera).
     * Returns true if successfully launched, false otherwise.
     */
    suspend fun launchAppByVoice(speech: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanSpeech = speech.lowercase().trim()
        
        // Extract what app we want to open (e.g. "open camera" -> "camera", "launch whatsapp" -> "whatsapp")
        val keywords = listOf("open ", "launch ", "start ", "show ", "ওপেন কর", "চালু কর", "ওপেন করো", "চালু করো")
        var appQuery: String? = null
        for (keyword in keywords) {
            val idx = cleanSpeech.indexOf(keyword)
            if (idx != -1) {
                appQuery = cleanSpeech.substring(idx + keyword.length)
                break
            }
        }

        // Standard startsWith fallbacks
        if (appQuery == null) {
            appQuery = when {
                cleanSpeech.startsWith("open") -> cleanSpeech.removePrefix("open")
                cleanSpeech.startsWith("launch") -> cleanSpeech.removePrefix("launch")
                cleanSpeech.startsWith("start") -> cleanSpeech.removePrefix("start")
                cleanSpeech.startsWith("show") -> cleanSpeech.removePrefix("show")
                else -> null
            }
        }

        if (appQuery == null) {
            return@withContext false to "No app intent detected in speech."
        }

        val cleanQuery = appQuery.trim()
        if (cleanQuery.isEmpty()) return@withContext false to "Empty app query."

        // Direct Native Intents for core system utilities
        if (cleanQuery.contains("setting") || cleanQuery.contains("সেটিং") || cleanQuery.contains("সেটিংস")) {
            try {
                val settingsIntent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return@withContext true to "Settings"
            } catch (e: Exception) {
                Log.e("AppControlManager", "Failed to launch native Settings intent", e)
            }
        }

        if (cleanQuery.contains("camera") || cleanQuery.contains("ক্যামেরা") || cleanQuery.contains("ছবি")) {
            try {
                // Try launching native camera activity first
                val cameraIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(cameraIntent)
                return@withContext true to "Camera"
            } catch (e: Exception) {
                Log.e("AppControlManager", "Failed to launch native Camera intent", e)
            }
        }

        if (cleanQuery.contains("dialer") || cleanQuery.contains("phone") || cleanQuery.contains("কল") || cleanQuery.contains("ফোন")) {
            try {
                val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                return@withContext true to "Phone"
            } catch (e: Exception) {
                Log.e("AppControlManager", "Failed to launch Dialer intent", e)
            }
        }

        // Try exact/loose matching in localized database aliases
        val enabledApps = repository.getEnabledAliases()
        val dbMatch = enabledApps.firstOrNull { app ->
            val alias = app.alias.lowercase()
            val name = app.appName.lowercase()
            alias == cleanQuery || name == cleanQuery ||
            cleanQuery.contains(alias) || cleanQuery.contains(name) ||
            alias.contains(cleanQuery) || name.contains(cleanQuery)
        }

        if (dbMatch != null) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(dbMatch.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext true to dbMatch.appName
                }
            } catch (e: Exception) {
                Log.e("AppControlManager", "Failed to launch database match ${dbMatch.appName}", e)
            }
        }

        // Broad dynamic lookup across ALL installed launcher activities on the device
        try {
            val systemIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = packageManager.queryIntentActivities(systemIntent, 0)
            
            // Try to match by package name or display label
            val systemMatch = resolveInfos.firstOrNull { info ->
                val label = info.loadLabel(packageManager).toString().lowercase()
                val pkg = info.activityInfo.packageName.lowercase()
                label.contains(cleanQuery) || cleanQuery.contains(label) ||
                pkg.contains(cleanQuery)
            }

            if (systemMatch != null) {
                val launchIntent = packageManager.getLaunchIntentForPackage(systemMatch.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext true to systemMatch.loadLabel(packageManager).toString()
                }
            }
        } catch (e: Exception) {
            Log.e("AppControlManager", "Error during dynamic app resolution", e)
        }

        // Hardcoded generic package name fallbacks (in case launcher category scan failed)
        val fallbackPackage = when {
            cleanQuery.contains("whatsapp") -> "com.whatsapp"
            cleanQuery.contains("facebook") -> "com.facebook.katana"
            cleanQuery.contains("messenger") -> "com.facebook.orca"
            cleanQuery.contains("youtube") -> "com.google.android.youtube"
            cleanQuery.contains("chrome") || cleanQuery.contains("browser") -> "com.android.chrome"
            cleanQuery.contains("calculator") || cleanQuery.contains("calc") -> "com.android.calculator2"
            else -> null
        }

        if (fallbackPackage != null) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(fallbackPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return@withContext true to cleanQuery.replaceFirstChar { it.uppercase() }
                }
            } catch (e: Exception) {
                Log.e("AppControlManager", "Failed to launch hardcoded fallback package", e)
            }
        }

        false to "App '$cleanQuery' is not installed or enabled."
    }
}
