package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.RoomAdapter
import com.craiovadata.android.messenger.util.DbUtil.removeRegistration
import com.craiovadata.android.messenger.util.DbUtil.writeNewUser
import com.craiovadata.android.messenger.util.Util.checkPlayServices
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(),
        RoomAdapter.OnRoomSelectedListener,
        FirebaseAuth.AuthStateListener {


    lateinit var firestore: FirebaseFirestore
    lateinit var query: Query
    lateinit var roomAdapter: RoomAdapter
    private lateinit var viewModel: MainActivityViewModel
    lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        toolbar.logo = null

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

//        FirebaseFirestore.setLoggingEnabled(true)
        firestore = FirebaseFirestore.getInstance()

        auth = FirebaseAuth.getInstance()
        initRoomAdapter()

        checkPlayServices(this)
    }

    private fun initRoomAdapter() {

        toolbar.title = auth.currentUser?.email

        query = firestore.collection("users/${auth.currentUser?.uid}/rooms")
                .orderBy("lastMsgTime", Query.Direction.DESCENDING)
                .limit(LIMIT.toLong())

        roomAdapter = object : RoomAdapter(query, this@MainActivity, auth.currentUser?.displayName) {
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
        recyclerRestaurants.adapter = roomAdapter


    }

    public override fun onStart() {
        super.onStart()

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        auth.addAuthStateListener(this)

        // Start listening for Firestore updates
        if (::roomAdapter.isInitialized) // maybe auth = null so roomAdapter not initialised
            roomAdapter.startListening()

    }

    public override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(this)
        if (::roomAdapter.isInitialized) // maybe auth = null so roomAdapter not initialised
            roomAdapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> {
                val intent = Intent(this@MainActivity, SearchActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_sign_out -> {
                AuthUI.getInstance().signOut(this)
                startSignIn()
            }
            R.id.menu_test -> {

            }

        }
        return super.onOptionsItemSelected(item)
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
                    writeNewUser(this@MainActivity, firebaseUser)
                    initRoomAdapter()
                    roomAdapter.startListening()
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

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null) {
            removeRegistration(this@MainActivity, this.auth.uid)
        }
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
