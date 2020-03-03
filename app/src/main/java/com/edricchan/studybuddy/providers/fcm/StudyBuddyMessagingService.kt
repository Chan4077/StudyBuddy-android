package com.edricchan.studybuddy.providers.fcm

import android.app.PendingIntent
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.edricchan.studybuddy.R
import com.edricchan.studybuddy.constants.Constants
import com.edricchan.studybuddy.extensions.TAG
import com.edricchan.studybuddy.extensions.buildIntent
import com.edricchan.studybuddy.interfaces.NotificationAction
import com.edricchan.studybuddy.ui.modules.main.MainActivity
import com.edricchan.studybuddy.ui.modules.settings.SettingsActivity
import com.edricchan.studybuddy.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson

class StudyBuddyMessagingService : FirebaseMessagingService() {
    private val notificationUtils = NotificationUtils.getInstance()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val manager = NotificationManagerCompat.from(this)
        val builder = NotificationCompat.Builder(
            this,
            getString(R.string.notification_channel_uncategorised_id)
        )

        if (remoteMessage.notification != null) {
            if (remoteMessage.notification?.title != null) {
                builder.setContentTitle(remoteMessage.notification?.title)
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(remoteMessage.notification?.body))
            }

            if (remoteMessage.notification?.body != null) {
                builder.setContentText(remoteMessage.notification?.body)
            }

            if (remoteMessage.notification?.icon != null) {
                val icon = resources.getIdentifier(
                    remoteMessage.notification?.icon,
                    "drawable",
                    packageName
                )
                // getIdentifier returns Resources.ID_NULL (0) if not such identifier exists
                if (icon != Resources.ID_NULL) {
                    builder.setSmallIcon(icon)
                } else {
                    // Use the default icon
                    builder.setSmallIcon(R.drawable.ic_notification_studybuddy_pencil_24dp)
                }
            } else {
                // Use the default icon
                builder.setSmallIcon(R.drawable.ic_notification_studybuddy_pencil_24dp)
            }

            if (remoteMessage.notification?.color != null) {
                builder.color = Color.parseColor(remoteMessage.notification?.color)
            } else {
                // Use the default color
                builder.color = ContextCompat.getColor(this, R.color.colorPrimary)
            }

            // Image support was added in FCM 20.0.0
            if (remoteMessage.notification?.imageUrl != null) {
                val thumbnailImageFutureTarget = Glide.with(this)
                    .asBitmap()
                    .load(remoteMessage.notification?.imageUrl)
                    .thumbnail()
                    .submit()
                val imageFutureTarget = Glide.with(this)
                    .asBitmap()
                    .load(remoteMessage.notification?.imageUrl)
                    .submit()
                builder.setLargeIcon(thumbnailImageFutureTarget.get())
                builder.setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(imageFutureTarget.get())
                        .bigLargeIcon(null)
                )
                Glide.with(this).apply {
                    clear(thumbnailImageFutureTarget)
                    clear(imageFutureTarget)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (remoteMessage.notification?.channelId != null) {
                    if (manager.getNotificationChannel(remoteMessage.notification?.channelId!!) == null) {
                        Log.w(
                            TAG,
                            "No such notification channel ${remoteMessage.notification?.channelId} exists." +
                                    "Assigning \"uncategorised\" channel."
                        )
                        builder.setChannelId(getString(R.string.notification_channel_uncategorised_id))
                    } else {
                        builder.setChannelId(remoteMessage.notification?.channelId!!)
                    }
                }
            }
        }

        if (remoteMessage.data != null) {
            if (remoteMessage.data.containsKey("notificationActions")) {
                Log.d(TAG, "notificationActions: ${remoteMessage.data["notificationActions"]}")
                var notificationActions: List<NotificationAction> = listOf()

                try {
                    val gson = Gson()
                    // TODO: Use Kotson library (https://github.com/SalomonBrys/Kotson)
                    val remoteNotificationActions = gson.fromJson(
                        remoteMessage.data["notificationActions"],
                        Array<NotificationAction>::class.java
                    )
                    notificationActions = remoteNotificationActions.asList()
                    Log.d(TAG, "Size: ${remoteNotificationActions.size}")
                    Log.d(TAG, "JSON: $remoteNotificationActions")
                    Log.d(
                        TAG,
                        "NotificationAction#actionTitle: ${remoteNotificationActions[0].actionTitle}"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Could not parse notification actions:", e)
                    Crashlytics.logException(e)
                }

                for (notificationAction in notificationActions) {
                    // This property is set to 0 by default to indicate that no such icon exists
                    var icon = 0
                    val intent: Intent
                    var notificationPendingIntent: PendingIntent? = null
                    val drawableIcon = resources.getIdentifier(
                        notificationAction.actionIcon,
                        "drawable",
                        packageName
                    )
                    // getIdentifier returns Resources.ID_NULL (0) if no such resource exists
                    if (drawableIcon != Resources.ID_NULL) {
                        icon = drawableIcon
                    }

                    when (notificationAction.actionType) {
                        // TODO: Don't hardcode action types
                        Constants.actionNotificationsSettingsIntent -> {
                            intent = buildIntent<SettingsActivity>(this) {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            }
                            notificationPendingIntent = PendingIntent.getActivity(
                                this,
                                0,
                                intent,
                                PendingIntent.FLAG_ONE_SHOT
                            )
                        }
                        else -> Log.w(
                            TAG,
                            "Unknown action type ${notificationAction.actionType} specified for $notificationAction."
                        )
                    }
                    builder.addAction(
                        NotificationCompat.Action(
                            icon,
                            notificationAction.actionTitle,
                            notificationPendingIntent
                        )
                    )
                }
            }
        }

        val mainActivityIntent = buildIntent<MainActivity>(this) {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent =
            PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_ONE_SHOT)
        builder.setContentIntent(mainPendingIntent)

        manager.notify(notificationUtils.incrementAndGetId(), builder.build())
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // Add token to Firebase Firestore in the user's document
        val fs = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            fs.document("users/${auth.currentUser?.uid}")
                .update("registrationToken", token)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Successfully updated token!")
                    } else {
                        Log.e(
                            TAG,
                            "An error occurred while attempting to update the token:",
                            task.exception
                        )
                    }
                }
        } else {
            Log.w(TAG, "There's currently no logged in user! Skipping document update.")
        }
    }
}
