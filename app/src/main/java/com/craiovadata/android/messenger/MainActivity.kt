package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
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
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.conversation_list.*

class MainActivity : AppCompatActivity(),
        ConversationAdapter.OnConversationSelectedListener {

    private lateinit var conversationAdapter: ConversationAdapter
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var user: FirebaseUser
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)

        firestore = FirebaseFirestore.getInstance()
        checkPlayServices(this)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

    }

    public override fun onStart() {
        super.onStart()
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        if (firebaseUser == null) {
            if (!viewModel.isSigningIn)
                startSignIn()
        } else {
            user = firebaseUser
            title = user.displayName
            setListAdapter()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (::conversationAdapter.isInitialized) conversationAdapter.stopListening()
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
                overridePendingTransition(com.craiovadata.android.messenger.R.anim.slide_in_from_right, com.craiovadata.android.messenger.R.anim.slide_out_to_left)
            }
            R.id.menu_sign_out -> {
                removeRegistration(this@MainActivity, user.uid)
                AuthUI.getInstance().signOut(this)
                startSignIn()
            }
            R.id.menu_test -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setListAdapter() {

        if (!::conversationAdapter.isInitialized) {
            val ref = firestore.collection("$USERS/${user.uid}/$CONVERSATIONS")
//            val query = ref.orderBy(LAST_MESSAGE_TIME, Query.Direction.DESCENDING)

            conversationAdapter = object : ConversationAdapter(ref, this@MainActivity) {}
            recyclerConversations.adapter = conversationAdapter
        }
        conversationAdapter.startListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {

                writeNewUser(this@MainActivity, user)

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

    override fun onConversationSelected(documentSnapshot: DocumentSnapshot) {
        val intent = Intent(this, DetailsActivity::class.java)
        intent.putExtra(KEY_USER_ID, documentSnapshot.reference.id)
        intent.putExtra(KEY_USER_NAME, documentSnapshot.data!!["palName"]!!.toString())
        intent.putExtra(KEY_USER_PHOTO_URL, documentSnapshot.data!!["palPhotoUrl"]!!.toString())
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }


    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.ic_person_black_24dp)
                .setTheme(R.style.AppTheme)
                .setTosAndPrivacyPolicyUrls(
                        "https://example.com/terms.html",
                        "https://sunshine-f15bf.firebaseapp.com/")
                .setAvailableProviders(listOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build()
                ))
                .enableAnonymousUsersAutoUpgrade()
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

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

}
