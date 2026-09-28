package com.walltext.app.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.firstOrNull
import com.walltext.app.data.WallTextStore
import java.io.ByteArrayOutputStream

class DynamicWallpaperWorker(appContext: Context, params: WorkerParameters): CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val config = WallTextStore(applicationContext).current.firstOrNull() ?: return Result.success()
        if (config.placeholders.none { it.mode.name == "DYNAMIC" }) return Result.success()
        val base = if(config.wallpaperUri!=null) applicationContext.contentResolver.openInputStream(Uri.parse(config.wallpaperUri))?.use{BitmapFactory.decodeStream(it)} else null
        val bitmap = base ?: Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888).also{it.eraseColor(config.wallpaperColor.toInt())}
        val index = ((System.currentTimeMillis()/60000L).toInt() / config.dynamicIntervalMinutes).coerceAtLeast(0)
        val rendered = WallpaperRenderer.render(bitmap,config,index)
        val bytes=ByteArrayOutputStream().also{rendered.compress(Bitmap.CompressFormat.PNG,100,it)}.toByteArray()
        WallpaperManager.getInstance(applicationContext).setStream(bytes.inputStream(),null,true,WallpaperManager.FLAG_LOCK)
        return Result.success()
    }
}
