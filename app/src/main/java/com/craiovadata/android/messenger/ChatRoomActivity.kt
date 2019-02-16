package com.craiovadata.android.messenger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.craiovadata.android.messenger.adapter.PeopleAdapter
import com.craiovadata.android.messenger.util.KEY_USER_ID
import com.craiovadata.android.messenger.util.KEY_USER_NAME
import com.craiovadata.android.messenger.util.KEY_USER_PHOTO_URL
import com.craiovadata.android.messenger.util.USERS
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.people_list.*

class ChatRoomActivity : AppCompatActivity(), PeopleAdapter.OnItemSelectedListener {
    private var adapter: PeopleAdapter? = null
    private lateinit var uid: String
    private lateinit var firestore: FirebaseFirestore
    private lateinit var menu: Menu

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_room)
        setSupportActionBar(toolbar)
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            uid = it
        } ?: finish()
        firestore = FirebaseFirestore.getInstance()
        val query = firestore.collection(USERS)
                .whereEqualTo("visible", true)
//                    .orderBy(LAST_MODIF, Query.Direction.DESCENDING)
        adapter = PeopleAdapter(query, this@ChatRoomActivity)
        recyclerPeople.adapter = adapter
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    // onBackPressed
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }

    public override fun onStart() {
        super.onStart()
        adapter?.startListening()
    }

    public override fun onStop() {
        super.onStop()
        adapter?.stopListening()
    }

    override fun onItemSelected(personData: Map<String, Any>) {
        val intent = Intent(this, DetailsActivity::class.java)
                .putExtra(KEY_USER_ID, personData["uid"] as? String)
                .putExtra(KEY_USER_NAME, personData["name"] as? String)
                .putExtra(KEY_USER_PHOTO_URL, personData["photoUrl"] as? String)

        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_people, menu)
        this.menu = menu
        checkVisibleState()
        return super.onCreateOptionsMenu(menu)
    }

    private fun checkVisibleState() {
        val docRef = firestore.collection(USERS).document(uid)

        docRef.get().addOnSuccessListener { document ->
            if (document != null) {
                Log.d(TAG, "DocumentSnapshot data: " + document.data)
                val userData = document.data

                val iAmVisible = userData?.get("visible")
                if (iAmVisible is Boolean) setVisibleUI(iAmVisible)

            } else {
                Log.d(TAG, "No such document")
            }
        }
    }

    private fun setVisibleUI(iAmVisible: Boolean) {
        menu.findItem(R.id.menu_hide_me).isVisible = iAmVisible
        menu.findItem(R.id.menu_show_me).isVisible = !iAmVisible
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_show_me -> {
                val data = HashMap<String, Any>()
                data["visible"] = true
                firestore.collection(USERS).document(uid).update(data)
                setVisibleUI(true)
            }
            R.id.menu_hide_me -> {
                val data = HashMap<String, Any>()
                data["visible"] = false
                firestore.collection(USERS).document(uid).update(data)
                setVisibleUI(false)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val TAG = "ChatRoomActivity"
    }

}

