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
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.layout_enter_msg.*
import kotlinx.android.synthetic.main.message_list.*
import java.io.File
import java.io.IOException
import java.util.HashMap

private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

class DetailsActivity : AppCompatActivity() {

    private var palID: String = ""
    private var palName: String = ""
    private var palPhotoUrl: String = ""
    private lateinit var adapter: MessageAdapter
    private lateinit var conversation: Conversation
    private lateinit var user: FirebaseUser
    private var mRecorder: MediaRecorder? = null
    private var mFileName: String = ""
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar_msgs)

        palID = intent.extras?.getString(KEY_USER_ID)!!
        palName = intent.extras?.getString(KEY_USER_NAME)!!
        palPhotoUrl = intent.extras?.getString(KEY_USER_PHOTO_URL)!!

        title = palName
        firestore = FirebaseFirestore.getInstance()
        updateHeaderUI()

        setButtonsListeners()
    }

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().currentUser?.let {
            user = it
            adapter = object : MessageAdapter(getMsgsQuery(), user) {}
            recyclerMsgs.adapter = adapter
            adapter.startListening()
        }

    }

    private fun setButtonsListeners() {
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
                    val txt = downloadUri.toString()
                    addMessage(txt)
                }
            } else {
                // Handle failures
            }
        }
    }

    private fun updateHeaderUI() {
        firestore.document("$USERS/$palID").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            title = user.name
                        }
                    }
                }
    }

    private fun onMsgSubmitClicked() {
        val msgText = msgFormText.text.toString()
        addMessage(msgText)
        msgFormText.text = null
    }

    public override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) adapter.stopListening()
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

                val batch = firestore.batch()
                conversation.iBlockedHim = !conversation.iBlockedHim
                batch.update(getMyConversationRef(), "iBlockedHim", conversation.iBlockedHim)
                batch.update(getPalConversationRef(), "heBlockedMe", conversation.iBlockedHim)
                batch.commit()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addMessage(msgText: String) {
        val message = Message(user, msgText)

        val myMsgRef = getMyConversationRef().collection(MESSAGES).document()
        val palMsgRef = getPalConversationRef().collection(MESSAGES).document(myMsgRef.id)

        val batch = firestore.batch()

        val mapForMe = HashMap<String, Any>()
        mapForMe[LAST_MESSAGE_AUTHOR] = user.displayName!!
        mapForMe[LAST_MESSAGE] = msgText
        mapForMe[PAL_ID] = palID
        mapForMe[PAL_NAME] = palName
        mapForMe[PAL_PHOTO_URL] = palPhotoUrl

        val mapForPal = HashMap<String, Any>()
        mapForPal[LAST_MESSAGE_AUTHOR] = user.displayName!!
        mapForPal[LAST_MESSAGE] = msgText
        mapForPal[PAL_ID] = user.uid
        mapForPal[PAL_NAME] = user.displayName!!
        mapForPal[PAL_PHOTO_URL] = user.photoUrl!!.toString()

        batch.set(myMsgRef, message)
        batch.set(palMsgRef, message)

        batch.set(getMyConversationRef(), mapForMe)
        batch.set(getPalConversationRef(), mapForPal)

//        if (!conversation.heBlockedMe) {
        batch.commit()
    }

    private fun getMyConversationRef(): DocumentReference {
        return firestore.document("$USERS/${user.uid}/$CONVERSATIONS/$palID")
    }

    private fun getPalConversationRef(): DocumentReference {
        return firestore.document("$USERS/$palID/$CONVERSATIONS/${user.uid}")
    }

    private fun getMsgsQuery(): Query {
        return getMyConversationRef().collection(MESSAGES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .limit(50L)
    }

}
