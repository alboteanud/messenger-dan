package com.craiovadata.android.messenger

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.MediaRecorder
import android.media.MediaRecorder.*
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.util.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.msg_editor.*
import kotlinx.android.synthetic.main.message_list.*
import java.io.File
import java.io.IOException
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.FirebaseFirestore
import androidx.recyclerview.widget.RecyclerView


private const val TAG = "DetailsActivity"
private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class DetailsActivity : AppCompatActivity() {

    private var adapter: MessageAdapter? = null
    private var uidP: String? = null
    private lateinit var conversationRef: DocumentReference
    private lateinit var firestore: FirebaseFirestore
    private var dataObserver: RecyclerView.AdapterDataObserver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar_msgs)
        initFirestore()
        getIntentExtras()
        attachRecyclerViewAdapter()
        sendButton.setOnClickListener { onSendTxtClicked() }
        recordButton.setOnClickListener { startRecording() }

    }

    private fun getIntentExtras() {
        intent.extras?.let {
            if (it.containsKey(KEY_USER_ID)) {
                uidP = it.getString(KEY_USER_ID)
            }
            if (it.containsKey(KEY_USER_NAME)) {
                title = it.getString(KEY_USER_NAME)
            }
        }
    }

    private fun initFirestore() {
        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore.firestoreSettings = settings
    }

    public override fun onStart() {
        super.onStart()
        adapter?.startListening()
        adapter?.registerAdapterDataObserver(dataObserver!!)
    }

    public override fun onStop() {
        super.onStop()
        adapter?.unregisterAdapterDataObserver(dataObserver!!)
        adapter?.stopListening()
    }

    private fun attachRecyclerViewAdapter() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        conversationRef = firestore
                .document("$USERS/${user.uid}/$CONVERSATIONS/$uidP")

        val query = conversationRef.collection(MESSAGES)
                .orderBy(MSG_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(30L)
        adapter = MessageAdapter(query)
        recyclerMsgs.adapter = adapter

        dataObserver = object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                recyclerMsgs.smoothScrollToPosition(0)
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startUpload(recordFileName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val file = Uri.fromFile(File(recordFileName))
        val ref = FirebaseStorage.getInstance().reference.child("sounds/users/$uid/$uidP/${file.lastPathSegment}")
        ref.putFile(file).addOnSuccessListener {
            //            val meta = it.metadata
//            val downloadUri = it.result
//            Log.d(TAG, "uploaded succesfully at: " + downloadUri)
            ref.downloadUrl.addOnSuccessListener {
                val downloadUri = it.toString()
                Log.d(TAG, "upload success $downloadUri")
            }
        }
    }

    private fun onSendTxtClicked() {
        val txt = msgFormText.text
        if (!txt.isBlank()) {
            addMessage(txt.toString())
            msgFormText.text = null
        }
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

    private fun startRecording() {
        if (!hasRecordPermission()) return

        recordButton.setColorFilter(getColor(android.R.color.black))
        val recFile = "${externalCacheDir.absolutePath}/msg_record.3gp"
        MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(recFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setMaxDuration(2000)
            setOnInfoListener { mr, what, extra ->
                Log.d(TAG, "recorder what $what")
                when (what) {
                    MEDIA_RECORDER_INFO_MAX_DURATION_REACHED,
                    MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED,
                    MEDIA_ERROR_SERVER_DIED,
                    MEDIA_RECORDER_ERROR_UNKNOWN -> {
                        delete(this)
                        recordButton.clearColorFilter()
                        startUpload(recFile)
                    }
                }
            }
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "prepare() failed")
            }

            start()
        }
    }

    private fun hasRecordPermission(): Boolean {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return false
        }
        return true
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
        conversationRef.collection("settings").document("blocked").get()
                .addOnSuccessListener { document ->
                    val item = menu.findItem(R.id.menu_block)
                    if (document.exists()) item.title = getString(R.string.menu_unblock_usr_label)
                    else item.title = getString(R.string.menu_block_usr_label)
                }

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_block -> {
                val ref = conversationRef.collection("settings").document("blocked")
                if (item.title == getString(R.string.menu_block_usr_label)) {
                    val data = HashMap<String, Any>()
                    ref.set(data)
                } else if (item.title == getString(R.string.menu_unblock_usr_label)) {
                    ref.delete()
                }
                Handler().postDelayed({ invalidateOptionsMenu() }, 500)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addMessage(msgText: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        val batch = firestore.batch()
        val now = System.currentTimeMillis().toString()

        val data = HashMap<String, Any?>()
        data[MSG_TEXT] = msgText
        data[MSG_AUTHOR] = currentUser.displayName ?: currentUser.email
        data[MSG_TIMESTAMP] = now
        data[TIMESTAMP_MODIF] = now

        // update conversation
        batch.set(conversationRef, data, SetOptions.merge())

        // add more stuff to msg - to update messages
        data[UID] = currentUser.uid
        data[PHOTO_URL] = currentUser.photoUrl.toString()

        batch.set(conversationRef.collection(MESSAGES).document(), data)
        batch.commit()

    }


}
