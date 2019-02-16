package com.craiovadata.android.messenger.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.DetailsActivity
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.ForegroundListener.Companion.isForeground
import com.craiovadata.android.messenger.util.Util.isMuteAll
import com.craiovadata.android.messenger.util.Util.sendRegistrationToServer
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.FirebaseStorage


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

            if (/* Check if data needs to be processed by long running job */ false) {
                // For long-running tasks (10 seconds or more) use Firebase Job Dispatcher.
                scheduleJob()
            } else {
                // Handle message within 10 seconds
                handleNow(remoteMessage.data)
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
    private fun scheduleJob() {
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val myJob = dispatcher.newJobBuilder()
                .setService(MyJobService::class.java)
                .setTag("my-job-tag")
                .build()
        dispatcher.schedule(myJob)
    }

    private fun handleNow(data: MutableMap<String, String>) {

        data["fromUid"]?.let {
            startPlaying(data)
        }

        data[MSG_TEXT]?.let {
            if (!isForeground())
                sendNotification(data)
        }


    }

    private fun sendNotification(data: MutableMap<String, String>) {
        val intent = Intent(this, DetailsActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra(KEY_USER_ID, data[UID_P])
        intent.putExtra(KEY_USER_NAME, data[MSG_AUTHOR])

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this)
                // add all of DetailsActivity's parents to the stack,
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT)

        val futureTarget = Glide.with(this)
                .asBitmap()
                .load(data[PHOTO_URL])
                .submit()

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_message_white_24px)
                .setLargeIcon(futureTarget.get())
                .setContentTitle("Message from " + data[MSG_AUTHOR])
                .setContentText(data[MSG_TEXT])
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)

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

    private fun startPlaying(data: MutableMap<String, String>) {
        if (isMuteAll(this)) return
        val currentUser = FirebaseAuth.getInstance().currentUser
        val fromUid = data["fromUid"]
        val toUid = data["toUid"]

        if (toUid == currentUser?.uid) {
            val storageRef = FirebaseStorage.getInstance().reference
            val ref = storageRef.child("sounds/users/$fromUid/$toUid/sound.3gp")
            Log.d(TAG, "service messaging startPlaying() ref: " + ref)

            val ONE_MEGABYTE: Long = 1024 * 1024
            ref.getBytes(ONE_MEGABYTE).addOnSuccessListener {bytes ->
                playSound(bytes)

                val txt = String.format(getString(R.string.toast_txt_new_audio_received), data["author"])
                Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun playSound(bytes: ByteArray) {

        val mediaSource = MyMediaDataSource(bytes)
        MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(mediaSource)
            setOnCompletionListener { release() }
            prepareAsync()
            setOnPreparedListener { start() }
        }


    }

    companion object {
        val TAG = "MessagingService"
    }

}

