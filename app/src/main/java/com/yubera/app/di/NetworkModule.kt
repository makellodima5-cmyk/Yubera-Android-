package com.yubera.app.di

import com.yubera.app.core.network.RetrofitFactory
import com.yubera.app.data.catalog.CatalogApi
import com.yubera.app.data.news.ShikimoriNewsApi
import com.yubera.app.data.news.ShikimoriEpisodeUpdatesApi
import com.yubera.app.data.player.KodikApi
import com.yubera.app.data.auth.TelegramSessionApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import retrofit2.Retrofit

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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", SHIKIMORI_USER_AGENT)
                .build()
            chain.proceed(request)
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(factory: RetrofitFactory): Retrofit = factory.create(YUBERA_API_BASE_URL)

    @Provides
    @Named("shikimori")
    @Singleton
    fun provideShikimoriRetrofit(factory: RetrofitFactory): Retrofit = factory.create(SHIKIMORI_API_BASE_URL)

    @Provides
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    fun provideKodikApi(retrofit: Retrofit): KodikApi = retrofit.create(KodikApi::class.java)

    @Provides
    fun provideTelegramSessionApi(retrofit: Retrofit): TelegramSessionApi = retrofit.create(TelegramSessionApi::class.java)

    @Provides
    fun provideShikimoriEpisodeUpdatesApi(@Named("shikimori") retrofit: Retrofit): ShikimoriEpisodeUpdatesApi =
        retrofit.create(ShikimoriEpisodeUpdatesApi::class.java)

    @Provides
    fun provideShikimoriNewsApi(@Named("shikimori") retrofit: Retrofit): ShikimoriNewsApi =
        retrofit.create(ShikimoriNewsApi::class.java)

    const val YUBERA_API_BASE_URL = "https://yume-website.yumesupport833.workers.dev/"
    const val SHIKIMORI_API_BASE_URL = "https://shikimori.io/"
    const val SHIKIMORI_USER_AGENT = "YUBERA Android"
}
