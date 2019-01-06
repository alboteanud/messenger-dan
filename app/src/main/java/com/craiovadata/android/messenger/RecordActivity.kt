package com.craiovadata.android.messenger

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.IOException


private const val LOG_TAG = "AudioRecordTest"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class RecordActivity : AppCompatActivity() {

    private var mFileName: String = ""

    private var mRecordButton: RecordButton? = null
    private var mRecorder: MediaRecorder? = null

    private var mPlayButton: PlayButton? = null
    private var mUploadButton: UploadButton? = null
    private var mPlayer: MediaPlayer? = null

    // Requesting permission to RECORD_AUDIO
    private var permissionToRecordAccepted = false
    private var permissions: Array<String> = arrayOf(Manifest.permission.RECORD_AUDIO)

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        if (!permissionToRecordAccepted) finish()
    }

    private fun onRecord(start: Boolean) = if (start) {
        startRecording()
    } else {
        stopRecording()
    }

    private fun onPlay(start: Boolean) = if (start) {
        startPlaying()
    } else {
        stopPlaying()
    }

    private fun onUpload() {
        startUpload()

    }

    private fun startUpload() {
        val file = Uri.fromFile(File(mFileName))
        val ref = FirebaseStorage.getInstance().reference.child("sounds/${file.lastPathSegment}")
        val uploadTask = ref.putFile(file)

        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d(LOG_TAG, "uploaded succesfully at: " + downloadUri)
                if (downloadUri != null) {
                    startPlayingUri(downloadUri)
                }
            } else {
                // Handle failures
            }
        }
    }

    private fun startPlaying() {
        mPlayer = MediaPlayer().apply {
            try {
                setDataSource(mFileName)
                prepare()
                start()
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun startPlayingUri(downloadUri: Uri) {
        mPlayer = MediaPlayer().apply {
            try {
                setDataSource(this@RecordActivity, downloadUri)
                prepare()
                start()
            } catch (e: Throwable) {
                Log.e(LOG_TAG, "prepare() failed")
            }
        }
    }

    private fun stopPlaying() {
        mPlayer?.release()
        mPlayer = null
    }

    private fun startRecording() {
        mRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(mFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e(LOG_TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        mRecorder?.apply {
            stop()
            release()
        }
        mRecorder = null
    }

    internal inner class RecordButton(ctx: Context) : Button(ctx) {

        var mStartRecording = true

        var clicker: OnClickListener = OnClickListener {
            onRecord(mStartRecording)
            text = when (mStartRecording) {
                true -> "Stop recording"
                false -> "Start recording"
            }
            mStartRecording = !mStartRecording
        }

        init {
            text = "Start recording"
            setOnClickListener(clicker)
        }
    }

    internal inner class PlayButton(ctx: Context) : Button(ctx) {
        var mStartPlaying = true
        var clicker: OnClickListener = OnClickListener {
            onPlay(mStartPlaying)
            text = when (mStartPlaying) {
                true -> "Stop playing"
                false -> "Start playing"
            }
            mStartPlaying = !mStartPlaying
        }

        init {
            text = "Start playing"
            setOnClickListener(clicker)
        }
    }

    internal inner class UploadButton(ctx: Context) : Button(ctx) {
        var clicker: OnClickListener = OnClickListener {
            onUpload()
        }

        init {
            text = "Upload"
            setOnClickListener(clicker)
        }
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Record to the external cache directory for visibility
        mFileName = "${externalCacheDir.absolutePath}/audiorecordtest.3gp"

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION)

        mRecordButton = RecordButton(this)
        mPlayButton = PlayButton(this)
        mUploadButton = UploadButton(this)
        val ll = LinearLayout(this).apply {
            addView(mRecordButton,
                    LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            0f))
            addView(mPlayButton,
                    LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            0f))
            addView(mUploadButton,
                    LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            0f))
        }
        setContentView(ll)
    }

    override fun onStop() {
        super.onStop()
        mRecorder?.release()
        mRecorder = null
        mPlayer?.release()
        mPlayer = null
    }
}


