package org.lumengallery.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import org.lumengallery.app.data.local.MediaDatabase
import org.lumengallery.app.data.privacy.ExifMetadataManager
import org.lumengallery.app.data.repository.MediaStoreRepository

class LumenApplication : Application(), ImageLoaderFactory {

    lateinit var database: MediaDatabase
        private set

    lateinit var mediaStoreRepository: MediaStoreRepository
        private set

    lateinit var exifMetadataManager: ExifMetadataManager
        private set

    override fun attachBaseContext(base: android.content.Context) {
        super.attachBaseContext(base)
        org.lumengallery.app.util.LocaleHelper.applyLocale(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MediaDatabase.getInstance(this)
        mediaStoreRepository = MediaStoreRepository(this, database.favoriteDao())
        exifMetadataManager = ExifMetadataManager(this)
        org.lumengallery.app.util.LocaleHelper.init(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(64L * 1024 * 1024) // 64 MB local thumbnail cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // 100% offline, local storage only
            .build()
    }

    companion object {
        lateinit var instance: LumenApplication
            private set
    }
}
