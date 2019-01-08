package com.craiovadata.android.messenger

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.model.Conversation
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.DbUtil.addMessage
import com.craiovadata.android.messenger.util.DbUtil.getRoomsRef
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import java.io.File
import java.io.IOException


class DetailsActivity : AppCompatActivity(), View.OnTouchListener {


    private lateinit var conversationID: String
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var conversation: Conversation
    private lateinit var auth: FirebaseAuth
    private var mRecorder: MediaRecorder? = null
    private var mFileName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (intent.hasExtra(KEY_ROOM_ID)) {
                conversationID = intent.extras?.getString(KEY_ROOM_ID)!!
                fetchConversation(conversationID, currentUser)
                initMsgList(conversationID, currentUser)
            } else if (intent.hasExtra(KEY_USER_ID)) {
                val palUid = intent.extras?.getString(KEY_USER_ID)!!
                findConversation(palUid, currentUser)
            }
        }

        mFileName = "${externalCacheDir.absolutePath}/audiorecordtest2.3gp"

        buttonBack.setOnClickListener { onBackArrowClicked() }
        sendButton.setOnClickListener { onMsgSubmitClicked() }
        recordButton.setOnTouchListener(this)


    }

    override fun onTouch(recordButton: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (mRecorder == null)
                    startRecording()
                else {
                    stopRecording()
                    startRecording()
                }

            }
            MotionEvent.ACTION_UP -> {

                stopRecording()
                startUpload()
            }
        }
        recordButton.performClick()
        return true
    }

    private fun startRecording() {
        mRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(mFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
            } catch (e: IOException) {
                Log.e(TAG, "prepare() failed")
            }
        }
    }

    private fun stopRecording() {
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
                        startPlaying(downloadUri.toString())
                        addMessage(conversationID, downloadUri.toString(), conversation.palId, it)
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

    private fun startPlaying(url: String) {
        MediaPlayer().apply {
            try {
                setDataSource(this@DetailsActivity, Uri.parse(url))
                prepare()
                start()
            } catch (e: Throwable) {
                Log.e("MsgingService", "prepare() failed")
            }
        }
    }

    private fun fetchConversation(roomID: String, currentUser: FirebaseUser) {
        getRoomsRef(currentUser.uid).document(roomID).get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                conversation = snapshot.toObject(Conversation::class.java)!!
                initHeaderUI(conversation)
            }
        }
    }

    private fun findConversation(palUid: String, currentUser: FirebaseUser) {
        getRoomsRef(currentUser.uid).whereEqualTo(PAL_ID, palUid).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val document = snapshot.documents[0]
                        conversationID = document.id
                        conversation = document.toObject(Conversation::class.java)!!
                        initHeaderUI(conversation)
                    } else {
                        Log.d(TAG, "No such document. Creating new conversation.")
                        conversationID = getRoomsRef(currentUser.uid).document().id
                        setNewRoom(conversationID, palUid, currentUser.uid) // incl. updateHeadUI
                        setNewRoom(conversationID, currentUser.uid, palUid)
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
                            getRoomsRef(destUid).document(roomID).set(newRoom)
                        }
                    }
                }
    }

    private fun initHeaderUI(conversation: Conversation?) {
        conversationName.text = conversation?.palName
        val photoUrl = conversation?.palPhotoUrl
        Glide.with(palImage.context)
                .load(photoUrl)
                .into(palImage)
    }

    private fun initMsgList(conversationID: String, user: FirebaseUser) {
        val query = getRoomsRef(user.uid).document(conversationID).collection(MESSAGES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .limit(50L)

        messageAdapter = object : MessageAdapter(query, user) {}
        val manager = LinearLayoutManager(this)
        manager.reverseLayout = true
        recyclerMsgs.layoutManager = manager
        recyclerMsgs.adapter = messageAdapter
        messageAdapter.startListening()
    }

    private fun onMsgSubmitClicked() {
        val msgText = msgFormText.text.toString()
        msgFormText.text = null
        auth.currentUser?.let {
            addMessage(conversationID, msgText, conversation.palId, it)
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
        if (::messageAdapter.isInitialized) {
            messageAdapter.startListening()

            // from notif
            Handler().postDelayed({ recyclerMsgs.smoothScrollToPosition(0) }, 1500)
        }

    }

    public override fun onStop() {
        super.onStop()
        if (::messageAdapter.isInitialized)
            messageAdapter.stopListening()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    private fun onBackArrowClicked() {
        onBackPressed()
    }

    companion object {
        val TAG = "DetailsActivity"
    }

}
