package ren.imyan.app_tracker.common.ktx

import android.content.Context
import android.graphics.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

fun Bitmap.setBackground(color: Int = Color.WHITE): Bitmap {
    val width = this.width
    val height = this.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565).apply {
        density = this@setBackground.density
    }
    val paint = Paint().apply {
        setColor(color)
    }
    val canvas = Canvas(bitmap).apply {
        density = this@setBackground.density
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    canvas.drawBitmap(
        this@setBackground,
        0f,
        0f,
        paint
    )
    return bitmap
}

fun Bitmap.toSize(width: Float, height: Float): Bitmap {
    val oldWidth = this.width
    val oldHeight = this.height
    val scaleWidth = width / oldWidth
    val scaleHeight = height / oldHeight
    val matrix = Matrix().apply {
        postScale(scaleWidth, scaleHeight)
    }
    return Bitmap.createBitmap(this, 0, 0, oldWidth, oldHeight, matrix, true)
}

fun Bitmap.toFile(
    fileName: String,
    path: String = get<Context>().cacheDir.path,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): File? {
    var file: File? = null
    return try {
        file = File(path + File.separator + fileName)
        file.createNewFile()

        ByteArrayOutputStream().use {
            this.compress(format, 80, it)
            val bitmapData = it.toByteArray()
            FileOutputStream(file).use { fos ->
                fos.write(bitmapData)
                fos.flush()
                fos.close()
                file
            }
        }
    } catch (e: Exception) {
        file
    }
}