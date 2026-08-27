package com.auracast.weather.di

import android.content.Context
import androidx.room.Room
import com.auracast.weather.data.location.GeocodingApi
import com.auracast.weather.data.location.GeocodingApiRetrofit
import com.auracast.weather.data.radar.RainViewerApiRetrofit
import com.auracast.weather.data.weather.NwsApi
import com.auracast.weather.data.weather.OpenMeteoApi
import com.auracast.weather.data.weather.WeatherDatabase
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    // Legacy stub kept for Hilt graph compatibility — Phase 2 migrates callers to GeocodingApiRetrofit
    @Provides @Singleton
    fun provideGeocodingApi(): GeocodingApi = GeocodingApi()

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        // Phase 9 — no manual interceptor needed: the com.google.firebase.firebase-perf Gradle
        // plugin (applied in app/build.gradle.kts) auto-instruments HTTP calls, including
        // OkHttp, via ASM bytecode transformation at build time.
        .build()

    @Provides @Singleton
    fun provideOpenMeteoApi(okHttp: OkHttpClient, json: Json): OpenMeteoApi =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenMeteoApi::class.java)

    @Provides @Singleton
    fun provideGeocodingRetrofit(okHttp: OkHttpClient, json: Json): GeocodingApiRetrofit =
        Retrofit.Builder()
            .baseUrl("https://geocoding-api.open-meteo.com/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(GeocodingApiRetrofit::class.java)

    @Provides @Singleton
    fun provideNwsApi(okHttp: OkHttpClient, json: Json): NwsApi =
        Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NwsApi::class.java)

    @Provides @Singleton
    fun provideRainViewerApi(okHttp: OkHttpClient, json: Json): RainViewerApiRetrofit =
        Retrofit.Builder()
            .baseUrl("https://api.rainviewer.com/")
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RainViewerApiRetrofit::class.java)

    @Provides @Singleton
    fun provideWeatherDb(@ApplicationContext ctx: Context): WeatherDatabase =
        Room.databaseBuilder(ctx, WeatherDatabase::class.java, "weather.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides fun provideWeatherCacheDao(db: WeatherDatabase) = db.cacheDao()
}
