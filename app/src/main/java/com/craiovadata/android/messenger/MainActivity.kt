package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.craiovadata.android.messenger.adapter.ConversationAdapter
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.DbUtil.removeRegistration
import com.craiovadata.android.messenger.util.DbUtil.writeNewUser
import com.craiovadata.android.messenger.util.Util.checkPlayServices
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS



class MainActivity : AppCompatActivity(),
        ConversationAdapter.OnRoomSelectedListener,
        FirebaseAuth.AuthStateListener {

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var auth: FirebaseAuth
    private var uid: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser

        if (currentUser != null) {
            uid = currentUser.uid
            supportActionBar?.title = currentUser.displayName
            initListAdapter(uid)
        }

        checkPlayServices(this)

    }

    public override fun onStart() {
        super.onStart()

        if (shouldStartSignIn())
            startSignIn()

        auth.addAuthStateListener(this)

        if (::conversationAdapter.isInitialized)
            conversationAdapter.startListening()

    }

    public override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(this)
        if (::conversationAdapter.isInitialized)
            conversationAdapter.stopListening()
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

    private fun initListAdapter(uid: String) {
        val ref = FirebaseFirestore.getInstance().collection("$USERS/$uid/$CONVERSATIONS")
        val query = ref.orderBy(LAST_MESSAGE_TIME, Query.Direction.DESCENDING)

        conversationAdapter = object : ConversationAdapter(query, this@MainActivity) {}
        recyclerConversations.layoutManager = LinearLayoutManager(this)
        recyclerConversations.adapter = conversationAdapter

        conversationAdapter.startListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    uid = currentUser.uid
                    writeNewUser(this@MainActivity, currentUser)
                    initListAdapter(currentUser.uid)

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
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(KEY_ROOM_ID, room.reference.id)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    private fun shouldStartSignIn(): Boolean {
        return !viewModel.isSigningIn && auth.currentUser == null
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        if (auth.currentUser == null && !uid.equals("")) {
            removeRegistration(this@MainActivity, uid)
        }
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
//                        AuthUI.IdpConfig.EmailBuilder().build(),
//                        AuthUI.IdpConfig.PhoneBuilder().build(),
                        AuthUI.IdpConfig.FacebookBuilder().build()
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


}
