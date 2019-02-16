package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.android.messenger.adapter.ConversationAdapter
import com.craiovadata.android.messenger.model.Conversation
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.Util.checkPlayServices
import com.craiovadata.android.messenger.util.Util.getRegistrationAndSendToServer
import com.craiovadata.android.messenger.util.Util.removeRegistration
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.conversation_list.*

class MainActivity : AppCompatActivity(),
        ConversationAdapter.OnConversationSelectedListener {

    private var adapter: ConversationAdapter? = null
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var firestore: FirebaseFirestore
    private var isMuteAllSounds: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)
        checkPlayServices(this)
        firestore = FirebaseFirestore.getInstance()

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
        isMuteAllSounds = Util.isMuteAll(this)
        setUpRecyclerView(false)
    }

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().currentUser?.let {
            adapter?.startListening()
            title = it.displayName
        } ?: if (!viewModel.isSigningIn) {
            startSignIn()
            finish()
        }

    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        if (isMuteAllSounds) menu.findItem(R.id.menu_mute_all).title = getString(R.string.unmute_all_menu_label)
        else menu.findItem(R.id.menu_mute_all).title = getString(R.string.mute_all_menu_label)

        if (BuildConfig.DEBUG) menu.findItem(R.id.menu_test).isVisible = true

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

                removeRegistration()
                AuthUI.getInstance().signOut(this)
                startSignIn()
//                finish() not ok. On signIn goes home
                recyclerConversations.adapter = null
            }
            R.id.menu_test -> {


            }
            R.id.menu_mute_all -> {
                isMuteAllSounds = !isMuteAllSounds
                getSharedPreferences("_", Context.MODE_PRIVATE).edit().putBoolean(MUTE_ALL, isMuteAllSounds).apply()
                Handler().postDelayed({ invalidateOptionsMenu() }, 500)

            }
            R.id.menu_chat_room -> {
                startActivity(Intent(this, ChatRoomActivity::class.java))
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpRecyclerView(startListening: Boolean) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        val ref = firestore.collection("$USERS/${user.uid}/$CONVERSATIONS")
        val query = ref.orderBy(MSG_TIMESTAMP, Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Conversation>()
                .setQuery(query, Conversation::class.java)
                .build()

        adapter = ConversationAdapter(options, this@MainActivity)
        recyclerConversations.adapter = adapter
        if (startListening) adapter?.startListening()
    }

    override fun onConversationSelected(conversation: Conversation, adapterPosition: Int) {

        val intent = Intent(this, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, conversation.uidP)
                .putExtra(KEY_USER_NAME, conversation.nameP)
                .putExtra(KEY_USER_PHOTO_URL, conversation.photoUrlP)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {
                setUpRecyclerView(true)
                getRegistrationAndSendToServer()

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

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.logo)
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

    companion object {
        val TAG = "MainActivity"
    }

}
