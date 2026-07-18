package com.vi5hnu.calculator.data.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.vi5hnu.calculator.data.local.HistoryDao
import com.vi5hnu.calculator.data.local.MathProDatabase
import com.vi5hnu.calculator.data.repository.HistoryRepositoryImpl
import com.vi5hnu.calculator.data.repository.SettingsRepositoryImpl
import com.vi5hnu.calculator.domain.repository.HistoryRepository
import com.vi5hnu.calculator.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore("settings")

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MathProDatabase =
        Room.databaseBuilder(context, MathProDatabase::class.java, MathProDatabase.NAME).build()

    @Provides
    fun provideHistoryDao(database: MathProDatabase): HistoryDao = database.historyDao()

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}
