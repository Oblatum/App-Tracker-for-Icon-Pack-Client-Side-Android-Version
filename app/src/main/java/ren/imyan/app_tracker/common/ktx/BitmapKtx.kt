package ren.imyan.app_tracker.common.ktx

import android.content.Context
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Bitmap.toFile(fileName:String):File?{
    var file: File? = null
    return try {
        file = File(get<Context>().cacheDir.path+File.separator+fileName)
        file.createNewFile()

        ByteArrayOutputStream().use {
            this.compress(Bitmap.CompressFormat.JPEG,80,it)
            val bitmapData = it.toByteArray()
            FileOutputStream(file).use { fos->
                fos.write(bitmapData)
                fos.flush()
                fos.close()
                file
            }
        }
    }catch (e:Exception){
        file
    }
}