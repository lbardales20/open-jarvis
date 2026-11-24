
package com.openjarvis.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.HandlerThread
import android.view.WindowManager

class ScreenshotCapture(private val context: Context) {

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private var latestBitmap: Bitmap? = null

    @Volatile
    private var isCapturing = false

    private val bitmapLock = Any()

    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0

    private val projectionCallback =
        object : MediaProjection.Callback() {

            override fun onStop() {
                release()
            }
        }

    fun startCapture(): Boolean {
        return isCapturing && mediaProjection != null
    }

    fun initialize(projection: MediaProjection): Bitmap? {
        release()

        try {
            val windowManager =
                context.getSystemService(Context.WINDOW_SERVICE)
                    as WindowManager

            val metrics = android.util.DisplayMetrics()
            windowManager.defaultDisplay.getRealMetrics(metrics)

            screenWidth = metrics.widthPixels
            screenHeight = metrics.heightPixels
            screenDensity = metrics.densityDpi

            if (screenWidth <= 0 || screenHeight <= 0 || screenDensity <= 0) {
                return null
            }

            handlerThread = HandlerThread("ScreenshotThread").apply {
                start()
            }

            handler = Handler(handlerThread!!.looper)

            imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
            )

            mediaProjection = projection

            projection.registerCallback(
                projectionCallback,
                handler
            )

            imageReader?.setOnImageAvailableListener(
                { reader ->
                    processImage(reader)
                },
                handler
            )

            virtualDisplay = projection.createVirtualDisplay(
                "JarvisScreenshot",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface,
                null,
                handler
            )

            isCapturing = virtualDisplay != null

            if (!isCapturing) {
                release()
                return null
            }

            return null

        } catch (e: Exception) {
            e.printStackTrace()
            release()
            return null
        }
    }

    private fun processImage(reader: ImageReader) {
        var image: Image? = null

        try {
            image = reader.acquireLatestImage() ?: return

            val plane = image.planes[0]
            val buffer = plane.buffer

            val pixelStride = plane.pixelStride
            val rowStride = plane.rowStride

            val rowPadding =
                rowStride - pixelStride * image.width

            val bitmapWidth =
                image.width + rowPadding / pixelStride

            val fullBitmap = Bitmap.createBitmap(
                bitmapWidth,
                image.height,
                Bitmap.Config.ARGB_8888
            )

            buffer.rewind()
            fullBitmap.copyPixelsFromBuffer(buffer)

            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap,
                0,
                0,
                image.width,
                image.height
            )

            if (croppedBitmap !== fullBitmap) {
                fullBitmap.recycle()
            }

            synchronized(bitmapLock) {
                latestBitmap?.recycle()
                latestBitmap = croppedBitmap
            }

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image?.close()
        }
    }

    fun capture(): Bitmap? {
        if (!isCapturing) return null

        return synchronized(bitmapLock) {
            latestBitmap?.copy(
                Bitmap.Config.ARGB_8888,
                false
            )
        }
    }

    fun release() {
        isCapturing = false

        try {
            imageReader?.setOnImageAvailableListener(null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        virtualDisplay = null

        try {
            mediaProjection?.unregisterCallback(projectionCallback)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mediaProjection = null

        try {
            imageReader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        imageReader = null

        synchronized(bitmapLock) {
            latestBitmap?.recycle()
            latestBitmap = null
        }

        handlerThread?.quitSafely()
        handlerThread = null
        handler = null

        screenWidth = 0
        screenHeight = 0
        screenDensity = 0
    }

    companion object {

        @Volatile
        private var instance: ScreenshotCapture? = null

        fun getInstance(context: Context): ScreenshotCapture {
            return instance ?: synchronized(this) {
                instance ?: ScreenshotCapture(
                    context.applicationContext
                ).also {
                    instance = it
                }
            }
        }
    }
}
