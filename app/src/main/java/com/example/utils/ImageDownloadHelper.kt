package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import java.io.InputStream
import java.io.OutputStream

object ImageDownloadHelper {
    fun saveImageToGallery(context: Context, imageUriStr: String) {
        try {
            Toast.makeText(context, "Bò Béo đang chuẩn bị tải ảnh nha... 🍼🥛", Toast.LENGTH_SHORT).show()
            
            val uri = Uri.parse(imageUriStr)
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                Toast.makeText(context, "Hix, hụt mất rồi cậu ơi... Định dạng ảnh bị lỗi rùi 🥺", Toast.LENGTH_LONG).show()
                return
            }

            val filename = "LovelyScheduler_${System.currentTimeMillis()}.jpg"
            val resolver = context.contentResolver
            
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/LovelyScheduler")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (imageUri != null) {
                val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(imageUri, contentValues, null, null)
                }
                
                Toast.makeText(context, "Tải ảnh thành công rồi nha! Ảnh lưu ở Pictures/LovelyScheduler đó 🌸✨", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Hix, hụt mất rồi cậu ơi... Không tạo được file mới 🥺", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Hix, hụt mất rồi cậu ơi... Có lỗi xảy ra: ${e.localizedMessage} 🥺", Toast.LENGTH_LONG).show()
        }
    }
}
