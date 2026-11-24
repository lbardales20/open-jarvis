
package com.openjarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class VisionModule(private val context: Context) {

    private val recognizer: TextRecognizer =
        TextRecognition.getClient(
            TextRecognizerOptions.DEFAULT_OPTIONS
        )

    suspend fun extractText(bitmap: Bitmap): String {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(visionText.text)
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "OCR failed", error)

                        if (continuation.isActive) {
                            continuation.resume("")
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Could not process image", e)

                if (continuation.isActive) {
                    continuation.resume("")
                }
            }
        }
    }

    suspend fun extractStructured(bitmap: Bitmap): ScreenOCR {
        return suspendCancellableCoroutine { continuation ->
            try {
                val image = InputImage.fromBitmap(bitmap, 0)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            val blocks = visionText.textBlocks.map { block ->
                                OCRBlock(
                                    text = block.text,
                                    confidence = 0f,
                                    boundingBox = block.boundingBox?.let { rect ->
                                        BoundingBox(
                                            rect.left,
                                            rect.top,
                                            rect.right,
                                            rect.bottom
                                        )
                                    },
                                    lines = block.lines.map { line ->
                                        OCRLine(
                                            text = line.text,
                                            confidence = 0f,
                                            boundingBox = line.boundingBox?.let { rect ->
                                                BoundingBox(
                                                    rect.left,
                                                    rect.top,
                                                    rect.right,
                                                    rect.bottom
                                                )
                                            }
                                        )
                                    }
                                )
                            }

                            continuation.resume(
                                ScreenOCR(
                                    fullText = visionText.text,
                                    blocks = blocks
                                )
                            )
                        }
                    }
                    .addOnFailureListener { error ->
                        Log.e(TAG, "Structured OCR failed", error)

                        if (continuation.isActive) {
                            continuation.resume(
                                ScreenOCR("", emptyList())
                            )
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Could not process structured image", e)

                if (continuation.isActive) {
                    continuation.resume(
                        ScreenOCR("", emptyList())
                    )
                }
            }
        }
    }

    fun release() {
        try {
            recognizer.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recognizer", e)
        }
    }

    data class ScreenOCR(
        val fullText: String,
        val blocks: List<OCRBlock>
    )

    data class OCRBlock(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?,
        val lines: List<OCRLine>
    )

    data class OCRLine(
        val text: String,
        val confidence: Float,
        val boundingBox: BoundingBox?
    )

    data class BoundingBox(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )

    companion object {

        private const val TAG = "VisionModule"

        @Volatile
        private var instance: VisionModule? = null

        fun getInstance(context: Context): VisionModule {
            return instance ?: synchronized(this) {
                instance ?: VisionModule(
                    context.applicationContext
                ).also {
                    instance = it
                }
            }
        }
    }
}
