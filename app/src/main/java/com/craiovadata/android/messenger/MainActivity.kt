package com.craiovadata.android.messenger

import android.app.Activity
import android.app.SearchManager
import android.content.Intent
import android.database.MatrixCursor
import android.os.Bundle
import android.provider.BaseColumns
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.cursoradapter.widget.CursorAdapter
import androidx.cursoradapter.widget.SimpleCursorAdapter
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.RoomAdapter
import com.craiovadata.android.messenger.model.User
import com.craiovadata.android.messenger.util.Util.checkPlayServices
import com.craiovadata.android.messenger.util.Util.getKeywords
import com.craiovadata.android.messenger.util.UtilUI.setSearch
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.*
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
        RoomAdapter.OnRoomSelectedListener {

    lateinit var firestore: FirebaseFirestore
    lateinit var query: Query
    lateinit var adapter: RoomAdapter
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)


        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

//        FirebaseFirestore.setLoggingEnabled(true)
        firestore = FirebaseFirestore.getInstance()

        initRoomAdapter()

        checkPlayServices(this)
    }

    private fun initRoomAdapter() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        toolbar.title = user.displayName

        query = firestore.collection("users").document(user.uid).collection("rooms")
                .orderBy("lastMsgTime", Query.Direction.DESCENDING)
                .limit(LIMIT.toLong())

        adapter = object : RoomAdapter(query, this@MainActivity) {
            override fun onDataChanged() {
                // Show/hide content if the query returns empty.
                if (itemCount == 0) {
                    recyclerRestaurants.visibility = View.GONE
                    viewEmpty.visibility = View.VISIBLE
                } else {
                    recyclerRestaurants.visibility = View.VISIBLE
                    viewEmpty.visibility = View.GONE
                }
            }

            override fun onError(e: FirebaseFirestoreException) {
                // Show a snackbar on errors
                Snackbar.make(findViewById(android.R.id.content),
                        "Error: check logs for info.", Snackbar.LENGTH_LONG).show()
            }
        }

        recyclerRestaurants.layoutManager = LinearLayoutManager(this)
        recyclerRestaurants.adapter = adapter

    }

    public override fun onStart() {
        super.onStart()

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        // Start listening for Firestore updates
        if (::adapter.isInitialized) // maybe user = null so adapter not initialised
            adapter.startListening()

    }

    public override fun onStop() {
        super.onStop()
        if (::adapter.isInitialized) // maybe user = null so adapter not initialised
            adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        setSearch(this@MainActivity, menu.findItem(R.id.action_search))
        return true
//        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sign_out -> {
                unsubscribeAllTopics()
                AuthUI.getInstance().signOut(this)
                startSignIn()
            }
            R.id.menu_test -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun unsubscribeAllTopics() {
        val uid = FirebaseAuth.getInstance().uid
        firestore.collection("users/${uid}/rooms").get().addOnSuccessListener {snapshot ->
            for (document in snapshot) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(document.id)
            }

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                if (firebaseUser != null) {
                    writeNewUser(firebaseUser)
                    initRoomAdapter()
                    adapter.startListening()
                    subscribeToAllTopic()
                }
            } else {
                if (response == null) {
                    // User pressed the back button.
                    finish()
                } else if (response.error != null && response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    showSignInErrorDialog(R.string.message_no_network)
                } else {
                    showSignInErrorDialog(R.string.message_unknown)
                }
            }
        }
    }

    private fun subscribeToAllTopic() {
        val uid = FirebaseAuth.getInstance().uid
        firestore.collection("users/${uid}/rooms").get().addOnSuccessListener {snapshot ->
            for (document in snapshot) {
                FirebaseMessaging.getInstance().subscribeToTopic(document.id)
            }

        }
    }

    private fun writeNewUser(firebaseUser: FirebaseUser) {

        val email = firebaseUser.email.toString()
        val displayName = firebaseUser.displayName.toString()

        val keywords = getKeywords(email, displayName)

        val user = User(email, displayName, firebaseUser.photoUrl.toString(), keywords)
        val uid = firebaseUser.uid

        firestore.collection("users").document(uid).set(user)
    }

    override fun onRoomSelected(room: DocumentSnapshot) {
        // Go to the details page for the selected room
        val intent = Intent(this, MessagesActivity::class.java)
        intent.putExtra(MessagesActivity.KEY_ROOM_ID, room.reference.id)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    private fun shouldStartSignIn(): Boolean {
        return !viewModel.isSigningIn && FirebaseAuth.getInstance().currentUser == null
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build(),
                        AuthUI.IdpConfig.PhoneBuilder().build()
//                        AuthUI.IdpConfig.FacebookBuilder().build(),
//                        AuthUI.IdpConfig.TwitterBuilder().build()
                ))
                .setIsSmartLockEnabled(false)
                .build()

        startActivityForResult(intent, RC_SIGN_IN)
        viewModel.isSigningIn = true
    }

    private fun showSignInErrorDialog(@StringRes message: Int) {
        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.title_sign_in_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.option_retry) { _, _ -> startSignIn() }
                .setNegativeButton(R.string.option_exit) { _, _ -> finish() }.create()

        dialog.show()
    }

    companion object {

        const val TAG = "MainActivity"

        private const val RC_SIGN_IN = 9001

        private const val LIMIT = 50
    }
}
