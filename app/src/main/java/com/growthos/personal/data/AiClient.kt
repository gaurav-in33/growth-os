package com.growthos.personal.data

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

object AiClient {

    private const val SYSTEM_TEXT =
        "You are the assistant inside Growth OS, a personal productivity app. " +
            "Reply in the same language the user writes in (English, Hindi or Hinglish). " +
            "Be short, practical and friendly. Use at most 6 short lines. " +
            "Use the context only if it helps the question."

    /** Builds a small summary. It NEVER includes money entries or notes. */
    suspend fun buildContext(context: Context, name: String): String {
        val db = AppDatabase.get(context)
        val today = LocalDate.now().toEpochDay()
        val sb = StringBuilder()
        if (name.isNotBlank()) sb.append("User name: ").append(name).append("\n")
        val goals = db.goalDao().observeActive().first()
        if (goals.isNotEmpty()) {
            sb.append("Goals: ")
            sb.append(goals.take(10).joinToString("; ") { it.title + " (" + it.horizon + ", " + it.progress + "%)" })
            sb.append("\n")
        }
        val habits = db.habitDao().observeActive().first()
        if (habits.isNotEmpty()) {
            sb.append("Habits: ").append(habits.take(10).joinToString("; ") { it.name }).append("\n")
        }
        val open = db.taskDao().getAll().filter { !it.isDone }
        if (open.isNotEmpty()) {
            sb.append("Open tasks: ")
            sb.append(
                open.take(12).joinToString("; ") {
                    val due = it.dueEpochDay
                    val tag = if (due == null) "" else if (due < today) " (overdue)" else if (due == today) " (today)" else ""
                    it.title + tag
                }
            )
            sb.append("\n")
        }
        return sb.toString()
    }

    /** Blocking call: run it on Dispatchers.IO. Returns the answer or a short error text. */
    fun ask(store: AiStore, question: String, contextText: String): String {
        val key = store.key()
        if (key.isEmpty()) return "Add your Gemini key first."
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/" + store.model() + ":generateContent")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 15000
            conn.readTimeout = 40000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-goog-api-key", key)

            val system = JSONObject().put(
                "parts", JSONArray().put(JSONObject().put("text", SYSTEM_TEXT))
            )
            val userText = if (contextText.isBlank()) question else "Context:\n" + contextText + "\nQuestion: " + question
            val contents = JSONArray().put(
                JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userText)))
            )
            val body = JSONObject().put("systemInstruction", system).put("contents", contents)
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            if (code != 200) {
                return when (code) {
                    400, 403 -> "Key not accepted. Check the key in Settings."
                    404 -> "Model name not found. Change the model name."
                    429 -> "Free limit reached. Try again later."
                    else -> "AI error ($code). Try again."
                }
            }
            val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            val parts = JSONObject(text)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
            val out = StringBuilder()
            for (i in 0 until parts.length()) {
                out.append(parts.getJSONObject(i).optString("text", ""))
            }
            if (out.isEmpty()) "No answer. Try asking differently." else out.toString().trim()
        } catch (e: Exception) {
            "Could not reach AI. Check your internet."
        }
    }
}
