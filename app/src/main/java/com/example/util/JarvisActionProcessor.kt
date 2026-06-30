package com.example.util

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.provider.AlarmClock
import android.util.Log
import com.example.data.repository.JarvisRepository
import java.text.SimpleDateFormat
import java.util.*

class JarvisActionProcessor(
    private val context: Context,
    private val repository: JarvisRepository,
    private val appControlManager: AppControlManager,
    private val onNavigationRequested: (String) -> Unit = {}
) {

    /**
     * Analyzes voice input and processes local triggers.
     * Returns a pair of (ResponseSpeechString, ActionSuccessBoolean) if handled, or null if it requires AI.
     */
    suspend fun processVoiceCommand(speech: String): Pair<String, Boolean>? {
        val cleanSpeech = speech.lowercase().trim()
        val isBangla = cleanSpeech.contains("সময়") || cleanSpeech.contains("তারিখ") || 
                       cleanSpeech.contains("কেমন") || cleanSpeech.contains("চার্জ") || 
                       cleanSpeech.contains("নাম") || cleanSpeech.contains("করো") ||
                       cleanSpeech.contains("যোগ") || cleanSpeech.contains("কাজ") ||
                       cleanSpeech.contains("সেটিং") || cleanSpeech.contains("পিছনে") ||
                       cleanSpeech.contains("অ্যাপ") || cleanSpeech.contains("মেমোরি")

        Log.d("JarvisActionProcessor", "Processing command: '$speech' (isBangla: $isBangla)")

        // 0. Voice Navigation inside our own app
        if (cleanSpeech.contains("go to setting") || cleanSpeech.contains("open setting") || cleanSpeech.contains("সেটিং এ যাও") || cleanSpeech.contains("সেটিংস এ যাও")) {
            onNavigationRequested("settings")
            return if (isBangla) {
                "আমি আপনার সেটিংস পৃষ্ঠা খুলছি, স্যার।" to true
            } else {
                "Opening settings console immediately, Sir." to true
            }
        }
        if (cleanSpeech.contains("go to model") || cleanSpeech.contains("go to ai") || cleanSpeech.contains("open ai setting") || cleanSpeech.contains("মডেল সেটিং")) {
            onNavigationRequested("ai_settings")
            return if (isBangla) {
                "আমি কৃত্রিম বুদ্ধিমত্তা মডেল সেটিংস খুলছি, স্যার।" to true
            } else {
                "Accessing neural network and AI core configuration panel, Sir." to true
            }
        }
        if (cleanSpeech.contains("go to memory") || cleanSpeech.contains("open memory") || cleanSpeech.contains("open logs") || cleanSpeech.contains("মেমোরি দেখাও")) {
            onNavigationRequested("memory_settings")
            return if (isBangla) {
                "আমি মেমোরি লগ খুলছি, স্যার।" to true
            } else {
                "Opening cognitive memories and local data logs, Sir." to true
            }
        }
        if (cleanSpeech.contains("go to app") || cleanSpeech.contains("open app manager") || cleanSpeech.contains("open app setting") || cleanSpeech.contains("অ্যাপ ম্যানেজার")) {
            onNavigationRequested("app_settings")
            return if (isBangla) {
                "আমি অ্যাপ্লিকেশন কন্ট্রোল সেন্টার খুলছি, স্যার।" to true
            } else {
                "Opening authorized applications control center, Sir." to true
            }
        }
        if (cleanSpeech.contains("go back") || cleanSpeech.contains("close this") || cleanSpeech.contains("পিছনে যাও")) {
            onNavigationRequested("BACK")
            return if (isBangla) {
                "পিছনে যাচ্ছি, স্যার।" to true
            } else {
                "Returning to the previous node, Sir." to true
            }
        }

        // 1. App launching command
        if (cleanSpeech.contains("open ") || cleanSpeech.contains("launch ") || 
            cleanSpeech.contains("start ") || cleanSpeech.contains("show ") ||
            cleanSpeech.contains("চালু কর") || cleanSpeech.contains("ওপেন কর") ||
            cleanSpeech.contains("চালু করো") || cleanSpeech.contains("ওপেন করো")) {
            
            // Try matching app launch
            val (launched, appName) = appControlManager.launchAppByVoice(speech)
            if (launched) {
                return if (isBangla) {
                    "স্যার, আমি $appName চালু করছি।" to true
                } else {
                    "Sir, launching $appName immediately. System gateway initialized." to true
                }
            } else if (cleanSpeech.contains("open") || cleanSpeech.contains("launch")) {
                return if (isBangla) {
                    "স্যার, দুঃখিত। এই অ্যাপ্লিকেশনটি খুঁজে পাওয়া যায়নি।" to false
                } else {
                    "Sir, I could not find a synchronized application matching that description." to false
                }
            }
        }

        // 2. Memory / Identity Save ("My name is Al Amin" or "আমার নাম আল আমিন")
        if (cleanSpeech.contains("my name is") || cleanSpeech.contains("আমার নাম")) {
            val name = extractName(speech)
            if (name.isNotEmpty()) {
                repository.saveSetting("user_name", name)
                repository.addMemory("User's name is $name", "profile")
                return if (isBangla) {
                    "আপনার সাথে পরিচিত হয়ে ভালো লাগলো $name। আমি আপনার নাম মনে রাখব।" to true
                } else {
                    "Acknowledged. It is an honor to meet you, $name. I have committed your identity to memory core." to true
                }
            }
        }

        // 3. Time command
        if (cleanSpeech.contains("time") || cleanSpeech.contains("সময়") || cleanSpeech.contains("কয়টা বাজে")) {
            val timeString = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
            return if (isBangla) {
                "স্যার, এখন সময় $timeString।" to true
            } else {
                "Sir, the current time is $timeString. All internal chronometers are synchronized." to true
            }
        }

        // 4. Date command
        if (cleanSpeech.contains("date") || cleanSpeech.contains("তারিখ") || cleanSpeech.contains("আজকে কি বার")) {
            val dateString = SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(Date())
            val banglaDate = SimpleDateFormat("EEEE, dd MMMM, yyyy", Locale( "bn")).format(Date())
            return if (isBangla) {
                "স্যার, আজকে $banglaDate।" to true
            } else {
                "Sir, today's planetary date is $dateString." to true
            }
        }

        // 5. Battery command
        if (cleanSpeech.contains("battery") || cleanSpeech.contains("charge") || cleanSpeech.contains("চার্জ") || cleanSpeech.contains("ব্যাটারি")) {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val pct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            return if (isBangla) {
                "স্যার, আমাদের মূল শক্তি কোর $pct শতাংশে চার্জ করা আছে।" to true
            } else {
                "Sir, the primary battery core is operating at $pct percent capacity." to true
            }
        }

        // 6. To-Do Manager commands
        // Add To-Do: e.g. "add to-do buy milk", "remind me to buy groceries", "কাজ যোগ করো বাজারে যাও"
        if (cleanSpeech.contains("add to-do") || cleanSpeech.contains("add todo") || 
            cleanSpeech.contains("remind me to") || cleanSpeech.contains("কাজ যোগ করো")) {
            val todoText = extractTodoText(speech)
            if (todoText.isNotEmpty()) {
                repository.addMemory(todoText, "todo")
                return if (isBangla) {
                    "স্যার, আমি আপনার তালিকায় কাজ যোগ করেছি: $todoText।" to true
                } else {
                    "Sir, I have added the following task to your tactical log: '$todoText'." to true
                }
            }
        }

        // List To-Dos: "list my to-dos", "list todo", "আমার কাজগুলো দেখাও"
        if (cleanSpeech.contains("list todo") || cleanSpeech.contains("list my to-dos") || 
            cleanSpeech.contains("show my tasks") || cleanSpeech.contains("আমার কাজ")) {
            val memories = repository.searchMemories("") // searches all
            val todos = memories.filter { it.category == "todo" }
            if (todos.isEmpty()) {
                return if (isBangla) {
                    "স্যার, আপনার তালিকায় কোনো কাজ খালি নেই। আপনি আজকের কাজগুলো সম্পন্ন করেছেন।" to true
                } else {
                    "Sir, your tactical to-do list is currently clear. Excellent coordination." to true
                }
            } else {
                val listStr = todos.mapIndexed { idx, it -> "${idx + 1}. ${it.fact}" }.joinToString(", ")
                return if (isBangla) {
                    "স্যার, আপনার কাজগুলো হল: $listStr।" to true
                } else {
                    "Sir, your active items are: $listStr." to true
                }
            }
        }

        // Clear To-Dos: "clear my to-dos", "delete all to-dos"
        if (cleanSpeech.contains("clear todo") || cleanSpeech.contains("clear all to-dos") || cleanSpeech.contains("কাজগুলো মুছে ফেলো")) {
            repository.deleteMemoriesByCategory("todo")
            return if (isBangla) {
                "স্যার, আপনার সব কাজ মুছে ফেলা হয়েছে।" to true
            } else {
                "Sir, I have purged all records from your to-do log." to true
            }
        }

        // 7. Alarms setting: "set alarm for 7:30" or "set alarm at 8" or "অ্যালার্ম দাও"
        if (cleanSpeech.contains("set alarm") || cleanSpeech.contains("alarm set") || cleanSpeech.contains("অ্যালার্ম")) {
            val (hour, minute) = parseTimeFromSpeech(cleanSpeech)
            if (hour != -1) {
                try {
                    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_MESSAGE, "JARVIS Tactical Alarm")
                        putExtra(AlarmClock.EXTRA_HOUR, hour)
                        putExtra(AlarmClock.EXTRA_MINUTES, minute)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(alarmIntent)
                    return if (isBangla) {
                        "স্যার, আমি $hour টা বেজে $minute মিনিটে অ্যালার্ম সেট করেছি।" to true
                    } else {
                        "Sir, I have initialized the system clock to trigger an alarm at $hour:$minute. Sleep cycles calibrated." to true
                    }
                } catch (e: Exception) {
                    return "Sir, I encountered an access validation error setting the alarm." to false
                }
            }
        }

        // 8. Weather query: "weather" or "আবহাওয়া"
        if (cleanSpeech.contains("weather") || cleanSpeech.contains("temp") || cleanSpeech.contains("আবহাওয়া")) {
            // Simulated highly futuristic atmospheric scan
            val temps = listOf(26, 27, 28, 29, 30)
            val temp = temps.random()
            return if (isBangla) {
                "স্যার, বায়ুমণ্ডলীয় স্ক্যান অনুযায়ী তাপমাত্রা এখন $temp ডিগ্রি সেলসিয়াস, আকাশ পরিষ্কার রয়েছে।" to true
            } else {
                "Sir, atmospheric sensors indicate local temperature is $temp degrees Celsius, barometric pressure is stable. Perfect conditions for flight." to true
            }
        }

        // 9. Save/remember phone contacts
        if (cleanSpeech.contains("save contact") || cleanSpeech.contains("save number") || cleanSpeech.contains("remember phone") || cleanSpeech.contains("remember number") || cleanSpeech.contains("মনে রাখো নাম্বার") || cleanSpeech.contains("নাম্বার সংরক্ষণ করো") || cleanSpeech.contains("নাম্বার সেভ করো")) {
            val phoneRegex = Regex("[+]?[0-9\\s-]{9,15}")
            val numberMatch = phoneRegex.find(speech)
            if (numberMatch != null) {
                val phoneNumber = numberMatch.value.replace(" ", "")
                var contactName = speech.replace(numberMatch.value, "")
                val cleanWords = listOf(
                    "save contact", "save number", "remember phone", "remember number",
                    "with number", "as", "is", "for", "phone", "number", "নাম্বার সংরক্ষণ করো",
                    "নাম্বার সেভ করো", "মনে রাখো নাম্বার", "সেভ করো", "নাম্বার", "নাম্বারটি"
                )
                var tempName = contactName
                for (word in cleanWords) {
                    tempName = tempName.replace(word, "", ignoreCase = true)
                }
                contactName = tempName.replace(Regex("[^a-zA-Z0-9\\s\u0980-\u09FF]"), "").trim()
                if (contactName.isNotEmpty()) {
                    repository.addMemory("$contactName: $phoneNumber", "contact")
                    return if (isBangla) {
                        "স্যার, আমি $contactName এর মোবাইল নাম্বার $phoneNumber আমার মেমোরি কোরে সংরক্ষণ করেছি।" to true
                    } else {
                        "Sir, I have recorded $contactName's phone number ($phoneNumber) to our contact database." to true
                    }
                }
            }
        }

        // 10. WhatsApp Messaging
        if (cleanSpeech.contains("whatsapp") || cleanSpeech.contains("মেসেজ") || cleanSpeech.contains("হোয়াটসঅ্যাপ") || cleanSpeech.contains("হোয়াটস অ্যাপ")) {
            var contactQuery = ""
            var messageContent = "Hello, Sir!"
            
            val messageTriggers = listOf("saying ", "message ", "with text ", "বল ", "মেসেজ ", "কে বলো ", "কে বল ")
            var triggerFoundIdx = -1
            var usedTrigger = ""
            for (trigger in messageTriggers) {
                val idx = cleanSpeech.indexOf(trigger)
                if (idx != -1 && idx > triggerFoundIdx) {
                    triggerFoundIdx = idx
                    usedTrigger = trigger
                }
            }
            
            if (triggerFoundIdx != -1) {
                val msgStart = triggerFoundIdx + usedTrigger.length
                messageContent = speech.substring(msgStart).trim()
                val leftSide = cleanSpeech.substring(0, triggerFoundIdx)
                contactQuery = extractContactNameFromPhrase(leftSide)
            } else {
                contactQuery = extractContactNameFromPhrase(cleanSpeech)
            }
            
            if (contactQuery.isNotEmpty()) {
                var phoneNumber = findContactNumberInDatabase(contactQuery)
                if (phoneNumber == null) {
                    phoneNumber = findContactNumberInSystem(contactQuery)
                }
                
                if (phoneNumber != null) {
                    launchWhatsAppChat(phoneNumber, messageContent)
                    return if (isBangla) {
                        "স্যার, আমি হোয়াটসঅ্যাপে $contactQuery কে মেসেজ পাঠাচ্ছি।" to true
                    } else {
                        "Sir, establishing a direct link with $contactQuery on WhatsApp. Prefilling your message." to true
                    }
                } else {
                    try {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                            return if (isBangla) {
                                "স্যার, আমি $contactQuery এর নাম্বার খুঁজে পাইনি, তবে হোয়াটসঅ্যাপ অ্যাপ্লিকেশনটি খুলেছি।" to true
                            } else {
                                "Sir, I could not resolve a contact number for $contactQuery. I have launched WhatsApp so you can chat with them." to true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ActionProcessor", "Could not launch WhatsApp", e)
                    }
                }
            }
        }

        return null // Delegating to AI
    }

    private fun extractName(speech: String): String {
        val lower = speech.lowercase()
        val index = lower.indexOf("my name is")
        if (index != -1) {
            return speech.substring(index + "my name is".length).trim().capitalize()
        }
        val banglaIndex = lower.indexOf("আমার নাম")
        if (banglaIndex != -1) {
            return speech.substring(banglaIndex + "আমার নাম".length).trim()
        }
        return ""
    }

    private fun extractTodoText(speech: String): String {
        val lower = speech.lowercase()
        var text = ""
        when {
            lower.contains("add to-do") -> {
                val idx = lower.indexOf("add to-do")
                text = speech.substring(idx + "add to-do".length).trim()
            }
            lower.contains("add todo") -> {
                val idx = lower.indexOf("add todo")
                text = speech.substring(idx + "add todo".length).trim()
            }
            lower.contains("remind me to") -> {
                val idx = lower.indexOf("remind me to")
                text = speech.substring(idx + "remind me to".length).trim()
            }
            lower.contains("কাজ যোগ করো") -> {
                val idx = lower.indexOf("কাজ যোগ করো")
                text = speech.substring(idx + "কাজ যোগ করো".length).trim()
            }
        }
        return text
    }

    private fun parseTimeFromSpeech(speech: String): Pair<Int, Int> {
        val regex = Regex("(\\d{1,2})[:\\s]?(\\d{2})?")
        val matches = regex.findAll(speech).toList()
        if (matches.isNotEmpty()) {
            val firstMatch = matches.first()
            val hour = firstMatch.groupValues[1].toInt()
            val minute = if (firstMatch.groupValues[2].isNotEmpty()) firstMatch.groupValues[2].toInt() else 0
            if (hour in 0..23 && minute in 0..59) {
                return hour to minute
            }
        }
        return -1 to -1
    }

    private fun extractContactNameFromPhrase(phrase: String): String {
        var clean = phrase.lowercase()
        val removePrefixes = listOf(
            "open whatsapp and message my ",
            "open whatsapp and message ",
            "send whatsapp to my ",
            "send whatsapp to ",
            "message my ",
            "message ",
            "send to my ",
            "send to ",
            "whatsapp my ",
            "whatsapp ",
            "হোয়াটসঅ্যাপে মেসেজ দাও ",
            "হোয়াটসঅ্যাপে মেসেজ করো ",
            "হোয়াটসঅ্যাপে ",
            "হোয়াটস অ্যাপে ",
            "মেসেজ করো ",
            "মেসেজ দাও ",
            "পাপাকে ",
            "পাপা কে "
        )
        for (prefix in removePrefixes) {
            clean = clean.replace(prefix, "")
        }
        return clean.replace(Regex("[^a-zA-Z0-9\u0980-\u09FF\\s]"), "").trim()
    }

    private suspend fun findContactNumberInDatabase(query: String): String? {
        val memories = repository.searchMemories("")
        val contacts = memories.filter { it.category == "contact" }
        val match = contacts.firstOrNull { contact ->
            val parts = contact.fact.split(":")
            if (parts.size >= 2) {
                val name = parts[0].trim().lowercase()
                name.contains(query.lowercase()) || query.lowercase().contains(name)
            } else {
                false
            }
        }
        return match?.fact?.split(":")?.getOrNull(1)?.trim()
    }

    private fun findContactNumberInSystem(nameQuery: String): String? {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CONTACTS) 
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return null
        }
        try {
            val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER,
                android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            )
            val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
            val selectionArgs = arrayOf("%$nameQuery%")
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numIdx = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx != -1) {
                        return cursor.getString(numIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ActionProcessor", "Error reading system contacts", e)
        }
        return null
    }

    private fun formatPhoneNumber(num: String): String {
        val clean = num.replace(Regex("[^0-9+]"), "")
        if (clean.startsWith("0") && clean.length == 11) {
            return "+88$clean"
        }
        return clean
    }

    private fun launchWhatsAppChat(phoneNumber: String, message: String) {
        val formattedNum = formatPhoneNumber(phoneNumber)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$formattedNum&text=${android.net.Uri.encode(message)}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }
            } catch (ex: Exception) {
                Log.e("ActionProcessor", "Could not open WhatsApp", ex)
            }
        }
    }
}
