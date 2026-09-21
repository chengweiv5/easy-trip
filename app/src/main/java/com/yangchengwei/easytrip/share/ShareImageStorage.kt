package com.yangchengwei.easytrip.share

import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShareImageStorage {
    /** Keep recent files readable by receiving apps; expire only our temporary exports. */
    suspend fun pruneCache(context: Context) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        for (directory in listOf("itinerary-share", "itinerary-maps")) {
            File(context.cacheDir, directory).listFiles()?.filter { it.isFile && it.lastModified() < cutoff }
                ?.forEach { it.delete() }
        }
    }
    fun newOutput(context:Context):File = File(context.cacheDir,"itinerary-share/${java.util.UUID.randomUUID()}.png")
    fun shareIntent(context:Context,image:ShareImage):Intent {
        require(image.file.isFile) { "图片已失效，请重新生成" }
        val uri=FileProvider.getUriForFile(context,"${context.packageName}.itinerary-share",image.file)
        return Intent(Intent.ACTION_SEND).apply {
            type="image/png";putExtra(Intent.EXTRA_STREAM,uri)
            clipData=ClipData.newUri(context.contentResolver,"行程长图",uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    suspend fun save(context:Context,image:ShareImage):Uri = withContext(Dispatchers.IO) {
        val name="EasyTrip_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}_${java.util.UUID.randomUUID().toString().take(6)}.png"
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,"image/png")
            if(Build.VERSION.SDK_INT>=29){put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/EasyTrip");put(MediaStore.Images.Media.IS_PENDING,1)}
            else {
                @Suppress("DEPRECATION") val dir=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"EasyTrip")
                check(dir.isDirectory || dir.mkdirs()) { "无法创建相册目录" }
                @Suppress("DEPRECATION") put(MediaStore.Images.Media.DATA,File(dir,name).absolutePath)
            }
        }
        val resolver=context.contentResolver
        val uri=checkNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)) { "无法保存图片" }
        try {
            checkNotNull(resolver.openOutputStream(uri)).use { output -> image.file.inputStream().use { it.copyTo(output) } }
            if(Build.VERSION.SDK_INT>=29) check(resolver.update(uri,ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING,0) },null,null)>0)
            uri
        } catch(error:Exception) { resolver.delete(uri,null,null);throw error }
    }
}
