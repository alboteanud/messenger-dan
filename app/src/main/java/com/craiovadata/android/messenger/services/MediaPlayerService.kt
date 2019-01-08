package com.craiovadata.android.messenger.services

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder


const val ACTION_PLAY: String = "com.craiovadata.action.PLAY"
const val URL_EXTRA_PLAY: String = "com.craiovadata.play.URL"

class MediaPlayerService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var mMediaPlayer: MediaPlayer? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        val action: String = intent.action

        when (action) {
            ACTION_PLAY -> {
                mMediaPlayer = MediaPlayer()
                mMediaPlayer?.apply {
                    val s = intent.extras?.getString(URL_EXTRA_PLAY)
                    setDataSource(s)
                    setOnPreparedListener { start() }
                    setOnCompletionListener { release() }
                    prepareAsync() // prepare async to not block main thread

                }

            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

}