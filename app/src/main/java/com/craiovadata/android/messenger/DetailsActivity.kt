package com.craiovadata.android.messenger

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.Room
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.DbUtil.addMessage
import com.craiovadata.android.messenger.util.DbUtil.getRoomsRef
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_details.*

class DetailsActivity : AppCompatActivity() {

    private lateinit var roomID: String
    private lateinit var messageAdapter: MessageAdapter
    private var room: Room? = null
    private lateinit var user: FirebaseUser
    val TAG = "Details Activity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        user = FirebaseAuth.getInstance().currentUser!!

        if (intent.hasExtra(KEY_ROOM_ID)) {
            roomID = intent.extras?.getString(KEY_ROOM_ID)!!
            fetchRoomAndUpdateHead(roomID)
            initMsgList(roomID, user)
            FirebaseMessaging.getInstance().subscribeToTopic(roomID)
        } else if (intent.hasExtra(KEY_USER_ID)) {
            val palUid = intent.extras?.getString(KEY_USER_ID)!!
            getRoom(palUid)
        }

        buttonBack.setOnClickListener { onBackArrowClicked() }
        sendButton.setOnClickListener { onMsgSubmitClicked() }
    }

    private fun fetchRoomAndUpdateHead(roomID: String) {
        getRoomsRef(user.uid).document(roomID).get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                room = snapshot.toObject(Room::class.java)
                initHeaderUI(room)
            }
        }
    }

    private fun getRoom(palUid: String) {
        getRoomsRef(user.uid).whereEqualTo(PAL_ID, palUid).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val document = snapshot.documents[0]
                        roomID = document.id
                        room = document.toObject(Room::class.java)!!
                        initHeaderUI(room)
                    } else {
                        Log.d(TAG, "No such document. Creating new room.")
                        roomID = getRoomsRef(user.uid).document().id
                        setNewRoom(roomID, palUid, user.uid) // incl. updateHeadUI
                        setNewRoom(roomID, user.uid, palUid)
                    }
                    initMsgList(roomID, user)
                }

    }

    private fun setNewRoom(roomID: String, sourceUid: String, destUid: String) {
        FirebaseFirestore.getInstance().document("${USERS}/${sourceUid}").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            val newRoom = Room(user.name, snapshot.id, user.photoUrl)
                            if (destUid == this.user.uid) {
                                room = newRoom
                                initHeaderUI(room)
                                FirebaseMessaging.getInstance().subscribeToTopic(roomID)
                            }
                            getRoomsRef(destUid).document(roomID).set(newRoom)
                        }
                    }
                }
    }

    private fun initHeaderUI(room: Room?) {
        conversationName.text = room?.palName
        val photoUrl = room?.palPhotoUrl
        Glide.with(palImage.context)
                .load(photoUrl)
                .into(palImage)
    }

    private fun initMsgList(roomID: String, user: FirebaseUser) {
        val query = getRoomsRef(user.uid).document(roomID).collection(MESSAGES)
                .orderBy(TIMESTAMP, Query.Direction.DESCENDING)
                .limit(20)

        messageAdapter = object : MessageAdapter(query, user) {}
        val manager = LinearLayoutManager(this)
        manager.reverseLayout = true
        recyclerMsgs.layoutManager = manager
        recyclerMsgs.adapter = messageAdapter
        messageAdapter.startListening()
    }


    private fun onMsgSubmitClicked() {
        val message = Message(user, msgFormText.text.toString())
        msgFormText.text = null
        addMessage(roomID, message, room?.palId, user.uid)
                .addOnSuccessListener(this) {
                    Log.d(TAG, "Message added")
//                    hideKeyboard()
                    recyclerMsgs.smoothScrollToPosition(0)

                }
                .addOnFailureListener(this) { e -> Log.w(TAG, "Add message failed", e) }

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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    private fun onBackArrowClicked() {
        onBackPressed()
    }


}
