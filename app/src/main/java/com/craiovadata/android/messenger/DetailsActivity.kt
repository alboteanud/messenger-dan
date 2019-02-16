package com.craiovadata.android.messenger

import android.Manifest
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.MediaActionSound
import android.media.MediaActionSound.START_VIDEO_RECORDING
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
import com.craiovadata.android.messenger.model.ConversationUpdate
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.util.*
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.message_list.*
import kotlinx.android.synthetic.main.msg_editor.*
import java.io.File
import java.io.IOException


class DetailsActivity : AppCompatActivity() {

    private lateinit var adapter: MessageAdapter
    private lateinit var uidP: String
    private lateinit var conversationRef: DocumentReference
    private lateinit var firestore: FirebaseFirestore
    private var iAmBlocked = false
    private var iAmBlockedListenerRegistration: ListenerRegistration? = null
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar)
        firestore = FirebaseFirestore.getInstance()
        getIntentExtras()
        setUpRecyclerView()
        sendButton.setOnClickListener { onSendTxtClicked() }
        recordButton.setOnClickListener { startRecording() }

    }

    private fun monitorIamBlockedStatus() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val uid = currentUser.uid
        val conversationRefP = firestore.document("$USERS/$uidP/$CONVERSATIONS/$uid/settings/palSettings")

        iAmBlockedListenerRegistration = conversationRefP.addSnapshotListener(EventListener<DocumentSnapshot> { snapshot, e ->
            if (e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@EventListener
            }

            if (snapshot != null && snapshot.exists()) {
                Log.d(TAG, "Current data:  " + snapshot.data)
                val conversationData = snapshot.data

                val blkState = conversationData?.get(BLOCKED_HIM)
                if (blkState is Boolean) iAmBlocked = blkState
                Log.d(TAG, "iAmBlocked: " + iAmBlocked)
            } else {
                Log.d(TAG, "Current data: null")
            }
        })
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

    public override fun onStart() {
        super.onStart()
        adapter.startListening()
        monitorIamBlockedStatus()
    }

    public override fun onStop() {
        super.onStop()
        adapter.stopListening()
        iAmBlockedListenerRegistration?.remove()
    }

    private fun setUpRecyclerView() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        conversationRef = firestore
                .document("$USERS/${user.uid}/$CONVERSATIONS/$uidP")

        val query = conversationRef.collection(MESSAGES)
                .orderBy(MSG_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(30L)

        val options = FirestoreRecyclerOptions.Builder<Message>()
                .setQuery(query, Message::class.java)
                .build()

        adapter = MessageAdapter(options)
        recyclerMsgs.setHasFixedSize(true)
        recyclerMsgs.adapter = adapter
        adapter.setOnItemAddedListener(object : MessageAdapter.OnItemAddedListener {
            override fun onItemAdded() {
                recyclerMsgs.smoothScrollToPosition(0)
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startUpload(recordFileName: String) {
        if (iAmBlocked) return
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val file = Uri.fromFile(File(recordFileName))
        val storageRef = FirebaseStorage.getInstance().reference
        val ref = storageRef.child("sounds/users/${user.uid}/${uidP}/${file.lastPathSegment}")
        Log.d(TAG, "ref  " + ref)
        val metadata = StorageMetadata.Builder()
                .setContentType("audio/3gpp")
                .setCustomMetadata("author", user.displayName ?: user.email)

                .build()
        val uploadTask = ref.putFile(file, metadata)

        val urlTask = uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let { it.printStackTrace() }
            }
            return@Continuation ref.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                Log.d(TAG, "sound upload success " + downloadUri)
            } else {
                // Handle failures
                Log.e(TAG, "sound upload failed ")
            }
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

        val recFile = "${externalCacheDir.absolutePath}/sound.3gp"
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
                        resetFab()
                        mr.release()
                        startUpload(recFile)
                    }
                }
            }
            try {
                prepare()

                recordButton.isEnabled = false
                MediaActionSound().play(START_VIDEO_RECORDING)
                recordButton.setColorFilter(getColor(R.color.greyPrimary))
                Handler().postDelayed({ resetFab() }, 3000)
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            } catch (e: IllegalStateException) {
                Log.e(TAG, "prepare() failed")
            }
            start()
        }
    }

    private fun resetFab() {
        recordButton.clearColorFilter()
        recordButton.isEnabled = true
    }

    private fun hasRecordPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return false
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
        this.menu = menu
        checkHeIsBlockedState()
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_block -> {
                val data = HashMap<String, Any>()
                data[BLOCKED_HIM] = true
                val ref = conversationRef.collection("settings").document("palSettings")
                ref.set(data)
                Handler().postDelayed({ setHeIsBlockedUI(true) }, 800)
            }
            R.id.menu_unblock -> {
                val data = HashMap<String, Any>()
                data[BLOCKED_HIM] = false
                val ref = conversationRef.collection("settings").document("palSettings")
                ref.set(data)
                Handler().postDelayed({ setHeIsBlockedUI(false) }, 800)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkHeIsBlockedState() {
        conversationRef.get().addOnSuccessListener { document ->
            if (document != null) {
                Log.d(ChatRoomActivity.TAG, "DocumentSnapshot data: " + document.data)
                val userData = document.data

                val iBlockedHim = userData?.get(BLOCKED_HIM)
                if (iBlockedHim is Boolean) setHeIsBlockedUI(iBlockedHim)

            } else {
                Log.d(ChatRoomActivity.TAG, "No such document")
            }
        }
    }

    private fun setHeIsBlockedUI(iBlockedHim: Boolean) {
        menu.findItem(R.id.menu_unblock).isVisible = iBlockedHim
        menu.findItem(R.id.menu_block).isVisible = !iBlockedHim
    }

    private fun addMessage(text: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val msg = Message(user, text)
        val conversationUpdate = ConversationUpdate(user, text)

        val batch = firestore.batch()
        batch.set(conversationRef, conversationUpdate, SetOptions.merge())
        batch.set(conversationRef.collection(MESSAGES).document(), msg)
        if (!iAmBlocked) {
            val conversationRefP = firestore.document("$USERS/$uidP/$CONVERSATIONS/${user.uid}")
            batch.set(conversationRefP.collection(MESSAGES).document(), msg)
            batch.set(conversationRefP, conversationUpdate, SetOptions.merge())
        }
        batch.commit()
    }

    private fun onSendTxtClicked() {
        val txt = msgFormText.text
        if (!txt.isBlank()) {
            addMessage(txt.toString())
            msgFormText.text = null
        }
    }

    companion object {
        private const val TAG = "DetailsActivity"
        private const val BLOCKED_HIM = "blockedHim"
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }

}
