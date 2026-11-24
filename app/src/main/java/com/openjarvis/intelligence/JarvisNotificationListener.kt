
package com.openjarvis.intelligence

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.openjarvis.accessibility.JarvisAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class JarvisNotificationListener : NotificationListenerService() {

    private val scope =
        CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val notifications =
        CopyOnWriteArrayList<JarvisNotification>()

    companion object {

        private val PRIVACY_PROTECTED_APPS = setOf(
            "com.google.android.apps.nbu.paisa.user",
            "com.phonepe.app",
            "net.one97.paytm",
            "com.bankofamerica.cashpromobile",
            "com.chase",
            "com.wells fargo",
            "com.usbank",
            "com.citi",
            "com.barclays",
            "com.hsbc",
            "com.db",
            "com.americanexpress",
            "com.discover",
            "com.paypal",
            "com.squareup",
            "com.stripe",
            "com.razorpay"
        )

        private val messagingApps = setOf(
            "com.whatsapp",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",
            "com.instagram.android",
            "com.facebook.orca",
            "org.telegram.messenger",
            "com.slack",
            "com.discord"
        )

        fun shouldProcessNotification(packageName: String): Boolean {
            return packageName !in PRIVACY_PROTECTED_APPS
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        if (!shouldProcessNotification(sbn.packageName)) {
            return
        }

        val parsed = parseNotification(sbn)

        notifications.removeAll {
            it.id == parsed.id &&
                it.packageName == parsed.packageName
        }

        notifications.add(parsed)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        notifications.removeAll {
            it.id == sbn.id &&
                it.packageName == sbn.packageName
        }
    }

    private fun parseNotification(
        sbn: StatusBarNotification
    ): JarvisNotification {

        val extras = sbn.notification.extras

        val title = extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()

        val body = extras
            .getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()

        val appLabel = try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    sbn.packageName,
                    0
                )
            ).toString()
        } catch (e: Exception) {
            sbn.packageName
        }

        val isMessaging = sbn.packageName in messagingApps

        val sender = if (isMessaging) {
            extras.getCharSequence(Notification.EXTRA_TITLE)
                ?.toString()
        } else {
            null
        }

        return JarvisNotification(
            id = sbn.id,
            packageName = sbn.packageName,
            appLabel = appLabel,
            title = title,
            body = body,
            timestamp = sbn.postTime,
            isMessaging = isMessaging,
            sender = sender
        )
    }

    suspend fun replyToNotification(
        sender: String,
        packageName: String,
        reply: String
    ): Boolean {

        return try {

            val notification = notifications.find {
                it.packageName == packageName &&
                    it.sender?.contains(
                        sender,
                        ignoreCase = true
                    ) == true
            } ?: return false

            val service =
                JarvisAccessibilityService.instance ?: return false

            val tapped = service.tapByText("Reply")
            if (!tapped) return false

            val typed = service.typeText(reply)
            if (!typed) return false

            service.tapByText("Send")

        } catch (e: Exception) {
            false
        }
    }

    fun getUnread(
        packageName: String? = null
    ): List<JarvisNotification> {

        return if (packageName != null) {
            notifications.filter {
                it.packageName == packageName
            }
        } else {
            notifications.toList()
        }
    }

    fun getSummary(): String {

        val grouped = notifications.groupBy {
            it.packageName
        }

        return grouped.entries
            .sortedByDescending { it.value.size }
            .take(5)
            .joinToString(", ") { (pkg, msgs) ->

                val app = msgs.first().appLabel
                val count = msgs.size
                val lastSender = msgs.lastOrNull()?.sender

                if (lastSender != null) {
                    "$count $app from $lastSender"
                } else {
                    "$count $app"
                }
            }
    }

    fun clearAll(packageName: String? = null) {

        if (packageName != null) {
            notifications.removeAll {
                it.packageName == packageName
            }
        } else {
            notifications.clear()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    data class JarvisNotification(
        val id: Int,
        val packageName: String,
        val appLabel: String,
        val title: String,
        val body: String,
        val timestamp: Long,
        val isMessaging: Boolean,
        val sender: String?
    )
}
