package com.yubera.app.di

import com.yubera.app.BuildConfig
import com.yubera.app.core.network.RetrofitFactory
import com.yubera.app.core.network.ShikimoriRateLimitInterceptor
import com.yubera.app.core.network.YuberaEndpoints
import com.yubera.app.data.auth.TelegramSessionApi
import com.yubera.app.data.auth.YuberaProfileApi
import com.yubera.app.data.anime.AnimeSocialApi
import com.yubera.app.data.anime.ShikimoriAnimeApi
import com.yubera.app.data.catalog.CatalogApi
import com.yubera.app.data.library.TelegramLibrarySyncApi
import com.yubera.app.data.news.ShikimoriEpisodeUpdatesApi
import com.yubera.app.data.news.ShikimoriNewsApi
import com.yubera.app.data.player.KodikApi
import com.yubera.app.data.support.SupportApi
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
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides
    @Named("shikimoriClient")
    @Singleton
    fun provideShikimoriOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(
                ShikimoriRateLimitInterceptor(
                    userAgent = "$SHIKIMORI_USER_AGENT/${BuildConfig.VERSION_NAME}"
                )
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(factory: RetrofitFactory): Retrofit = factory.create(YUBERA_API_BASE_URL)

    @Provides
    @Named("shikimori")
    @Singleton
    fun provideShikimoriRetrofit(
        factory: RetrofitFactory,
        @Named("shikimoriClient") client: OkHttpClient
    ): Retrofit = factory.create(SHIKIMORI_API_BASE_URL, client)

    @Provides
    fun provideCatalogApi(retrofit: Retrofit): CatalogApi = retrofit.create(CatalogApi::class.java)

    @Provides
    fun provideKodikApi(retrofit: Retrofit): KodikApi = retrofit.create(KodikApi::class.java)

    @Provides
    fun provideSupportApi(retrofit: Retrofit): SupportApi = retrofit.create(SupportApi::class.java)

    @Provides
    fun provideAnimeSocialApi(retrofit: Retrofit): AnimeSocialApi = retrofit.create(AnimeSocialApi::class.java)

    @Provides
    fun provideTelegramSessionApi(retrofit: Retrofit): TelegramSessionApi = retrofit.create(TelegramSessionApi::class.java)

    @Provides
    fun provideYuberaProfileApi(retrofit: Retrofit): YuberaProfileApi = retrofit.create(YuberaProfileApi::class.java)

    @Provides
    fun provideTelegramLibrarySyncApi(retrofit: Retrofit): TelegramLibrarySyncApi =
        retrofit.create(TelegramLibrarySyncApi::class.java)

    @Provides
    fun provideShikimoriAnimeApi(@Named("shikimori") retrofit: Retrofit): ShikimoriAnimeApi =
        retrofit.create(ShikimoriAnimeApi::class.java)

    @Provides
    fun provideShikimoriEpisodeUpdatesApi(@Named("shikimori") retrofit: Retrofit): ShikimoriEpisodeUpdatesApi =
        retrofit.create(ShikimoriEpisodeUpdatesApi::class.java)

    @Provides
    fun provideShikimoriNewsApi(@Named("shikimori") retrofit: Retrofit): ShikimoriNewsApi =
        retrofit.create(ShikimoriNewsApi::class.java)

    const val YUBERA_API_BASE_URL = YuberaEndpoints.API_BASE_URL
    const val SHIKIMORI_API_BASE_URL = "https://shikimori.io/"
    const val SHIKIMORI_USER_AGENT = "YUBERA Android"
}
