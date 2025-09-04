package com.ibylin.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 阅读分析管理器
 * 负责记录用户阅读时长、频率，并智能预测阅读完成时间
 */
object ReadingAnalyticsManager {
    
    private const val TAG = "ReadingAnalyticsManager"
    private const val PREFS_NAME = "reading_analytics"
    private const val KEY_READING_SESSIONS = "reading_sessions"
    private const val KEY_DAILY_STATS = "daily_stats"
    private const val KEY_BOOK_PREDICTIONS = "book_predictions"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 开始阅读会话
     */
    fun startReadingSession(
        context: Context,
        bookPath: String,
        bookName: String,
        startPosition: Double
    ): String {
        val sessionId = generateSessionId()
        val startTime = System.currentTimeMillis()
        
        val session = ReadingSession(
            sessionId = sessionId,
            bookPath = bookPath,
            bookName = bookName,
            startTime = startTime,
            startPosition = startPosition,
            endTime = null,
            endPosition = null,
            duration = 0,
            isActive = true
        )
        
        saveReadingSession(context, session)
        Log.d(TAG, "开始阅读会话: $bookName, 位置: $startPosition")
        
        return sessionId
    }
    
    /**
     * 结束阅读会话
     */
    fun endReadingSession(
        context: Context,
        sessionId: String,
        endPosition: Double
    ) {
        try {
            val prefs = getPrefs(context)
            val sessionsJson = prefs.getString(KEY_READING_SESSIONS, "[]")
            val sessionsArray = JSONArray(sessionsJson)
            
            for (i in 0 until sessionsArray.length()) {
                val sessionJson = sessionsArray.getJSONObject(i)
                if (sessionJson.getString("sessionId") == sessionId) {
                    val endTime = System.currentTimeMillis()
                    val startTime = sessionJson.getLong("startTime")
                    val duration = endTime - startTime
                    
                    val updatedSession = ReadingSession(
                        sessionId = sessionId,
                        bookPath = sessionJson.getString("bookPath"),
                        bookName = sessionJson.getString("bookName"),
                        startTime = startTime,
                        startPosition = sessionJson.getDouble("startPosition"),
                        endTime = endTime,
                        endPosition = endPosition,
                        duration = duration,
                        isActive = false
                    )
                    
                    // 更新会话
                    sessionsArray.put(i, updatedSession.toJSON())
                    prefs.edit().putString(KEY_READING_SESSIONS, sessionsArray.toString()).apply()
                    
                    // 更新每日统计
                    updateDailyStats(context, updatedSession)
                    
                    Log.d(TAG, "结束阅读会话: ${updatedSession.bookName}, 时长: ${duration / 1000}s")
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "结束阅读会话失败", e)
        }
    }
    
    /**
     * 获取图书的阅读统计
     */
    fun getBookReadingStats(context: Context, bookPath: String): BookReadingStats? {
        return try {
            val sessions = getAllReadingSessions(context)
            val bookSessions = sessions.filter { it.bookPath == bookPath && !it.isActive }
            
            if (bookSessions.isEmpty()) return null
            
            val totalDuration = bookSessions.sumOf { it.duration }
            val totalSessions = bookSessions.size
            val avgSessionDuration = totalDuration / totalSessions
            val firstReadTime = bookSessions.minOf { it.startTime }
            val lastReadTime = bookSessions.maxOf { it.endTime ?: it.startTime }
            
            // 计算阅读速度（每分钟阅读的进度百分比）
            val totalProgress = bookSessions.sumOf { 
                (it.endPosition ?: it.startPosition) - it.startPosition 
            }
            val readingSpeed = if (totalDuration > 0) {
                (totalProgress / (totalDuration / 60000.0)) // 每分钟的进度
            } else 0.0
            
            BookReadingStats(
                bookPath = bookPath,
                bookName = bookSessions.first().bookName,
                totalReadingTime = totalDuration,
                totalSessions = totalSessions,
                averageSessionDuration = avgSessionDuration,
                firstReadTime = firstReadTime,
                lastReadTime = lastReadTime,
                readingSpeed = readingSpeed,
                totalProgress = totalProgress
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取图书阅读统计失败", e)
            null
        }
    }
    
    /**
     * 智能预测阅读完成时间
     */
    fun predictReadingCompletion(
        context: Context,
        bookPath: String,
        currentPosition: Double
    ): ReadingPrediction? {
        return try {
            val stats = getBookReadingStats(context, bookPath) ?: return null
            
            // 计算剩余进度
            val remainingProgress = 1.0 - currentPosition
            if (remainingProgress <= 0) return null
            
            // 基于历史阅读速度预测
            val estimatedMinutes = if (stats.readingSpeed > 0) {
                (remainingProgress / stats.readingSpeed).toLong()
            } else {
                // 如果没有足够的历史数据，使用默认速度
                (remainingProgress * 120).toLong() // 假设2小时读完剩余内容
            }
            
            // 考虑用户阅读习惯
            val dailyReadingTime = getDailyAverageReadingTime(context)
            val estimatedDays = if (dailyReadingTime > 0) {
                (estimatedMinutes * 60 * 1000) / dailyReadingTime
            } else {
                estimatedMinutes / 30 // 假设每天30分钟
            }
            
            val estimatedCompletionTime = System.currentTimeMillis() + (estimatedMinutes * 60 * 1000)
            
            ReadingPrediction(
                bookPath = bookPath,
                bookName = stats.bookName,
                currentPosition = currentPosition,
                remainingProgress = remainingProgress,
                estimatedMinutes = estimatedMinutes,
                estimatedDays = estimatedDays,
                estimatedCompletionTime = estimatedCompletionTime,
                confidence = calculatePredictionConfidence(stats)
            )
        } catch (e: Exception) {
            Log.e(TAG, "预测阅读完成时间失败", e)
            null
        }
    }
    
    /**
     * 获取每日平均阅读时间
     */
    private fun getDailyAverageReadingTime(context: Context): Long {
        return try {
            val prefs = getPrefs(context)
            val dailyStatsJson = prefs.getString(KEY_DAILY_STATS, "[]")
            val dailyStatsArray = JSONArray(dailyStatsJson)
            
            if (dailyStatsArray.length() == 0) return 0
            
            var totalTime = 0L
            var totalDays = 0
            
            for (i in 0 until dailyStatsArray.length()) {
                val dayStats = dailyStatsArray.getJSONObject(i)
                val readingTime = dayStats.getLong("readingTime")
                if (readingTime > 0) {
                    totalTime += readingTime
                    totalDays++
                }
            }
            
            if (totalDays > 0) totalTime / totalDays else 0
        } catch (e: Exception) {
            Log.e(TAG, "获取每日平均阅读时间失败", e)
            0
        }
    }
    
    /**
     * 计算预测置信度
     */
    private fun calculatePredictionConfidence(stats: BookReadingStats): Double {
        // 基于历史数据量计算置信度
        val sessionWeight = minOf(stats.totalSessions / 10.0, 1.0) // 最多10个会话
        val timeWeight = minOf(stats.totalReadingTime / (30 * 60 * 1000.0), 1.0) // 最多30分钟
        
        return (sessionWeight + timeWeight) / 2.0
    }
    
    /**
     * 获取用户阅读习惯分析
     */
    fun getUserReadingHabits(context: Context): UserReadingHabits {
        return try {
            val sessions = getAllReadingSessions(context)
            val activeSessions = sessions.filter { !it.isActive }
            
            if (activeSessions.isEmpty()) {
                return UserReadingHabits(
                    totalReadingTime = 0,
                    averageSessionDuration = 0,
                    favoriteReadingTime = null,
                    totalBooks = 0,
                    averageDailyReading = 0
                )
            }
            
            val totalReadingTime = activeSessions.sumOf { it.duration }
            val averageSessionDuration = totalReadingTime / activeSessions.size
            val uniqueBooks = activeSessions.map { it.bookPath }.distinct().size
            
            // 分析最喜欢的阅读时间
            val hourStats = mutableMapOf<Int, Long>()
            activeSessions.forEach { session ->
                val hour = Calendar.getInstance().apply { timeInMillis = session.startTime }.get(Calendar.HOUR_OF_DAY)
                hourStats[hour] = (hourStats[hour] ?: 0) + session.duration
            }
            val favoriteHour = hourStats.maxByOrNull { it.value }?.key
            
            // 计算每日平均阅读时间
            val dailyStats = getDailyReadingStats(context)
            val averageDailyReading = if (dailyStats.isNotEmpty()) {
                dailyStats.values.average().toLong()
            } else 0
            
            UserReadingHabits(
                totalReadingTime = totalReadingTime,
                averageSessionDuration = averageSessionDuration,
                favoriteReadingTime = favoriteHour,
                totalBooks = uniqueBooks,
                averageDailyReading = averageDailyReading
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取用户阅读习惯失败", e)
            UserReadingHabits()
        }
    }
    
    /**
     * 获取每日阅读统计
     */
    private fun getDailyReadingStats(context: Context): Map<String, Long> {
        return try {
            val prefs = getPrefs(context)
            val dailyStatsJson = prefs.getString(KEY_DAILY_STATS, "[]")
            val dailyStatsArray = JSONArray(dailyStatsJson)
            
            val dailyStats = mutableMapOf<String, Long>()
            for (i in 0 until dailyStatsArray.length()) {
                val dayStats = dailyStatsArray.getJSONObject(i)
                val date = dayStats.getString("date")
                val readingTime = dayStats.getLong("readingTime")
                dailyStats[date] = readingTime
            }
            dailyStats
        } catch (e: Exception) {
            Log.e(TAG, "获取每日阅读统计失败", e)
            emptyMap()
        }
    }
    
    /**
     * 更新每日统计
     */
    private fun updateDailyStats(context: Context, session: ReadingSession) {
        try {
            val prefs = getPrefs(context)
            val dailyStatsJson = prefs.getString(KEY_DAILY_STATS, "[]")
            val dailyStatsArray = JSONArray(dailyStatsJson)
            
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sessionDate = dateFormat.format(Date(session.startTime))
            
            var found = false
            for (i in 0 until dailyStatsArray.length()) {
                val dayStats = dailyStatsArray.getJSONObject(i)
                if (dayStats.getString("date") == sessionDate) {
                    val currentTime = dayStats.getLong("readingTime")
                    dayStats.put("readingTime", currentTime + session.duration)
                    found = true
                    break
                }
            }
            
            if (!found) {
                val newDayStats = JSONObject().apply {
                    put("date", sessionDate)
                    put("readingTime", session.duration)
                }
                dailyStatsArray.put(newDayStats)
            }
            
            prefs.edit().putString(KEY_DAILY_STATS, dailyStatsArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "更新每日统计失败", e)
        }
    }
    
    /**
     * 保存阅读会话
     */
    private fun saveReadingSession(context: Context, session: ReadingSession) {
        try {
            val prefs = getPrefs(context)
            val sessionsJson = prefs.getString(KEY_READING_SESSIONS, "[]")
            val sessionsArray = JSONArray(sessionsJson)
            
            sessionsArray.put(session.toJSON())
            prefs.edit().putString(KEY_READING_SESSIONS, sessionsArray.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "保存阅读会话失败", e)
        }
    }
    
    /**
     * 获取所有阅读会话
     */
    private fun getAllReadingSessions(context: Context): List<ReadingSession> {
        return try {
            val prefs = getPrefs(context)
            val sessionsJson = prefs.getString(KEY_READING_SESSIONS, "[]")
            val sessionsArray = JSONArray(sessionsJson)
            
            val sessions = mutableListOf<ReadingSession>()
            for (i in 0 until sessionsArray.length()) {
                val sessionJson = sessionsArray.getJSONObject(i)
                sessions.add(ReadingSession.fromJSON(sessionJson))
            }
            sessions
        } catch (e: Exception) {
            Log.e(TAG, "获取阅读会话失败", e)
            emptyList()
        }
    }
    
    /**
     * 生成会话ID
     */
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${Random().nextInt(10000)}"
    }
    
    /**
     * 清理过期数据
     */
    fun cleanupOldData(context: Context, daysToKeep: Int = 90) {
        try {
            val cutoffTime = System.currentTimeMillis() - (daysToKeep * 24 * 60 * 60 * 1000L)
            val prefs = getPrefs(context)
            
            // 清理过期会话
            val sessionsJson = prefs.getString(KEY_READING_SESSIONS, "[]")
            val sessionsArray = JSONArray(sessionsJson)
            val validSessions = mutableListOf<JSONObject>()
            
            for (i in 0 until sessionsArray.length()) {
                val session = sessionsArray.getJSONObject(i)
                val startTime = session.getLong("startTime")
                if (startTime > cutoffTime) {
                    validSessions.add(session)
                }
            }
            
            val newSessionsArray = JSONArray(validSessions)
            prefs.edit().putString(KEY_READING_SESSIONS, newSessionsArray.toString()).apply()
            
            Log.d(TAG, "清理过期数据完成，保留 ${validSessions.size} 个会话")
        } catch (e: Exception) {
            Log.e(TAG, "清理过期数据失败", e)
        }
    }
}

/**
 * 阅读会话数据
 */
data class ReadingSession(
    val sessionId: String,
    val bookPath: String,
    val bookName: String,
    val startTime: Long,
    val startPosition: Double,
    val endTime: Long?,
    val endPosition: Double?,
    val duration: Long,
    val isActive: Boolean
) {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("bookPath", bookPath)
            put("bookName", bookName)
            put("startTime", startTime)
            put("startPosition", startPosition)
            put("endTime", endTime)
            put("endPosition", endPosition)
            put("duration", duration)
            put("isActive", isActive)
        }
    }
    
    companion object {
        fun fromJSON(json: JSONObject): ReadingSession {
            return ReadingSession(
                sessionId = json.getString("sessionId"),
                bookPath = json.getString("bookPath"),
                bookName = json.getString("bookName"),
                startTime = json.getLong("startTime"),
                startPosition = json.getDouble("startPosition"),
                endTime = json.optLong("endTime"),
                endPosition = json.optDouble("endPosition"),
                duration = json.getLong("duration"),
                isActive = json.getBoolean("isActive")
            )
        }
    }
}

/**
 * 图书阅读统计
 */
data class BookReadingStats(
    val bookPath: String,
    val bookName: String,
    val totalReadingTime: Long,
    val totalSessions: Int,
    val averageSessionDuration: Long,
    val firstReadTime: Long,
    val lastReadTime: Long,
    val readingSpeed: Double,
    val totalProgress: Double
) {
    fun getFormattedTotalTime(): String {
        val hours = totalReadingTime / (60 * 60 * 1000)
        val minutes = (totalReadingTime % (60 * 60 * 1000)) / (60 * 1000)
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            else -> "${minutes}分钟"
        }
    }
    
    fun getFormattedAverageSession(): String {
        val minutes = averageSessionDuration / (60 * 1000)
        return "${minutes}分钟"
    }
    
    fun getFormattedLastReadTime(): String {
        val currentTime = System.currentTimeMillis()
        val diff = currentTime - lastReadTime
        
        return when {
            diff < 60 * 1000 -> "刚刚"
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}分钟前"
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}小时前"
            diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}天前"
            else -> {
                val date = Date(lastReadTime)
                val formatter = SimpleDateFormat("MM-dd", Locale.getDefault())
                formatter.format(date)
            }
        }
    }
}

/**
 * 阅读预测
 */
data class ReadingPrediction(
    val bookPath: String,
    val bookName: String,
    val currentPosition: Double,
    val remainingProgress: Double,
    val estimatedMinutes: Long,
    val estimatedDays: Long,
    val estimatedCompletionTime: Long,
    val confidence: Double
) {
    fun getFormattedEstimatedTime(): String {
        return when {
            estimatedDays > 0 -> "约${estimatedDays}天"
            estimatedMinutes > 60 -> "约${estimatedMinutes / 60}小时"
            else -> "约${estimatedMinutes}分钟"
        }
    }
    
    fun getFormattedCompletionDate(): String {
        val date = Date(estimatedCompletionTime)
        val formatter = SimpleDateFormat("MM月dd日", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getConfidenceText(): String {
        return when {
            confidence >= 0.8 -> "高"
            confidence >= 0.5 -> "中"
            else -> "低"
        }
    }
}

/**
 * 用户阅读习惯
 */
data class UserReadingHabits(
    val totalReadingTime: Long = 0,
    val averageSessionDuration: Long = 0,
    val favoriteReadingTime: Int? = null,
    val totalBooks: Int = 0,
    val averageDailyReading: Long = 0
) {
    fun getFormattedTotalTime(): String {
        val hours = totalReadingTime / (60 * 60 * 1000)
        val minutes = (totalReadingTime % (60 * 60 * 1000)) / (60 * 1000)
        return when {
            hours > 0 -> "${hours}小时${minutes}分钟"
            else -> "${minutes}分钟"
        }
    }
    
    fun getFormattedAverageSession(): String {
        val minutes = averageSessionDuration / (60 * 1000)
        return "${minutes}分钟"
    }
    
    fun getFormattedDailyAverage(): String {
        val minutes = averageDailyReading / (60 * 1000)
        return "${minutes}分钟"
    }
    
    fun getFavoriteTimeText(): String {
        return favoriteReadingTime?.let { hour ->
            when {
                hour < 6 -> "凌晨${hour}点"
                hour < 12 -> "上午${hour}点"
                hour < 18 -> "下午${hour}点"
                else -> "晚上${hour}点"
            }
        } ?: "暂无数据"
    }
}
