package com.ibylin.app.di

import android.content.Context
import androidx.room.Room
import com.ibylin.app.data.local.AppDatabase
import com.ibylin.app.data.local.UserDao
import com.ibylin.app.data.repository.UserRepository
import com.ibylin.app.data.repository.UserRepositoryImpl
import com.ibylin.app.utils.ReadiumPreferencesManager
import com.ibylin.app.utils.ReadiumHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "ibylin_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }
    
    @Provides
    @Singleton
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepositoryImpl(userDao)
    }
    
    @Provides
    @Singleton
    fun provideReadiumPreferencesManager(): ReadiumPreferencesManager {
        return ReadiumPreferencesManager(ReadiumPreferencesManager.getDefaultPreferences())
    }
    

}
