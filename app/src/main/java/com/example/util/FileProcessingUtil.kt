package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

private const val TAG = "HBXFileProcessing"

object FileProcessingUtil {

    private const val MAX_IMAGE_DIMENSION = 1600
    private const val THUMBNAIL_DIMENSION = 300
    private const val COMPRESSION_QUALITY = 82

    fun copyUriToPrivateFile(context: Context, uri: Uri, targetFileName: String): File? {
        return try {
            val destinationDir = File(context.filesDir, "hbx_documents")
            if (!destinationDir.exists()) destinationDir.mkdirs()

            val destinationFile = File(destinationDir, targetFileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(destinationFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            destinationFile
        } catch (e: Exception) {
            Log.e(TAG, "Failed copying URI to private storage: ${e.message}")
            null
        }
    }

    fun optimizeImageFile(context: Context, sourceFile: File): File {
        return try {
            // First decode with inJustDecodeBounds to check dimensions
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight

            if (width <= 0 || height <= 0) return sourceFile

            // Calculate sample size
            var inSampleSize = 1
            var maxDim = Math.max(width, height)
            while (maxDim > MAX_IMAGE_DIMENSION) {
                inSampleSize *= 2
                maxDim /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return sourceFile

            val optimizedDir = File(context.filesDir, "optimized_photos")
            if (!optimizedDir.exists()) optimizedDir.mkdirs()

            val optimizedFile = File(optimizedDir, "opt_" + sourceFile.name)
            FileOutputStream(optimizedFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            }
            bitmap.recycle()
            optimizedFile
        } catch (e: Exception) {
            Log.w(TAG, "Image optimization skipped due to error: ${e.message}")
            sourceFile
        }
    }

    fun generateThumbnail(context: Context, sourceFile: File): String? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(sourceFile.absolutePath, options)

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            var inSampleSize = 1
            var maxDim = Math.max(width, height)
            while (maxDim > THUMBNAIL_DIMENSION) {
                inSampleSize *= 2
                maxDim /= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }

            val bitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return null

            val thumbDir = File(context.filesDir, "thumbnails")
            if (!thumbDir.exists()) thumbDir.mkdirs()

            val thumbFile = File(thumbDir, "thumb_" + sourceFile.nameWithoutExtension + ".jpg")
            FileOutputStream(thumbFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            }
            bitmap.recycle()
            thumbFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Thumbnail generation failed: ${e.message}")
            null
        }
    }

    fun calculateMD5Checksum(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val md5Bytes = digest.digest()
            val sb = StringBuilder()
            for (b in md5Bytes) {
                sb.append(String.format(Locale.US, "%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            Log.w(TAG, "Checksum calculation failed: ${e.message}")
            null
        }
    }

    fun calculateSHA256Checksum(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format(Locale.US, "%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            Log.w(TAG, "SHA-256 calculation failed: ${e.message}")
            null
        }
    }

    fun verifyFileIntegrity(file: File, expectedChecksum: String): Boolean {
        if (!file.exists() || expectedChecksum.isBlank()) return false
        val cleanExpected = expectedChecksum.trim().lowercase(Locale.US)
        val actualHash = if (cleanExpected.length == 64) {
            calculateSHA256Checksum(file)
        } else {
            calculateMD5Checksum(file)
        }
        return actualHash != null && actualHash.equals(cleanExpected, ignoreCase = true)
    }

    fun getMimeTypeFromExtension(extension: String): String {
        return when (extension.lowercase(Locale.US).removePrefix(".")) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            "pdf" -> "application/pdf"
            "mp4" -> "video/mp4"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }
    }
}
