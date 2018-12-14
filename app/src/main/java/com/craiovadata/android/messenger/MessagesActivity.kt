package com.craiovadata.android.messenger

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.MessageAdapter
import com.craiovadata.android.messenger.model.Message
import com.craiovadata.android.messenger.model.Room
import com.craiovadata.android.messenger.model.User
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.activity_restaurant_detail.*

class MessagesActivity : AppCompatActivity(),
        EventListener<DocumentSnapshot>,
        MessageDialogFragment.MsgListener {

    private var messageDialog: MessageDialogFragment? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomRef: DocumentReference
    private lateinit var messageAdapter: MessageAdapter
    private var listenerRegistration: ListenerRegistration? = null
    private lateinit var palUid: String
    var palUser: User? = null
    val uid: String
        get() = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_detail)

        palUid = intent.extras?.getString(KEY_ROOM_ID)
                ?: throw IllegalArgumentException("Must pass extra $KEY_ROOM_ID")

        firestore = FirebaseFirestore.getInstance()

        val palUserRef = firestore.collection("users").document(palUid)
        palUserRef.get().addOnSuccessListener { result ->
                    palUser = result.toObject(User::class.java)
                }

        roomRef = firestore.collection("users").document(uid).collection("rooms").document(palUid)

        val query = roomRef
                .collection("messages")
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

        messageDialog = MessageDialogFragment()

        restaurantButtonBack.setOnClickListener { onBackArrowClicked() }
        fabShowRatingDialog.setOnClickListener { onAddRatingClicked() }
    }

    public override fun onStart() {
        super.onStart()

        messageAdapter.startListening()
        listenerRegistration = roomRef.addSnapshotListener(this)
    }

    public override fun onStop() {
        super.onStop()

        messageAdapter.stopListening()

        listenerRegistration?.remove()
        listenerRegistration = null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
        if (e != null) {
            Log.w(TAG, "restaurant:onEvent", e)
            return
        }

        snapshot?.let {
            val room = snapshot.toObject(Room::class.java)
            if (room != null) {
                onRoomLoaded(room)
            }
        }
    }

    private fun onRoomLoaded(room: Room) {
        var participants = ""
        for (one in room.participants){
            if (one["uid"] != uid)
                participants+= (one["name"].toString() + "\n")
        }


        restaurantName.text = participants

//        Glide.with(restaurantImage.context)
//                .load(room.photo)
//                .into(restaurantImage)
    }

    private fun onBackArrowClicked() {
        onBackPressed()
    }

    private fun onAddRatingClicked() {
        messageDialog?.show(supportFragmentManager, RatingDialogFragment.TAG)
    }

    override fun onMessage(message: Message) {
        // In a transaction, add the new rating and update the aggregate totals
        addMessage(roomRef, message)
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

    private fun addMessage(roomRef: DocumentReference, message: Message): Task<Void> {
        // Create reference for new message, for use inside the transaction
        val messagesRef = roomRef.collection("messages").document()
        val palRoomRef = firestore.collection("users").document(palUid).collection("rooms").document(uid)
        val palMessagesRef = palRoomRef.collection("messages").document()

        // In a transaction, add the new message and update the aggregate totals
        return firestore.runTransaction { transaction ->

            var room = transaction.get(roomRef).toObject(Room::class.java)

            if (room == null) {
                room = Room()

                val usr = HashMap<String, Any>()
                usr.put("name", message.displayName!!)
                usr.put("uid", message.userId!!)
                usr.put("photoUrl", message.photoUrl!!)

                val usrPal = HashMap<String, Any>()
                usrPal.put("name", palUser!!.name)
                usrPal.put("uid", palUid)
                usrPal.put("photoUrl", palUser!!.photoUrl)

                room.participants = listOf(usr, usrPal)
            }

            room.lastMsg = message.text
            room.lastMsgAuthor = message.displayName

            transaction.set(roomRef, room)
            transaction.set(palRoomRef, room)
            transaction.set(messagesRef, message)
            transaction.set(palMessagesRef, message)

            null
        }


    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    companion object {

        private const val TAG = "RoomMessages"

        const val KEY_ROOM_ID = "key_room_id"
    }
}
