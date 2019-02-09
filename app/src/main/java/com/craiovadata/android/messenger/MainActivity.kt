package com.craiovadata.android.messenger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import com.craiovadata.android.messenger.adapter.ConversationAdapter
import com.craiovadata.android.messenger.services.MessagingService
import com.craiovadata.android.messenger.util.*
import com.craiovadata.android.messenger.util.Util.checkPlayServices
import com.craiovadata.android.messenger.util.Util.getRegistrationAndSendToServer
import com.craiovadata.android.messenger.util.Util.removeRegistration
import com.craiovadata.android.messenger.viewmodel.MainActivityViewModel
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.conversation_list.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity(),
        ConversationAdapter.OnConversationSelectedListener {

    private var adapter: ConversationAdapter? = null
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbarMain)

        firestore = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build()
        firestore.firestoreSettings = settings

        checkPlayServices(this)
        viewModel = ViewModelProviders.of(this).get(MainActivityViewModel::class.java)
    }

    public override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().currentUser?.let { firebaseUser ->
            title = firebaseUser.displayName
            attachRecyclerViewAdapter(firebaseUser)
        } ?: if (!viewModel.isSigningIn) startSignIn()

    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val isMuteAll = getSharedPreferences("_", Context.MODE_PRIVATE).getBoolean("mute_all", false)

        if (isMuteAll) menu.findItem(R.id.menu_mute_all).title = getString(R.string.unmute_all_menu_label)
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

                val storageRef = FirebaseStorage.getInstance().reference
        val ref = storageRef.child("sounds/users/D4d0gaOukTf0oPKsUP9wKcuqIIC2/busTPSm6uXV33LAh5lz55WLXyGE2/msg_record.3gp")
//                val ref = storageRef.child("dir/btn_picture.png")
                val gsReference = FirebaseStorage.getInstance().getReferenceFromUrl("gs://messenger-2e357.appspot.com/dir/btn_picture.png")
                Log.d(MessagingService.TAG, "  ref storage: " + ref.toString())
                Log.d(MessagingService.TAG, "gsRef storage: " + gsReference.toString())

                val ONE_MEGABYTE: Long = 1024 * 1024
//                gsReference.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                ref.getBytes(ONE_MEGABYTE).addOnSuccessListener {
                    // Data for "images/island.jpg" is returned, use this as needed
                    Log.d(MessagingService.TAG, "storage obj downloaded ")
                    playSound(it)
                }.addOnFailureListener {
                    // Handle any errors
                    Log.d(MessagingService.TAG, "error - storage obj NOT downloaded ")
                }

            }
            R.id.menu_mute_all -> {
                val pref = getSharedPreferences("_", Context.MODE_PRIVATE)
                var isMuteAll = pref.getBoolean("mute_all", false)
                isMuteAll = !isMuteAll
                pref.edit().putBoolean("mute_all", isMuteAll).apply()
                Handler().postDelayed({ invalidateOptionsMenu() }, 500)

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun playSound(soundByteArray: ByteArray) {

         // create temp file that will hold byte array
        val tempMp3 = File.createTempFile("mySound", "3gp", getCacheDir());
        tempMp3.deleteOnExit()
        val fos = FileOutputStream(tempMp3);
        fos.write(soundByteArray);
        fos.close();
        val fis =  FileInputStream(tempMp3);
val mediaSource = MyMediaDataSource(soundByteArray)
        MediaPlayer().apply {
            setAudioStreamType(AudioManager.STREAM_MUSIC)
            setVolume(1.0f, 1.0f)
//            setDataSource(fis.getFD())
            setDataSource(mediaSource)
            setOnCompletionListener { release() }
            prepareAsync() // might take long! (for buffering, etc)
            setOnPreparedListener{
                start()
            }


        }
    }

    private fun attachRecyclerViewAdapter(firebaseUser: FirebaseUser) {
        if (recyclerConversations.adapter == null) {
            val ref = firestore.collection("$USERS/${firebaseUser.uid}/$CONVERSATIONS")
            val query = ref.orderBy(TIMESTAMP_MODIF, Query.Direction.DESCENDING)

            adapter = ConversationAdapter(query, this@MainActivity)
            recyclerConversations.adapter = adapter
        }
        adapter?.startListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            viewModel.isSigningIn = false

            if (resultCode == Activity.RESULT_OK) {
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

    override fun onConversationSelected(conversation: HashMap<String, String?>) {
        Log.d(TAG, "onConvSelected" + conversation)
        val intent = Intent(this, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, conversation[UID])
                .putExtra(KEY_USER_NAME, conversation[NAME])
                .putExtra(KEY_USER_PHOTO_URL, conversation[PHOTO_URL])

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    private fun startSignIn() {
        // Sign in with FirebaseUI
        val intent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setLogo(R.drawable.bird_logo)
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
