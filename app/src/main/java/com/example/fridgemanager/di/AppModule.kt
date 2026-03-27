package com.example.fridgemanager.di

import android.content.Context
import androidx.room.Room
import com.example.fridgemanager.data.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob())

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        scope: CoroutineScope
    ): AppDatabase {
        lateinit var db: AppDatabase
        db = Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addCallback(AppDatabase.buildCallback(scope) { db })
            .build()
        return db
    }

    @Provides fun provideFoodItemDao(db: AppDatabase) = db.foodItemDao()
    @Provides fun provideCategoryDao(db: AppDatabase) = db.categoryDao()
}
