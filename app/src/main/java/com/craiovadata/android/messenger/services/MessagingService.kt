package com.craiovadata.android.messenger.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.DetailsActivity
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.ForegroundListener.Companion.isForeground
import com.craiovadata.android.messenger.util.Util.sendRegistrationToServer
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the auth taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]

        // TODO(developer): Handle FCM messages here.
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ
//        Log.d(TAG, "Message from ${remoteMessage?.from}")

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

            val data = remoteMessage.data
            FirebaseAuth.getInstance().currentUser?.let { currentUser ->
                if (currentUser.uid == data["destinationUid"]) {
                    val msgId = data["msgId"]
                    val lastMsgId = getSharedPreferences("_", Context.MODE_PRIVATE).getString("lastMsgId", null)
                    if (msgId == lastMsgId) return

                    if (/* Check if data needs to be processed by long running job */ data["text"] == "🗣️") {
                        // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                        scheduleJobPlaySound(data)
                    } else {
                        // Handle message within 10 seconds
                        handleNow(data)
                    }

                    getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString("lastMsgId", msgId).apply()

                }
            }
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.

//        sendNotification(remoteMessage?.notification?.body!!)
    }

    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private fun scheduleJobPlaySound(data: MutableMap<String, String>) {

        val bundle = Bundle()
        for (entry in data.entries)
            bundle.putString(entry.key, entry.value)

        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val myJob = dispatcher.newJobBuilder()
                .setService(MessagingJobService::class.java)
                .setTag("my-play-job-tag")
                .setExtras(bundle)
                .build()
        dispatcher.schedule(myJob)
    }

    private fun handleNow(data: MutableMap<String, String>) {

        if (!isForeground())
            sendNotification(data)

    }

    private fun sendNotification(data: MutableMap<String, String>) {
        val intent = Intent(this, DetailsActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(KEY_USER_ID, data[UID])
        intent.putExtra(KEY_USER_NAME, data[MSG_AUTHOR])

        val pendingIntentOpenActivity: PendingIntent? = TaskStackBuilder.create(this)
                // add all of DetailsActivity's parents to the stack,
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT)

        val futureTarget = Glide.with(this)
                .asBitmap()
                .load(data[PHOTO_URL])
                .submit()

        val txtNotif = String.format(getString(com.craiovadata.android.messenger.R.string.notification_text), data[MSG_AUTHOR])
        val channelId = getString(com.craiovadata.android.messenger.R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(com.craiovadata.android.messenger.R.drawable.ic_notif_bird)
                .setLargeIcon(futureTarget.get())
                .setContentTitle(txtNotif)
                .setContentText(data[MSG_TEXT])
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntentOpenActivity)
//                .addAction()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

// Do something with the Bitmap and then when you're done with it:
        Glide.with(this).clear(futureTarget)

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }


    companion object {
        val TAG = "MessagingService"
    }

}


