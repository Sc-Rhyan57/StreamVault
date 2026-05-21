package com.streamvault.di

import android.content.Context
import androidx.room.Room
import com.streamvault.data.local.AppPreferences
import com.streamvault.data.local.StreamVaultDatabase
import com.streamvault.data.local.WatchProgressDao
import com.streamvault.data.local.WatchlistDao
import com.streamvault.data.remote.StreamApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://placeholder.api/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides @Singleton
    fun provideApiService(retrofit: Retrofit): StreamApiService =
        retrofit.create(StreamApiService::class.java)

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): StreamVaultDatabase =
        Room.databaseBuilder(ctx, StreamVaultDatabase::class.java, "streamvault.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideProgressDao(db: StreamVaultDatabase): WatchProgressDao = db.watchProgressDao()
    @Provides fun provideWatchlistDao(db: StreamVaultDatabase): WatchlistDao = db.watchlistDao()

    @Provides @Singleton
    fun providePreferences(@ApplicationContext ctx: Context): AppPreferences = AppPreferences(ctx)
}
