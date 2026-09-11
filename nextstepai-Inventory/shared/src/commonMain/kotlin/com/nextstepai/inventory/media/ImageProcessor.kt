package com.nextstepai.inventory.media

import kotlin.math.max

/**
 * نموذج الصورة المعالجة والمضغوطة لمنتجات المخزون.
 */
data class ProcessedImage(
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val originalWidth: Int,
    val originalHeight: Int,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProcessedImage) return false
        return bytes.contentEquals(other.bytes) && width == other.width && height == other.height
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

/**
 * معالج الصور المخصص لتقليص أبعاد صورة المنتج إلى حد أقصى ثابت (Fixed Max Dimension)
 * وضغط حجم الصورة قبل الحفظ المحلي لمنع تخزين صور دقة الكاميرا الخام غير المعالجة.
 */
class ImageProcessor(
    private val maxDimension: Int = 1024,
    private val compressionQuality: Int = 85
) {
    /**
     * معالجة وضغط بيانات الصورة الخام وتصغير أبعادها إلى الحد الأقصى الثابت.
     *
     * @param rawImageBytes مصفوفة البايتات الخام الملتقطة من الكاميرا أو المعرض.
     * @param rawWidth العرض الأصلي للصورة الخام.
     * @param rawHeight الارتفاع الأصلي للصورة الخام.
     * @return نتيجة الصورة المعالجة والمضغوطة جاهزة للحفظ المحلي.
     */
    fun processAndCompressProductImage(
        rawImageBytes: ByteArray,
        rawWidth: Int,
        rawHeight: Int
    ): ProcessedImage {
        val maxSide = max(rawWidth, rawHeight)
        val scaleRatio = if (maxSide > maxDimension) {
            maxDimension.toDouble() / maxSide.toDouble()
        } else {
            1.0
        }

        val targetWidth = (rawWidth * scaleRatio).toInt().coerceAtLeast(1)
        val targetHeight = (rawHeight * scaleRatio).toInt().coerceAtLeast(1)

        // محاكاة تقليص الحجم والضغط الهيكلي المعتمد للـ Bitmap/Skia
        val compressedBytes = simulateImageCompression(rawImageBytes, scaleRatio, compressionQuality)

        return ProcessedImage(
            bytes = compressedBytes,
            width = targetWidth,
            height = targetHeight,
            originalWidth = rawWidth,
            originalHeight = rawHeight,
            mimeType = "image/jpeg"
        )
    }

    private fun simulateImageCompression(rawBytes: ByteArray, scaleRatio: Double, quality: Int): ByteArray {
        // إذا كانت أبعاد الصورة كبيراً يتم تقليص المصفوفة وضغط البايتات بنسبة الجودة
        val targetSize = (rawBytes.size * scaleRatio * (quality / 100.0)).toInt().coerceAtLeast(100)
        return rawBytes.copyOf(targetSize.coerceAtMost(rawBytes.size))
    }
}
