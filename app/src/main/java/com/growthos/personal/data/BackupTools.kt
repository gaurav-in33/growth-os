package com.growthos.personal.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

object BackupTools {

    private fun tableNames(db: SupportSQLiteDatabase): List<String> {
        val names = mutableListOf<String>()
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                names.add(cursor.getString(0))
            }
        }
        return names
    }

    fun buildBackup(context: Context): String {
        val db = AppDatabase.get(context).openHelper.readableDatabase
        val root = JSONObject()
        root.put("app", "growth-os")
        root.put("dbVersion", db.version)
        root.put("exportedAtMillis", System.currentTimeMillis())
        val tables = JSONObject()
        for (table in tableNames(db)) {
            val rows = JSONArray()
            db.query("SELECT * FROM `" + table + "`").use { cursor ->
                while (cursor.moveToNext()) {
                    val row = JSONObject()
                    for (i in 0 until cursor.columnCount) {
                        val name = cursor.getColumnName(i)
                        when (cursor.getType(i)) {
                            Cursor.FIELD_TYPE_NULL -> row.put(name, JSONObject.NULL)
                            Cursor.FIELD_TYPE_INTEGER -> row.put(name, cursor.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> row.put(name, cursor.getDouble(i))
                            else -> row.put(name, cursor.getString(i))
                        }
                    }
                    rows.put(row)
                }
            }
            tables.put(table, rows)
        }
        root.put("tables", tables)
        return root.toString()
    }

    fun restoreBackup(context: Context, text: String): String {
        val root = try {
            JSONObject(text)
        } catch (e: Exception) {
            return "This file is not a valid backup."
        }
        if (root.optString("app") != "growth-os") {
            return "This file is not a Growth OS backup."
        }
        val db = AppDatabase.get(context).openHelper.writableDatabase
        if (root.optInt("dbVersion", -1) != db.version) {
            return "This backup was made with a different app version, so it cannot be restored."
        }
        val tablesJson = root.optJSONObject("tables") ?: return "This backup has no data."
        val known = tableNames(db).toSet()
        val columnName = Regex("^[A-Za-z0-9_]+\$")

        val parsed = mutableListOf<Pair<String, List<ContentValues>>>()
        val tableKeys = tablesJson.keys()
        while (tableKeys.hasNext()) {
            val table = tableKeys.next()
            if (!known.contains(table)) continue
            val arr = tablesJson.optJSONArray(table) ?: return "The backup file is damaged."
            val list = mutableListOf<ContentValues>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: return "The backup file is damaged."
                val cv = ContentValues()
                val cols = obj.keys()
                while (cols.hasNext()) {
                    val col = cols.next()
                    if (!columnName.matches(col)) return "The backup file is damaged."
                    val value = obj.get(col)
                    if (value == JSONObject.NULL) {
                        cv.putNull(col)
                    } else if (value is Int) {
                        cv.put(col, value.toLong())
                    } else if (value is Long) {
                        cv.put(col, value)
                    } else if (value is Double) {
                        cv.put(col, value)
                    } else if (value is String) {
                        cv.put(col, value)
                    } else {
                        return "The backup file is damaged."
                    }
                }
                list.add(cv)
            }
            parsed.add(Pair(table, list))
        }
        if (parsed.isEmpty()) return "This backup has no data."

        db.beginTransaction()
        try {
            for (item in parsed) {
                db.delete(item.first, null, null)
                for (cv in item.second) {
                    db.insert(item.first, SQLiteDatabase.CONFLICT_REPLACE, cv)
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            return "Restore failed. Your current data was kept."
        } finally {
            db.endTransaction()
        }
        return "Restore done. Close the app fully and open it again."
    }
}
