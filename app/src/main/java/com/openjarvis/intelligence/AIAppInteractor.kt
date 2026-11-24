
package com.openjarvis.intelligence

import android.content.Context
import com.openjarvis.accessibility.JarvisAccessibilityService
import com.openjarvis.accessibility.ScreenReader
import kotlinx.coroutines.delay

class AIAppInteractor(private val context: Context) {

    private val workingMemory = TaskWorkingMemory()

    private fun getScreenReader(): ScreenReader? {
        val service = JarvisAccessibilityService.instance
            ?: return null

        return ScreenReader(service)
    }

    fun openAIApp(meta: AIAppMeta): Boolean {
        return JarvisAccessibilityService.instance
            ?.let { service ->
                service.openAppByPackage(meta.packageName)
                true
            } ?: false
    }

    suspend fun clearContext() {
        try {
            JarvisAccessibilityService.instance?.pressBack()
            delay(300)
        } catch (e: Exception) {
        }
    }

    fun typePrompt(prompt: String): Boolean {
        return JarvisAccessibilityService.instance
            ?.typeText(prompt) ?: false
    }

    suspend fun waitForResponse(timeoutMs: Long = 60_000): String {
        val startTime = System.currentTimeMillis()
        var lastText = ""
        var sameCount = 0

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            delay(1000)

            val currentText =
                getScreenReader()?.extractAllText() ?: return lastText

            if (currentText == lastText) {
                sameCount++

                if (sameCount >= 2 && currentText.isNotBlank()) {
                    return currentText
                }
            } else {
                sameCount = 0
                lastText = currentText
            }
        }

        return lastText
    }

    fun extractResponse(meta: AIAppMeta): String {
        return when (meta.responseExtraction) {
            ResponseExtraction.SCREEN_TEXT ->
                getScreenReader()?.extractAllText() ?: ""

            ResponseExtraction.OCR_REQUIRED ->
                getScreenReader()?.extractAllText() ?: ""

            ResponseExtraction.COPY_BUTTON ->
                tryCopyFromUI()

            ResponseExtraction.SHARE_MENU ->
                tryShareFromUI()
        }
    }

    fun getWorkingMemory(): TaskWorkingMemory = workingMemory

    private fun tryCopyFromUI(): String {
        return getScreenReader()?.extractAllText() ?: ""
    }

    private fun tryShareFromUI(): String {
        return getScreenReader()?.extractAllText() ?: ""
    }
}
