package com.craiovadata.android.messenger

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.Room
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.DbUtil.addMessage
import com.craiovadata.android.messenger.util.DbUtil.getRoomsRef
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_conversation.*

class MessagesActivity : AppCompatActivity(),
        MessageDialogFragment.MsgListener {

    private var messageDialog: MessageDialogFragment? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomID: String
    private lateinit var messageAdapter: MessageAdapter
    private var room: Room? = null
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_conversation)
        firestore = FirebaseFirestore.getInstance()
        uid = FirebaseAuth.getInstance().currentUser!!.uid

        if (intent.hasExtra(KEY_ROOM_ID)) {
            roomID = intent.extras?.getString(KEY_ROOM_ID)!!
            fetchRoomAndUpdateHead(roomID)
            initMsgList(roomID)
            FirebaseMessaging.getInstance().subscribeToTopic(roomID)
        } else if (intent.hasExtra(KEY_USER_ID)) {
            val palUid = intent.extras?.getString(KEY_USER_ID)!!
            getRoom(palUid)
        }

        messageDialog = MessageDialogFragment()

        restaurantButtonBack.setOnClickListener { onBackArrowClicked() }
        fabShowRatingDialog.setOnClickListener { onAddMessageClicked() }
    }

    private fun fetchRoomAndUpdateHead(roomID: String) {
        getRoomsRef(uid).document(roomID).get().addOnSuccessListener { snapshot ->
            if (snapshot != null) {
                room = snapshot.toObject(Room::class.java)
                initHeaderUI(room)
            }
        }
    }

    private fun getRoom(palUid: String) {
        getRoomsRef(uid).whereEqualTo("palId", palUid).limit(1).get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null && !snapshot.isEmpty) {
                        val document = snapshot.documents[0]
                        roomID = document.id
                        room = document.toObject(Room::class.java)!!
                        initHeaderUI(room)
                    } else {
                        Log.d(TAG, "No such document. Creating new room.")
                        roomID = getRoomsRef(uid).document().id
                        setNewRoom(roomID, palUid, uid) // incl. updateHeadUI
                        setNewRoom(roomID, uid, palUid)
                    }
                    initMsgList(roomID)
                }

    }

    private fun setNewRoom(roomID: String, sourceUid: String, destUid: String) {
        firestore.document("${USERS}/${sourceUid}").get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot != null) {
                        val user = snapshot.toObject(User::class.java)
                        if (user != null) {
                            val newRoom = Room(user.name, snapshot.id, user.photoUrl)
                            if (destUid == uid) {
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
        restaurantName.text = room?.palName
        val photoUrl = room?.palPhotoUrl
        Glide.with(restaurantImage.context)
                .load(photoUrl)
                .into(restaurantImage)
    }

    private fun initMsgList(roomID: String) {
        val query = getRoomsRef(uid).document(roomID).collection(MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)

        messageAdapter = object : MessageAdapter(query) {
            override fun onDataChanged() {
                if (itemCount == 0) {
                    recyclerRatings.visibility = View.VISIBLE
                    viewEmptyRatings.visibility = View.VISIBLE
                } else {
                    recyclerRatings.visibility = View.VISIBLE
                    viewEmptyRatings.visibility = View.GONE
                }
            }
        }
        recyclerRatings.layoutManager = LinearLayoutManager(this)
        recyclerRatings.adapter = messageAdapter

        messageAdapter.startListening()
    }

    private fun onAddMessageClicked() {
        messageDialog?.show(supportFragmentManager, MessageDialogFragment.TAG)
    }

    override fun onMessage(message: Message) {
        // In a transaction, add the new rating and update the aggregate totals
        addMessage(firestore, roomID, message, room?.palId, uid)
                .addOnSuccessListener(this) {
                    Log.d(TAG, "Rating added")

                    // Hide keyboard and scroll to top
                    hideKeyboard()
                    recyclerRatings.smoothScrollToPosition(0)
                }
                .addOnFailureListener(this) { e ->
                    Log.w(TAG, "Add rating failed", e)

                    // Show failure message and hide keyboard
                    hideKeyboard()
                    Snackbar.make(findViewById(android.R.id.content), "Failed to add rating",
                            Snackbar.LENGTH_SHORT).show()
                }

    }


    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
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

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    private fun onBackArrowClicked() {
        onBackPressed()
    }


    companion object {
        private const val TAG = "RoomMessages"
        const val KEY_ROOM_ID = "key_room_id"
        const val KEY_USER_ID = "key_user_id"
        const val USERS = "users"
        const val MESSAGES = "messages"
    }
}
