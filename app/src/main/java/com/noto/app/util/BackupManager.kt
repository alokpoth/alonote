package com.noto.app.util

import com.noto.app.data.local.entity.Note
import org.json.JSONArray
import org.json.JSONObject

/**
 * 100% Offline, Zero-Dependency JSON Backup & Restore Manager.
 * Serializes and deserializes the entire note library using native Android org.json.
 */
object BackupManager {

    fun exportToJson(notes: List<Note>): String {
        val root = JSONObject().apply {
            put("app", "Noto")
            put("version", 4)
            put("exportedAt", System.currentTimeMillis())
            put("noteCount", notes.size)

            val notesArray = JSONArray()
            notes.forEach { note ->
                val noteObj = JSONObject().apply {
                    put("id", note.id)
                    put("title", note.title)
                    put("body", note.body)
                    put("isFavorite", note.isFavorite)
                    put("isPinned", note.isPinned)
                    put("isDeleted", note.isDeleted)
                    if (note.deletedAt != null) put("deletedAt", note.deletedAt)
                    put("imagePaths", note.imagePaths)
                    put("audioPaths", note.audioPaths)
                    if (note.reminderTime != null) put("reminderTime", note.reminderTime)
                    put("createdAt", note.createdAt)
                    put("updatedAt", note.updatedAt)
                }
                notesArray.put(noteObj)
            }
            put("notes", notesArray)
        }
        return root.toString(2)
    }

    fun parseFromJson(jsonString: String): List<Note> {
        val root = JSONObject(jsonString)
        val notesArray = root.optJSONArray("notes") ?: return emptyList()

        val list = mutableListOf<Note>()
        for (i in 0 until notesArray.length()) {
            val obj = notesArray.optJSONObject(i) ?: continue
            val note = Note(
                id = obj.optLong("id", 0L),
                title = obj.optString("title", ""),
                body = obj.optString("body", ""),
                isFavorite = obj.optBoolean("isFavorite", false),
                isPinned = obj.optBoolean("isPinned", false),
                isDeleted = obj.optBoolean("isDeleted", false),
                deletedAt = if (obj.has("deletedAt") && !obj.isNull("deletedAt")) obj.optLong("deletedAt") else null,
                imagePaths = obj.optString("imagePaths", ""),
                audioPaths = obj.optString("audioPaths", ""),
                reminderTime = if (obj.has("reminderTime") && !obj.isNull("reminderTime")) obj.optLong("reminderTime") else null,
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            )
            list.add(note)
        }
        return list
    }
}
