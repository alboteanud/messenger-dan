package com.craiovadata.android.messenger

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.model.Conversation
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.DbUtil.addMessage
import com.craiovadata.android.messenger.util.DbUtil.getUserConversationsRef
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.layout_enter_msg.*
import kotlinx.android.synthetic.main.message_list.*
import java.io.File
import java.io.IOException

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class DetailsActivity : AppCompatActivity() {

    private lateinit var conversationID: String
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var conversation: Conversation
    private lateinit var auth: FirebaseAuth
    private var mRecorder: MediaRecorder? = null
    private var mFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar_msgs)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { currentUser ->
            if (intent.hasExtra(KEY_ROOM_ID)) {
                conversationID = intent.extras?.getString(KEY_ROOM_ID)!!
                fetchConversation(conversationID, currentUser)
                initMsgList(conversationID, currentUser)
            } else if (intent.hasExtra(KEY_USER_ID)) {
                val palUid = intent.extras?.getString(KEY_USER_ID)
                palUid?.let { palId -> findConversation(palId, currentUser) }
            }
        }


    }

    private fun setListeners() {
        sendButton.setOnClickListener { onMsgSubmitClicked() }

        recordButton.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    stopRecording()
                    startRecording()
                }
                MotionEvent.ACTION_UP -> {
                    //view.performClick()
                    stopRecording()
                    startUpload()
                }
            }
            return@OnTouchListener false
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(this@DetailsActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
            return
        }

        if (mRecorder == null) {
            mFileName = "${externalCacheDir.absolutePath}/audiorecordtest.3gp"
            mRecorder = MediaRecorder().apply {

                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile(mFileName)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            }
        }
        try {
            mRecorder!!.prepare()
            mRecorder!!.start()
        } catch (e: IOException) {
            Log.e(TAG, "prepare() failed")
        }
    }

    private fun stopRecording() {

        if (mRecorder == null) return

        mRecorder?.apply {
            try {
                stop()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        mRecorder = null
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
                Log.d(TAG, "uploaded succesfully at: " + downloadUri)
                if (downloadUri != null) {
                    auth.currentUser?.let {
                        //                        startPlaying(downloadUri.toString())
                        addMessage(conversationID, downloadUri.toString(), conversation, it)
                                .addOnSuccessListener(this) {
                                    Log.d(TAG, "Message added")
                                    recyclerMsgs.smoothScrollToPosition(0)

                                }
                                .addOnFailureListener(this) { e -> Log.w(TAG, "Add message failed", e) }
                    }
                }
            } else {
                // Handle failures
            }
        }
    }

    private fun fetchConversation(convID: String, currentUser: FirebaseUser) {
        getUserConversationsRef(currentUser.uid).document(convID).get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                conversation = snapshot.toObject(Conversation::class.java)!!
                initHeaderUI(conversation)
            }
        }
    }

    private fun findConversation(palUid: String, currentUser: FirebaseUser) {
        getUserConversationsRef(currentUser.uid).whereEqualTo(PAL_ID, palUid).limit(1).get()
                .addOnSuccessListener { snapshot ->

                    if (snapshot == null || snapshot.isEmpty) {
                        Log.d(TAG, "No such document. Creating new conversation.")
                        conversationID = getUserConversationsRef(currentUser.uid).document().id
                        setNewRoom(conversationID, palUid, currentUser.uid) // incl. updateHeadUI
                        setNewRoom(conversationID, currentUser.uid, palUid)
                    } else {
                        val document = snapshot.documents[0]
                        conversationID = document.id
                        conversation = document.toObject(Conversation::class.java)!!
                        initHeaderUI(conversation)
                    }
                    initMsgList(conversationID, currentUser)
                }

    }

    private fun setNewRoom(roomID: String, sourceUid: String, destUid: String) {
        FirebaseFirestore.getInstance().document("${USERS}/${sourceUid}").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            val newRoom = Conversation(user.name, snapshot.id, user.photoUrl)
                            if (destUid == auth.currentUser?.uid) {
                                conversation = newRoom
                                initHeaderUI(conversation)
                                FirebaseMessaging.getInstance().subscribeToTopic(roomID)
                            }
                            getUserConversationsRef(destUid).document(roomID).set(newRoom)
                        }
                    }
                }
    }

    private fun initHeaderUI(conversation: Conversation?) {
        toolbar_msgs.title = conversation?.palName
    }

    private fun initMsgList(conversationID: String, user: FirebaseUser) {
        val query = getUserConversationsRef(user.uid).document(conversationID).collection(MESSAGES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .limit(50L)

        messageAdapter = object : MessageAdapter(query, user) {}
        recyclerMsgs.adapter = messageAdapter
        messageAdapter.startListening()
        setListeners()
    }

    private fun onMsgSubmitClicked() {
        val msgText = msgFormText.text.toString()
        msgFormText.text = null
        auth.currentUser?.let { currentUser ->
            addMessage(conversationID, msgText, conversation, currentUser)
                    .addOnSuccessListener(this) {
                        Log.d(TAG, "Message added")
//                    hideKeyboard()
                        recyclerMsgs.smoothScrollToPosition(0)

                    }
                    .addOnFailureListener(this) { e -> Log.w(TAG, "Add message failed", e) }
        }

    }

    public override fun onStart() {
        super.onStart()
        if (::messageAdapter.isInitialized)
            messageAdapter.startListening()

    }

    public override fun onStop() {
        super.onStop()
        if (::messageAdapter.isInitialized)
            messageAdapter.stopListening()
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    companion object {
        val TAG = "DetailsActivity"
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return
            }

            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_details, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_block -> {

                val uid = auth.currentUser?.uid!!
                val palID = conversation.palId
                val batch = FirebaseFirestore.getInstance().batch()
                val docRefMe = getUserConversationsRef(uid).document(conversationID)
                val docRefPal = getUserConversationsRef(palID).document(conversationID)

                conversation.iBlockedHim = !conversation.iBlockedHim
                batch.update(docRefMe, "iBlockedHim", conversation.iBlockedHim)
                batch.update(docRefPal, "heBlockedMe", conversation.iBlockedHim)
                batch.commit()


            }
        }
        return super.onOptionsItemSelected(item)
    }

}
