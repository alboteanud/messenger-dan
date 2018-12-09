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
import com.craiovadata.android.messenger.model.Rating
import com.craiovadata.android.messenger.model.Restaurant
import com.craiovadata.android.messenger.model.Room
import com.craiovadata.android.messenger.util.RestaurantUtil
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import kotlinx.android.synthetic.main.activity_restaurant_detail.*

class ConversationDetailActivity : AppCompatActivity(),
        EventListener<DocumentSnapshot>,
        MessageDialogFragment.MsgListener {

    private var messageDialog: MessageDialogFragment? = null
    private lateinit var firestore: FirebaseFirestore
    private lateinit var roomRef: DocumentReference
    private lateinit var palRoomRef: DocumentReference
    private lateinit var messageAdapter: MessageAdapter
    private var restaurantRegistration: ListenerRegistration? = null

    val selfUserId: String
        get() = FirebaseAuth.getInstance().currentUser!!.uid

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_restaurant_detail)

        // Get restaurant ID from extras
        val palUserId = intent.extras?.getString(KEY_RESTAURANT_ID)
                ?: throw IllegalArgumentException("Must pass extra $KEY_RESTAURANT_ID")

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()


        // Get reference to the restaurant
        roomRef = firestore.collection("users").document(selfUserId).collection("rooms").document(palUserId)
        palRoomRef = firestore.collection("users").document(palUserId).collection("rooms").document(selfUserId)

        // Get ratings
        val ratingsQuery = roomRef
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(50)

        // RecyclerView
        messageAdapter = object : MessageAdapter(ratingsQuery) {
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
        restaurantRegistration = roomRef.addSnapshotListener(this)
    }

    public override fun onStop() {
        super.onStop()

        messageAdapter.stopListening()

        restaurantRegistration?.remove()
        restaurantRegistration = null
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    /**
     * Listener for the Restaurant document ([.roomRef]).
     */
    override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
        if (e != null) {
            Log.w(TAG, "restaurant:onEvent", e)
            return
        }

        snapshot?.let {
            val restaurant = snapshot.toObject(Restaurant::class.java)
            if (restaurant != null) {
                onRestaurantLoaded(restaurant)
            }
        }
    }

    private fun onRestaurantLoaded(restaurant: Restaurant) {
        restaurantName.text = restaurant.name
        restaurantRating.rating = restaurant.avgRating.toFloat()
        restaurantNumRatings.text = getString(R.string.fmt_num_ratings, restaurant.numRatings)
        restaurantCity.text = restaurant.city
        restaurantCategory.text = restaurant.category
        restaurantPrice.text = RestaurantUtil.getPriceString(restaurant)

        // Background image
        Glide.with(restaurantImage.context)
                .load(restaurant.photo)
                .into(restaurantImage)
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

    private fun addRating(restaurantRef: DocumentReference, rating: Rating): Task<Void> {
        // Create reference for new rating, for use inside the transaction
        val ratingRef = restaurantRef.collection("ratings").document()

        // In a transaction, add the new rating and update the aggregate totals
        return firestore.runTransaction { transaction ->
            val restaurant = transaction.get(restaurantRef).toObject(Restaurant::class.java)
            if (restaurant == null) {
                throw Exception("Resraurant not found at ${restaurantRef.path}")
            }

            // Compute new number of ratings
            val newNumRatings = restaurant.numRatings + 1

            // Compute new average rating
            val oldRatingTotal = restaurant.avgRating * restaurant.numRatings
            val newAvgRating = (oldRatingTotal + rating.rating) / newNumRatings

            // Set new restaurant info
            restaurant.numRatings = newNumRatings
            restaurant.avgRating = newAvgRating

            // Commit to Firestore
            transaction.set(restaurantRef, restaurant)
            transaction.set(ratingRef, rating)

            null
        }
    }

    private fun addMessage(roomRef: DocumentReference, message: Message): Task<Void> {
        // Create reference for new message, for use inside the transaction
        val messagesRef = roomRef.collection("messages").document()
        val palMessagesRef = palRoomRef.collection("messages").document()

        // In a transaction, add the new message and update the aggregate totals
        return firestore.runTransaction { transaction ->

            var room = transaction.get(roomRef).toObject(Room::class.java)

            if (room==null){
                room = Room()
            }

            room.lastMsg = message.text
            room.author = message.userName

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

        private const val TAG = "RestaurantDetail"

        const val KEY_RESTAURANT_ID = "key_restaurant_id"
    }
}
