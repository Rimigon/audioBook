package com.nikit.audiobook.app.di

import android.content.Context
import com.nikit.audiobook.data.db.dao.MetadataCacheDao
import com.nikit.audiobook.data.saf.AndroidFileDeleter
import com.nikit.audiobook.data.saf.FileDeleter
import com.nikit.audiobook.data.saf.FolderScanner
import com.nikit.audiobook.data.saf.ScanSettings
import com.nikit.audiobook.metadata.online.GoogleBooksSource
import com.nikit.audiobook.metadata.online.MetadataEnricher
import com.nikit.audiobook.metadata.online.OnlineMetadataSource
import com.nikit.audiobook.metadata.online.OpenLibrarySource
import com.nikit.audiobook.metadata.tags.AndroidTagReader
import com.nikit.audiobook.metadata.tags.TagReader
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScanMetadataModule {
    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

    @Provides @Singleton
    fun provideFolderScanner(
        @ApplicationContext ctx: Context,
    ): FolderScanner = FolderScanner(ctx)

    @Provides @Singleton
    fun provideScanSettings(
        @ApplicationContext ctx: Context,
    ): ScanSettings = ScanSettings(ctx)

    @Provides @Singleton
    fun provideMetadataEnricher(
        cache: MetadataCacheDao,
        sources: @JvmSuppressWildcards Set<OnlineMetadataSource>,
    ): MetadataEnricher = MetadataEnricher(cache, sources.toList())
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ScanMetadataBindingsModule {
    @Binds
    abstract fun bindTagReader(impl: AndroidTagReader): TagReader

    @Binds
    abstract fun bindFileDeleter(impl: AndroidFileDeleter): FileDeleter

    companion object {
        @Provides @IntoSet
        fun provideOpenLibrary(client: OkHttpClient): OnlineMetadataSource = OpenLibrarySource(client)

        @Provides @IntoSet
        fun provideGoogleBooks(client: OkHttpClient): OnlineMetadataSource = GoogleBooksSource(client)
    }
}
