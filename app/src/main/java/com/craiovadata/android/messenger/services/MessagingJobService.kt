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
import com.craiovadata.android.messenger.services.PlayerIntentService.Companion.ACTION_BAZ
import com.craiovadata.android.messenger.services.PlayerIntentService.Companion.EXTRA_PARAM1
import com.craiovadata.android.messenger.services.PlayerIntentService.Companion.EXTRA_PARAM2
import com.craiovadata.android.messenger.util.*
import com.firebase.jobdispatcher.JobParameters
import com.firebase.jobdispatcher.JobService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import java.io.File


class MessagingJobService : JobService() {

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        Log.d(TAG, "Performing long running task in scheduled job")
        getSound(jobParameters)
        return false
    }

    private fun getSound(jobParameters: JobParameters) {

        val senderUid = jobParameters.extras?.get(UID) as String
        val senderName = jobParameters.extras?.get(MSG_AUTHOR) as String
        val destUid = FirebaseAuth.getInstance().currentUser?.uid

        val storageRef = FirebaseStorage.getInstance().reference
        val soundLocation = String.format(getString(R.string.firebase_storage_sound_location), senderUid, destUid)
        val ref = storageRef.child(soundLocation)

        val localFile = File.createTempFile(destUid, ".3gp")
        Log.d(TAG, "File path: " + localFile.getAbsolutePath())
//        ref.getBytes(ONE_MEGABYTE).addOnSuccessListener { bytes -> playSound(bytes)     }
        ref.getFile(localFile).addOnSuccessListener {
            // Local temp file has been created
            Log.d(TAG, "sound file downloaded successfully")
//            playSound(localFile.path)

            PlayerIntentService.startActionBaz(this, localFile.path, "")

            val txt = String.format(getString(com.craiovadata.android.messenger.R.string.toast_txt_new_audio_received), senderName)
            Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
            sendNotification(jobParameters, localFile.path)

        }

    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        return true
    }

    private fun playSound(path: String) {
        if (Util.isMuteAll(this)) return
//        val mediaSource = MyMediaDataSource(bytes)
        MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setDataSource(path)
//            setDataSource(mediaSource)
            setOnCompletionListener {
                release()
                Log.d(TAG, "sound play completed")
            }
            prepareAsync()
            setOnPreparedListener { start() }
        }
    }

//    private lateinit var futureTarget: FutureTarget<Bitmap>

    private fun sendNotification(jobParameters: JobParameters, path: String) {

        val senderUid = jobParameters.extras?.get(UID) as String
        val senderName = jobParameters.extras?.get(MSG_AUTHOR) as String
        val txt = jobParameters.extras?.get(MSG_TEXT) as String
        val senderPhotoUrl = jobParameters.extras?.get(PHOTO_URL) as String

        val intentDetailsActivity = Intent(this, DetailsActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intentDetailsActivity.putExtra(KEY_USER_ID, senderUid)
        intentDetailsActivity.putExtra(KEY_USER_NAME, senderName)

        val pendingIntentOpenActivity: PendingIntent? = TaskStackBuilder.create(this)
                // add all of DetailsActivity's parents to the stack,
                .addNextIntentWithParentStack(intentDetailsActivity)
                .getPendingIntent(0, PendingIntent.FLAG_ONE_SHOT)


        val intentStartPlayer = Intent(this, PlayerIntentService::class.java)
        intentStartPlayer.action = ACTION_BAZ
        intentStartPlayer.putExtra(EXTRA_PARAM1, path)
        intentStartPlayer.putExtra(EXTRA_PARAM2, "")

        val pendingIntentStartPlayer: PendingIntent? = PendingIntent.getService(this, 0, intentStartPlayer, PendingIntent.FLAG_UPDATE_CURRENT)
//        var futureTarget : FutureTarget<Bitmap>? = null


        val txtNotif = String.format(getString(R.string.toast_txt_new_audio_received), senderName)
        val channelId = getString(R.string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_notif_bird)
//                .setLargeIcon(futureTarget?.get())
                .setContentTitle(txtNotif)
                .setContentText(txt)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntentOpenActivity)
                .addAction(R.drawable.ic_notif_bird, getString(R.string.play_sound_label), pendingIntentStartPlayer)


        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                    "Channel",
                    NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        Thread(Runnable {
            val futureTarget = Glide.with(this).asBitmap().load(senderPhotoUrl).submit()
            builder.setLargeIcon(futureTarget.get())
            Glide.with(this).clear(futureTarget)

            notificationManager.notify(0 /* ID of notification */, builder.build())
        }).start()

//        val executor = Executors.newFixedThreadPool(2)
//        executor.execute {
//            val futureTarget = Glide.with(this).asBitmap().load(senderPhotoUrl).submit()
//            builder.setLargeIcon(futureTarget.get())
//            Glide.with(this).clear(futureTarget)
//        }


    }

    companion object {
        val ONE_MEGABYTE: Long = 1024 * 1024
        private const val TAG = "MessagingJobService"
    }


}
