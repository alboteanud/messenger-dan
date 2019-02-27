package com.craiovadata.android.messenger.services

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import com.craiovadata.android.messenger.util.Util
import com.google.common.collect.ComparisonChain.start
import io.grpc.internal.SharedResourceHolder.release
import java.util.concurrent.Executors

// TODO: Rename actions, choose action names that describe tasks that this
// IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
//private const val ACTION_FOO = "com.craiovadata.android.messenger.services.action.FOO"
//private const val ACTION_PLAY = "com.craiovadata.android.messenger.services.action.BAZ"

// TODO: Rename parameters
//private const val EXTRA_PARAM_PATH = "com.craiovadata.android.messenger.services.extra.PARAM1"
//private const val EXTRA_PARAM2 = "com.craiovadata.android.messenger.services.extra.PARAM2"

/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class PlayerIntentService : IntentService("PlayerIntentService") {

    override fun onHandleIntent(intent: Intent?) {
        when (intent?.action) {
            ACTION_FOO -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM_PATH)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionPlaySound(param1, param2)
            }
            ACTION_PLAY -> {
                val param1 = intent.getStringExtra(EXTRA_PARAM_PATH)
                val param2 = intent.getStringExtra(EXTRA_PARAM2)
                handleActionBaz(param1, param2)
            }
        }
    }

    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionPlaySound(param1: String, param2: String) {
        TODO("Handle action Foo")
    }

    /**
     * Handle action Baz in the provided background thread with the provided
     * parameters.
     */
    private fun handleActionBaz(param1: String, param2: String) {

        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            playSound(param1)
        }

    }

    companion object {

        private const val TAG = "PlayerIntentService"
        const val ACTION_FOO = "com.craiovadata.android.messenger.services.action.FOO"
       const val ACTION_PLAY = "com.craiovadata.android.messenger.services.action.BAZ"
        const val EXTRA_PARAM_PATH = "com.craiovadata.android.messenger.services.extra.PARAM1"
        const val EXTRA_PARAM2 = "com.craiovadata.android.messenger.services.extra.PARAM2"


        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionFoo(context: Context, param1: String, param2: String) {
            val intent = Intent(context, PlayerIntentService::class.java).apply {
                action = ACTION_FOO
                putExtra(EXTRA_PARAM_PATH, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }

        /**
         * Starts this service to perform action Baz with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        // TODO: Customize helper method
        @JvmStatic
        fun startActionBaz(context: Context, param1: String, param2: String) {
            val intent = Intent(context, PlayerIntentService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_PARAM_PATH, param1)
                putExtra(EXTRA_PARAM2, param2)
            }
            context.startService(intent)
        }
    }

    private fun playSound(path: String) {
        if (Util.isMuteAll(this)) return
//        val mediaSource = MyMediaDataSource(bytes)
        val mp = MediaPlayer().apply {
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


}
