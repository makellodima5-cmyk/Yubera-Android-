package com.yubera.app.di

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import com.yubera.app.core.network.RetrofitFactory
import com.yubera.app.data.catalog.CatalogApi
import com.yubera.app.data.player.KodikApi
import retrofit2.Retrofit
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Singleton
    fun provideRetrofit(factory: RetrofitFactory): Retrofit = factory.create(YUBERA_API_BASE_URL)

    @Provides
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    fun provideKodikApi(retrofit: Retrofit): KodikApi = retrofit.create(KodikApi::class.java)

    const val YUBERA_API_BASE_URL = "https://yume-website.yumesupport833.workers.dev/"
}
