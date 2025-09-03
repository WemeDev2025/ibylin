package com.ibylin.app.di

import android.content.Context
import androidx.room.Room
import com.ibylin.app.data.local.AppDatabase
import com.ibylin.app.data.local.UserDao
import com.ibylin.app.data.repository.UserRepository
import com.ibylin.app.data.repository.UserRepositoryImpl
import com.ibylin.app.utils.LibreraConfig
import com.ibylin.app.utils.LibreraHelper
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
    fun provideLibreraConfig(
        @ApplicationContext context: Context
    ): LibreraConfig {
        return LibreraConfig(context)
    }
    
    @Provides
    @Singleton
    fun provideLibreraHelper(
        @ApplicationContext context: Context,
        config: LibreraConfig
    ): LibreraHelper {
        return LibreraHelper(context, config)
    }
}
