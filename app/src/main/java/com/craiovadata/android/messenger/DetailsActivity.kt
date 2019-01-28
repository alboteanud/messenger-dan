package com.craiovadata.android.messenger

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaRecorder.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.util.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.layout_enter_msg.*
import kotlinx.android.synthetic.main.message_list.*
import java.io.File
import java.io.IOException

private const val TAG = "DetailsActivity"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class DetailsActivity : AppCompatActivity() {

    var adapter : MessageAdapter? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var uidP: String
    private lateinit var convRef: DocumentReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar_msgs)

        uidP = intent.extras?.getString(KEY_USER_ID) ?: return
        title = intent.extras?.getString(KEY_USER_NAME)

        firestore = FirebaseFirestore.getInstance()
        setButtonsListeners()

    }

    public override fun onStart() {
        super.onStart()
        initList()
    }

    private fun initList() {
        FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->
            convRef = firestore.document("$USERS/${firebaseUser.uid}/$CONVERSATIONS/$uidP")

            val query = convRef.collection(MESSAGES).orderBy(TIMESTAMP, Query.Direction.DESCENDING).limit(30L)
            adapter =  MessageAdapter(query)
            recyclerMsgs.adapter = adapter

            adapter?.startListening()
        }
    }

    private fun setButtonsListeners() {
        sendButton.setOnClickListener { onSendClicked() }
        recordButton.setOnClickListener { startRecording() }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startUpload(recFile: String) {
        val file = Uri.fromFile(File(recFile))
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
                Log.d(TAG, "uploaded succesfully at: " + downloadUri)
                if (downloadUri != null) {
                    val txt = downloadUri.toString()
                    addMessage(txt)
                }
            } else {
                // Handle failures
            }
        }
    }

    private fun onSendClicked() {
        val txt = msgFormText.text
        if (!txt.isBlank()) {
            addMessage(txt.toString())
            msgFormText.text = null
        }
    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionToRecordAccepted = if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }

        if (!permissionToRecordAccepted) {
//            finish()
        }
    }

    private fun startPlaying(recFile: String) {
        MediaPlayer().apply {
            try {
                setDataSource(recFile)
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }
        recordButton.setColorFilter(getColor(R.color.colorRed))
        recordButton.isEnabled = false
        val recFile = "${externalCacheDir.absolutePath}/msg_record.3gp"
        MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(recFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setMaxDuration(3600)
            setOnInfoListener { mr, what, extra ->
                Log.d(TAG, "recorder what $what")
                when (what) {
                    MEDIA_RECORDER_INFO_MAX_DURATION_REACHED,
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED,
                    MEDIA_RECORDER_ERROR_UNKNOWN -> {
                        delete(this)
                        recordButton.clearColorFilter()
                        recordButton.isEnabled = true
                        startPlaying(recFile)
                    }
                }
            }
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            }
            start()
        }
    }

    private fun delete(recorder: MediaRecorder) {
            try {
                recorder.stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
            recorder.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)

        convRef.collection("block").document(uidP).get()
                .addOnSuccessListener { task ->
                    if (task.exists()) {
                        menu.findItem(R.id.menu_unblock).isVisible = true
                    } else {
                        menu.findItem(R.id.menu_block).isVisible = true
                    }
                }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_block -> {
                val data = HashMap<String, Any>()
                convRef.collection("block").document(uidP).set(data)
                invalidateOptionsMenu()
            }
            R.id.menu_unblock -> {
                convRef.collection("block").document(uidP).delete()
                invalidateOptionsMenu()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addMessage(msgText: String) {
        FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->

            val batch = firestore.batch()

            val msg = HashMap<String, Any?>()
            msg[TEXT] = msgText
            msg[AUTHOR] = firebaseUser.displayName ?: firebaseUser.email
            msg[TIMESTAMP] = System.currentTimeMillis().toString()

            batch.set(convRef, msg, SetOptions.merge())

            msg[UID] = firebaseUser.uid
            msg[PHOTO_URL] = firebaseUser.photoUrl.toString()

            batch.set(convRef.collection(MESSAGES).document(), msg)
            batch.commit()
        }
    }

}
