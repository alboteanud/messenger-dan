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
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.util.*
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.layout_enter_msg.*
import kotlinx.android.synthetic.main.message_list.*
import java.io.File
import java.io.IOException

class DetailsActivity : AppCompatActivity() {


    private var mRecorder: MediaRecorder? = null
    private var mFileName: String = ""
    private lateinit var adapter: MessageAdapter
    private lateinit var user: FirebaseUser
    private lateinit var firestore: FirebaseFirestore
    private lateinit var palId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        setSupportActionBar(toolbar_msgs)

        palId = intent.extras?.getString(KEY_USER_ID) ?: throw Exception("invalid palId")
        title = intent.extras?.getString(KEY_USER_NAME)
        firestore = FirebaseFirestore.getInstance()

        setButtonsListeners()
    }

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().currentUser?.let {
            user = it
            adapter = object : MessageAdapter(getQuery(), user) {}
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

    private fun onMsgSubmitClicked() {
        val txt = msgFormText.text
        if (!txt.isBlank()) {
            addMessage(txt.toString())
            msgFormText.text = null
        }
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

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addMessage(msgText: String) {
        val msg =  Message(user, msgText)

        val convRef = firestore.document("$USERS/${user.uid}/$CONVERSATIONS/$palId")
        val convRefPal = firestore.document("$USERS/${palId}/$CONVERSATIONS/${user.uid}")
        val msgRef = convRef.collection(MESSAGES).document()
        val msgRefPal = convRefPal.collection(MESSAGES).document()

        firestore.batch()
                .set(msgRef, msg)
                .set(msgRefPal, msg)
                .commit()
                .addOnSuccessListener { result ->
                    Log.d(TAG, "Transaction success: " + result)
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Transaction failure.", e)
                }

        // TODO for test only
        convRef.update("lastMessage", msgText)
    }

    private fun getQuery(): Query {
        val convRef = firestore.document("$USERS/${user.uid}/$CONVERSATIONS/$palId")
        return convRef.collection(MESSAGES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .limit(30L)
    }

}
