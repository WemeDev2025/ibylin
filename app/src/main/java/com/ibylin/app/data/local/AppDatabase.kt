package com.ibylin.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ibylin.app.data.model.User

@Database(
    entities = [User::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
