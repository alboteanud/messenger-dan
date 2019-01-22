package com.craiovadata.android.messenger.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.DetailsActivity
import com.craiovadata.android.messenger.R
import com.craiovadata.android.messenger.util.*
import com.firebase.jobdispatcher.FirebaseJobDispatcher
import com.firebase.jobdispatcher.GooglePlayDriver
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyMessagingService : FirebaseMessagingService(), MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener {


    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
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
        Log.d(TAG, "Message from ${remoteMessage?.from}")

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
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
        saveRegistrationLocally(token)


    }

    private fun saveRegistrationLocally(token: String?) {
        getSharedPreferences("_", MODE_PRIVATE).edit().putString("token", token).apply()
    }

    /**
     * Schedule a job using FirebaseJobDispatcher.
     */
    private fun scheduleJob() {
        // [START dispatch_job]
        val dispatcher = FirebaseJobDispatcher(GooglePlayDriver(this))
        val myJob = dispatcher.newJobBuilder()
                .setService(MyJobService::class.java)
                .setTag("my-job-tag")
                .build()
        dispatcher.schedule(myJob)
        // [END dispatch_job]
    }

    /**
     * Handle time allotted to BroadcastReceivers.
     */
    private fun handleNow(data: MutableMap<String, String>) {

        val isActive = ForegroundBackgroundListener.isForeground()
        Log.d(TAG, "Short lived task is done. active: " + isActive)

        val text = data[TEXT]
        if (text != null) {
            if (text.startsWith("https://firebasestorage.googleapis.")) {
                startPlaying(data)
                data[TEXT] = "audio message"
            }
            if (!isActive)
                sendNotification(data)

        }
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the auth's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    fun sendRegistrationToServer(token: String?) {
        val currentUser: FirebaseUser? = FirebaseAuth.getInstance().currentUser

        if (token == null || currentUser == null) return

        val ref = FirebaseFirestore.getInstance().document("$USERS/${currentUser.uid}/$TOKENS/$token")
        val hashMap = HashMap<String, Any>()
        hashMap["updated"] = Timestamp.now()
        ref.set(hashMap, SetOptions.merge())

    }

    private fun sendNotification(data: MutableMap<String, String>) {
        val intent = Intent(this, DetailsActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val convID = data["palId"]  // from functions
        intent.putExtra(KEY_ROOM_ID, convID)

        val pendingIntent: PendingIntent? = TaskStackBuilder.create(this)
                // add all of DetailsActivity's parents to the stack,
                .addNextIntentWithParentStack(intent)
                .getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT)

        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_message_white_24px)
                .setContentTitle("Message from " + data[AUTHOR])
                .setContentText(data[TEXT])
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


        val futureTarget = Glide.with(this)
                .asBitmap()
                .load(data[PHOTO_URL])
                .submit()

        val bitmap = futureTarget.get()
        notificationBuilder.setLargeIcon(bitmap)

// Do something with the Bitmap and then when you're done with it:
        Glide.with(this).clear(futureTarget)


        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    private fun startPlaying(data: MutableMap<String, String>) {

//        val intent = Intent(this@MyMessagingService, MediaPlayerService::class.java)
//        intent.action = ACTION_PLAY
//        intent.putExtra(URL_EXTRA_PLAY, url)
//        startService(intent)
        val previousMsgId = getSharedPreferences("_", Context.MODE_PRIVATE).getString("lastMsgID", "")
        val thisMsgID = data["messageId"]

        if (thisMsgID.equals(previousMsgId)) return

        getSharedPreferences("_", Context.MODE_PRIVATE).edit().putString("lastMsgID", thisMsgID).apply()

        val msgText = data[TEXT]
        MediaPlayer().apply {
            Log.d(TAG, "url: " + msgText)
            setDataSource(msgText)
            setOnPreparedListener(this@MyMessagingService)
//            setOnPreparedListener { start() } // not working
            setOnCompletionListener(this@MyMessagingService)
            prepareAsync() // prepare async to not block main thread
        }

    }

    override fun onPrepared(mp: MediaPlayer) {
        mp.start()
    }

    override fun onCompletion(mp: MediaPlayer) {
        mp.stop()
        mp.release()
    }

    companion object {
        val TAG = "MyFirMessengingService"
    }

}
