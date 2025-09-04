package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class LockHistoryRecord(
    val bookName: String,
    val action: String,
    val timestamp: Long,
    val unlockMethod: String? = null
) {
    fun getFormattedTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }
}

object LockHistoryManager {
    
    private const val TAG = "LockHistoryManager"
    private const val PREFS_NAME = "lock_history"
    private const val KEY_HISTORY = "history"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    fun addHistory(context: Context, record: LockHistoryRecord) {
        try {
            val prefs = getPrefs(context)
            val historyJson = prefs.getString(KEY_HISTORY, "[]")
            val historyArray = JSONArray(historyJson)
            
            val recordJson = JSONObject().apply {
                put("bookName", record.bookName)
                put("action", record.action)
                put("timestamp", record.timestamp)
                record.unlockMethod?.let { put("unlockMethod", it) }
            }
            
            historyArray.put(recordJson)
            prefs.edit().putString(KEY_HISTORY, historyArray.toString()).apply()
            Log.d(TAG, "历史记录已添加: ${record.bookName} - ${record.action}")
            
        } catch (e: Exception) {
            Log.e(TAG, "添加历史记录失败", e)
        }
    }
    
    fun getAllHistory(context: Context): List<LockHistoryRecord> {
        val records = mutableListOf<LockHistoryRecord>()
        try {
            val prefs = getPrefs(context)
            val historyJson = prefs.getString(KEY_HISTORY, "[]")
            val historyArray = JSONArray(historyJson)
            
            for (i in 0 until historyArray.length()) {
                val recordJson = historyArray.getJSONObject(i)
                val record = LockHistoryRecord(
                    bookName = recordJson.getString("bookName"),
                    action = recordJson.getString("action"),
                    timestamp = recordJson.getLong("timestamp"),
                    unlockMethod = if (recordJson.has("unlockMethod")) recordJson.getString("unlockMethod") else null
                )
                records.add(record)
            }
            
            records.sortByDescending { it.timestamp }
            
        } catch (e: Exception) {
            Log.e(TAG, "获取历史记录失败", e)
        }
        return records
    }
}
