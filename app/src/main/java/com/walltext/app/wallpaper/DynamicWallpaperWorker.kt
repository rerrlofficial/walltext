package com.walltext.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.walltext.app.data.WallTextStore
import com.walltext.app.model.DisplayMode
import kotlinx.coroutines.flow.firstOrNull
import java.io.ByteArrayOutputStream

class DynamicWallpaperWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {

        val store =
            WallTextStore(applicationContext)

        val config =
            store.current.firstOrNull()
                ?: return Result.success()

        val dynamicPlaceholders =
            config.placeholders.filter {
                it.mode == DisplayMode.DYNAMIC &&
                    it.normalizedEntries().isNotEmpty()
            }

        if (dynamicPlaceholders.isEmpty()) {
            return Result.success()
        }

        val baseBitmap =
            if (config.wallpaperUri != null) {

                applicationContext
                    .contentResolver
                    .openInputStream(
                        Uri.parse(config.wallpaperUri)
                    )
                    ?.use {
                        BitmapFactory.decodeStream(it)
                    }

            } else {
                null
            }

        val bitmap =
            baseBitmap
                ?: Bitmap.createBitmap(
                    1080,
                    2400,
                    Bitmap.Config.ARGB_8888
                ).also {
                    it.eraseColor(
                        config.wallpaperColor.toInt()
                    )
                }

        val interval =
            config.dynamicIntervalMinutes
                .coerceAtLeast(15)

        val index =
            (
                System.currentTimeMillis() /
                    (interval * 60_000L)
                ).toInt()

        val rendered =
            WallpaperRenderer.render(
                base = bitmap,
                config = config,
                dynamicIndex = index
            )

        val output =
            ByteArrayOutputStream()

        rendered.compress(
            Bitmap.CompressFormat.PNG,
            100,
            output
        )

        WallpaperManager
            .getInstance(applicationContext)
            .setStream(
                output.toByteArray().inputStream(),
                null,
                true,
                WallpaperManager.FLAG_LOCK
            )

        return Result.success()
    }
}