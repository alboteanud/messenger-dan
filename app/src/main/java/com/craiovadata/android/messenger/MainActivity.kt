package com.craiovadata.android.messenger

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
import com.craiovadata.android.messenger.util.Util.sendRegistrationToServer
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
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)
        checkPlayServices(this)
        firestore = FirebaseFirestore.getInstance()

        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)

        setUpRecyclerView()
    }

    public override fun onStart() {
        super.onStart()

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn()
            return
        }

        // Apply filters
//        onFilter(mViewModel.getFilters())

        // Start listening for Firestore updates
        adapter?.startListening()

    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        this.menu = menu

        val isMuteAllSounds = Util.isMuteAll(this)
        setMuteAllSounds(isMuteAllSounds)

        return super.onCreateOptionsMenu(menu)
    }

    private fun setMuteAllSounds(isMutedAllSounds: Boolean) {
        val editor = getSharedPreferences("_", Context.MODE_PRIVATE).edit()
        editor.putBoolean(MUTE_ALL, isMutedAllSounds).apply()

        Handler().postDelayed({
            menu.findItem(R.id.menu_mute_all).isVisible = !isMutedAllSounds
            menu.findItem(R.id.menu_unmute_all).isVisible = isMutedAllSounds
        }, 800)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_search -> {
                val intent = Intent(this@MainActivity, SearchActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            }
            R.id.menu_sign_out -> {
//                removeTokenRegistration()
                AuthUI.getInstance().signOut(this)
                startSignIn()
//                finish() not ok. On signIn goes home
                recyclerConversations.adapter = null
            }
            R.id.menu_mute_all -> {
                setMuteAllSounds(true)
            }
            R.id.menu_unmute_all -> {
                setMuteAllSounds(false)
            }
            R.id.menu_chat_room -> {
                startActivity(Intent(this, AllPeopleActivity::class.java))
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpRecyclerView() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        title = user.displayName

        val ref = firestore.collection("$USERS/${user.uid}/$CONVERSATIONS")
        val query = ref.orderBy(MSG_TIMESTAMP, Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Conversation>()
                .setQuery(query, Conversation::class.java)
                .build()

        adapter = ConversationAdapter(options, this@MainActivity)
        recyclerConversations.setHasFixedSize(true)
        recyclerConversations.adapter = adapter

        adapter?.startListening()
    }

    override fun onConversationSelected(conversation: Conversation, adapterPosition: Int) {

        val intent = Intent(this, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, conversation.uidP)
                .putExtra(KEY_USER_NAME, conversation.nameP)
                .putExtra(KEY_USER_PHOTO_URL, conversation.photoUrlP)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    private fun shouldStartSignIn(): Boolean {
        return viewModel.isSigningIn && FirebaseAuth.getInstance().currentUser == null
    }

    // Sign in with FirebaseUI
    private fun startSignIn() {

        val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.logo)
                .setTheme(R.style.AppTheme)
                .setTosAndPrivacyPolicyUrls(
                        "https://sunshine-f15bf.firebaseapp.com/",
                        "https://sunshine-f15bf.firebaseapp.com/")
                .setAvailableProviders(arrayListOf(
                        AuthUI.IdpConfig.GoogleBuilder().build(),
                        AuthUI.IdpConfig.EmailBuilder().build()
                ))
                .enableAnonymousUsersAutoUpgrade()
                .setIsSmartLockEnabled(false)
                .build()

        startActivityForResult(intent, RC_SIGN_IN)
        viewModel.isSigningIn = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == RESULT_OK) {
                setUpRecyclerView()
                sendRegistrationToServer()
            } else if (shouldStartSignIn()) {
                startSignIn();
            } else if (response == null) {
                // User pressed the back button.
                if (!shouldStartSignIn())
                    finish()
            } else if (response.error != null
                    && response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                showSignInErrorDialog(R.string.message_no_network);
            } else {
                showSignInErrorDialog(R.string.message_unknown);
            }

        }

    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    companion object {
        val TAG = "MainActivity"
    }

    private fun showSignInErrorDialog(@StringRes message: Int) {
        val dialog = AlertDialog.Builder(this)
                .setTitle(com.craiovadata.android.messenger.R.string.title_sign_in_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(com.craiovadata.android.messenger.R.string.option_retry) { _, _ -> startSignIn() }
                .setNegativeButton(com.craiovadata.android.messenger.R.string.option_exit) { _, _ -> finish() }.create()
        dialog.show()
    }

}
